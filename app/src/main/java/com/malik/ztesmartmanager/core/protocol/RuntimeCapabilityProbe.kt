package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityEvidence
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState
import org.json.JSONObject

object RuntimeCapabilityProbe {
    suspend fun probe(client: ZteRouterClient): RuntimeCapabilityReport =
        evaluate(client.profile.capabilities, client.readRaw(PROBE_FIELDS))

    fun evaluate(capabilities: RouterCapabilities, raw: JSONObject): RuntimeCapabilityReport {
        val lteFields = present(raw, "lte_band_lock", "lte_band_mask")
        val lteMask = firstExact(raw, "lte_band_lock", "lte_band_mask")
        val lte = when {
            !capabilities.supportsLteBandLock -> unavailable("هذا Profile لا يعلن LTE Band Lock")
            lteMask != null && !isEmptyOrZero(lteMask) -> safe("قناع LTE الحالي ظاهر ويمكن حفظه واستعادته حرفيًا قبل أي تغيير", lteFields)
            lteFields.isNotEmpty() -> readOnly("الراوتر يعرض حقل LTE lock لكن القيمة الحالية لا توفر rollback حرفيًا وآمنًا", lteFields)
            else -> profileOnly("Profile يدعم LTE Band Lock لكن الـFirmware لم يعرض حقل read-back")
        }

        val nrFields = present(raw, "nr5g_band_lock", "nr5g_band_mask", "nr5g_sa_band_lock", "nr5g_nsa_band_lock")
        val nrSafe = canRestoreNr(raw)
        val nr = when {
            !capabilities.supportsNrBandLock -> unavailable("هذا Profile لا يعلن NR Band Lock")
            nrSafe -> safe("قناع 5G الحالي ظاهر ومتسق ويمكن استعادته قبل أي تغيير", nrFields)
            nrFields.isNotEmpty() -> readOnly("حقول NR lock موجودة لكن لا يوجد rollback موثوق غير متعارض للقيمة الحالية", nrFields)
            else -> profileOnly("Profile يدعم NR Band Lock لكن الـFirmware لم يعرض read-back موثوقًا")
        }

        val cellFields = present(raw, "lte_pci_lock", "lte_earfcn_lock")
        val pci = exact(raw, "lte_pci_lock")
        val earfcn = exact(raw, "lte_earfcn_lock")
        val completeCellState = pci != null && earfcn != null && validCellLockPair(pci, earfcn)
        val cell = when {
            !capabilities.supportsCellLock -> unavailable("هذا Profile لا يعلن LTE Cell Lock")
            completeCellState -> safe("حالتا PCI/EARFCN lock ظاهرتان ويمكن استعادة القفل أو حالة unlock", cellFields)
            cellFields.isNotEmpty() -> readOnly("حالة Cell Lock ناقصة أو غير قابلة للاستعادة بأمان", cellFields)
            else -> profileOnly("Profile يدعم Cell Lock لكن حقول read-back غير ظاهرة")
        }

        val modeFields = present(raw, "BearerPreference")
        val bearer = exact(raw, "BearerPreference")?.trim()
        val networkMode = when {
            !bearer.isNullOrBlank() -> safe("BearerPreference الحالي ظاهر؛ يمكن حفظه والتحقق من القيمة الجديدة", modeFields)
            modeFields.isNotEmpty() -> readOnly("BearerPreference موجود لكنه فارغ؛ لا يمكن ضمان rollback", modeFields)
            else -> unavailable("الـFirmware لم يعرض BearerPreference؛ تغيير الوضع سيبقى غير قابل للتحقق")
        }

        val neighborFields = present(raw, "current_cell_info", "neighbor_cell_info", "ngbr_cell_info")
        val neighbor = if (neighborFields.isNotEmpty()) {
            readOnly("الراوتر يعرض سطح قراءة للخلايا الحالية/المجاورة؛ عدم وجود نتائج الآن لا يعني عدم الدعم", neighborFields)
        } else {
            unavailable("لم يظهر أي حقل current/neighbor cell في probe القراءة فقط")
        }

        val caFields = present(raw, "wan_lte_ca", "Lte_ca_status", "lte_multi_ca_scell_info", "lte_ca_scell_info")
        val ca = when {
            !capabilities.supportsCarrierAggregationRead -> unavailable("هذا Profile لا يعلن قراءة Carrier Aggregation")
            caFields.isNotEmpty() -> readOnly("حقول CA الحية ظاهرة ويمكن استخدامها وفق قواعد Truth-First", caFields)
            else -> profileOnly("Profile يعلن قراءة CA لكن الـFirmware لم يعرض حقولها في probe")
        }

        val antenna = when {
            !capabilities.supportsAntennaControl -> unavailable("هذا Profile لا يعلن التحكم بالهوائي")
            else -> profileOnly("التحكم بالهوائي معروف للـProfile لكن لا يوجد read-back موثوق في هذا التطبيق؛ لن يُصنف كتابة آمنة")
        }

        return RuntimeCapabilityReport(
            lteBandControl = lte,
            nrBandControl = nr,
            cellLock = cell,
            networkMode = networkMode,
            neighborScan = neighbor,
            antennaControl = antenna,
            carrierAggregationTelemetry = ca,
            probedAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun canRestoreNr(raw: JSONObject): Boolean {
        firstExact(raw, "nr5g_band_lock", "nr5g_band_mask")?.let {
            return !isEmptyOrZero(it)
        }
        val sa = exact(raw, "nr5g_sa_band_lock")
        val nsa = exact(raw, "nr5g_nsa_band_lock")
        val split = listOfNotNull(sa, nsa)
        if (split.isEmpty() || split.any(::isEmptyOrZero)) return false
        return split.map(::normalizeBandList).distinct().size == 1
    }

    private fun validCellLockPair(pci: String, earfcn: String): Boolean {
        val pciUnlocked = isEmptyOrZero(pci)
        val earfcnUnlocked = isEmptyOrZero(earfcn)
        if (pciUnlocked || earfcnUnlocked) return pciUnlocked && earfcnUnlocked
        val pciValue = pci.trim().toIntOrNull()
        val earfcnValue = earfcn.trim().toIntOrNull()
        return pciValue != null && pciValue in 0..503 && earfcnValue != null && earfcnValue > 0
    }

    private fun present(raw: JSONObject, vararg names: String): Set<String> =
        names.filterTo(linkedSetOf()) { raw.has(it) && !raw.isNull(it) }

    private fun exact(raw: JSONObject, name: String): String? =
        if (raw.has(name) && !raw.isNull(name)) raw.optString(name) else null

    private fun firstExact(raw: JSONObject, vararg names: String): String? {
        names.forEach { name -> exact(raw, name)?.let { return it } }
        return null
    }

    private fun isEmptyOrZero(value: String): Boolean {
        val normalized = value.trim().lowercase()
        return normalized.isBlank() || normalized == "0" || normalized == "0x0"
    }

    private fun normalizeBandList(value: String): String = value
        .replace("%2C", ",", ignoreCase = true)
        .split(',', '+', ';', ' ')
        .mapNotNull { Regex("\\d+").find(it)?.value?.toIntOrNull() }
        .distinct()
        .sorted()
        .joinToString(",")

    private fun safe(reason: String, fields: Set<String>) = RuntimeCapabilityEvidence(
        RuntimeCapabilityState.SAFE_TO_ATTEMPT, reason, fields
    )

    private fun readOnly(reason: String, fields: Set<String>) = RuntimeCapabilityEvidence(
        RuntimeCapabilityState.READ_ONLY, reason, fields
    )

    private fun profileOnly(reason: String) = RuntimeCapabilityEvidence(
        RuntimeCapabilityState.PROFILE_ONLY, reason
    )

    private fun unavailable(reason: String) = RuntimeCapabilityEvidence(
        RuntimeCapabilityState.UNAVAILABLE, reason
    )

    val PROBE_FIELDS: Set<String> = linkedSetOf(
        "lte_band_lock", "lte_band_mask",
        "nr5g_band_lock", "nr5g_band_mask", "nr5g_sa_band_lock", "nr5g_nsa_band_lock",
        "lte_pci_lock", "lte_earfcn_lock", "BearerPreference",
        "current_cell_info", "neighbor_cell_info", "ngbr_cell_info",
        "wan_lte_ca", "Lte_ca_status", "lte_multi_ca_scell_info", "lte_ca_scell_info"
    )
}
