package com.malik.ztesmartmanager.core.tower

import com.malik.ztesmartmanager.core.model.RouterSnapshot
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
    val sinr: Double?
)

/**
 * Verified tower/cell control.
 *
 * The legacy MC801A command can lock only PCI + EARFCN. Cell ID/eNodeB are retained as
 * observational identity evidence, never presented as writable keys when the firmware cannot
 * write them. Guard repair requires repeated drift plus a cooldown to avoid ping-pong.
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

    suspend fun lockCurrent(): TowerGuardStatus = lockCurrent(client.readSnapshot())

    suspend fun lockCurrent(snapshot: RouterSnapshot): TowerGuardStatus {
        val target = captureCurrent(snapshot)
            ?: error("لا توجد هوية LTE موثقة كافية لتثبيت البرج: نحتاج PCI و EARFCN")

        val operation = client.setCellLock(target.pci, target.earfcn)
        if (!operation.success) {
            return TowerGuardStatus(target, TowerMatch.UNKNOWN, 0, false, operation.message)
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
                TowerMatch.MATCHED -> "تم تثبيت الخلية والتحقق من بقاء الراوتر على الهدف"
                TowerMatch.RADIO_MATCH_ID_CHANGED -> "PCI/EARFCN ثابتان لكن هوية Cell ID/eNodeB تغيّرت؛ لن يدّعي التطبيق أن البرج الفيزيائي ثابت"
                TowerMatch.DRIFTED -> "قبل الراوتر أمر القفل لكنه لم يبقَ على الخلية المطلوبة"
                TowerMatch.UNKNOWN -> "قبل الراوتر الأمر، لكن التحقق اللاحق غير كافٍ"
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
                if (match == TowerMatch.RADIO_MATCH_ID_CHANGED) "تغيّرت هوية البرج رغم تطابق PCI/EARFCN" else "اكتُشف انتقال عن الخلية المستهدفة"
            )
        }

        val operation = runCatching { client.setCellLock(target.pci, target.earfcn) }.getOrNull()
        lastRepairAtMs = now
        consecutiveDriftSamples = 0
        val repaired = operation?.success == true
        return TowerGuardStatus(
            target,
            match,
            0,
            repaired,
            if (repaired) "أعاد Tower Guard تطبيق القفل الموثق" else "تعذر على Tower Guard إعادة تطبيق القفل"
        )
    }

    /** Read-only discovery. Unsupported firmware returns no cells rather than fabricated data. */
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
                val isNr = rat == "16" || (arfcn ?: 0) > 65_535 ||
                    rawBand?.startsWith("N", true) == true || rawBand?.startsWith("NR", true) == true
                add(
                    NearbyCell(
                        rat = if (isNr) "NR" else "LTE",
                        band = normalizeBand(rawBand, isNr),
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
            value /= 10.0
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
