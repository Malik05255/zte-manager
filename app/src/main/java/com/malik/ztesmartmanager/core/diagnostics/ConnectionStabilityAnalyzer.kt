package com.malik.ztesmartmanager.core.diagnostics

import kotlin.math.abs
import kotlin.math.roundToInt

enum class ConnectionStabilityLevel {
    INSUFFICIENT,
    STABLE,
    VARIABLE,
    UNSTABLE
}

data class ConnectionStabilityReport(
    val sampleCount: Int,
    val level: ConnectionStabilityLevel,
    val score: Int?,
    val cellStabilityPercent: Int?,
    val modeStabilityPercent: Int?,
    val signalStabilityPercent: Int?,
    val nrActivePercent: Int?,
    val caActivePercent: Int?,
    val cellSwitches: Int,
    val modeSwitches: Int,
    val summary: String
)

object ConnectionStabilityAnalyzer {
    fun analyze(samples: List<SafeTelemetrySample>): ConnectionStabilityReport {
        if (samples.size < MIN_SAMPLES) {
            return ConnectionStabilityReport(
                sampleCount = samples.size,
                level = ConnectionStabilityLevel.INSUFFICIENT,
                score = null,
                cellStabilityPercent = null,
                modeStabilityPercent = null,
                signalStabilityPercent = null,
                nrActivePercent = null,
                caActivePercent = null,
                cellSwitches = 0,
                modeSwitches = 0,
                summary = "نحتاج $MIN_SAMPLES قراءات على الأقل قبل الحكم على الثبات"
            )
        }

        val cellIds = samples.mapNotNull { sample ->
            val pci = sample.pci
            val earfcn = sample.earfcn
            if (pci != null && earfcn != null) "$pci:$earfcn" else null
        }
        val modes = samples.mapNotNull { it.networkType?.trim()?.takeIf(String::isNotEmpty) }

        val cellStability = dominantPercent(cellIds, minimumEvidence = 3)
        val modeStability = dominantPercent(modes, minimumEvidence = 3)
        val cellSwitches = countSwitches(samples.map { sample ->
            val pci = sample.pci
            val earfcn = sample.earfcn
            if (pci != null && earfcn != null) "$pci:$earfcn" else null
        })
        val modeSwitches = countSwitches(samples.map { it.networkType?.trim()?.takeIf(String::isNotEmpty) })

        val lteSignal = robustSignalStability(samples.mapNotNull { it.lteRsrp })
        val nrSignal = robustSignalStability(samples.mapNotNull { it.nrRsrp })
        val signalStability = listOfNotNull(lteSignal, nrSignal).takeIf { it.isNotEmpty() }?.average()?.roundToInt()

        val nrActivePercent = percent(samples.count { it.nrVerified }, samples.size)
        val caEvidence = samples.filter { it.caVerified }
        val caActivePercent = caEvidence.takeIf { it.size >= 3 }
            ?.let { percent(it.count(SafeTelemetrySample::caActive), it.size) }

        val dimensions = buildList {
            cellStability?.let { add(it to 0.40) }
            modeStability?.let { add(it to 0.35) }
            signalStability?.let { add(it to 0.25) }
        }
        val score = if (dimensions.isEmpty()) null else {
            val totalWeight = dimensions.sumOf { it.second }
            (dimensions.sumOf { it.first * it.second } / totalWeight).roundToInt().coerceIn(0, 100)
        }

        val level = when {
            score == null -> ConnectionStabilityLevel.INSUFFICIENT
            score >= 85 -> ConnectionStabilityLevel.STABLE
            score >= 65 -> ConnectionStabilityLevel.VARIABLE
            else -> ConnectionStabilityLevel.UNSTABLE
        }

        return ConnectionStabilityReport(
            sampleCount = samples.size,
            level = level,
            score = score,
            cellStabilityPercent = cellStability,
            modeStabilityPercent = modeStability,
            signalStabilityPercent = signalStability,
            nrActivePercent = nrActivePercent,
            caActivePercent = caActivePercent,
            cellSwitches = cellSwitches,
            modeSwitches = modeSwitches,
            summary = summary(level, score, cellSwitches, modeSwitches)
        )
    }

    private fun dominantPercent(values: List<String>, minimumEvidence: Int): Int? {
        if (values.size < minimumEvidence) return null
        val dominant = values.groupingBy { it }.eachCount().maxOfOrNull { it.value } ?: return null
        return percent(dominant, values.size)
    }

    private fun countSwitches(values: List<String?>): Int {
        var previous: String? = null
        var switches = 0
        values.forEach { current ->
            if (current == null) return@forEach
            if (previous != null && previous != current) switches++
            previous = current
        }
        return switches
    }

    private fun robustSignalStability(values: List<Double>): Int? {
        if (values.size < 3) return null
        val median = median(values)
        val mad = median(values.map { abs(it - median) })
        return when {
            mad <= 1.0 -> 100
            mad <= 2.0 -> 90
            mad <= 3.5 -> 80
            mad <= 5.0 -> 68
            mad <= 7.0 -> 52
            mad <= 10.0 -> 35
            else -> 15
        }
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2.0 else sorted[middle]
    }

    private fun percent(part: Int, total: Int): Int =
        if (total <= 0) 0 else (part * 100.0 / total).roundToInt().coerceIn(0, 100)

    private fun summary(
        level: ConnectionStabilityLevel,
        score: Int?,
        cellSwitches: Int,
        modeSwitches: Int
    ): String = when (level) {
        ConnectionStabilityLevel.INSUFFICIENT -> "لا توجد أدلة زمنية كافية للحكم"
        ConnectionStabilityLevel.STABLE -> "ثبات قوي عبر عدة قراءات${score?.let { " • $it/100" }.orEmpty()}"
        ConnectionStabilityLevel.VARIABLE -> "يوجد تذبذب ملحوظ • تبدل خلية $cellSwitches • تبدل وضع $modeSwitches"
        ConnectionStabilityLevel.UNSTABLE -> "الاتصال غير مستقر عبر القراءات • تبدل خلية $cellSwitches • تبدل وضع $modeSwitches"
    }

    private const val MIN_SAMPLES = 5
}
