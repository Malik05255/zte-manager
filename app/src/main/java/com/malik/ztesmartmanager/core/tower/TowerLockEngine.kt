package com.malik.ztesmartmanager.core.tower

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

/**
 * Physical-tower oriented identity for LTE.
 *
 * ZTE's verified legacy MC801A write command accepts PCI + EARFCN. Cell ID/eNodeB are therefore
 * used as identity/verification evidence, not falsely claimed as writable lock keys.
 */
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
    val sinr: Double?
)

/**
 * Tower lock policy learned from mature CPE tools:
 * 1) identify the current serving cell,
 * 2) apply only a command verified for this profile,
 * 3) read back and verify,
 * 4) monitor for drift,
 * 5) re-assert only after repeated drift and with a cooldown (no ping-pong).
 */
class TowerLockEngine(
    private val client: ZteRouterClient
) {
    private var consecutiveDriftSamples = 0
    private var lastRepairAtMs = 0L

    fun captureCurrent(snapshot: RouterSnapshot): TowerTarget? {
        val pci = snapshot.pci ?: return null
        val earfcn = snapshot.earfcn ?: return null
        return TowerTarget(
            pci = pci,
            earfcn = earfcn,
            band = snapshot.lteBand,
            cellId = snapshot.cellId,
            enodebId = snapshot.raw["enodeb_id"]?.trim()?.takeIf { it.isNotBlank() && it != "--" }
        )
    }

    fun compare(target: TowerTarget, snapshot: RouterSnapshot): TowerMatch {
        val pci = snapshot.pci
        val earfcn = snapshot.earfcn
        if (pci == null || earfcn == null) return TowerMatch.UNKNOWN
        if (pci != target.pci || earfcn != target.earfcn) return TowerMatch.DRIFTED

        val targetCellId = target.cellId
        val currentCellId = snapshot.cellId
        if (targetCellId != null && currentCellId != null && targetCellId != currentCellId) {
            return TowerMatch.RADIO_MATCH_ID_CHANGED
        }

        val targetEnodeb = target.enodebId
        val currentEnodeb = snapshot.raw["enodeb_id"]?.trim()?.takeIf { it.isNotBlank() && it != "--" }
        if (targetEnodeb != null && currentEnodeb != null && targetEnodeb != currentEnodeb) {
            return TowerMatch.RADIO_MATCH_ID_CHANGED
        }
        return TowerMatch.MATCHED
    }

    suspend fun lockCurrent(snapshot: RouterSnapshot = client.readSnapshot()): TowerGuardStatus {
        val target = captureCurrent(snapshot)
            ?: error("لا توجد هوية LTE كافية لتثبيت البرج: نحتاج PCI و EARFCN")

        val operation = client.setCellLock(target.pci, target.earfcn)
        if (!operation.success) {
            return TowerGuardStatus(
                target = target,
                match = TowerMatch.UNKNOWN,
                consecutiveDriftSamples = 0,
                repaired = false,
                message = operation.message
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
                TowerMatch.MATCHED -> "تم تثبيت الخلية والتحقق من بقاء الراوتر على البرج المختار"
                TowerMatch.RADIO_MATCH_ID_CHANGED -> "تم تثبيت PCI/EARFCN، لكن Cell ID/eNodeB تغيّر؛ لا يمكن ضمان البرج الفيزيائي بهذا الـFirmware"
                TowerMatch.DRIFTED -> "قبل الراوتر أمر القفل لكنه لم يبقَ على الخلية المطلوبة"
                TowerMatch.UNKNOWN -> "قبل الراوتر الأمر، لكن لا توجد قراءة كافية للتحقق من البرج"
            }
        )
    }

    /**
     * One Tower Guard iteration. Call from the app polling loop while guard is enabled.
     * Repair is deliberately bounded: three consecutive drift samples + 30 s cooldown.
     */
    suspend fun guardOnce(target: TowerTarget, snapshot: RouterSnapshot): TowerGuardStatus {
        val match = compare(target, snapshot)
        if (match == TowerMatch.MATCHED) {
            consecutiveDriftSamples = 0
            return TowerGuardStatus(target, match, 0, false, "البرج ثابت")
        }

        if (match == TowerMatch.UNKNOWN) {
            return TowerGuardStatus(target, match, consecutiveDriftSamples, false, "تعذر التحقق مؤقتًا؛ لن يرسل التطبيق أوامر عمياء")
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
                if (match == TowerMatch.RADIO_MATCH_ID_CHANGED) "PCI/EARFCN متطابقان لكن هوية البرج تغيّرت" else "اكتُشف انتقال عن البرج المختار"
            )
        }

        val operation = runCatching { client.setCellLock(target.pci, target.earfcn) }.getOrNull()
        lastRepairAtMs = now
        consecutiveDriftSamples = 0
        val repaired = operation?.success == true
        return TowerGuardStatus(
            target = target,
            match = match,
            consecutiveDriftSamples = 0,
            repaired = repaired,
            message = if (repaired) "أعاد Tower Guard تثبيت الخلية المطلوبة" else "تعذر على Tower Guard إعادة تثبيت الخلية"
        )
    }

    /**
     * Safe, read-only neighbor discovery. Some newer ZTE goform firmwares expose these arrays;
     * MC801A variants that do not support them simply return empty/absent values.
     */
    suspend fun readNearbyCells(): List<NearbyCell> {
        val raw = client.readRaw(setOf("neighbor_cell_info", "current_cell_info", "locked_cell_info"))
        return buildList {
            addAll(parseCellArray(raw.opt("current_cell_info")))
            addAll(parseCellArray(raw.opt("neighbor_cell_info")))
        }.distinctBy { Triple(it.rat, it.pci, it.arfcn) }
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
                val isNr = rat == "16" || (arfcn ?: 0) > 65_535 || rawBand?.startsWith("N", true) == true || rawBand?.startsWith("NR", true) == true
                val band = normalizeBand(rawBand, isNr)
                add(
                    NearbyCell(
                        rat = if (isNr) "NR" else "LTE",
                        band = band,
                        pci = positiveInt(item, "pci"),
                        arfcn = arfcn,
                        rsrp = signal(item, "rsrp", -170.0, -35.0),
                        rsrq = signal(item, "rsrq", -40.0, 0.0),
                        sinr = signal(item, "sinr", -30.0, 60.0)
                    )
                )
            }
        }
    }

    private fun positiveInt(item: JSONObject, key: String): Int? =
        item.optString(key).trim().toIntOrNull()?.takeIf { it >= 0 }

    private fun signal(item: JSONObject, key: String, min: Double, max: Double): Double? {
        var value = item.optString(key).replace(Regex("[^0-9.\\-]"), "").toDoubleOrNull() ?: return null
        repeat(3) {
            if (value in min..max) return value
            val scaled = value / 10.0
            if (scaled !in min..max && kotlin.math.abs(scaled) >= kotlin.math.abs(value)) return null
            value = scaled
        }
        return value.takeIf { it in min..max }
    }

    private fun normalizeBand(raw: String?, nr: Boolean): String? {
        val number = Regex("\\d+").find(raw.orEmpty())?.value?.toIntOrNull() ?: return null
        return if (nr) "N$number" else "B$number"
    }

    companion object {
        private const val DRIFT_SAMPLES_BEFORE_REPAIR = 3
        private const val REPAIR_COOLDOWN_MS = 30_000L
    }
}
