package com.malik.ztesmartmanager.core.tower

enum class TowerRecommendationDecision {
    KEEP_CURRENT,
    RECOMMEND_CANDIDATE,
    INSUFFICIENT_EVIDENCE
}

data class TowerRecommendation(
    val decision: TowerRecommendationDecision,
    val candidate: NearbyCell?,
    val current: NearbyCell?,
    val scoreAdvantage: Int?,
    val reason: String
)

data class CandidateValidation(
    val valid: Boolean,
    val candidate: NearbyCell,
    val observed: NearbyCell?,
    val report: TowerScanReport,
    val message: String
)

/**
 * Conservative recommendation policy layered on top of the evidence-ranked scan.
 * Ranking alone is not enough to recommend a lock.
 */
object TowerRecommendationEngine {
    private const val MIN_RECOMMEND_SCORE = 55
    private const val MIN_PRESENCE_PERCENT = 60
    private const val MIN_SEEN_SAMPLES = 3
    private const val MIN_SCORE_ADVANTAGE_OVER_CURRENT = 8
    private const val MIN_SCORE_ADVANTAGE_OVER_RUNNER_UP = 4

    fun recommend(
        cells: List<NearbyCell>,
        currentPci: Int?,
        currentArfcn: Int?
    ): TowerRecommendation {
        val eligible = cells
            .filter(::isEligible)
            .sortedWith(
                compareByDescending<NearbyCell> { it.evidenceScore ?: -1 }
                    .thenByDescending { it.presencePercent }
                    .thenByDescending { it.rsrp ?: -999.0 }
            )

        if (eligible.isEmpty()) {
            return TowerRecommendation(
                decision = TowerRecommendationDecision.INSUFFICIENT_EVIDENCE,
                candidate = null,
                current = findCurrent(cells, currentPci, currentArfcn),
                scoreAdvantage = null,
                reason = "لا توجد خلية LTE بدليل كافٍ للتوصية؛ لن يختار التطبيق برجًا بالتخمين"
            )
        }

        val best = eligible.first()
        val current = findCurrent(cells, currentPci, currentArfcn)
        val currentEligible = current?.takeIf(::isEligible)

        if (sameIdentity(best, current)) {
            return TowerRecommendation(
                decision = TowerRecommendationDecision.KEEP_CURRENT,
                candidate = best,
                current = current,
                scoreAdvantage = 0,
                reason = "الخلية الحالية هي الأعلى دليلًا؛ لا حاجة لتغيير القفل"
            )
        }

        val bestScore = best.evidenceScore ?: return insufficient(current)
        val currentScore = currentEligible?.evidenceScore
        if (currentScore != null) {
            val advantage = bestScore - currentScore
            if (advantage < MIN_SCORE_ADVANTAGE_OVER_CURRENT) {
                return TowerRecommendation(
                    decision = TowerRecommendationDecision.KEEP_CURRENT,
                    candidate = currentEligible,
                    current = currentEligible,
                    scoreAdvantage = advantage,
                    reason = "الفارق عن الخلية الحالية صغير ($advantage نقاط)؛ الأفضل إبقاء الاتصال الحالي بدل التبديل غير الضروري"
                )
            }
        }

        // A candidate that may trigger a user lock must be high-confidence, not merely eligible.
        if (best.confidence != CellConfidence.HIGH || bestScore < MIN_RECOMMEND_SCORE) {
            return TowerRecommendation(
                decision = TowerRecommendationDecision.INSUFFICIENT_EVIDENCE,
                candidate = best,
                current = current,
                scoreAdvantage = currentScore?.let { bestScore - it },
                reason = "أفضل نتيجة موجودة، لكن الثقة ليست عالية بما يكفي لاقتراح تثبيتها"
            )
        }

        val runnerUp = eligible.drop(1).firstOrNull { !sameIdentity(it, current) }
        val runnerScore = runnerUp?.evidenceScore
        if (runnerScore != null && bestScore - runnerScore < MIN_SCORE_ADVANTAGE_OVER_RUNNER_UP) {
            return TowerRecommendation(
                decision = TowerRecommendationDecision.INSUFFICIENT_EVIDENCE,
                candidate = best,
                current = current,
                scoreAdvantage = currentScore?.let { bestScore - it },
                reason = "أفضل خليتين متقاربتان جدًا؛ أعد المسح بدل تثبيت نتيجة غير حاسمة"
            )
        }

        return TowerRecommendation(
            decision = TowerRecommendationDecision.RECOMMEND_CANDIDATE,
            candidate = best,
            current = current,
            scoreAdvantage = currentScore?.let { bestScore - it },
            reason = buildString {
                append("مرشح موثوق: ظهر ${best.samplesSeen}/${best.samplesTotal} مرات")
                best.evidenceScore?.let { append(" • دليل $it/100") }
                currentScore?.let { append(" • أفضل من الحالية بـ ${bestScore - it} نقاط") }
            }
        )
    }

    fun sameIdentity(a: NearbyCell?, b: NearbyCell?): Boolean {
        if (a == null || b == null) return false
        return a.rat == b.rat && a.pci != null && a.arfcn != null &&
            a.pci == b.pci && a.arfcn == b.arfcn
    }

    private fun findCurrent(cells: List<NearbyCell>, pci: Int?, arfcn: Int?): NearbyCell? {
        if (pci == null || arfcn == null) return null
        return cells.firstOrNull { it.rat == "LTE" && it.pci == pci && it.arfcn == arfcn }
    }

    private fun isEligible(cell: NearbyCell): Boolean =
        cell.rat == "LTE" &&
            cell.pci != null &&
            cell.arfcn != null &&
            cell.evidenceScore != null &&
            cell.samplesTotal >= 3 &&
            cell.samplesSeen >= MIN_SEEN_SAMPLES &&
            cell.presencePercent >= MIN_PRESENCE_PERCENT &&
            (cell.confidence == CellConfidence.MEDIUM || cell.confidence == CellConfidence.HIGH)

    private fun insufficient(current: NearbyCell?) = TowerRecommendation(
        decision = TowerRecommendationDecision.INSUFFICIENT_EVIDENCE,
        candidate = null,
        current = current,
        scoreAdvantage = null,
        reason = "بيانات التقييم ناقصة؛ لا توجد توصية آمنة"
    )
}

/**
 * Fresh read-only validation performed immediately before a lock attempt.
 * No router write occurs in this function.
 */
suspend fun TowerLockEngine.revalidateCandidate(candidate: NearbyCell): CandidateValidation {
    if (candidate.rat != "LTE" || candidate.pci == null || candidate.arfcn == null) {
        return CandidateValidation(
            valid = false,
            candidate = candidate,
            observed = null,
            report = TowerScanReport(3, 0, emptyList(), 0L),
            message = "الخلية لا تملك هوية LTE كاملة؛ لم يُرسل أي أمر"
        )
    }

    val report = scanNearbyCells(requestedSamples = 3, intervalMs = 450L)
    val observed = report.cells.firstOrNull {
        it.rat == "LTE" && it.pci == candidate.pci && it.arfcn == candidate.arfcn
    }

    if (report.successfulSamples < 2 || observed == null) {
        return CandidateValidation(
            valid = false,
            candidate = candidate,
            observed = observed,
            report = report,
            message = "لم تُرصد الخلية المختارة بثبات في فحص ما قبل القفل؛ لم يُرسل أمر القفل"
        )
    }

    val repeatedEnough = observed.samplesSeen >= 2 && observed.presencePercent >= 60
    val hasRfEvidence = observed.evidenceScore != null && observed.rsrp != null
    val severeDrop = candidate.rsrp != null && observed.rsrp != null && observed.rsrp < candidate.rsrp - 15.0
    val valid = repeatedEnough && hasRfEvidence && !severeDrop

    return CandidateValidation(
        valid = valid,
        candidate = candidate,
        observed = observed,
        report = report,
        message = when {
            severeDrop -> "الخلية نفسها ما زالت موجودة لكن الإشارة هبطت أكثر من 15 dB؛ أعد المسح قبل التثبيت"
            !repeatedEnough -> "ظهور الخلية غير متكرر بما يكفي قبل القفل؛ لم يُرسل أي أمر"
            !hasRfEvidence -> "هوية الخلية ظهرت لكن قياس RF غير كافٍ؛ لم يُرسل أي أمر"
            else -> "تمت إعادة رؤية الخلية المختارة في ${observed.samplesSeen}/${observed.samplesTotal} قراءات وهي جاهزة لمحاولة قفل موثّقة"
        }
    )
}
