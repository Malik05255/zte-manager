package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.OperationResult
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
        // MC801A-family web UIs obtain these values before login. LD is required for the
        // password challenge; WA/CR/RD allow us to include the same AD proof used by the
        // stock UI on firmwares that expose it.
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

        // goform_set_cmd_process is a POST endpoint in the router web UI. Using GET here can
        // appear to work on permissive firmware while silently failing on stricter builds.
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

    suspend fun setLteBands(bands: Set<Int>): OperationResult {
        if (!profile.capabilities.supportsLteBandLock) return unsupported("تثبيت ترددات 4G")
        if (bands.isEmpty() || !profile.capabilities.supportedLteBands.containsAll(bands)) {
            return OperationResult(false, false, "اختيار 4G يحتوي ترددًا غير مدعوم لهذا الـProfile")
        }

        val requestedMask = BandEncoding.lteMask(bands)
        val raw = writeWithAd(
            goformId = "BAND_SELECT",
            values = mapOf(
                "is_gw_band" to "0",
                "gw_band_mask" to "0",
                "is_lte_band" to "1",
                "lte_band_mask" to requestedMask
            )
        )
        if (!commandAccepted(raw)) return OperationResult(false, false, "الراوتر رفض تثبيت ترددات 4G", raw)

        delay(900)
        val readBack = readRaw(setOf("lte_band_lock", "wan_active_band"))
        val configuredMask = readBack.optString("lte_band_lock").trim()
        if (configuredMask.isNotBlank() && masksEqual(configuredMask, requestedMask)) {
            return OperationResult(true, true, "تم تثبيت ترددات 4G والتحقق من القناع", raw)
        }

        val activeBand = extractBandNumber(readBack.optString("wan_active_band"))
        val activeConsistent = activeBand == null || activeBand in bands
        return OperationResult(
            success = activeConsistent,
            verified = false,
            message = if (activeConsistent) {
                "قبل الراوتر الأمر، لكن الـFirmware لم يعطِ read-back موثوقًا للقناع؛ لن نعرض العملية كمتحقق منها"
            } else {
                "قبل الراوتر الأمر لكن التردد النشط لا يطابق الاختيار؛ اعتُبرت العملية غير ناجحة"
            },
            rawResult = raw
        )
    }

    suspend fun setNrBands(bands: Set<Int>): OperationResult {
        if (!profile.capabilities.supportsNrBandLock) return unsupported("تثبيت ترددات 5G")
        if (bands.isEmpty() || !profile.capabilities.supportedNrBands.containsAll(bands)) {
            return OperationResult(false, false, "اختيار 5G يحتوي ترددًا غير مدعوم لهذا الـProfile")
        }

        val requested = BandEncoding.nrMask(bands)
        val raw = writeWithAd(
            goformId = "WAN_PERFORM_NR5G_BAND_LOCK",
            values = mapOf("nr5g_band_mask" to requested)
        )
        if (!commandAccepted(raw)) return OperationResult(false, false, "الراوتر رفض تثبيت ترددات 5G", raw)

        delay(900)
        val readBack = readRaw(setOf("nr5g_sa_band_lock", "nr5g_nsa_band_lock", "nr5g_action_band"))
        val exact = listOf("nr5g_sa_band_lock", "nr5g_nsa_band_lock")
            .map { readBack.optString(it) }
            .filter { it.isNotBlank() }
            .any { parseBandList(it) == bands }

        if (exact) return OperationResult(true, true, "تم تثبيت ترددات 5G والتحقق منها", raw)

        val active = extractBandNumber(readBack.optString("nr5g_action_band"))
        val activeConsistent = active == null || active in bands
        return OperationResult(
            activeConsistent,
            false,
            if (activeConsistent) "قبل الراوتر أمر 5G، لكن لا يوجد read-back موثوق لهذا الـFirmware" else "التردد النشط لا يطابق اختيار 5G",
            raw
        )
    }

    suspend fun setCellLock(pci: Int, earfcn: Int): OperationResult {
        if (!profile.capabilities.supportsCellLock) return unsupported("تثبيت الخلية")
        if (pci !in 0..1007 || earfcn <= 0) return OperationResult(false, false, "PCI أو EARFCN غير صالح")

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
        val verified = readBack.optString("lte_pci_lock") == pci.toString() &&
            readBack.optString("lte_earfcn_lock") == earfcn.toString()

        return OperationResult(
            success = true,
            verified = verified,
            message = if (verified) "تم حفظ PCI/EARFCN والتحقق منهما" else "قبل الراوتر الأمر؛ يلزم تأكيده بعد استقرار/إعادة تشغيل الراوتر",
            rawResult = raw
        )
    }

    suspend fun setNetworkMode(mode: String): OperationResult {
        if (mode !in NETWORK_MODES) return OperationResult(false, false, "وضع شبكة غير معروف")
        val raw = writeWithAd(
            goformId = "SET_BEARER_PREFERENCE",
            values = mapOf("BearerPreference" to mode)
        )
        return OperationResult(commandAccepted(raw), false, if (commandAccepted(raw)) "تم إرسال وضع الشبكة" else "رفض الراوتر وضع الشبكة", raw)
    }

    suspend fun setAntennaState(state: Int): OperationResult {
        if (!profile.capabilities.supportsAntennaControl) return unsupported("التحكم بالهوائي")
        if (state !in 1..3) return OperationResult(false, false, "حالة الهوائي يجب أن تكون 1 أو 2 أو 3")
        val raw = writeWithAd(
            goformId = "BSP_ANTENNA_STATE_SET",
            values = mapOf("antenna_name" to "6", "state" to state.toString())
        )
        return OperationResult(commandAccepted(raw), false, if (commandAccepted(raw)) "تم إرسال إعداد الهوائي" else "رفض الراوتر إعداد الهوائي", raw)
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

    private fun masksEqual(a: String, b: String): Boolean {
        fun normalize(value: String) = value.trim().removePrefix("0x").removePrefix("0X").trimStart('0').lowercase().ifBlank { "0" }
        return normalize(a) == normalize(b)
    }

    private fun parseBandList(value: String): Set<Int> = value
        .replace("%2C", ",", ignoreCase = true)
        .split(',', '+', ';')
        .mapNotNull { extractBandNumber(it) }
        .toSet()

    private fun extractBandNumber(value: String): Int? = Regex("\\d+").find(value)?.value?.toIntOrNull()

    private fun firstValue(json: JSONObject, vararg names: String): String? = names
        .asSequence()
        .map { json.optString(it).trim() }
        .firstOrNull { it.isNotBlank() }

    companion object {
        private const val GET_PATH = "/goform/goform_get_cmd_process"
        private const val SET_PATH = "/goform/goform_set_cmd_process"

        private val IDENTITY_FIELDS = linkedSetOf(
            "device_name", "model_name", "product_name",
            "hardware_version", "web_version", "wa_inner_version", "cr_version", "loginfo"
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
