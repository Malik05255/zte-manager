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

private val IxBgTop = Color(0xFFF7FAFF)
private val IxBgBottom = Color(0xFFF1F5FB)
private val IxInk = Color(0xFF071B3B)
private val IxMuted = Color(0xFF71819B)
private val IxBlue = Color(0xFF1677FF)
private val IxAqua = Color(0xFF10C9B2)
private val IxPurple = Color(0xFF795CE6)
private val IxGreen = Color(0xFF1FBE82)
private val IxAmber = Color(0xFFF4AA32)
private val IxRed = Color(0xFFE85B66)
private val IxLine = Color(0xFFE3EAF4)
private val IxNavy = Color(0xFF071A34)
private val IxNavy2 = Color(0xFF0C315A)
private val IxNavy3 = Color(0xFF0B5A70)

private enum class IxScreen { HOME, NETWORK, TOWERS, BANDS, CLIENTS, PLACEMENT, TOOLS }

@Composable
fun ImmersiveDashboard(
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
        val history = remember { mutableStateListOf(IxScreen.HOME) }
        val screen = history.last()
        val aliases = remember { mutableStateMapOf<String, String>() }
        var placementSound by rememberSaveable { mutableStateOf(true) }

        fun navigate(target: IxScreen) {
            if (history.lastOrNull() != target) history += target
        }
        fun root(target: IxScreen) {
            history.clear()
            history += target
        }

        BackHandler(enabled = history.size > 1 || screen != IxScreen.HOME) {
            if (history.size > 1) history.removeAt(history.lastIndex) else root(IxScreen.HOME)
        }

        val wifi = remember(snapshot?.raw?.get("station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("station_list"), DeviceTransport.WIFI)
        }
        val lan = remember(snapshot?.raw?.get("lan_station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("lan_station_list"), DeviceTransport.LAN)
        }
        val devices = remember(wifi, lan) { ConnectedDeviceParser.merge(wifi, lan) }

        PlacementSoundEffect(placementSound, placementMode && screen == IxScreen.PLACEMENT, placementReading)

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(IxBgTop, IxBgBottom))
            )
        ) {
            Column(Modifier.fillMaxSize()) {
                IxHeader(
                    screen = screen,
                    connected = snapshot != null,
                    onBack = { if (history.size > 1) history.removeAt(history.lastIndex) else root(IxScreen.HOME) }
                )

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (snapshot == null) {
                        IxEmpty("جاري الاتصال بالراوتر", status.ifBlank { "بانتظار أول قراءة حقيقية من الراوتر" })
                    } else when (screen) {
                        IxScreen.HOME -> IxHome(
                            snapshot = snapshot,
                            status = status,
                            operationMessage = operationMessage,
                            performance = lastPerformance,
                            speedBusy = speedBusy,
                            controlBusy = controlBusy,
                            devices = devices,
                            nearbyCells = nearbyCells,
                            placementReading = placementReading,
                            onSpeedTest = onSpeedTest,
                            onMode = onSetNetworkMode,
                            onNetwork = { navigate(IxScreen.NETWORK) },
                            onTowers = { navigate(IxScreen.TOWERS) },
                            onBands = { navigate(IxScreen.BANDS) },
                            onClients = { navigate(IxScreen.CLIENTS) },
                            onPlacement = { navigate(IxScreen.PLACEMENT) },
                            onTools = { navigate(IxScreen.TOOLS) }
                        )
                        IxScreen.NETWORK -> IxNetwork(snapshot, traffic, thermal, stability, controlBusy, onSetNetworkMode)
                        IxScreen.TOWERS -> IxTowers(
                            snapshot, nearbyCells, scanBusy, controlBusy, towerTarget, towerGuardEnabled, towerGuardStatus,
                            onScanCells, onLockCurrentCell, onLockNearbyCell, onClearCellLock, onTowerGuardChange
                        )
                        IxScreen.BANDS -> IxBands(
                            snapshot, capabilities, selectedLte, selectedNr, controlBusy,
                            onLteToggle, onNrToggle, onApplyLte, onApplyNr
                        )
                        IxScreen.CLIENTS -> IxClients(devices, aliases)
                        IxScreen.PLACEMENT -> IxPlacement(
                            placementMode, placementReading, placementSound, { placementSound = it }, onPlacementToggle
                        )
                        IxScreen.TOOLS -> IxTools(
                            runtime, telemetrySamples.size, stability, capabilities, smartMode, smartGoal, smartBusy,
                            smartReport, safetyBackupAvailable, controlBusy, onSmartModeChange, onSmartGoalChange,
                            onOptimizeNow, onRestoreSafetyBackup, onAntennaState, onCopyDiagnostics,
                            onShareDiagnostics, onDisconnect
                        )
                    }
                }

                IxBottomDock(screen) { root(it) }
            }
        }
    }
}

@Composable
private fun IxHeader(screen: IxScreen, connected: Boolean, onBack: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "header-live")
    val pulseScale by pulse.animateFloat(
        initialValue = .78f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "header-live-scale"
    )

    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp).clickable(enabled = screen != IxScreen.HOME, onClick = onBack),
            shape = CircleShape,
            color = if (screen == IxScreen.HOME) Color.White else IxInk,
            shadowElevation = if (screen == IxScreen.HOME) 2.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    if (screen == IxScreen.HOME) "H" else "‹",
                    color = if (screen == IxScreen.HOME) IxBlue else Color.White,
                    fontSize = if (screen == IxScreen.HOME) 19.sp else 30.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(ixTitle(screen), color = IxInk, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(
                if (screen == IxScreen.HOME) "شبكتك بشكل حي — بدون تخمين" else "كل تغيير حساس يتم التحقق منه عبر read-back",
                color = IxMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            Modifier.clip(RoundedCornerShape(50))
                .background(if (connected) Color(0xFFE9F8F2) else Color(0xFFFFECEE))
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(9.dp).graphicsLayer {
                        scaleX = if (connected) pulseScale else 1f
                        scaleY = if (connected) pulseScale else 1f
                        alpha = if (connected) .25f else 0f
                    }.clip(CircleShape).background(IxGreen)
                )
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (connected) IxGreen else IxRed))
            }
            Spacer(Modifier.width(6.dp))
            Text(if (connected) "متصل" else "غير متصل", color = IxInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun ixTitle(screen: IxScreen) = when (screen) {
    IxScreen.HOME -> "HAI Network"
    IxScreen.NETWORK -> "الشبكة الآن"
    IxScreen.TOWERS -> "الأبراج والخريطة"
    IxScreen.BANDS -> "الترددات"
    IxScreen.CLIENTS -> "المتواجدون الآن"
    IxScreen.PLACEMENT -> "أفضل مكان للراوتر"
    IxScreen.TOOLS -> "الأدوات الذكية"
}

@Composable
private fun IxBottomDock(screen: IxScreen, onSelect: (IxScreen) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp).navigationBarsPadding()
            .shadow(12.dp, RoundedCornerShape(27.dp)),
        shape = RoundedCornerShape(27.dp),
        color = Color.White,
        tonalElevation = 0.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 7.dp)) {
            listOf(
                Triple(IxScreen.HOME, "الرئيسية", "⌂"),
                Triple(IxScreen.TOWERS, "الأبراج", "⌁"),
                Triple(IxScreen.BANDS, "الترددات", "≋"),
                Triple(IxScreen.CLIENTS, "الأجهزة", "◉"),
                Triple(IxScreen.TOOLS, "الأدوات", "✦")
            ).forEach { (target, label, glyph) ->
                val selected = screen == target
                val bg by animateColorAsState(
                    if (selected) Color(0xFFEAF3FF) else Color.Transparent,
                    animationSpec = tween(220),
                    label = "dock-bg"
                )
                val scale by animateFloatAsState(if (selected) 1f else .94f, tween(220), label = "dock-scale")
                Column(
                    Modifier.weight(1f).graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(19.dp)).background(bg).clickable { onSelect(target) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(glyph, color = if (selected) IxBlue else IxMuted, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(2.dp))
                    Text(label, color = if (selected) IxBlue else IxMuted, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun IxHome(
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 3.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        item { IxHero(snapshot, onNetwork) }
        item {
            Text("وصول سريع", color = IxInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 1.dp)) {
                item { IxActionCard("الأبراج", if (nearbyCells.isEmpty()) "افتح الخريطة والمسح" else "${nearbyCells.size} خلية مرصودة", "⌁", IxAqua, onTowers) }
                item { IxActionCard("أفضل مكان", placementReading?.score?.let { "آخر تقييم ${it.total}/100" } ?: "رادار حي مع رنين", "◎", IxPurple, onPlacement) }
                item { IxActionCard("المتواجدون", "${devices.size} جهاز ظاهر", "◉", IxBlue, onClients) }
                item { IxActionCard("الترددات", ixBandSummary(snapshot), "≋", IxAmber, onBands) }
                item { IxActionCard("الأدوات", "تحسين وتشخيص واستعادة", "✦", IxGreen, onTools) }
            }
        }
        item { IxSpeedCard(performance, speedBusy, onSpeedTest) }
        item { IxModeStrip(snapshot, controlBusy, onMode) }
        item { IxSignalInsight(snapshot) }
        item { IxTowerIdentity(snapshot, nearbyCells.size, onTowers) }
        item {
            val text = operationMessage.ifBlank { status }
            if (text.isNotBlank()) IxStatus(text)
        }
    }
}

@Composable
private fun IxHero(snapshot: RouterSnapshot, onOpen: () -> Unit) {
    val operator = ixClean(snapshot.raw["network_provider_fullname"])
        ?: ixClean(snapshot.raw["network_provider"])
        ?: snapshot.operatorCode
        ?: "الشبكة"
    val type = snapshot.networkType ?: "غير مؤكد"
    val bands = ixBands(snapshot)
    val rsrp = snapshot.nrRsrp ?: snapshot.lteRsrp
    val sinr = snapshot.nrSinr ?: snapshot.lteSinr
    val pulse = rememberInfiniteTransition(label = "hero-pulse")
    val orb by pulse.animateFloat(
        initialValue = .85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "hero-orb"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(32.dp)).clickable(onClick = onOpen),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(IxNavy, IxNavy2, IxNavy3))
            )
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha = .06f), radius = size.width * .32f * orb, center = Offset(size.width * .12f, size.height * .15f))
                drawCircle(IxAqua.copy(alpha = .12f), radius = size.width * .22f * orb, center = Offset(size.width * .91f, size.height * .10f))
                drawCircle(IxBlue.copy(alpha = .13f), radius = size.width * .28f, center = Offset(size.width * .83f, size.height * .96f))
            }
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IxLiveDot()
                            Spacer(Modifier.width(7.dp))
                            Text("مباشر من الراوتر", color = Color.White.copy(alpha = .82f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(operator, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(type, color = Color(0xFF8BEBDD), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            Modifier.size(88.dp).clip(RoundedCornerShape(27.dp)).background(
                                Brush.linearGradient(listOf(Color(0xFF1782FF), Color(0xFF12BED8)))
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(ixGeneration(snapshot), color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
                                Text(if (snapshot.caActive) "CA نشط" else "اتصال موثّق", color = Color.White.copy(alpha = .84f), fontSize = 9.sp)
                            }
                        }
                    }
                }

                if (bands.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        bands.forEachIndexed { index, band ->
                            val role = when {
                                band.startsWith("N", true) -> "5G"
                                index == 0 -> "أساسي"
                                else -> "مدمج"
                            }
                            Text(
                                "$band • $role",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .11f))
                                    .padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(15.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { IxHeroMetric("RSRP", rsrp.metric(), "dBm") }
                    Box(Modifier.weight(1f)) { IxHeroMetric("SINR", sinr.metric(), "dB") }
                    Box(Modifier.weight(1f)) { IxHeroMetric("RSRQ", snapshot.lteRsrq.metric(), "dB") }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "PCI ${snapshot.pci ?: "—"}   •   EARFCN ${snapshot.earfcn ?: "—"}   •   ${ixEnbSector(snapshot)}",
                    color = Color.White.copy(alpha = .66f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun IxLiveDot() {
    val transition = rememberInfiniteTransition(label = "live-dot")
    val scale by transition.animateFloat(
        initialValue = .75f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "live-dot-scale"
    )
    Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(10.dp).graphicsLayer { scaleX = scale; scaleY = scale; alpha = .22f }.clip(CircleShape).background(IxGreen))
        Box(Modifier.size(7.dp).clip(CircleShape).background(IxGreen))
    }
}

@Composable
private fun IxHeroMetric(label: String, value: String, unit: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .10f)).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color.White.copy(alpha = .62f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(unit, color = Color.White.copy(alpha = .55f), fontSize = 8.sp)
    }
}

@Composable
private fun IxActionCard(title: String, subtitle: String, glyph: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(158.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Text(glyph, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = IxInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = IxMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
            Spacer(Modifier.height(9.dp))
            Text("افتح ←", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun IxSpeedCard(performance: NetworkPerformance?, busy: Boolean, onTest: () -> Unit) {
    IxCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("قياس السرعة", color = IxInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("اختبار فعلي عبر Cloudflare", color = IxMuted, fontSize = 10.sp)
            }
            Text("↗", color = IxBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(8.dp))
        IxSpeedGauge(performance?.downloadMbps, busy)
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IxMiniMetric("Ping", "${performance?.latencyMs.metric()} ms")
            IxMiniMetric("Jitter", "${performance?.jitterMs.metric()} ms")
            IxMiniMetric("الفقد", "${performance?.packetLossPercent.metric()}%")
        }
        Spacer(Modifier.height(13.dp))
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(IxBlue, Color(0xFF0B9DEB))))
                .clickable(enabled = !busy, onClick = onTest),
            contentAlignment = Alignment.Center
        ) {
            Text(if (busy) "جاري القياس الفعلي…" else "ابدأ القياس", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun IxSpeedGauge(mbps: Double?, busy: Boolean) {
    val target = (mbps ?: 0.0).coerceIn(0.0, 500.0).toFloat()
    val animated by animateFloatAsState(target, tween(850), label = "speed-value")
    val fraction = (animated / 500f).coerceIn(.025f, 1f)
    val pulse = rememberInfiniteTransition(label = "speed-busy")
    val busyAlpha by pulse.animateFloat(
        initialValue = .25f,
        targetValue = .72f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "speed-busy-alpha"
    )

    Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(206.dp)) {
            val stroke = 16.dp.toPx()
            drawArc(Color(0xFFE5ECF6), 145f, 250f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                brush = Brush.sweepGradient(listOf(IxBlue, Color(0xFF13C8D0), IxGreen, IxBlue)),
                startAngle = 145f,
                sweepAngle = 250f * if (busy && mbps == null) .22f else fraction,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            if (busy) drawCircle(IxBlue.copy(alpha = busyAlpha * .10f), radius = size.minDimension * .36f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (busy && mbps == null) "…" else if (mbps == null) "—" else String.format("%.1f", animated), color = IxInk, fontSize = 39.sp, fontWeight = FontWeight.Black)
            Text("Mb/s", color = IxMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun IxMiniMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = IxMuted, fontSize = 9.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = IxInk, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun IxModeStrip(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    val current = ixClean(snapshot.raw["BearerPreference"]) ?: ixClean(snapshot.raw["current_network_mode"])
    IxCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("أوضاع الشبكة", color = IxInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("اختيار سريع مع تحقق بعد التطبيق", color = IxMuted, fontSize = 10.sp)
            }
            Text(ixModeLabel(current), color = IxBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                listOf(
                    Triple("تلقائي", "WL_AND_5G", "A"),
                    Triple("5G فقط", "Only_5G", "5G"),
                    Triple("4G فقط", "Only_LTE", "4G"),
                    Triple("3G فقط", "Only_WCDMA", "3G")
                )
            ) { (label, mode, glyph) ->
                val selected = current.equals(mode, true)
                val bg by animateColorAsState(if (selected) IxInk else Color(0xFFF3F6FB), tween(180), label = "mode-bg")
                Column(
                    Modifier.width(112.dp).clip(RoundedCornerShape(19.dp)).background(bg)
                        .clickable(enabled = !busy) { onMode(mode) }.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(glyph, color = if (selected) Color.White else IxBlue, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(3.dp))
                    Text(label, color = if (selected) Color.White else IxInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (busy) {
            Spacer(Modifier.height(9.dp))
            Text("جاري التحقق من الوضع الجديد…", color = IxBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IxSignalInsight(snapshot: RouterSnapshot) {
    val rsrp = snapshot.nrRsrp ?: snapshot.lteRsrp
    val sinr = snapshot.nrSinr ?: snapshot.lteSinr
    val score = ixSignalScore(rsrp)
    val animatedScore by animateFloatAsState(score.toFloat(), tween(600), label = "signal-score")
    IxCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("نبض الإشارة", color = IxInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("${rsrp.metric()} dBm", color = IxInk, fontSize = 31.sp, fontWeight = FontWeight.Black)
                Text("${ixQualityLabel(score)} • SINR ${sinr.metric()} dB", color = qualityColor(score), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                repeat(5) { index ->
                    val threshold = (index + 1) * 20
                    val active = animatedScore >= threshold
                    val h by animateFloatAsState(if (active) (19 + index * 8).toFloat() else 10f, tween(420 + index * 55), label = "bar-$index")
                    Box(
                        Modifier.width(9.dp).height(h.dp).clip(RoundedCornerShape(7.dp))
                            .background(if (active) qualityColor(score) else Color(0xFFE1E8F1))
                    )
                }
            }
        }
    }
}

@Composable
private fun IxTowerIdentity(snapshot: RouterSnapshot, nearbyCount: Int, onOpen: () -> Unit) {
    IxCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("البرج المتصل", color = IxInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("هوية خلوية حقيقية — لا نختلق إحداثيات", color = IxMuted, fontSize = 10.sp)
            }
            if (nearbyCount > 0) {
                Text("$nearbyCount خلية", color = IxAqua, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(50)).background(IxAqua.copy(alpha = .11f)).padding(horizontal = 9.dp, vertical = 6.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { IxDataPill("eNB", snapshot.cellId?.ushr(8)?.toString() ?: "—") }
            Box(Modifier.weight(1f)) { IxDataPill("القطاع", snapshot.cellId?.and(0xFF)?.toString() ?: "—") }
            Box(Modifier.weight(1f)) { IxDataPill("PCI", snapshot.pci?.toString() ?: "—") }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)) {
            Text("افتح الخريطة واختر خلية", color = IxBlue, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun IxNetwork(
    snapshot: RouterSnapshot,
    traffic: TrafficTelemetry?,
    thermal: ThermalTelemetry?,
    stability: ConnectionStabilityReport,
    busy: Boolean,
    onMode: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { IxHero(snapshot) {} }
        item { IxModeStrip(snapshot, busy, onMode) }
        item {
            IxCard {
                IxSectionTitle("هوية الاتصال", "قراءات الراوتر الحالية")
                Spacer(Modifier.height(8.dp))
                IxInfo("الموديل", snapshot.model ?: "—")
                IxInfo("Cell ID", snapshot.cellId?.toString() ?: "—")
                IxInfo("eNB / القطاع", ixEnbSector(snapshot))
                IxInfo("PCI / EARFCN", "${snapshot.pci ?: "—"} / ${snapshot.earfcn ?: "—"}")
                IxInfo("المشغل", snapshot.operatorCode ?: "—")
                IxInfo("دمج الترددات", if (snapshot.caActive) "نشط ومتحقق" else "غير نشط أو غير متحقق")
            }
        }
        item {
            IxCard {
                IxSectionTitle("الصحة اللحظية", "معلومات مساعدة غير تخمينية")
                Spacer(Modifier.height(8.dp))
                IxInfo("الثبات", stability.summary)
                IxInfo("حركة الإنترنت", if (traffic != null) "بيانات الراوتر متاحة" else "غير متاحة من هذا Firmware")
                IxInfo("حرارة المودم", if (thermal != null) "بيانات الراوتر متاحة" else "غير متاحة من هذا Firmware")
                IxInfo("RSRP", "${(snapshot.nrRsrp ?: snapshot.lteRsrp).metric()} dBm")
                IxInfo("SINR", "${(snapshot.nrSinr ?: snapshot.lteSinr).metric()} dB")
            }
        }
    }
}

@Composable
private fun IxTowers(
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
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 4.dp) {
                Column {
                    Box(Modifier.fillMaxWidth().height(285.dp)) { RealNetworkMap(Modifier.fillMaxSize()) }
                    Column(Modifier.padding(15.dp)) {
                        IxSectionTitle("الخريطة الحقيقية", "لا نضع برجًا في موقع غير موثق")
                        Spacer(Modifier.height(5.dp))
                        Text("المتصل الآن: ${ixEnbSector(snapshot)} • PCI ${snapshot.pci ?: "—"}", color = IxMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            IxCard {
                IxSectionTitle("اختيار الخلية", if (cells.isEmpty()) "المسح من الراوتر نفسه" else "تم رصد ${cells.size} خلية")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onScan, enabled = !scanBusy && !controlBusy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = IxBlue)) {
                        Text(if (scanBusy) "جاري المسح…" else "مسح الخلايا", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(onClick = onLockCurrent, enabled = snapshot.pci != null && snapshot.earfcn != null && !controlBusy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                        Text("تثبيت الحالية", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (target != null) {
                    Spacer(Modifier.height(13.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("PCI ${target.pci} • EARFCN ${target.earfcn}", color = IxInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(guardStatus?.message ?: "القفل الحالي موثّق", color = IxMuted, fontSize = 9.sp, maxLines = 2)
                        }
                        Switch(checked = guardEnabled, onCheckedChange = onGuard, enabled = !controlBusy)
                    }
                    TextButton(onClick = onClear, enabled = !controlBusy) { Text("إزالة التثبيت", color = IxRed, fontWeight = FontWeight.Bold) }
                }
            }
        }
        if (cells.isEmpty()) {
            item { IxEmpty("لا توجد خلايا بعد", "اضغط مسح الخلايا، وإذا أخفى Firmware النتائج سنعرض ذلك كما هو") }
        } else {
            items(cells) { cell -> IxCellCard(cell, controlBusy, onLockCell) }
        }
    }
}

@Composable
private fun IxCellCard(cell: NearbyCell, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val isNr = cell.rat.contains("NR", true) || cell.rat.contains("5G", true) || cell.band.orEmpty().startsWith("N", true)
    IxCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).background(if (isNr) IxPurple.copy(alpha = .12f) else IxBlue.copy(alpha = .11f)), contentAlignment = Alignment.Center) {
                Text(if (isNr) "5G" else "4G", color = if (isNr) IxPurple else IxBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = IxInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("ARFCN ${cell.arfcn ?: "—"} • RSRP ${cell.rsrp.metric()} dBm • SINR ${cell.sinr.metric()} dB", color = IxMuted, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onLock(cell) },
            enabled = !isNr && cell.pci != null && cell.arfcn != null && !busy,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isNr) IxMuted else IxInk)
        ) {
            Text(if (isNr) "تثبيت 5G غير متاح على هذا المسار" else "اختيار وتثبيت هذه الخلية", fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IxBands(
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
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IxCard {
                IxSectionTitle("المتصل فعليًا الآن", "الحاملات التي كشفها الراوتر")
                Spacer(Modifier.height(9.dp))
                if (snapshot.cells.isEmpty()) {
                    Text("لا توجد تفاصيل Carrier موثقة الآن", color = IxMuted, fontSize = 11.sp)
                } else snapshot.cells.forEach { cell ->
                    IxInfo(cell.role.toString(), "${cell.band ?: "—"} • PCI ${cell.pci ?: "—"} • ARFCN ${cell.arfcn ?: "—"}")
                }
            }
        }
        item { IxBandPicker("ترددات 4G LTE", capabilities.supportedLteBands.sorted(), selectedLte, busy, false, onLteToggle, onApplyLte) }
        item { IxBandPicker("ترددات 5G NR", capabilities.supportedNrBands.sorted(), selectedNr, busy, true, onNrToggle, onApplyNr) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IxBandPicker(
    title: String,
    bands: List<Int>,
    selected: Set<Int>,
    busy: Boolean,
    nr: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit
) {
    IxCard {
        IxSectionTitle(title, "اختر ثم طبّق؛ النجاح يحتاج read-back مطابق")
        Spacer(Modifier.height(12.dp))
        if (bands.isEmpty()) {
            Text("لا توجد ترددات معلنة لهذا Profile", color = IxMuted, fontSize = 11.sp)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bands.forEach { band ->
                    val checked = band in selected
                    Text(
                        "${if (nr) "n" else "B"}$band",
                        color = if (checked) Color.White else IxInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            .background(if (checked) (if (nr) IxPurple else IxBlue) else Color(0xFFF2F5F9))
                            .clickable(enabled = !busy) { onToggle(band) }.padding(horizontal = 13.dp, vertical = 10.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onApply, enabled = selected.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = if (nr) IxPurple else IxBlue)) {
                Text(if (busy) "جاري التحقق…" else "تطبيق الترددات المختارة", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun IxClients(devices: List<ConnectedDevice>, aliases: MutableMap<String, String>) {
    var editing by remember { mutableStateOf<ConnectedDevice?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            IxCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("المتواجدون الآن", color = IxInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("قراءة حقيقية من station_list و lan_station_list — لا نختلق جهازًا", color = IxMuted, fontSize = 10.sp)
                    }
                    Text(devices.size.toString(), color = IxBlue, fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        if (devices.isEmpty()) {
            item { IxEmpty("لا توجد أجهزة ظاهرة", "إما لا يوجد عملاء الآن، أو أن Firmware لا يكشف القائمة في هذه اللحظة") }
        } else {
            items(devices, key = { it.macAddress }) { device ->
                IxDeviceCard(device, aliases[device.macAddress]) { editing = device }
            }
        }
    }

    editing?.let { device ->
        IxDeviceDialog(
            device = device,
            initialAlias = aliases[device.macAddress] ?: device.displayName,
            onSave = { aliases[device.macAddress] = it },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun IxDeviceCard(device: ConnectedDevice, alias: String?, onOpen: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 2.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(18.dp)).background(if (device.transport == DeviceTransport.WIFI) IxBlue.copy(alpha = .10f) else IxAmber.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Text(if (device.transport == DeviceTransport.WIFI) "Wi‑Fi" else "LAN", color = IxInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(alias?.takeIf { it.isNotBlank() } ?: device.displayName, color = IxInk, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(device.ipAddress ?: "IP غير ظاهر", color = IxMuted, fontSize = 10.sp)
                Text(device.macAddress, color = IxMuted, fontSize = 9.sp)
            }
            Text("‹", color = IxBlue, fontSize = 27.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
private fun IxDeviceDialog(device: ConnectedDevice, initialAlias: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var alias by remember(device.macAddress) { mutableStateOf(initialAlias) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة الجهاز", color = IxInk, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("${device.ipAddress ?: "—"} • ${device.macAddress}", color = IxMuted, fontSize = 10.sp)
                OutlinedTextField(value = alias, onValueChange = { alias = it.take(40) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("اسم الجهاز داخل التطبيق") })
                IxDisabledControl("تحديد السرعة", "ينتظر API موثوق مع read-back")
                IxDisabledControl("حظر من الشبكة", "لن نستخدم أمرًا خاصًا بـFirmware مختلف")
                IxDisabledControl("حظر بعد مدة", "يحتاج أمر حظر موثوق ثم جدولة آمنة")
                Text("لا نعرض زرًا تنفيذيًا لوظيفة لا يستطيع التطبيق إثبات نتيجتها.", color = IxRed, fontSize = 9.sp, lineHeight = 14.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(alias); onDismiss() }) { Text("حفظ", color = IxBlue, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = IxMuted) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(26.dp)
    )
}

@Composable
private fun IxDisabledControl(title: String, reason: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F7FA)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = IxInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(reason, color = IxMuted, fontSize = 9.sp)
        }
        Text("غير متاح", color = IxRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun IxPlacement(active: Boolean, reading: PlacementReading?, soundEnabled: Boolean, onSound: (Boolean) -> Unit, onToggle: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IxCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IxSectionTitle("الرادار المكاني", "حرّك الراوتر ببطء واتبع التقييم الحي")
                    Spacer(Modifier.height(13.dp))
                    PlacementScore(reading)
                    Spacer(Modifier.height(9.dp))
                    Text(placementGuidanceText(reading?.guidance), color = IxInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 19.sp)
                    if (reading != null) {
                        Spacer(Modifier.height(6.dp))
                        Text("أفضل نقطة ${reading.bestScore}/100 • الفرق ${reading.deltaFromBest}", color = IxMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            IxCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("الرنين التفاعلي", color = IxInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("كلما تحسنت النقطة يتسارع الرنين", color = IxMuted, fontSize = 10.sp)
                    }
                    Switch(checked = soundEnabled, onCheckedChange = onSound)
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onToggle, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) IxRed else IxBlue)) {
                    Text(if (active) "إيقاف البحث" else "ابدأ البحث عن أفضل مكان", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IxTools(
    runtime: RuntimeCapabilityReport?,
    telemetryCount: Int,
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
    onOptimizeNow: () -> Unit,
    onRestore: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDisconnect: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IxCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("التحسين الذكي", color = IxInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text("يجرّب المسارات الموثقة ويحتفظ بمسار رجوع", color = IxMuted, fontSize = 10.sp)
                    }
                    Switch(checked = smartMode, onCheckedChange = onSmartModeChange, enabled = !smartBusy && !busy)
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptimizationGoal.entries.forEach { goal ->
                        val selected = goal == smartGoal
                        Text(
                            ixGoal(goal),
                            color = if (selected) Color.White else IxInk,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) IxInk else Color(0xFFF1F4F8))
                                .clickable(enabled = !smartBusy) { onSmartGoalChange(goal) }.padding(horizontal = 13.dp, vertical = 9.dp)
                        )
                    }
                }
                Spacer(Modifier.height(13.dp))
                Button(onClick = onOptimizeNow, enabled = !smartBusy && !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = IxBlue)) {
                    Text(if (smartBusy) "جاري اختبار أفضل إعداد…" else "حسّن الآن", fontWeight = FontWeight.Black)
                }
                smartReport?.message?.let { Spacer(Modifier.height(7.dp)); Text(it, color = IxMuted, fontSize = 10.sp) }
            }
        }
        item {
            IxCard {
                IxSectionTitle("الأمان والاستعادة", "كل تغيير حساس له مسار رجوع")
                Spacer(Modifier.height(10.dp))
                Button(onClick = onRestore, enabled = backupAvailable && !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = IxInk)) {
                    Text("استعادة آخر نسخة أمان", fontWeight = FontWeight.Black)
                }
            }
        }
        item {
            IxCard {
                IxSectionTitle("قدرات الراوتر", "Profile + Runtime probe")
                Spacer(Modifier.height(7.dp))
                IxInfo("LTE Band Lock", ixSupported(capabilities.supportsLteBandLock))
                IxInfo("5G Band Lock", ixSupported(capabilities.supportsNrBandLock))
                IxInfo("Cell Lock", ixSupported(capabilities.supportsCellLock))
                IxInfo("Carrier Aggregation", ixSupported(capabilities.supportsCarrierAggregationRead))
                IxInfo("أوامر كتابة آمنة", runtime?.safeWriteCount?.toString() ?: "لم يكتمل الفحص")
                IxInfo("عينات الثبات", telemetryCount.toString())
                IxInfo("الاستقرار", stability.summary)
                if (capabilities.supportsAntennaControl) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (1..3).forEach { state ->
                            OutlinedButton(onClick = { onAntennaState(state) }, enabled = !busy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) {
                                Text("هوائي $state", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            IxCard {
                IxSectionTitle("التشخيص", "شارك تقريرًا عند اختلاف Firmware أو فشل وظيفة")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text("نسخ التقرير") }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text("مشاركة") }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("قطع الاتصال بالراوتر", color = IxRed, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun IxCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun IxSectionTitle(title: String, subtitle: String) {
    Text(title, color = IxInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(3.dp))
    Text(subtitle, color = IxMuted, fontSize = 10.sp, lineHeight = 14.sp)
}

@Composable
private fun IxDataPill(label: String, value: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color(0xFFF4F7FB)).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = IxMuted, fontSize = 9.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = IxInk, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun IxInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = IxMuted, fontSize = 10.sp, modifier = Modifier.weight(.43f))
        Text(value, color = IxInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(.57f))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(IxLine.copy(alpha = .75f)))
}

@Composable
private fun IxStatus(message: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Color(0xFFEAF3FF)).padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(IxBlue))
        Spacer(Modifier.width(9.dp))
        Text(message, color = IxInk, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun IxEmpty(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFEAF3FF)), contentAlignment = Alignment.Center) {
            Text("⌁", color = IxBlue, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(11.dp))
        Text(title, color = IxInk, fontSize = 15.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(body, color = IxMuted, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 15.sp)
    }
}

private fun ixClean(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() && it != "---" && !it.equals("null", true) }

private fun ixBands(snapshot: RouterSnapshot): List<String> {
    val cells = snapshot.cells.mapNotNull { it.band?.uppercase() }.distinct()
    if (cells.isNotEmpty()) return cells
    return listOfNotNull(snapshot.lteBand?.uppercase(), snapshot.nrBand?.uppercase()).distinct()
}

private fun ixBandSummary(snapshot: RouterSnapshot): String {
    val bands = ixBands(snapshot)
    return if (bands.isEmpty()) "لا توجد ترددات موثقة" else bands.joinToString(" + ") + if (snapshot.caActive) " • مدمجة" else ""
}

private fun ixGeneration(snapshot: RouterSnapshot): String {
    val type = snapshot.networkType.orEmpty().uppercase()
    return when {
        "5G" in type || snapshot.nrRsrp != null -> "5G"
        "4G" in type || snapshot.lteRsrp != null -> "4G"
        else -> "—"
    }
}

private fun ixEnbSector(snapshot: RouterSnapshot): String {
    val id = snapshot.cellId ?: return "eNB غير متاح"
    return "eNB ${id ushr 8} / قطاع ${id and 0xFF}"
}

private fun ixSignalScore(rsrp: Double?): Int = when {
    rsrp == null -> 0
    rsrp >= -85 -> 95
    rsrp >= -95 -> 80
    rsrp >= -105 -> 62
    rsrp >= -115 -> 42
    else -> 22
}

private fun ixQualityLabel(score: Int) = when {
    score >= 90 -> "ممتاز"
    score >= 75 -> "جيد جدًا"
    score >= 58 -> "جيد"
    score >= 40 -> "متوسط"
    score > 0 -> "ضعيف"
    else -> "غير مؤكد"
}

private fun ixModeLabel(mode: String?): String = when (mode) {
    "WL_AND_5G" -> "تلقائي"
    "Only_5G" -> "5G فقط"
    "Only_LTE" -> "4G فقط"
    "Only_WCDMA" -> "3G فقط"
    null -> "غير مكشوف"
    else -> "وضع موثّق"
}

private fun ixGoal(goal: OptimizationGoal) = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun ixSupported(value: Boolean) = if (value) "مدعوم" else "غير متاح"
