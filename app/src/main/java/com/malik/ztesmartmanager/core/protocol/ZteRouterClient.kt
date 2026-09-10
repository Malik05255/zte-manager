package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.OperationResult
import com.malik.ztesmartmanager.core.model.RouterRestoreReport
import com.malik.ztesmartmanager.core.model.RouterRestoreStep
import com.malik.ztesmartmanager.core.model.RouterSettingsBackup
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.profile.GenericZteProfile
import com.malik.ztesmartmanager.core.profile.RouterProfile
import com.malik.ztesmartmanager.core.profile.RouterProfileRegistry
import kotlinx.coroutines.delay
import org.json.JSONObject

class ZteRouterClient(routerAddress: String) {
    private val transport = ZteHttpTransport(routerAddress)

    var profile: RouterProfile = GenericZteProfile
        private set

    suspend fun login(adminPassword: String): RouterProfile {
        val auth = readRaw(setOf("LD", "wa_inner_version", "cr_version", "RD"))
        val ld = auth.optString("LD").trim()
        if (ld.isBlank()) throw ZteAuthenticationException("لم يُرجع الراوتر قيمة LD المطلوبة للمصادقة")

        val loginParams = linkedMapOf(
            "isTest" to "false",
            "goformId" to "LOGIN",
            "password" to ZteCrypto.loginPassword(adminPassword, ld)
        )

        val wa = auth.optString("wa_inner_version").trim()
        val cr = auth.optString("cr_version").trim()
        val rd = auth.optString("RD").trim()
        if (wa.isNotBlank() && cr.isNotBlank() && rd.isNotBlank()) {
            loginParams["AD"] = ZteCrypto.adValue(wa, cr, rd)
        }

        val loginRaw = transport.postForm(SET_PATH, loginParams)
        val loginJson = runCatching { JSONObject(loginRaw) }
            .getOrElse { throw ZteAuthenticationException("رد تسجيل الدخول غير صالح") }
        val result = loginJson.optString("result").trim()
        if (result != "0" && !result.equals("success", true)) {
            throw ZteAuthenticationException("رفض الراوتر تسجيل الدخول")
        }

        val identity = readRaw(IDENTITY_FIELDS)
        val logInfo = identity.optString("loginfo").trim()
        if (logInfo.isNotBlank() && !logInfo.equals("ok", true)) {
            throw ZteAuthenticationException("قبل الراوتر الطلب لكن لم يتم إنشاء جلسة إدارة موثقة")
        }

        val model = firstValue(identity, "device_name", "model_name", "product_name")
        val hardware = identity.optString("hardware_version").takeIf { it.isNotBlank() }
        val firmware = firstValue(identity, "wa_inner_version", "web_version", "cr_version")
        profile = RouterProfileRegistry.resolve(model, hardware, firmware)
        return profile
    }

    suspend fun readSnapshot(): RouterSnapshot =
        ZteSnapshotParser.parse(readRaw(profile.statusFields))

    suspend fun readRaw(fields: Set<String>): JSONObject = transport.getJson(
        path = GET_PATH,
        params = mapOf(
            "isTest" to "false",
            "cmd" to fields.joinToString(","),
            "multi_data" to "1"
        )
    )

    /**
     * Capture exact restorable radio settings immediately before a write.
     * Authentication material and personal/SIM identifiers are deliberately excluded.
     */
    suspend fun captureSettingsBackup(routerAddress: String): RouterSettingsBackup {
        val raw = readRaw(BACKUP_FIELDS)
        val backup = RouterSettingsBackup(
            createdAtEpochMs = System.currentTimeMillis(),
            routerAddress = routerAddress,
            profileId = profile.id,
            modelFamily = profile.capabilities.modelFamily,
            model = firstValue(raw, "device_name", "model_name", "product_name"),
            firmware = firstValue(raw, "wa_inner_version", "web_version", "cr_version"),
            hardwareVersion = exactValue(raw, "hardware_version"),
            lteBandLock = firstExactValue(raw, "lte_band_lock", "lte_band_mask"),
            nrBandLock = firstExactValue(raw, "nr5g_band_lock", "nr5g_band_mask"),
            nrSaBandLock = exactValue(raw, "nr5g_sa_band_lock"),
            nrNsaBandLock = exactValue(raw, "nr5g_nsa_band_lock"),
            ltePciLock = exactValue(raw, "lte_pci_lock"),
            lteEarfcnLock = exactValue(raw, "lte_earfcn_lock"),
            bearerPreference = exactValue(raw, "BearerPreference")
        )
        if (!backup.hasAnyRestorableRadioSetting) {
            throw ZteProtocolException("الـFirmware لم يعرض أي إعداد راديو يمكن حفظه قبل التغيير")
        }
        return backup
    }

    /** Restore only values that were actually exposed in the captured snapshot. */
    suspend fun restoreSettings(backup: RouterSettingsBackup): RouterRestoreReport {
        if (backup.profileId != profile.id) {
            return RouterRestoreReport(
                listOf(
                    RouterRestoreStep(
                        "Profile",
                        OperationResult(false, false, "النسخة تخص ${backup.profileId} والجهاز الحالي ${profile.id}; تم إيقاف الاستعادة")
                    )
                )
            )
        }

        val steps = mutableListOf<RouterRestoreStep>()

        backup.lteBandLock?.let { mask ->
            steps += RouterRestoreStep("LTE bands", restoreLteMask(mask))
        }

        val nrTarget = resolveRestorableNrMask(backup)
        when {
            nrTarget == NR_CONFLICT -> steps += RouterRestoreStep(
                "5G bands",
                OperationResult(false, false, "النسخة تحتوي قيم SA وNSA مختلفة؛ لن أخمّن أمر استعادة قد يغيّر النمط الخطأ")
            )
            nrTarget != null -> steps += RouterRestoreStep("5G bands", restoreNrMask(nrTarget))
        }

        val pci = backup.ltePciLock
        val earfcn = backup.lteEarfcnLock
        when {
            pci == null && earfcn == null -> Unit
            pci == null || earfcn == null -> steps += RouterRestoreStep(
                "Cell lock",
                OperationResult(false, false, "نسخة Cell Lock ناقصة؛ لم يتم إرسال أمر غير قابل للتحقق")
            )
            isUnlockedValue(pci) && isUnlockedValue(earfcn) ->
                steps += RouterRestoreStep("Cell lock", clearCellLock())
            else -> {
                val pciNumber = pci.toIntOrNull()
                val earfcnNumber = earfcn.toIntOrNull()
                val result = if (pciNumber != null && earfcnNumber != null) {
                    setCellLock(pciNumber, earfcnNumber)
                } else {
                    OperationResult(false, false, "قيم Cell Lock المحفوظة غير قابلة للتحويل بأمان")
                }
                steps += RouterRestoreStep("Cell lock", result)
            }
        }

        backup.bearerPreference?.takeIf { it.isNotBlank() }?.let { mode ->
            steps += RouterRestoreStep("Network mode", setNetworkModeRaw(mode))
        }

        return RouterRestoreReport(steps)
    }

    suspend fun setLteBands(bands: Set<Int>): OperationResult {
        if (!profile.capabilities.supportsLteBandLock) return unsupported("تثبيت ترددات 4G")
        if (bands.isEmpty() || !profile.capabilities.supportedLteBands.containsAll(bands)) {
            return OperationResult(false, false, "اختيار 4G يحتوي ترددًا غير مدعوم لهذا الـProfile")
        }
        return restoreLteMask(BandEncoding.lteMask(bands), successMessage = "تم تثبيت ترددات 4G والتحقق من القناع")
    }

    suspend fun setNrBands(bands: Set<Int>): OperationResult {
        if (!profile.capabilities.supportsNrBandLock) return unsupported("تثبيت ترددات 5G")
        if (bands.isEmpty() || !profile.capabilities.supportedNrBands.containsAll(bands)) {
            return OperationResult(false, false, "اختيار 5G يحتوي ترددًا غير مدعوم لهذا الـProfile")
        }
        return restoreNrMask(BandEncoding.nrMask(bands), expectedBands = bands, successMessage = "تم تثبيت ترددات 5G والتحقق منها")
    }

    suspend fun setCellLock(pci: Int, earfcn: Int): OperationResult {
        if (!profile.capabilities.supportsCellLock) return unsupported("تثبيت الخلية")
        if (pci !in 0..503 || earfcn <= 0) return OperationResult(false, false, "PCI LTE يجب أن يكون 0..503 وEARFCN أكبر من صفر")

        val raw = writeWithAd(
            goformId = "LTE_LOCK_CELL_SET",
            values = mapOf(
                "lte_pci_lock" to pci.toString(),
                "lte_earfcn_lock" to earfcn.toString()
            )
        )
        if (!commandAccepted(raw)) return OperationResult(false, false, "الراوتر رفض تثبيت الخلية", raw)

        delay(700)
        val readBack = readRaw(setOf("lte_pci_lock", "lte_earfcn_lock"))
        val verified = readBack.optString("lte_pci_lock").trim() == pci.toString() &&
            readBack.optString("lte_earfcn_lock").trim() == earfcn.toString()

        return OperationResult(
            success = verified,
            verified = verified,
            message = if (verified) "تم حفظ PCI/EARFCN والتحقق منهما" else "قبل الراوتر الأمر لكن read-back لم يطابق القيم؛ لن نعرضه كنجاح",
            rawResult = raw
        )
    }

    /**
     * Empty LTE_LOCK_CELL_SET values are a known ZTE goform removal path.
     * We still require read-back to become blank/zero before calling it successful.
     */
    suspend fun clearCellLock(): OperationResult {
        if (!profile.capabilities.supportsCellLock) return unsupported("إزالة تثبيت الخلية")
        val raw = writeWithAd(
            goformId = "LTE_LOCK_CELL_SET",
            values = mapOf("lte_pci_lock" to "", "lte_earfcn_lock" to "")
        )
        if (!commandAccepted(raw)) return OperationResult(false, false, "الراوتر رفض إزالة Cell Lock", raw)

        delay(700)
        val readBack = readRaw(setOf("lte_pci_lock", "lte_earfcn_lock"))
        val verified = isUnlockedValue(readBack.optString("lte_pci_lock")) &&
            isUnlockedValue(readBack.optString("lte_earfcn_lock"))
        return OperationResult(
            success = verified,
            verified = verified,
            message = if (verified) "تمت إزالة Cell Lock والتحقق من القيم" else "أُرسل أمر الإزالة لكن الراوتر لم يؤكد فراغ/صفر القفل",
            rawResult = raw
        )
    }

    suspend fun setNetworkMode(mode: String): OperationResult {
        if (mode !in NETWORK_MODES) return OperationResult(false, false, "وضع شبكة غير معروف")
        return setNetworkModeRaw(mode)
    }

    suspend fun setAntennaState(state: Int): OperationResult {
        if (!profile.capabilities.supportsAntennaControl) return unsupported("التحكم بالهوائي")
        if (state !in 1..3) return OperationResult(false, false, "حالة الهوائي يجب أن تكون 1 أو 2 أو 3")
        val raw = writeWithAd(
            goformId = "BSP_ANTENNA_STATE_SET",
            values = mapOf("antenna_name" to "6", "state" to state.toString())
        )
        return OperationResult(commandAccepted(raw), false, if (commandAccepted(raw)) "تم إرسال إعداد الهوائي؛ لا يوجد read-back موثوق لهذا الـFirmware" else "رفض الراوتر إعداد الهوائي", raw)
    }

    private suspend fun restoreLteMask(
        mask: String,
        successMessage: String = "تمت استعادة قناع LTE والتحقق منه"
    ): OperationResult {
        if (!profile.capabilities.supportsLteBandLock) return unsupported("تثبيت ترددات 4G")
        if (mask.isBlank() || isZeroMask(mask)) {
            return OperationResult(
                false,
                false,
                "كانت قيمة LTE الأصلية فارغة/صفر. لا يوجد AUTO mask موحّد وآمن بين Firmware ZTE، لذلك لم أرسل رقمًا ثابتًا تخمينيًا"
            )
        }

        val raw = writeWithAd(
            goformId = "BAND_SELECT",
            values = mapOf(
                "is_gw_band" to "0",
                "gw_band_mask" to "0",
                "is_lte_band" to "1",
                "lte_band_mask" to mask
            )
        )
        if (!commandAccepted(raw)) return OperationResult(false, false, "الراوتر رفض قناع LTE", raw)

        delay(900)
        val readBack = readRaw(setOf("lte_band_lock", "lte_band_mask", "wan_active_band"))
        val configured = firstExactValue(readBack, "lte_band_lock", "lte_band_mask")
        if (configured != null && masksEqual(configured, mask)) {
            return OperationResult(true, true, successMessage, raw)
        }

        return OperationResult(
            success = false,
            verified = false,
            message = "قبل الراوتر أمر LTE لكن القناع لم يعد مطابقًا في read-back؛ اعتُبرت العملية غير ناجحة",
            rawResult = raw
        )
    }

    private suspend fun restoreNrMask(
        mask: String,
        expectedBands: Set<Int>? = null,
        successMessage: String = "تمت استعادة قناع 5G والتحقق منه"
    ): OperationResult {
        if (!profile.capabilities.supportsNrBandLock) return unsupported("تثبيت ترددات 5G")
        if (mask.isBlank() || mask.trim() == "0") {
            return OperationResult(
                false,
                false,
                "كانت قيمة 5G الأصلية فارغة/صفر؛ لم أرسل Unlock غير موثّق لهذا Firmware"
            )
        }

        val raw = writeWithAd(
            goformId = "WAN_PERFORM_NR5G_BAND_LOCK",
            values = mapOf("nr5g_band_mask" to mask)
        )
        if (!commandAccepted(raw)) return OperationResult(false, false, "الراوتر رفض قناع 5G", raw)

        delay(900)
        val readBack = readRaw(setOf("nr5g_band_lock", "nr5g_band_mask", "nr5g_sa_band_lock", "nr5g_nsa_band_lock", "nr5g_action_band"))
        val requested = expectedBands ?: parseBandList(mask)
        val configuredValues = listOf("nr5g_band_lock", "nr5g_band_mask", "nr5g_sa_band_lock", "nr5g_nsa_band_lock")
            .mapNotNull { key -> exactValue(readBack, key)?.takeIf { it.isNotBlank() } }
        val exact = requested.isNotEmpty() && configuredValues.any { parseBandList(it) == requested }

        return if (exact) {
            OperationResult(true, true, successMessage, raw)
        } else {
            OperationResult(false, false, "قبل الراوتر أمر 5G لكن read-back لم يطابق القناع المطلوب", raw)
        }
    }

    private suspend fun setNetworkModeRaw(mode: String): OperationResult {
        if (mode.isBlank()) return OperationResult(false, false, "وضع الشبكة المحفوظ فارغ")
        val raw = writeWithAd(
            goformId = "SET_BEARER_PREFERENCE",
            values = mapOf("BearerPreference" to mode)
        )
        if (!commandAccepted(raw)) return OperationResult(false, false, "رفض الراوتر وضع الشبكة", raw)

        delay(600)
        val readBack = readRaw(setOf("BearerPreference"))
        val actual = exactValue(readBack, "BearerPreference")?.trim()
        val verified = actual != null && actual.equals(mode.trim(), ignoreCase = true)
        return OperationResult(
            success = if (actual == null) true else verified,
            verified = verified,
            message = when {
                verified -> "تم تطبيق وضع الشبكة والتحقق منه"
                actual == null -> "قبل الراوتر وضع الشبكة لكن الـFirmware لا يعرض BearerPreference للتحقق"
                else -> "قبل الراوتر الأمر لكن BearerPreference لا يطابق القيمة المطلوبة"
            },
            rawResult = raw
        )
    }

    private fun resolveRestorableNrMask(backup: RouterSettingsBackup): String? {
        backup.nrBandLock?.let { return it }
        val split = listOfNotNull(backup.nrSaBandLock, backup.nrNsaBandLock)
        if (split.isEmpty()) return null
        val normalized = split.map { normalizeBandList(it) }.distinct()
        return if (normalized.size == 1) split.first() else NR_CONFLICT
    }

    private suspend fun writeWithAd(goformId: String, values: Map<String, String>): String {
        val auth = readRaw(setOf("wa_inner_version", "cr_version", "RD"))
        val wa = auth.optString("wa_inner_version")
        val cr = auth.optString("cr_version")
        val rd = auth.optString("RD")
        if (wa.isBlank() || cr.isBlank() || rd.isBlank()) throw ZteProtocolException("تعذر إنشاء AD للأمر")

        val ad = ZteCrypto.adValue(wa, cr, rd)
        return transport.postForm(
            path = SET_PATH,
            params = linkedMapOf(
                "isTest" to "false",
                "goformId" to goformId
            ) + values + mapOf("AD" to ad)
        )
    }

    private fun commandAccepted(raw: String): Boolean = runCatching {
        val result = JSONObject(raw).optString("result")
        result.equals("success", true) || result == "0"
    }.getOrDefault(false)

    private fun unsupported(feature: String) = OperationResult(
        success = false,
        verified = false,
        message = "$feature غير مفعّل لهذا الموديل/الـFirmware حتى يتم التحقق من دعمه"
    )

    private fun masksEqual(a: String, b: String): Boolean = normalizeHexMask(a) == normalizeHexMask(b)

    private fun normalizeHexMask(value: String): String = value.trim()
        .removePrefix("0x")
        .removePrefix("0X")
        .trimStart('0')
        .lowercase()
        .ifBlank { "0" }

    private fun isZeroMask(value: String): Boolean = normalizeHexMask(value) == "0"

    private fun normalizeBandList(value: String): String = parseBandList(value).sorted().joinToString(",")

    private fun parseBandList(value: String): Set<Int> = value
        .replace("%2C", ",", ignoreCase = true)
        .split(',', '+', ';', ' ')
        .mapNotNull { extractBandNumber(it) }
        .toSet()

    private fun extractBandNumber(value: String): Int? = Regex("\\d+").find(value)?.value?.toIntOrNull()

    private fun isUnlockedValue(value: String): Boolean {
        val normalized = value.trim()
        return normalized.isBlank() || normalized == "0" || normalized == "0x0"
    }

    private fun exactValue(json: JSONObject, name: String): String? =
        if (json.has(name) && !json.isNull(name)) json.optString(name) else null

    private fun firstExactValue(json: JSONObject, vararg names: String): String? {
        for (name in names) {
            val value = exactValue(json, name)
            if (value != null) return value
        }
        return null
    }

    private fun firstValue(json: JSONObject, vararg names: String): String? = names
        .asSequence()
        .map { json.optString(it).trim() }
        .firstOrNull { it.isNotBlank() }

    companion object {
        private const val GET_PATH = "/goform/goform_get_cmd_process"
        private const val SET_PATH = "/goform/goform_set_cmd_process"
        private const val NR_CONFLICT = "__NR_SA_NSA_CONFLICT__"

        private val IDENTITY_FIELDS = linkedSetOf(
            "device_name", "model_name", "product_name",
            "hardware_version", "web_version", "wa_inner_version", "cr_version", "loginfo"
        )

        private val BACKUP_FIELDS = linkedSetOf(
            "device_name", "model_name", "product_name",
            "hardware_version", "web_version", "wa_inner_version", "cr_version",
            "lte_band_lock", "lte_band_mask",
            "nr5g_band_lock", "nr5g_band_mask", "nr5g_sa_band_lock", "nr5g_nsa_band_lock",
            "lte_pci_lock", "lte_earfcn_lock", "BearerPreference"
        )

        val NETWORK_MODES = setOf(
            "WL_AND_5G",
            "LTE_AND_5G",
            "Only_5G",
            "WCDMA_AND_LTE",
            "Only_LTE",
            "Only_WCDMA",
            "Only_GSM"
        )
    }
}

class ZteAuthenticationException(message: String) : Exception(message)
class ZteProtocolException(message: String) : Exception(message)
