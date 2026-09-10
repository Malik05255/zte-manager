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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.NetworkPerformanceProbe
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartBandOptimizer
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReferenceMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = PremiumLightColors) {
                    Surface(modifier = Modifier, color = PremiumBackground) { ReferenceManagerApp() }
                }
            }
        }
    }
}

private const val REF_SMART_COOLDOWN_MS = 15 * 60 * 1000L

@Composable
private fun ReferenceManagerApp() {
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
    val scope = rememberCoroutineScope()

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
                placementEngine.reset()
                placementReading = placementEngine.add(first)
                smartBaseline = monitorScorer.score(first).total
                poorSamples = 0
                status = "متصل • ${profile.capabilities.modelFamily}"
            }.onFailure {
                client = null
                snapshot = null
                status = it.message ?: "تعذر الاتصال بالراوتر"
            }
            connectBusy = false
        }
    }

    fun optimize(manual: Boolean) {
        val connected = client ?: return
        if (smartBusy || controlBusy) return
        scope.launch {
            smartBusy = true
            operationMessage = if (manual) "جاري البحث عن أفضل إعداد للشبكة..." else "رصدنا تدهورًا مستمرًا؛ بدأ التحسين الذكي..."
            runCatching { SmartBandOptimizer(connected).optimizeOnce(smartGoal) { operationMessage = it } }
                .onSuccess { report ->
                    smartReport = report
                    operationMessage = report.message
                    smartBaseline = report.best.qualityScore
                    lastSmartRun = SystemClock.elapsedRealtime()
                    snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                    selectedLte = snapshot?.let(::premiumCurrentLteBands).orEmpty()
                    selectedNr = snapshot?.let(::premiumCurrentNrBands).orEmpty()
                }
                .onFailure {
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
                        val cooldownDone = SystemClock.elapsedRealtime() - lastSmartRun >= REF_SMART_COOLDOWN_MS
                        if (poorSamples >= 5 && cooldownDone) optimize(false)
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
    ReferenceDashboard(
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
        onDisconnect = {
            client = null
            snapshot = null
            placementMode = false
            smartMode = false
            placementReading = null
            smartReport = null
            lastPerformance = null
            status = "غير متصل"
        },
        onSpeedTest = {
            if (!speedBusy) scope.launch {
                speedBusy = true
                operationMessage = "جاري قياس السرعة..."
                runCatching { performanceProbe.measure(true) }
                    .onSuccess { lastPerformance = it; operationMessage = "اكتمل قياس الشبكة" }
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
        onOptimizeNow = { optimize(true) },
        onLteToggle = { band -> selectedLte = premiumToggleBand(selectedLte, band) },
        onNrToggle = { band -> selectedNr = premiumToggleBand(selectedNr, band) },
        onApplyLte = {
            if (selectedLte.isNotEmpty() && !controlBusy) scope.launch {
                controlBusy = true
                operationMessage = connected.setLteBands(selectedLte).message
                delay(900)
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                controlBusy = false
            }
        },
        onAllowAllLte = {
            if (!controlBusy) {
                selectedLte = capabilities.supportedLteBands
                scope.launch {
                    controlBusy = true
                    operationMessage = connected.setLteBands(capabilities.supportedLteBands).message
                    delay(900)
                    snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                    controlBusy = false
                }
            }
        },
        onApplyNr = {
            if (selectedNr.isNotEmpty() && !controlBusy) scope.launch {
                controlBusy = true
                operationMessage = connected.setNrBands(selectedNr).message
                delay(900)
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                controlBusy = false
            }
        },
        onAllowAllNr = {
            if (!controlBusy) {
                selectedNr = capabilities.supportedNrBands
                scope.launch {
                    controlBusy = true
                    operationMessage = connected.setNrBands(capabilities.supportedNrBands).message
                    delay(900)
                    snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                    controlBusy = false
                }
            }
        },
        onSetNetworkMode = { mode ->
            if (!controlBusy) scope.launch {
                controlBusy = true
                operationMessage = connected.setNetworkMode(mode).message
                delay(700)
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                controlBusy = false
            }
        },
        onLockCurrentCell = {
            val pci = snapshot?.pci
            val earfcn = snapshot?.earfcn
            if (pci != null && earfcn != null && !controlBusy) scope.launch {
                controlBusy = true
                operationMessage = connected.setCellLock(pci, earfcn).message
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
