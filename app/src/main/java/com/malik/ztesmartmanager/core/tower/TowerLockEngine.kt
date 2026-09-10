package com.malik.ztesmartmanager.core.tower

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.CellLockState
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
 * A write acknowledgement is not proof of a lock. We require exact lock-key read-back and a live
 * serving-cell match. Any user-requested switch to another LTE cell is transactional: the previous
 * cell-lock state is read first and restored automatically if the target cannot be verified.
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

        if (target.cellId != null && snapshot.cellId != null && target.cellId != snapshot.cellId) {
            return TowerMatch.RADIO_MATCH_ID_CHANGED
        }
        val currentEnodeb = snapshot.raw["enodeb_id"]?.trim()?.takeIf { it.isNotBlank() && it != "--" }
        if (target.enodebId != null && currentEnodeb != null && target.enodebId != currentEnodeb) {
            return TowerMatch.RADIO_MATCH_ID_CHANGED
        }
        return TowerMatch.MATCHED
    }

    suspend fun lockCurrent(): TowerGuardStatus = lockCurrent(client.readSnapshot())

    suspend fun lockCurrent(snapshot: RouterSnapshot): TowerGuardStatus {
        val target = captureCurrent(snapshot)
            ?: error("لا توجد هوية LTE موثقة كافية لتثبيت الخلية: نحتاج PCI و EARFCN")
        return applyTargetTransaction(target)
    }

    suspend fun lockNearbyCell(cell: NearbyCell): TowerGuardStatus {
        require(cell.rat.equals("LTE", true)) { "Cell Lock الحالي موثق لـLTE فقط" }
        val pci = cell.pci ?: error("الخلية القريبة لا تحتوي PCI موثوقًا")
        val earfcn = cell.arfcn ?: error("الخلية القريبة لا تحتوي EARFCN موثوقًا")
        val target = TowerTarget(
            pci = pci,
            earfcn = earfcn,
            band = cell.band,
            cellId = null,
            enodebId = null
        )
        return applyTargetTransaction(target)
    }

    private suspend fun applyTargetTransaction(target: TowerTarget): TowerGuardStatus {
        val previous = client.readCellLockState()
            ?: return TowerGuardStatus(
                target,
                TowerMatch.UNKNOWN,
                0,
                false,
                "لن يغيّر التطبيق الخلية: تعذر قراءة حالة Cell Lock الأصلية اللازمة للاستعادة الآمنة"
            )

        val operation = runCatching { client.setCellLock(target.pci, target.earfcn) }.getOrNull()
        if (operation?.success != true || !operation.verified) {
            val restored = restore(previous)
            return TowerGuardStatus(
                target,
                TowerMatch.UNKNOWN,
                0,
                false,
                if (restored) {
                    "لم يثبت read-back القفل المطلوب؛ تمت استعادة حالة القفل الأصلية"
                } else {
                    "لم يثبت read-back القفل المطلوب وتعذر تأكيد الاستعادة؛ راجع Cell Lock في الراوتر"
                }
            )
        }

        delay(TARGET_SETTLE_MS)
        val after = runCatching { client.readSnapshot() }.getOrNull()
        val match = after?.let { compare(target, it) } ?: TowerMatch.UNKNOWN
        if (match == TowerMatch.MATCHED) {
            consecutiveDriftSamples = 0
            return TowerGuardStatus(
                target,
                match,
                0,
                false,
                "تم حفظ Cell Lock والتحقق من أن الخلية الحية تطابق PCI/EARFCN المطلوبين"
            )
        }

        val restored = restore(previous)
        val reason = when (match) {
            TowerMatch.RADIO_MATCH_ID_CHANGED -> "تطابق PCI/EARFCN لكن تغيّرت هوية Cell ID/eNodeB"
            TowerMatch.DRIFTED -> "الراوتر لم يبقَ على PCI/EARFCN المطلوبين"
            TowerMatch.UNKNOWN -> "لم تتوفر قراءة حية كافية بعد القفل"
            TowerMatch.MATCHED -> ""
        }
        return TowerGuardStatus(
            target,
            match,
            0,
            false,
            if (restored) "$reason؛ تمت استعادة حالة Cell Lock الأصلية" else "$reason؛ وتعذر تأكيد الاستعادة"
        )
    }

    private suspend fun restore(previous: CellLockState): Boolean {
        val result = runCatching { client.restoreCellLock(previous) }.getOrNull()
        return result?.success == true && result.verified
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
     * Read-only discovery. Only cells with PCI + ARFCN are returned because anything less cannot
     * be uniquely targeted by the verified LTE cell-lock command.
     */
    suspend fun readNearbyCells(): List<NearbyCell> {
        val raw = client.readRaw(setOf("neighbor_cell_info", "current_cell_info", "locked_cell_info"))
        return buildList {
            addAll(parseCellArray(raw.opt("current_cell_info")))
            addAll(parseCellArray(raw.opt("neighbor_cell_info")))
        }
            .filter { it.pci != null && it.arfcn != null }
            .distinctBy { Triple(it.rat, it.pci, it.arfcn) }
            .sortedWith(
                compareByDescending<NearbyCell> { it.rsrp ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.sinr ?: Double.NEGATIVE_INFINITY }
            )
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
        private const val TARGET_SETTLE_MS = 2_000L
        private const val DRIFT_SAMPLES_BEFORE_REPAIR = 3
        private const val REPAIR_COOLDOWN_MS = 30_000L
    }
}
