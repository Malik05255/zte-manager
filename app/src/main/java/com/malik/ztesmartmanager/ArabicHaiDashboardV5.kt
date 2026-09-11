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
import androidx.compose.foundation.layout.fillMaxHeight
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

private val V5Bg = Color(0xFFF7F9FC)
private val V5Card = Color.White
private val V5Ink = Color(0xFF0A2C68)
private val V5Muted = Color(0xFF7B879D)
private val V5Blue = Color(0xFF0F73F6)
private val V5BlueDeep = Color(0xFF0B3D8D)
private val V5Green = Color(0xFF15B978)
private val V5Red = Color(0xFFE14C4C)
private val V5Border = Color(0xFFDCE5EF)
private val V5SoftBlue = Color(0xFFEEF6FF)
private val V5SoftGreen = Color(0xFFEAF9F2)
private val V5SoftGray = Color(0xFFF4F6FA)

private enum class V5Section { HOME, NETWORK, TOWERS, BANDS, TOOLS }

private data class V5Layout(
    val compact: Boolean,
    val tiny: Boolean,
    val padding: Int,
    val gap: Int,
    val gridHeight: Int,
    val speedHeight: Int,
    val bandColumns: Int
)

@Composable
fun ArabicHaiDashboardV5(
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
        var section by rememberSaveable { mutableStateOf(V5Section.HOME) }
        BoxWithConstraints(Modifier.fillMaxSize().background(V5Bg)) {
            val width = maxWidth.value.roundToInt().coerceAtLeast(1)
            val compact = width < 390
            val tiny = width < 345
            val layout = V5Layout(
                compact = compact,
                tiny = tiny,
                padding = when { tiny -> 10; compact -> 12; width < 470 -> 16; else -> 20 },
                gap = if (compact) 10 else 12,
                gridHeight = when { tiny -> 520; compact -> 500; else -> 520 },
                speedHeight = if (compact) 204 else 214,
                bandColumns = if (width < 360) 3 else 4
            )

            Column(Modifier.fillMaxSize()) {
                V5Header(layout, snapshot != null, onDisconnect)

                if (snapshot == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (status.isNotBlank() && status != "غير متصل") status else "جاري الاتصال…",
                            color = V5Muted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    V5BottomNav(section, { section = it })
                    return@Column
                }

                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = layout.padding.dp,
                            end = layout.padding.dp,
                            top = 2.dp,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(layout.gap.dp)
                    ) {
                        if (section != V5Section.HOME) {
                            v5CompactMessage(operationMessage).takeIf { it.isNotBlank() }?.let { msg ->
                                item { V5Message(msg) }
                            }
                        }

                        when (section) {
                            V5Section.HOME -> {
                                item { V5HeroNetworkCard(layout, snapshot) }
                                item {
                                    V5HomeGrid(
                                        layout = layout,
                                        snapshot = snapshot,
                                        performance = lastPerformance,
                                        speedBusy = speedBusy,
                                        controlBusy = controlBusy,
                                        scanBusy = scanBusy,
                                        towerTarget = towerTarget,
                                        onSpeedTest = onSpeedTest,
                                        onSetNetworkMode = onSetNetworkMode,
                                        onScanCells = onScanCells
                                    )
                                }
                            }

                            V5Section.NETWORK -> {
                                item { V5NetworkDetail(snapshot) }
                                item { V5NetworkModePanel(snapshot, controlBusy, onSetNetworkMode) }
                                item { V5TrafficPanel(traffic) }
                                if (thermal?.hasAnyEvidence == true) item { V5ThermalPanel(thermal) }
                            }

                            V5Section.TOWERS -> {
                                item {
                                    V5TowerPanel(
                                        snapshot = snapshot,
                                        cells = nearbyCells,
                                        busy = controlBusy,
                                        scanBusy = scanBusy,
                                        target = towerTarget,
                                        guardEnabled = towerGuardEnabled,
                                        guardStatus = towerGuardStatus,
                                        onScan = onScanCells,
                                        onLockCurrent = onLockCurrentCell,
                                        onLockCell = onLockNearbyCell,
                                        onClear = onClearCellLock,
                                        onGuardChange = onTowerGuardChange
                                    )
                                }
                            }

                            V5Section.BANDS -> {
                                item {
                                    V5BandsPanel(
                                        layout = layout,
                                        snapshot = snapshot,
                                        capabilities = capabilities,
                                        selectedLte = selectedLte,
                                        selectedNr = selectedNr,
                                        busy = controlBusy,
                                        onLteToggle = onLteToggle,
                                        onNrToggle = onNrToggle,
                                        onApplyLte = onApplyLte,
                                        onApplyNr = onApplyNr
                                    )
                                }
                            }

                            V5Section.TOOLS -> {
                                item {
                                    V5ToolsPanel(
                                        runtime = runtime,
                                        stability = stability,
                                        placementMode = placementMode,
                                        placementReading = placementReading,
                                        smartMode = smartMode,
                                        smartGoal = smartGoal,
                                        smartBusy = smartBusy,
                                        smartReport = smartReport,
                                        backupAvailable = safetyBackupAvailable,
                                        supportsAntenna = capabilities.supportsAntennaControl,
                                        busy = controlBusy,
                                        onPlacementToggle = onPlacementToggle,
                                        onSmartModeChange = onSmartModeChange,
                                        onSmartGoalChange = onSmartGoalChange,
                                        onOptimizeNow = onOptimizeNow,
                                        onRestore = onRestoreSafetyBackup,
                                        onAntennaState = onAntennaState,
                                        onCopyDiagnostics = onCopyDiagnostics,
                                        onShareDiagnostics = onShareDiagnostics
                                    )
                                }
                            }
                        }
                    }

                    V5BottomNav(section, { section = it }, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun V5Header(layout: V5Layout, connected: Boolean, onDisconnect: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = layout.padding.dp, vertical = if (layout.compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.width(if (layout.tiny) 58.dp else 68.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("☼", color = V5Ink, fontSize = if (layout.tiny) 23.sp else 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(if (layout.tiny) 8.dp else 12.dp))
                Text("⌕", color = V5Ink, fontSize = if (layout.tiny) 25.sp else 28.sp, fontWeight = FontWeight.Bold)
            }

            Column(Modifier.weight(1f).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ZTE Smart HAI",
                    color = V5Ink,
                    fontSize = when { layout.tiny -> 18.sp; layout.compact -> 20.sp; else -> 22.sp },
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text("إدارة شبكتك ... بكل سهولة", color = V5Muted, fontSize = if (layout.tiny) 10.sp else 11.sp, maxLines = 1)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(RoundedCornerShape(26.dp)).background(if (connected) V5SoftGreen else Color(0xFFFFEEEE))
                        .clickable(enabled = connected, onClick = onDisconnect)
                        .padding(horizontal = if (layout.tiny) 9.dp else 11.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) V5Green else V5Red))
                    Spacer(Modifier.width(6.dp))
                    Text(if (connected) "متصل" else "غير متصل", color = V5Ink, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(if (layout.tiny) 5.dp else 8.dp))
                Text("☰", color = V5Ink, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun V5HeroNetworkCard(layout: V5Layout, snapshot: RouterSnapshot) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val network = when { nr -> "5G"; lte -> "4G"; else -> "—" }
    val radioMode = when {
        nr && snapshot.networkType.orEmpty().contains("SA", true) && !snapshot.networkType.orEmpty().contains("NSA", true) -> "SA"
        nr -> "NR"
        lte -> "LTE"
        else -> "غير مؤكد"
    }
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val rsrq = snapshot.lteRsrq
    val bands = v5ActiveBands(snapshot)
    val merged = snapshot.caActive && bands.size > 1
    val summary = bands.take(3).joinToString(" + ").ifBlank { snapshot.lteBand?.uppercase().orEmpty().ifBlank { "—" } }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(27.dp)),
        shape = RoundedCornerShape(27.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFFB9D7FF))
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color.White, Color(0xFFF5FAFF), Color(0xFFEAF4FF)))
            )
        ) {
            Canvas(Modifier.fillMaxWidth().height(if (layout.compact) 238.dp else 250.dp)) {
                drawCircle(Color(0x342D8EFF), radius = size.width * .25f, center = Offset(size.width * .90f, size.height * .08f))
                drawCircle(Color(0x252D8EFF), radius = size.width * .38f, center = Offset(size.width * .18f, size.height * .82f))
            }

            Column(Modifier.fillMaxWidth().padding(if (layout.compact) 10.dp else 12.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier.fillMaxWidth().height(if (layout.compact) 108.dp else 114.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (layout.tiny) 6.dp else 8.dp)
                    ) {
                        Row(
                            Modifier.weight(1.02f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(V5SoftGreen)
                                .padding(horizontal = if (layout.tiny) 8.dp else 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(if (layout.tiny) 38.dp else 43.dp).clip(CircleShape).background(V5Green),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", color = Color.White, fontSize = if (layout.tiny) 23.sp else 27.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(7.dp))
                            Column {
                                Text("متصل بالإنترنت", color = V5Green, fontSize = if (layout.tiny) 10.sp else 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                Text("جودة الإشارة ${v5Quality(rsrp)}", color = V5Ink, fontSize = if (layout.tiny) 9.sp else 10.sp, maxLines = 1)
                            }
                        }

                        Column(Modifier.weight(1.18f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("حالة الشبكة", color = V5Ink, fontSize = if (layout.tiny) 18.sp else 21.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Text(v5OperatorLabel(snapshot), color = V5Ink, fontSize = if (layout.tiny) 11.sp else 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text("${snapshot.model ?: "ZTE"}  |  ${snapshot.operatorCode ?: "—"}", color = V5Muted, fontSize = 10.sp, maxLines = 1)
                        }

                        Box(
                            Modifier.size(if (layout.tiny) 74.dp else 84.dp).clip(RoundedCornerShape(23.dp)).background(V5Blue),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(network, color = Color.White, fontSize = if (layout.tiny) 30.sp else 34.sp, fontWeight = FontWeight.Black)
                                Text(radioMode, color = Color(0xFFD8E8FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier.fillMaxWidth().height(if (layout.compact) 102.dp else 108.dp)
                            .clip(RoundedCornerShape(23.dp)).background(Color.White.copy(alpha = .94f))
                            .border(1.dp, Color(0xFFE5EDF6), RoundedCornerShape(23.dp)).padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        V5SignalStrength(rsrp, Modifier.weight(1f))
                        V5HeroDivider()
                        V5SignalQuality(rsrq, Modifier.weight(1f))
                        V5HeroDivider()
                        V5ConnectedBands(bands, Modifier.weight(1.38f))
                        V5HeroDivider()
                        V5MergeState(merged, summary, Modifier.weight(1.08f))
                    }
                }
            }
        }
    }
}

@Composable
private fun V5HeroDivider() {
    Box(Modifier.width(1.dp).height(76.dp).background(Color(0xFFE1E8F1)))
}

@Composable
private fun V5SignalStrength(rsrp: Double?, modifier: Modifier) {
    Column(modifier.padding(horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("▂▄▆█", color = if (rsrp == null) V5Muted else V5Green, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text("قوة الإشارة", color = V5Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(rsrp?.let { "${v5Fmt1(it)} dBm" } ?: "—", color = V5Ink, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        V5Progress(v5SignalProgress(rsrp))
    }
}

@Composable
private fun V5SignalQuality(rsrq: Double?, modifier: Modifier) {
    Column(modifier.padding(horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("∿", color = V5Blue, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("جودة الإشارة", color = V5Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(rsrq?.let { "RSRQ: ${v5Fmt0(it)} dB" } ?: "RSRQ: —", color = V5Ink, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        V5Progress(v5RsrqProgress(rsrq))
    }
}

@Composable
private fun V5ConnectedBands(bands: List<String>, modifier: Modifier) {
    Column(modifier.padding(horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("♜", color = V5Blue, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("الترددات المتصلة", color = V5Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        val shown = bands.take(2)
        if (shown.isEmpty()) {
            Text("—", color = V5Muted, fontSize = 9.sp)
        } else {
            shown.forEach { band ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(V5Green))
                    Spacer(Modifier.width(4.dp))
                    Text("$band  ${v5BandFrequency(band)}", color = V5Ink, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun V5MergeState(active: Boolean, summary: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔗", color = V5Blue, fontSize = 17.sp)
        Text("دمج الترددات", color = V5Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (active) V5SoftGreen else Color(0xFFF1F3F6))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(if (active) "مفعل" else "غير مفعل", color = if (active) V5Green else V5Muted, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Text(summary, color = V5Ink, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun V5Progress(progress: Float) {
    Box(Modifier.fillMaxWidth(.72f).height(7.dp).clip(CircleShape).background(Color(0xFFE3E8EF))) {
        Box(Modifier.fillMaxWidth(progress).height(7.dp).clip(CircleShape).background(V5Green))
    }
}

@Composable
private fun V5HomeGrid(
    layout: V5Layout,
    snapshot: RouterSnapshot,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    controlBusy: Boolean,
    scanBusy: Boolean,
    towerTarget: TowerTarget?,
    onSpeedTest: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onScanCells: () -> Unit
) {
    if (layout.tiny) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            V5MapCard(snapshot, towerTarget, scanBusy, onScanCells, Modifier.fillMaxWidth().height(370.dp))
            V5SpeedCard(performance, speedBusy, onSpeedTest, Modifier.fillMaxWidth().height(210.dp))
            V5NetworkModes(snapshot, controlBusy, onSetNetworkMode, Modifier.fillMaxWidth())
        }
        return
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.fillMaxWidth().height(layout.gridHeight.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            V5MapCard(snapshot, towerTarget, scanBusy, onScanCells, Modifier.weight(1.03f).fillMaxHeight())
            Column(Modifier.weight(.97f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                V5SpeedCard(performance, speedBusy, onSpeedTest, Modifier.fillMaxWidth().height(layout.speedHeight.dp))
                V5NetworkModes(snapshot, controlBusy, onSetNetworkMode, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun V5MapCard(
    snapshot: RouterSnapshot,
    target: TowerTarget?,
    scanBusy: Boolean,
    onScanCells: () -> Unit,
    modifier: Modifier
) {
    val bands = v5ActiveBands(snapshot).take(2).joinToString(" + ").ifBlank { snapshot.lteBand?.uppercase().orEmpty().ifBlank { "—" } }
    val identityReady = snapshot.pci != null && snapshot.earfcn != null

    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(24.dp)).clickable(enabled = !scanBusy, onClick = onScanCells),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, V5Border)
    ) {
        Column(Modifier.fillMaxSize().padding(11.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("⌖", color = V5Blue, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("الخريطة", color = V5Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF4F7F2))) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0xFFF6F8F5))
                    drawCircle(Color(0xFFE5F2E1), size.width * .22f, Offset(size.width * .12f, size.height * .16f))
                    drawCircle(Color(0xFFE5F2E1), size.width * .25f, Offset(size.width * .88f, size.height * .24f))
                    drawCircle(Color(0xFFD2EBFF), size.width * .20f, Offset(size.width * .91f, size.height * .91f))
                    val road = Color.White
                    val sw = size.width * .018f
                    drawLine(road, Offset(0f, size.height * .22f), Offset(size.width, size.height * .04f), sw)
                    drawLine(road, Offset(0f, size.height * .52f), Offset(size.width, size.height * .38f), sw)
                    drawLine(road, Offset(size.width * .06f, size.height), Offset(size.width * .47f, 0f), sw)
                    drawLine(road, Offset(size.width * .48f, size.height), Offset(size.width * .72f, 0f), sw)
                    drawLine(road, Offset(0f, size.height * .79f), Offset(size.width, size.height * .63f), sw)
                    drawLine(road, Offset(size.width * .20f, 0f), Offset(size.width * .98f, size.height), sw)
                    val tower = Offset(size.width * .62f, size.height * .32f)
                    val user = Offset(size.width * .23f, size.height * .78f)
                    drawLine(
                        V5Blue,
                        start = user,
                        end = tower,
                        strokeWidth = size.width * .012f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(17f, 13f), 0f)
                    )
                    drawCircle(Color(0x33E14C4C), size.width * .11f, tower)
                    drawCircle(Color(0x66E14C4C), size.width * .075f, tower)
                    drawCircle(V5Red, size.width * .030f, tower)
                    drawCircle(Color(0x331073F6), size.width * .11f, user)
                    drawCircle(Color(0x661073F6), size.width * .075f, user)
                    drawCircle(V5Blue, size.width * .032f, user)
                    drawCircle(Color.White, size.width * .019f, user)
                }

                Column(
                    Modifier.align(Alignment.TopCenter).padding(top = 46.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = .96f)).padding(horizontal = 9.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("البرج المتصل", color = V5Ink, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(bands, color = V5Ink, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    if (snapshot.pci != null) Text("PCI ${snapshot.pci}", color = V5Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                Text("♜", color = V5Red, fontSize = 26.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 112.dp))
                Text("●", color = V5Blue, fontSize = 24.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 30.dp, bottom = 58.dp))
                Text(
                    "موقعك الحالي",
                    color = V5BlueDeep,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 23.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .96f)).padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(V5SoftGray).padding(horizontal = 9.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("⌖", color = if (identityReady) V5Blue else V5Muted, fontSize = 15.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    when {
                        scanBusy -> "جاري مسح الخلايا…"
                        target != null -> "هوية البرج مثبتة"
                        identityReady -> "هوية الخلية مؤكدة"
                        else -> "الموقع الجغرافي غير مؤكد"
                    },
                    color = if (identityReady) V5Ink else V5Muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun V5SpeedCard(
    performance: NetworkPerformance?,
    busy: Boolean,
    onSpeedTest: () -> Unit,
    modifier: Modifier
) {
    val value = performance?.downloadMbps
    val display = value?.let(::v5Fmt1) ?: "—"
    val progress = ((value ?: 0.0) / 300.0).coerceIn(0.0, 1.0).toFloat()

    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(24.dp)).clickable(enabled = !busy, onClick = onSpeedTest),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, V5Border)
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("◉", color = V5Blue, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text("السرعة", color = V5Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                    val stroke = size.width * .035f
                    drawArc(Color(0xFFE1E7EF), 150f, 240f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
                    if (progress > 0f) drawArc(V5Blue, 150f, 240f * progress, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
                    val center = Offset(size.width / 2f, size.height * .68f)
                    val radius = size.width * .23f
                    val angle = (150f + 240f * progress) * (PI / 180.0)
                    val end = Offset(center.x + (cos(angle) * radius).toFloat(), center.y + (sin(angle) * radius).toFloat())
                    drawLine(V5Ink, center, end, strokeWidth = size.width * .010f, cap = StrokeCap.Round)
                    drawCircle(V5Ink, size.width * .018f, center)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 9.dp)) {
                    Text(display, color = V5Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("Mb/s", color = V5Muted, fontSize = 13.sp)
                }
            }
            Text(if (busy) "جاري القياس…" else "اضغط للقياس", color = if (busy) V5Muted else V5Blue, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(5.dp))
        }
    }
}

@Composable
private fun V5NetworkModes(
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
        modifier = modifier.shadow(3.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, V5Border)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("⚙", color = V5Blue, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Text("أوضاع الشبكة", color = V5Ink, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
            modes.forEachIndexed { index, (label, mode) ->
                val selected = current.equals(mode, true)
                Row(
                    Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(13.dp))
                        .background(if (selected) V5SoftBlue else Color.White)
                        .border(1.dp, if (selected) Color(0xFF8DC1FF) else Color(0xFFE4E9F0), RoundedCornerShape(13.dp))
                        .clickable(enabled = !busy) { onSetNetworkMode(mode) }.padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = if (selected) V5Blue else V5Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.End, maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(15.dp).clip(CircleShape).border(1.dp, if (selected) V5Blue else V5Muted, CircleShape), contentAlignment = Alignment.Center) {
                        if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(V5Blue))
                    }
                }
                if (index != modes.lastIndex) Spacer(Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun V5NetworkDetail(snapshot: RouterSnapshot) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val sinr = if (nr) snapshot.nrSinr else snapshot.lteSinr
    V5CardContainer {
        Column(Modifier.padding(18.dp)) {
            Text("الشبكة", color = V5Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5ValueBox("RSRP", rsrp?.let(::v5Fmt1) ?: "—", "dBm", Modifier.weight(1f))
                V5ValueBox("SINR", sinr?.let(::v5Fmt1) ?: "—", "dB", Modifier.weight(1f))
                V5ValueBox("RSRQ", snapshot.lteRsrq?.let(::v5Fmt1) ?: "—", "dB", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V5NetworkModePanel(snapshot: RouterSnapshot, busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    V5NetworkModes(snapshot, busy, onSetNetworkMode, Modifier.fillMaxWidth())
}

@Composable
private fun V5TrafficPanel(traffic: TrafficTelemetry?) {
    V5CardContainer {
        Column(Modifier.padding(18.dp)) {
            Text("حركة البيانات", color = V5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V5ValueBox("تنزيل", TrafficTelemetryFormatter.rateMbps(traffic?.rxBytesPerSecond), "Mb/s", Modifier.weight(1f))
                V5ValueBox("رفع", TrafficTelemetryFormatter.rateMbps(traffic?.txBytesPerSecond), "Mb/s", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V5ThermalPanel(thermal: ThermalTelemetry) {
    V5CardContainer {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("الحرارة", color = V5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(thermal.highestObserved?.let { ThermalTelemetryFormatter.celsius(it.celsius) } ?: "—", color = V5Blue, fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V5TowerPanel(
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
    V5CardContainer {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الأبراج", color = V5Ink, fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                V5SmallAction(if (scanBusy) "جاري…" else "مسح الخلايا", !scanBusy && !busy, onScan)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5ValueBox("PCI", snapshot.pci?.toString() ?: "—", "", Modifier.weight(1f))
                V5ValueBox("EARFCN", snapshot.earfcn?.toString() ?: "—", "", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5ActionButton("تثبيت الحالية", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
                V5OutlineButton("إزالة القفل", !busy, Modifier.weight(1f), onClear)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حارس البرج", color = V5Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    target?.let { Text("PCI ${it.pci} • EARFCN ${it.earfcn}", color = V5Muted, fontSize = 10.sp) }
                }
                Switch(checked = guardEnabled, onCheckedChange = onGuardChange, enabled = target != null && !busy, colors = SwitchDefaults.colors(checkedTrackColor = V5Green))
            }
            guardStatus?.let {
                Text(
                    if (it.match == TowerMatch.MATCHED) "مطابق" else v5CompactMessage(it.message),
                    color = if (it.match == TowerMatch.MATCHED) V5Green else V5Muted,
                    fontSize = 11.sp
                )
            }
            if (cells.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                cells.take(12).forEachIndexed { index, cell ->
                    V5CellRow(index + 1, cell, snapshot, busy, onLockCell)
                    if (index != cells.take(12).lastIndex) Spacer(Modifier.height(7.dp))
                }
            }
        }
    }
}

@Composable
private fun V5CellRow(index: Int, cell: NearbyCell, snapshot: RouterSnapshot, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val current = cell.rat == "LTE" && cell.pci == snapshot.pci && cell.arfcn == snapshot.earfcn
    val lockable = cell.rat == "LTE" && cell.pci != null && cell.arfcn != null
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(if (current) V5SoftGreen else V5SoftGray).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(index.toString(), color = V5Blue, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(23.dp))
        Column(Modifier.weight(1f)) {
            Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = V5Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("EARFCN ${cell.arfcn ?: "—"} • ${cell.rsrp?.let { "${v5Fmt0(it)} dBm" } ?: "—"}", color = V5Muted, fontSize = 10.sp)
        }
        when {
            current -> V5Pill("الحالية", V5Green)
            lockable -> Text("تثبيت", color = if (busy) V5Muted else V5Blue, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(enabled = !busy) { onLock(cell) }.padding(8.dp))
        }
    }
}

@Composable
private fun V5BandsPanel(
    layout: V5Layout,
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
        snapshot.cells.filter { if (showNr) it.role == CellRole.NR else it.role != CellRole.NR }.mapNotNull { v5BandNumber(it.band) }.toSet()
    }

    V5CardContainer {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الترددات", color = V5Ink, fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    V5Choice("4G", !showNr) { showNr = false }
                    V5Choice("5G", showNr) { showNr = true }
                }
            }
            Spacer(Modifier.height(12.dp))
            bands.chunked(layout.bandColumns).forEach { chunk ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunk.forEach { band ->
                        val chosen = band in selected
                        val live = band in active
                        Box(
                            Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(15.dp))
                                .background(if (live) V5SoftGreen else if (chosen) V5SoftBlue else V5SoftGray)
                                .border(1.dp, if (live) V5Green else if (chosen) V5Blue else V5Border, RoundedCornerShape(15.dp))
                                .clickable(enabled = !busy) { if (showNr) onNrToggle(band) else onLteToggle(band) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${if (showNr) "N" else "B"}$band", color = V5Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    repeat(layout.bandColumns - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
            V5ActionButton(
                if (showNr) "تطبيق 5G" else "تطبيق 4G",
                selected.isNotEmpty() && !busy && (!showNr || capabilities.supportsNrBandLock),
                Modifier.fillMaxWidth()
            ) { if (showNr) onApplyNr() else onApplyLte() }
        }
    }
}

@Composable
private fun V5ToolsPanel(
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        V5CardContainer {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("التحسين الذكي", color = V5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        smartReport?.let { Text("آخر نتيجة: ${it.best.qualityScore}/100", color = V5Muted, fontSize = 10.sp) }
                    }
                    Switch(checked = smartMode, onCheckedChange = onSmartModeChange, colors = SwitchDefaults.colors(checkedTrackColor = V5Green))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OptimizationGoal.entries.forEach { goal -> V5Choice(v5Goal(goal), smartGoal == goal) { onSmartGoalChange(goal) } }
                }
                Spacer(Modifier.height(10.dp))
                V5ActionButton(if (smartBusy) "جاري…" else "تحسين الآن", !smartBusy && !busy, Modifier.fillMaxWidth(), onOptimizeNow)
            }
        }

        V5CardContainer {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("أفضل مكان", color = V5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    placementReading?.let { Text("${it.score.total}/100", color = V5Blue, fontSize = 18.sp, fontWeight = FontWeight.Black) }
                }
                Switch(checked = placementMode, onCheckedChange = { onPlacementToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = V5Green))
            }
        }

        V5CardContainer {
            Column(Modifier.padding(18.dp)) {
                Text("التشخيص", color = V5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(7.dp))
                val ready = runtime?.let {
                    listOf(it.lteBandControl, it.nrBandControl, it.cellLock, it.networkMode, it.neighborScan, it.antennaControl)
                        .count { capability -> capability.state == RuntimeCapabilityState.SAFE_TO_ATTEMPT }
                }
                Text(ready?.let { "$it وظائف جاهزة" } ?: "غير جاهز", color = if (ready != null) V5Green else V5Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(stability.summary, color = V5Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V5OutlineButton("نسخ", true, Modifier.weight(1f), onCopyDiagnostics)
                    V5OutlineButton("مشاركة", true, Modifier.weight(1f), onShareDiagnostics)
                }
            }
        }

        V5CardContainer {
            Column(Modifier.padding(18.dp)) {
                Text("الاستعادة", color = V5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(9.dp))
                V5ActionButton("استعادة النسخة", backupAvailable && !busy, Modifier.fillMaxWidth(), onRestore)
            }
        }

        if (supportsAntenna) {
            V5CardContainer {
                Column(Modifier.padding(18.dp)) {
                    Text("الهوائي", color = V5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { state -> V5OutlineButton(state.toString(), !busy, Modifier.weight(1f)) { onAntennaState(state) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun V5ValueBox(title: String, value: String, unit: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(V5SoftGray).padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = V5Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = V5Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
        if (unit.isNotBlank()) Text(unit, color = V5Muted, fontSize = 9.sp)
    }
}

@Composable
private fun V5ActionButton(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(15.dp)).background(if (enabled) V5Blue else Color(0xFFE6EAF0))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.White else V5Muted, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun V5OutlineButton(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(15.dp)).background(Color.White)
            .border(1.dp, V5Border, RoundedCornerShape(15.dp)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) V5Ink else V5Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun V5SmallAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(V5SoftBlue).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(text, color = if (enabled) V5Blue else V5Muted, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V5Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (selected) V5Blue else V5SoftGray).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp)) {
        Text(text, color = if (selected) Color.White else V5Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V5Pill(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .12f)).padding(horizontal = 9.dp, vertical = 6.dp)) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V5Message(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(V5SoftBlue).padding(horizontal = 13.dp, vertical = 9.dp)) {
        Text(text, color = V5Ink, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V5CardContainer(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(23.dp)),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = V5Card),
        border = BorderStroke(1.dp, V5Border)
    ) { content() }
}

@Composable
private fun V5BottomNav(selected: V5Section, onSelect: (V5Section) -> Unit, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier.fillMaxWidth().navigationBarsPadding().height(82.dp)
                .shadow(12.dp, RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp))
                .clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp)).background(Color.White)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V5Nav("☰", "المزيد", selected == V5Section.BANDS, Modifier.weight(1f)) { onSelect(V5Section.BANDS) }
            V5Nav("▣", "السجلات", selected == V5Section.TOWERS, Modifier.weight(1f)) { onSelect(V5Section.TOWERS) }
            V5Nav("⚒", "الأدوات", selected == V5Section.TOOLS, Modifier.weight(1f)) { onSelect(V5Section.TOOLS) }
            V5Nav("▂▄▆█", "الشبكة", selected == V5Section.NETWORK, Modifier.weight(1f)) { onSelect(V5Section.NETWORK) }
            V5Nav("⌂", "الرئيسية", selected == V5Section.HOME, Modifier.weight(1f)) { onSelect(V5Section.HOME) }
        }
    }
}

@Composable
private fun V5Nav(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected) V5Blue else V5Muted, fontSize = if (icon.length > 2) 13.sp else 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (selected) V5Blue else V5Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, maxLines = 1)
    }
}

private fun v5CompactMessage(message: String): String {
    val value = message.trim()
    if (value.isBlank()) return ""
    val first = value.substringBefore(" • ").substringBefore("؛").substringBefore(" (").trim()
    return if (first.length > 90) first.take(87) + "…" else first
}

private fun v5OperatorLabel(snapshot: RouterSnapshot): String = when (snapshot.operatorCode?.trim()) {
    "42001" -> "SA - STC"
    "42003" -> "SA - Mobily"
    "42004" -> "SA - Zain"
    else -> snapshot.operatorCode?.takeIf { it.isNotBlank() } ?: "الشبكة"
}

private fun v5ActiveBands(snapshot: RouterSnapshot): List<String> {
    val fromCells = snapshot.cells.filter { it.role != CellRole.NR }.mapNotNull { it.band?.trim()?.uppercase() }.distinct()
    if (fromCells.isNotEmpty()) return fromCells
    return listOfNotNull(snapshot.lteBand?.trim()?.uppercase()).distinct()
}

private fun v5BandFrequency(band: String): String = when (v5BandNumber(band)) {
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

private fun v5SignalProgress(rsrp: Double?): Float = when {
    rsrp == null -> 0f
    rsrp >= -80 -> 1f
    rsrp <= -120 -> .08f
    else -> ((rsrp + 120.0) / 40.0).toFloat().coerceIn(.08f, 1f)
}

private fun v5RsrqProgress(rsrq: Double?): Float = when {
    rsrq == null -> 0f
    rsrq >= -7 -> 1f
    rsrq <= -20 -> .08f
    else -> ((rsrq + 20.0) / 13.0).toFloat().coerceIn(.08f, 1f)
}

private fun v5Quality(rsrp: Double?): String = when {
    rsrp == null -> "غير معروفة"
    rsrp >= -85 -> "ممتازة"
    rsrp >= -95 -> "جيدة"
    rsrp >= -105 -> "متوسطة"
    else -> "ضعيفة"
}

private fun v5Goal(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun v5BandNumber(value: String?): Int? = value?.trim()?.removePrefix("n")?.removePrefix("N")?.removePrefix("b")?.removePrefix("B")?.toIntOrNull()
private fun v5Fmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
private fun v5Fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
