package com.malik.ztesmartmanager.core.smart

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.BandEncoding
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class OptimizationGoal {
    BALANCED,
    SPEED,
    GAMING,
    STABILITY
}

data class CandidateEvaluation(
    val bands: Set<Int>,
    val score: Int,
    val qualityScore: Int,
    val performance: NetworkPerformance,
    val caActive: Boolean,
    val verified: Boolean
)

data class SmartOptimizationReport(
    val baseline: CandidateEvaluation,
    val best: CandidateEvaluation,
    val evaluations: List<CandidateEvaluation>,
    val changed: Boolean,
    val message: String
)

/**
 * Safe, bounded LTE optimizer.
 *
 * It measures a baseline, tries a small candidate set, verifies router writes, then keeps the
 * winner only when the improvement is meaningful. Otherwise it rolls back to the original band
 * mask. The candidate count is intentionally capped so Smart Mode does not become a disruptive
 * full spectrum scanner.
 */
class SmartBandOptimizer(
    private val client: ZteRouterClient,
    private val probe: NetworkPerformanceProbe = NetworkPerformanceProbe()
) {
    private val qualityEngine = NetworkQualityEngine()

    suspend fun optimizeOnce(
        goal: OptimizationGoal = OptimizationGoal.BALANCED,
        onProgress: (String) -> Unit = {}
    ): SmartOptimizationReport {
        val supported = client.profile.capabilities.supportedLteBands
        require(client.profile.capabilities.supportsLteBandLock) { "هذا الراوتر لا يعلن دعم Band Lock" }

        onProgress("قياس الوضع الحالي...")
        val initialSnapshot = client.readSnapshot()
        val originalBands = readConfiguredBands(initialSnapshot, supported)
        val baselinePerformance = probe.measure(includeDownload = true)
        val baseline = evaluate(originalBands, initialSnapshot, baselinePerformance, goal, verified = true)

        val candidates = buildCandidates(initialSnapshot, originalBands, supported)
            .filter { it.isNotEmpty() && it != originalBands }
            .take(MAX_CANDIDATES)

        val evaluations = mutableListOf(baseline)
        var best = baseline

        for ((index, bands) in candidates.withIndex()) {
            onProgress("اختبار ${index + 1}/${candidates.size}: ${formatBands(bands)}")
            val operation = runCatching { client.setLteBands(bands) }.getOrNull() ?: continue
            if (!operation.success) continue

            delay(SETTLE_MS)
            val snapshot = runCatching { client.readSnapshot() }.getOrNull() ?: continue
            val performance = runCatching { probe.measure(includeDownload = true) }
                .getOrElse { NetworkPerformance(null, null, 100.0, null) }
            val evaluation = evaluate(
                bands = bands,
                snapshot = snapshot,
                performance = performance,
                goal = goal,
                verified = operation.verified
            )
            evaluations += evaluation
            if (evaluation.score > best.score) best = evaluation
        }

        val improvement = best.score - baseline.score
        val shouldKeep = best.bands != originalBands && improvement >= MIN_IMPROVEMENT

        if (shouldKeep) {
            onProgress("تثبيت أفضل نتيجة ${formatBands(best.bands)}...")
            val finalWrite = client.setLteBands(best.bands)
            if (!finalWrite.success) {
                restore(originalBands, supported)
                return SmartOptimizationReport(
                    baseline = baseline,
                    best = baseline,
                    evaluations = evaluations,
                    changed = false,
                    message = "تعذر تثبيت أفضل نتيجة؛ تمت استعادة الإعداد السابق"
                )
            }
            delay(2_000)
            return SmartOptimizationReport(
                baseline = baseline,
                best = best.copy(verified = best.verified || finalWrite.verified),
                evaluations = evaluations,
                changed = true,
                message = "تم اختيار ${formatBands(best.bands)} بتحسن $improvement نقطة"
            )
        }

        onProgress("لا يوجد تحسن كافٍ — استعادة الإعداد السابق...")
        restore(originalBands, supported)
        return SmartOptimizationReport(
            baseline = baseline,
            best = baseline,
            evaluations = evaluations,
            changed = false,
            message = if (evaluations.size <= 1) {
                "لم تتوفر بدائل قابلة للاختبار بأمان"
            } else {
                "لم يظهر تحسن موثوق؛ تم الحفاظ على الإعداد السابق"
            }
        )
    }

    private suspend fun restore(originalBands: Set<Int>, supported: Set<Int>) {
        val restoreBands = originalBands.ifEmpty { supported }
        if (restoreBands.isNotEmpty()) runCatching { client.setLteBands(restoreBands) }
    }

    private suspend fun readConfiguredBands(snapshot: RouterSnapshot, supported: Set<Int>): Set<Int> {
        val raw = runCatching { client.readRaw(setOf("lte_band_lock", "wan_active_band")) }.getOrNull()
        val mask = raw?.optString("lte_band_lock").orEmpty()
        val decoded = BandEncoding.decodeLteMask(mask, supported)
        if (decoded.isNotEmpty()) return decoded
        return activeBands(snapshot).ifEmpty {
            extractBand(snapshot.lteBand)?.let(::setOf).orEmpty()
        }
    }

    private fun buildCandidates(
        snapshot: RouterSnapshot,
        original: Set<Int>,
        supported: Set<Int>
    ): List<Set<Int>> {
        val result = linkedSetOf<Set<Int>>()
        val active = activeBands(snapshot).filter { it in supported }.toSet()
        val primary = extractBand(snapshot.lteBand)?.takeIf { it in supported }

        if (original.isNotEmpty()) result += original
        if (active.isNotEmpty()) result += active
        active.sorted().forEach { result += setOf(it) }

        val activeList = active.sorted()
        for (i in activeList.indices) {
            for (j in i + 1 until activeList.size) {
                result += setOf(activeList[i], activeList[j])
            }
        }

        val discovery = DISCOVERY_PRIORITY.filter { it in supported }
        discovery.take(6).forEach { band -> result += setOf(band) }
        primary?.let { p ->
            discovery.filter { it != p }.take(4).forEach { band -> result += setOf(p, band) }
        }

        COMMON_CA.forEach { combo ->
            if (supported.containsAll(combo)) result += combo
        }
        return result.toList()
    }

    private fun activeBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
        extractBand(snapshot.lteBand)?.let(::add)
        snapshot.cells.forEach { cell -> extractBand(cell.band)?.let(::add) }
    }

    private fun evaluate(
        bands: Set<Int>,
        snapshot: RouterSnapshot,
        performance: NetworkPerformance,
        goal: OptimizationGoal,
        verified: Boolean
    ): CandidateEvaluation {
        val quality = qualityEngine.score(snapshot).total
        val score = performanceScore(performance, quality, snapshot.caActive, goal)
        return CandidateEvaluation(
            bands = bands,
            score = score,
            qualityScore = quality,
            performance = performance,
            caActive = snapshot.caActive,
            verified = verified
        )
    }

    private fun performanceScore(
        performance: NetworkPerformance,
        quality: Int,
        caActive: Boolean,
        goal: OptimizationGoal
    ): Int {
        val speed = performance.downloadMbps?.let { normalize(it, 2.0, 300.0) }
        val latency = performance.latencyMs?.let { inverseNormalize(it, 15.0, 180.0) }
        val jitter = performance.jitterMs?.let { inverseNormalize(it, 2.0, 70.0) }
        val loss = performance.packetLossPercent?.let { inverseNormalize(it, 0.0, 12.0) }

        val weights = when (goal) {
            OptimizationGoal.BALANCED -> mapOf("speed" to .40, "latency" to .18, "jitter" to .08, "loss" to .09, "quality" to .25)
            OptimizationGoal.SPEED -> mapOf("speed" to .64, "latency" to .10, "jitter" to .04, "loss" to .07, "quality" to .15)
            OptimizationGoal.GAMING -> mapOf("speed" to .12, "latency" to .34, "jitter" to .23, "loss" to .21, "quality" to .10)
            OptimizationGoal.STABILITY -> mapOf("speed" to .12, "latency" to .16, "jitter" to .22, "loss" to .25, "quality" to .25)
        }

        val values = mapOf(
            "speed" to speed,
            "latency" to latency,
            "jitter" to jitter,
            "loss" to loss,
            "quality" to quality
        )
        var sum = 0.0
        var weight = 0.0
        weights.forEach { (key, w) ->
            val value = values[key] ?: return@forEach
            sum += value * w
            weight += w
        }
        if (weight <= 0.0) return quality
        val caBonus = if (caActive) 3 else 0
        return ((sum / weight).roundToInt() + caBonus).coerceIn(0, 100)
    }

    private fun normalize(value: Double, bad: Double, excellent: Double): Int =
        (((value - bad) / (excellent - bad)).coerceIn(0.0, 1.0) * 100).roundToInt()

    private fun inverseNormalize(value: Double, excellent: Double, bad: Double): Int =
        (100 - normalize(value, excellent, bad)).coerceIn(0, 100)

    private fun extractBand(text: String?): Int? = Regex("\\d+").find(text.orEmpty())?.value?.toIntOrNull()

    private fun formatBands(bands: Set<Int>): String = bands.sorted().joinToString("+") { "B$it" }

    companion object {
        private const val MAX_CANDIDATES = 8
        private const val SETTLE_MS = 3_500L
        private const val MIN_IMPROVEMENT = 6

        private val DISCOVERY_PRIORITY = listOf(3, 1, 7, 28, 8, 40, 41, 20, 38)
        private val COMMON_CA = listOf(
            setOf(1, 3),
            setOf(3, 7),
            setOf(3, 28),
            setOf(1, 3, 28),
            setOf(1, 3, 7),
            setOf(3, 40),
            setOf(3, 41)
        )
    }
}
