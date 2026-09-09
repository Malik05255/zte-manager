package com.malik.ztesmartmanager.core.smart

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import kotlin.math.pow
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
    val guidance: PlacementGuidance,
    val deltaFromBest: Int,
    val bestScore: Int
)

enum class PlacementGuidance {
    INITIAL,
    MUCH_BETTER,
    BETTER,
    STABLE,
    WORSE,
    BEST_SO_FAR
}

class NetworkQualityEngine(private val windowSize: Int = 8) {
    private val history = ArrayDeque<RouterSnapshot>()
    private var bestScore = 0
    private var previousScore: Int? = null

    fun add(snapshot: RouterSnapshot): PlacementReading {
        history.addLast(snapshot)
        while (history.size > windowSize) history.removeFirst()

        val score = score(snapshot, history.toList())
        val previous = previousScore
        val wasBest = score.total > bestScore
        if (wasBest) bestScore = score.total

        val guidance = when {
            previous == null -> PlacementGuidance.INITIAL
            wasBest && score.total >= 85 -> PlacementGuidance.BEST_SO_FAR
            score.total - previous >= 7 -> PlacementGuidance.MUCH_BETTER
            score.total - previous >= 3 -> PlacementGuidance.BETTER
            previous - score.total >= 4 -> PlacementGuidance.WORSE
            else -> PlacementGuidance.STABLE
        }
        previousScore = score.total

        return PlacementReading(
            score = score,
            guidance = guidance,
            deltaFromBest = score.total - bestScore,
            bestScore = bestScore
        )
    }

    fun reset() {
        history.clear()
        bestScore = 0
        previousScore = null
    }

    private fun score(current: RouterSnapshot, samples: List<RouterSnapshot>): QualityScore {
        val rsrpScore = normalize(current.lteRsrp ?: current.nrRsrp, bad = -120.0, excellent = -72.0)
        val rsrqScore = normalize(current.lteRsrq, bad = -20.0, excellent = -6.0)
        val sinrScore = normalize(current.lteSinr ?: current.nrSinr, bad = -5.0, excellent = 28.0)

        val stability = calculateStability(samples)
        val total = (
            rsrpScore * 0.25 +
                sinrScore * 0.40 +
                rsrqScore * 0.20 +
                stability * 0.15
            ).toInt().coerceIn(0, 100)

        return QualityScore(
            total = total,
            signal = rsrpScore,
            cleanliness = sinrScore,
            quality = rsrqScore,
            stability = stability,
            label = when {
                total >= 90 -> "ممتاز جدًا"
                total >= 80 -> "ممتاز"
                total >= 68 -> "جيد جدًا"
                total >= 55 -> "جيد"
                total >= 40 -> "متوسط"
                else -> "ضعيف"
            }
        )
    }

    private fun calculateStability(samples: List<RouterSnapshot>): Int {
        if (samples.size < 3) return 75

        val rsrp = samples.mapNotNull { it.lteRsrp ?: it.nrRsrp }
        val sinr = samples.mapNotNull { it.lteSinr ?: it.nrSinr }
        if (rsrp.size < 3 && sinr.size < 3) return 60

        val rsrpDeviation = standardDeviation(rsrp)
        val sinrDeviation = standardDeviation(sinr)
        val penalty = (rsrpDeviation * 7.0 + sinrDeviation * 5.0).toInt()
        return (100 - penalty).coerceIn(0, 100)
    }

    private fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean).pow(2) } / values.size)
    }

    private fun normalize(value: Double?, bad: Double, excellent: Double): Int {
        if (value == null) return 45
        val ratio = ((value - bad) / (excellent - bad)).coerceIn(0.0, 1.0)
        return (ratio * 100).toInt()
    }
}
