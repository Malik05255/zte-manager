package com.malik.ztesmartmanager

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementGuidance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget
import kotlin.math.roundToInt

private val PulseBg = Color(0xFFF6F8FB)
private val PulsePaper = Color(0xFFFFFFFF)
private val PulseInk = Color(0xFF0B1B31)
private val PulseMuted = Color(0xFF6D7D92)
private val PulseLine = Color(0xFFE5EBF2)
private val PulseBlue = Color(0xFF176BFF)
private val PulseSky = Color(0xFF21B8F5)
private val PulseMint = Color(0xFF19BD83)
private val PulseAmber = Color(0xFFF3A83A)
private val PulseRed = Color(0xFFE45D68)
private val PulseNight = Color(0xFF071729)
private val PulseNight2 = Color(0xFF0C2942)

private enum class PulseScreen { HOME, MAP, BANDS, DEVICES, TOOLS }

@Composable
fun PulseDashboard(
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
        val history = remember { mutableStateListOf(PulseScreen.HOME) }
        val screen = history.last()
        val aliases = remember { mutableStateMapOf<String, String>() }
        var soundEnabled by rememberSaveable { mutableStateOf(true) }

        val wifi = remember(snapshot?.raw?.get("station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("station_list"), DeviceTransport.WIFI)
        }
        val lan = remember(snapshot?.raw?.get("lan_station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("lan_station_list"), DeviceTransport.LAN)
        }
        val devices = remember(wifi, lan) { ConnectedDeviceParser.merge(wifi, lan) }

        fun root(target: PulseScreen) {
            history.clear()
            history += target
        }

        BackHandler(enabled = screen != PulseScreen.HOME) { root(PulseScreen.HOME) }
        PlacementSoundEffect(soundEnabled, placementMode, placementReading)

        Column(Modifier.fillMaxSize().background(PulseBg)) {
            PulseTopBar(screen, snapshot != null) { root(PulseScreen.HOME) }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (snapshot == null) {
                    PulseEmpty(status.ifBlank { "جاري الاتصال بالراوتر" })
                } else when (screen) {
                    PulseScreen.HOME -> PulseHome(
                        snapshot = snapshot,
                        status = status,
                        operationMessage = operationMessage,
                        placementMode = placementMode,
                        placementReading = placementReading,
                        soundEnabled = soundEnabled,
                        onSoundChange = { soundEnabled = it },
                        performance = lastPerformance,
                        speedBusy = speedBusy,
                        controlBusy = controlBusy,
                        devices = devices,
                        nearbyCells = nearbyCells,
                        towerTarget = towerTarget,
                        onSpeedTest = onSpeedTest,
                        onPlacementToggle = onPlacementToggle,
                        onMode = onSetNetworkMode,
                        onOpenMap = { root(PulseScreen.MAP) },
                        onOpenBands = { root(PulseScreen.BANDS) },
                        onOpenDevices = { root(PulseScreen.DEVICES) },
                        onOpenTools = { root(PulseScreen.TOOLS) }
                    )
                    PulseScreen.MAP -> PulseMapScreen(
                        snapshot, nearbyCells, scanBusy, controlBusy, towerTarget, towerGuardEnabled,
                        towerGuardStatus, onScanCells, onLockCurrentCell, onLockNearbyCell,
                        onClearCellLock, onTowerGuardChange
                    )
                    PulseScreen.BANDS -> PulseBands(
                        capabilities, selectedLte, selectedNr, controlBusy,
                        onLteToggle, onNrToggle, onApplyLte, onApplyNr
                    )
                    PulseScreen.DEVICES -> PulseDevices(devices, aliases)
                    PulseScreen.TOOLS -> PulseTools(
                        runtime, telemetrySamples, stability, traffic, thermal, capabilities,
                        smartMode, smartGoal, smartBusy, smartReport, safetyBackupAvailable, controlBusy,
                        onSmartModeChange, onSmartGoalChange, onOptimizeNow, onAntennaState,
                        onRestoreSafetyBackup, onCopyDiagnostics, onShareDiagnostics, onDisconnect
                    )
                }
            }

            PulseBottomNav(screen) { root(it) }
        }
    }
}

@Composable
private fun PulseTopBar(screen: PulseScreen, connected: Boolean, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (screen != PulseScreen.HOME) {
            Surface(
                modifier = Modifier.size(36.dp).clickable(onClick = onBack),
                color = PulseNight,
                shape = CircleShape
            ) { Box(contentAlignment = Alignment.Center) { Text("‹", color = Color.White, fontSize = 25.sp) } }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(pulseScreenTitle(screen), color = PulseInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            if (screen == PulseScreen.HOME) {
                Text("كل المهم أمامك، والتفاصيل الفنية عند الحاجة فقط", color = PulseMuted, fontSize = 10.sp)
            }
        }
        Row(
            Modifier.clip(RoundedCornerShape(50))
                .background(if (connected) Color(0xFFE9F8F2) else Color(0xFFFFECEE))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (connected) PulseMint else PulseRed))
            Spacer(Modifier.width(6.dp))
            Text(if (connected) "متصل" else "غير متصل", color = PulseInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun pulseScreenTitle(screen: PulseScreen): String = when (screen) {
    PulseScreen.HOME -> "HAI"
    PulseScreen.MAP -> "الخريطة والبرج"
    PulseScreen.BANDS -> "الترددات"
    PulseScreen.DEVICES -> "الأجهزة المتصلة"
    PulseScreen.TOOLS -> "الإعدادات المتقدمة"
}

@Composable
private fun PulseBottomNav(screen: PulseScreen, onSelect: (PulseScreen) -> Unit) {
    Row(
        Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(PulseNight)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            PulseScreen.HOME to "الرئيسية",
            PulseScreen.MAP to "الخريطة",
            PulseScreen.DEVICES to "الأجهزة",
            PulseScreen.TOOLS to "المزيد"
        ).forEach { (target, label) ->
            val selected = screen == target
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(17.dp))
                    .background(if (selected) Color.White.copy(alpha = 0.13f) else Color.Transparent)
                    .clickable { onSelect(target) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (selected) Color.White else Color(0xFF92A4B9), fontSize = 11.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PulseHome(
    snapshot: RouterSnapshot,
    status: String,
    operationMessage: String,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    controlBusy: Boolean,
    devices: List<ConnectedDevice>,
    nearbyCells: List<NearbyCell>,
    towerTarget: TowerTarget?,
    onSpeedTest: () -> Unit,
    onPlacementToggle: () -> Unit,
    onMode: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenBands: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenTools: () -> Unit
) {
    val message = operationMessage.ifBlank { status.takeIf { it.isNotBlank() && it != "متصل" }.orEmpty() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PulseHero(snapshot) }
        item {
            PulsePlacementCard(
                placementMode, placementReading, soundEnabled, onSoundChange, onPlacementToggle
            )
        }
        item {
            PulseSectionHeader("أنت والبرج", "خريطة فعلية، وموقع البرج لا يُرسم إلا إذا كان موثقًا")
            Spacer(Modifier.height(8.dp))
            PulseNetworkMap(snapshot, Modifier.fillMaxWidth().height(330.dp))
            Spacer(Modifier.height(8.dp))
            PulseTextAction("افتح أدوات البرج والمسح", onOpenMap)
        }
        item { PulseSpeedCard(performance, speedBusy, onSpeedTest) }
        item { PulseModeCard(snapshot, controlBusy, onMode) }
        item { PulseTowerSummary(snapshot, nearbyCells.size, towerTarget != null, onOpenMap) }
        item { PulseSimpleLinkCard("الأجهزة المتصلة", pulseDeviceSummary(devices.size), "افتح الأجهزة", onOpenDevices) }
        item { PulseSimpleLinkCard("الترددات", pulseBandSummary(snapshot), "تخصيص الترددات", onOpenBands) }
        item { PulseSimpleLinkCard("المزيد", "التحسين الآمن، النسخة الاحتياطية والتشخيص", "افتح الإعدادات", onOpenTools) }
        if (message.isNotBlank()) item { PulseNotice(message) }
    }
}

@Composable
private fun PulseHero(snapshot: RouterSnapshot) {
    val quality = remember(snapshot) { NetworkQualityEngine().score(snapshot) }
    val active5g = snapshot.nrRsrp != null || snapshot.nrSinr != null || snapshot.networkType.orEmpty().contains("5G", true)
    val operator = snapshot.raw["network_provider_fullname"]?.takeIf { it.isNotBlank() }
        ?: snapshot.raw["network_provider"]?.takeIf { it.isNotBlank() }
        ?: snapshot.operatorCode
        ?: "شبكتك"
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color.Transparent
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(PulseNight, PulseNight2, Color(0xFF0A5262)),
                    start = Offset.Zero,
                    end = Offset(900f, 700f)
                )
            ).padding(18.dp)
        ) {
            Column {
                Text("حالة شبكتك الآن", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                Spacer(Modifier.height(5.dp))
                Text(simpleQualityLabel(quality.total), color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text(simpleQualityAdvice(quality.total), color = Color.White.copy(alpha = 0.84f), fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(18.dp))

                PulseSignalHalo(quality.total)
                Spacer(Modifier.height(18.dp))

                PulseHeroRow("الشبكة", operator)
                PulseHeroDivider()
                PulseHeroRow("الاتصال", if (active5g) "جيل خامس مع دعم من الجيل الرابع" else "جيل رابع")
                PulseHeroDivider()
                PulseHeroRow("دمج الشبكة", if (snapshot.caActive) "نشط ويجمع أكثر من مسار للسرعة" else "غير نشط الآن")
                PulseHeroDivider()
                PulseHeroRow("ثبات الإشارة", simpleStabilityLabel(quality.stability))

                Spacer(Modifier.height(13.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(horizontal = 13.dp, vertical = 11.dp)
                ) {
                    Text(
                        if (showAdvanced) "إخفاء التفاصيل الفنية" else "عرض التفاصيل الفنية للمختصين",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                AnimatedVisibility(showAdvanced) {
                    Column(Modifier.padding(top = 10.dp)) {
                        PulseHeroRow("قوة الإشارة", snapshot.nrRsrp?.let { "${it.roundToInt()} dBm" } ?: snapshot.lteRsrp?.let { "${it.roundToInt()} dBm" } ?: "غير متاحة")
                        PulseHeroRow("نظافة الإشارة", snapshot.nrSinr?.let { "${it.roundToInt()} dB" } ?: snapshot.lteSinr?.let { "${it.roundToInt()} dB" } ?: "غير متاحة")
                        PulseHeroRow("الخلية", listOfNotNull(snapshot.pci?.let { "PCI $it" }, snapshot.earfcn?.let { "EARFCN $it" }).joinToString(" • ").ifBlank { "غير متاحة" })
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseSignalHalo(score: Int) {
    val transition = rememberInfiniteTransition(label = "pulse-halo")
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse-halo-wave"
    )
    val accent = simpleQualityColor(score)
    Box(Modifier.fillMaxWidth().height(138.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(138.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(accent.copy(alpha = 0.10f * pulse), radius = 64.dp.toPx() * pulse, center = center)
            drawCircle(accent.copy(alpha = 0.18f), radius = 48.dp.toPx(), center = center, style = Stroke(1.6.dp.toPx()))
            drawCircle(accent.copy(alpha = 0.28f), radius = 33.dp.toPx(), center = center, style = Stroke(2.2.dp.toPx()))
            drawCircle(accent, radius = 17.dp.toPx(), center = center)
            repeat(4) { index ->
                val height = (18 + index * 7).dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.92f),
                    start = Offset(center.x - 19.dp.toPx() + index * 13.dp.toPx(), center.y + 18.dp.toPx()),
                    end = Offset(center.x - 19.dp.toPx() + index * 13.dp.toPx(), center.y + 18.dp.toPx() - height),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Text(simpleQualityShort(score), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 92.dp))
    }
}

@Composable
private fun PulseHeroRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, modifier = Modifier.weight(0.34f))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.66f), textAlign = TextAlign.End)
    }
}

@Composable
private fun PulseHeroDivider() {
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
}

@Composable
private fun PulsePlacementCard(
    active: Boolean,
    reading: PlacementReading?,
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onToggle: () -> Unit
) {
    val title = placementTitle(reading)
    val body = placementBody(reading)
    val color = placementColor(reading)
    PulseCard {
        Column {
            PulseSectionHeader("أفضل مكان للراوتر", "بدل الأرقام نقول لك ماذا تفعل مباشرة")
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(color.copy(alpha = 0.10f)).padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(8.dp))
                        Text(title, color = PulseInk, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(body, color = PulseMuted, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(12.dp))
                    PulsePlacementSteps(reading)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("صوت الإرشاد", color = PulseInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("يزداد الإيقاع كلما اقتربت من مكان أفضل", color = PulseMuted, fontSize = 10.sp)
                }
                Switch(checked = soundEnabled, onCheckedChange = onSoundChange)
            }
            Spacer(Modifier.height(10.dp))
            PulsePrimaryButton(if (active) "إيقاف البحث" else "ابدأ البحث عن أفضل مكان", enabled = true, onClick = onToggle)
        }
    }
}

@Composable
private fun PulsePlacementSteps(reading: PlacementReading?) {
    val level = when {
        reading == null -> 0
        reading.score.total >= 90 -> 5
        reading.score.total >= 82 -> 4
        reading.score.total >= 70 -> 3
        reading.score.total >= 55 -> 2
        else -> 1
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(5) { index ->
            Box(
                Modifier.weight(1f).height(7.dp).clip(CircleShape)
                    .background(if (index < level) placementColor(reading) else Color(0xFFE7ECF2))
            )
        }
    }
}

@Composable
private fun PulseSpeedCard(performance: NetworkPerformance?, busy: Boolean, onRun: () -> Unit) {
    PulseCard {
        Column {
            PulseSectionHeader("سرعة الإنترنت", "قياس فعلي عبر Cloudflare")
            Spacer(Modifier.height(14.dp))
            if (performance == null) {
                Text(if (busy) "جاري قياس السرعة…" else "لم نقس السرعة بعد", color = PulseInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("اضغط الزر وسنعرض النتيجة بكلام واضح.", color = PulseMuted, fontSize = 12.sp)
            } else {
                Text(speedWords(performance.downloadMbps), color = simpleSpeedColor(performance.downloadMbps), fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(
                    performance.downloadMbps?.let { "حوالي ${it.roundToInt()} ميجابت في الثانية" } ?: "تعذر قياس سرعة التنزيل",
                    color = PulseMuted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(7.dp))
                Text(latencyWords(performance.latencyMs), color = PulseInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            PulsePrimaryButton(if (busy) "جاري القياس…" else "قِس السرعة الآن", enabled = !busy, onClick = onRun)
        }
    }
}

@Composable
private fun PulseModeCard(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    PulseCard {
        Column {
            PulseSectionHeader("نوع الشبكة", pulseCurrentModeWords(snapshot))
            Spacer(Modifier.height(12.dp))
            PulseModeButton("تلقائي", "دع الراوتر يختار الأفضل", enabled = !busy) { onMode("AUTO") }
            Spacer(Modifier.height(8.dp))
            PulseModeButton("الجيل الخامس فقط", "استخدمه عندما تكون تغطية 5G مستقرة", enabled = !busy) { onMode("5G_ONLY") }
            Spacer(Modifier.height(8.dp))
            PulseModeButton("الجيل الرابع فقط", "مفيد إذا كان 5G يتغير كثيرًا", enabled = !busy) { onMode("4G_ONLY") }
        }
    }
}

@Composable
private fun PulseModeButton(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF2F6FA),
        border = BorderStroke(1.dp, PulseLine)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, color = PulseInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = PulseMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun PulseTowerSummary(snapshot: RouterSnapshot, nearbyCount: Int, locked: Boolean, onOpen: () -> Unit) {
    PulseCard {
        Column {
            PulseSectionHeader("البرج الحالي", if (locked) "تم تثبيت الخلية الحالية والتحقق منها" else "الراوتر يختار الخلية تلقائيًا")
            Spacer(Modifier.height(11.dp))
            Text(towerQualityWords(snapshot), color = PulseInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(
                if (nearbyCount > 0) "وجدنا $nearbyCount خلية قريبة يمكن مقارنتها." else "يمكنك تشغيل المسح لمعرفة الخلايا القريبة.",
                color = PulseMuted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            PulseTextAction("افتح الخريطة وأدوات البرج", onOpen)
        }
    }
}

@Composable
private fun PulseSimpleLinkCard(title: String, body: String, action: String, onClick: () -> Unit) {
    PulseCard {
        Column {
            Text(title, color = PulseInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(body, color = PulseMuted, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(10.dp))
            PulseTextAction(action, onClick)
        }
    }
}

@Composable
private fun PulseMapScreen(
    snapshot: RouterSnapshot,
    nearbyCells: List<NearbyCell>,
    scanBusy: Boolean,
    controlBusy: Boolean,
    towerTarget: TowerTarget?,
    guardEnabled: Boolean,
    guardStatus: TowerGuardStatus?,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onLockNearby: (NearbyCell) -> Unit,
    onClear: () -> Unit,
    onGuardChange: (Boolean) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PulseNetworkMap(snapshot, Modifier.fillMaxWidth().height(430.dp)) }
        item {
            PulseCard {
                Column {
                    PulseSectionHeader("البرج الذي تستخدمه", towerQualityWords(snapshot))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (towerTarget != null) "الخلية مثبتة حاليًا." else "الخلية تتغير تلقائيًا حسب اختيار الراوتر.",
                        color = PulseMuted,
                        fontSize = 12.sp
                    )
                    guardStatus?.message?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = PulseInk, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    PulsePrimaryButton(if (scanBusy) "جاري البحث…" else "ابحث عن الخلايا القريبة", !scanBusy, onScan)
                    Spacer(Modifier.height(8.dp))
                    if (towerTarget == null) {
                        OutlinedButton(onClick = onLockCurrent, enabled = !controlBusy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Text("ثبّت الخلية الحالية", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(onClick = onClear, enabled = !controlBusy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Text("إلغاء التثبيت", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("حماية التثبيت", color = PulseInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("يعيد التحقق من الخلية بدل الاعتماد على نجاح الأمر فقط", color = PulseMuted, fontSize = 10.sp)
                        }
                        Switch(checked = guardEnabled, onCheckedChange = onGuardChange, enabled = towerTarget != null && !controlBusy)
                    }
                }
            }
        }
        if (nearbyCells.isNotEmpty()) {
            item { PulseSectionHeader("الخلايا القريبة", "مرتبة من البيانات التي قرأها الراوتر") }
            items(nearbyCells) { cell ->
                PulseNearbyCell(cell, controlBusy) { onLockNearby(cell) }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun PulseNearbyCell(cell: NearbyCell, busy: Boolean, onSelect: () -> Unit) {
    PulseCard {
        Column {
            Text(nearbyCellWords(cell), color = PulseInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(nearbyCellQuality(cell), color = PulseMuted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            PulseTextAction(if (busy) "انتظر انتهاء العملية" else "ثبّت هذه الخلية", onSelect, enabled = !busy)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PulseBands(
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
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PulseCard {
                Column {
                    PulseSectionHeader("ترددات الجيل الرابع", "هذه شاشة متقدمة؛ لا تحتاجها غالبًا إذا كان الاتصال جيدًا")
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        capabilities.supportedLteBands.sorted().forEach { band ->
                            PulseBandChip("B$band", band in selectedLte) { onLteToggle(band) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    PulsePrimaryButton("طبّق ترددات 4G المختارة", selectedLte.isNotEmpty() && !busy, onApplyLte)
                }
            }
        }
        if (capabilities.supportsNrBandLock) {
            item {
                PulseCard {
                    Column {
                        PulseSectionHeader("ترددات الجيل الخامس", "لن نعلن نجاح التغيير قبل أن يقرأه الراوتر مرة أخرى")
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            capabilities.supportedNrBands.sorted().forEach { band ->
                                PulseBandChip("n$band", band in selectedNr) { onNrToggle(band) }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        PulsePrimaryButton("طبّق ترددات 5G المختارة", selectedNr.isNotEmpty() && !busy, onApplyNr)
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseBandChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PulseBlue else Color(0xFFF2F5F8),
        border = BorderStroke(1.dp, if (selected) PulseBlue else PulseLine)
    ) {
        Text(label, color = if (selected) Color.White else PulseInk, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

@Composable
private fun PulseDevices(devices: List<ConnectedDevice>, aliases: MutableMap<String, String>) {
    if (devices.isEmpty()) {
        PulseEmpty("الراوتر لم يعرض أجهزة متصلة الآن")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { PulseSectionHeader("المتواجدون الآن", "${devices.size} جهاز متصل حسب قراءة الراوتر") }
        items(devices) { device ->
            val key = device.mac.ifBlank { device.ip.orEmpty() }
            PulseCard {
                Column {
                    Text(aliases[key]?.takeIf { it.isNotBlank() } ?: device.name ?: "جهاز بدون اسم", color = PulseInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(device.ip ?: "عنوان الشبكة غير ظاهر", color = PulseMuted, fontSize = 11.sp)
                    Text(if (device.transport == DeviceTransport.WIFI) "متصل لاسلكيًا" else "متصل بسلك الشبكة", color = PulseMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PulseTools(
    runtime: RuntimeCapabilityReport?,
    samples: List<SafeTelemetrySample>,
    stability: ConnectionStabilityReport,
    traffic: TrafficTelemetry?,
    thermal: ThermalTelemetry?,
    capabilities: RouterCapabilities,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    backupAvailable: Boolean,
    controlBusy: Boolean,
    onSmartModeChange: (Boolean) -> Unit,
    onGoalChange: (OptimizationGoal) -> Unit,
    onOptimize: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onRestore: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDisconnect: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PulseCard {
                Column {
                    PulseSectionHeader("تحسين تلقائي", "يجرّب فقط ما يسمح به الراوتر ويتحقق بعد كل تغيير")
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (smartMode) "التحسين التلقائي مفعّل" else "التحسين التلقائي متوقف", color = PulseInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(checked = smartMode, onCheckedChange = onSmartModeChange)
                    }
                    Spacer(Modifier.height(8.dp))
                    PulsePrimaryButton(if (smartBusy) "جاري التحسين…" else "حسّن الشبكة الآن", !smartBusy, onOptimize)
                    smartReport?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it.message, color = PulseMuted, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
        item {
            PulseSimpleLinkCard("النسخة الآمنة", if (backupAvailable) "توجد نسخة يمكن الرجوع إليها إذا لم يعجبك أي تغيير" else "لا توجد نسخة محفوظة حاليًا", "استرجاع آخر إعداد آمن", onRestore)
        }
        if (capabilities.supportsAntennaControl) {
            item {
                PulseCard {
                    Column {
                        PulseSectionHeader("الهوائي", "تحكم متقدم متاح على هذا الراوتر")
                        Spacer(Modifier.height(10.dp))
                        PulseModeButton("تلقائي", "دع الراوتر يختار", !controlBusy) { onAntennaState(0) }
                        Spacer(Modifier.height(8.dp))
                        PulseModeButton("الهوائي الداخلي", "استخدم هوائي الراوتر الداخلي", !controlBusy) { onAntennaState(1) }
                    }
                }
            }
        }
        item {
            PulseCard {
                Column {
                    PulseSectionHeader("تقرير الدعم", "للمشاركة عند وجود مشكلة؛ لا نعرض الأرقام الفنية في الواجهة الرئيسية")
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onCopy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("نسخ تقرير التشخيص", fontSize = 12.sp) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("مشاركة تقرير التشخيص", fontSize = 12.sp) }
                    Spacer(Modifier.height(8.dp))
                    Text("تم جمع ${samples.size} قراءة محلية لهذه الجلسة.", color = PulseMuted, fontSize = 10.sp)
                    Text("حالة الاستقرار: ${stability.toString().take(80)}", color = PulseMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Runtime: ${if (runtime == null) "غير مكتشف" else "تم فحص القدرات"}", color = PulseMuted, fontSize = 10.sp)
                    if (traffic != null || thermal != null) Text("قراءات الحركة والحرارة متاحة في تقرير التشخيص.", color = PulseMuted, fontSize = 10.sp)
                }
            }
        }
        item {
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text("فصل الاتصال عن الراوتر", color = PulseRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun PulseCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = PulsePaper,
        border = BorderStroke(1.dp, PulseLine)
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun PulseSectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, color = PulseInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = PulseMuted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun PulsePrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PulseBlue)
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun PulseTextAction(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Text(
        text,
        color = if (enabled) PulseBlue else PulseMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(vertical = 3.dp)
    )
}

@Composable
private fun PulseNotice(message: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFEAF3FF), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = PulseInk, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(13.dp))
    }
}

@Composable
private fun PulseEmpty(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFEAF2FF)), contentAlignment = Alignment.Center) {
                Text("H", color = PulseBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(12.dp))
            Text(message, color = PulseInk, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

private fun simpleQualityLabel(score: Int): String = when {
    score >= 90 -> "ممتاز جدًا"
    score >= 82 -> "ممتاز"
    score >= 70 -> "جيد جدًا"
    score >= 55 -> "جيد"
    score >= 42 -> "مقبول"
    else -> "ضعيف"
}

private fun simpleQualityShort(score: Int): String = when {
    score >= 82 -> "ثبّت مكانه"
    score >= 70 -> "قريب من الأفضل"
    score >= 55 -> "يمكن تحسينه"
    else -> "جرّب مكانًا آخر"
}

private fun simpleQualityAdvice(score: Int): String = when {
    score >= 90 -> "الاتصال في أفضل حالاته تقريبًا. لا تحرّك الراوتر الآن."
    score >= 82 -> "المكان مناسب جدًا. ثبّت الراوتر هنا وراقب الاستقرار."
    score >= 70 -> "الشبكة جيدة جدًا، وبقي تحسين بسيط للوصول للأفضل."
    score >= 55 -> "الاتصال جيد، لكن تحريك الراوتر قليلًا قد يعطي نتيجة أفضل."
    score >= 42 -> "الاتصال يعمل، لكن المكان ليس مثاليًا. جرّب جهة أخرى أو قرب نافذة."
    else -> "الإشارة ضعيفة. غيّر مكان الراوتر واستخدم مساعد أفضل مكان."
}

private fun simpleQualityColor(score: Int): Color = when {
    score >= 82 -> PulseMint
    score >= 70 -> PulseSky
    score >= 55 -> PulseBlue
    score >= 42 -> PulseAmber
    else -> PulseRed
}

private fun simpleStabilityLabel(score: Int): String = when {
    score >= 85 -> "ثابت جدًا"
    score >= 70 -> "ثابت"
    score >= 50 -> "يتغير قليلًا"
    score > 0 -> "يتغير كثيرًا"
    else -> "نحتاج عدة قراءات للحكم"
}

private fun placementTitle(reading: PlacementReading?): String = when {
    reading == null -> "جاهز للبحث"
    reading.score.total >= 90 -> "ممتاز جدًا — ثبّت الراوتر هنا"
    reading.score.total >= 82 -> "ممتاز — هذا مكان قوي"
    reading.score.total >= 72 -> "جيد جدًا — بقيت خطوة بسيطة"
    reading.score.total >= 60 -> "جيد — جرّب تحريكًا بسيطًا"
    reading.score.total >= 45 -> "مقبول — نقدر نحسنه"
    else -> "ضعيف — غيّر المكان"
}

private fun placementBody(reading: PlacementReading?): String {
    if (reading == null) return "ابدأ المساعد ثم حرّك الراوتر ببطء. سنقارن كل نقطة بأفضل نقطة وجدناها."
    return when (reading.guidance) {
        PlacementGuidance.INITIAL -> "ابدأ التحريك ببطء في اتجاه واحد."
        PlacementGuidance.MUCH_BETTER -> "تحسّن واضح. استمر في نفس الاتجاه قليلًا."
        PlacementGuidance.BETTER -> "أنت تتحرك للاتجاه الصحيح. استمر قليلًا."
        PlacementGuidance.STABLE -> "المكان مستقر. جرّب خطوة صغيرة فقط إذا أردت نتيجة أفضل."
        PlacementGuidance.WORSE -> "النتيجة تراجعت. ارجع خطوة للخلف."
        PlacementGuidance.RETURN_TO_BEST -> "ارجع للمكان السابق؛ كان أفضل من موقعك الحالي."
        PlacementGuidance.CELL_CHANGED_WORSE -> "تغيرت الخلية وانخفضت الجودة. ارجع للمكان الأفضل السابق."
        PlacementGuidance.EXCELLENT_HOLD -> "وصلت لنتيجة ممتازة. لا تحرّك الراوتر."
        PlacementGuidance.BEST_SO_FAR -> "هذا أفضل مكان وجدناه حتى الآن. ثبّت الراوتر هنا أو جرّب خطوة صغيرة فقط."
    }
}

private fun placementColor(reading: PlacementReading?): Color = simpleQualityColor(reading?.score?.total ?: 0)

private fun speedWords(download: Double?): String = when {
    download == null -> "السرعة غير متاحة"
    download >= 300 -> "سرعة ممتازة جدًا"
    download >= 150 -> "سرعة ممتازة"
    download >= 70 -> "سرعة جيدة جدًا"
    download >= 30 -> "سرعة جيدة"
    download >= 10 -> "سرعة مقبولة"
    else -> "السرعة بطيئة"
}

private fun simpleSpeedColor(download: Double?): Color = when {
    download == null -> PulseMuted
    download >= 150 -> PulseMint
    download >= 70 -> PulseSky
    download >= 30 -> PulseBlue
    download >= 10 -> PulseAmber
    else -> PulseRed
}

private fun latencyWords(latency: Double?): String = when {
    latency == null -> "زمن الاستجابة لم يُقاس"
    latency <= 25 -> "الاستجابة ممتازة للألعاب والمكالمات"
    latency <= 50 -> "الاستجابة جيدة جدًا"
    latency <= 90 -> "الاستجابة جيدة"
    latency <= 150 -> "الاستجابة مقبولة"
    else -> "الاستجابة بطيئة"
}

private fun pulseCurrentModeWords(snapshot: RouterSnapshot): String {
    val type = snapshot.networkType.orEmpty()
    return when {
        snapshot.nrRsrp != null || snapshot.nrSinr != null || type.contains("5G", true) -> "أنت متصل بالجيل الخامس الآن"
        type.contains("4G", true) || snapshot.lteRsrp != null -> "أنت متصل بالجيل الرابع الآن"
        else -> "نقرأ نوع الشبكة من الراوتر"
    }
}

private fun towerQualityWords(snapshot: RouterSnapshot): String {
    val score = NetworkQualityEngine().score(snapshot).total
    return when {
        score >= 82 -> "اتصال قوي جدًا بالخلية الحالية"
        score >= 70 -> "اتصال جيد جدًا بالخلية الحالية"
        score >= 55 -> "اتصال جيد بالخلية الحالية"
        score >= 42 -> "اتصال مقبول بالخلية الحالية"
        else -> "اتصال ضعيف بالخلية الحالية"
    }
}

private fun nearbyCellWords(cell: NearbyCell): String = when {
    (cell.evidenceScore ?: 0) >= 80 -> "خلية قوية جدًا"
    (cell.evidenceScore ?: 0) >= 65 -> "خلية جيدة جدًا"
    (cell.evidenceScore ?: 0) >= 50 -> "خلية جيدة"
    else -> "خلية قريبة"
}

private fun nearbyCellQuality(cell: NearbyCell): String {
    val presence = cell.presencePercent
    return when {
        presence >= 80 -> "ظهرت باستمرار في المسح"
        presence >= 50 -> "ظهرت في أغلب قراءات المسح"
        else -> "ظهرت أحيانًا؛ تحتاج تحقق إضافي"
    }
}

private fun pulseDeviceSummary(count: Int): String = when (count) {
    0 -> "لا توجد أجهزة ظاهرة من الراوتر الآن"
    1 -> "جهاز واحد متصل الآن"
    2 -> "جهازان متصلان الآن"
    in 3..10 -> "$count أجهزة متصلة الآن"
    else -> "$count جهازًا متصلًا الآن"
}

private fun pulseBandSummary(snapshot: RouterSnapshot): String {
    val active5g = snapshot.nrBand?.takeIf { it.isNotBlank() }
    val lte = snapshot.lteBand?.takeIf { it.isNotBlank() }
    return when {
        active5g != null && lte != null -> "الراوتر يستخدم الجيل الخامس مع دعم من الجيل الرابع"
        active5g != null -> "يوجد تردد جيل خامس نشط"
        lte != null -> "يوجد تردد جيل رابع نشط"
        else -> "الترددات الحالية غير ظاهرة بشكل كافٍ"
    }
}
