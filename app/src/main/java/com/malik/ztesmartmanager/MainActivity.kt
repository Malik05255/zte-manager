package com.malik.ztesmartmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.core.content.ContextCompat
import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.PlacementGuidance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = AppColors) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        LocalNetworkPermissionGate {
                            ZteManagerApp()
                        }
                    }
                }
            }
        }
    }
}

private val AppColors = lightColorScheme(
    primary = Color(0xFF245C52),
    onPrimary = Color.White,
    surface = Color(0xFFF8F8F5),
    background = Color(0xFFF8F8F5),
    surfaceVariant = Color(0xFFECEFEA)
)

@Composable
private fun LocalNetworkPermissionGate(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT < 37) {
        content()
        return
    }

    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }

    if (granted) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("يحتاج التطبيق إذن الشبكة المحلية", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("يُستخدم الإذن فقط للتواصل مباشرة مع راوتر ZTE داخل شبكتك المحلية.")
            Spacer(Modifier.height(20.dp))
            Button(onClick = { launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK) }) {
                Text("منح الإذن")
            }
        }
    }
}

@Composable
private fun ZteManagerApp() {
    var routerAddress by rememberSaveable { mutableStateOf("192.168.0.1") }
    var password by rememberSaveable { mutableStateOf("") }
    var client by remember { mutableStateOf<ZteRouterClient?>(null) }
    var snapshot by remember { mutableStateOf<RouterSnapshot?>(null) }
    var status by remember { mutableStateOf("غير متصل") }
    var busy by remember { mutableStateOf(false) }
    var placementMode by rememberSaveable { mutableStateOf(false) }
    var placementReading by remember { mutableStateOf<PlacementReading?>(null) }
    val qualityEngine = remember { NetworkQualityEngine() }
    val scope = rememberCoroutineScope()

    fun connect() {
        if (busy || password.isBlank() || routerAddress.isBlank()) return
        scope.launch {
            busy = true
            status = "جاري الاتصال..."
            runCatching {
                val newClient = ZteRouterClient(routerAddress)
                val profile = newClient.login(password)
                val firstSnapshot = newClient.readSnapshot()
                client = newClient
                snapshot = firstSnapshot
                qualityEngine.reset()
                placementReading = qualityEngine.add(firstSnapshot)
                status = "متصل • ${profile.capabilities.modelFamily}"
            }.onFailure {
                client = null
                snapshot = null
                status = it.message ?: "تعذر الاتصال بالراوتر"
            }
            busy = false
        }
    }

    LaunchedEffect(client, placementMode) {
        val connected = client ?: return@LaunchedEffect
        if (placementMode) {
            qualityEngine.reset()
            snapshot?.let { placementReading = qualityEngine.add(it) }
        }

        while (client === connected) {
            delay(if (placementMode) 700 else 2_000)
            runCatching { connected.readSnapshot() }
                .onSuccess { latest ->
                    snapshot = latest
                    if (placementMode) placementReading = qualityEngine.add(latest)
                }
                .onFailure { status = "انقطع التحديث مؤقتًا: ${it.message.orEmpty()}" }
        }
    }

    if (client == null) {
        LoginScreen(
            routerAddress = routerAddress,
            onRouterAddressChange = { routerAddress = it },
            password = password,
            onPasswordChange = { password = it },
            status = status,
            busy = busy,
            onConnect = ::connect
        )
    } else {
        DashboardScreen(
            snapshot = snapshot,
            status = status,
            placementMode = placementMode,
            placementReading = placementReading,
            onPlacementToggle = {
                placementMode = !placementMode
                if (!placementMode) placementReading = null
            },
            onDisconnect = {
                client = null
                snapshot = null
                placementMode = false
                placementReading = null
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
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("ZTE Smart Manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("إدارة وقياس وتحسين راوترات ZTE", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("كلمة المرور تُستخدم محليًا للمصادقة مع الراوتر ولا تُرسل إلى أي خدمة خارجية.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DashboardScreen(
    snapshot: RouterSnapshot?,
    status: String,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    onPlacementToggle: () -> Unit,
    onDisconnect: () -> Unit
) {
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

        if (placementMode && placementReading != null) {
            item { PlacementCard(placementReading) }
        }

        item {
            Button(onClick = onPlacementToggle, modifier = Modifier.fillMaxWidth()) {
                Text(if (placementMode) "إيقاف مساعد أفضل مكان" else "مساعد أفضل مكان")
            }
        }

        snapshot?.let { data ->
            item { SignalCard(data) }
            item { NetworkCard(data) }
            if (data.cells.isNotEmpty()) {
                item {
                    Text("الخلايا والتجميع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(data.cells) { cell -> CellCard(cell) }
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
            MetricRow("RSRP", snapshot.lteRsrp ?: snapshot.nrRsrp, "dBm")
            MetricRow("RSRQ", snapshot.lteRsrq, "dB")
            MetricRow("SINR", snapshot.lteSinr ?: snapshot.nrSinr, "dB")
            MetricRow("RSSI", snapshot.lteRssi, "dBm")
        }
    }
}

@Composable
private fun NetworkCard(snapshot: RouterSnapshot) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("الشبكة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            InfoRow("النوع", snapshot.networkType ?: "—")
            InfoRow("4G", snapshot.lteBand ?: "—")
            InfoRow("5G", snapshot.nrBand ?: "—")
            InfoRow("PCI", snapshot.pci?.toString() ?: "—")
            InfoRow("EARFCN", snapshot.earfcn?.toString() ?: "—")
            InfoRow("Carrier Aggregation", if (snapshot.caActive) "نشط" else "غير نشط")
        }
    }
}

@Composable
private fun PlacementCard(reading: PlacementReading) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("مساعد أفضل مكان", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("${reading.score.total} / 100", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(reading.score.label)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { reading.score.total / 100f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Text(placementGuidanceText(reading.guidance), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            InfoRow("قوة الإشارة", "${reading.score.signal}/100")
            InfoRow("نظافة الإشارة", "${reading.score.cleanliness}/100")
            InfoRow("جودة الإشارة", "${reading.score.quality}/100")
            InfoRow("الثبات", "${reading.score.stability}/100")
            InfoRow("أفضل قراءة", "${reading.bestScore}/100")
        }
    }
}

private fun placementGuidanceText(guidance: PlacementGuidance): String = when (guidance) {
    PlacementGuidance.INITIAL -> "ابدأ بتحريك الراوتر ببطء عدة سنتيمترات"
    PlacementGuidance.MUCH_BETTER -> "تحسن كبير — استمر قليلًا في هذا الاتجاه"
    PlacementGuidance.BETTER -> "أفضل — استمر ببطء"
    PlacementGuidance.STABLE -> "التغيير بسيط — حرّكه قليلًا للمقارنة"
    PlacementGuidance.WORSE -> "المكان أصبح أسوأ — ارجع قليلًا"
    PlacementGuidance.BEST_SO_FAR -> "ممتاز — هذه أفضل نقطة حتى الآن"
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

private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
