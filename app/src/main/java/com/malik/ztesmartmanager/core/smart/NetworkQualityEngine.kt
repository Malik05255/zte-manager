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
 * Fast placement scorer tuned for small physical router movements.
 *
 * The score intentionally gives SINR more weight than raw RSRP because a strong but noisy
 * signal can perform worse than a slightly weaker clean signal. A short moving window and
 * EMA smoothing keep the assistant responsive without reacting to every radio spike.
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

        val instant = score(snapshot, history.toList())
        val previous = previousSmoothed
        val smoothed = if (previous == null) {
            instant.total.toDouble()
        } else {
            previous + smoothingAlpha * (instant.total - previous)
        }
        val total = smoothed.roundToInt().coerceIn(0, 100)

        val previousRouter = previousSnapshot
        val cellChanged = previousRouter != null && (
            (snapshot.cellId != null && previousRouter.cellId != null && snapshot.cellId != previousRouter.cellId) ||
                (snapshot.pci != null && previousRouter.pci != null && snapshot.pci != previousRouter.pci)
            )

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
            confidence = confidence(history.size),
            cellChanged = cellChanged
        )
    }

    fun reset() {
        history.clear()
        bestScore = 0
        previousSmoothed = null
        previousSnapshot = null
    }

    fun score(snapshot: RouterSnapshot): QualityScore = score(snapshot, listOf(snapshot))

    private fun score(current: RouterSnapshot, samples: List<RouterSnapshot>): QualityScore {
        val lteRsrp = normalize(current.lteRsrp, bad = -122.0, excellent = -75.0)
        val lteRsrq = normalize(current.lteRsrq, bad = -21.0, excellent = -7.0)
        val lteSinr = normalize(current.lteSinr, bad = -5.0, excellent = 27.0)
        val nrRsrp = normalizeNullable(current.nrRsrp, bad = -122.0, excellent = -74.0)
        val nrSinr = normalizeNullable(current.nrSinr, bad = -5.0, excellent = 28.0)
        val stability = calculateStability(samples)

        val weighted = mutableListOf<Pair<Int, Double>>()
        weighted += lteRsrp to 0.20
        weighted += lteRsrq to 0.15
        weighted += lteSinr to 0.32
        nrRsrp?.let { weighted += it to 0.08 }
        nrSinr?.let { weighted += it to 0.10 }
        weighted += stability to 0.15

        val weightSum = weighted.sumOf { it.second }
        val total = if (weightSum <= 0.0) 0 else {
            (weighted.sumOf { it.first * it.second } / weightSum).roundToInt().coerceIn(0, 100)
        }

        val signal = if (nrRsrp == null) lteRsrp else ((lteRsrp * 0.65) + (nrRsrp * 0.35)).roundToInt()
        val cleanliness = if (nrSinr == null) lteSinr else ((lteSinr * 0.65) + (nrSinr * 0.35)).roundToInt()

        return QualityScore(
            total = total,
            signal = signal.coerceIn(0, 100),
            cleanliness = cleanliness.coerceIn(0, 100),
            quality = lteRsrq,
            stability = stability,
            label = qualityLabel(total)
        )
    }

    private fun calculateStability(samples: List<RouterSnapshot>): Int {
        if (samples.size < 3) return 74

        val rsrp = samples.mapNotNull { it.lteRsrp ?: it.nrRsrp }
        val sinr = samples.mapNotNull { it.lteSinr ?: it.nrSinr }
        if (rsrp.size < 3 && sinr.size < 3) return 60

        val rsrpDeviation = standardDeviation(rsrp)
        val sinrDeviation = standardDeviation(sinr)
        val identityChanges = samples.zipWithNext().count { (a, b) ->
            (a.cellId != null && b.cellId != null && a.cellId != b.cellId) ||
                (a.pci != null && b.pci != null && a.pci != b.pci)
        }
        val penalty = (rsrpDeviation * 6.0 + sinrDeviation * 5.0 + identityChanges * 7.0).roundToInt()
        return (100 - penalty).coerceIn(0, 100)
    }

    private fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean).pow(2) } / values.size)
    }

    private fun normalize(value: Double?, bad: Double, excellent: Double): Int =
        normalizeNullable(value, bad, excellent) ?: 45

    private fun normalizeNullable(value: Double?, bad: Double, excellent: Double): Int? {
        if (value == null) return null
        val ratio = ((value - bad) / (excellent - bad)).coerceIn(0.0, 1.0)
        return (ratio * 100).roundToInt()
    }

    private fun confidence(sampleCount: Int): Int = when {
        sampleCount >= windowSize -> 100
        sampleCount <= 1 -> 35
        else -> (35 + (sampleCount - 1) * (65.0 / (windowSize - 1))).roundToInt()
    }

    private fun qualityLabel(total: Int): String = when {
        total >= 92 -> "ممتاز جدًا"
        total >= 82 -> "ممتاز"
        total >= 70 -> "جيد جدًا"
        total >= 58 -> "جيد"
        total >= 43 -> "متوسط"
        else -> "ضعيف"
    }
}
