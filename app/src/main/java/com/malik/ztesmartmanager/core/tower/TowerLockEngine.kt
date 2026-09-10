package com.malik.ztesmartmanager.core.tower

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRadioIdParser
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

data class TowerTarget(
    val pci: Int,
    val earfcn: Int,
    val band: String?,
    val cellId: Long?,
    val enodebId: String?
)

enum class TowerMatch {
    MATCHED,
    RADIO_MATCH_ID_CHANGED,
    DRIFTED,
    UNKNOWN
}

data class TowerGuardStatus(
    val target: TowerTarget,
    val match: TowerMatch,
    val consecutiveDriftSamples: Int,
    val repaired: Boolean,
    val message: String
)

data class NearbyCell(
    val rat: String,
    val band: String?,
    val pci: Int?,
    val arfcn: Int?,
    val rsrp: Double?,
    val rsrq: Double?,
    val sinr: Double?,
    val cellId: Long? = null,
    val areaCode: Int? = null,
    val samplesSeen: Int = 1,
    val samplesTotal: Int = 1,
    val presencePercent: Int = 100,
    val stabilityScore: Int? = null,
    val evidenceScore: Int? = null,
    val confidence: CellConfidence? = null
)

/**
 * Verified tower/cell control.
 *
 * A write acknowledgement is not proof of a lock. We require the router to read back the exact
 * lock keys before the app is allowed to say that a tower/cell is locked or enable Tower Guard.
 */
class TowerLockEngine(
    private val client: ZteRouterClient
) {
    private var consecutiveDriftSamples = 0
    private var lastRepairAtMs = 0L

    fun captureCurrent(snapshot: RouterSnapshot): TowerTarget? {
        val pci = snapshot.pci ?: return null
        val earfcn = snapshot.earfcn ?: return null
        val derivedEnodeb = derivedEnodebId(snapshot.cellId)
        return TowerTarget(
            pci = pci,
            earfcn = earfcn,
            band = snapshot.lteBand,
            cellId = snapshot.cellId,
            enodebId = derivedEnodeb
        )
    }

    fun compare(target: TowerTarget, snapshot: RouterSnapshot): TowerMatch {
        val pci = snapshot.pci
        val earfcn = snapshot.earfcn
        if (pci == null || earfcn == null) return TowerMatch.UNKNOWN
        if (pci != target.pci || earfcn != target.earfcn) return TowerMatch.DRIFTED

        // Cell ID is stronger identity evidence than an opaque enodeb_id field. If both sides expose
        // it, compare it directly. We no longer treat raw enodeb_id as numeric evidence because its
        // radix/format is firmware-specific and can otherwise create a false physical-tower claim.
        if (target.cellId != null && snapshot.cellId != null) {
            return if (target.cellId == snapshot.cellId) TowerMatch.MATCHED else TowerMatch.RADIO_MATCH_ID_CHANGED
        }

        val currentEnodeb = derivedEnodebId(snapshot.cellId)
        if (target.enodebId != null && currentEnodeb != null && target.enodebId != currentEnodeb) {
            return TowerMatch.RADIO_MATCH_ID_CHANGED
        }
        return TowerMatch.MATCHED
    }

    suspend fun lockCurrent(): TowerGuardStatus = lockCurrent(client.readSnapshot())

    suspend fun lockCurrent(snapshot: RouterSnapshot): TowerGuardStatus {
        val target = captureCurrent(snapshot)
            ?: error("لا توجد هوية LTE موثقة كافية لتثبيت البرج: نحتاج PCI و EARFCN")

        val operation = client.setCellLock(target.pci, target.earfcn)
        if (!operation.success) {
            return TowerGuardStatus(target, TowerMatch.UNKNOWN, 0, false, operation.message)
        }
        if (!operation.verified) {
            return TowerGuardStatus(
                target,
                TowerMatch.UNKNOWN,
                0,
                false,
                "قبل الراوتر أمر القفل، لكن لم يعطِ read-back مطابقًا؛ لذلك لن يعتبره التطبيق قفلًا مؤكدًا ولن يشغّل Tower Guard"
            )
        }

        delay(1_200)
        val after = runCatching { client.readSnapshot() }.getOrNull()
        val match = after?.let { compare(target, it) } ?: TowerMatch.UNKNOWN
        consecutiveDriftSamples = 0
        return TowerGuardStatus(
            target = target,
            match = match,
            consecutiveDriftSamples = 0,
            repaired = false,
            message = when (match) {
                TowerMatch.MATCHED -> "تم حفظ القفل وقراءته مرة أخرى، والخلية الحالية تطابق الهدف"
                TowerMatch.RADIO_MATCH_ID_CHANGED -> "القفل محفوظ، لكن Cell ID الموثق لا يطابق الهوية الأصلية؛ لن يدّعي التطبيق ثبات البرج الفيزيائي"
                TowerMatch.DRIFTED -> "القفل محفوظ في الراوتر لكن الخلية الحية لا تطابق الهدف"
                TowerMatch.UNKNOWN -> "القفل محفوظ، لكن بيانات الخلية الحية غير كافية للتحقق"
            }
        )
    }

    suspend fun guardOnce(target: TowerTarget, snapshot: RouterSnapshot): TowerGuardStatus {
        val match = compare(target, snapshot)
        if (match == TowerMatch.MATCHED) {
            consecutiveDriftSamples = 0
            return TowerGuardStatus(target, match, 0, false, "الخلية المستهدفة موثقة وثابتة")
        }
        if (match == TowerMatch.UNKNOWN) {
            return TowerGuardStatus(target, match, consecutiveDriftSamples, false, "تعذر التحقق مؤقتًا؛ لن يرسل التطبيق أمرًا عشوائيًا")
        }

        consecutiveDriftSamples++
        val now = System.currentTimeMillis()
        val cooldownDone = now - lastRepairAtMs >= REPAIR_COOLDOWN_MS
        if (consecutiveDriftSamples < DRIFT_SAMPLES_BEFORE_REPAIR || !cooldownDone) {
            return TowerGuardStatus(
                target,
                match,
                consecutiveDriftSamples,
                false,
                if (match == TowerMatch.RADIO_MATCH_ID_CHANGED) "تغيّرت هوية الخلية رغم تطابق PCI/EARFCN" else "اكتُشف انتقال عن الخلية المستهدفة"
            )
        }

        val operation = runCatching { client.setCellLock(target.pci, target.earfcn) }.getOrNull()
        lastRepairAtMs = now
        consecutiveDriftSamples = 0
        val repaired = operation?.success == true && operation.verified
        return TowerGuardStatus(
            target,
            match,
            0,
            repaired,
            when {
                repaired -> "أعاد Tower Guard القفل وتحقق من read-back"
                operation?.success == true -> "قبل الراوتر إعادة القفل لكن لم يؤكدها؛ لا تُحسب كإصلاح ناجح"
                else -> "تعذر على Tower Guard إعادة تطبيق القفل"
            }
        )
    }

    /**
     * Multi-sample read-only discovery. Five scans are used by default so a one-off neighbor does
     * not outrank a stable cell. The identity key is RAT + PCI + ARFCN, so cells sharing the same
     * frequency remain distinct when their PCI differs.
     */
    suspend fun scanNearbyCells(
        requestedSamples: Int = DEFAULT_SCAN_SAMPLES,
        intervalMs: Long = DEFAULT_SCAN_INTERVAL_MS
    ): TowerScanReport {
        val sampleCount = requestedSamples.coerceIn(MIN_SCAN_SAMPLES, MAX_SCAN_SAMPLES)
        val pause = intervalMs.coerceIn(MIN_SCAN_INTERVAL_MS, MAX_SCAN_INTERVAL_MS)
        val startedAt = System.currentTimeMillis()
        val samples = mutableListOf<List<NearbyCell>>()

        repeat(sampleCount) { index ->
            runCatching { readNearbyCellsOnce() }
                .onSuccess { samples.add(it) }
            if (index < sampleCount - 1) delay(pause)
        }

        return TowerScanReport(
            requestedSamples = sampleCount,
            successfulSamples = samples.size,
            rankedCells = TowerScanAggregator.rank(samples),
            elapsedMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        )
    }

    /** Existing callers now receive the evidence-ranked multi-sample result. */
    suspend fun readNearbyCells(): List<NearbyCell> = scanNearbyCells().cells

    private suspend fun readNearbyCellsOnce(): List<NearbyCell> {
        val raw = client.readRaw(
            setOf(
                "neighbor_cell_info",
                "current_cell_info",
                "locked_cell_info",
                "ngbr_cell_info",
                "lte_pci",
                "wan_active_channel",
                "wan_active_band",
                "lte_rsrp",
                "lte_rsrq",
                "lte_snr"
            )
        )

        return buildList {
            addAll(parseCellArray(raw.opt("current_cell_info")))
            addAll(parseCellArray(raw.opt("neighbor_cell_info")))
            addAll(parseLegacyNeighborCells(raw.optString("ngbr_cell_info")))

            val currentPci = parseZtePciToken(raw.optString("lte_pci"), 503)
            val currentArfcn = raw.optString("wan_active_channel").trim().toIntOrNull()?.takeIf { it > 0 }
            if (currentPci != null && currentArfcn != null) {
                add(
                    NearbyCell(
                        rat = "LTE",
                        band = normalizeBand(raw.optString("wan_active_band"), false),
                        pci = currentPci,
                        arfcn = currentArfcn,
                        rsrp = scaledSignal(raw.optString("lte_rsrp"), -170.0, -35.0),
                        rsrq = scaledSignal(raw.optString("lte_rsrq"), -40.0, 0.0),
                        sinr = scaledSignal(raw.optString("lte_snr"), -30.0, 60.0)
                    )
                )
            }
        }
            .filter { it.pci != null && it.arfcn != null }
            .distinctBy { Triple(it.rat, it.pci, it.arfcn) }
    }

    private fun parseLegacyNeighborCells(value: String?): List<NearbyCell> {
        if (value.isNullOrBlank()) return emptyList()
        return value.trimEnd(';')
            .split(';')
            .mapNotNull { row ->
                val fields = row.split(',').map { it.trim() }
                if (fields.size < 4) return@mapNotNull null
                val arfcn = fields[0].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                val pci = parseZtePciToken(fields[1], 503) ?: return@mapNotNull null
                NearbyCell(
                    rat = "LTE",
                    band = null,
                    pci = pci,
                    arfcn = arfcn,
                    rsrq = scaledSignal(fields[2], -40.0, 0.0),
                    rsrp = scaledSignal(fields[3], -170.0, -35.0),
                    sinr = null
                )
            }
    }

    private fun parseCellArray(value: Any?): List<NearbyCell> {
        val array = when (value) {
            is JSONArray -> value
            is String -> value.trim().takeIf { it.startsWith("[") }?.let { runCatching { JSONArray(it) }.getOrNull() }
            else -> null
        } ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val rat = item.optString("rat").trim()
                val rawBand = item.optString("band").trim().takeIf { it.isNotBlank() }
                val arfcn = positiveInt(item, "fcn") ?: positiveInt(item, "earfcn")
                val isNr = rat == "16" || (arfcn ?: 0) > 65_535 ||
                    rawBand?.startsWith("N", true) == true || rawBand?.startsWith("NR", true) == true
                val pci = parseZtePciToken(item.optString("pci"), if (isNr) 1007 else 503)
                add(
                    NearbyCell(
                        rat = if (isNr) "NR" else "LTE",
                        band = normalizeBand(rawBand, isNr),
                        pci = pci,
                        arfcn = arfcn,
                        rsrp = signal(item, "rsrp", -170.0, -35.0),
                        rsrq = signal(item, "rsrq", -40.0, 0.0),
                        sinr = signal(item, "sinr", -30.0, 60.0),
                        cellId = firstCellId(item),
                        areaCode = firstAreaCode(item)
                    )
                )
            }
        }
    }

    private fun firstCellId(item: JSONObject): Long? =
        listOf("cell_id", "cellid", "eci", "nci")
            .asSequence()
            .mapNotNull { key ->
                ZteRadioIdParser.parseLong(
                    item.optString(key),
                    encoding = client.profile.radioIdEncoding
                )
            }
            .firstOrNull { it > 0 }

    private fun firstAreaCode(item: JSONObject): Int? =
        listOf("tac", "lac", "area_code")
            .asSequence()
            .mapNotNull { key -> parseAreaCodeToken(item.optString(key)) }
            .firstOrNull()

    private fun parseAreaCodeToken(value: String?): Int? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val explicitHex = text.startsWith("0x", true) || text.any { it.lowercaseChar() in 'a'..'f' }
        val token = text.removePrefix("0x").removePrefix("0X")
        val parsed = if (explicitHex) token.toIntOrNull(16) else token.toIntOrNull(10)
        return parsed?.takeIf { it >= 0 }
    }

    private fun positiveInt(item: JSONObject, key: String): Int? =
        item.optString(key).trim().toIntOrNull()?.takeIf { it >= 0 }

    private fun signal(item: JSONObject, key: String, min: Double, max: Double): Double? =
        scaledSignal(item.optString(key), min, max)

    private fun scaledSignal(text: String?, min: Double, max: Double): Double? {
        var value = text
            ?.replace(Regex("[^0-9.\\-]"), "")
            ?.toDoubleOrNull()
            ?: return null
        repeat(3) {
            if (value in min..max) return value
            value /= 10.0
        }
        return value.takeIf { it in min..max }
    }

    private fun parseZtePciToken(value: String?, max: Int): Int? =
        ZteRadioIdParser.parseInt(value, max, client.profile.radioIdEncoding)

    private fun derivedEnodebId(cellId: Long?): String? =
        cellId?.takeIf { it > 0 }?.let { (it shr 8).toString() }

    private fun normalizeBand(raw: String?, nr: Boolean): String? {
        val number = Regex("\\d+").find(raw.orEmpty())?.value?.toIntOrNull() ?: return null
        return if (nr) "N$number" else "B$number"
    }

    companion object {
        private const val DRIFT_SAMPLES_BEFORE_REPAIR = 3
        private const val REPAIR_COOLDOWN_MS = 30_000L
        private const val DEFAULT_SCAN_SAMPLES = 5
        private const val MIN_SCAN_SAMPLES = 3
        private const val MAX_SCAN_SAMPLES = 8
        private const val DEFAULT_SCAN_INTERVAL_MS = 650L
        private const val MIN_SCAN_INTERVAL_MS = 250L
        private const val MAX_SCAN_INTERVAL_MS = 1_500L
    }
}
