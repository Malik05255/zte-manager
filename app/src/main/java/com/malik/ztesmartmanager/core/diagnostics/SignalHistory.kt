package com.malik.ztesmartmanager.core.diagnostics

import kotlin.math.abs

data class SignalHistoryPoint(
    val slot: Int,
    val timestampEpochMs: Long,
    val dbm: Double
)

enum class SignalTrend { IMPROVING, FLAT, DECLINING, INSUFFICIENT }

data class SignalSeries(
    val points: List<SignalHistoryPoint>,
    val trend: SignalTrend,
    val changeDb: Double?,
    val latestDbm: Double?
)

data class SignalHistoryModel(
    val sampleSlots: Int,
    val lte: SignalSeries,
    val nr: SignalSeries
) {
    val hasAnySignal: Boolean get() = lte.points.isNotEmpty() || nr.points.isNotEmpty()
}

object SignalHistoryPresenter {
    private const val MAX_SLOTS = 30
    private const val MIN_TREND_POINTS = 5
    private const val TREND_THRESHOLD_DB = 3.0

    fun from(samples: List<SafeTelemetrySample>): SignalHistoryModel {
        val window = samples.takeLast(MAX_SLOTS)
        val ltePoints = mutableListOf<SignalHistoryPoint>()
        val nrPoints = mutableListOf<SignalHistoryPoint>()

        window.forEachIndexed { slot, sample ->
            if (lteBearingMode(sample.networkType)) {
                sample.lteRsrp?.takeIf(::plausibleRsrp)?.let {
                    ltePoints += SignalHistoryPoint(slot, sample.timestampEpochMs, it)
                }
            }
            if (sample.nrVerified) {
                sample.nrRsrp?.takeIf(::plausibleRsrp)?.let {
                    nrPoints += SignalHistoryPoint(slot, sample.timestampEpochMs, it)
                }
            }
        }

        return SignalHistoryModel(
            sampleSlots = window.size,
            lte = series(ltePoints),
            nr = series(nrPoints)
        )
    }

    private fun lteBearingMode(networkType: String?): Boolean {
        val mode = networkType?.uppercase()?.trim().orEmpty()
        if (mode.isEmpty()) return false
        if ("SA" in mode && "NSA" !in mode) return false
        return "4G" in mode || "LTE" in mode || "NSA" in mode
    }

    private fun plausibleRsrp(value: Double): Boolean = value.isFinite() && value in -160.0..-30.0

    private fun series(points: List<SignalHistoryPoint>): SignalSeries {
        if (points.isEmpty()) return SignalSeries(emptyList(), SignalTrend.INSUFFICIENT, null, null)
        if (points.size < MIN_TREND_POINTS) {
            return SignalSeries(points, SignalTrend.INSUFFICIENT, null, points.last().dbm)
        }

        val edgeCount = minOf(3, points.size / 2)
        val start = median(points.take(edgeCount).map { it.dbm })
        val end = median(points.takeLast(edgeCount).map { it.dbm })
        val delta = end - start
        val trend = when {
            delta >= TREND_THRESHOLD_DB -> SignalTrend.IMPROVING
            delta <= -TREND_THRESHOLD_DB -> SignalTrend.DECLINING
            abs(delta) < TREND_THRESHOLD_DB -> SignalTrend.FLAT
            else -> SignalTrend.FLAT
        }
        return SignalSeries(points, trend, delta, points.last().dbm)
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
}
