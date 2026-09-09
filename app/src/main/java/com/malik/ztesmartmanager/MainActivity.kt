package com.malik.ztesmartmanager

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementGuidance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartBandOptimizer
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = if (isSystemInDarkTheme()) AppDarkColors else AppLightColors) {
                    Surface(modifier = Modifier.fillMaxSize()) { ZteManagerApp() }
                }
            }
        }
    }
}

private val AppLightColors = lightColorScheme(
    primary = Color(0xFF245C52),
    onPrimary = Color.White,
    surface = Color(0xFFF8F8F5),
    background = Color(0xFFF8F8F5),
    surfaceVariant = Color(0xFFECEFEA)
)

private val AppDarkColors = darkColorScheme(
    primary = Color(0xFF8BCBC0),
    surface = Color(0xFF111513),
    background = Color(0xFF111513),
    surfaceVariant = Color(0xFF202824)
)

private const val SMART_COOLDOWN_MS = 15 * 60 * 1000L

@Composable
private fun ZteManagerApp() {
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
                selectedLte = currentLteBands(first)
                selectedNr = currentNrBands(first)
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

    fun runSmartOptimization(manual: Boolean) {
        val connected = client ?: return
        if (smartBusy || controlBusy) return
        scope.launch {
            smartBusy = true
            operationMessage = if (manual) "بدء التحسين الذكي..." else "اكتشف الوضع الذكي تدهورًا مستمرًا..."
            runCatching {
                SmartBandOptimizer(connected).optimizeOnce(smartGoal) { operationMessage = it }
            }.onSuccess { report ->
                smartReport = report
                operationMessage = report.message
                smartBaseline = report.best.qualityScore
                lastSmartRun = SystemClock.elapsedRealtime()
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                selectedLte = snapshot?.let(::currentLteBands).orEmpty()
            }.onFailure {
                operationMessage = "تعذر التحسين: ${it.message.orEmpty()}"
                lastSmartRun = SystemClock.elapsedRealtime()
            }
            poorSamples = 0
            smartBusy = false
        }
    }

    LaunchedEffect(client, placementMode, smartMode) {
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

                        val cooldownDone = SystemClock.elapsedRealtime() - lastSmartRun >= SMART_COOLDOWN_MS
                        if (poorSamples >= 5 && cooldownDone) runSmartOptimization(manual = false)
                    }
                }
                .onFailure { status = "انقطع التحديث مؤقتًا: ${it.message.orEmpty()}" }
        }
    }

    val connectedClient = client
    if (connectedClient == null) {
        LoginScreen(
            routerAddress = routerAddress,
            onRouterAddressChange = { routerAddress = it },
            password = password,
            onPasswordChange = { password = it },
            status = status,
            busy = connectBusy,
            onConnect = ::connect
        )
    } else {
        val capabilities = connectedClient.profile.capabilities
        DashboardScreen(
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
            onLteToggle = { band -> selectedLte = toggleBand(selectedLte, band) },
            onNrToggle = { band -> selectedNr = toggleBand(selectedNr, band) },
            onApplyLte = {
                if (selectedLte.isNotEmpty() && !controlBusy) scope.launch {
                    controlBusy = true
                    operationMessage = connectedClient.setLteBands(selectedLte).message
                    delay(900)
                    snapshot = runCatching { connectedClient.readSnapshot() }.getOrNull() ?: snapshot
                    controlBusy = false
                }
            },
            onAllowAllLte = {
                if (!controlBusy) {
                    selectedLte = capabilities.supportedLteBands
                    scope.launch {
                        controlBusy = true
                        operationMessage = connectedClient.setLteBands(capabilities.supportedLteBands).message
                        controlBusy = false
                    }
                }
            },
            onApplyNr = {
                if (selectedNr.isNotEmpty() && !controlBusy) scope.launch {
                    controlBusy = true
                    operationMessage = connectedClient.setNrBands(selectedNr).message
                    delay(900)
                    snapshot = runCatching { connectedClient.readSnapshot() }.getOrNull() ?: snapshot
                    controlBusy = false
                }
            },
            onSetNetworkMode = { mode ->
                if (!controlBusy) scope.launch {
                    controlBusy = true
                    operationMessage = connectedClient.setNetworkMode(mode).message
                    controlBusy = false
                }
            },
            onLockCurrentCell = {
                val current = snapshot
                val pci = current?.pci
                val earfcn = current?.earfcn
                if (pci != null && earfcn != null && !controlBusy) scope.launch {
                    controlBusy = true
                    operationMessage = connectedClient.setCellLock(pci, earfcn).message
                    controlBusy = false
                }
            },
            onDisconnect = {
                client = null
                snapshot = null
                placementMode = false
                smartMode = false
                placementReading = null
                smartReport = null
                status = "غير متصل"
            }
        )
    }
}

@Composable
private fun LoginScreen(
    routerAddress: String,
    onRouterAddressChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    status: String,
    busy: Boolean,
    onConnect: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ZTE Smart Manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("تحكم ذكي ومريح براوترات ZTE", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = routerAddress,
            onValueChange = onRouterAddressChange,
            label = { Text("عنوان الراوتر") },
            supportingText = { Text("مثال: 192.168.0.1") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("كلمة مرور الإدارة") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onConnect, enabled = !busy && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "جاري الاتصال..." else "اتصال بالراوتر")
        }
        Spacer(Modifier.height(12.dp))
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("المصادقة محلية ولا تُرسل كلمة المرور إلى أي خدمة خارجية.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DashboardScreen(
    snapshot: RouterSnapshot?,
    capabilities: RouterCapabilities,
    status: String,
    operationMessage: String,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    controlBusy: Boolean,
    onPlacementToggle: () -> Unit,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onAllowAllLte: () -> Unit,
    onApplyNr: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onLockCurrentCell: () -> Unit,
    onDisconnect: () -> Unit
) {
    if (placementMode && placementReading != null) PlacementFeedback(placementReading)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ZTE Smart Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onDisconnect) { Text("فصل") }
            }
        }

        snapshot?.let { data ->
            item { CurrentConnectionCard(data) }
        }

        if (operationMessage.isNotBlank()) item { StatusCard(operationMessage) }
        if (placementMode && placementReading != null) item { PlacementCard(placementReading) }
        item {
            Button(onClick = onPlacementToggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (placementMode) "إيقاف مساعد أفضل مكان" else "مساعد أفضل مكان")
            }
        }
        item {
            SmartModeCard(
                enabled = smartMode,
                goal = smartGoal,
                busy = smartBusy,
                report = smartReport,
                onEnabledChange = onSmartModeChange,
                onGoalChange = onSmartGoalChange,
                onOptimizeNow = onOptimizeNow
            )
        }

        snapshot?.let { data ->
            item { SignalCard(data) }
            item { NetworkCard(data) }
            item { NetworkModeCard(controlBusy, onSetNetworkMode) }
            if (capabilities.supportsLteBandLock) {
                item {
                    BandSelectorCard(
                        title = "ترددات 4G",
                        bands = capabilities.supportedLteBands,
                        selected = selectedLte,
                        prefix = "B",
                        busy = controlBusy,
                        onToggle = onLteToggle,
                        onApply = onApplyLte,
                        secondaryActionLabel = "السماح بكل الترددات",
                        onSecondaryAction = onAllowAllLte
                    )
                }
            }
            if (capabilities.supportsNrBandLock) {
                item {
                    BandSelectorCard(
                        title = "ترددات 5G",
                        bands = capabilities.supportedNrBands,
                        selected = selectedNr,
                        prefix = "N",
                        busy = controlBusy,
                        onToggle = onNrToggle,
                        onApply = onApplyNr
                    )
                }
            }
            if (capabilities.supportsCellLock && data.pci != null && data.earfcn != null) {
                item {
                    OutlinedButton(onClick = onLockCurrentCell, enabled = !controlBusy, modifier = Modifier.fillMaxWidth()) {
                        Text("تثبيت الخلية الحالية • PCI ${data.pci} / EARFCN ${data.earfcn}")
                    }
                }
            }
            if (data.cells.isNotEmpty()) {
                item { Text("الخلايا والتجميع الفعلي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(data.cells) { cell -> CellCard(cell) }
            }
        }
    }
}

@Composable
private fun CurrentConnectionCard(snapshot: RouterSnapshot) {
    val primary = snapshot.cells.firstOrNull { it.role == CellRole.PRIMARY }
    val secondary = snapshot.cells.filter { it.role == CellRole.SECONDARY }
    val nrCells = snapshot.cells.filter { it.role == CellRole.NR }

    val primaryBand = displayBand(primary?.band ?: snapshot.lteBand, "B")
    val secondaryBands = secondary.mapNotNull { displayBand(it.band, "B") }.distinct()
    val nrBands = buildList {
        nrCells.mapNotNullTo(this) { displayBand(it.band, "N") }
        displayBand(snapshot.nrBand, "N")?.let(::add)
    }.distinct()

    val lteBands = buildList {
        primaryBand?.let(::add)
        addAll(secondaryBands)
        currentLteBands(snapshot).sorted().mapTo(this) { "B$it" }
    }.distinct()

    val allBands = (lteBands + nrBands).joinToString(" + ").ifBlank { "جاري قراءة التردد..." }
    val mode = currentRadioMode(snapshot, lteBands, nrBands)
    val caCount = lteBands.size.coerceAtLeast(if (snapshot.caActive) 2 else lteBands.size)
    val lteAggregation = when {
        lteBands.size > 1 -> "${lteBands.joinToString(" + ")} • ${lteBands.size}CA"
        snapshot.caActive -> "نشط • CA"
        lteBands.size == 1 -> "لا يوجد دمج • ${lteBands.first()}"
        else -> "غير معروف"
    }
    val fiveG = when {
        nrBands.isNotEmpty() -> nrBands.joinToString(" + ")
        snapshot.nrRsrp != null || snapshot.nrSinr != null -> "نشط"
        else -> "غير متصل"
    }
    val primaryCell = buildString {
        append(primaryBand ?: "—")
        val pci = primary?.pci ?: snapshot.pci
        if (pci != null) append(" • PCI $pci")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("الاتصال الآن", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(mode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(allBands, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))

            InfoRow("دمج 4G", lteAggregation)
            InfoRow("5G", fiveG)
            InfoRow("الخلية الرئيسية", primaryCell)
            if (secondaryBands.isNotEmpty()) {
                InfoRow("خلايا الدمج", secondaryBands.joinToString(" + "))
            } else if (snapshot.caActive && caCount >= 2) {
                InfoRow("خلايا الدمج", "CA نشط — الراوتر لم يُرجع أسماء كل الـBands")
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "يعرض الترددات النشطة فعليًا الآن، وليس مجرد الترددات المسموح بها في الإعدادات.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun currentRadioMode(snapshot: RouterSnapshot, lteBands: List<String>, nrBands: List<String>): String {
    val raw = snapshot.networkType.orEmpty().uppercase()
    val hasNr = nrBands.isNotEmpty() || snapshot.nrRsrp != null || snapshot.nrSinr != null || "5G" in raw || "NR" in raw
    val hasLte = lteBands.isNotEmpty() || snapshot.lteRsrp != null || "LTE" in raw
    val explicitSa = ("SA" in raw && "NSA" !in raw)
    val explicitNsa = "NSA" in raw || "ENDC" in raw || "EN-DC" in raw

    return when {
        hasNr && hasLte && (explicitNsa || !explicitSa) -> "5G NSA + 4G"
        hasNr && explicitSa && !hasLte -> "5G SA"
        hasNr && explicitSa -> "5G SA"
        hasNr && hasLte -> "5G + 4G"
        hasNr -> "5G"
        hasLte && (snapshot.caActive || lteBands.size > 1) -> "4G+ • دمج ترددات"
        hasLte -> "4G"
        else -> snapshot.networkType?.takeIf { it.isNotBlank() } ?: "نوع الشبكة غير معروف"
    }
}

private fun displayBand(value: String?, prefix: String): String? = extractBand(value)?.let { "$prefix$it" }

@Composable
private fun StatusCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}

@Composable
private fun SmartModeCard(
    enabled: Boolean,
    goal: OptimizationGoal,
    busy: Boolean,
    report: SmartOptimizationReport?,
    onEnabledChange: (Boolean) -> Unit,
    onGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit
) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("الوضع الذكي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("يراقب التدهور ويبحث عن Band/CA أفضل", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            Spacer(Modifier.height(10.dp))
            OptimizationGoal.entries.forEach { item ->
                FilterChip(
                    selected = goal == item,
                    onClick = { onGoalChange(item) },
                    label = { Text(goalLabel(item)) },
                    modifier = Modifier.padding(end = 6.dp, bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOptimizeNow, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "جاري القياس والمقارنة..." else "تحسين الآن")
            }
            report?.let {
                Spacer(Modifier.height(10.dp))
                InfoRow("قبل", "${formatBands(it.baseline.bands)} • ${it.baseline.score}/100")
                InfoRow("الأفضل", "${formatBands(it.best.bands)} • ${it.best.score}/100")
                it.best.performance.downloadMbps?.let { speed -> InfoRow("سرعة العينة", "$speed Mbps") }
                it.best.performance.latencyMs?.let { latency -> InfoRow("Ping", "$latency ms") }
            }
        }
    }
}

@Composable
private fun SignalCard(snapshot: RouterSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("الإشارة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            MetricRow("RSRP 4G", snapshot.lteRsrp, "dBm")
            MetricRow("RSRQ", snapshot.lteRsrq, "dB")
            MetricRow("SINR 4G", snapshot.lteSinr, "dB")
            MetricRow("RSRP 5G", snapshot.nrRsrp, "dBm")
            MetricRow("SINR 5G", snapshot.nrSinr, "dB")
        }
    }
}

@Composable
private fun NetworkCard(snapshot: RouterSnapshot) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("تفاصيل الشبكة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            InfoRow("النوع الخام", snapshot.networkType ?: "—")
            InfoRow("4G", snapshot.lteBand ?: "—")
            InfoRow("5G", snapshot.nrBand ?: "—")
            InfoRow("PCI", snapshot.pci?.toString() ?: "—")
            InfoRow("EARFCN", snapshot.earfcn?.toString() ?: "—")
            InfoRow("Carrier Aggregation", if (snapshot.caActive) "نشط ✓" else "غير نشط")
        }
    }
}

@Composable
private fun NetworkModeCard(busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("وضع الشبكة", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSetNetworkMode("Only_LTE") }, enabled = !busy) { Text("4G فقط") }
                OutlinedButton(onClick = { onSetNetworkMode("LTE_AND_5G") }, enabled = !busy) { Text("4G + 5G") }
                OutlinedButton(onClick = { onSetNetworkMode("Only_5G") }, enabled = !busy) { Text("5G فقط") }
            }
        }
    }
}

@Composable
private fun BandSelectorCard(
    title: String,
    bands: Set<Int>,
    selected: Set<Int>,
    prefix: String,
    busy: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            bands.sorted().chunked(4).forEach { rowBands ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowBands.forEach { band ->
                        FilterChip(selected = band in selected, onClick = { onToggle(band) }, label = { Text("$prefix$band") })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onApply, enabled = selected.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
                Text("تطبيق والتحقق")
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onSecondaryAction, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun PlacementCard(reading: PlacementReading) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("مساعد أفضل مكان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("${reading.score.total} / 100", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(reading.score.label)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { reading.score.total / 100f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(placementGuidanceText(reading.guidance), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            InfoRow("قوة الإشارة", "${reading.score.signal}/100")
            InfoRow("نظافة الإشارة", "${reading.score.cleanliness}/100")
            InfoRow("الثبات", "${reading.score.stability}/100")
            InfoRow("أفضل نقطة", "${reading.bestScore}/100")
            InfoRow("ثقة القياس", "${reading.confidence}%")
        }
    }
}

@Composable
private fun PlacementFeedback(reading: PlacementReading) {
    val haptic = LocalHapticFeedback.current
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    LaunchedEffect(reading.guidance) {
        when (reading.guidance) {
            PlacementGuidance.BEST_SO_FAR, PlacementGuidance.EXCELLENT_HOLD -> {
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 100)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            PlacementGuidance.WORSE, PlacementGuidance.RETURN_TO_BEST, PlacementGuidance.CELL_CHANGED_WORSE -> {
                tone.startTone(ToneGenerator.TONE_PROP_NACK, 100)
            }
            else -> Unit
        }
    }

    LaunchedEffect(reading.score.total) {
        while (true) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
            val interval = (1_350 - reading.score.total * 11L).coerceIn(180L, 1_050L)
            delay(interval)
        }
    }
}

private fun placementGuidanceText(guidance: PlacementGuidance): String = when (guidance) {
    PlacementGuidance.INITIAL -> "حرّك الراوتر ببطء عدة سنتيمترات"
    PlacementGuidance.MUCH_BETTER -> "تحسن واضح — استمر في هذا الاتجاه"
    PlacementGuidance.BETTER -> "أفضل — استمر ببطء"
    PlacementGuidance.STABLE -> "الفرق بسيط — جرّب حركة صغيرة"
    PlacementGuidance.WORSE -> "أسوأ — ارجع قليلًا"
    PlacementGuidance.RETURN_TO_BEST -> "ابتعدت عن أفضل نقطة — ارجع للمكان السابق"
    PlacementGuidance.CELL_CHANGED_WORSE -> "انتقلت لخلية أضعف — ارجع قليلًا"
    PlacementGuidance.EXCELLENT_HOLD -> "ممتاز — ثبّت الراوتر هنا"
    PlacementGuidance.BEST_SO_FAR -> "هذه أفضل نقطة حتى الآن ✓"
}

@Composable
private fun CellCard(cell: CarrierCell) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                when (cell.role) {
                    CellRole.PRIMARY -> "الخلية الرئيسية PCell"
                    CellRole.SECONDARY -> "خلية تجميع SCell"
                    CellRole.NR -> "خلية 5G NR"
                },
                fontWeight = FontWeight.Bold
            )
            InfoRow("Band", cell.band ?: "—")
            InfoRow("PCI", cell.pci?.toString() ?: "—")
            InfoRow("ARFCN", cell.arfcn?.toString() ?: "—")
            InfoRow("Bandwidth", cell.bandwidthMhz?.let { "${formatNumber(it)} MHz" } ?: "—")
        }
    }
}

@Composable
private fun MetricRow(label: String, value: Double?, unit: String) {
    InfoRow(label, value?.let { "${formatNumber(it)} $unit" } ?: "—")
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun toggleBand(current: Set<Int>, band: Int): Set<Int> = if (band in current) current - band else current + band

private fun currentLteBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
    extractBand(snapshot.lteBand)?.let(::add)
    snapshot.cells.filter { it.role != CellRole.NR }.forEach { extractBand(it.band)?.let(::add) }
}

private fun currentNrBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
    extractBand(snapshot.nrBand)?.let(::add)
    snapshot.cells.filter { it.role == CellRole.NR }.forEach { extractBand(it.band)?.let(::add) }
}

private fun extractBand(value: String?): Int? = Regex("\\d+").find(value.orEmpty())?.value?.toIntOrNull()
private fun formatBands(bands: Set<Int>): String = bands.sorted().joinToString("+") { "B$it" }.ifBlank { "تلقائي" }
private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

private fun goalLabel(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "أعلى سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}
