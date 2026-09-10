package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.OperationResult
import com.malik.ztesmartmanager.core.model.RouterRestoreReport
import com.malik.ztesmartmanager.core.model.RouterRestoreStep
import com.malik.ztesmartmanager.core.model.RouterSettingsBackup
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityEvidence
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.profile.GenericZteProfile
import com.malik.ztesmartmanager.core.profile.RouterProfile
import com.malik.ztesmartmanager.core.profile.RouterProfileRegistry
import com.malik.ztesmartmanager.core.storage.RouterBackupIdentityGuard
import kotlinx.coroutines.delay
import org.json.JSONObject

data class ZteLoginBootstrap(
    val profile: RouterProfile,
    val snapshot: RouterSnapshot
)

private data class AdMaterial(
    val waInnerVersion: String,
    val crVersion: String,
    val rd: String
)

class ZteRouterClient(private val routerAddress: String) {
    private val transport = ZteHttpTransport(routerAddress)
    private var cachedWaInnerVersion: String? = null
    private var cachedCrVersion: String? = null

    var profile: RouterProfile = GenericZteProfile
        private set

    /**
     * Compatibility login path. It verifies the management session with a post-login identity read.
     * The app's interactive login uses loginAndReadSnapshot() to combine that verification with the
     * first telemetry read and save one complete router round-trip.
     */
    suspend fun login(adminPassword: String): RouterProfile {
        authenticate(adminPassword)
        val identity = readRaw(IDENTITY_FIELDS)
        verifyManagementSession(identity)
        profile = resolveProfile(identity)
        return profile
    }

    /**
     * Fast verified bootstrap for the UI: auth GET -> login POST -> one post-login GET that both
     * proves the management session and supplies the first real snapshot. No truth-first check is
     * removed; the previous separate identity + snapshot GETs are simply merged.
     */
    suspend fun loginAndReadSnapshot(adminPassword: String): ZteLoginBootstrap {
        authenticate(adminPassword)
        val bootstrap = readRaw(GenericZteProfile.statusFields)
        verifyManagementSession(bootstrap)
        profile = resolveProfile(bootstrap)
        val first = ZteSnapshotParser.parse(
            json = bootstrap,
            radioIdEncoding = profile.radioIdEncoding
        )
        return ZteLoginBootstrap(profile = profile, snapshot = first)
    }

    private suspend fun authenticate(adminPassword: String) {
        val auth = readRaw(setOf("LD", "wa_inner_version", "cr_version", "RD"))
        val ld = auth.optString("LD").trim()
        if (ld.isBlank()) throw ZteAuthenticationException("لم يُرجع الراوتر قيمة LD المطلوبة للمصادقة")

        val loginParams = linkedMapOf(
            "isTest" to "false",
            "goformId" to "LOGIN",
            "password" to ZteCrypto.loginPassword(adminPassword, ld)
        )

        val wa = exactValue(auth, "wa_inner_version")?.trim().orEmpty()
        val cr = exactValue(auth, "cr_version")?.trim()
        val rd = exactValue(auth, "RD")?.trim().orEmpty()
        if (wa.isNotBlank()) cachedWaInnerVersion = wa
        if (cr != null) cachedCrVersion = cr
        if (wa.isNotBlank() && cr != null && rd.isNotBlank()) {
            loginParams["AD"] = ZteCrypto.adValue(wa, cr, rd)
        }

        val loginRaw = transport.postForm(SET_PATH, loginParams)
        val loginJson = runCatching { JSONObject(loginRaw) }
            .getOrElse { throw ZteAuthenticationException("رد تسجيل الدخول غير صالح") }
        val result = loginJson.optString("result").trim()
        if (result != "0" && !result.equals("success", true)) {
            throw ZteAuthenticationException("رفض الراوتر تسجيل الدخول")
        }
    }

    private fun verifyManagementSession(identity: JSONObject) {
        val logInfo = identity.optString("loginfo").trim()
        if (logInfo.isNotBlank() && !logInfo.equals("ok", true)) {
            throw ZteAuthenticationException("قبل الراوتر الطلب لكن لم يتم إنشاء جلسة إدارة موثقة")
        }
    }

    private fun resolveProfile(identity: JSONObject): RouterProfile {
        val model = firstValue(identity, "device_name", "model_name", "product_name")
        val hardware = identity.optString("hardware_version").takeIf { it.isNotBlank() }
        val firmware = firstValue(identity, "wa_inner_version", "web_version", "cr_version")
        return RouterProfileRegistry.resolve(model, hardware, firmware)
    }

    suspend fun readSnapshot(): RouterSnapshot =
        ZteSnapshotParser.parse(
            json = readRaw(profile.statusFields),
            radioIdEncoding = profile.radioIdEncoding
        )

    suspend fun readRaw(fields: Set<String>): JSONObject = transport.getJson(
        path = GET_PATH,
        params = mapOf(
            "isTest" to "false",
            "cmd" to fields.joinToString(","),
            "multi_data" to "1"
        )
    )

    private suspend fun readSingleRaw(field: String): JSONObject = transport.getJson(
        path = GET_PATH,
        params = mapOf(
            "isTest" to "false",
            "cmd" to field,
            "_" to System.currentTimeMillis().toString()
        )
    )

    /**
     * Reads MC801A AD material without inventing values.
     *
     * The normal path is the documented/observed multi_data query. Some ZTE firmware revisions
     * omit one token from a combined response, so a missing token is retried as a single-field GET.
     * wa_inner_version and RD must be non-blank. cr_version may legitimately be an explicitly
     * returned empty string; missing and empty are deliberately treated differently.
     */
    private suspend fun readAdMaterial(): AdMaterial {
        val combined = readRaw(setOf("wa_inner_version", "cr_version", "RD"))

        var wa = exactValue(combined, "wa_inner_version")?.trim().orEmpty()
        var cr: String? = exactValue(combined, "cr_version")?.trim()
        var rd = exactValue(combined, "RD")?.trim().orEmpty()

        if (wa.isNotBlank()) cachedWaInnerVersion = wa
        if (cr != null) cachedCrVersion = cr

        if (wa.isBlank()) {
            wa = cachedWaInnerVersion.orEmpty()
        }
        if (cr == null) {
            cr = cachedCrVersion
        }

        if (wa.isBlank()) {
            val retry = runCatching { readSingleRaw("wa_inner_version") }.getOrNull()
            val retried = retry?.let { exactValue(it, "wa_inner_version") }?.trim().orEmpty()
            if (retried.isNotBlank()) {
                wa = retried
                cachedWaInnerVersion = retried
            }
        }

        if (cr == null) {
            val retry = runCatching { readSingleRaw("cr_version") }.getOrNull()
            if (retry != null && retry.has("cr_version") && !retry.isNull("cr_version")) {
                cr = retry.optString("cr_version").trim()
                cachedCrVersion = cr
            }
        }

        if (rd.isBlank()) {
            val retry = runCatching { readSingleRaw("RD") }.getOrNull()
            rd = retry?.let { exactValue(it, "RD") }?.trim().orEmpty()
        }

        val missing = buildList {
            if (wa.isBlank()) add("wa_inner_version")
            if (cr == null) add("cr_version")
            if (rd.isBlank()) add("RD")
        }
        if (missing.isNotEmpty()) {
            throw ZteProtocolException(
                "تعذر إنشاء AD: الراوتر لم يوفّر ${missing.joinToString("، ")} حتى بعد إعادة القراءة"
            )
        }

        return AdMaterial(
            waInnerVersion = wa,
            crVersion = cr.orEmpty(),
            rd = rd
        )
    }

    suspend fun readRuntimeCapabilities(): RuntimeCapabilityReport = RuntimeCapabilityProbe.probe(this)

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

    /**
     * Restore only values that were actually exposed in the captured snapshot.
     *
     * The persistent backup is fail-closed: before the first mutating goform, the client performs
     * a fresh read-only identity query and requires the saved router address/profile plus every
     * captured Model/Hardware/Firmware value to match. If identity cannot be proven, no write is sent.
     */
    suspend fun restoreSettings(backup: RouterSettingsBackup): RouterRestoreReport {
        val identity = runCatching { readRaw(IDENTITY_FIELDS) }.getOrElse {
            return restoreBlocked("تعذر قراءة هوية الراوتر قبل الاستعادة (${it.message.orEmpty()})؛ لم يتم إرسال أي أمر كتابة")
        }

        val logInfo = identity.optString("loginfo").trim()
        if (logInfo.isNotBlank() && !logInfo.equals("ok", true)) {
            return restoreBlocked("جلسة الإدارة الحالية غير موثقة أثناء فحص الهوية؛ لم يتم إرسال أي أمر كتابة")
        }

        val currentModel = firstValue(identity, "device_name", "model_name", "product_name")
        val currentHardware = exactValue(identity, "hardware_version")?.trim()?.takeIf { it.isNotBlank() }
        val currentFirmware = firstValue(identity, "wa_inner_version", "web_version", "cr_version")
        val resolvedProfile = RouterProfileRegistry.resolve(currentModel, currentHardware, currentFirmware)

        if (resolvedProfile.id != profile.id) {
            return restoreBlocked(
                "هوية الراوتر المقروءة الآن تحل إلى Profile ${resolvedProfile.id} بينما الجلسة الحالية ${profile.id}; أُوقفت الاستعادة قبل أي كتابة"
            )
        }

        val identityDecision = RouterBackupIdentityGuard.verify(
            backup = backup,
            currentRouterAddress = routerAddress,
            currentProfileId = resolvedProfile.id,
            currentModel = currentModel,
            currentHardwareVersion = currentHardware,
            currentFirmware = currentFirmware
        )
        if (!identityDecision.allowed) {
            return restoreBlocked(identityDecision.reason)
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
                steps += RouterRestoreStep("Cell lock", clearCellLockInternal())
            else -> {
                val pciNumber = pci.toIntOrNull()
                val earfcnNumber = earfcn.toIntOrNull()
                val result = if (pciNumber != null && earfcnNumber != null) {
                    setCellLockInternal(pciNumber, earfcnNumber)
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
        preflight("تثبيت ترددات 4G") { it.lteBandControl }?.let { return it }
        return restoreLteMask(BandEncoding.lteMask(bands), successMessage = "تم تثبيت ترددات 4G والتحقق من القناع")
    }

    suspend fun setNrBands(bands: Set<Int>): OperationResult {
        if (!profile.capabilities.supportsNrBandLock) return unsupported("تثبيت ترددات 5G")
        if (bands.isEmpty() || !profile.capabilities.supportedNrBands.containsAll(bands)) {
            return OperationResult(false, false, "اختيار 5G يحتوي ترددًا غير مدعوم لهذا الـProfile")
        }
        preflight("تثبيت ترددات 5G") { it.nrBandControl }?.let { return it }
        return restoreNrMask(BandEncoding.nrMask(bands), expectedBands = bands, successMessage = "تم تثبيت ترددات 5G والتحقق منها")
    }

    suspend fun setCellLock(pci: Int, earfcn: Int): OperationResult {
        if (!profile.capabilities.supportsCellLock) return unsupported("تثبيت الخلية")
        if (pci !in 0..503 || earfcn <= 0) return OperationResult(false, false, "PCI LTE يجب أن يكون 0..503 وEARFCN أكبر من صفر")
        preflight("تثبيت الخلية") { it.cellLock }?.let { return it }
        return setCellLockInternal(pci, earfcn)
    }

    private suspend fun setCellLockInternal(pci: Int, earfcn: Int): OperationResult {
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
            message = if (verified) "تم حفظ PCI/EARFCN والتحقق منهما" else "قبل الراوتر أمر القفل، لكن لم يعطِ read-back مطابقًا؛ لذلك لن يعتبره التطبيق قفلًا مؤكدًا ولن يشغّل Tower Guard",
            rawResult = raw
        )
    }

    suspend fun clearCellLock(): OperationResult {
        if (!profile.capabilities.supportsCellLock) return unsupported("إزالة تثبيت الخلية")
        preflight("إزالة تثبيت الخلية") { it.cellLock }?.let { return it }
        return clearCellLockInternal()
    }

    private suspend fun clearCellLockInternal(): OperationResult {
        val raw = writeWithAd(
            goformId = "LTE_LOCK_CELL_SET",
            values = mapOf("lte_pci_lock" to "", "lte_earfcn_lock" to "")
        )
        if (!commandAccepted(raw)) return OperationResult(false, false, "الراوتر رفض إزالة Cell Lock", raw)

        delay(700)
        val readBack = readRaw(setOf("lte_pci_lock", "lte_earfcn_lock"))
        val verified = isUnlockedValue(readBack.optString("lte_pci_lock")) &&
            readBack.optString("lte_earfcn_lock").let(::isUnlockedValue)
        return OperationResult(
            success = verified,
            verified = verified,
            message = if (verified) "تمت إزالة Cell Lock والتحقق من القيم" else "أُرسل أمر الإزالة لكن الراوتر لم يؤكد فراغ/صفر القفل",
            rawResult = raw
        )
    }

    suspend fun setNetworkMode(mode: String): OperationResult {
        if (mode !in NETWORK_MODES) return OperationResult(false, false, "وضع شبكة غير معروف")
        preflight("تغيير وضع الشبكة") { it.networkMode }?.let { return it }
        return setNetworkModeRaw(mode)
    }

    suspend fun setAntennaState(state: Int): OperationResult {
        if (!profile.capabilities.supportsAntennaControl) return unsupported("التحكم بالهوائي")
        if (state !in 1..3) return OperationResult(false, false, "حالة الهوائي يجب أن تكون 1 أو 2 أو 3")
        return OperationResult(
            success = false,
            verified = false,
            message = "تم إيقاف أمر الهوائي: هذا الـProfile يعرف الأمر لكن لا يوجد read-back موثوق يثبت النتيجة أو يسمح بالرجوع الآمن"
        )
    }

    private suspend fun preflight(
        feature: String,
        selector: (RuntimeCapabilityReport) -> RuntimeCapabilityEvidence
    ): OperationResult? {
        val report = runCatching { readRuntimeCapabilities() }.getOrElse {
            return OperationResult(
                false,
                false,
                "تم إيقاف $feature: تعذر تنفيذ capability probe القراءة فقط (${it.message.orEmpty()})"
            )
        }
        val evidence = selector(report)
        if (evidence.canAttemptWrite) return null
        return OperationResult(
            success = false,
            verified = false,
            message = "تم إيقاف $feature قبل إرسال أي أمر: ${evidence.reason}"
        )
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

        var lastActual: String? = null
        repeat(NETWORK_MODE_VERIFY_ATTEMPTS) { attempt ->
            delay(if (attempt == 0) 700 else 500)
            val readBack = runCatching { readRaw(setOf("BearerPreference")) }.getOrNull()
            val actual = readBack?.let { exactValue(it, "BearerPreference")?.trim() }
            if (actual != null) lastActual = actual
            if (NetworkModeReadBackVerifier.matches(mode, actual)) {
                return OperationResult(
                    success = true,
                    verified = true,
                    message = "تم تطبيق وضع الشبكة والتحقق من BearerPreference",
                    rawResult = raw
                )
            }
        }

        return OperationResult(
            success = false,
            verified = false,
            message = if (lastActual == null) {
                "قبل الراوتر أمر وضع الشبكة، لكن لم نحصل على BearerPreference مطابق بعد عدة قراءات؛ لن تعتبر العملية ناجحة"
            } else {
                "قبل الراوتر الأمر لكن آخر BearerPreference مقروء ($lastActual) لا يطابق الوضع المطلوب؛ لن تعتبر العملية ناجحة"
            },
            rawResult = raw
        )
    }

    private fun restoreBlocked(reason: String): RouterRestoreReport = RouterRestoreReport(
        listOf(
            RouterRestoreStep(
                "Backup identity",
                OperationResult(false, false, reason)
            )
        )
    )

    private fun resolveRestorableNrMask(backup: RouterSettingsBackup): String? {
        backup.nrBandLock?.let { return it }
        val split = listOfNotNull(backup.nrSaBandLock, backup.nrNsaBandLock)
        if (split.isEmpty()) return null
        val normalized = split.map { normalizeBandList(it) }.distinct()
        return if (normalized.size == 1) split.first() else NR_CONFLICT
    }

    private suspend fun writeWithAd(goformId: String, values: Map<String, String>): String {
        val auth = readAdMaterial()
        val ad = ZteCrypto.adValue(auth.waInnerVersion, auth.crVersion, auth.rd)
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
        private const val NETWORK_MODE_VERIFY_ATTEMPTS = 3

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
