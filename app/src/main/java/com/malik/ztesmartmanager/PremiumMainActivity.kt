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
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.NetworkPerformanceProbe
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartBandOptimizer
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.storage.RouterBackupStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PremiumMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = PremiumLightColors) {
                    Surface(modifier = Modifier, color = PremiumBackground) {
                        PremiumManagerApp()
                    }
                }
            }
        }
    }
}

private const val PREMIUM_SMART_COOLDOWN_MS = 15 * 60 * 1000L

@Composable
private fun PremiumManagerApp() {
    var routerAddress by rememberSaveable { mutableStateOf("192.168.0.1") }
    var password by rememberSaveable { mutableStateOf("") }
    var client by remember { mutableStateOf<ZteRouterClient?>(null) }
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
    val context = LocalContext.current
    val backupStore = remember(context) { RouterBackupStore(context) }
    var latestSafetyBackup by remember { mutableStateOf<RouterSettingsBackup?>(null) }
    val scope = rememberCoroutineScope()

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
        delay(800)
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
                client = newClient
                snapshot = first
                selectedLte = premiumCurrentLteBands(first)
                selectedNr = premiumCurrentNrBands(first)
                latestSafetyBackup = backupStore.latest(routerAddress)
                placementEngine.reset()
                placementReading = placementEngine.add(first)
                smartBaseline = monitorScorer.score(first).total
                poorSamples = 0
                status = "متصل • ${profile.capabilities.modelFamily}"
            }.onFailure {
                client = null
                snapshot = null
                latestSafetyBackup = null
                status = it.message ?: "تعذر الاتصال بالراوتر"
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
                unavailableMessage = "الراوتر لم يعرض قناع LTE أصليًا غير فارغ يمكن إرجاعه حرفيًا"
            )
            if (backup == null) {
                smartBusy = false
                return@launch
            }

            operationMessage = if (manual) "جاري البحث عن أفضل إعداد للشبكة..." else "رصدنا تدهورًا مستمرًا؛ بدأ التحسين الذكي..."
            runCatching {
                SmartBandOptimizer(connected).optimizeOnce(smartGoal) { operationMessage = it }
            }.onSuccess { report ->
                smartReport = report
                operationMessage = report.message
                smartBaseline = report.best.qualityScore
                lastSmartRun = SystemClock.elapsedRealtime()
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                selectedLte = snapshot?.let(::premiumCurrentLteBands).orEmpty()
                selectedNr = snapshot?.let(::premiumCurrentNrBands).orEmpty()
            }.onFailure {
                operationMessage = "تعذر التحسين: ${it.message.orEmpty()}"
                lastSmartRun = SystemClock.elapsedRealtime()
            }
            poorSamples = 0
            smartBusy = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(client, placementMode, smartMode) {
        val connected = client ?: return@LaunchedEffect
        if (placementMode) {
            placementEngine.reset()
            snapshot?.let { placementReading = placementEngine.add(it) }
        }

        while (client === connected) {
            delay(if (placementMode) 650 else 2_000)
            if (controlBusy || smartBusy) continue
            runCatching { connected.readSnapshot() }
                .onSuccess { latest ->
                    snapshot = latest
                    if (placementMode) {
                        placementReading = placementEngine.add(latest)
                    } else if (smartMode) {
                        val quality = monitorScorer.score(latest).total
                        val baseline = smartBaseline ?: quality.also { smartBaseline = it }
                        if (quality > baseline) smartBaseline = quality
                        if ((smartBaseline ?: quality) - quality >= 12) poorSamples++ else poorSamples = 0

                        val cooldownDone = SystemClock.elapsedRealtime() - lastSmartRun >= PREMIUM_SMART_COOLDOWN_MS
                        if (poorSamples >= 5 && cooldownDone) runSmartOptimization(manual = false)
                    }
                }
                .onFailure { status = "انقطع التحديث مؤقتًا: ${it.message.orEmpty()}" }
        }
    }

    val connected = client
    if (connected == null) {
        PremiumLoginScreen(
            routerAddress = routerAddress,
            onRouterAddressChange = { routerAddress = it },
            password = password,
            onPasswordChange = { password = it },
            status = status,
            busy = connectBusy,
            onConnect = ::connect
        )
        return
    }

    val capabilities = connected.profile.capabilities

    PremiumDashboard(
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
        onDisconnect = {
            client = null
            snapshot = null
            placementMode = false
            smartMode = false
            placementReading = null
            smartReport = null
            lastPerformance = null
            latestSafetyBackup = null
            status = "غير متصل"
        },
        onSpeedTest = {
            if (!speedBusy) scope.launch {
                speedBusy = true
                operationMessage = "جاري قياس السرعة..."
                runCatching { performanceProbe.measure(includeDownload = true) }
                    .onSuccess {
                        lastPerformance = it
                        operationMessage = "اكتمل قياس الشبكة"
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
        onOptimizeNow = { runSmartOptimization(manual = true) },
        onLteToggle = { band -> selectedLte = premiumToggleBand(selectedLte, band) },
        onNrToggle = { band -> selectedNr = premiumToggleBand(selectedNr, band) },
        onApplyLte = {
            if (selectedLte.isNotEmpty() && !controlBusy) scope.launch {
                controlBusy = true
                val backup = captureSafetyBackup(
                    connected,
                    canRestore = { it.canRestoreLteBands },
                    unavailableMessage = "لا يوجد LTE mask أصلي موثوق يمكن استعادته؛ لم يتم تغيير الترددات"
                )
                if (backup != null) {
                    operationMessage = connected.setLteBands(selectedLte).message
                    refreshAfterControl(connected)
                }
                controlBusy = false
            }
        },
        onAllowAllLte = {
            if (!controlBusy) scope.launch {
                controlBusy = true
                val backup = captureSafetyBackup(
                    connected,
                    canRestore = { it.canRestoreLteBands },
                    unavailableMessage = "لا يوجد LTE mask أصلي موثوق يمكن استعادته؛ لم يتم تغيير الترددات"
                )
                if (backup != null) {
                    selectedLte = capabilities.supportedLteBands
                    operationMessage = connected.setLteBands(capabilities.supportedLteBands).message
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
                    canRestore = { it.canRestoreNrBands },
                    unavailableMessage = "قناع 5G الأصلي غير متاح بشكل يسمح باستعادته دون تخمين"
                )
                if (backup != null) {
                    operationMessage = connected.setNrBands(selectedNr).message
                    refreshAfterControl(connected)
                }
                controlBusy = false
            }
        },
        onAllowAllNr = {
            if (!controlBusy) scope.launch {
                controlBusy = true
                val backup = captureSafetyBackup(
                    connected,
                    canRestore = { it.canRestoreNrBands },
                    unavailableMessage = "قناع 5G الأصلي غير متاح بشكل يسمح باستعادته دون تخمين"
                )
                if (backup != null) {
                    selectedNr = capabilities.supportedNrBands
                    operationMessage = connected.setNrBands(capabilities.supportedNrBands).message
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
                    canRestore = { it.canRestoreNetworkMode },
                    unavailableMessage = "BearerPreference الأصلي غير ظاهر؛ لن نغيّر وضع الشبكة دون مسار رجوع"
                )
                if (backup != null) {
                    operationMessage = connected.setNetworkMode(mode).message
                    refreshAfterControl(connected)
                }
                controlBusy = false
            }
        },
        onLockCurrentCell = {
            val pci = snapshot?.pci
            val earfcn = snapshot?.earfcn
            if (pci != null && earfcn != null && !controlBusy) scope.launch {
                controlBusy = true
                val backup = captureSafetyBackup(
                    connected,
                    canRestore = { it.hasCompleteCellLockState },
                    unavailableMessage = "حالة Cell Lock الأصلية غير مكتملة؛ لن نثبت الخلية دون مسار استعادة"
                )
                if (backup != null) {
                    val clearPathVerified = if (backup.cellWasUnlocked) connected.clearCellLock().verified else true
                    if (!clearPathVerified) {
                        operationMessage = "تم إيقاف Cell Lock: الـFirmware لم يثبت أن إزالة القفل تعمل على جهازك"
                    } else {
                        operationMessage = connected.setCellLock(pci, earfcn).message
                    }
                    refreshAfterControl(connected)
                }
                controlBusy = false
            }
        },
        onClearCellLock = {
            if (!controlBusy) scope.launch {
                controlBusy = true
                val backup = captureSafetyBackup(
                    connected,
                    canRestore = { it.hasCompleteCellLockState },
                    unavailableMessage = "حالة Cell Lock الأصلية غير مكتملة؛ لم يتم إرسال أمر الإزالة"
                )
                if (backup != null) {
                    operationMessage = connected.clearCellLock().message
                    refreshAfterControl(connected)
                }
                controlBusy = false
            }
        },
        onRestoreSafetyBackup = {
            val backup = latestSafetyBackup ?: backupStore.latest(routerAddress)
            if (backup != null && !controlBusy) scope.launch {
                controlBusy = true
                operationMessage = "جاري استعادة آخر نسخة أمان والتحقق..."
                val report = runCatching { connected.restoreSettings(backup) }
                    .getOrElse {
                        operationMessage = "تعذر تنفيذ الاستعادة: ${it.message.orEmpty()}"
                        controlBusy = false
                        return@launch
                    }
                operationMessage = report.message
                refreshAfterControl(connected)
                controlBusy = false
            }
        },
        onAntennaState = { state ->
            if (!controlBusy) scope.launch {
                controlBusy = true
                operationMessage = connected.setAntennaState(state).message
                controlBusy = false
            }
        }
    )
}
