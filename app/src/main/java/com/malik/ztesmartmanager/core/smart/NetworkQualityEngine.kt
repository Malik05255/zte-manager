package com.malik.ztesmartmanager.core.smart

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class QualityScore(
    val total: Int,
    val signal: Int,
    val cleanliness: Int,
    val quality: Int,
    val stability: Int,
    val label: String
)

data class PlacementReading(
    val score: QualityScore,
    val instantScore: Int,
    val guidance: PlacementGuidance,
    val deltaFromBest: Int,
    val bestScore: Int,
    val confidence: Int,
    val cellChanged: Boolean
)

enum class PlacementGuidance {
    INITIAL,
    MUCH_BETTER,
    BETTER,
    STABLE,
    WORSE,
    RETURN_TO_BEST,
    CELL_CHANGED_WORSE,
    EXCELLENT_HOLD,
    BEST_SO_FAR
}

/**
 * Evidence-only RF scorer.
 *
 * Missing metrics contribute no weight. A missing LTE or NR metric is never replaced by a made-up
 * middle score. Multi-sample scoring uses medians to reduce radio spikes, while stability is based
 * on measured variance and actual serving-cell identity changes.
 */
class NetworkQualityEngine(
    private val windowSize: Int = 6,
    private val smoothingAlpha: Double = 0.48
) {
    private val history = ArrayDeque<RouterSnapshot>()
    private var bestScore = 0
    private var previousSmoothed: Double? = null
    private var previousSnapshot: RouterSnapshot? = null

    fun add(snapshot: RouterSnapshot): PlacementReading {
        history.addLast(snapshot)
        while (history.size > windowSize) history.removeFirst()

        val samples = history.toList()
        val instant = scoreSamples(samples)
        val previous = previousSmoothed
        val smoothed = if (previous == null) instant.total.toDouble()
        else previous + smoothingAlpha * (instant.total - previous)
        val total = smoothed.roundToInt().coerceIn(0, 100)

        val previousRouter = previousSnapshot
        val cellChanged = previousRouter != null && identityChanged(previousRouter, snapshot)
        val previousInt = previous?.roundToInt()
        val oldBest = bestScore
        val wasBest = total > bestScore
        if (wasBest) bestScore = total
        val delta = if (previousInt == null) 0 else total - previousInt
        val gapFromBest = total - bestScore

        val guidance = when {
            previousInt == null -> PlacementGuidance.INITIAL
            cellChanged && delta <= -2 -> PlacementGuidance.CELL_CHANGED_WORSE
            wasBest && total >= 82 -> PlacementGuidance.BEST_SO_FAR
            oldBest - total >= 6 -> PlacementGuidance.RETURN_TO_BEST
            delta >= 5 -> PlacementGuidance.MUCH_BETTER
            delta >= 2 -> PlacementGuidance.BETTER
            delta <= -2 -> PlacementGuidance.WORSE
            total >= 90 && abs(delta) <= 1 -> PlacementGuidance.EXCELLENT_HOLD
            else -> PlacementGuidance.STABLE
        }

        previousSmoothed = smoothed
        previousSnapshot = snapshot

        return PlacementReading(
            score = instant.copy(total = total, label = qualityLabel(total)),
            instantScore = instant.total,
            guidance = guidance,
            deltaFromBest = gapFromBest,
            bestScore = bestScore,
            confidence = confidence(samples),
            cellChanged = cellChanged
        )
    }

    fun reset() {
        history.clear()
        bestScore = 0
        previousSmoothed = null
        previousSnapshot = null
    }

    fun score(snapshot: RouterSnapshot): QualityScore = scoreSamples(listOf(snapshot))

    fun scoreSamples(samples: List<RouterSnapshot>): QualityScore {
        if (samples.isEmpty()) return emptyScore()

        val lteRsrp = median(samples.mapNotNull { it.lteRsrp })?.let { normalize(it, -122.0, -75.0) }
        val lteRsrq = median(samples.mapNotNull { it.lteRsrq })?.let { normalize(it, -21.0, -7.0) }
        val lteSinr = median(samples.mapNotNull { it.lteSinr })?.let { normalize(it, -5.0, 27.0) }
        val nrRsrp = median(samples.mapNotNull { it.nrRsrp })?.let { normalize(it, -122.0, -74.0) }
        val nrSinr = median(samples.mapNotNull { it.nrSinr })?.let { normalize(it, -5.0, 28.0) }
        val stability = calculateStability(samples)

        val weighted = mutableListOf<Pair<Int, Double>>()
        lteRsrp?.let { weighted += it to 0.18 }
        lteRsrq?.let { weighted += it to 0.14 }
        lteSinr?.let { weighted += it to 0.28 }
        nrRsrp?.let { weighted += it to 0.14 }
        nrSinr?.let { weighted += it to 0.16 }
        if (samples.size >= 3) weighted += stability to 0.10

        if (weighted.isEmpty()) return emptyScore()
        val weightSum = weighted.sumOf { it.second }
        val total = (weighted.sumOf { it.first * it.second } / weightSum).roundToInt().coerceIn(0, 100)

        val signalValues = listOfNotNull(lteRsrp, nrRsrp)
        val cleanlinessValues = listOfNotNull(lteSinr, nrSinr)
        val signal = signalValues.takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: 0
        val cleanliness = cleanlinessValues.takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: 0
        val quality = lteRsrq ?: 0

        return QualityScore(
            total = total,
            signal = signal.coerceIn(0, 100),
            cleanliness = cleanliness.coerceIn(0, 100),
            quality = quality.coerceIn(0, 100),
            stability = stability,
            label = qualityLabel(total)
        )
    }

    private fun calculateStability(samples: List<RouterSnapshot>): Int {
        if (samples.size < 3) return 0

        val rsrp = samples.mapNotNull { it.nrRsrp ?: it.lteRsrp }
        val sinr = samples.mapNotNull { it.nrSinr ?: it.lteSinr }
        val rsrpDeviation = standardDeviation(rsrp)
        val sinrDeviation = standardDeviation(sinr)
        val identityChanges = samples.zipWithNext().count { (a, b) -> identityChanged(a, b) }
        val penalty = (rsrpDeviation * 6.0 + sinrDeviation * 5.0 + identityChanges * 12.0).roundToInt()
        return (100 - penalty).coerceIn(0, 100)
    }

    private fun identityChanged(a: RouterSnapshot, b: RouterSnapshot): Boolean {
        if (a.cellId != null && b.cellId != null && a.cellId != b.cellId) return true
        if (a.pci != null && b.pci != null && a.pci != b.pci) return true
        if (a.earfcn != null && b.earfcn != null && a.earfcn != b.earfcn) return true
        return false
    }

    private fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean).pow(2) } / values.size)
    }

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle]
        else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    private fun normalize(value: Double, bad: Double, excellent: Double): Int {
        val ratio = ((value - bad) / (excellent - bad)).coerceIn(0.0, 1.0)
        return (ratio * 100).roundToInt()
    }

    private fun confidence(samples: List<RouterSnapshot>): Int {
        val countScore = when {
            samples.size >= windowSize -> 100
            samples.size <= 1 -> 30
            else -> (30 + (samples.size - 1) * (70.0 / (windowSize - 1))).roundToInt()
        }
        val latest = samples.lastOrNull() ?: return 0
        val evidenceCount = listOf(
            latest.lteRsrp,
            latest.lteRsrq,
            latest.lteSinr,
            latest.nrRsrp,
            latest.nrSinr
        ).count { it != null }
        val evidenceScore = (evidenceCount * 20).coerceIn(0, 100)
        return ((countScore * 0.65) + (evidenceScore * 0.35)).roundToInt().coerceIn(0, 100)
    }

    private fun emptyScore() = QualityScore(
        total = 0,
        signal = 0,
        cleanliness = 0,
        quality = 0,
        stability = 0,
        label = "غير مؤكد"
    )

    private fun qualityLabel(total: Int): String = when {
        total >= 92 -> "ممتاز جدًا"
        total >= 82 -> "ممتاز"
        total >= 70 -> "جيد جدًا"
        total >= 58 -> "جيد"
        total >= 43 -> "متوسط"
        total > 0 -> "ضعيف"
        else -> "غير مؤكد"
    }
}
