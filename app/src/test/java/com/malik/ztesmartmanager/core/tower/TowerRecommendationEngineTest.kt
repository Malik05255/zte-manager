package com.malik.ztesmartmanager.core.tower

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TowerRecommendationEngineTest {
    @Test
    fun strongHighConfidenceCandidateWithClearAdvantage_isRecommended() {
        val current = rankedCell(pci = 10, arfcn = 1300, score = 62, confidence = CellConfidence.HIGH)
        val best = rankedCell(pci = 20, arfcn = 1650, score = 78, confidence = CellConfidence.HIGH)

        val result = TowerRecommendationEngine.recommend(listOf(best, current), 10, 1300)

        assertEquals(TowerRecommendationDecision.RECOMMEND_CANDIDATE, result.decision)
        assertEquals(20, result.candidate?.pci)
        assertEquals(16, result.scoreAdvantage)
    }

    @Test
    fun smallDifference_keepsCurrentCell() {
        val current = rankedCell(pci = 10, arfcn = 1300, score = 70, confidence = CellConfidence.HIGH)
        val best = rankedCell(pci = 20, arfcn = 1650, score = 76, confidence = CellConfidence.HIGH)

        val result = TowerRecommendationEngine.recommend(listOf(best, current), 10, 1300)

        assertEquals(TowerRecommendationDecision.KEEP_CURRENT, result.decision)
        assertEquals(10, result.candidate?.pci)
        assertEquals(6, result.scoreAdvantage)
    }

    @Test
    fun bestCellIsCurrent_keepsCurrentCell() {
        val current = rankedCell(pci = 10, arfcn = 1300, score = 82, confidence = CellConfidence.HIGH)
        val other = rankedCell(pci = 20, arfcn = 1650, score = 65, confidence = CellConfidence.HIGH)

        val result = TowerRecommendationEngine.recommend(listOf(current, other), 10, 1300)

        assertEquals(TowerRecommendationDecision.KEEP_CURRENT, result.decision)
        assertEquals(10, result.candidate?.pci)
        assertEquals(0, result.scoreAdvantage)
    }

    @Test
    fun mediumConfidenceBestCell_doesNotRecommendLock() {
        val best = rankedCell(
            pci = 20,
            arfcn = 1650,
            score = 80,
            confidence = CellConfidence.MEDIUM,
            seen = 3,
            total = 5,
            presence = 60
        )

        val result = TowerRecommendationEngine.recommend(listOf(best), null, null)

        assertEquals(TowerRecommendationDecision.INSUFFICIENT_EVIDENCE, result.decision)
        assertEquals(20, result.candidate?.pci)
    }

    @Test
    fun twoNearlyTiedHighConfidenceCandidates_returnsInsufficientEvidence() {
        val first = rankedCell(pci = 20, arfcn = 1650, score = 80, confidence = CellConfidence.HIGH)
        val second = rankedCell(pci = 21, arfcn = 1800, score = 78, confidence = CellConfidence.HIGH)

        val result = TowerRecommendationEngine.recommend(listOf(first, second), null, null)

        assertEquals(TowerRecommendationDecision.INSUFFICIENT_EVIDENCE, result.decision)
        assertEquals(20, result.candidate?.pci)
    }

    @Test
    fun missingRfScore_neverCreatesRecommendation() {
        val cell = NearbyCell(
            rat = "LTE",
            band = "B3",
            pci = 20,
            arfcn = 1650,
            rsrp = null,
            rsrq = null,
            sinr = null,
            samplesSeen = 5,
            samplesTotal = 5,
            presencePercent = 100,
            stabilityScore = null,
            evidenceScore = null,
            confidence = CellConfidence.LOW
        )

        val result = TowerRecommendationEngine.recommend(listOf(cell), null, null)

        assertEquals(TowerRecommendationDecision.INSUFFICIENT_EVIDENCE, result.decision)
        assertNull(result.candidate)
    }

    private fun rankedCell(
        pci: Int,
        arfcn: Int,
        score: Int,
        confidence: CellConfidence,
        seen: Int = 5,
        total: Int = 5,
        presence: Int = 100
    ) = NearbyCell(
        rat = "LTE",
        band = "B3",
        pci = pci,
        arfcn = arfcn,
        rsrp = -88.0,
        rsrq = -10.0,
        sinr = 18.0,
        samplesSeen = seen,
        samplesTotal = total,
        presencePercent = presence,
        stabilityScore = 90,
        evidenceScore = score,
        confidence = confidence
    )
}
