package com.malik.ztesmartmanager.core.tower

import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import kotlinx.coroutines.delay

data class VerifiedTowerLockResult(
    val success: Boolean,
    val target: TowerTarget?,
    val fingerprint: TowerFingerprint?,
    val validation: CandidateValidation,
    val message: String
)

/**
 * Single safe path for locking a scanned/current LTE cell.
 * It never writes unless the candidate is freshly re-observed first.
 */
class VerifiedTowerLockCoordinator(
    private val client: ZteRouterClient,
    private val engine: TowerLockEngine,
    private val routerAddress: String
) {
    suspend fun lock(candidate: NearbyCell): VerifiedTowerLockResult {
        val validation = engine.revalidateCandidate(candidate)
        if (!validation.valid) {
            return VerifiedTowerLockResult(false, null, null, validation, validation.message)
        }

        val observed = validation.observed ?: return VerifiedTowerLockResult(
            false, null, null, validation, "فشل فحص ما قبل القفل رغم اكتمال المسح؛ لم يُرسل أمر"
        )
        val pci = observed.pci ?: return VerifiedTowerLockResult(false, null, null, validation, "PCI غير متاح")
        val arfcn = observed.arfcn ?: return VerifiedTowerLockResult(false, null, null, validation, "EARFCN غير متاح")

        val write = client.setCellLock(pci, arfcn)
        if (!write.success || !write.verified) {
            return VerifiedTowerLockResult(false, null, null, validation, write.message)
        }

        delay(1_500)
        val after = runCatching { client.readSnapshot() }.getOrNull()
            ?: return VerifiedTowerLockResult(
                false, null, null, validation,
                "تمت قراءة القفل من الراوتر لكن تعذر قراءة الخلية الحية؛ لن نسجل نجاحًا أو بصمة"
            )

        val requestedTarget = TowerTarget(
            pci = pci,
            earfcn = arfcn,
            band = observed.band,
            cellId = null,
            enodebId = null
        )
        val match = engine.compare(requestedTarget, after)
        if (match != TowerMatch.MATCHED) {
            return VerifiedTowerLockResult(
                false, null, null, validation,
                "القفل محفوظ لكن الخلية الحية لا تطابق PCI/EARFCN بعد الانتظار؛ لن نسجل نجاحًا أو بصمة"
            )
        }

        val liveTarget = engine.captureCurrent(after) ?: requestedTarget
        val fingerprint = TowerFingerprint(
            createdAtEpochMs = System.currentTimeMillis(),
            routerAddress = routerAddress,
            profileId = client.profile.id,
            pci = liveTarget.pci,
            earfcn = liveTarget.earfcn,
            band = liveTarget.band ?: observed.band,
            cellId = liveTarget.cellId,
            enodebId = liveTarget.enodebId,
            evidenceScore = observed.evidenceScore,
            presencePercent = observed.presencePercent,
            medianRsrp = observed.rsrp
        )

        return VerifiedTowerLockResult(
            success = true,
            target = liveTarget,
            fingerprint = fingerprint,
            validation = validation,
            message = "تمت إعادة رؤية الخلية، تطبيق القفل، مطابقة read-back، ثم مطابقة الخلية الحية؛ حُفظت بصمة الراديو الموثقة"
        )
    }
}
