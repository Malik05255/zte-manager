package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetryFormatter
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetryFormatter
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState
import com.malik.ztesmartmanager.core.presentation.HaiResponsivePolicy
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerMatch
import com.malik.ztesmartmanager.core.tower.TowerTarget
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val V4Bg = Color(0xFFF7F9FC)
private val V4Card = Color.White
private val V4Ink = Color(0xFF0B2D69)
private val V4Muted = Color(0xFF7A859C)
private val V4Blue = Color(0xFF1472F3)
private val V4BlueDeep = Color(0xFF0B3D8C)
private val V4Green = Color(0xFF16B979)
private val V4Red = Color(0xFFE14B4B)
private val V4Border = Color(0xFFDCE4EE)
private val V4SoftBlue = Color(0xFFF1F6FF)
private val V4SoftGreen = Color(0xFFEAF9F2)
private val V4SoftOrange = Color(0xFFFFF4E8)

private enum class V4Section { HOME, NETWORK, TOWERS, BANDS, TOOLS }

private data class V4Layout(
    val compact: Boolean,
    val padding: Int,
    val gap: Int,
    val scale: Float,
    val bandColumns: Int
)

@Composable
fun ArabicHaiDashboardV4(
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
        var section by rememberSaveable { mutableStateOf(V4Section.HOME) }
        BoxWithConstraints(Modifier.fillMaxSize().background(V4Bg)) {
            val width = maxWidth.value.roundToInt().coerceAtLeast(1)
            val height = maxHeight.value.roundToInt().coerceAtLeast(1)
            val spec = remember(width, height) { HaiResponsivePolicy.resolve(width, height) }
            val layout = V4Layout(
                compact = width < 380,
                padding = if (width < 360) 12 else if (width < 430) 16 else 20,
                gap = if (height < 760) 10 else 12,
                scale = spec.textScale.coerceIn(1.0f, 1.10f),
                bandColumns = if (width < 350) 3 else 4
            )

            Column(Modifier.fillMaxSize()) {
                if (section == V4Section.HOME) {
                    V4HomeHeader(layout, snapshot != null, onDisconnect)
                } else {
                    V4Header(layout, snapshot != null, onDisconnect)
                }

                if (snapshot == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (status.isNotBlank() && status != "غير متصل") status else "جاري الاتصال…",
                            color = V4Muted,
                            fontSize = v4sp(layout, 14),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    if (section == V4Section.HOME) V4HomeBottomNav(section, { section = it })
                    else V4BottomNav(section, { section = it })
                    return@Column
                }

                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = layout.padding.dp,
                            end = layout.padding.dp,
                            top = if (section == V4Section.HOME) 2.dp else 4.dp,
                            bottom = 98.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(layout.gap.dp)
                    ) {
                        if (section != V4Section.HOME) {
                            compactOperationMessageV4(operationMessage).takeIf { it.isNotBlank() }?.let { msg ->
                                item { V4Message(layout, msg) }
                            }
                        }

                        when (section) {
                            V4Section.HOME -> {
                                item { V4HomeNetworkCard(layout, snapshot) }
                                item {
                                    V4HomeMainGrid(
                                        layout = layout,
                                        snapshot = snapshot,
                                        performance = lastPerformance,
                                        speedBusy = speedBusy,
                                        controlBusy = controlBusy,
                                        onSpeedTest = onSpeedTest,
                                        onSetNetworkMode = onSetNetworkMode
                                    )
                                }
                            }

                            V4Section.NETWORK -> {
                                item { V4NetworkCard(layout, snapshot) }
                                item { V4NetworkMode(layout, controlBusy, onSetNetworkMode) }
                                item { V4Traffic(layout, traffic) }
                                if (thermal?.hasAnyEvidence == true) item { V4Thermal(layout, thermal) }
                            }

                            V4Section.TOWERS -> {
                                item {
                                    V4TowerControls(
                                        layout,
                                        snapshot,
                                        nearbyCells,
                                        controlBusy,
                                        scanBusy,
                                        towerTarget,
                                        towerGuardEnabled,
                                        towerGuardStatus,
                                        onScanCells,
                                        onLockCurrentCell,
                                        onLockNearbyCell,
                                        onClearCellLock,
                                        onTowerGuardChange
                                    )
                                }
                            }

                            V4Section.BANDS -> {
                                item {
                                    V4Bands(
                                        layout,
                                        snapshot,
                                        capabilities,
                                        selectedLte,
                                        selectedNr,
                                        controlBusy,
                                        onLteToggle,
                                        onNrToggle,
                                        onApplyLte,
                                        onApplyNr
                                    )
                                }
                            }

                            V4Section.TOOLS -> {
                                item {
                                    V4Tools(
                                        layout,
                                        runtime,
                                        stability,
                                        placementMode,
                                        placementReading,
                                        smartMode,
                                        smartGoal,
                                        smartBusy,
                                        smartReport,
                                        safetyBackupAvailable,
                                        capabilities.supportsAntennaControl,
                                        controlBusy,
                                        onPlacementToggle,
                                        onSmartModeChange,
                                        onSmartGoalChange,
                                        onOptimizeNow,
                                        onRestoreSafetyBackup,
                                        onAntennaState,
                                        onCopyDiagnostics,
                                        onShareDiagnostics
                                    )
                                }
                            }
                        }
                    }

                    if (section == V4Section.HOME) {
                        V4HomeBottomNav(section, { section = it }, Modifier.align(Alignment.BottomCenter))
                    } else {
                        V4BottomNav(section, { section = it }, Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
    }
}

@Composable
private fun V4HomeHeader(layout: V4Layout, connected: Boolean, onDisconnect: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = layout.padding.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☼", color = V4Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                Text("⌕", color = V4Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ZTE Smart HAI", color = V4Ink, fontSize = v4sp(layout, 25), fontWeight = FontWeight.Black, maxLines = 1)
                Text("إدارة شبكتك ... بكل سهولة", color = V4Muted, fontSize = v4sp(layout, 12), maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(RoundedCornerShape(28.dp)).background(if (connected) V4SoftGreen else Color(0xFFFFEEEE))
                        .clickable(enabled = connected, onClick = onDisconnect).padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) V4Green else V4Red))
                    Spacer(Modifier.width(7.dp))
                    Text(if (connected) "متصل" else "غير متصل", color = V4Ink, fontSize = v4sp(layout, 12), fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(10.dp))
                Text("☰", color = V4Ink, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun V4HomeNetworkCard(layout: V4Layout, snapshot: RouterSnapshot) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val network = when { nr -> "5G"; lte -> "4G"; else -> "—" }
    val mode = when {
        nr && snapshot.networkType.orEmpty().contains("SA", true) && !snapshot.networkType.orEmpty().contains("NSA", true) -> "SA"
        nr -> "NR"
        lte -> "LTE"
        else -> "غير مؤكد"
    }
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val rsrq = snapshot.lteRsrq
    val activeBands = v4ActiveBands(snapshot)
    val bandSummary = activeBands.take(3).joinToString(" + ").ifBlank { snapshot.lteBand?.uppercase().orEmpty().ifBlank { "—" } }
    val mergeActive = snapshot.caActive && activeBands.size > 1

    Card(
        modifier = Modifier.fillMaxWidth().shadow(5.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFFBFD9FF))
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(
                    listOf(Color.White, Color(0xFFF4FAFF), Color(0xFFEAF4FF))
                )
            )
        ) {
            Canvas(Modifier.fillMaxWidth().height(205.dp)) {
                drawCircle(Color(0x332A8CFF), radius = size.width * .25f, center = Offset(size.width * .88f, size.height * .08f))
                drawCircle(Color(0x222A8CFF), radius = size.width * .36f, center = Offset(size.width * .22f, size.height * .72f))
            }

            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.weight(1.02f).clip(RoundedCornerShape(22.dp)).background(V4SoftGreen).padding(horizontal = 12.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(43.dp).clip(CircleShape).background(V4Green), contentAlignment = Alignment.Center) {
                                Text("✓", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text("متصل بالإنترنت", color = V4Green, fontSize = v4sp(layout, 13), fontWeight = FontWeight.Black)
                                Text("جودة الإشارة ${v4Quality(rsrp)}", color = V4Ink, fontSize = v4sp(layout, 10), fontWeight = FontWeight.Medium)
                            }
                        }

                        Column(Modifier.weight(1.22f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("حالة الشبكة", color = V4Ink, fontSize = v4sp(layout, 22), fontWeight = FontWeight.Black)
                            Text(v4OperatorLabel(snapshot), color = V4Ink, fontSize = v4sp(layout, 14), fontWeight = FontWeight.Medium)
                            Text("${snapshot.model ?: "ZTE"}  |  ${snapshot.operatorCode ?: "—"}", color = V4Muted, fontSize = v4sp(layout, 11))
                        }

                        Box(
                            Modifier.size(84.dp).clip(RoundedCornerShape(25.dp)).background(V4Blue),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(network, color = Color.White, fontSize = v4sp(layout, 34), fontWeight = FontWeight.Black)
                                Text(mode, color = Color(0xFFD7E8FF), fontSize = v4sp(layout, 14), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))

                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .92f))
                        .border(1.dp, Color(0xFFE6EDF6), RoundedCornerShape(24.dp)).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    V4HomeSignalStrength(layout, rsrp, Modifier.weight(1f))
                    V4HomeDivider()
                    V4HomeSignalQuality(layout, rsrq, Modifier.weight(1f))
                    V4HomeDivider()
                    V4HomeBands(layout, activeBands, Modifier.weight(1.35f))
                    V4HomeDivider()
                    V4HomeMerge(layout, mergeActive, bandSummary, Modifier.weight(1.05f))
                }
            }
        }
    }
}

@Composable
private fun V4HomeDivider() {
    Box(Modifier.width(1.dp).height(78.dp).background(Color(0xFFE2E9F2)))
}

@Composable
private fun V4HomeSignalStrength(layout: V4Layout, rsrp: Double?, modifier: Modifier) {
    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("▂▄▆█", color = if (rsrp == null) V4Muted else V4Green, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text("قوة الإشارة", color = V4Ink, fontSize = v4sp(layout, 10), fontWeight = FontWeight.Bold, maxLines = 1)
        Text(rsrp?.let { "${v4Fmt1(it)} dBm" } ?: "—", color = V4Ink, fontSize = v4sp(layout, 11), fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth(.72f).height(7.dp).clip(CircleShape).background(Color(0xFFE4E9F0))) {
            Box(Modifier.fillMaxWidth(v4SignalProgress(rsrp)).height(7.dp).clip(CircleShape).background(V4Green))
        }
    }
}

@Composable
private fun V4HomeSignalQuality(layout: V4Layout, rsrq: Double?, modifier: Modifier) {
    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("∿", color = V4Blue, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("جودة الإشارة", color = V4Ink, fontSize = v4sp(layout, 10), fontWeight = FontWeight.Bold, maxLines = 1)
        Text(rsrq?.let { "RSRQ: ${v4Fmt0(it)} dB" } ?: "RSRQ: —", color = V4Ink, fontSize = v4sp(layout, 10), fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth(.72f).height(7.dp).clip(CircleShape).background(Color(0xFFE4E9F0))) {
            Box(Modifier.fillMaxWidth(v4RsrqProgress(rsrq)).height(7.dp).clip(CircleShape).background(V4Green))
        }
    }
}

@Composable
private fun V4HomeBands(layout: V4Layout, bands: List<String>, modifier: Modifier) {
    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("♜", color = V4Blue, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("الترددات المتصلة", color = V4Ink, fontSize = v4sp(layout, 10), fontWeight = FontWeight.Bold, maxLines = 1)
        val shown = bands.take(2)
        if (shown.isEmpty()) {
            Text("—", color = V4Muted, fontSize = v4sp(layout, 10))
        } else {
            shown.forEach { band ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(V4Green))
                    Spacer(Modifier.width(5.dp))
                    Text("$band  ${v4BandFrequency(band)}", color = V4Ink, fontSize = v4sp(layout, 9), fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun V4HomeMerge(layout: V4Layout, active: Boolean, summary: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔗", color = V4Blue, fontSize = 18.sp)
        Text("دمج الترددات", color = V4Ink, fontSize = v4sp(layout, 10), fontWeight = FontWeight.Bold, maxLines = 1)
        Box(Modifier.clip(RoundedCornerShape(50)).background(if (active) V4SoftGreen else Color(0xFFF1F3F6)).padding(horizontal = 11.dp, vertical = 4.dp)) {
            Text(if (active) "مفعل" else "غير مفعل", color = if (active) V4Green else V4Muted, fontSize = v4sp(layout, 10), fontWeight = FontWeight.Black)
        }
        Text(summary, color = V4Ink, fontSize = v4sp(layout, 11), fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun V4HomeMainGrid(
    layout: V4Layout,
    snapshot: RouterSnapshot,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    controlBusy: Boolean,
    onSpeedTest: () -> Unit,
    onSetNetworkMode: (String) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.fillMaxWidth().height(if (layout.compact) 390.dp else 410.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            V4HomeMapCard(layout, snapshot, Modifier.weight(1.06f).fillMaxSize())
            Column(Modifier.weight(.94f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                V4HomeSpeedCard(layout, performance, speedBusy, onSpeedTest, Modifier.fillMaxWidth().height(196.dp))
                V4HomeNetworkModes(layout, snapshot, controlBusy, onSetNetworkMode, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun V4HomeMapCard(layout: V4Layout, snapshot: RouterSnapshot, modifier: Modifier) {
    val bands = v4ActiveBands(snapshot).take(2).joinToString(" + ").ifBlank { snapshot.lteBand?.uppercase().orEmpty().ifBlank { "—" } }
    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(25.dp)),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, V4Border)
    ) {
        Column(Modifier.fillMaxSize().padding(11.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("⌖", color = V4Blue, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("الخريطة", color = V4Ink, fontSize = v4sp(layout, 20), fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF4F7F2))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0xFFF6F8F5))
                    drawCircle(Color(0xFFE6F3E3), size.width * .20f, Offset(size.width * .12f, size.height * .15f))
                    drawCircle(Color(0xFFE6F3E3), size.width * .24f, Offset(size.width * .87f, size.height * .23f))
                    drawCircle(Color(0xFFD3ECFF), size.width * .20f, Offset(size.width * .91f, size.height * .91f))
                    val road = Color.White
                    val thin = size.width * .018f
                    drawLine(road, Offset(0f, size.height * .24f), Offset(size.width, size.height * .04f), thin)
                    drawLine(road, Offset(0f, size.height * .53f), Offset(size.width, size.height * .39f), thin)
                    drawLine(road, Offset(size.width * .06f, size.height), Offset(size.width * .48f, 0f), thin)
                    drawLine(road, Offset(size.width * .48f, size.height), Offset(size.width * .72f, 0f), thin)
                    drawLine(road, Offset(0f, size.height * .78f), Offset(size.width, size.height * .63f), thin)
                    drawLine(road, Offset(size.width * .20f, 0f), Offset(size.width * .98f, size.height), thin)
                    val tower = Offset(size.width * .62f, size.height * .31f)
                    val user = Offset(size.width * .22f, size.height * .77f)
                    drawLine(
                        V4Blue,
                        start = user,
                        end = tower,
                        strokeWidth = size.width * .012f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                    )
                    drawCircle(Color(0x33E14B4B), size.width * .105f, tower)
                    drawCircle(Color(0x66E14B4B), size.width * .072f, tower)
                    drawCircle(V4Red, size.width * .030f, tower)
                    drawCircle(Color(0x331472F3), size.width * .11f, user)
                    drawCircle(Color(0x661472F3), size.width * .075f, user)
                    drawCircle(V4Blue, size.width * .032f, user)
                    drawCircle(Color.White, size.width * .020f, user)
                }

                Column(
                    Modifier.align(Alignment.TopCenter).padding(top = 48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .95f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("البرج المتصل", color = V4Ink, fontSize = v4sp(layout, 9), fontWeight = FontWeight.Black)
                    Text(bands, color = V4Ink, fontSize = v4sp(layout, 9), fontWeight = FontWeight.Black)
                }

                Text(
                    "♜",
                    color = V4Red,
                    fontSize = 26.sp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 101.dp)
                )

                Text(
                    "●",
                    color = V4Blue,
                    fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 31.dp, bottom = 57.dp)
                )
                Text(
                    "موقعك الحالي",
                    color = V4BlueDeep,
                    fontSize = v4sp(layout, 9),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 22.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .94f)).padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5F7FA)).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("⌖", color = V4Muted, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text("الموقع غير مؤكد", color = V4Muted, fontSize = v4sp(layout, 10), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun V4HomeSpeedCard(
    layout: V4Layout,
    performance: NetworkPerformance?,
    busy: Boolean,
    onSpeedTest: () -> Unit,
    modifier: Modifier
) {
    val value = performance?.downloadMbps
    val display = value?.let(::v4Fmt1) ?: "—"
    val progress = ((value ?: 0.0) / 300.0).coerceIn(0.0, 1.0).toFloat()

    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(25.dp)),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, V4Border)
    ) {
        Column(Modifier.fillMaxSize().padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("◉", color = V4Blue, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text("السرعة", color = V4Ink, fontSize = v4sp(layout, 20), fontWeight = FontWeight.Black)
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                    val stroke = size.width * .035f
                    drawArc(Color(0xFFE1E7EF), 150f, 240f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
                    if (progress > 0f) {
                        drawArc(V4Blue, 150f, 240f * progress, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
                    }
                    val center = Offset(size.width / 2f, size.height * .68f)
                    val radius = size.width * .23f
                    val angle = (150f + 240f * progress) * (PI / 180.0)
                    val end = Offset(
                        center.x + (cos(angle) * radius).toFloat(),
                        center.y + (sin(angle) * radius).toFloat()
                    )
                    drawLine(V4Ink, center, end, strokeWidth = size.width * .010f, cap = StrokeCap.Round)
                    drawCircle(V4Ink, size.width * .018f, center)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 10.dp)) {
                    Text(display, color = V4Ink, fontSize = v4sp(layout, 28), fontWeight = FontWeight.Black)
                    Text("Mb/s", color = V4Muted, fontSize = v4sp(layout, 14))
                }
            }
            Text(
                if (busy) "جاري القياس…" else "اضغط للقياس",
                color = if (busy) V4Muted else V4Blue,
                fontSize = v4sp(layout, 12),
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable(enabled = !busy, onClick = onSpeedTest).padding(6.dp)
            )
        }
    }
}

@Composable
private fun V4HomeNetworkModes(
    layout: V4Layout,
    snapshot: RouterSnapshot,
    busy: Boolean,
    onSetNetworkMode: (String) -> Unit,
    modifier: Modifier
) {
    val current = snapshot.raw["BearerPreference"]?.trim().orEmpty()
    val modes = listOf(
        "تلقائي" to "WL_AND_5G",
        "2G فقط" to "Only_GSM",
        "3G فقط" to "Only_WCDMA",
        "4G فقط" to "Only_LTE",
        "5G فقط" to "Only_5G"
    )

    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(25.dp)),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, V4Border)
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("⚙", color = V4Blue, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Text("أوضاع الشبكة", color = V4Ink, fontSize = v4sp(layout, 16), fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(5.dp))
            modes.forEach { (label, mode) ->
                val selected = current.equals(mode, true)
                Row(
                    Modifier.fillMaxWidth().height(31.dp).clip(RoundedCornerShape(13.dp))
                        .background(if (selected) Color(0xFFEEF6FF) else Color.White)
                        .border(1.dp, if (selected) Color(0xFF8DC1FF) else Color(0xFFE6EAF0), RoundedCornerShape(13.dp))
                        .clickable(enabled = !busy) { onSetNetworkMode(mode) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = if (selected) V4Blue else V4Muted, fontSize = v4sp(layout, 10), fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.size(14.dp).clip(CircleShape).border(1.dp, if (selected) V4Blue else V4Muted, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Box(Modifier.size(7.dp).clip(CircleShape).background(V4Blue))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun V4Header(layout: V4Layout, connected: Boolean, onDisconnect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = layout.padding.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("ZTE Smart HAI", color = V4Ink, fontSize = v4sp(layout, 21), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(V4Card).border(1.dp, V4Border, RoundedCornerShape(50))
                .clickable(enabled = connected, onClick = onDisconnect).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) V4Green else V4Red))
            Spacer(Modifier.width(6.dp))
            Text(if (connected) "متصل" else "غير متصل", color = V4Ink, fontSize = v4sp(layout, 12), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V4NetworkCard(layout: V4Layout, snapshot: RouterSnapshot) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val network = when { nr -> "5G"; lte -> "4G"; else -> "—" }
    val mode = when {
        nr && snapshot.networkType.orEmpty().contains("SA", true) && !snapshot.networkType.orEmpty().contains("NSA", true) -> "SA"
        nr -> "NSA"
        lte -> "LTE"
        else -> "غير مؤكد"
    }
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val sinr = if (nr) snapshot.nrSinr else snapshot.lteSinr

    V4Card(layout) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(network, color = V4Ink, fontSize = v4sp(layout, 48), fontWeight = FontWeight.Black)
                    Text(mode, color = V4Blue, fontSize = v4sp(layout, 16), fontWeight = FontWeight.Bold)
                }
                V4Pill(v4Quality(rsrp), if (rsrp == null) V4Muted else V4Green)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V4Metric("RSRP", rsrp, "dBm", Modifier.weight(1f))
                V4Metric("SINR", sinr, "dB", Modifier.weight(1f))
                V4Metric("RSRQ", snapshot.lteRsrq, "dB", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V4NetworkMode(layout: V4Layout, busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    V4Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("وضع الشبكة", color = V4Ink, fontSize = v4sp(layout, 18), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            V4Action("4G فقط", !busy) { onSetNetworkMode("Only_LTE") }
            Spacer(Modifier.height(8.dp))
            V4Action("4G + 5G", !busy) { onSetNetworkMode("LTE_AND_5G") }
            Spacer(Modifier.height(8.dp))
            V4Action("5G فقط", !busy) { onSetNetworkMode("Only_5G") }
        }
    }
}

@Composable
private fun V4Traffic(layout: V4Layout, traffic: TrafficTelemetry?) {
    V4Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("حركة البيانات", color = V4Ink, fontSize = v4sp(layout, 18), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V4ValueBox("تنزيل", TrafficTelemetryFormatter.rateMbps(traffic?.rxBytesPerSecond), "Mb/s", V4SoftBlue, Modifier.weight(1f))
                V4ValueBox("رفع", TrafficTelemetryFormatter.rateMbps(traffic?.txBytesPerSecond), "Mb/s", V4SoftGreen, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V4Thermal(layout: V4Layout, thermal: ThermalTelemetry) {
    val highest = thermal.highestObserved
    V4Card(layout) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("الحرارة", color = V4Ink, fontSize = v4sp(layout, 18), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(highest?.let { ThermalTelemetryFormatter.celsius(it.celsius) } ?: "—", color = V4Blue, fontSize = v4sp(layout, 20), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V4TowerControls(
    layout: V4Layout,
    snapshot: RouterSnapshot,
    cells: List<NearbyCell>,
    busy: Boolean,
    scanBusy: Boolean,
    target: TowerTarget?,
    guardEnabled: Boolean,
    guardStatus: TowerGuardStatus?,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onLockCell: (NearbyCell) -> Unit,
    onClear: () -> Unit,
    onGuardChange: (Boolean) -> Unit
) {
    V4Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الأبراج", color = V4Ink, fontSize = v4sp(layout, 20), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                V4CompactAction(if (scanBusy) "جاري…" else "مسح", !scanBusy && !busy, onScan)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V4ValueBox("PCI", snapshot.pci?.toString() ?: "—", "", V4SoftBlue, Modifier.weight(1f))
                V4ValueBox("EARFCN", snapshot.earfcn?.toString() ?: "—", "", V4SoftGreen, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V4Primary("تثبيت الحالية", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
                V4Outline("إزالة القفل", !busy, Modifier.weight(1f), onClear)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("حارس البرج", color = V4Ink, fontSize = v4sp(layout, 14), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(
                    checked = guardEnabled,
                    onCheckedChange = onGuardChange,
                    enabled = target != null && !busy,
                    colors = SwitchDefaults.colors(checkedTrackColor = V4Green)
                )
            }
            if (target != null) {
                Text("PCI ${target.pci} • EARFCN ${target.earfcn}", color = V4Blue, fontSize = v4sp(layout, 12), fontWeight = FontWeight.Bold)
            }
            guardStatus?.let {
                val short = if (it.match == TowerMatch.MATCHED) "مطابق" else compactOperationMessageV4(it.message)
                if (short.isNotBlank()) Text(short, color = if (it.match == TowerMatch.MATCHED) V4Green else V4Muted, fontSize = v4sp(layout, 11), modifier = Modifier.padding(top = 4.dp))
            }

            if (cells.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                cells.take(12).forEachIndexed { index, cell ->
                    V4CellRow(index + 1, cell, snapshot, busy, onLockCell)
                    if (index != cells.take(12).lastIndex) Spacer(Modifier.height(7.dp))
                }
            }
        }
    }
}

@Composable
private fun V4CellRow(index: Int, cell: NearbyCell, snapshot: RouterSnapshot, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val current = cell.rat == "LTE" && cell.pci == snapshot.pci && cell.arfcn == snapshot.earfcn
    val lockable = cell.rat == "LTE" && cell.pci != null && cell.arfcn != null
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (current) V4SoftGreen else Color(0xFFF8FAFD)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(index.toString(), color = V4Blue, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = V4Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text("ARFCN ${cell.arfcn ?: "—"} • ${cell.rsrp?.let { "${v4Fmt0(it)} dBm" } ?: "—"}", color = V4Muted, fontSize = 11.sp)
        }
        if (lockable && !current) {
            Text("تثبيت", color = if (busy) V4Muted else V4Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = !busy) { onLock(cell) }.padding(8.dp))
        } else if (current) {
            V4Pill("الحالية", V4Green)
        }
    }
}

@Composable
private fun V4Bands(
    layout: V4Layout,
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
    var showNr by rememberSaveable { mutableStateOf(false) }
    val bands = if (showNr) capabilities.supportedNrBands.sorted() else capabilities.supportedLteBands.sorted()
    val selected = if (showNr) selectedNr else selectedLte
    val active = remember(snapshot.cells, showNr) {
        snapshot.cells.filter { if (showNr) it.role == CellRole.NR else it.role != CellRole.NR }
            .mapNotNull { v4BandNumber(it.band) }.toSet()
    }

    V4Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الترددات", color = V4Ink, fontSize = v4sp(layout, 20), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    V4Choice("4G", !showNr) { showNr = false }
                    V4Choice("5G", showNr) { showNr = true }
                }
            }
            Spacer(Modifier.height(14.dp))
            bands.chunked(layout.bandColumns).forEach { chunk ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunk.forEach { band ->
                        val isSelected = band in selected
                        val isActive = band in active
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (isActive) V4SoftGreen else if (isSelected) V4SoftBlue else Color(0xFFF8FAFD))
                                .border(1.dp, if (isActive) V4Green else if (isSelected) V4Blue else V4Border, RoundedCornerShape(16.dp))
                                .clickable(enabled = !busy) { if (showNr) onNrToggle(band) else onLteToggle(band) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${if (showNr) "N" else "B"}$band", color = V4Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    repeat(layout.bandColumns - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(6.dp))
            V4Primary(
                if (showNr) "تطبيق 5G" else "تطبيق 4G",
                selected.isNotEmpty() && !busy && (!showNr || capabilities.supportsNrBandLock),
                Modifier.fillMaxWidth()
            ) {
                if (showNr) onApplyNr() else onApplyLte()
            }
        }
    }
}

@Composable
private fun V4Tools(
    layout: V4Layout,
    runtime: RuntimeCapabilityReport?,
    stability: ConnectionStabilityReport,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    backupAvailable: Boolean,
    supportsAntenna: Boolean,
    busy: Boolean,
    onPlacementToggle: () -> Unit,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onRestore: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onCopyDiagnostics: () -> Unit,
    onShareDiagnostics: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
        V4Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("التحسين الذكي", color = V4Ink, fontSize = v4sp(layout, 18), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Switch(checked = smartMode, onCheckedChange = onSmartModeChange, colors = SwitchDefaults.colors(checkedTrackColor = V4Green))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OptimizationGoal.entries.forEach { goal -> V4Choice(v4Goal(goal), smartGoal == goal) { onSmartGoalChange(goal) } }
                }
                Spacer(Modifier.height(10.dp))
                V4Primary(if (smartBusy) "جاري…" else "تحسين الآن", !smartBusy && !busy, Modifier.fillMaxWidth(), onOptimizeNow)
                smartReport?.let { Text("آخر نتيجة: ${it.best.qualityScore}/100", color = V4Muted, fontSize = v4sp(layout, 11), modifier = Modifier.padding(top = 8.dp)) }
            }
        }

        V4Card(layout) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("أفضل مكان", color = V4Ink, fontSize = v4sp(layout, 18), fontWeight = FontWeight.Black)
                    placementReading?.let { Text("${it.score.total}/100", color = V4Blue, fontSize = v4sp(layout, 20), fontWeight = FontWeight.Black) }
                }
                Switch(checked = placementMode, onCheckedChange = { onPlacementToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = V4Green))
            }
        }

        V4Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Text("التشخيص", color = V4Ink, fontSize = v4sp(layout, 18), fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                if (runtime == null) {
                    Text("غير جاهز", color = V4Muted, fontSize = v4sp(layout, 12))
                } else {
                    val ready = listOf(
                        runtime.lteBandControl,
                        runtime.nrBandControl,
                        runtime.cellLock,
                        runtime.networkMode,
                        runtime.neighborScan,
                        runtime.antennaControl
                    ).count { it.state == RuntimeCapabilityState.SAFE_TO_ATTEMPT }
                    Text("$ready وظائف جاهزة", color = V4Green, fontSize = v4sp(layout, 14), fontWeight = FontWeight.Bold)
                }
                Text(stability.summary, color = V4Muted, fontSize = v4sp(layout, 11), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V4Outline("نسخ", true, Modifier.weight(1f), onCopyDiagnostics)
                    V4Outline("مشاركة", true, Modifier.weight(1f), onShareDiagnostics)
                }
            }
        }

        V4Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Text("الاستعادة", color = V4Ink, fontSize = v4sp(layout, 18), fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                V4Primary("استعادة النسخة", backupAvailable && !busy, Modifier.fillMaxWidth(), onRestore)
            }
        }

        if (supportsAntenna) {
            V4Card(layout) {
                Column(Modifier.padding(18.dp)) {
                    Text("الهوائي", color = V4Ink, fontSize = v4sp(layout, 18), fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { state -> V4Outline(state.toString(), !busy, Modifier.weight(1f)) { onAntennaState(state) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun V4Metric(label: String, value: Double?, unit: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF8FAFD)).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = V4Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value?.let(::v4Fmt1) ?: "—", color = V4Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(unit, color = V4Muted, fontSize = 10.sp)
    }
}

@Composable
private fun V4ValueBox(title: String, value: String, unit: String, bg: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(bg).padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = V4Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = V4Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
        if (unit.isNotBlank()) Text(unit, color = V4Muted, fontSize = 10.sp)
    }
}

@Composable
private fun V4Action(text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) V4SoftBlue else Color(0xFFF1F3F6))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = if (enabled) V4Ink else V4Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("‹", color = if (enabled) V4Blue else V4Muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V4Primary(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) V4Blue else Color(0xFFE7EAF0)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) Color.White else V4Muted, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun V4Outline(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(V4Card).border(1.dp, V4Border, RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) V4Ink else V4Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun V4CompactAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(V4SoftBlue).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(text, color = if (enabled) V4Blue else V4Muted, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V4Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (selected) V4Blue else Color(0xFFF0F3F8)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(text, color = if (selected) Color.White else V4Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V4Pill(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .12f)).padding(horizontal = 11.dp, vertical = 7.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V4Message(layout: V4Layout, text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(V4SoftBlue).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(text, color = V4Ink, fontSize = v4sp(layout, 12), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V4Card(layout: V4Layout, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(7.dp, RoundedCornerShape(if (layout.compact) 22.dp else 26.dp)),
        shape = RoundedCornerShape(if (layout.compact) 22.dp else 26.dp),
        colors = CardDefaults.cardColors(containerColor = V4Card),
        border = BorderStroke(1.dp, V4Border)
    ) { content() }
}

@Composable
private fun V4HomeBottomNav(selected: V4Section, onSelect: (V4Section) -> Unit, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier.fillMaxWidth().navigationBarsPadding().height(82.dp).shadow(12.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V4Nav("☰", "المزيد", false, Modifier.weight(1f)) { onSelect(V4Section.BANDS) }
            V4Nav("▣", "السجلات", false, Modifier.weight(1f)) { onSelect(V4Section.TOWERS) }
            V4Nav("⚒", "الأدوات", false, Modifier.weight(1f)) { onSelect(V4Section.TOOLS) }
            V4Nav("▂▄▆█", "الشبكة", false, Modifier.weight(1f)) { onSelect(V4Section.NETWORK) }
            V4Nav("⌂", "الرئيسية", selected == V4Section.HOME, Modifier.weight(1f)) { onSelect(V4Section.HOME) }
        }
    }
}

@Composable
private fun V4BottomNav(selected: V4Section, onSelect: (V4Section) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().height(78.dp).background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V4Nav("⌂", "الرئيسية", selected == V4Section.HOME, Modifier.weight(1f)) { onSelect(V4Section.HOME) }
        V4Nav("⌁", "الشبكة", selected == V4Section.NETWORK, Modifier.weight(1f)) { onSelect(V4Section.NETWORK) }
        V4Nav("⌾", "الأبراج", selected == V4Section.TOWERS, Modifier.weight(1f)) { onSelect(V4Section.TOWERS) }
        V4Nav("▦", "الترددات", selected == V4Section.BANDS, Modifier.weight(1f)) { onSelect(V4Section.BANDS) }
        V4Nav("⚒", "الأدوات", selected == V4Section.TOOLS, Modifier.weight(1f)) { onSelect(V4Section.TOOLS) }
    }
}

@Composable
private fun V4Nav(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected) V4Blue else V4Muted, fontSize = if (icon.length > 2) 14.sp else 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (selected) V4Blue else V4Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, maxLines = 1)
    }
}

private fun compactOperationMessageV4(message: String): String {
    val value = message.trim()
    if (value.isBlank()) return ""
    val first = value.substringBefore(" • ").substringBefore("؛").substringBefore(" (").trim()
    return when {
        first.contains("حفظ نسخة أمان") -> "جاري الحفظ…"
        first.contains("إعادة التحقق") -> "جاري التحقق…"
        first.contains("جاري أخذ عدة قراءات") -> "جاري المسح…"
        first.contains("جاري قياس السرعة") -> "جاري القياس…"
        first.contains("جاري استعادة") -> "جاري الاستعادة…"
        first.length > 86 -> first.take(83) + "…"
        else -> first
    }
}

private fun v4OperatorLabel(snapshot: RouterSnapshot): String = when (snapshot.operatorCode?.trim()) {
    "42001" -> "SA - STC"
    "42003" -> "SA - Mobily"
    "42004" -> "SA - Zain"
    else -> snapshot.operatorCode?.takeIf { it.isNotBlank() } ?: "الشبكة"
}

private fun v4ActiveBands(snapshot: RouterSnapshot): List<String> {
    val fromCells = snapshot.cells.filter { it.role != CellRole.NR }.mapNotNull { it.band?.trim()?.uppercase() }.distinct()
    if (fromCells.isNotEmpty()) return fromCells
    return listOfNotNull(snapshot.lteBand?.trim()?.uppercase()).distinct()
}

private fun v4BandFrequency(band: String): String = when (v4BandNumber(band)) {
    1 -> "2100 MHz"
    3 -> "1800 MHz"
    5 -> "850 MHz"
    7 -> "2600 MHz"
    8 -> "900 MHz"
    20 -> "800 MHz"
    28 -> "700 MHz"
    38 -> "2600 MHz"
    40 -> "2300 MHz"
    41 -> "2500 MHz"
    else -> ""
}

private fun v4SignalProgress(rsrp: Double?): Float = when {
    rsrp == null -> 0f
    rsrp >= -80 -> 1f
    rsrp <= -120 -> .08f
    else -> ((rsrp + 120.0) / 40.0).toFloat().coerceIn(.08f, 1f)
}

private fun v4RsrqProgress(rsrq: Double?): Float = when {
    rsrq == null -> 0f
    rsrq >= -7 -> 1f
    rsrq <= -20 -> .08f
    else -> ((rsrq + 20.0) / 13.0).toFloat().coerceIn(.08f, 1f)
}

private fun v4Quality(rsrp: Double?): String = when {
    rsrp == null -> "غير معروفة"
    rsrp >= -85 -> "ممتازة"
    rsrp >= -95 -> "جيدة"
    rsrp >= -105 -> "متوسطة"
    else -> "ضعيفة"
}

private fun v4Goal(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun v4BandNumber(value: String?): Int? = value?.trim()?.removePrefix("n")?.removePrefix("N")?.removePrefix("b")?.removePrefix("B")?.toIntOrNull()
private fun v4Fmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
private fun v4Fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun v4sp(layout: V4Layout, base: Int) = (base * layout.scale).sp
