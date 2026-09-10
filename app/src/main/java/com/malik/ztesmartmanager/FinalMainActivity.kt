package com.malik.ztesmartmanager

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.malik.ztesmartmanager.core.model.RouterSettingsBackup
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.presentation.RouterConnectionMessage
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.NetworkPerformanceProbe
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartBandOptimizer
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.storage.RouterBackupStore
import com.malik.ztesmartmanager.core.storage.SecureRouterCredentialStore
import com.malik.ztesmartmanager.core.storage.TowerFingerprintStore
import com.malik.ztesmartmanager.core.storage.TowerGuardStateStore
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.PersistedTowerGuardState
import com.malik.ztesmartmanager.core.tower.TowerGuardResumeVerifier
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerLockEngine
import com.malik.ztesmartmanager.core.tower.TowerMatch
import com.malik.ztesmartmanager.core.tower.TowerRecommendationEngine
import com.malik.ztesmartmanager.core.tower.TowerTarget
import com.malik.ztesmartmanager.core.tower.VerifiedTowerLockCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FinalMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = PremiumLightColors) {
                    Surface(modifier = Modifier, color = PremiumBackground) {
                        FinalManagerApp()
                    }
                }
            }
        }
    }
}

private const val FINAL_SMART_COOLDOWN_MS = 15 * 60 * 1000L

@Composable
private fun FinalManagerApp() {
    var routerAddress by rememberSaveable { mutableStateOf("192.168.0.1") }
    var password by remember { mutableStateOf("") }
    var client by remember { mutableStateOf<ZteRouterClient?>(null) }
    var towerEngine by remember { mutableStateOf<TowerLockEngine?>(null) }
    var snapshot by remember { mutableStateOf<RouterSnapshot?>(null) }
    var status by remember { mutableStateOf("غير متصل") }
    var operationMessage by remember { mutableStateOf("") }
    var connectBusy by remember { mutableStateOf(false) }
    var controlBusy by remember { mutableStateOf(false) }

    var placementMode by rememberSaveable { mutableStateOf(false) }
    var placementReading by remember { mutableStateOf<PlacementReading?>(null) }
    val placementEngine = remember { NetworkQualityEngine() }
    val monitorScorer = remember { NetworkQualityEngine() }

    var smartMode by rememberSaveable { mutableStateOf(false) }
    var smartGoal by remember { mutableStateOf(OptimizationGoal.BALANCED) }
    var smartBusy by remember { mutableStateOf(false) }
    var smartReport by remember { mutableStateOf<SmartOptimizationReport?>(null) }
    var smartBaseline by remember { mutableStateOf<Int?>(null) }
    var poorSamples by remember { mutableStateOf(0) }
    var lastSmartRun by remember { mutableStateOf(0L) }

    var selectedLte by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedNr by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var speedBusy by remember { mutableStateOf(false) }
    var lastPerformance by remember { mutableStateOf<NetworkPerformance?>(null) }
    val performanceProbe = remember { NetworkPerformanceProbe() }

    var scanBusy by remember { mutableStateOf(false) }
    var nearbyCells by remember { mutableStateOf<List<NearbyCell>>(emptyList()) }
    var towerTarget by remember { mutableStateOf<TowerTarget?>(null) }
    var towerGuardEnabled by rememberSaveable { mutableStateOf(false) }
    var towerGuardStatus by remember { mutableStateOf<TowerGuardStatus?>(null) }

    val context = LocalContext.current
    val backupStore = remember(context) { RouterBackupStore(context) }
    val fingerprintStore = remember(context) { TowerFingerprintStore(context) }
    val guardStateStore = remember(context) { TowerGuardStateStore(context) }
    val credentialStore = remember(context) { SecureRouterCredentialStore(context) }
    var rememberPassword by rememberSaveable { mutableStateOf(false) }
    var credentialsLoaded by remember { mutableStateOf(false) }
    var latestSafetyBackup by remember { mutableStateOf<RouterSettingsBackup?>(null) }
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(credentialStore) {
        if (!credentialsLoaded) {
            credentialStore.load()?.let { saved ->
                routerAddress = saved.routerAddress
                password = saved.password
                rememberPassword = true
            }
            credentialsLoaded = true
        }
    }

    suspend fun captureSafetyBackup(
        connected: ZteRouterClient,
        canRestore: (RouterSettingsBackup) -> Boolean,
        unavailableMessage: String
    ): RouterSettingsBackup? {
        operationMessage = "حفظ نسخة أمان قبل التغيير..."
        val backup = runCatching { connected.captureSettingsBackup(routerAddress) }
            .getOrElse {
                operationMessage = "تم إيقاف التغيير: تعذر حفظ نسخة أمان (${it.message.orEmpty()})"
                return null
            }
        if (!canRestore(backup)) {
            operationMessage = "تم إيقاف التغيير: $unavailableMessage"
            return null
        }
        backupStore.save(backup)
        latestSafetyBackup = backup
        return backup
    }

    suspend fun refreshAfterControl(connected: ZteRouterClient) {
        delay(900)
        snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
        selectedLte = snapshot?.let(::premiumCurrentLteBands).orEmpty()
        selectedNr = snapshot?.let(::premiumCurrentNrBands).orEmpty()
    }

    fun connect() {
        if (connectBusy || password.isBlank() || routerAddress.isBlank()) return
        scope.launch {
            connectBusy = true
            status = "جاري الاتصال..."
            runCatching {
                val newClient = ZteRouterClient(routerAddress)
                val profile = newClient.login(password)
                val first = newClient.readSnapshot()
                val newTowerEngine = TowerLockEngine(newClient)
                client = newClient
                towerEngine = newTowerEngine
                snapshot = first
                selectedLte = premiumCurrentLteBands(first)
                selectedNr = premiumCurrentNrBands(first)
                latestSafetyBackup = backupStore.latest(routerAddress)
                nearbyCells = emptyList()
                towerTarget = null
                towerGuardEnabled = false
                towerGuardStatus = null
                placementEngine.reset()
                placementReading = placementEngine.add(first)
                smartBaseline = monitorScorer.score(first).total
                poorSamples = 0

                if (rememberPassword) {
                    if (!credentialStore.save(routerAddress, password)) {
                        operationMessage = "تم الاتصال، لكن تعذر حفظ كلمة المرور بشكل آمن على هذا الجهاز"
                    }
                } else {
                    credentialStore.clear()
                }

                val savedGuardState = guardStateStore.load(routerAddress)
                val lockReadBack = if (savedGuardState != null) {
                    runCatching { newClient.readRaw(setOf("lte_pci_lock", "lte_earfcn_lock")) }.getOrNull()
                } else null
                val liveMatch = savedGuardState?.let { newTowerEngine.compare(it.target, first) }
                val resumeDecision = TowerGuardResumeVerifier.decide(
                    saved = savedGuardState,
                    currentRouterAddress = routerAddress,
                    currentProfileId = profile.id,
                    configuredPci = lockReadBack?.optString("lte_pci_lock"),
                    configuredEarfcn = lockReadBack?.optString("lte_earfcn_lock"),
                    liveMatch = liveMatch
                )
                if (resumeDecision.discardPersistedState) guardStateStore.clear(routerAddress)
                towerTarget = resumeDecision.target
                towerGuardEnabled = resumeDecision.enableGuard
                towerGuardStatus = resumeDecision.target?.let { target ->
                    TowerGuardStatus(
                        target = target,
                        match = liveMatch ?: TowerMatch.UNKNOWN,
                        consecutiveDriftSamples = 0,
                        repaired = false,
                        message = resumeDecision.message
                    )
                }

                val baseStatus = "متصل • ${profile.capabilities.modelFamily}"
                if (savedGuardState != null) {
                    status = "$baseStatus • ${resumeDecision.message}"
                    operationMessage = resumeDecision.message
                } else {
                    val savedFingerprint = fingerprintStore.latest(routerAddress, profile.id)
                    val fingerprintMatch = savedFingerprint?.let { newTowerEngine.compare(it.toTarget(), first) }
                    status = when (fingerprintMatch) {
                        TowerMatch.MATCHED -> "$baseStatus • على البصمة المحفوظة"
                        TowerMatch.RADIO_MATCH_ID_CHANGED -> "$baseStatus • PCI/EARFCN يطابقان بصمة محفوظة لكن هوية الخلية تغيّرت"
                        TowerMatch.DRIFTED -> "$baseStatus • توجد بصمة خلية محفوظة غير نشطة الآن"
                        TowerMatch.UNKNOWN -> "$baseStatus • توجد بصمة محفوظة لكن لا يمكن التحقق منها الآن"
                        null -> baseStatus
                    }
                }
            }.onFailure {
                client = null
                towerEngine = null
                snapshot = null
                towerTarget = null
                towerGuardEnabled = false
                towerGuardStatus = null
                status = RouterConnectionMessage.from(it, routerAddress)
            }
            connectBusy = false
        }
    }

    fun runSmartOptimization(manual: Boolean) {
        val connected = client ?: return
        if (smartBusy || controlBusy) return
        scope.launch {
            smartBusy = true
            val backup = captureSafetyBackup(
                connected,
                canRestore = { it.canRestoreLteBands },
                unavailableMessage = "قناع LTE الأصلي غير ظاهر بشكل يسمح باستعادته حرفيًا"
            )
            if (backup == null) {
                smartBusy = false
                return@launch
            }

            operationMessage = if (manual) "جاري اختبار أفضل إعداد موثّق..." else "رصدنا تدهورًا مستمرًا؛ بدأ التحسين الموثّق..."
            runCatching {
                SmartBandOptimizer(connected).optimizeOnce(smartGoal) { operationMessage = it }
            }.onSuccess { report ->
                smartReport = report
                operationMessage = report.message
                smartBaseline = report.best.qualityScore
                lastSmartRun = SystemClock.elapsedRealtime()
                refreshAfterControl(connected)
            }.onFailure {
                operationMessage = "تعذر التحسين: ${it.message.orEmpty()}"
                lastSmartRun = SystemClock.elapsedRealtime()
            }
            poorSamples = 0
            smartBusy = false
        }
    }

    fun requestVerifiedCellLock(candidate: NearbyCell) {
        val connected = client ?: return
        val engine = towerEngine ?: return
        if (controlBusy || scanBusy || smartBusy) return

        scope.launch {
            controlBusy = true
            towerGuardEnabled = false
            guardStateStore.setGuardRequested(routerAddress, false)

            val backup = captureSafetyBackup(
                connected,
                canRestore = { it.hasCompleteCellLockState },
                unavailableMessage = "حالة Cell Lock الأصلية غير مكتملة؛ لن نرسل قفلًا دون مسار رجوع"
            )
            if (backup == null) {
                controlBusy = false
                return@launch
            }

            val removalVerified = if (backup.cellWasUnlocked) {
                runCatching { connected.clearCellLock() }.getOrNull()?.verified == true
            } else true
            if (!removalVerified) {
                operationMessage = "تم إيقاف القفل: الـFirmware لم يثبت أن إزالة Cell Lock تعمل على هذا الجهاز"
                controlBusy = false
                return@launch
            }

            operationMessage = "إعادة التحقق من الخلية المختارة قبل أي أمر قفل..."
            val result = runCatching {
                VerifiedTowerLockCoordinator(connected, engine, routerAddress).lock(candidate)
            }.getOrElse {
                operationMessage = "تعذر فحص/تثبيت الخلية: ${it.message.orEmpty()}"
                controlBusy = false
                return@launch
            }

            if (result.success && result.target != null && result.fingerprint != null) {
                towerTarget = result.target
                towerGuardStatus = TowerGuardStatus(
                    target = result.target,
                    match = TowerMatch.MATCHED,
                    consecutiveDriftSamples = 0,
                    repaired = false,
                    message = result.message
                )
                fingerprintStore.save(result.fingerprint)
                guardStateStore.saveVerifiedTarget(
                    routerAddress = routerAddress,
                    profileId = connected.profile.id,
                    target = result.target,
                    guardRequested = false
                )
                operationMessage = "${result.message} • يمكنك تشغيل حارس البرج الآن"
            } else {
                towerTarget = null
                towerGuardEnabled = false
                towerGuardStatus = null

                operationMessage = if (result.writeAttempted) {
                    val rollback = runCatching { connected.restoreSettings(backup) }.getOrNull()
                    if (rollback?.verified != true) guardStateStore.clear(routerAddress)
                    when {
                        rollback?.verified == true -> "${result.message} • أُعيدت إعدادات ما قبل المحاولة وتم التحقق منها"
                        rollback != null -> "${result.message} • محاولة الرجوع: ${rollback.message}"
                        else -> "${result.message} • تعذر تنفيذ مسار الرجوع؛ راجع الإعدادات قبل محاولة جديدة"
                    }
                } else {
                    result.message
                }
            }

            refreshAfterControl(connected)
            controlBusy = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(client, placementMode, smartMode, towerGuardEnabled, towerTarget) {
        val connected = client ?: return@LaunchedEffect
        val guardEngine = towerEngine
        if (placementMode) {
            placementEngine.reset()
            snapshot?.let { placementReading = placementEngine.add(it) }
        }

        while (client === connected) {
            delay(if (placementMode) 650 else 2_000)
            if (controlBusy || smartBusy || scanBusy) continue
            runCatching { connected.readSnapshot() }
                .onSuccess { latest ->
                    snapshot = latest

                    if (towerGuardEnabled && towerTarget != null && guardEngine != null) {
                        runCatching { guardEngine.guardOnce(towerTarget!!, latest) }
                            .onSuccess { guard ->
                                towerGuardStatus = guard
                                if (guard.repaired || guard.match == TowerMatch.RADIO_MATCH_ID_CHANGED) {
                                    operationMessage = guard.message
                                }
                            }
                    }

                    if (placementMode) {
                        placementReading = placementEngine.add(latest)
                    } else if (smartMode) {
                        val quality = monitorScorer.score(latest).total
                        val baseline = smartBaseline ?: quality.also { smartBaseline = it }
                        if (quality > baseline) smartBaseline = quality
                        if ((smartBaseline ?: quality) - quality >= 12) poorSamples++ else poorSamples = 0
                        val cooldownDone = SystemClock.elapsedRealtime() - lastSmartRun >= FINAL_SMART_COOLDOWN_MS
                        if (poorSamples >= 5 && cooldownDone) runSmartOptimization(manual = false)
                    }
                }
                .onFailure { status = "انقطع التحديث مؤقتًا: ${it.message.orEmpty()}" }
        }
    }

    val connected = client
    if (connected == null) {
        ZteRouterLoginScreen(
            routerAddress = routerAddress,
            onRouterAddressChange = { routerAddress = it },
            password = password,
            onPasswordChange = { password = it },
            rememberPassword = rememberPassword,
            onRememberPasswordChange = { checked ->
                rememberPassword = checked
                if (!checked) credentialStore.clear()
            },
            status = status,
            busy = connectBusy,
            onConnect = ::connect
        )
        return
    }

    val capabilities = connected.profile.capabilities

    RuntimeAwareFinalDashboard(
        snapshot = snapshot,
        capabilities = capabilities,
        status = status,
        operationMessage = operationMessage,
        placementMode = placementMode,
        placementReading = placementReading,
        smartMode = smartMode,
        smartGoal = smartGoal,
        smartBusy = smartBusy,
        smartReport = smartReport,
        selectedLte = selectedLte,
        selectedNr = selectedNr,
        controlBusy = controlBusy,
        speedBusy = speedBusy,
        lastPerformance = lastPerformance,
        safetyBackupAvailable = latestSafetyBackup != null,
        nearbyCells = nearbyCells,
        scanBusy = scanBusy,
        towerTarget = towerTarget,
        towerGuardEnabled = towerGuardEnabled,
        towerGuardStatus = towerGuardStatus,
        onDisconnect = {
            client = null
            towerEngine = null
            snapshot = null
            placementMode = false
            smartMode = false
            placementReading = null
            smartReport = null
            lastPerformance = null
            nearbyCells = emptyList()
            towerTarget = null
            towerGuardEnabled = false
            towerGuardStatus = null
            status = "غير متصل"
        },
        onSpeedTest = {
            if (!speedBusy) scope.launch {
                speedBusy = true
                operationMessage = "جاري قياس السرعة الفعلية..."
                runCatching { performanceProbe.measure(includeDownload = true) }
                    .onSuccess {
                        lastPerformance = it
                        operationMessage = "اكتمل القياس الفعلي"
                    }
                    .onFailure { operationMessage = "تعذر القياس: ${it.message.orEmpty()}" }
                speedBusy = false
            }
        },
        onPlacementToggle = {
            placementMode = !placementMode
            if (!placementMode) placementReading = null
        },
        onSmartModeChange = {
            smartMode = it
            poorSamples = 0
            smartBaseline = snapshot?.let { s -> monitorScorer.score(s).total }
        },
        onSmartGoalChange = { smartGoal = it },
        onOptimizeNow = { runSmartOptimization(true) },
        onLteToggle = { band -> selectedLte = premiumToggleBand(selectedLte, band) },
        onNrToggle = { band -> selectedNr = premiumToggleBand(selectedNr, band) },
        onApplyLte = {
            if (selectedLte.isNotEmpty() && !controlBusy) scope.launch {
                controlBusy = true
                val backup = captureSafetyBackup(
                    connected,
                    { it.canRestoreLteBands },
                    "لا يوجد LTE mask أصلي موثوق يمكن استعادته؛ لم يتم تغيير الترددات"
                )
                if (backup != null) {
                    operationMessage = connected.setLteBands(selectedLte).message
                    refreshAfterControl(connected)
                }
                controlBusy = false
            }
        },
        onApplyNr = {
            if (selectedNr.isNotEmpty() && !controlBusy) scope.launch {
                controlBusy = true
                val backup = captureSafetyBackup(
                    connected,
                    { it.canRestoreNrBands },
                    "قناع 5G الأصلي غير متاح بشكل يسمح باستعادته دون تخمين"
                )
                if (backup != null) {
                    operationMessage = connected.setNrBands(selectedNr).message
                    refreshAfterControl(connected)
                }
                controlBusy = false
            }
        },
        onSetNetworkMode = { mode ->
            if (!controlBusy) scope.launch {
                controlBusy = true
                val backup = captureSafetyBackup(
                    connected,
                    { it.canRestoreNetworkMode },
                    "BearerPreference الأصلي غير ظاهر؛ لن نغيّر وضع الشبكة دون مسار رجوع"
                )
                if (backup != null) {
                    operationMessage = connected.setNetworkMode(mode).message
                    refreshAfterControl(connected)
                }
                controlBusy = false
            }
        },
        onAntennaState = { state ->
            if (!controlBusy) scope.launch {
                controlBusy = true
                operationMessage = connected.setAntennaState(state).message
                controlBusy = false
            }
        },
        onScanCells = {
            val engine = towerEngine
            if (engine != null && !scanBusy && !controlBusy) scope.launch {
                scanBusy = true
                operationMessage = "جاري أخذ عدة قراءات فعلية للخلايا..."
                runCatching { engine.scanNearbyCells() }
                    .onSuccess { report ->
                        nearbyCells = report.cells
                        val recommendation = TowerRecommendationEngine.recommend(
                            cells = report.cells,
                            currentPci = snapshot?.pci,
                            currentArfcn = snapshot?.earfcn
                        )
                        operationMessage = "${report.message} • ${recommendation.reason}"
                    }
                    .onFailure { operationMessage = "تعذر مسح الخلايا: ${it.message.orEmpty()}" }
                scanBusy = false
            }
        },
        onLockCurrentCell = {
            val current = snapshot
            if (current?.pci != null && current.earfcn != null) {
                requestVerifiedCellLock(
                    NearbyCell(
                        rat = "LTE",
                        band = current.lteBand,
                        pci = current.pci,
                        arfcn = current.earfcn,
                        rsrp = current.lteRsrp,
                        rsrq = current.lteRsrq,
                        sinr = current.lteSinr
                    )
                )
            }
        },
        onLockNearbyCell = { cell -> requestVerifiedCellLock(cell) },
        onClearCellLock = {
            if (!controlBusy) scope.launch {
                controlBusy = true
                towerGuardEnabled = false
                guardStateStore.setGuardRequested(routerAddress, false)
                val backup = captureSafetyBackup(
                    connected,
                    { it.hasCompleteCellLockState },
                    "حالة القفل الحالية غير مكتملة؛ لم يتم إرسال أمر إزالة"
                )
                if (backup != null) {
                    val result = connected.clearCellLock()
                    operationMessage = result.message
                    if (result.verified) {
                        towerTarget = null
                        towerGuardEnabled = false
                        towerGuardStatus = null
                        guardStateStore.clear(routerAddress)
                    }
                    refreshAfterControl(connected)
                }
                controlBusy = false
            }
        },
        onTowerGuardChange = { enabled ->
            if (!enabled) {
                towerGuardEnabled = false
                guardStateStore.setGuardRequested(routerAddress, false)
                operationMessage = "تم إيقاف حارس البرج • قفل الراوتر نفسه لم يتغير"
            } else {
                val target = towerTarget
                val engine = towerEngine
                if (target == null || engine == null) {
                    operationMessage = "ثبّت خلية وتحقق منها أولًا قبل تشغيل حارس البرج"
                } else if (!controlBusy) {
                    scope.launch {
                        operationMessage = "إعادة التحقق من القفل والخلية قبل تشغيل حارس البرج..."
                        val lockReadBack = runCatching {
                            connected.readRaw(setOf("lte_pci_lock", "lte_earfcn_lock"))
                        }.getOrNull()
                        val latest = runCatching { connected.readSnapshot() }.getOrNull()
                        if (latest != null) snapshot = latest
                        val liveMatch = latest?.let { engine.compare(target, it) }
                        val candidateState = PersistedTowerGuardState(
                            routerAddress = routerAddress,
                            profileId = connected.profile.id,
                            target = target,
                            guardRequested = true,
                            savedAtEpochMs = System.currentTimeMillis()
                        )
                        val decision = TowerGuardResumeVerifier.decide(
                            saved = candidateState,
                            currentRouterAddress = routerAddress,
                            currentProfileId = connected.profile.id,
                            configuredPci = lockReadBack?.optString("lte_pci_lock"),
                            configuredEarfcn = lockReadBack?.optString("lte_earfcn_lock"),
                            liveMatch = liveMatch
                        )

                        if (decision.discardPersistedState) {
                            guardStateStore.clear(routerAddress)
                        } else if (decision.enableGuard) {
                            guardStateStore.save(candidateState)
                        } else if (decision.target != null) {
                            guardStateStore.saveVerifiedTarget(
                                routerAddress,
                                connected.profile.id,
                                decision.target,
                                false
                            )
                        } else {
                            guardStateStore.setGuardRequested(routerAddress, false)
                        }

                        towerTarget = decision.target
                        towerGuardEnabled = decision.enableGuard
                        towerGuardStatus = decision.target?.let { verifiedTarget ->
                            TowerGuardStatus(
                                target = verifiedTarget,
                                match = liveMatch ?: TowerMatch.UNKNOWN,
                                consecutiveDriftSamples = 0,
                                repaired = false,
                                message = decision.message
                            )
                        }
                        operationMessage = if (decision.enableGuard) {
                            "${decision.message} • المراقبة تعمل فقط أثناء تشغيل التطبيق"
                        } else {
                            decision.message
                        }
                    }
                }
            }
        },
        onRestoreSafetyBackup = {
            val backup = latestSafetyBackup ?: backupStore.latest(routerAddress)
            if (backup != null && !controlBusy) scope.launch {
                controlBusy = true
                towerGuardEnabled = false
                guardStateStore.setGuardRequested(routerAddress, false)
                operationMessage = "جاري استعادة آخر نسخة أمان والتحقق..."
                val report = runCatching { connected.restoreSettings(backup) }
                    .getOrElse {
                        operationMessage = "تعذر تنفيذ الاستعادة: ${it.message.orEmpty()}"
                        controlBusy = false
                        return@launch
                    }
                operationMessage = report.message
                if (report.verified) {
                    towerTarget = null
                    towerGuardStatus = null
                    guardStateStore.clear(routerAddress)
                }
                refreshAfterControl(connected)
                controlBusy = false
            }
        }
    )
}
