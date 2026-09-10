package com.malik.ztesmartmanager.core.tower

import kotlin.math.abs
import kotlin.math.roundToInt

enum class CellConfidence {
    HIGH,
    MEDIUM,
    LOW
}

data class RankedNearbyCell(
    val cell: NearbyCell,
    val seenSamples: Int,
    val successfulSamples: Int,
    val presencePercent: Int,
    val medianRsrp: Double?,
    val medianRsrq: Double?,
    val medianSinr: Double?,
    val rsrpSpreadDb: Double?,
    val stabilityScore: Int?,
    val evidenceScore: Int?,
    val confidence: CellConfidence
)

data class TowerScanReport(
    val requestedSamples: Int,
    val successfulSamples: Int,
    val rankedCells: List<RankedNearbyCell>,
    val elapsedMs: Long
) {
    val cells: List<NearbyCell>
        get() = rankedCells.map { ranked ->
            ranked.cell.copy(
                samplesSeen = ranked.seenSamples,
                samplesTotal = ranked.successfulSamples,
                presencePercent = ranked.presencePercent,
                stabilityScore = ranked.stabilityScore,
                evidenceScore = ranked.evidenceScore,
                confidence = ranked.confidence
            )
        }

    val message: String
        get() = when {
            successfulSamples == 0 -> "تعذر الحصول على أي قراءة خلية موثوقة"
            rankedCells.isEmpty() -> "اكتملت $successfulSamples قراءات، لكن الـFirmware لم يعرض خلايا قابلة للتعريف بـ PCI + ARFCN"
            successfulSamples < requestedSamples -> "تم تحليل $successfulSamples من $requestedSamples قراءات ناجحة ورصد ${rankedCells.size} خلية حقيقية"
            else -> "تم تحليل $successfulSamples قراءات فعلية ورصد ${rankedCells.size} خلية حقيقية مرتبة حسب الدليل"
        }
}

/**
 * Pure, deterministic evidence aggregator for repeated router cell scans.
 * Identity is RAT + PCI + ARFCN; same frequency with a different PCI remains a separate cell.
 * Missing RF metrics are never replaced with synthetic middle values.
 */
object TowerScanAggregator {
    fun rank(samples: List<List<NearbyCell>>): List<RankedNearbyCell> {
        if (samples.isEmpty()) return emptyList()
        val sampleCount = samples.size
        val grouped = linkedMapOf<CellKey, MutableList<NearbyCell>>()

        samples.forEach { sample ->
            sample
                .filter { it.pci != null && it.arfcn != null }
                .distinctBy { CellKey(it.rat, it.pci!!, it.arfcn!!) }
                .forEach { cell ->
                    grouped.getOrPut(CellKey(cell.rat, cell.pci!!, cell.arfcn!!)) { mutableListOf() } += cell
                }
        }

        return grouped.map { (key, observations) ->
            val rsrps = observations.mapNotNull { it.rsrp }
            val rsrqs = observations.mapNotNull { it.rsrq }
            val sinrs = observations.mapNotNull { it.sinr }
            val rsrp = median(rsrps)
            val rsrq = median(rsrqs)
            val sinr = median(sinrs)
            val spread = rsrp?.let { center -> rsrps.takeIf { it.size >= 2 }?.map { abs(it - center) }?.average() }
            val stability = spread?.let { (100.0 - it * 12.0).coerceIn(0.0, 100.0).roundToInt() }
            val presence = ((observations.size * 100.0) / sampleCount).roundToInt().coerceIn(0, 100)
            val evidence = score(rsrp, rsrq, sinr, presence, stability)
            val confidence = when {
                observations.size >= 4 && presence >= 75 && rsrp != null -> CellConfidence.HIGH
                observations.size >= 2 && presence >= 40 && (rsrp != null || rsrq != null || sinr != null) -> CellConfidence.MEDIUM
                else -> CellConfidence.LOW
            }
            val band = observations.mapNotNull { it.band }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

            RankedNearbyCell(
                cell = NearbyCell(
                    rat = key.rat,
                    band = band,
                    pci = key.pci,
                    arfcn = key.arfcn,
                    rsrp = rsrp,
                    rsrq = rsrq,
                    sinr = sinr
                ),
                seenSamples = observations.size,
                successfulSamples = sampleCount,
                presencePercent = presence,
                medianRsrp = rsrp,
                medianRsrq = rsrq,
                medianSinr = sinr,
                rsrpSpreadDb = spread?.let(::round1),
                stabilityScore = stability,
                evidenceScore = evidence,
                confidence = confidence
            )
        }.sortedWith(
            compareByDescending<RankedNearbyCell> { it.evidenceScore ?: -1 }
                .thenByDescending { it.presencePercent }
                .thenByDescending { it.medianRsrp ?: -999.0 }
                .thenBy { it.cell.arfcn }
                .thenBy { it.cell.pci }
        )
    }

    private data class CellKey(val rat: String, val pci: Int, val arfcn: Int)

    private fun score(
        rsrp: Double?,
        rsrq: Double?,
        sinr: Double?,
        presence: Int,
        stability: Int?
    ): Int? {
        val rf = mutableListOf<Pair<Double, Double>>()
        rsrp?.let { rf += normalize(it, -130.0, -75.0) to 0.55 }
        rsrq?.let { rf += normalize(it, -20.0, -7.0) to 0.20 }
        sinr?.let { rf += normalize(it, -5.0, 25.0) to 0.25 }
        if (rf.isEmpty()) return null

        val rfWeight = rf.sumOf { it.second }
        val rfScore = rf.sumOf { it.first * it.second } / rfWeight
        val components = mutableListOf(rfScore to 0.72, presence.toDouble() to 0.18)
        stability?.let { components += it.toDouble() to 0.10 }
        val totalWeight = components.sumOf { it.second }
        return (components.sumOf { it.first * it.second } / totalWeight)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun normalize(value: Double, bad: Double, good: Double): Double =
        (((value - bad) / (good - bad)) * 100.0).coerceIn(0.0, 100.0)

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    private fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0
}
