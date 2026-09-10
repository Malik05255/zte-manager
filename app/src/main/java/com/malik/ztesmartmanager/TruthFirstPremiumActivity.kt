package com.malik.ztesmartmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.NetworkPerformanceProbe
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.SmartBandOptimizer
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerLockEngine
import com.malik.ztesmartmanager.core.tower.TowerMatch
import com.malik.ztesmartmanager.core.tower.TowerTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TruthFirstPremiumActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) TruthDark else TruthLight) {
                    Surface(Modifier.fillMaxSize()) { TruthFirstPremiumApp() }
                }
            }
        }
    }
}

private val TruthGold = Color(0xFF9B742D)
private val TruthSoftGold = Color(0xFFE8D7B1)
private val TruthLight = lightColorScheme(
    primary = TruthGold,
    background = Color(0xFFF4F1EB),
    surface = Color(0xFFFCFAF6),
    surfaceVariant = Color(0xFFEDE8DF),
    onSurface = Color(0xFF292620),
    onSurfaceVariant = Color(0xFF706A60)
)
private val TruthDark = darkColorScheme(
    primary = Color(0xFFD9B86F),
    background = Color(0xFF151411),
    surface = Color(0xFF1D1B17),
    surfaceVariant = Color(0xFF29261F),
    onSurface = Color(0xFFF1ECE1),
    onSurfaceVariant = Color(0xFFBEB6A8)
)

@Composable
private fun TruthFirstPremiumApp() {
    var routerAddress by rememberSaveable { mutableStateOf("192.168.0.1") }
    var password by rememberSaveable { mutableStateOf("") }
    var client by remember { mutableStateOf<ZteRouterClient?>(null) }
    var snapshot by remember { mutableStateOf<RouterSnapshot?>(null) }
    var status by remember { mutableStateOf("غير متصل") }
    var message by remember { mutableStateOf("") }
    var connectBusy by remember { mutableStateOf(false) }
    var speedBusy by remember { mutableStateOf(false) }
    var smartBusy by remember { mutableStateOf(false) }
    var controlBusy by remember { mutableStateOf(false) }
    var discoveryBusy by remember { mutableStateOf(false) }
    var towerBusy by remember { mutableStateOf(false) }
    var performance by remember { mutableStateOf<NetworkPerformance?>(null) }
    var smartGoal by remember { mutableStateOf(OptimizationGoal.BALANCED) }
    var selectedLte by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedNr by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var nearbyCells by remember { mutableStateOf<List<NearbyCell>>(emptyList()) }
    var towerEngine by remember { mutableStateOf<TowerLockEngine?>(null) }
    var towerTarget by remember { mutableStateOf<TowerTarget?>(null) }
    var towerGuardEnabled by remember { mutableStateOf(false) }
    var towerGuardMessage by remember { mutableStateOf("") }
    var showBands by rememberSaveable { mutableStateOf(false) }
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }

    val probe = remember { NetworkPerformanceProbe() }
    val scope = rememberCoroutineScope()

    fun connect() {
        if (connectBusy || routerAddress.isBlank() || password.isBlank()) return
        scope.launch {
            connectBusy = true
            status = "جاري الاتصال والتحقق..."
            runCatching {
                val connected = ZteRouterClient(routerAddress)
                val profile = connected.login(password)
                val first = connected.readSnapshot()
                client = connected
                snapshot = first
                towerEngine = TowerLockEngine(connected)
                selectedLte = currentLteBands(first)
                selectedNr = currentNrBands(first)
                status = "متصل • ${profile.capabilities.modelFamily}"
                message = if (first.networkType == "غير مؤكد") {
                    "تم تسجيل الدخول، لكن حالة الراديو الحالية لم تكتمل أدلتها بعد"
                } else "تم الاتصال وقراءة حالة الراديو الموثقة"
            }.onFailure {
                client = null
                snapshot = null
                towerEngine = null
                status = it.message ?: "تعذر الاتصال بالراوتر"
            }
            connectBusy = false
        }
    }

    fun disconnect() {
        client = null
        snapshot = null
        towerEngine = null
        towerTarget = null
        towerGuardEnabled = false
        nearbyCells = emptyList()
        performance = null
        message = ""
        password = ""
        status = "غير متصل"
    }

    fun measureSpeed() {
        if (speedBusy) return
        scope.launch {
            speedBusy = true
            message = "جاري قياس الأداء الفعلي..."
            runCatching { probe.measure(includeDownload = true) }
                .onSuccess {
                    performance = it
                    message = "اكتمل قياس الأداء"
                }
                .onFailure { message = "تعذر قياس الأداء: ${it.message.orEmpty()}" }
            speedBusy = false
        }
    }

    fun optimize() {
        val connected = client ?: return
        if (smartBusy || controlBusy || towerBusy) return
        scope.launch {
            smartBusy = true
            message = "جاري قياس خط الأساس واختبار بدائل موثقة..."
            runCatching {
                SmartBandOptimizer(connected).optimizeOnce(smartGoal) { message = it }
            }.onSuccess { report ->
                message = report.message
                performance = report.best.performance
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                selectedLte = snapshot?.let(::currentLteBands).orEmpty()
                selectedNr = snapshot?.let(::currentNrBands).orEmpty()
            }.onFailure { message = "تعذر التحسين: ${it.message.orEmpty()}" }
            smartBusy = false
        }
    }

    fun lockCurrentCell() {
        val engine = towerEngine ?: return
        val current = snapshot ?: return
        if (towerBusy) return
        scope.launch {
            towerBusy = true
            message = "جاري حفظ Cell Lock والتحقق من الخلية الحية..."
            runCatching { engine.lockCurrent(current) }
                .onSuccess { result ->
                    val verified = result.match == TowerMatch.MATCHED
                    towerTarget = result.target.takeIf { verified }
                    towerGuardEnabled = verified
                    towerGuardMessage = result.message
                    message = result.message
                }
                .onFailure { message = "تعذر تثبيت الخلية: ${it.message.orEmpty()}" }
            towerBusy = false
        }
    }

    fun discoverCells() {
        val engine = towerEngine ?: return
        if (discoveryBusy) return
        scope.launch {
            discoveryBusy = true
            message = "جاري قراءة Neighbor Cells من الراوتر..."
            runCatching { engine.readNearbyCells() }
                .onSuccess { cells ->
                    nearbyCells = cells
                    message = if (cells.isEmpty()) {
                        "الـFirmware لم يقدّم خلايا قريبة بهوية PCI + ARFCN قابلة للاستخدام"
                    } else {
                        "تمت قراءة ${cells.size} خلية بهوية راديوية قابلة للتحقق"
                    }
                }
                .onFailure { message = "تعذر قراءة الخلايا القريبة: ${it.message.orEmpty()}" }
            discoveryBusy = false
        }
    }

    fun selectNearby(cell: NearbyCell) {
        val engine = towerEngine ?: return
        if (towerBusy || !cell.rat.equals("LTE", true)) return
        scope.launch {
            towerBusy = true
            message = "جاري تجربة الخلية PCI ${cell.pci} / EARFCN ${cell.arfcn} مع إمكانية الاستعادة..."
            runCatching { engine.lockNearbyCell(cell) }
                .onSuccess { result ->
                    val verified = result.match == TowerMatch.MATCHED
                    towerTarget = result.target.takeIf { verified }
                    towerGuardEnabled = verified
                    towerGuardMessage = result.message
                    snapshot = client?.let { runCatching { it.readSnapshot() }.getOrNull() } ?: snapshot
                    message = result.message
                }
                .onFailure { message = "تعذر تجربة الخلية: ${it.message.orEmpty()}" }
            towerBusy = false
        }
    }

    fun clearCellLock() {
        val connected = client ?: return
        if (towerBusy) return
        scope.launch {
            towerBusy = true
            message = "جاري إعادة Cell Lock إلى الوضع التلقائي والتحقق..."
            runCatching { connected.clearCellLock() }
                .onSuccess { result ->
                    if (result.success && result.verified) {
                        towerTarget = null
                        towerGuardEnabled = false
                        towerGuardMessage = "الوضع التلقائي موثق"
                    }
                    message = result.message
                }
                .onFailure { message = "تعذر إلغاء Cell Lock: ${it.message.orEmpty()}" }
            towerBusy = false
        }
    }

    LaunchedEffect(client, towerGuardEnabled, towerTarget) {
        val connected = client ?: return@LaunchedEffect
        val engine = towerEngine ?: return@LaunchedEffect
        while (client === connected) {
            delay(2_000)
            if (smartBusy || controlBusy || towerBusy || discoveryBusy) continue
            val latest = runCatching { connected.readSnapshot() }.getOrNull() ?: continue
            snapshot = latest
            val target = towerTarget
            if (towerGuardEnabled && target != null) {
                val guard = runCatching { engine.guardOnce(target, latest) }.getOrNull()
                if (guard != null) {
                    towerGuardMessage = guard.message
                    if (guard.repaired) message = guard.message
                }
            }
        }
    }

    if (client == null) {
        TruthLogin(
            routerAddress = routerAddress,
            password = password,
            status = status,
            busy = connectBusy,
            onAddress = { routerAddress = it },
            onPassword = { password = it },
            onConnect = ::connect
        )
        return
    }

    TruthDashboard(
        snapshot = snapshot,
        capabilities = client!!.profile.capabilities,
        status = status,
        message = message,
        performance = performance,
        speedBusy = speedBusy,
        smartBusy = smartBusy,
        controlBusy = controlBusy,
        towerBusy = towerBusy,
        discoveryBusy = discoveryBusy,
        smartGoal = smartGoal,
        selectedLte = selectedLte,
        selectedNr = selectedNr,
        nearbyCells = nearbyCells,
        towerTarget = towerTarget,
        towerGuardEnabled = towerGuardEnabled,
        towerGuardMessage = towerGuardMessage,
        showBands = showBands,
        showDiagnostics = showDiagnostics,
        onDisconnect = ::disconnect,
        onMeasureSpeed = ::measureSpeed,
        onOptimize = ::optimize,
        onGoal = { smartGoal = it },
        onLockCurrent = ::lockCurrentCell,
        onDiscover = ::discoverCells,
        onSelectNearby = ::selectNearby,
        onClearLock = ::clearCellLock,
        onToggleBandsPanel = { showBands = !showBands },
        onToggleDiagnostics = { showDiagnostics = !showDiagnostics },
        onLteToggle = { selectedLte = toggle(selectedLte, it) },
        onNrToggle = { selectedNr = toggle(selectedNr, it) },
        onApplyLte = {
            val connected = client ?: return@TruthDashboard
            if (selectedLte.isNotEmpty() && !controlBusy) scope.launch {
                controlBusy = true
                val result = runCatching { connected.setLteBands(selectedLte) }
                message = result.fold({ it.message }, { "تعذر تطبيق 4G: ${it.message.orEmpty()}" })
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                controlBusy = false
            }
        },
        onApplyNr = {
            val connected = client ?: return@TruthDashboard
            if (selectedNr.isNotEmpty() && !controlBusy) scope.launch {
                controlBusy = true
                val result = runCatching { connected.setNrBands(selectedNr) }
                message = result.fold({ it.message }, { "تعذر تطبيق 5G: ${it.message.orEmpty()}" })
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                controlBusy = false
            }
        },
        onSetNetworkMode = { mode ->
            val connected = client ?: return@TruthDashboard
            if (!controlBusy) scope.launch {
                controlBusy = true
                val result = runCatching { connected.setNetworkMode(mode) }
                message = result.fold({ it.message }, { "تعذر تغيير وضع الشبكة: ${it.message.orEmpty()}" })
                delay(800)
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                controlBusy = false
            }
        }
    )
}

@Composable
private fun TruthLogin(
    routerAddress: String,
    password: String,
    status: String,
    busy: Boolean,
    onAddress: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConnect: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Text("ZTE Manager", fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("Truth‑First Premium", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = routerAddress,
                    onValueChange = onAddress,
                    label = { Text("عنوان الراوتر") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPassword,
                    label = { Text("كلمة مرور الإدارة") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onConnect, enabled = !busy && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "جاري التحقق..." else "اتصال")
                }
                Spacer(Modifier.height(10.dp))
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("المصادقة محلية مع الراوتر. لا تُرسل كلمة المرور لخدمة خارجية.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TruthDashboard(
    snapshot: RouterSnapshot?,
    capabilities: RouterCapabilities,
    status: String,
    message: String,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    smartBusy: Boolean,
    controlBusy: Boolean,
    towerBusy: Boolean,
    discoveryBusy: Boolean,
    smartGoal: OptimizationGoal,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    nearbyCells: List<NearbyCell>,
    towerTarget: TowerTarget?,
    towerGuardEnabled: Boolean,
    towerGuardMessage: String,
    showBands: Boolean,
    showDiagnostics: Boolean,
    onDisconnect: () -> Unit,
    onMeasureSpeed: () -> Unit,
    onOptimize: () -> Unit,
    onGoal: (OptimizationGoal) -> Unit,
    onLockCurrent: () -> Unit,
    onDiscover: () -> Unit,
    onSelectNearby: (NearbyCell) -> Unit,
    onClearLock: () -> Unit,
    onToggleBandsPanel: () -> Unit,
    onToggleDiagnostics: () -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    onSetNetworkMode: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("لوحة التحكم الشبكية", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onDisconnect) { Text("فصل") }
            }
        }

        snapshot?.let { data ->
            item { VerifiedNetworkCard(data) }
            item { PerformanceAndQualityCard(data, performance, speedBusy, onMeasureSpeed) }
            item { SmartCard(smartGoal, smartBusy, onGoal, onOptimize) }
            item {
                CellControlCard(
                    snapshot = data,
                    target = towerTarget,
                    guardEnabled = towerGuardEnabled,
                    guardMessage = towerGuardMessage,
                    towerBusy = towerBusy,
                    discoveryBusy = discoveryBusy,
                    onLockCurrent = onLockCurrent,
                    onDiscover = onDiscover,
                    onClearLock = onClearLock
                )
            }

            if (nearbyCells.isNotEmpty()) {
                item {
                    Text("الخلايا التي يعرضها الراوتر", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("هذه ليست إحداثيات أبراج. هي Neighbor Cells بهوية راديوية فقط.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(nearbyCells) { cell -> NearbyCellRow(cell, towerBusy, onSelectNearby) }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onToggleBandsPanel, modifier = Modifier.weight(1f)) {
                        Text(if (showBands) "إخفاء الترددات" else "قفل الترددات")
                    }
                    OutlinedButton(onClick = onToggleDiagnostics, modifier = Modifier.weight(1f)) {
                        Text(if (showDiagnostics) "إخفاء الأدلة" else "أدلة القراءة")
                    }
                }
            }

            if (showBands) {
                item {
                    BandPanel(
                        capabilities = capabilities,
                        selectedLte = selectedLte,
                        selectedNr = selectedNr,
                        busy = controlBusy,
                        onLteToggle = onLteToggle,
                        onNrToggle = onNrToggle,
                        onApplyLte = onApplyLte,
                        onApplyNr = onApplyNr,
                        onSetNetworkMode = onSetNetworkMode
                    )
                }
            }

            if (showDiagnostics) item { EvidenceCard(data) }
        }

        if (message.isNotBlank()) item { MessageCard(message) }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun VerifiedNetworkCard(snapshot: RouterSnapshot) {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val caVerified = snapshot.raw["_zte_ca_verified"].equals("true", true)
    val badge = when {
        nrVerified -> "5G"
        caVerified && snapshot.caActive -> "4G+"
        lteVerified -> "4G"
        else -> "؟"
    }
    val title = snapshot.networkType?.takeIf { it.isNotBlank() } ?: "غير مؤكد"
    val bands = activeBandText(snapshot)

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("الحالة الفعلية الموثقة", color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
                    Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text(bands, color = Color.White.copy(alpha = .90f), fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier.border(1.dp, Color.White.copy(alpha = .4f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 10.dp)
                ) { Text(badge, color = Color.White, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.height(14.dp))
            InfoRowLight("5G NR", if (nrVerified) "نشط موثق" else "لا يوجد حامل NR موثق الآن")
            InfoRowLight(
                "CA",
                when {
                    caVerified && snapshot.caActive -> "نشط موثق"
                    caVerified -> "غير نشط موثق"
                    else -> "غير مؤكد"
                }
            )
            InfoRowLight("PCI / EARFCN", "${snapshot.pci ?: "—"} / ${snapshot.earfcn ?: "—"}")
        }
    }
}

@Composable
private fun PerformanceAndQualityCard(
    snapshot: RouterSnapshot,
    performance: NetworkPerformance?,
    busy: Boolean,
    onMeasure: () -> Unit
) {
    val quality = remember(snapshot) { NetworkQualityEngine().score(snapshot) }
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val rsrp = if (nrVerified) snapshot.nrRsrp ?: snapshot.lteRsrp else snapshot.lteRsrp
    val sinr = if (nrVerified) snapshot.nrSinr ?: snapshot.lteSinr else snapshot.lteSinr

    TruthCard {
        Text("الأداء", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("RSRP", rsrp?.let(::fmt) ?: "—", "dBm", Modifier.weight(1f))
            MetricBox("SINR", sinr?.let(::fmt) ?: "—", "dB", Modifier.weight(1f))
            MetricBox("الجودة", quality.total.toString(), "/100", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("Download", performance?.downloadMbps?.let(::fmt) ?: "—", "Mb/s", Modifier.weight(1f))
            MetricBox("Ping", performance?.latencyMs?.let(::fmt) ?: "—", "ms", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onMeasure, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "جاري القياس..." else "قياس السرعة والأداء")
        }
    }
}

@Composable
private fun SmartCard(goal: OptimizationGoal, busy: Boolean, onGoal: (OptimizationGoal) -> Unit, onOptimize: () -> Unit) {
    TruthCard {
        Text("التحسين الذكي", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("يحتفظ بالإعداد الفائز فقط بعد read‑back وقراءات حية متعددة؛ وإلا يستعيد السابق.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            OptimizationGoal.entries.forEach { item ->
                FilterChip(
                    selected = goal == item,
                    onClick = { onGoal(item) },
                    label = { Text(goalLabel(item), fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOptimize, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "جاري الاختبار والتحقق..." else "تحسين الآن")
        }
    }
}

@Composable
private fun CellControlCard(
    snapshot: RouterSnapshot,
    target: TowerTarget?,
    guardEnabled: Boolean,
    guardMessage: String,
    towerBusy: Boolean,
    discoveryBusy: Boolean,
    onLockCurrent: () -> Unit,
    onDiscover: () -> Unit,
    onClearLock: () -> Unit
) {
    TruthCard {
        Text("الخلية والبرج", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("PCI + EARFCN يمكنهما استهداف خلية LTE. لا ندّعي موقع البرج الجغرافي دون إحداثيات موثقة.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        InfoRow("PCI", snapshot.pci?.toString() ?: "—")
        InfoRow("EARFCN", snapshot.earfcn?.toString() ?: "—")
        InfoRow("Cell ID", snapshot.cellId?.toString() ?: "—")
        InfoRow("eNodeB", snapshot.raw["enodeb_id"]?.takeIf { it.isNotBlank() } ?: "—")
        InfoRow("Cell Lock", if (target != null) "موثق • PCI ${target.pci} / ${target.earfcn}" else "لا يوجد قفل موثق داخل الجلسة")
        if (guardEnabled) InfoRow("Tower Guard", guardMessage.ifBlank { "فعال" })
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onLockCurrent,
            enabled = !towerBusy && snapshot.pci != null && snapshot.earfcn != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (towerBusy) "جاري التحقق..." else "تثبيت الخلية الحالية والتحقق") }
        Spacer(Modifier.height(7.dp))
        OutlinedButton(onClick = onDiscover, enabled = !discoveryBusy && !towerBusy, modifier = Modifier.fillMaxWidth()) {
            Text(if (discoveryBusy) "جاري قراءة الخلايا..." else "عرض الخلايا القريبة من الراوتر")
        }
        Spacer(Modifier.height(7.dp))
        OutlinedButton(onClick = onClearLock, enabled = !towerBusy, modifier = Modifier.fillMaxWidth()) {
            Text("إلغاء Cell Lock والعودة إلى 0/0")
        }
    }
}

@Composable
private fun NearbyCellRow(cell: NearbyCell, busy: Boolean, onSelect: (NearbyCell) -> Unit) {
    TruthCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${cell.rat} • ${cell.band ?: "Band ؟"}", fontWeight = FontWeight.Bold)
                Text("PCI ${cell.pci ?: "—"} • ARFCN ${cell.arfcn ?: "—"}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "RSRP ${cell.rsrp?.let(::fmt) ?: "—"} • SINR ${cell.sinr?.let(::fmt) ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (cell.rat.equals("LTE", true)) {
                OutlinedButton(onClick = { onSelect(cell) }, enabled = !busy) { Text("اختبار") }
            } else {
                Text("قراءة فقط", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BandPanel(
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    busy: Boolean,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    onSetNetworkMode: (String) -> Unit
) {
    TruthCard {
        Text("التحكم بالترددات", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("القائمة مرشحات Profile وليست ادعاءً بأن كل Band مدعوم في كل Firmware. نجاح التطبيق يتطلب read‑back مطابقًا.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text("LTE", fontWeight = FontWeight.SemiBold)
        BandChips(capabilities.supportedLteBands, selectedLte, "B", onLteToggle)
        Button(onClick = onApplyLte, enabled = selectedLte.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
            Text("تطبيق LTE والتحقق")
        }
        if (capabilities.supportsNrBandLock) {
            Spacer(Modifier.height(12.dp))
            Text("5G NR", fontWeight = FontWeight.SemiBold)
            BandChips(capabilities.supportedNrBands, selectedNr, "N", onNrToggle)
            Button(onClick = onApplyNr, enabled = selectedNr.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
                Text("تطبيق NR والتحقق")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("الوضع المسموح", fontWeight = FontWeight.SemiBold)
        Text("هذا إعداد Desired State؛ لا يغيّر بطاقة الحالة إلى 5G حتى يظهر حامل NR حي موثق.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { onSetNetworkMode("Only_LTE") }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("4G") }
            OutlinedButton(onClick = { onSetNetworkMode("LTE_AND_5G") }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("4G+5G") }
            OutlinedButton(onClick = { onSetNetworkMode("Only_5G") }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("5G") }
        }
    }
}

@Composable
private fun BandChips(bands: Set<Int>, selected: Set<Int>, prefix: String, onToggle: (Int) -> Unit) {
    bands.sorted().chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            row.forEach { band ->
                FilterChip(
                    selected = band in selected,
                    onClick = { onToggle(band) },
                    label = { Text("$prefix$band") },
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun EvidenceCard(snapshot: RouterSnapshot) {
    val evidence = listOf(
        "verified_network_type" to snapshot.raw["_zte_verified_network_type"],
        "radio_mode" to snapshot.raw["_zte_radio_mode"],
        "lte_verified" to snapshot.raw["_zte_lte_active_verified"],
        "nr_verified" to snapshot.raw["_zte_nr_active_verified"],
        "ca_verified" to snapshot.raw["_zte_ca_verified"],
        "ca_active" to snapshot.raw["_zte_ca_active"],
        "ca_secondary_evidence" to snapshot.raw["_zte_ca_secondary_evidence"],
        "raw_network_type" to snapshot.raw["_zte_raw_network_type"]
    )
    TruthCard {
        Text("أدلة القراءة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("هذه هي المؤشرات التي تسمح للواجهة بقول 5G/CA أو تمنعها.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        evidence.forEach { (label, value) -> InfoRow(label, value ?: "—") }
    }
}

@Composable
private fun MessageCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = TruthSoftGold.copy(alpha = .35f)), shape = RoundedCornerShape(18.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(14.dp), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TruthCard(content: @Composable Column.() -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), content = content)
    }
}

@Composable
private fun MetricBox(label: String, value: String, unit: String, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Black, fontSize = 19.sp)
            Text(unit, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.48f))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(.52f))
    }
}

@Composable
private fun InfoRowLight(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = .72f))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private fun activeBandText(snapshot: RouterSnapshot): String {
    val nr = currentNrBands(snapshot).sorted().map { "N$it" }
    val lte = currentLteBands(snapshot).sorted().map { "B$it" }
    return (nr + lte).joinToString(" + ").ifBlank { "لا توجد ترددات حية موثقة" }
}

private fun currentLteBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
    bandNumber(snapshot.lteBand)?.let(::add)
    snapshot.cells.filter { it.role != CellRole.NR }.forEach { bandNumber(it.band)?.let(::add) }
}

private fun currentNrBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
    bandNumber(snapshot.nrBand)?.let(::add)
    snapshot.cells.filter { it.role == CellRole.NR }.forEach { bandNumber(it.band)?.let(::add) }
}

private fun toggle(current: Set<Int>, value: Int): Set<Int> = if (value in current) current - value else current + value
private fun bandNumber(value: String?): Int? = Regex("\\d+").find(value.orEmpty())?.value?.toIntOrNull()
private fun fmt(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
private fun goalLabel(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}
