package com.malik.ztesmartmanager.core.tower

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import kotlinx.coroutines.delay

data class VerifiedTowerLockResult(
    val success: Boolean,
    val writeAttempted: Boolean,
    val target: TowerTarget?,
    val fingerprint: TowerFingerprint?,
    val validation: CandidateValidation,
    val message: String,
    val postVerification: PostLockVerificationReport? = null
)

/**
 * Single safe path for locking a scanned/current LTE cell.
 * It never writes unless the candidate is freshly re-observed first, and never records success
 * from a single post-write serving-cell sample.
 */
class VerifiedTowerLockCoordinator(
    private val client: ZteRouterClient,
    private val engine: TowerLockEngine,
    private val routerAddress: String
) {
    suspend fun lock(candidate: NearbyCell): VerifiedTowerLockResult {
        val validation = engine.revalidateCandidate(candidate)
        if (!validation.valid) {
            return VerifiedTowerLockResult(false, false, null, null, validation, validation.message)
        }

        val observed = validation.observed ?: return VerifiedTowerLockResult(
            false, false, null, null, validation, "فشل فحص ما قبل القفل رغم اكتمال المسح؛ لم يُرسل أمر"
        )
        val pci = observed.pci ?: return VerifiedTowerLockResult(false, false, null, null, validation, "PCI غير متاح")
        val arfcn = observed.arfcn ?: return VerifiedTowerLockResult(false, false, null, null, validation, "EARFCN غير متاح")

        val write = client.setCellLock(pci, arfcn)
        if (!write.success || !write.verified) {
            return VerifiedTowerLockResult(false, true, null, null, validation, write.message)
        }

        val requestedTarget = TowerTarget(
            pci = pci,
            earfcn = arfcn,
            band = observed.band,
            cellId = null,
            enodebId = null
        )

        delay(POST_LOCK_SETTLE_MS)
        val liveSamples = mutableListOf<RouterSnapshot?>()
        repeat(POST_LOCK_SAMPLE_COUNT) { index ->
            liveSamples += runCatching { client.readSnapshot() }.getOrNull()
            if (index < POST_LOCK_SAMPLE_COUNT - 1) delay(POST_LOCK_SAMPLE_INTERVAL_MS)
        }

        val postVerification = PostLockVerificationAnalyzer.analyze(
            requestedTarget = requestedTarget,
            samples = liveSamples,
            requiredExactMatches = REQUIRED_EXACT_MATCHES
        )
        if (!postVerification.radioVerified) {
            return VerifiedTowerLockResult(
                success = false,
                writeAttempted = true,
                target = null,
                fingerprint = null,
                validation = validation,
                message = "القفل مطابق في read-back، لكن التحقق الحي متعدد العينات فشل: ${postVerification.message}",
                postVerification = postVerification
            )
        }

        val liveTarget = TowerTarget(
            pci = pci,
            earfcn = arfcn,
            band = postVerification.stableBand ?: observed.band,
            cellId = postVerification.stableCellId,
            enodebId = postVerification.stableEnodebId
        )
        val fingerprint = TowerFingerprint(
            createdAtEpochMs = System.currentTimeMillis(),
            routerAddress = routerAddress,
            profileId = client.profile.id,
            pci = liveTarget.pci,
            earfcn = liveTarget.earfcn,
            band = liveTarget.band,
            cellId = liveTarget.cellId,
            enodebId = liveTarget.enodebId,
            evidenceScore = observed.evidenceScore,
            presencePercent = observed.presencePercent,
            medianRsrp = postVerification.medianRsrp ?: observed.rsrp
        )

        return VerifiedTowerLockResult(
            success = true,
            writeAttempted = true,
            target = liveTarget,
            fingerprint = fingerprint,
            validation = validation,
            message = "تمت إعادة رؤية الخلية، تطبيق القفل، مطابقة read-back، ثم ${postVerification.exactRadioMatches}/${postVerification.requestedSamples} تطابقات حية متعددة؛ حُفظت بصمة الراديو الموثقة. ${postVerification.message}",
            postVerification = postVerification
        )
    }

    companion object {
        private const val POST_LOCK_SETTLE_MS = 1_500L
        private const val POST_LOCK_SAMPLE_COUNT = 4
        private const val POST_LOCK_SAMPLE_INTERVAL_MS = 500L
        private const val REQUIRED_EXACT_MATCHES = 3
    }
}
