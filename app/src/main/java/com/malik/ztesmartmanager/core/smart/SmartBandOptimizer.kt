package com.malik.ztesmartmanager.core.smart

import com.malik.ztesmartmanager.core.model.CellRole
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
    val verified: Boolean,
    val observedBands: Set<Int> = emptySet(),
    val sampleCount: Int = 0
)

data class SmartOptimizationReport(
    val baseline: CandidateEvaluation,
    val best: CandidateEvaluation,
    val evaluations: List<CandidateEvaluation>,
    val changed: Boolean,
    val message: String
)

/**
 * Verified, bounded LTE optimizer.
 *
 * Truth-first rules:
 * - configured bands are never treated as active carriers;
 * - the original band mask must be read back exactly before any mutation is allowed;
 * - a candidate is eligible only after exact band-mask read-back;
 * - RF quality is derived from several post-settle samples, not one transient snapshot;
 * - CA is counted only when the parser has verified CA live and at least two LTE carriers exist;
 * - the winner is committed only after a second verified write, otherwise original state is restored.
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

        onProgress("التحقق من الإعداد الأصلي قبل أي تغيير...")
        val originalBands = readConfiguredBands(supported)
            ?: error("لن يبدأ التحسين: الـFirmware لم يعطِ lte_band_lock موثوقًا يمكن استعادته بأمان")

        onProgress("قياس خط الأساس من عدة قراءات...")
        val first = client.readSnapshot()
        val baselineSamples = collectRadioSamples(first)
        require(baselineSamples.isNotEmpty()) { "تعذر جمع قراءات راديو موثوقة؛ لم يتم تغيير أي إعداد" }

        val baselinePerformance = safePerformanceProbe()
        val baseline = evaluate(
            bands = originalBands,
            samples = baselineSamples,
            performance = baselinePerformance,
            goal = goal,
            writeVerified = true
        )
        require(baseline.verified) { "خط الأساس لا يطابق الإعداد المقروء بثقة؛ لم يتم تغيير أي إعداد" }

        val candidates = buildCandidates(first, originalBands, supported)
            .filter { it.isNotEmpty() && it != originalBands }
            .take(MAX_CANDIDATES)

        val evaluations = mutableListOf(baseline)
        var best = baseline

        for ((index, bands) in candidates.withIndex()) {
            onProgress("اختبار ${index + 1}/${candidates.size}: السماح بـ ${formatBands(bands)}")
            val operation = runCatching { client.setLteBands(bands) }.getOrNull() ?: continue

            // A HTTP success is not proof. Exact requested mask read-back is mandatory.
            if (!operation.success || !operation.verified) {
                restoreExact(originalBands)
                continue
            }

            delay(SETTLE_MS)
            val samples = collectRadioSamples()
            if (samples.isEmpty()) {
                restoreExact(originalBands)
                continue
            }

            val observed = samples.flatMap { activeLteBands(it) }.toSet()
            val observedConsistent = observed.isNotEmpty() && observed.all { it in bands }
            if (!observedConsistent) {
                restoreExact(originalBands)
                continue
            }

            val performance = safePerformanceProbe()
            val evaluation = evaluate(
                bands = bands,
                samples = samples,
                performance = performance,
                goal = goal,
                writeVerified = true
            )
            evaluations += evaluation
            if (evaluation.verified && evaluation.score > best.score) best = evaluation
        }

        val improvement = best.score - baseline.score
        val shouldKeep = best.verified && best.bands != originalBands && improvement >= MIN_IMPROVEMENT

        if (shouldKeep) {
            onProgress("تطبيق أفضل إعداد والتحقق النهائي...")
            val finalWrite = runCatching { client.setLteBands(best.bands) }.getOrNull()
            if (finalWrite?.success != true || !finalWrite.verified) {
                restoreExact(originalBands)
                return SmartOptimizationReport(
                    baseline,
                    baseline,
                    evaluations,
                    false,
                    "لم ينجح التحقق النهائي من إعداد الفائز؛ تمت استعادة الإعداد الأصلي الموثق"
                )
            }

            delay(FINAL_VERIFY_SETTLE_MS)
            val finalSamples = collectRadioSamples()
            val finalObserved = finalSamples.flatMap { activeLteBands(it) }.toSet()
            val finalConsistent = finalSamples.isNotEmpty() &&
                finalObserved.isNotEmpty() && finalObserved.all { it in best.bands }
            if (!finalConsistent) {
                restoreExact(originalBands)
                return SmartOptimizationReport(
                    baseline,
                    baseline,
                    evaluations,
                    false,
                    "القناع محفوظ لكن الترددات الحية لم تطابق الإعداد؛ تمت الاستعادة بدل ادعاء نجاح غير مؤكد"
                )
            }

            return SmartOptimizationReport(
                baseline = baseline,
                best = best.copy(observedBands = finalObserved, sampleCount = finalSamples.size, verified = true),
                evaluations = evaluations,
                changed = true,
                message = "تم اعتماد ${formatBands(best.bands)} بعد read-back وقياسات حية متعددة؛ التحسن $improvement نقطة"
            )
        }

        onProgress("لا يوجد تحسن موثوق — استعادة الإعداد الأصلي...")
        val restored = restoreExact(originalBands)
        return SmartOptimizationReport(
            baseline = baseline,
            best = baseline,
            evaluations = evaluations,
            changed = false,
            message = when {
                !restored -> "لم يوجد تحسن موثوق، وتعذر التحقق من الاستعادة؛ راجع إعداد Band Lock في الراوتر"
                evaluations.size <= 1 -> "لم توجد بدائل اجتازت التحقق الصارم؛ الإعداد الأصلي مستعاد ومتحقق"
                else -> "لا يوجد تحسن موثوق كافٍ؛ تم استعادة الإعداد الأصلي والتحقق منه"
            }
        )
    }

    private suspend fun collectRadioSamples(first: RouterSnapshot? = null): List<RouterSnapshot> {
        val samples = mutableListOf<RouterSnapshot>()
        first?.let(samples::add)
        var attempts = if (first == null) 0 else 1

        while (samples.size < RADIO_SAMPLES && attempts < MAX_SAMPLE_ATTEMPTS) {
            attempts++
            val snapshot = runCatching { client.readSnapshot() }.getOrNull()
            if (snapshot != null) samples += snapshot
            if (samples.size < RADIO_SAMPLES && attempts < MAX_SAMPLE_ATTEMPTS) delay(SAMPLE_INTERVAL_MS)
        }
        return samples
    }

    private suspend fun safePerformanceProbe(): NetworkPerformance =
        runCatching { probe.measure(includeDownload = true) }
            .getOrElse { NetworkPerformance(null, null, null, null) }

    private suspend fun restoreExact(originalBands: Set<Int>): Boolean {
        if (originalBands.isEmpty()) return false
        val result = runCatching { client.setLteBands(originalBands) }.getOrNull()
        return result?.success == true && result.verified
    }

    private suspend fun readConfiguredBands(supported: Set<Int>): Set<Int>? {
        val raw = runCatching { client.readRaw(setOf("lte_band_lock")) }.getOrNull() ?: return null
        val mask = raw.optString("lte_band_lock").trim()
        if (mask.isBlank()) return null
        return BandEncoding.decodeLteMask(mask, supported).takeIf { it.isNotEmpty() }
    }

    private fun buildCandidates(
        snapshot: RouterSnapshot,
        original: Set<Int>,
        supported: Set<Int>
    ): List<Set<Int>> {
        val result = linkedSetOf<Set<Int>>()
        val active = activeLteBands(snapshot).filter { it in supported }.toSet()
        val primary = extractBand(snapshot.lteBand)?.takeIf { it in supported }

        result += original
        if (active.isNotEmpty()) result += active
        active.sorted().forEach { result += setOf(it) }

        val activeList = active.sorted()
        for (i in activeList.indices) {
            for (j in i + 1 until activeList.size) result += setOf(activeList[i], activeList[j])
        }

        val discovery = DISCOVERY_PRIORITY.filter { it in supported }
        discovery.take(6).forEach { result += setOf(it) }
        primary?.let { p -> discovery.filter { it != p }.take(4).forEach { result += setOf(p, it) } }
        COMMON_CA.forEach { combo -> if (supported.containsAll(combo)) result += combo }
        return result.toList()
    }

    private fun activeLteBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
        extractBand(snapshot.lteBand)?.let(::add)
        snapshot.cells.filter { it.role != CellRole.NR }.forEach { cell -> extractBand(cell.band)?.let(::add) }
    }

    private fun actualLteCarrierCount(snapshot: RouterSnapshot): Int = snapshot.cells
        .filter { it.role != CellRole.NR }
        .distinctBy { Triple(it.band, it.pci, it.arfcn) }
        .size

    private fun evaluate(
        bands: Set<Int>,
        samples: List<RouterSnapshot>,
        performance: NetworkPerformance,
        goal: OptimizationGoal,
        writeVerified: Boolean
    ): CandidateEvaluation {
        val quality = qualityEngine.scoreSamples(samples).total
        val observed = samples.flatMap { activeLteBands(it) }.toSet()
        val observedConsistent = observed.isNotEmpty() && observed.all { it in bands }
        val caConfirmedSamples = samples.count { it.caActive && actualLteCarrierCount(it) >= 2 }
        val caRequired = minOf(REQUIRED_CA_SAMPLES, samples.size).coerceAtLeast(1)
        val caActive = samples.isNotEmpty() && caConfirmedSamples >= caRequired
        val verified = writeVerified && observedConsistent && quality > 0
        val score = if (verified) performanceScore(performance, quality, caActive, goal) else 0

        return CandidateEvaluation(
            bands = bands,
            score = score,
            qualityScore = quality,
            performance = performance,
            caActive = caActive,
            verified = verified,
            observedBands = observed,
            sampleCount = samples.size
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

        val caBonus = if (caActive) 2 else 0
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
        private const val SETTLE_MS = 6_000L
        private const val FINAL_VERIFY_SETTLE_MS = 3_000L
        private const val RADIO_SAMPLES = 4
        private const val MAX_SAMPLE_ATTEMPTS = 8
        private const val SAMPLE_INTERVAL_MS = 750L
        private const val REQUIRED_CA_SAMPLES = 2
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
