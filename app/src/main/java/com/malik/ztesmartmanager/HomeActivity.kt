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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.NetworkPerformanceProbe
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.SmartBandOptimizer
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
    var controlBusy by remember { mutableStateOf(false) }
    var speedBusy by remember { mutableStateOf(false) }
    var optimizeBusy by remember { mutableStateOf(false) }
    var performance by remember { mutableStateOf<NetworkPerformance?>(null) }
    var selectedLte by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedNr by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showBandControls by rememberSaveable { mutableStateOf(false) }
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
                selectedLte = currentLteBandsHome(first)
                selectedNr = currentNrBandsHome(first)
                status = "متصل • ${profile.capabilities.modelFamily}"
                message = ""
            }.onFailure {
                client = null
                snapshot = null
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
                    message = "اكتمل قياس السرعة"
                }
                .onFailure { message = "تعذر قياس السرعة: ${it.message.orEmpty()}" }
            speedBusy = false
        }
    }

    fun optimizeNetwork() {
        val connected = client ?: return
        if (optimizeBusy || controlBusy) return
        scope.launch {
            optimizeBusy = true
            message = "جاري تحليل الترددات واختيار الأفضل..."
            runCatching {
                SmartBandOptimizer(connected).optimizeOnce(OptimizationGoal.BALANCED) { message = it }
            }.onSuccess { report ->
                message = report.message
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                selectedLte = snapshot?.let(::currentLteBandsHome).orEmpty()
                selectedNr = snapshot?.let(::currentNrBandsHome).orEmpty()
                performance = report.best.performance
            }.onFailure { message = "تعذر تحسين الشبكة: ${it.message.orEmpty()}" }
            optimizeBusy = false
        }
    }

    LaunchedEffect(client) {
        val connected = client ?: return@LaunchedEffect
        while (client === connected) {
            delay(2_000)
            if (controlBusy || optimizeBusy) continue
            runCatching { connected.readSnapshot() }.onSuccess { snapshot = it }
        }
    }

    val connected = client
    if (connected == null) {
        HomeLoginScreen(
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

    HomeDashboard(
        snapshot = snapshot,
        capabilities = connected.profile.capabilities,
        status = status,
        message = message,
        performance = performance,
        speedBusy = speedBusy,
        optimizeBusy = optimizeBusy,
        controlBusy = controlBusy,
        selectedLte = selectedLte,
        selectedNr = selectedNr,
        showBandControls = showBandControls,
        onMeasureSpeed = ::measureSpeed,
        onOptimize = ::optimizeNetwork,
        onToggleBandControls = { showBandControls = !showBandControls },
        onLteToggle = { band -> selectedLte = toggleHome(selectedLte, band) },
        onNrToggle = { band -> selectedNr = toggleHome(selectedNr, band) },
        onApplyLte = {
            if (selectedLte.isNotEmpty() && !controlBusy) scope.launch {
                controlBusy = true
                message = connected.setLteBands(selectedLte).message
                delay(900)
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                controlBusy = false
            }
        },
        onApplyNr = {
            if (selectedNr.isNotEmpty() && !controlBusy) scope.launch {
                controlBusy = true
                message = connected.setNrBands(selectedNr).message
                delay(900)
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                controlBusy = false
            }
        },
        onSetNetworkMode = { mode ->
            if (!controlBusy) scope.launch {
                controlBusy = true
                message = connected.setNetworkMode(mode).message
                delay(700)
                snapshot = runCatching { connected.readSnapshot() }.getOrNull() ?: snapshot
                controlBusy = false
            }
        },
        onLockCurrentCell = {
            val current = snapshot
            val pci = current?.pci
            val earfcn = current?.earfcn
            if (pci != null && earfcn != null && !controlBusy) scope.launch {
                controlBusy = true
                message = connected.setCellLock(pci, earfcn).message
                controlBusy = false
            }
        },
        onDisconnect = {
            client = null
            snapshot = null
            performance = null
            message = ""
            status = "غير متصل"
        }
    )
}

@Composable
private fun HomeLoginScreen(
    routerAddress: String,
    onRouterAddressChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    status: String,
    busy: Boolean,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("ZTE Manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("واجهة مباشرة لحالة الشبكة والتحكم", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Button(
            onClick = onConnect,
            enabled = !busy && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "جاري الاتصال..." else "اتصال") }
        Spacer(Modifier.height(10.dp))
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeDashboard(
    snapshot: RouterSnapshot?,
    capabilities: RouterCapabilities,
    status: String,
    message: String,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    optimizeBusy: Boolean,
    controlBusy: Boolean,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    showBandControls: Boolean,
    onMeasureSpeed: () -> Unit,
    onOptimize: () -> Unit,
    onToggleBandControls: () -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onLockCurrentCell: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ZTE Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onDisconnect) { Text("فصل") }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NetworkStatusCard(snapshot, Modifier.weight(1f))
                SpeedCard(performance, speedBusy, onMeasureSpeed, Modifier.weight(1f))
            }
        }

        snapshot?.let { data ->
            item { PerformanceCard(data) }
            item { TowerMapCard(data) }
        }

        if (message.isNotBlank()) item { MessageCard(message) }

        item {
            ControlToolsCard(
                optimizeBusy = optimizeBusy,
                controlBusy = controlBusy,
                showBandControls = showBandControls,
                onToggleBandControls = onToggleBandControls,
                onOptimize = onOptimize,
                onOpenAdvanced = { context.startActivity(Intent(context, MainActivity::class.java)) }
            )
        }

        if (showBandControls) {
            item { NetworkModeCompactCard(controlBusy, onSetNetworkMode) }
            if (capabilities.supportsLteBandLock) {
                item {
                    CompactBandSelector(
                        title = "ترددات 4G",
                        bands = capabilities.supportedLteBands,
                        selected = selectedLte,
                        prefix = "B",
                        busy = controlBusy,
                        onToggle = onLteToggle,
                        onApply = onApplyLte
                    )
                }
            }
            if (capabilities.supportsNrBandLock) {
                item {
                    CompactBandSelector(
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
            snapshot?.let { data ->
                if (capabilities.supportsCellLock && data.pci != null && data.earfcn != null) {
                    item {
                        OutlinedButton(
                            onClick = onLockCurrentCell,
                            enabled = !controlBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("تثبيت الخلية الحالية • PCI ${data.pci}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkStatusCard(snapshot: RouterSnapshot?, modifier: Modifier = Modifier) {
    val type = snapshot?.let(::connectionTypeHome) ?: "—"
    val nrActive = snapshot?.raw?.get("_zte_nr_active").equals("true", ignoreCase = true)
    val rsrp = when {
        snapshot == null -> null
        nrActive -> snapshot.nrRsrp ?: snapshot.lteRsrp
        else -> snapshot.lteRsrp
    }
    val sinr = when {
        snapshot == null -> null
        nrActive -> snapshot.nrSinr ?: snapshot.lteSinr
        else -> snapshot.lteSinr
    }

    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("حالة الشبكة", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(type, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("RSRP: ${rsrp?.let(::formatHome) ?: "—"} dBm", style = MaterialTheme.typography.bodySmall)
            Text("SINR: ${sinr?.let(::formatHome) ?: "—"} dB", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SpeedCard(
    performance: NetworkPerformance?,
    busy: Boolean,
    onMeasureSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("السرعة الحالية", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                performance?.downloadMbps?.let { "${formatHome(it)} Mb/s" } ?: "— Mb/s",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            performance?.latencyMs?.let {
                Text("Ping ${formatHome(it)} ms", style = MaterialTheme.typography.bodySmall)
            } ?: Spacer(Modifier.height(18.dp))
            Spacer(Modifier.height(8.dp))
            Button(onClick = onMeasureSpeed, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "قياس..." else "قياس السرعة")
            }
        }
    }
}

@Composable
private fun PerformanceCard(snapshot: RouterSnapshot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("الأداء", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            HomeInfoRow("اسم الشبكة", operatorNameHome(snapshot))
            HomeInfoRow("نوع الاتصال", connectionTypeHome(snapshot))
            HomeInfoRow("حالة الترددات", bandStateHome(snapshot))
        }
    }
}

@Composable
private fun TowerMapCard(snapshot: RouterSnapshot) {
    val detected = snapshot.cells.size.coerceAtLeast(if (snapshot.pci != null) 1 else 0)
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("خريطة الأبراج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("الأبراج/الخلايا المكتشفة من الراوتر: $detected", fontWeight = FontWeight.SemiBold)
            Text(
                "لن يعرض التطبيق نقاطًا جغرافية وهمية. الراوتر الحالي يرسل PCI/Cell ID والترددات، لكنه لا يرسل إحداثيات البرج؛ ستظهر الخريطة الجغرافية فقط عند ربط مصدر إحداثيات موثوق.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ControlToolsCard(
    optimizeBusy: Boolean,
    controlBusy: Boolean,
    showBandControls: Boolean,
    onToggleBandControls: () -> Unit,
    onOptimize: () -> Unit,
    onOpenAdvanced: () -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("التحكم والأدوات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onToggleBandControls, enabled = !controlBusy, modifier = Modifier.weight(1f)) {
                    Text(if (showBandControls) "إخفاء الترددات" else "قفل الترددات")
                }
                Button(onClick = onOptimize, enabled = !optimizeBusy && !controlBusy, modifier = Modifier.weight(1f)) {
                    Text(if (optimizeBusy) "تحسين..." else "تحسين الشبكة")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenAdvanced, modifier = Modifier.fillMaxWidth()) {
                Text("الأدوات المتقدمة والتشخيص")
            }
        }
    }
}

@Composable
private fun NetworkModeCompactCard(busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("وضع الشبكة", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onSetNetworkMode("Only_LTE") }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("4G") }
                OutlinedButton(onClick = { onSetNetworkMode("LTE_AND_5G") }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("4G+5G") }
                OutlinedButton(onClick = { onSetNetworkMode("Only_5G") }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("5G") }
            }
        }
    }
}

@Composable
private fun CompactBandSelector(
    title: String,
    bands: Set<Int>,
    selected: Set<Int>,
    prefix: String,
    busy: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit
) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            bands.sorted().chunked(4).forEach { rowBands ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowBands.forEach { band ->
                        FilterChip(
                            selected = band in selected,
                            onClick = { onToggle(band) },
                            label = { Text("$prefix$band") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - rowBands.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onApply, enabled = selected.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
                Text("تطبيق والتحقق")
            }
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(14.dp))
    }
}

@Composable
private fun HomeInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun connectionTypeHome(snapshot: RouterSnapshot): String {
    val mode = snapshot.raw["_zte_radio_mode"].orEmpty()
    val nrActive = snapshot.raw["_zte_nr_active"].equals("true", ignoreCase = true)
    return when {
        mode == "NSA_ACTIVE" -> "5G NSA / 4G"
        mode == "SA_ACTIVE" -> "5G SA"
        nrActive && snapshot.lteRsrp != null -> "5G / 4G"
        nrActive -> "5G"
        snapshot.caActive -> "4G+"
        snapshot.lteRsrp != null -> "4G"
        else -> snapshot.networkType?.takeIf { it.isNotBlank() } ?: "—"
    }
}

private fun operatorNameHome(snapshot: RouterSnapshot): String {
    val raw = listOf("network_provider", "network_operator", "operator_name", "spn_name")
        .asSequence()
        .mapNotNull { snapshot.raw[it]?.trim()?.takeIf(String::isNotBlank) }
        .firstOrNull()
    if (raw != null) return raw

    return when (snapshot.operatorCode?.filter(Char::isDigit)) {
        "42001" -> "stc ksa"
        "42003" -> "Mobily"
        "42004" -> "Zain KSA"
        else -> snapshot.operatorCode?.takeIf { it.isNotBlank() } ?: "—"
    }
}

private fun bandStateHome(snapshot: RouterSnapshot): String {
    val lte = buildList {
        extractBandHome(snapshot.lteBand)?.let { add("B$it") }
        snapshot.cells.filter { it.role != CellRole.NR }.forEach { cell ->
            extractBandHome(cell.band)?.let { band -> if ("B$band" !in this) add("B$band") }
        }
    }
    val nr = buildList {
        extractBandHome(snapshot.nrBand)?.let { add("N$it") }
        snapshot.cells.filter { it.role == CellRole.NR }.forEach { cell ->
            extractBandHome(cell.band)?.let { band -> if ("N$band" !in this) add("N$band") }
        }
    }
    val bands = (lte + nr).joinToString(" + ").ifBlank { "غير معروف" }
    val aggregation = if (snapshot.caActive) "مدمجة" else "غير مدمجة"
    return "$bands | $aggregation"
}

private fun currentLteBandsHome(snapshot: RouterSnapshot): Set<Int> = buildSet {
    extractBandHome(snapshot.lteBand)?.let(::add)
    snapshot.cells.filter { it.role != CellRole.NR }.forEach { extractBandHome(it.band)?.let(::add) }
}

private fun currentNrBandsHome(snapshot: RouterSnapshot): Set<Int> = buildSet {
    extractBandHome(snapshot.nrBand)?.let(::add)
    snapshot.cells.filter { it.role == CellRole.NR }.forEach { extractBandHome(it.band)?.let(::add) }
}

private fun extractBandHome(value: String?): Int? = Regex("\\d+").find(value.orEmpty())?.value?.toIntOrNull()?.takeIf { it > 0 }
private fun toggleHome(current: Set<Int>, band: Int): Set<Int> = if (band in current) current - band else current + band
private fun formatHome(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
