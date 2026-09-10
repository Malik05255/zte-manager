package com.malik.ztesmartmanager.core.tower

import com.malik.ztesmartmanager.core.model.RouterSnapshot

data class PostLockVerificationReport(
    val requestedSamples: Int,
    val successfulReads: Int,
    val exactRadioMatches: Int,
    val unknownSamples: Int,
    val driftSamples: Int,
    val radioVerified: Boolean,
    val stableBand: String?,
    val stableCellId: Long?,
    val stableEnodebId: String?,
    val medianRsrp: Double?,
    val medianRsrq: Double?,
    val medianSinr: Double?,
    val message: String
)

/**
 * Pure multi-sample verifier for the serving LTE cell after a successful Cell Lock read-back.
 * A lock is considered live-verified only when at least three samples match PCI+EARFCN and no
 * structurally complete sample shows a different serving cell. Missing samples never become zero.
 */
object PostLockVerificationAnalyzer {
    fun analyze(
        requestedTarget: TowerTarget,
        samples: List<RouterSnapshot?>,
        requiredExactMatches: Int = 3
    ): PostLockVerificationReport {
        val required = requiredExactMatches.coerceAtLeast(1)
        val matched = mutableListOf<RouterSnapshot>()
        var unknown = 0
        var drift = 0

        samples.forEach { snapshot ->
            if (snapshot == null || snapshot.pci == null || snapshot.earfcn == null) {
                unknown++
                return@forEach
            }
            if (snapshot.pci == requestedTarget.pci && snapshot.earfcn == requestedTarget.earfcn) {
                matched += snapshot
            } else {
                drift++
            }
        }

        val radioVerified = matched.size >= required && drift == 0
        val stableBand = stableRepeated(matched.mapNotNull { it.lteBand }, minimumObservations = 2)
        val stableCellId = stableRepeated(
            matched.mapNotNull { it.cellId?.takeIf { id -> id > 0 } },
            minimumObservations = 2
        )
        val stableEnodeb = stableCellId?.let { (it shr 8).toString() }
        val medianRsrp = median(matched.mapNotNull { it.lteRsrp })
        val medianRsrq = median(matched.mapNotNull { it.lteRsrq })
        val medianSinr = median(matched.mapNotNull { it.lteSinr })

        val message = when {
            drift > 0 -> "ظهرت $drift قراءة حية على PCI/EARFCN مختلفين بعد القفل؛ لم يثبت القفل الحي"
            matched.size < required -> "تطابقت ${matched.size}/$required قراءات مطلوبة فقط بعد القفل؛ الدليل الحي غير كافٍ"
            stableCellId != null -> "تطابقت ${matched.size}/${samples.size} قراءات حية مع القفل، وتكرر Cell ID نفسه"
            else -> "تطابقت ${matched.size}/${samples.size} قراءات حية مع القفل؛ Cell ID غير كافٍ لإثبات هوية فيزيائية ثابتة"
        }

        return PostLockVerificationReport(
            requestedSamples = samples.size,
            successfulReads = samples.count { it != null },
            exactRadioMatches = matched.size,
            unknownSamples = unknown,
            driftSamples = drift,
            radioVerified = radioVerified,
            stableBand = stableBand,
            stableCellId = stableCellId,
            stableEnodebId = stableEnodeb,
            medianRsrp = medianRsrp,
            medianRsrq = medianRsrq,
            medianSinr = medianSinr,
            message = message
        )
    }

    private fun <T> stableRepeated(values: List<T>, minimumObservations: Int): T? {
        if (values.size < minimumObservations) return null
        val distinct = values.distinct()
        return distinct.singleOrNull()
    }

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle]
        else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
}
