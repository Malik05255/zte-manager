package com.malik.ztesmartmanager

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget
import kotlin.math.roundToInt

private val RefBg = Color(0xFFF5F8FC)
private val RefInk = Color(0xFF0C2145)
private val RefMuted = Color(0xFF7D8CA3)
private val RefBlue = Color(0xFF126DFA)
private val RefCyan = Color(0xFF1AB6D9)
private val RefGreen = Color(0xFF20B87B)
private val RefAmber = Color(0xFFF1A528)
private val RefPurple = Color(0xFF8467E8)
private val RefBorder = Color(0xFFE2E9F2)
private val RefHeroA = Color(0xFF0B2550)
private val RefHeroB = Color(0xFF123B66)
private val RefHeroC = Color(0xFF12566F)
private val RefSoftBlue = Color(0xFFEAF3FF)
private val RefSoftCyan = Color(0xFFE8F8FB)
private val RefSoftGreen = Color(0xFFE9F8F1)
private val RefSoftAmber = Color(0xFFFFF4E2)
private val RefSoftPurple = Color(0xFFF1EDFF)

private enum class RefScreen { HOME, NETWORK, TOWERS, BANDS, CLIENTS, PLACEMENT, TOOLS }

/**
 * Production dashboard rebuilt to follow the supplied phone reference closely while keeping all
 * telemetry truth-first. Values are always sourced from RouterSnapshot/verified operations; missing
 * data renders as — instead of a decorative sample number.
 */
@Composable
fun ReferenceExactDashboard(
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
        val history = remember { mutableStateListOf(RefScreen.HOME) }
        val screen = history.last()
        val aliases = remember { mutableStateMapOf<String, String>() }
        var placementSound by rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }

        fun navigate(target: RefScreen) {
            if (history.lastOrNull() != target) history += target
        }
        fun root(target: RefScreen) {
            history.clear()
            history += target
        }

        BackHandler(enabled = history.size > 1 || screen != RefScreen.HOME) {
            if (history.size > 1) history.removeAt(history.lastIndex) else root(RefScreen.HOME)
        }

        val wifi = remember(snapshot?.raw?.get("station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("station_list"), DeviceTransport.WIFI)
        }
        val lan = remember(snapshot?.raw?.get("lan_station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("lan_station_list"), DeviceTransport.LAN)
        }
        val devices = remember(wifi, lan) { ConnectedDeviceParser.merge(wifi, lan) }

        PlacementSoundEffect(placementSound, placementMode && screen == RefScreen.PLACEMENT, placementReading)

        Column(Modifier.fillMaxSize().background(RefBg)) {
            ReferenceHeader(
                screen = screen,
                connected = snapshot != null,
                onBack = { if (history.size > 1) history.removeAt(history.lastIndex) else root(RefScreen.HOME) }
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (snapshot == null) {
                    ReferenceEmpty(status)
                } else when (screen) {
                    RefScreen.HOME -> ReferenceHome(
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
                        onNetwork = { navigate(RefScreen.NETWORK) },
                        onTowers = { navigate(RefScreen.TOWERS) },
                        onBands = { navigate(RefScreen.BANDS) },
                        onClients = { navigate(RefScreen.CLIENTS) },
                        onPlacement = { navigate(RefScreen.PLACEMENT) },
                        onTools = { navigate(RefScreen.TOOLS) }
                    )
                    RefScreen.NETWORK -> ReferenceNetwork(snapshot, stability, status, operationMessage, controlBusy, onSetNetworkMode)
                    RefScreen.TOWERS -> ReferenceTowers(
                        snapshot, nearbyCells, scanBusy, controlBusy, towerGuardEnabled, towerGuardStatus,
                        onScanCells, onLockCurrentCell, onLockNearbyCell, onClearCellLock, onTowerGuardChange
                    )
                    RefScreen.BANDS -> ReferenceBands(
                        snapshot, capabilities, selectedLte, selectedNr, controlBusy,
                        onLteToggle, onNrToggle, onApplyLte, onApplyNr
                    )
                    RefScreen.CLIENTS -> ReferenceClients(devices, aliases)
                    RefScreen.PLACEMENT -> ReferencePlacement(
                        placementMode, placementReading, placementSound, { placementSound = it }, onPlacementToggle
                    )
                    RefScreen.TOOLS -> ReferenceTools(
                        runtime = runtime,
                        telemetryCount = telemetrySamples.size,
                        stability = stability,
                        smartMode = smartMode,
                        smartGoal = smartGoal,
                        smartBusy = smartBusy,
                        smartReport = smartReport,
                        safetyBackupAvailable = safetyBackupAvailable,
                        controlBusy = controlBusy,
                        onSmartModeChange = onSmartModeChange,
                        onSmartGoalChange = onSmartGoalChange,
                        onOptimizeNow = onOptimizeNow,
                        onRestoreSafetyBackup = onRestoreSafetyBackup,
                        onAntennaState = onAntennaState,
                        onCopyDiagnostics = onCopyDiagnostics,
                        onShareDiagnostics = onShareDiagnostics,
                        onDisconnect = onDisconnect
                    )
                }
            }

            ReferenceBottomBar(screen) { root(it) }
        }
    }
}

@Composable
private fun ReferenceHeader(screen: RefScreen, connected: Boolean, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(43.dp).clip(CircleShape).background(RefBlue)
                .clickable(enabled = screen != RefScreen.HOME, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text(if (screen == RefScreen.HOME) "H" else "‹", color = Color.White, fontSize = if (screen == RefScreen.HOME) 19.sp else 31.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(referenceTitle(screen), color = RefInk, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(
                if (screen == RefScreen.HOME) "كل الشبكة أمامك — بدون تخمين" else "تحكم موثّق مع read-back",
                color = RefMuted,
                fontSize = 10.sp
            )
        }
        Row(
            Modifier.clip(RoundedCornerShape(50))
                .background(if (connected) RefSoftGreen else Color(0xFFFFECEC))
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) RefGreen else Color(0xFFE35A5A)))
            Spacer(Modifier.width(6.dp))
            Text(if (connected) "متصل" else "غير متصل", color = RefInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun referenceTitle(screen: RefScreen): String = when (screen) {
    RefScreen.HOME -> "HAI Network"
    RefScreen.NETWORK -> "الشبكة الآن"
    RefScreen.TOWERS -> "الأبراج والخريطة"
    RefScreen.BANDS -> "الترددات"
    RefScreen.CLIENTS -> "المتواجدون الآن"
    RefScreen.PLACEMENT -> "أفضل مكان للراوتر"
    RefScreen.TOOLS -> "الأدوات الذكية"
}

@Composable
private fun ReferenceBottomBar(screen: RefScreen, onSelect: (RefScreen) -> Unit) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = Color.White,
        tonalElevation = 9.dp
    ) {
        listOf(
            Triple(RefScreen.HOME, "الرئيسية", "⌂"),
            Triple(RefScreen.TOWERS, "الأبراج", "⌁"),
            Triple(RefScreen.BANDS, "الترددات", "≋"),
            Triple(RefScreen.CLIENTS, "الأجهزة", "◉"),
            Triple(RefScreen.TOOLS, "الأدوات", "✦")
        ).forEach { (target, label, glyph) ->
            val selected = screen == target
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(target) },
                icon = { Text(glyph, fontSize = 19.sp, color = if (selected) RefBlue else RefMuted) },
                label = { Text(label, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = RefSoftBlue,
                    selectedIconColor = RefBlue,
                    selectedTextColor = RefBlue,
                    unselectedIconColor = RefMuted,
                    unselectedTextColor = RefMuted
                )
            )
        }
    }
}

@Composable
private fun ReferenceHome(
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
        contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 2.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { ReferenceNetworkHero(snapshot, onNetwork) }

        item {
            BoxWithConstraints {
                if (maxWidth < 365.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReferenceSpeedCard(performance, speedBusy, onSpeedTest)
                        ReferenceModeCard(snapshot, controlBusy, onMode)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { ReferenceSpeedCard(performance, speedBusy, onSpeedTest) }
                        Box(Modifier.weight(1f)) { ReferenceModeCard(snapshot, controlBusy, onMode) }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Box(Modifier.weight(1f)) {
                    ReferenceQuickTile("الأبراج", "${nearbyCells.size} خلية مقروءة", "⌁", RefSoftGreen, RefGreen, onTowers)
                }
                Box(Modifier.weight(1f)) {
                    val score = placementReading?.score?.total?.let { "$it/100" } ?: "رادار حي"
                    ReferenceQuickTile("أفضل مكان", score, "◎", RefSoftPurple, RefPurple, onPlacement)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Box(Modifier.weight(1f)) {
                    ReferenceQuickTile("المتواجدون الآن", "${devices.size} جهاز ظاهر", "◉", RefSoftBlue, RefBlue, onClients)
                }
                Box(Modifier.weight(1f)) {
                    ReferenceQuickTile("الترددات", referenceBandSummary(snapshot), "≋", RefSoftAmber, RefAmber, onBands)
                }
            }
        }

        item { ReferenceLiveNetworkCard(snapshot, onNetwork) }
        item { ReferenceSignalCard(snapshot) }
        item { ReferenceQuickTile("الأدوات الذكية", "تحسين موثّق • استعادة • تشخيص", "✦", RefSoftCyan, RefCyan, onTools) }
        item { ReferenceStatus(operationMessage.ifBlank { status }) }
    }
}

@Composable
private fun ReferenceNetworkHero(snapshot: RouterSnapshot, onClick: () -> Unit) {
    val operator = clean(snapshot.raw["network_provider_fullname"])
        ?: clean(snapshot.raw["network_provider"])
        ?: snapshot.operatorCode
        ?: "الشبكة"
    val technology = snapshot.networkType ?: "غير مؤكد"
    val bands = referenceBands(snapshot)
    val enb = snapshot.cellId?.let { it ushr 8 }
    val sector = snapshot.cellId?.let { it and 0xFFL }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(29.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(29.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(
                    colors = listOf(RefHeroA, RefHeroB, RefHeroC),
                    start = Offset.Zero,
                    end = Offset(1000f, 620f)
                )
            ).padding(18.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(operator, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(technology, color = Color(0xFFD8E8FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(RefGreen))
                            Spacer(Modifier.width(5.dp))
                            Text("اتصال مباشر", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        ReferenceSignalBars(signalScore(snapshot))
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (bands.isEmpty()) {
                        ReferenceDarkPill("التردد غير مؤكد")
                    } else {
                        bands.forEach { ReferenceDarkPill(it) }
                    }
                    if (snapshot.caActive) ReferenceAccentPill("CA مفعّل")
                }

                Spacer(Modifier.height(17.dp))
                HorizontalDivider(color = Color.White.copy(alpha = .16f))
                Spacer(Modifier.height(13.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReferenceHeroMetric("RSRP", snapshot.lteRsrp ?: snapshot.nrRsrp, "dBm", Modifier.weight(1f))
                    ReferenceHeroMetric("SINR", snapshot.lteSinr ?: snapshot.nrSinr, "dB", Modifier.weight(1f))
                    ReferenceHeroMetric("RSRQ", snapshot.lteRsrq, "dB", Modifier.weight(1f))
                }

                Spacer(Modifier.height(13.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReferenceHeroInfo("PCI", snapshot.pci?.toString() ?: "—", Modifier.weight(1f))
                    ReferenceHeroInfo("EARFCN", snapshot.earfcn?.toString() ?: "—", Modifier.weight(1f))
                    ReferenceHeroInfo("eNB", enb?.toString() ?: "—", Modifier.weight(1f))
                    ReferenceHeroInfo("قطاع", sector?.toString() ?: "—", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReferenceSignalBars(score: Int) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        (1..5).forEach { index ->
            Box(
                Modifier.width(4.dp).height((6 + index * 3).dp).clip(RoundedCornerShape(2.dp))
                    .background(if (index <= ((score + 19) / 20).coerceIn(0, 5)) Color.White else Color.White.copy(alpha = .24f))
            )
        }
    }
}

@Composable
private fun ReferenceDarkPill(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .11f)).padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

@Composable
private fun ReferenceAccentPill(text: String) {
    Text(
        text,
        color = Color(0xFFD9FFF3),
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(RefGreen.copy(alpha = .22f)).padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

@Composable
private fun ReferenceHeroMetric(label: String, value: Double?, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .09f)).padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFFBCD0E8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(value?.let { String.format("%.1f", it) } ?: "—", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(unit, color = Color(0xFFBCD0E8), fontSize = 8.sp)
    }
}

@Composable
private fun ReferenceHeroInfo(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFFAFC4DE), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReferenceSpeedCard(performance: NetworkPerformance?, busy: Boolean, onSpeedTest: () -> Unit) {
    ReferenceCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("قياس السرعة", color = RefInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text("اختبار فعلي", color = RefMuted, fontSize = 9.sp)
            }
            Box(Modifier.size(9.dp).clip(CircleShape).background(if (busy) RefAmber else RefGreen))
        }
        Spacer(Modifier.height(7.dp))
        ReferenceSpeedGauge(performance?.downloadMbps, busy, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ReferenceMiniMetric("Ping", performance?.latencyMs, "ms")
            ReferenceMiniMetric("Jitter", performance?.jitterMs, "ms")
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onSpeedTest,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RefBlue)
        ) {
            Text(if (busy) "جاري القياس…" else "قياس السرعة", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ReferenceSpeedGauge(mbps: Double?, running: Boolean, modifier: Modifier = Modifier) {
    val target = (mbps ?: 0.0).coerceIn(0.0, 1000.0).toFloat()
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(800), label = "reference-speed")
    val fraction = if (running && mbps == null) .24f else (animated / 500f).coerceIn(.025f, 1f)
    Box(modifier.height(128.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(130.dp)) {
            val stroke = 13.dp.toPx()
            drawArc(Color(0xFFE6EDF5), 145f, 250f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                brush = Brush.sweepGradient(listOf(RefBlue, RefCyan, RefGreen, RefBlue)),
                startAngle = 145f,
                sweepAngle = 250f * fraction,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (running) "…" else mbps?.let { String.format("%.1f", animated) } ?: "—",
                color = RefInk,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black
            )
            Text("Mb/s", color = RefMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ReferenceMiniMetric(label: String, value: Double?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = RefMuted, fontSize = 8.sp)
        Text(value?.let { String.format("%.0f %s", it, unit) } ?: "—", color = RefInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReferenceModeCard(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    val rawMode = clean(snapshot.raw["BearerPreference"])
    ReferenceCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("وضع الشبكة", color = RefInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text("تطبيق مع تحقق read-back", color = RefMuted, fontSize = 9.sp)
            }
            Box(Modifier.size(35.dp).clip(CircleShape).background(RefSoftBlue), contentAlignment = Alignment.Center) {
                Text("5G", color = RefBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("الوضع الحالي", color = RefMuted, fontSize = 8.sp)
        Text(referenceModeName(rawMode), color = RefInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))

        ReferenceModeButton("5G فقط", rawMode == "Only_5G", !busy) { onMode("Only_5G") }
        Spacer(Modifier.height(7.dp))
        ReferenceModeButton("4G فقط", rawMode == "Only_LTE", !busy) { onMode("Only_LTE") }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.weight(1f)) { ReferenceModeButton("تلقائي", rawMode == "WL_AND_5G", !busy) { onMode("WL_AND_5G") } }
            Box(Modifier.weight(1f)) { ReferenceModeButton("3G فقط", rawMode == "Only_WCDMA", !busy) { onMode("Only_WCDMA") } }
        }
    }
}

@Composable
private fun ReferenceModeButton(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .background(if (selected) RefBlue else Color(0xFFF2F6FB))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else RefInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ReferenceQuickTile(title: String, subtitle: String, glyph: String, bg: Color, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(22.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, RefBorder)
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp)).background(bg), contentAlignment = Alignment.Center) {
                Text(glyph, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = RefInk, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = RefMuted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("‹", color = RefMuted, fontSize = 20.sp)
        }
    }
}

@Composable
private fun ReferenceLiveNetworkCard(snapshot: RouterSnapshot, onClick: () -> Unit) {
    ReferenceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("الشبكة الآن", color = RefInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text("البيانات المقروءة من الراوتر مباشرة", color = RefMuted, fontSize = 9.sp)
            }
            Text(referenceBandSummary(snapshot), color = RefBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReferenceSmallStat("النوع", snapshot.networkType ?: "—", RefSoftBlue, RefBlue, Modifier.weight(1f))
            ReferenceSmallStat("التجميع", if (snapshot.caActive) "مفعّل" else "غير مؤكد", RefSoftGreen, RefGreen, Modifier.weight(1f))
            ReferenceSmallStat("الخلايا", snapshot.cells.size.toString(), RefSoftPurple, RefPurple, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReferenceSmallStat(label: String, value: String, bg: Color, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(15.dp)).background(bg).padding(horizontal = 8.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = RefMuted, fontSize = 8.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReferenceSignalCard(snapshot: RouterSnapshot) {
    val score = signalScore(snapshot)
    ReferenceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("جودة الإشارة", color = RefInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(referenceSignalLabel(score), color = if (score >= 65) RefGreen else if (score >= 40) RefAmber else Color(0xFFE05D5D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(56.dp).clip(CircleShape).background(RefSoftBlue), contentAlignment = Alignment.Center) {
                Text("$score", color = RefBlue, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFFE8EEF5))) {
            Box(Modifier.fillMaxWidth(score / 100f).height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(RefAmber, RefCyan, RefGreen))))
        }
    }
}

@Composable
private fun ReferenceNetwork(
    snapshot: RouterSnapshot,
    stability: ConnectionStabilityReport,
    status: String,
    operationMessage: String,
    busy: Boolean,
    onMode: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ReferenceNetworkHero(snapshot) {} }
        item { ReferenceModeCard(snapshot, busy, onMode) }
        item {
            ReferenceCard {
                Text("استقرار الاتصال", color = RefInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text(stability.summary, color = RefMuted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        item { ReferenceStatus(operationMessage.ifBlank { status }) }
    }
}

@Composable
private fun ReferenceTowers(
    snapshot: RouterSnapshot,
    nearbyCells: List<NearbyCell>,
    scanBusy: Boolean,
    controlBusy: Boolean,
    guardEnabled: Boolean,
    guardStatus: TowerGuardStatus?,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onLockCell: (NearbyCell) -> Unit,
    onClear: () -> Unit,
    onGuard: (Boolean) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, RefBorder)) {
                RealNetworkMap(modifier = Modifier.fillMaxWidth().height(330.dp))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = onScan,
                    enabled = !scanBusy && !controlBusy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RefBlue)
                ) { Text(if (scanBusy) "جاري المسح…" else "مسح الخلايا", fontSize = 10.sp, fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onLockCurrent,
                    enabled = snapshot.pci != null && snapshot.earfcn != null && !controlBusy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp)
                ) { Text("تثبيت الحالية", fontSize = 10.sp, fontWeight = FontWeight.Black) }
            }
        }
        item {
            ReferenceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("حارس البرج", color = RefInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(guardStatus?.message ?: "يعمل فقط بعد قفل موثّق", color = RefMuted, fontSize = 9.sp, maxLines = 2)
                    }
                    Switch(checked = guardEnabled, onCheckedChange = onGuard, enabled = !controlBusy)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onClear, enabled = !controlBusy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("إزالة التثبيت", color = RefInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        if (nearbyCells.isEmpty()) {
            item { ReferenceEmpty("لا توجد خلايا إضافية حتى الآن • اضغط مسح الخلايا") }
        } else {
            items(nearbyCells) { cell ->
                ReferenceCard(onClick = { if (cell.rat.equals("LTE", true) && !controlBusy) onLockCell(cell) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (cell.rat.equals("LTE", true)) RefSoftGreen else RefSoftBlue), contentAlignment = Alignment.Center) {
                            Text(cell.rat, color = if (cell.rat.equals("LTE", true)) RefGreen else RefBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${cell.band ?: "Band —"} • PCI ${cell.pci ?: "—"}", color = RefInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("ARFCN ${cell.arfcn ?: "—"} • RSRP ${cell.rsrp?.let { String.format("%.0f", it) } ?: "—"}", color = RefMuted, fontSize = 9.sp)
                        }
                        Text(if (cell.rat.equals("LTE", true)) "اختيار" else "غير متاح", color = if (cell.rat.equals("LTE", true)) RefBlue else RefMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { ReferenceStatus("لا نختلق إحداثيات برج. علامة البرج تظهر فقط عند توفر مصدر إحداثيات موثوق.") }
    }
}

@Composable
private fun ReferenceBands(
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
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ReferenceLiveNetworkCard(snapshot) {} }
        item {
            ReferenceCard {
                Text("ترددات 4G", color = RefInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text("اختيار ثم تطبيق مع read-back موثّق", color = RefMuted, fontSize = 9.sp)
                Spacer(Modifier.height(10.dp))
                ReferenceBandChips(capabilities.supportedLteBands, selectedLte, onLteToggle)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onApplyLte, enabled = selectedLte.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = RefBlue)) {
                    Text("تطبيق ترددات 4G", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        item {
            ReferenceCard {
                Text("ترددات 5G", color = RefInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text("لا يُعتبر التغيير ناجحًا قبل تطابق read-back", color = RefMuted, fontSize = 9.sp)
                Spacer(Modifier.height(10.dp))
                ReferenceBandChips(capabilities.supportedNrBands, selectedNr, onNrToggle, prefix = "n")
                Spacer(Modifier.height(10.dp))
                Button(onClick = onApplyNr, enabled = selectedNr.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = RefBlue)) {
                    Text("تطبيق ترددات 5G", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ReferenceBandChips(supported: Set<Int>, selected: Set<Int>, onToggle: (Int) -> Unit, prefix: String = "B") {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        supported.sorted().chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { band ->
                    val active = band in selected
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (active) RefBlue else Color(0xFFF2F6FB))
                            .clickable { onToggle(band) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$prefix$band", color = if (active) Color.White else RefInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                repeat((4 - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ReferenceClients(devices: List<ConnectedDevice>, aliases: MutableMap<String, String>) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ReferenceCard {
                Text("${devices.size} جهاز ظاهر للراوتر", color = RefInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text("لا نختلق جهازًا إذا لم يرجعه Firmware", color = RefMuted, fontSize = 9.sp)
            }
        }
        if (devices.isEmpty()) {
            item { ReferenceEmpty("لم يرجع الراوتر قائمة أجهزة قابلة للتحقق حاليًا") }
        } else {
            items(devices) { device ->
                ReferenceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(if (device.transport == DeviceTransport.WIFI) RefSoftBlue else RefSoftGreen), contentAlignment = Alignment.Center) {
                            Text(if (device.transport == DeviceTransport.WIFI) "Wi‑Fi" else "LAN", color = if (device.transport == DeviceTransport.WIFI) RefBlue else RefGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(aliases[device.macAddress] ?: device.displayName, color = RefInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text(device.ipAddress ?: "IP غير ظاهر", color = RefMuted, fontSize = 9.sp)
                            Text(device.macAddress, color = RefMuted, fontSize = 8.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("حظر/QoS غير مفعّل حتى يثبت Firmware مسار تحكم مع read-back موثوق.", color = RefMuted, fontSize = 8.sp, lineHeight = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ReferencePlacement(
    placementMode: Boolean,
    reading: PlacementReading?,
    soundEnabled: Boolean,
    onSound: (Boolean) -> Unit,
    onToggle: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ReferenceCard {
                Text("رادار أفضل مكان", color = RefInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("حرّك الراوتر ببطء والصوت يتسارع كلما تحسنت الجودة", color = RefMuted, fontSize = 9.sp)
                Spacer(Modifier.height(12.dp))
                PlacementScore(reading, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Text(placementGuidanceText(reading?.guidance), color = RefInk, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الرنين", color = RefInk, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Switch(checked = soundEnabled, onCheckedChange = onSound)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onToggle, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (placementMode) Color(0xFFE35A5A) else RefBlue)) {
                    Text(if (placementMode) "إيقاف البحث" else "ابدأ البحث", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ReferenceTools(
    runtime: RuntimeCapabilityReport?,
    telemetryCount: Int,
    stability: ConnectionStabilityReport,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    safetyBackupAvailable: Boolean,
    controlBusy: Boolean,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onRestoreSafetyBackup: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onCopyDiagnostics: () -> Unit,
    onShareDiagnostics: () -> Unit,
    onDisconnect: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ReferenceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("التحسين الذكي", color = RefInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("اختبارات موثقة مع نسخة أمان قبل التغيير", color = RefMuted, fontSize = 9.sp)
                    }
                    Switch(checked = smartMode, onCheckedChange = onSmartModeChange)
                }
                Spacer(Modifier.height(9.dp))
                Text("الهدف: ${referenceGoalName(smartGoal)}", color = RefInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OptimizationGoal.values().take(3).forEach { goal ->
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (goal == smartGoal) RefBlue else Color(0xFFF2F6FB))
                                .clickable { onSmartGoalChange(goal) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(referenceGoalName(goal), color = if (goal == smartGoal) Color.White else RefInk, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOptimizeNow, enabled = !smartBusy && !controlBusy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = RefBlue)) {
                    Text(if (smartBusy) "جاري التحسين…" else "تحسين الآن", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                smartReport?.message?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = RefMuted, fontSize = 9.sp, lineHeight = 13.sp)
                }
            }
        }
        item {
            ReferenceCard {
                Text("السلامة والتشخيص", color = RefInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text("عينات محفوظة: $telemetryCount • ${stability.summary}", color = RefMuted, fontSize = 9.sp, maxLines = 3)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onRestoreSafetyBackup, enabled = safetyBackupAvailable && !controlBusy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = RefGreen)) {
                    Text("استعادة آخر نسخة أمان", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCopyDiagnostics, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("نسخ التشخيص", fontSize = 9.sp) }
                    OutlinedButton(onClick = onShareDiagnostics, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("مشاركة", fontSize = 9.sp) }
                }
            }
        }
        item {
            ReferenceCard {
                Text("قدرات Firmware الحالية", color = RefInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                ReferenceCapability("تغيير وضع الشبكة", runtime?.networkMode?.canAttemptWrite == true)
                ReferenceCapability("تثبيت 4G", runtime?.lteBandControl?.canAttemptWrite == true)
                ReferenceCapability("تثبيت 5G", runtime?.nrBandControl?.canAttemptWrite == true)
                ReferenceCapability("تثبيت الخلية", runtime?.cellLock?.canAttemptWrite == true)
                ReferenceCapability("مسح الخلايا", runtime?.neighborScan?.hasRuntimeEvidence == true)
                ReferenceCapability("الهوائي", runtime?.antennaControl?.canAttemptWrite == true)
                if (runtime?.antennaControl?.canAttemptWrite == true) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..3).forEach { state ->
                            OutlinedButton(onClick = { onAntennaState(state) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                Text("هوائي $state", fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFE7B6B6))) {
                Text("قطع الاتصال بالراوتر", color = Color(0xFFD34F4F), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ReferenceCapability(label: String, available: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (available) RefGreen else RefMuted.copy(alpha = .35f)))
        Spacer(Modifier.width(8.dp))
        Text(label, color = RefInk, fontSize = 9.sp, modifier = Modifier.weight(1f))
        Text(if (available) "متاح" else "غير متاح", color = if (available) RefGreen else RefMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReferenceCard(onClick: (() -> Unit)? = null, content: @Composable Column.() -> Unit) {
    val modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(23.dp))
        .clip(RoundedCornerShape(23.dp))
        .background(Color.White)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(15.dp)
    Column(modifier, content = content)
}

@Composable
private fun ReferenceStatus(message: String) {
    if (message.isBlank()) return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(RefSoftBlue).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(RefBlue))
        Spacer(Modifier.width(8.dp))
        Text(message, color = RefInk, fontSize = 9.sp, lineHeight = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ReferenceEmpty(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(RefSoftBlue), contentAlignment = Alignment.Center) {
            Text("H", color = RefBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        Text(message, color = RefMuted, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 15.sp)
    }
}

private fun clean(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() && it != "---" && !it.equals("null", true) }

private fun referenceBands(snapshot: RouterSnapshot): List<String> = buildList {
    snapshot.cells.forEach { cell ->
        val band = clean(cell.band)
        if (band != null) {
            val normalized = when {
                cell.role.name == "NR" && band.startsWith("n", true) -> band
                cell.role.name == "NR" -> "n${band.filter(Char::isDigit)}"
                band.startsWith("B", true) -> band.uppercase()
                else -> "B${band.filter(Char::isDigit)}"
            }
            if (normalized.length > 1 && normalized !in this) add(normalized)
        }
    }
    if (isEmpty()) {
        clean(snapshot.lteBand)?.let { add(if (it.startsWith("B", true)) it.uppercase() else "B${it.filter(Char::isDigit)}") }
        clean(snapshot.nrBand)?.let {
            val normalized = if (it.startsWith("n", true)) it.lowercase() else "n${it.filter(Char::isDigit)}"
            if (normalized !in this) add(normalized)
        }
    }
}.filter { it.length > 1 }

private fun referenceBandSummary(snapshot: RouterSnapshot): String = referenceBands(snapshot).take(4).joinToString(" + ").ifBlank { "غير مؤكد" }

private fun referenceModeName(raw: String?): String = when (raw) {
    "Only_5G" -> "5G فقط"
    "Only_LTE" -> "4G فقط"
    "Only_WCDMA" -> "3G فقط"
    "WL_AND_5G" -> "تلقائي"
    null -> "غير مؤكد"
    else -> raw
}

private fun referenceGoalName(goal: OptimizationGoal): String = when (goal.name) {
    "SPEED", "MAX_SPEED", "THROUGHPUT" -> "السرعة"
    "STABILITY", "STABLE" -> "الثبات"
    else -> "متوازن"
}

private fun signalScore(snapshot: RouterSnapshot): Int {
    val rsrp = snapshot.nrRsrp ?: snapshot.lteRsrp
    val sinr = snapshot.nrSinr ?: snapshot.lteSinr
    val rsrpScore = when {
        rsrp == null -> 0
        rsrp >= -80 -> 100
        rsrp >= -90 -> 84
        rsrp >= -100 -> 66
        rsrp >= -110 -> 46
        rsrp >= -120 -> 28
        else -> 12
    }
    val sinrScore = when {
        sinr == null -> rsrpScore
        sinr >= 25 -> 100
        sinr >= 15 -> 82
        sinr >= 8 -> 62
        sinr >= 0 -> 40
        else -> 18
    }
    return ((rsrpScore * .62) + (sinrScore * .38)).roundToInt().coerceIn(0, 100)
}

private fun referenceSignalLabel(score: Int): String = when {
    score >= 85 -> "ممتاز"
    score >= 68 -> "جيد جدًا"
    score >= 50 -> "جيد"
    score >= 32 -> "متوسط"
    else -> "ضعيف"
}
