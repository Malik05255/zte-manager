package com.malik.ztesmartmanager

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetry
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.ConnectedDevice
import com.malik.ztesmartmanager.core.model.ConnectedDeviceParser
import com.malik.ztesmartmanager.core.model.DeviceTransport
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget
import kotlin.math.roundToInt

private val NvBg = Color(0xFFF3F6FA)
private val NvPaper = Color(0xFFFFFFFF)
private val NvInk = Color(0xFF071525)
private val NvMuted = Color(0xFF728096)
private val NvLine = Color(0xFFE5EAF0)
private val NvBlue = Color(0xFF2D7CFF)
private val NvCyan = Color(0xFF10C7D5)
private val NvMint = Color(0xFF20C888)
private val NvPurple = Color(0xFF8066E8)
private val NvAmber = Color(0xFFF2A93B)
private val NvRed = Color(0xFFE45D68)
private val NvNight = Color(0xFF06101F)
private val NvNight2 = Color(0xFF0A2638)
private val NvNight3 = Color(0xFF0A4654)

private enum class NvScreen { HOME, NETWORK, TOWERS, BANDS, CLIENTS, PLACEMENT, TOOLS }

@Composable
fun NovaDashboard(
    snapshot: RouterSnapshot?,
    capabilities: RouterCapabilities,
    runtime: RuntimeCapabilityReport?,
    traffic: TrafficTelemetry?,
    thermal: ThermalTelemetry?,
    telemetrySamples: List<SafeTelemetrySample>,
    stability: ConnectionStabilityReport,
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
    speedBusy: Boolean,
    lastPerformance: NetworkPerformance?,
    safetyBackupAvailable: Boolean,
    nearbyCells: List<NearbyCell>,
    scanBusy: Boolean,
    towerTarget: TowerTarget?,
    towerGuardEnabled: Boolean,
    towerGuardStatus: TowerGuardStatus?,
    onDisconnect: () -> Unit,
    onSpeedTest: () -> Unit,
    onPlacementToggle: () -> Unit,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onAntennaState: (Int) -> Unit,
    onScanCells: () -> Unit,
    onLockCurrentCell: () -> Unit,
    onLockNearbyCell: (NearbyCell) -> Unit,
    onClearCellLock: () -> Unit,
    onTowerGuardChange: (Boolean) -> Unit,
    onRestoreSafetyBackup: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onShareDiagnostics: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val history = remember { mutableStateListOf(NvScreen.HOME) }
        val screen = history.last()
        val aliases = remember { mutableStateMapOf<String, String>() }
        var soundEnabled by rememberSaveable { mutableStateOf(true) }

        fun navigate(target: NvScreen) {
            if (history.lastOrNull() != target) history += target
        }
        fun root(target: NvScreen) {
            history.clear()
            history += target
        }

        BackHandler(enabled = history.size > 1 || screen != NvScreen.HOME) {
            if (history.size > 1) history.removeAt(history.lastIndex) else root(NvScreen.HOME)
        }

        val wifi = remember(snapshot?.raw?.get("station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("station_list"), DeviceTransport.WIFI)
        }
        val lan = remember(snapshot?.raw?.get("lan_station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("lan_station_list"), DeviceTransport.LAN)
        }
        val devices = remember(wifi, lan) { ConnectedDeviceParser.merge(wifi, lan) }

        PlacementSoundEffect(soundEnabled, placementMode && screen == NvScreen.PLACEMENT, placementReading)

        Box(Modifier.fillMaxSize().background(NvBg)) {
            Column(Modifier.fillMaxSize()) {
                NovaTopBar(
                    screen = screen,
                    connected = snapshot != null,
                    onBack = { if (history.size > 1) history.removeAt(history.lastIndex) else root(NvScreen.HOME) }
                )

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (snapshot == null) {
                        NovaEmpty("جاري فتح جلسة الراوتر", status.ifBlank { "بانتظار أول قراءة حقيقية" })
                    } else when (screen) {
                        NvScreen.HOME -> NovaHome(
                            snapshot, status, operationMessage, lastPerformance, speedBusy, controlBusy,
                            devices, nearbyCells, placementReading, onSpeedTest, onSetNetworkMode,
                            { navigate(NvScreen.NETWORK) }, { navigate(NvScreen.TOWERS) },
                            { navigate(NvScreen.BANDS) }, { navigate(NvScreen.CLIENTS) },
                            { navigate(NvScreen.PLACEMENT) }, { navigate(NvScreen.TOOLS) }
                        )
                        NvScreen.NETWORK -> NovaNetwork(snapshot, traffic, thermal, stability, controlBusy, onSetNetworkMode)
                        NvScreen.TOWERS -> NovaTowers(
                            snapshot, nearbyCells, scanBusy, controlBusy, towerTarget, towerGuardEnabled,
                            towerGuardStatus, onScanCells, onLockCurrentCell, onLockNearbyCell,
                            onClearCellLock, onTowerGuardChange
                        )
                        NvScreen.BANDS -> NovaBands(
                            snapshot, capabilities, selectedLte, selectedNr, controlBusy,
                            onLteToggle, onNrToggle, onApplyLte, onApplyNr
                        )
                        NvScreen.CLIENTS -> NovaClients(devices, aliases)
                        NvScreen.PLACEMENT -> NovaPlacement(
                            placementMode, placementReading, soundEnabled, { soundEnabled = it }, onPlacementToggle
                        )
                        NvScreen.TOOLS -> NovaTools(
                            runtime, telemetrySamples.size, stability, capabilities, smartMode, smartGoal,
                            smartBusy, smartReport, safetyBackupAvailable, controlBusy,
                            onSmartModeChange, onSmartGoalChange, onOptimizeNow, onRestoreSafetyBackup,
                            onAntennaState, onCopyDiagnostics, onShareDiagnostics, onDisconnect
                        )
                    }
                }

                NovaNav(screen) { root(it) }
            }
        }
    }
}

@Composable
private fun NovaTopBar(screen: NvScreen, connected: Boolean, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (screen != NvScreen.HOME) {
            Surface(
                modifier = Modifier.size(38.dp).clickable(onClick = onBack),
                shape = CircleShape,
                color = NvNight
            ) { Box(contentAlignment = Alignment.Center) { Text("‹", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold) } }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(novaTitle(screen), color = NvInk, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(novaSubtitle(screen), color = NvMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        NovaLiveBadge(connected)
    }
}

private fun novaTitle(screen: NvScreen) = when (screen) {
    NvScreen.HOME -> "HAI"
    NvScreen.NETWORK -> "الشبكة"
    NvScreen.TOWERS -> "الخلايا"
    NvScreen.BANDS -> "الترددات"
    NvScreen.CLIENTS -> "الأجهزة"
    NvScreen.PLACEMENT -> "المكان"
    NvScreen.TOOLS -> "المختبر"
}

private fun novaSubtitle(screen: NvScreen) = when (screen) {
    NvScreen.HOME -> "لوحة حيّة للراوتر • بدون أرقام تجريبية"
    NvScreen.NETWORK -> "قراءة وتحكم مع تحقق read-back"
    NvScreen.TOWERS -> "الخريطة حقيقية، والإحداثيات لا تُختلق"
    NvScreen.BANDS -> "اختيار ثم تحقق قبل إعلان النجاح"
    NvScreen.CLIENTS -> "ما يكشفه الراوتر فقط • لا نختلق جهازًا"
    NvScreen.PLACEMENT -> "حوّل الإشارة إلى دليل حركة مباشر"
    NvScreen.TOOLS -> "تحسين، أمان، وتشخيص"
}

@Composable
private fun NovaLiveBadge(connected: Boolean) {
    val transition = rememberInfiniteTransition(label = "nova-live")
    val alpha by transition.animateFloat(
        initialValue = .35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "nova-live-alpha"
    )
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (connected) Color(0xFFE8F8F1) else Color(0xFFFFECEE))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).graphicsLayer(alpha = if (connected) alpha else 1f).clip(CircleShape).background(if (connected) NvMint else NvRed))
        Spacer(Modifier.width(6.dp))
        Text(if (connected) "LIVE" else "OFF", color = NvInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun NovaNav(screen: NvScreen, onSelect: (NvScreen) -> Unit) {
    Row(
        Modifier.navigationBarsPadding().fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 9.dp)
            .shadow(18.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(NvNight)
            .padding(horizontal = 5.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple(NvScreen.HOME, "الرئيسية", "●"),
            Triple(NvScreen.TOWERS, "الخلايا", "⌁"),
            Triple(NvScreen.BANDS, "الباند", "≋"),
            Triple(NvScreen.CLIENTS, "الأجهزة", "◉"),
            Triple(NvScreen.TOOLS, "المختبر", "✦")
        ).forEach { (target, label, icon) ->
            val selected = screen == target
            val scale by animateFloatAsState(if (selected) 1f else .92f, tween(180), label = "nav-scale")
            Column(
                Modifier.weight(1f).graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(RoundedCornerShape(17.dp)).clickable { onSelect(target) }
                    .background(if (selected) Color.White.copy(alpha = .12f) else Color.Transparent)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(icon, color = if (selected) NvCyan else Color(0xFF91A1B7), fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(label, color = if (selected) Color.White else Color(0xFF91A1B7), fontSize = 8.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun NovaHome(
    snapshot: RouterSnapshot,
    status: String,
    operationMessage: String,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    controlBusy: Boolean,
    devices: List<ConnectedDevice>,
    nearbyCells: List<NearbyCell>,
    placementReading: PlacementReading?,
    onSpeedTest: () -> Unit,
    onMode: (String) -> Unit,
    onNetwork: () -> Unit,
    onTowers: () -> Unit,
    onBands: () -> Unit,
    onClients: () -> Unit,
    onPlacement: () -> Unit,
    onTools: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { NovaHomeStage(snapshot, onNetwork) }
        item { NovaQuickRail(snapshot, devices.size, nearbyCells.size, placementReading, onTowers, onBands, onClients, onPlacement, onTools) }
        item { NovaSpeedStudio(performance, speedBusy, onSpeedTest) }
        item { NovaModeRail(snapshot, controlBusy, onMode) }
        item { NovaSignalStory(snapshot) }
        item { NovaTowerPeek(snapshot, nearbyCells.size, onTowers) }
        val message = operationMessage.ifBlank { status }
        if (message.isNotBlank()) item { NovaNotice(message) }
    }
}

@Composable
private fun NovaHomeStage(snapshot: RouterSnapshot, onOpen: () -> Unit) {
    val operator = nvClean(snapshot.raw["network_provider_fullname"])
        ?: nvClean(snapshot.raw["network_provider"])
        ?: snapshot.operatorCode
        ?: "الشبكة"
    val rsrp = snapshot.nrRsrp ?: snapshot.lteRsrp
    val sinr = snapshot.nrSinr ?: snapshot.lteSinr
    val score = nvSignalScore(rsrp)
    val transition = rememberInfiniteTransition(label = "stage-orbit")
    val orbit by transition.animateFloat(
        initialValue = .86f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "stage-orbit-scale"
    )

    Box(
        Modifier.fillMaxWidth().height(252.dp).shadow(16.dp, RoundedCornerShape(30.dp))
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(NvNight, NvNight2, NvNight3)))
            .clickable(onClick = onOpen)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = .035f), radius = size.minDimension * .34f, center = Offset(size.width * .13f, size.height * .12f))
            drawCircle(NvCyan.copy(alpha = .07f), radius = size.minDimension * .28f, center = Offset(size.width * .88f, size.height * .92f))
        }

        Column(Modifier.fillMaxSize().padding(17.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("مباشر من الراوتر", color = Color.White.copy(alpha = .62f), fontSize = 9.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(operator, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(snapshot.networkType ?: "النوع غير مكشوف", color = NvCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().graphicsLayer(scaleX = orbit, scaleY = orbit)) {
                        drawCircle(NvCyan.copy(alpha = .10f))
                        drawCircle(NvCyan.copy(alpha = .32f), style = Stroke(width = 2.5f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(nvGeneration(snapshot), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text(if (snapshot.caActive) "CA ON" else "LINK", color = NvCyan, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NovaDarkMetric("RSRP", "${rsrp.nvMetric()} dBm", nvQualityColor(score), Modifier.weight(1f))
                NovaDarkMetric("SINR", "${sinr.nvMetric()} dB", nvQualityColor(nvSinrScore(sinr)), Modifier.weight(1f))
                NovaDarkMetric("RSRQ", "${snapshot.lteRsrq.nvMetric()} dB", nvQualityColor(nvRsrqScore(snapshot.lteRsrq)), Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                nvBands(snapshot).forEach { band -> NovaDarkChip(band) }
                NovaDarkChip("PCI ${snapshot.pci ?: "—"}")
                NovaDarkChip("EARFCN ${snapshot.earfcn ?: "—"}")
                NovaDarkChip(nvEnbSector(snapshot))
            }
        }
    }
}

@Composable
private fun NovaDarkMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .075f)).padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(label, color = Color.White.copy(alpha = .55f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun NovaDarkChip(text: String) {
    Text(
        text, color = Color.White.copy(alpha = .88f), fontSize = 8.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .08f)).padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

@Composable
private fun NovaQuickRail(
    snapshot: RouterSnapshot,
    deviceCount: Int,
    cellCount: Int,
    placementReading: PlacementReading?,
    onTowers: () -> Unit,
    onBands: () -> Unit,
    onClients: () -> Unit,
    onPlacement: () -> Unit,
    onTools: () -> Unit
) {
    Column {
        Text("تحكم سريع", color = NvInk, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 3.dp))
        Spacer(Modifier.height(7.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
            item { NovaQuickCard("الخلايا", if (cellCount > 0) "$cellCount حولك" else "امسح الآن", "⌁", NvMint, onTowers) }
            item { NovaQuickCard("الترددات", nvBandSummary(snapshot), "≋", NvBlue, onBands) }
            item { NovaQuickCard("الأجهزة", "$deviceCount ظاهر", "◉", NvPurple, onClients) }
            item { NovaQuickCard("أفضل مكان", placementReading?.score?.total?.let { "$it/100" } ?: "ابدأ الرادار", "◎", NvAmber, onPlacement) }
            item { NovaQuickCard("المختبر", "تحسين وتشخيص", "✦", NvCyan, onTools) }
        }
    }
}

@Composable
private fun NovaQuickCard(title: String, subtitle: String, icon: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.widthIn(min = 142.dp, max = 165.dp).height(94.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp), color = NvPaper, shadowElevation = 5.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Text(icon, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.weight(1f))
                Text("↗", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(7.dp))
            Text(title, color = NvInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = NvMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NovaSpeedStudio(performance: NetworkPerformance?, busy: Boolean, onTest: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(27.dp), color = NvPaper, shadowElevation = 6.dp
    ) {
        BoxWithConstraints(Modifier.padding(16.dp)) {
            val compact = maxWidth < 355.dp
            if (compact) {
                Column {
                    NovaSectionHead("قياس السرعة", "اختبار فعلي عبر Cloudflare")
                    Spacer(Modifier.height(10.dp))
                    NovaSpeedRing(performance?.downloadMbps, busy, Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(10.dp))
                    NovaPerformanceRow(performance)
                    Spacer(Modifier.height(10.dp))
                    NovaPrimaryButton(if (busy) "جاري القياس…" else "ابدأ القياس", !busy, onTest)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        NovaSectionHead("قياس السرعة", "اختبار فعلي عبر Cloudflare")
                        Spacer(Modifier.height(12.dp))
                        NovaPerformanceRow(performance)
                        Spacer(Modifier.height(13.dp))
                        NovaPrimaryButton(if (busy) "جاري القياس…" else "ابدأ القياس", !busy, onTest)
                    }
                    Spacer(Modifier.width(10.dp))
                    NovaSpeedRing(performance?.downloadMbps, busy)
                }
            }
        }
    }
}

@Composable
private fun NovaSpeedRing(value: Double?, busy: Boolean, modifier: Modifier = Modifier) {
    val normalized = when {
        busy -> .72f
        value == null -> .08f
        else -> (value / 500.0).coerceIn(.05, 1.0).toFloat()
    }
    val progress by animateFloatAsState(normalized, tween(650), label = "speed-progress")
    Box(modifier.size(134.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 10.dp.toPx()
            drawArc(Color(0xFFE7EDF4), 135f, 270f, false, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(NvBlue, NvCyan, NvMint)), 135f, 270f * progress, false, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (busy) "…" else value?.let { "%.1f".format(it) } ?: "—", color = NvInk, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("Mb/s", color = NvMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun NovaPerformanceRow(performance: NetworkPerformance?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        NovaMiniStat("Ping", "${performance?.latencyMs.nvMetric()} ms", Modifier.weight(1f))
        NovaMiniStat("Jitter", "${performance?.jitterMs.nvMetric()} ms", Modifier.weight(1f))
        NovaMiniStat("Loss", "${performance?.packetLossPercent.nvMetric()}%", Modifier.weight(1f))
    }
}

@Composable
private fun NovaMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = NvMuted, fontSize = 8.sp)
        Text(value, color = NvInk, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun NovaModeRail(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    val current = nvClean(snapshot.raw["BearerPreference"]) ?: nvClean(snapshot.raw["current_network_mode"])
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), color = NvPaper, shadowElevation = 4.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NovaSectionHead("وضع الشبكة", "يتغير اللون فقط بعد قراءة الحالة")
                Spacer(Modifier.weight(1f))
                Text(if (current == null) "غير مكشوف" else "مقروء", color = if (current == null) NvAmber else NvMint, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(
                    listOf(
                        Triple("تلقائي", "WL_AND_5G", "A"),
                        Triple("5G فقط", "Only_5G", "5G"),
                        Triple("4G فقط", "Only_LTE", "4G"),
                        Triple("3G فقط", "Only_WCDMA", "3G")
                    )
                ) { (label, mode, glyph) ->
                    val selected = current.equals(mode, true)
                    val bg by animateColorAsState(if (selected) NvNight else Color(0xFFF1F4F8), label = "mode-bg")
                    Column(
                        Modifier.width(91.dp).clip(RoundedCornerShape(18.dp)).background(bg)
                            .clickable(enabled = !busy) { onMode(mode) }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(glyph, color = if (selected) NvCyan else NvInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text(label, color = if (selected) Color.White else NvInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaSignalStory(snapshot: RouterSnapshot) {
    val rsrp = snapshot.nrRsrp ?: snapshot.lteRsrp
    val score = nvSignalScore(rsrp)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), color = NvPaper, shadowElevation = 4.dp) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("نبض الإشارة", color = NvInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${rsrp.nvMetric()}", color = NvInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("dBm", color = NvMuted, fontSize = 9.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text("${nvQualityLabel(score)} • SINR ${(snapshot.nrSinr ?: snapshot.lteSinr).nvMetric()} dB", color = nvQualityColor(score), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            NovaSignalBars(score)
        }
    }
}

@Composable
private fun NovaSignalBars(score: Int) {
    val transition = rememberInfiniteTransition(label = "signal-wave")
    val wave by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "wave")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(7) { i ->
            val threshold = (i + 1) * 14
            val active = score >= threshold
            val height = (12 + i * 4 + if (active) (wave * 4).roundToInt() else 0).dp
            Box(Modifier.width(6.dp).height(height).clip(CircleShape).background(if (active) nvQualityColor(score) else Color(0xFFE4E9EF)))
        }
    }
}

@Composable
private fun NovaTowerPeek(snapshot: RouterSnapshot, nearbyCount: Int, onOpen: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(25.dp), color = NvPaper, shadowElevation = 4.dp
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE8F8F1)), contentAlignment = Alignment.Center) {
                Text("⌁", color = NvMint, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("الخلية الحالية", color = NvInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("${nvEnbSector(snapshot)} • PCI ${snapshot.pci ?: "—"}", color = NvMuted, fontSize = 9.sp)
            }
            if (nearbyCount > 0) Text("+$nearbyCount", color = NvMint, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(6.dp))
            Text("‹", color = NvInk, fontSize = 23.sp)
        }
    }
}

@Composable
private fun NovaNetwork(
    snapshot: RouterSnapshot,
    traffic: TrafficTelemetry?,
    thermal: ThermalTelemetry?,
    stability: ConnectionStabilityReport,
    busy: Boolean,
    onMode: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { NovaNetworkCanvas(snapshot) }
        item { NovaModeRail(snapshot, busy, onMode) }
        item {
            NovaPanel("هوية الاتصال", "ما أعاده الراوتر في هذه الجلسة") {
                NovaInfoRow("الموديل", snapshot.model ?: "—")
                NovaInfoRow("Cell ID", snapshot.cellId?.toString() ?: "—")
                NovaInfoRow("eNB / Sector", nvEnbSector(snapshot))
                NovaInfoRow("PCI / EARFCN", "${snapshot.pci ?: "—"} / ${snapshot.earfcn ?: "—"}")
                NovaInfoRow("المشغل", snapshot.operatorCode ?: "—")
            }
        }
        item {
            NovaPanel("الثبات", "قراءة من عينات متتابعة") {
                NovaInfoRow("الاستقرار", stability.summary)
                NovaInfoRow("WAN Telemetry", if (traffic != null) "متاح" else "غير مكشوف")
                NovaInfoRow("Thermal Telemetry", if (thermal != null) "متاح" else "غير مكشوف")
                NovaInfoRow("Carrier Aggregation", if (snapshot.caActive) "نشط" else "غير نشط / غير مكشوف")
            }
        }
    }
}

@Composable
private fun NovaNetworkCanvas(snapshot: RouterSnapshot) {
    val rsrp = snapshot.nrRsrp ?: snapshot.lteRsrp
    val sinr = snapshot.nrSinr ?: snapshot.lteSinr
    Box(
        Modifier.fillMaxWidth().height(214.dp).clip(RoundedCornerShape(29.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF101A2B), Color(0xFF0A3444))))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * .80f
            val cy = size.height * .48f
            repeat(3) { index ->
                drawCircle(NvCyan.copy(alpha = .10f - index * .02f), radius = 44f + index * 28f, center = Offset(cx, cy), style = Stroke(2f))
            }
            drawCircle(NvCyan, radius = 7f, center = Offset(cx, cy))
        }
        Column(Modifier.fillMaxSize().padding(17.dp)) {
            Text(snapshot.networkType ?: "الراديو", color = Color.White.copy(alpha = .58f), fontSize = 9.sp)
            Text(nvGeneration(snapshot), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaDarkMetric("RSRP", "${rsrp.nvMetric()} dBm", nvQualityColor(nvSignalScore(rsrp)), Modifier.weight(1f))
                NovaDarkMetric("SINR", "${sinr.nvMetric()} dB", nvQualityColor(nvSinrScore(sinr)), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NovaTowers(
    snapshot: RouterSnapshot,
    cells: List<NearbyCell>,
    scanBusy: Boolean,
    controlBusy: Boolean,
    target: TowerTarget?,
    guardEnabled: Boolean,
    guardStatus: TowerGuardStatus?,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onLockCell: (NearbyCell) -> Unit,
    onClear: () -> Unit,
    onGuard: (Boolean) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(29.dp))) {
                RealNetworkMap(Modifier.fillMaxSize())
            }
        }
        item {
            Surface(shape = RoundedCornerShape(25.dp), color = NvNight, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("الخلية الحالية", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("${nvEnbSector(snapshot)} • PCI ${snapshot.pci ?: "—"} • EARFCN ${snapshot.earfcn ?: "—"}", color = Color.White.copy(alpha = .58f), fontSize = 8.sp)
                        }
                        Text(if (target != null) "مُثبت" else "حر", color = if (target != null) NvMint else NvCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(11.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onScan, enabled = !scanBusy && !controlBusy, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = NvBlue)
                        ) { Text(if (scanBusy) "يمسح…" else "مسح الخلايا", fontSize = 10.sp, fontWeight = FontWeight.Black) }
                        OutlinedButton(
                            onClick = onLockCurrent, enabled = snapshot.pci != null && snapshot.earfcn != null && !controlBusy,
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)
                        ) { Text("تثبيت الحالية", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (target != null) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("PCI ${target.pci} • EARFCN ${target.earfcn}", color = NvMint, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Text(guardStatus?.message ?: "القفل فعّال بعد التحقق", color = Color.White.copy(alpha = .58f), fontSize = 8.sp)
                            }
                            Switch(checked = guardEnabled, onCheckedChange = onGuard, enabled = !controlBusy)
                        }
                        TextButton(onClick = onClear, enabled = !controlBusy) { Text("إزالة التثبيت", color = NvRed) }
                    }
                }
            }
        }
        if (cells.isEmpty()) {
            item { NovaEmpty("لا توجد خلايا بعد", "اضغط مسح الخلايا؛ إذا أخفاها الـFirmware فلن نخترع نتائج") }
        } else {
            item {
                Text("الخلايا المكتشفة", color = NvInk, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 2.dp))
            }
            items(cells) { cell -> NovaCellRow(cell, controlBusy, onLockCell) }
        }
    }
}

@Composable
private fun NovaCellRow(cell: NearbyCell, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val isNr = cell.rat.contains("NR", true) || cell.rat.contains("5G", true) || cell.band.orEmpty().startsWith("N", true)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = NvPaper, shadowElevation = 3.dp) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).clip(CircleShape).background(if (isNr) Color(0xFFF0ECFF) else Color(0xFFEAF3FF)), contentAlignment = Alignment.Center) {
                Text(if (isNr) "5G" else "4G", color = if (isNr) NvPurple else NvBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = NvInk, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text("ARFCN ${cell.arfcn ?: "—"} • RSRP ${cell.rsrp.nvMetric()} • SINR ${cell.sinr.nvMetric()}", color = NvMuted, fontSize = 8.sp)
            }
            Text(
                if (isNr) "قراءة" else "ثبّت",
                color = if (isNr) NvMuted else NvBlue, fontSize = 9.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (isNr) Color(0xFFF1F3F6) else Color(0xFFEAF3FF))
                    .clickable(enabled = !isNr && cell.pci != null && cell.arfcn != null && !busy) { onLock(cell) }
                    .padding(horizontal = 11.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NovaBands(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    busy: Boolean,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { NovaCarrierLane(snapshot) }
        item { NovaBandMatrix("4G LTE", capabilities.supportedLteBands.sorted(), selectedLte, false, busy, onLteToggle, onApplyLte) }
        item { NovaBandMatrix("5G NR", capabilities.supportedNrBands.sorted(), selectedNr, true, busy, onNrToggle, onApplyNr) }
    }
}

@Composable
private fun NovaCarrierLane(snapshot: RouterSnapshot) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = NvNight) {
        Column(Modifier.padding(15.dp)) {
            Text("المتصل الآن", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("حاملات قرأها الراوتر فعليًا", color = Color.White.copy(alpha = .55f), fontSize = 8.sp)
            Spacer(Modifier.height(10.dp))
            if (snapshot.cells.isEmpty()) {
                Text("لا توجد Carrier details موثقة", color = Color.White.copy(alpha = .65f), fontSize = 10.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(snapshot.cells) { cell ->
                        Column(
                            Modifier.width(138.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .08f)).padding(11.dp)
                        ) {
                            Text(cell.band ?: "—", color = NvCyan, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Text(cell.role.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("PCI ${cell.pci ?: "—"} • ARFCN ${cell.arfcn ?: "—"}", color = Color.White.copy(alpha = .55f), fontSize = 7.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NovaBandMatrix(
    title: String,
    bands: List<Int>,
    selected: Set<Int>,
    nr: Boolean,
    busy: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit
) {
    NovaPanel(title, "حدد ما تحتاجه فقط؛ read-back شرط النجاح") {
        if (bands.isEmpty()) {
            Text("هذا الـProfile لا يعلن ترددات قابلة للتثبيت", color = NvMuted, fontSize = 9.sp)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                bands.forEach { band ->
                    val checked = band in selected
                    val bg by animateColorAsState(
                        if (checked) (if (nr) NvPurple else NvBlue) else Color(0xFFF0F3F7),
                        label = "band-chip"
                    )
                    Text(
                        "${if (nr) "n" else "B"}$band",
                        color = if (checked) Color.White else NvInk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.clip(RoundedCornerShape(15.dp)).background(bg)
                            .clickable(enabled = !busy) { onToggle(band) }.padding(horizontal = 13.dp, vertical = 10.dp)
                    )
                }
            }
            Spacer(Modifier.height(13.dp))
            NovaPrimaryButton(
                if (busy) "جاري التحقق…" else if (selected.isEmpty()) "اختر ترددًا" else "طبّق ${selected.size} تردد",
                selected.isNotEmpty() && !busy,
                onApply,
                if (nr) NvPurple else NvBlue
            )
        }
    }
}

@Composable
private fun NovaClients(devices: List<ConnectedDevice>, aliases: MutableMap<String, String>) {
    var editing by remember { mutableStateOf<ConnectedDevice?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = NvNight) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("من على الشبكة؟", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("station_list + lan_station_list إذا كشفهما الراوتر", color = Color.White.copy(alpha = .55f), fontSize = 8.sp)
                    }
                    Text(devices.size.toString(), color = NvCyan, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        if (devices.isEmpty()) {
            item { NovaEmpty("لا توجد أجهزة ظاهرة", "قد لا يوجد عملاء الآن، أو الـFirmware لا يعيد القائمة في هذا الاستعلام") }
        } else {
            items(devices, key = { it.macAddress }) { device ->
                NovaDeviceRow(device, aliases[device.macAddress]) { editing = device }
            }
        }
    }

    editing?.let { device ->
        NovaDeviceDialog(
            device,
            aliases[device.macAddress] ?: device.displayName,
            { aliases[device.macAddress] = it },
            { editing = null }
        )
    }
}

@Composable
private fun NovaDeviceRow(device: ConnectedDevice, alias: String?, onEdit: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onEdit), shape = RoundedCornerShape(21.dp), color = NvPaper, shadowElevation = 3.dp) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(if (device.transport == DeviceTransport.WIFI) Color(0xFFEAF3FF) else Color(0xFFFFF4E4)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (device.transport == DeviceTransport.WIFI) "Wi‑Fi" else "LAN", color = NvInk, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(alias?.takeIf { it.isNotBlank() } ?: device.displayName, color = NvInk, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(device.ipAddress ?: "IP غير ظاهر", color = NvMuted, fontSize = 8.sp)
                Text(device.macAddress, color = NvMuted, fontSize = 7.sp)
            }
            Text("إدارة", color = NvBlue, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun NovaDeviceDialog(device: ConnectedDevice, initialAlias: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var alias by remember(device.macAddress) { mutableStateOf(initialAlias) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة الجهاز", color = NvInk, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${device.ipAddress ?: "—"} • ${device.macAddress}", color = NvMuted, fontSize = 8.sp)
                OutlinedTextField(value = alias, onValueChange = { alias = it.take(40) }, singleLine = true, label = { Text("اسم داخل التطبيق") })
                HorizontalDivider(color = NvLine)
                NovaUnavailable("تحديد السرعة", "لا يوجد QoS موثق لكل جهاز مع read-back")
                NovaUnavailable("حظر تطبيق", "لا يوجد API موثق لهذا الـFirmware")
                NovaUnavailable("حظر مؤقت", "يتطلب Block API موثوق + جدولة")
                NovaUnavailable("حظر الشبكة", "لن نرسل Blacklist خاصًا بموديل مختلف")
            }
        },
        confirmButton = { TextButton(onClick = { onSave(alias); onDismiss() }) { Text("حفظ", color = NvBlue, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = NvMuted) } },
        containerColor = NvPaper,
        shape = RoundedCornerShape(25.dp)
    )
}

@Composable
private fun NovaUnavailable(title: String, reason: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NvInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(reason, color = NvMuted, fontSize = 8.sp)
        }
        Text("مغلق", color = NvRed, fontSize = 8.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFECEE)).padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

@Composable
private fun NovaPlacement(
    active: Boolean,
    reading: PlacementReading?,
    soundEnabled: Boolean,
    onSound: (Boolean) -> Unit,
    onToggle: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { NovaRadar(reading, active) }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), color = NvPaper, shadowElevation = 4.dp) {
                Column(Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("الرنين الذكي", color = NvInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text("يتسارع كلما اقتربت من أفضل نقطة", color = NvMuted, fontSize = 8.sp)
                        }
                        Switch(checked = soundEnabled, onCheckedChange = onSound)
                    }
                    Spacer(Modifier.height(11.dp))
                    NovaPrimaryButton(if (active) "إيقاف الرادار" else "ابدأ الرادار", true, onToggle, if (active) NvRed else NvBlue)
                }
            }
        }
        item {
            NovaPanel("كيف نحسبها؟", "ليست قوة الإشارة وحدها") {
                NovaInfoRow("الإشارة", "LTE/NR RSRP")
                NovaInfoRow("النظافة", "SINR")
                NovaInfoRow("الجودة", "RSRQ")
                NovaInfoRow("الثبات", "عدة عينات + تغير الخلية")
                NovaInfoRow("الثقة", "ترتفع مع اكتمال القراءات")
            }
        }
    }
}

@Composable
private fun NovaRadar(reading: PlacementReading?, active: Boolean) {
    val score = reading?.score?.total ?: 0
    val transition = rememberInfiniteTransition(label = "radar")
    val pulse by transition.animateFloat(.82f, 1.1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "radar-pulse")
    val tint = nvQualityColor(score)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), color = NvNight, shadowElevation = 8.dp) {
        Column(Modifier.padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("رادار المكان", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(if (active) "حرّك الراوتر ببطء" else "ابدأ لالتقاط عينات حيّة", color = Color.White.copy(alpha = .55f), fontSize = 8.sp)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(205.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    repeat(4) { index ->
                        drawCircle(Color.White.copy(alpha = .08f), radius = size.minDimension * (.17f + index * .10f), style = Stroke(1.5f))
                    }
                    drawLine(Color.White.copy(alpha = .06f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1f)
                    drawLine(Color.White.copy(alpha = .06f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1f)
                }
                Box(Modifier.size(96.dp).graphicsLayer(scaleX = if (active) pulse else 1f, scaleY = if (active) pulse else 1f).clip(CircleShape).background(tint.copy(alpha = .16f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (score > 0) score.toString() else "—", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Text("/100", color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(nvPlacementText(reading?.guidance), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (reading != null) {
                Spacer(Modifier.height(5.dp))
                Text("أفضل نقطة ${reading.bestScore}/100 • الفرق ${reading.deltaFromBest}", color = Color.White.copy(alpha = .55f), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun NovaTools(
    runtime: RuntimeCapabilityReport?,
    sampleCount: Int,
    stability: ConnectionStabilityReport,
    capabilities: RouterCapabilities,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    backupAvailable: Boolean,
    busy: Boolean,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimize: () -> Unit,
    onRestore: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDisconnect: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(29.dp), color = NvNight) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("المحسّن", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Text("يجرب مسارات موثقة فقط ويحافظ على الرجوع", color = Color.White.copy(alpha = .55f), fontSize = 8.sp)
                        }
                        Switch(checked = smartMode, onCheckedChange = onSmartModeChange, enabled = !smartBusy && !busy)
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(OptimizationGoal.entries) { goal ->
                            val selected = goal == smartGoal
                            Text(
                                nvGoal(goal), color = if (selected) NvNight else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black,
                                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) NvCyan else Color.White.copy(alpha = .08f))
                                    .clickable(enabled = !smartBusy) { onSmartGoalChange(goal) }.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                    NovaPrimaryButton(if (smartBusy) "يختبر…" else "ابدأ تحسينًا موثقًا", !smartBusy && !busy, onOptimize, NvBlue)
                    smartReport?.message?.let { Spacer(Modifier.height(7.dp)); Text(it, color = Color.White.copy(alpha = .62f), fontSize = 8.sp) }
                }
            }
        }

        item {
            BoxWithConstraints {
                val narrow = maxWidth < 360.dp
                if (narrow) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        NovaToolTile("Runtime", if (runtime != null) "مكتمل" else "غير مكتمل", NvCyan)
                        NovaToolTile("عينات", sampleCount.toString(), NvPurple)
                        NovaToolTile("Cell Lock", nvSupport(capabilities.supportsCellLock), NvMint)
                        NovaToolTile("NR Lock", nvSupport(capabilities.supportsNrBandLock), NvAmber)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.weight(1f)) { NovaToolTile("Runtime", if (runtime != null) "مكتمل" else "غير مكتمل", NvCyan) }
                            Box(Modifier.weight(1f)) { NovaToolTile("عينات", sampleCount.toString(), NvPurple) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.weight(1f)) { NovaToolTile("Cell Lock", nvSupport(capabilities.supportsCellLock), NvMint) }
                            Box(Modifier.weight(1f)) { NovaToolTile("NR Lock", nvSupport(capabilities.supportsNrBandLock), NvAmber) }
                        }
                    }
                }
            }
        }

        item {
            NovaPanel("الأمان", "نسخة رجوع قبل التغييرات الحساسة") {
                NovaInfoRow("الثبات", stability.summary)
                NovaPrimaryButton("استعادة آخر نسخة أمان", backupAvailable && !busy, onRestore, NvInk)
            }
        }

        if (capabilities.supportsAntennaControl) {
            item {
                NovaPanel("الهوائي", "التحكم يظهر فقط عندما يعلن Runtime أنه قابل للمحاولة") {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (1..3).forEach { state ->
                            OutlinedButton(onClick = { onAntennaState(state) }, enabled = !busy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) {
                                Text("$state", fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        item {
            NovaPanel("التشخيص", "خذ تقريرًا بدل التخمين عند اختلاف Firmware") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("نسخ") }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("مشاركة") }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("قطع الاتصال بالراوتر", color = NvRed, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun NovaToolTile(title: String, value: String, accent: Color) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = NvPaper, shadowElevation = 3.dp) {
        Column(Modifier.padding(13.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.height(9.dp))
            Text(title, color = NvMuted, fontSize = 8.sp)
            Text(value, color = NvInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun NovaPanel(title: String, subtitle: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), color = NvPaper, shadowElevation = 4.dp) {
        Column(Modifier.padding(15.dp)) {
            NovaSectionHead(title, subtitle)
            Spacer(Modifier.height(11.dp))
            content()
        }
    }
}

@Composable
private fun NovaSectionHead(title: String, subtitle: String) {
    Column {
        Text(title, color = NvInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = NvMuted, fontSize = 8.sp)
    }
}

@Composable
private fun NovaInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = NvMuted, fontSize = 9.sp, modifier = Modifier.weight(.42f))
        Text(value, color = NvInk, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(.58f))
    }
    HorizontalDivider(color = NvLine)
}

@Composable
private fun NovaPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    color: Color = NvBlue
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black) }
}

@Composable
private fun NovaNotice(text: String) {
    Text(
        text, color = NvMuted, fontSize = 8.sp, lineHeight = 12.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFEAF0F6)).padding(11.dp)
    )
}

@Composable
private fun NovaEmpty(title: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().padding(14.dp).height(165.dp).clip(RoundedCornerShape(25.dp)).background(NvPaper), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
            Text("○", color = NvCyan, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(title, color = NvInk, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(subtitle, color = NvMuted, fontSize = 8.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
        }
    }
}

private fun nvClean(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() && it != "---" && !it.equals("null", true) }

private fun nvBands(snapshot: RouterSnapshot): List<String> {
    val fromCells = snapshot.cells.mapNotNull { it.band?.uppercase() }.distinct()
    if (fromCells.isNotEmpty()) return fromCells
    return listOfNotNull(snapshot.lteBand?.uppercase(), snapshot.nrBand?.uppercase()).distinct()
}

private fun nvBandSummary(snapshot: RouterSnapshot): String {
    val bands = nvBands(snapshot)
    return if (bands.isEmpty()) "غير مكشوف" else bands.take(3).joinToString(" + ") + if (snapshot.caActive) " • CA" else ""
}

private fun nvGeneration(snapshot: RouterSnapshot): String {
    val type = snapshot.networkType.orEmpty().uppercase()
    return when {
        "5G" in type || snapshot.nrRsrp != null -> "5G"
        "4G" in type || snapshot.lteRsrp != null -> "4G"
        "3G" in type -> "3G"
        else -> "—"
    }
}

private fun nvEnbSector(snapshot: RouterSnapshot): String {
    val id = snapshot.cellId ?: return "eNB غير متاح"
    return "eNB ${id ushr 8} / قطاع ${id and 0xFF}"
}

private fun nvSignalScore(rsrp: Double?): Int = when {
    rsrp == null -> 0
    rsrp >= -85 -> 95
    rsrp >= -95 -> 82
    rsrp >= -105 -> 64
    rsrp >= -115 -> 44
    else -> 22
}

private fun nvSinrScore(sinr: Double?): Int = when {
    sinr == null -> 0
    sinr >= 20 -> 95
    sinr >= 13 -> 82
    sinr >= 5 -> 65
    sinr >= 0 -> 48
    else -> 25
}

private fun nvRsrqScore(rsrq: Double?): Int = when {
    rsrq == null -> 0
    rsrq >= -10 -> 92
    rsrq >= -15 -> 74
    rsrq >= -20 -> 50
    else -> 25
}

private fun nvQualityColor(score: Int) = when {
    score >= 80 -> NvMint
    score >= 58 -> NvCyan
    score >= 40 -> NvAmber
    else -> NvRed
}

private fun nvQualityLabel(score: Int) = when {
    score >= 90 -> "ممتاز"
    score >= 75 -> "جيد جدًا"
    score >= 58 -> "جيد"
    score >= 40 -> "متوسط"
    score > 0 -> "ضعيف"
    else -> "غير مؤكد"
}

private fun nvPlacementText(value: Any?): String = value?.toString()?.takeIf { it.isNotBlank() } ?: "لا توجد إرشادات بعد"

private fun nvGoal(goal: OptimizationGoal) = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun nvSupport(value: Boolean) = if (value) "مدعوم" else "غير متاح"

private fun Double?.nvMetric(): String = this?.let {
    if (it == it.toLong().toDouble()) it.toLong().toString() else "%.1f".format(it)
} ?: "—"
