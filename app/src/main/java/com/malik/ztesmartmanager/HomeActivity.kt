package com.malik.ztesmartmanager

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.NetworkPerformanceProbe
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.SmartBandOptimizer
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerLockEngine
import com.malik.ztesmartmanager.core.tower.TowerMatch
import com.malik.ztesmartmanager.core.tower.TowerTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = if (isSystemInDarkTheme()) HomeDarkColors else HomeLightColors) {
                    Surface(modifier = Modifier.fillMaxSize()) { ZteHomeApp() }
                }
            }
        }
    }
}

private val HomeLightColors = lightColorScheme(
    primary = Color(0xFF245C52),
    onPrimary = Color.White,
    surface = Color(0xFFF8F8F5),
    background = Color(0xFFF8F8F5),
    surfaceVariant = Color(0xFFECEFEA)
)

private val HomeDarkColors = darkColorScheme(
    primary = Color(0xFF8BCBC0),
    surface = Color(0xFF111513),
    background = Color(0xFF111513),
    surfaceVariant = Color(0xFF202824)
)

@Composable
private fun ZteHomeApp() {
    var routerAddress by rememberSaveable { mutableStateOf("192.168.0.1") }
    var password by rememberSaveable { mutableStateOf("") }
    var client by remember { mutableStateOf<ZteRouterClient?>(null) }
    var snapshot by remember { mutableStateOf<RouterSnapshot?>(null) }
    var status by remember { mutableStateOf("غير متصل") }
    var message by remember { mutableStateOf("") }
    var connectBusy by remember { mutableStateOf(false) }
    var speedBusy by remember { mutableStateOf(false) }
    var optimizeBusy by remember { mutableStateOf(false) }
    var towerBusy by remember { mutableStateOf(false) }
    var performance by remember { mutableStateOf<NetworkPerformance?>(null) }
    var towerEngine by remember { mutableStateOf<TowerLockEngine?>(null) }
    var towerTarget by remember { mutableStateOf<TowerTarget?>(null) }
    var towerGuardEnabled by rememberSaveable { mutableStateOf(false) }
    var towerGuardMessage by remember { mutableStateOf("") }
    var nearbyCells by remember { mutableStateOf<List<NearbyCell>>(emptyList()) }
    var neighborBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val probe = remember { NetworkPerformanceProbe() }

    fun connect() {
        if (connectBusy || routerAddress.isBlank() || password.isBlank()) return
        scope.launch {
            connectBusy = true
            status = "جاري الاتصال..."
            runCatching {
                val connected = ZteRouterClient(routerAddress)
                val profile = connected.login(password)
                val first = connected.readSnapshot()
                client = connected
                snapshot = first
                towerEngine = TowerLockEngine(connected)
                status = "متصل بالراوتر • ${profile.capabilities.modelFamily}"
                message = ""
            }.onFailure {
                client = null
                snapshot = null
                towerEngine = null
                status = it.message ?: "تعذر الاتصال بالراوتر"
            }
            connectBusy = false
        }
    }

    fun measureSpeed() {
        if (speedBusy) return
        scope.launch {
            speedBusy = true
            message = "جاري قياس السرعة..."
            runCatching { probe.measure(includeDownload = true) }
                .onSuccess {
                    performance = it
                    message = if (it.downloadMbps != null) "اكتمل قياس السرعة" else "لم تتوفر عينة تنزيل موثوقة"
                }
                .onFailure { message = "تعذر قياس السرعة: ${it.message.orEmpty()}" }
            speedBusy = false
        }
    }

    fun optimizeNetwork() {
        val connected = client ?: return
        if (optimizeBusy || towerBusy) return
        scope.launch {
            optimizeBusy = true
            message = "جاري اختبار الخيارات والتحقق منها..."
            runCatching {
                SmartBandOptimizer(connected).optimizeOnce(OptimizationGoal.BALANCED) { message = it }
            }.onSuccess { report ->
                message = report.message
                performance = report.best.performance
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
            }.onFailure { message = "لم يبدأ/يكتمل التحسين بأمان: ${it.message.orEmpty()}" }
            optimizeBusy = false
        }
    }

    fun lockCurrentTower() {
        val engine = towerEngine ?: return
        val current = snapshot ?: return
        if (towerBusy) return
        scope.launch {
            towerBusy = true
            message = "جاري إرسال القفل ثم قراءته والتحقق من الخلية الحية..."
            runCatching { engine.lockCurrent(current) }
                .onSuccess { result ->
                    val fullyVerified = result.match == TowerMatch.MATCHED
                    towerTarget = result.target.takeIf { fullyVerified }
                    towerGuardEnabled = fullyVerified
                    towerGuardMessage = result.message
                    message = result.message
                }
                .onFailure {
                    towerTarget = null
                    towerGuardEnabled = false
                    message = "تعذر تثبيت الخلية: ${it.message.orEmpty()}"
                }
            towerBusy = false
        }
    }

    fun discoverNearbyCells() {
        val engine = towerEngine ?: return
        if (neighborBusy) return
        scope.launch {
            neighborBusy = true
            message = "جاري قراءة الخلايا القريبة من الراوتر..."
            runCatching { engine.readNearbyCells() }
                .onSuccess { cells ->
                    nearbyCells = cells
                    message = if (cells.isEmpty()) {
                        "هذا الـFirmware لا يعرض Neighbor Cells موثوقة؛ لن يعرض التطبيق أبراجًا مخمّنة"
                    } else {
                        "قرأ الراوتر ${cells.size} خلية حالية/قريبة"
                    }
                }
                .onFailure { message = "تعذر قراءة الخلايا القريبة: ${it.message.orEmpty()}" }
            neighborBusy = false
        }
    }

    LaunchedEffect(client, towerGuardEnabled, towerTarget) {
        val connected = client ?: return@LaunchedEffect
        val engine = towerEngine ?: return@LaunchedEffect
        while (client === connected) {
            delay(2_000)
            if (optimizeBusy || towerBusy) continue
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
        LoginPanel(
            routerAddress = routerAddress,
            onRouterAddressChange = { routerAddress = it },
            password = password,
            onPasswordChange = { password = it },
            status = status,
            busy = connectBusy,
            onConnect = ::connect
        )
    } else {
        HomeDashboard(
            snapshot = snapshot,
            status = status,
            message = message,
            performance = performance,
            speedBusy = speedBusy,
            optimizeBusy = optimizeBusy,
            towerBusy = towerBusy,
            neighborBusy = neighborBusy,
            towerTarget = towerTarget,
            towerGuardEnabled = towerGuardEnabled,
            towerGuardMessage = towerGuardMessage,
            nearbyCells = nearbyCells,
            onMeasureSpeed = ::measureSpeed,
            onOptimize = ::optimizeNetwork,
            onLockCurrentTower = ::lockCurrentTower,
            onDiscoverNearbyCells = ::discoverNearbyCells,
            onTowerGuardChange = { enabled ->
                towerGuardEnabled = enabled && towerTarget != null
                if (!towerGuardEnabled) towerGuardMessage = "Tower Guard متوقف"
            },
            onDisconnect = {
                client = null
                snapshot = null
                towerEngine = null
                towerTarget = null
                towerGuardEnabled = false
                nearbyCells = emptyList()
                performance = null
                message = ""
                status = "غير متصل"
            }
        )
    }
}

@Composable
private fun LoginPanel(
    routerAddress: String,
    onRouterAddressChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    status: String,
    busy: Boolean,
    onConnect: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("ZTE Manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("إدارة موثقة للشبكة والخلية", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = routerAddress,
            onValueChange = onRouterAddressChange,
            label = { Text("عنوان الراوتر") },
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
        Spacer(Modifier.height(16.dp))
        Button(onClick = onConnect, enabled = !busy && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "جاري الاتصال..." else "اتصال")
        }
        Spacer(Modifier.height(10.dp))
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeDashboard(
    snapshot: RouterSnapshot?,
    status: String,
    message: String,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    optimizeBusy: Boolean,
    towerBusy: Boolean,
    neighborBusy: Boolean,
    towerTarget: TowerTarget?,
    towerGuardEnabled: Boolean,
    towerGuardMessage: String,
    nearbyCells: List<NearbyCell>,
    onMeasureSpeed: () -> Unit,
    onOptimize: () -> Unit,
    onLockCurrentTower: () -> Unit,
    onDiscoverNearbyCells: () -> Unit,
    onTowerGuardChange: (Boolean) -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ZTE Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onDisconnect) { Text("فصل") }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NetworkCard(snapshot, Modifier.weight(1f))
                SpeedCard(performance, speedBusy, onMeasureSpeed, Modifier.weight(1f))
            }
        }

        snapshot?.let { data ->
            item { PerformanceCard(data) }
            item {
                TowerCard(
                    snapshot = data,
                    towerTarget = towerTarget,
                    guardEnabled = towerGuardEnabled,
                    guardMessage = towerGuardMessage,
                    busy = towerBusy,
                    neighborBusy = neighborBusy,
                    onLock = onLockCurrentTower,
                    onDiscover = onDiscoverNearbyCells,
                    onGuardChange = onTowerGuardChange
                )
            }
        }

        if (nearbyCells.isNotEmpty()) {
            item { Text("الخلايا التي أعادها الراوتر فعليًا", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(nearbyCells) { cell -> NearbyCellCard(cell) }
        }

        if (message.isNotBlank()) item { MessageCard(message) }

        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("التحكم والأدوات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onOptimize, enabled = !optimizeBusy && !towerBusy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (optimizeBusy) "جاري التحسين والتحقق..." else "تحسين الشبكة تلقائيًا")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(context, MainActivity::class.java)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("قفل الترددات والأدوات المتقدمة") }
                }
            }
        }
    }
}

@Composable
private fun NetworkCard(snapshot: RouterSnapshot?, modifier: Modifier = Modifier) {
    val nrVerified = snapshot?.raw?.get("_zte_nr_active_verified").equals("true", ignoreCase = true)
    val lteVerified = snapshot?.raw?.get("_zte_lte_active_verified").equals("true", ignoreCase = true)
    val rsrp = when {
        nrVerified -> snapshot?.nrRsrp
        lteVerified -> snapshot?.lteRsrp
        else -> null
    }
    val sinr = when {
        nrVerified -> snapshot?.nrSinr
        lteVerified -> snapshot?.lteSinr
        else -> null
    }

    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("حالة الشبكة", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                snapshot?.let(::connectionType) ?: "غير مؤكد",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text("RSRP: ${rsrp?.let(::formatNumber) ?: "—"} dBm", style = MaterialTheme.typography.bodySmall)
            Text("SINR: ${sinr?.let(::formatNumber) ?: "—"} dB", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SpeedCard(performance: NetworkPerformance?, busy: Boolean, onMeasure: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("سرعة مقاسة", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                performance?.downloadMbps?.let { "${formatNumber(it)} Mb/s" } ?: "— Mb/s",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(performance?.latencyMs?.let { "Ping ${formatNumber(it)} ms" } ?: " ", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onMeasure, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "قياس..." else "قياس الآن")
            }
        }
    }
}

@Composable
private fun PerformanceCard(snapshot: RouterSnapshot) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("الأداء الموثق", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoRow("اسم الشبكة", operatorName(snapshot))
            InfoRow("نوع الاتصال", connectionType(snapshot))
            InfoRow("الترددات النشطة", activeBands(snapshot))
            InfoRow("Carrier Aggregation", carrierAggregationStatus(snapshot))
        }
    }
}

@Composable
private fun TowerCard(
    snapshot: RouterSnapshot,
    towerTarget: TowerTarget?,
    guardEnabled: Boolean,
    guardMessage: String,
    busy: Boolean,
    neighborBusy: Boolean,
    onLock: () -> Unit,
    onDiscover: () -> Unit,
    onGuardChange: (Boolean) -> Unit
) {
    val currentCellId = snapshot.cellId?.toString() ?: "—"
    val enodeb = snapshot.raw["enodeb_id"]?.takeIf { it.isNotBlank() && it != "--" } ?: "—"
    val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", true)

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("البرج والخلية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoRow("PCI", snapshot.pci?.toString() ?: "—")
            InfoRow("EARFCN", snapshot.earfcn?.toString() ?: "—")
            InfoRow("Cell ID", currentCellId)
            InfoRow("eNodeB", enodeb)
            InfoRow("Band", snapshot.lteBand ?: "—")
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onLock,
                enabled = !busy && lteVerified && snapshot.pci != null && snapshot.earfcn != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "جاري القفل والتحقق..." else "تثبيت الخلية الحالية") }
            Spacer(Modifier.height(7.dp))
            OutlinedButton(onClick = onDiscover, enabled = !neighborBusy, modifier = Modifier.fillMaxWidth()) {
                Text(if (neighborBusy) "جاري القراءة..." else "عرض الخلايا القريبة")
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tower Guard", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (towerTarget == null) "غير متاح حتى ينجح القفل وread-back والتحقق الحي" else guardMessage.ifBlank { "يراقب هوية الخلية المستهدفة" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = guardEnabled, onCheckedChange = onGuardChange, enabled = towerTarget != null)
            }
        }
    }
}

@Composable
private fun NearbyCellCard(cell: NearbyCell) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("${cell.rat} • ${cell.band ?: "Band غير معروف"}", fontWeight = FontWeight.Bold)
            InfoRow("PCI", cell.pci?.toString() ?: "—")
            InfoRow("ARFCN", cell.arfcn?.toString() ?: "—")
            InfoRow("RSRP", cell.rsrp?.let { "${formatNumber(it)} dBm" } ?: "—")
            InfoRow("SINR", cell.sinr?.let { "${formatNumber(it)} dB" } ?: "—")
            InfoRow("RSRQ", cell.rsrq?.let { "${formatNumber(it)} dB" } ?: "—")
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun connectionType(snapshot: RouterSnapshot): String =
    snapshot.raw["_zte_verified_network_type"]?.takeIf { it.isNotBlank() }
        ?: snapshot.networkType?.takeIf { it.isNotBlank() }
        ?: "غير مؤكد"

private fun operatorName(snapshot: RouterSnapshot): String {
    val provider = listOf("network_provider", "network_provider_fullname", "network_operator", "operator_name")
        .asSequence().mapNotNull { snapshot.raw[it]?.trim()?.takeIf(String::isNotBlank) }.firstOrNull()
    if (provider != null) return provider
    return when (snapshot.operatorCode?.filter(Char::isDigit)) {
        "42001" -> "stc ksa"
        "42003" -> "Mobily"
        "42004" -> "Zain KSA"
        else -> snapshot.operatorCode?.takeIf { it.isNotBlank() } ?: "غير مؤكد"
    }
}

private fun activeLteBands(snapshot: RouterSnapshot): List<String> = snapshot.cells
    .filter { it.role != CellRole.NR }
    .mapNotNull { it.band?.uppercase()?.takeIf(String::isNotBlank) }
    .distinct()

private fun activeBands(snapshot: RouterSnapshot): String {
    val bands = snapshot.cells
        .mapNotNull { it.band?.uppercase()?.takeIf(String::isNotBlank) }
        .distinct()
    return bands.joinToString(" + ").ifBlank { "غير مؤكد" }
}

private fun carrierAggregationStatus(snapshot: RouterSnapshot): String {
    val verified = snapshot.raw["_zte_ca_verified"].equals("true", true)
    if (!verified) return "غير مؤكد"
    if (!snapshot.caActive) return "غير نشط"

    val lteBands = activeLteBands(snapshot)
    return if (lteBands.size >= 2) {
        "نشط فعليًا • ${lteBands.joinToString(" + ")}"
    } else {
        "نشط حسب حالة الراوتر • تفاصيل الحوامل غير مكتملة"
    }
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
