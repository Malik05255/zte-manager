package com.malik.ztesmartmanager

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityEvidence
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState
import com.malik.ztesmartmanager.core.presentation.HaiLayoutSpec
import com.malik.ztesmartmanager.core.presentation.HaiResponsivePolicy
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.CellConfidence
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerMatch
import com.malik.ztesmartmanager.core.tower.TowerRecommendationDecision
import com.malik.ztesmartmanager.core.tower.TowerRecommendationEngine
import com.malik.ztesmartmanager.core.tower.TowerTarget
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val HaiBackground = Color(0xFFF8FAFE)
private val HaiCard = Color(0xFFFFFFFF)
private val HaiInk = Color(0xFF09245D)
private val HaiMuted = Color(0xFF657397)
private val HaiBlue = Color(0xFF1665F5)
private val HaiBlueDeep = Color(0xFF073C9D)
private val HaiCyan = Color(0xFF13C9EE)
private val HaiMint = Color(0xFF17DCA3)
private val HaiGreen = Color(0xFF13C966)
private val HaiRed = Color(0xFFE3515C)
private val HaiBorder = Color(0xFFE8EDF6)
private val HaiSoftBlue = Color(0xFFF0F5FF)
private val HaiSoftMint = Color(0xFFECFBF5)
private val HaiPurple = Color(0xFF9A50E8)

private enum class HaiSection { HOME, NETWORK, TOWERS, BANDS, TOOLS }
private enum class HaiBandFilter { ALL, NR, LTE }

private data class HaiBandRow(
    val nr: Boolean,
    val band: Int,
    val active: Boolean,
    val selected: Boolean,
    val arfcn: Int?,
    val bandwidthMhz: Double?
)

@Composable
fun HaiAdaptiveDashboard(
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
    var section by rememberSaveable { mutableStateOf(HaiSection.HOME) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HaiBackground)
    ) {
        val widthDp = maxWidth.value.roundToInt().coerceAtLeast(1)
        val heightDp = maxHeight.value.roundToInt().coerceAtLeast(1)
        val spec = remember(widthDp, heightDp) { HaiResponsivePolicy.resolve(widthDp, heightDp) }

        Column(Modifier.fillMaxSize()) {
            HaiHeader(
                spec = spec,
                connected = snapshot != null,
                status = status,
                onDisconnect = onDisconnect
            )
            HaiTopTabs(
                spec = spec,
                selected = section,
                connected = snapshot != null,
                onSelect = { section = it }
            )

            if (snapshot == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    HaiCardShell(spec = spec, modifier = Modifier.padding(spec.horizontalPaddingDp.dp)) {
                        Column(
                            Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("HAI", color = HaiBlueDeep, fontSize = hs(spec, 34), fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(8.dp))
                            Text("Reading and verifying the router…", color = HaiMuted, fontSize = hs(spec, 12))
                        }
                    }
                }
                HaiBottomNav(spec, section, onSelect = { section = it })
                return@Column
            }

            Box(Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spec.horizontalPaddingDp.dp,
                        end = spec.horizontalPaddingDp.dp,
                        top = spec.sectionGapDp.dp,
                        bottom = (spec.bottomBarHeightDp + 20).dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)
                ) {
                    if (operationMessage.isNotBlank()) {
                        item { HaiOperationStrip(spec, operationMessage) }
                    }

                    when (section) {
                        HaiSection.HOME -> {
                            item {
                                HaiTopDashboardRow(
                                    spec = spec,
                                    snapshot = snapshot,
                                    performance = lastPerformance,
                                    speedBusy = speedBusy,
                                    onSpeedTest = onSpeedTest
                                )
                            }
                            item {
                                HaiHomeBody(
                                    spec = spec,
                                    snapshot = snapshot,
                                    capabilities = capabilities,
                                    selectedLte = selectedLte,
                                    selectedNr = selectedNr,
                                    nearbyCells = nearbyCells,
                                    telemetrySamples = telemetrySamples,
                                    onSection = { section = it },
                                    onOptimizeNow = onOptimizeNow,
                                    onLteToggle = onLteToggle,
                                    onNrToggle = onNrToggle,
                                    onApplyLte = onApplyLte,
                                    onApplyNr = onApplyNr,
                                    onScanCells = onScanCells
                                )
                            }
                        }
                        HaiSection.NETWORK -> {
                            item {
                                HaiTopDashboardRow(spec, snapshot, lastPerformance, speedBusy, onSpeedTest)
                            }
                            item {
                                HaiNetworkToolsCard(
                                    spec = spec,
                                    snapshot = snapshot,
                                    busy = controlBusy,
                                    traffic = traffic,
                                    thermal = thermal,
                                    onSetNetworkMode = onSetNetworkMode
                                )
                            }
                            item { HaiActiveConnectionsCard(spec, snapshot, Modifier.fillMaxWidth()) }
                        }
                        HaiSection.TOWERS -> {
                            item {
                                HaiTowerMapCard(spec, snapshot, nearbyCells, scanBusy, onScanCells, Modifier.fillMaxWidth())
                            }
                            item {
                                HaiTowerControlCard(
                                    spec = spec,
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
                        HaiSection.BANDS -> {
                            item {
                                HaiBandsCard(
                                    spec = spec,
                                    snapshot = snapshot,
                                    capabilities = capabilities,
                                    selectedLte = selectedLte,
                                    selectedNr = selectedNr,
                                    onLteToggle = onLteToggle,
                                    onNrToggle = onNrToggle,
                                    onApplyLte = onApplyLte,
                                    onApplyNr = onApplyNr,
                                    busy = controlBusy,
                                    modifier = Modifier.fillMaxWidth(),
                                    expanded = true
                                )
                            }
                        }
                        HaiSection.TOOLS -> {
                            item {
                                HaiToolsScreen(
                                    spec = spec,
                                    runtime = runtime,
                                    stability = stability,
                                    thermal = thermal,
                                    placementMode = placementMode,
                                    placementReading = placementReading,
                                    smartMode = smartMode,
                                    smartGoal = smartGoal,
                                    smartBusy = smartBusy,
                                    smartReport = smartReport,
                                    backupAvailable = safetyBackupAvailable,
                                    capabilities = capabilities,
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
                HaiBottomNav(
                    spec = spec,
                    selected = section,
                    onSelect = { section = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun HaiHeader(spec: HaiLayoutSpec, connected: Boolean, status: String, onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = spec.horizontalPaddingDp.dp,
                vertical = if (spec.denseHeader) 5.dp else 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HaiHeaderAction("☰") {}
        Spacer(Modifier.width(if (spec.denseHeader) 8.dp else 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("H", color = HaiBlue, fontSize = hs(spec, 34), fontWeight = FontWeight.Black)
            Text("AI", color = HaiBlueDeep, fontSize = hs(spec, 34), fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.weight(1f))
        HaiHeaderAction("⌕") {}
        Spacer(Modifier.width(4.dp))
        HaiHeaderAction("◌") {}
        Spacer(Modifier.width(4.dp))
        HaiHeaderAction("⚙") {}
    }
}

@Composable
private fun HaiHeaderAction(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, color = HaiInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HaiTopTabs(
    spec: HaiLayoutSpec,
    selected: HaiSection,
    connected: Boolean,
    onSelect: (HaiSection) -> Unit
) {
    if (spec.twoColumn) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spec.horizontalPaddingDp.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HaiTopTab("⌂", "Home", selected == HaiSection.HOME) { onSelect(HaiSection.HOME) }
                HaiTopTab("⌁", "Network", selected == HaiSection.NETWORK) { onSelect(HaiSection.NETWORK) }
                HaiTopTab("♜", "Towers", selected == HaiSection.TOWERS) { onSelect(HaiSection.TOWERS) }
                HaiTopTab("▦", "Bands", selected == HaiSection.BANDS) { onSelect(HaiSection.BANDS) }
                HaiTopTab("⚒", "Tools", selected == HaiSection.TOOLS) { onSelect(HaiSection.TOOLS) }
            }
            Spacer(Modifier.width(8.dp))
            HaiConnectedPill(connected)
        }
    } else {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = spec.horizontalPaddingDp.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HaiTopTab("⌂", "Home", selected == HaiSection.HOME) { onSelect(HaiSection.HOME) }
                HaiTopTab("⌁", "Network", selected == HaiSection.NETWORK) { onSelect(HaiSection.NETWORK) }
                HaiTopTab("♜", "Towers", selected == HaiSection.TOWERS) { onSelect(HaiSection.TOWERS) }
                HaiTopTab("▦", "Bands", selected == HaiSection.BANDS) { onSelect(HaiSection.BANDS) }
                HaiTopTab("⚒", "Tools", selected == HaiSection.TOOLS) { onSelect(HaiSection.TOOLS) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = spec.horizontalPaddingDp.dp, vertical = 5.dp)) {
                Spacer(Modifier.weight(1f))
                HaiConnectedPill(connected)
            }
        }
    }
}

@Composable
private fun HaiTopTab(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF3DBCF5), HaiBlue))
    Row(
        modifier = Modifier
            .clip(shape)
            .then(if (selected) Modifier.background(gradient) else Modifier.background(HaiCard))
            .border(1.dp, if (selected) Color.Transparent else HaiBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = if (selected) Color.White else HaiInk, fontSize = 12.sp)
        Spacer(Modifier.width(5.dp))
        Text(label, color = if (selected) Color.White else HaiInk, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HaiConnectedPill(connected: Boolean) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(HaiCard)
            .border(1.dp, HaiBorder, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(if (connected) HaiGreen else HaiRed))
        Spacer(Modifier.width(7.dp))
        Text(if (connected) "Connected" else "Offline", color = HaiInk, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Text("›", color = HaiInk, fontSize = 16.sp)
    }
}

@Composable
private fun HaiTopDashboardRow(
    spec: HaiLayoutSpec,
    snapshot: RouterSnapshot,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    onSpeedTest: () -> Unit
) {
    if (spec.twoColumn) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)) {
            HaiNetworkStatusCard(spec, snapshot, Modifier.weight(1.58f))
            HaiSpeedTestCard(spec, performance, speedBusy, onSpeedTest, Modifier.weight(0.92f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)) {
            HaiNetworkStatusCard(spec, snapshot, Modifier.fillMaxWidth())
            HaiSpeedTestCard(spec, performance, speedBusy, onSpeedTest, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HaiNetworkStatusCard(spec: HaiLayoutSpec, snapshot: RouterSnapshot, modifier: Modifier) {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val network = snapshot.networkType ?: "—"
    val mainLabel = when {
        nrVerified -> "5G"
        lteVerified -> "4G"
        else -> "—"
    }
    val subLabel = when {
        network.contains("SA", true) && !network.contains("NSA", true) -> "SA (5G)"
        nrVerified && (network.contains("NSA", true) || network.contains("ENDC", true) || network.contains("EN-DC", true)) -> "NSA (5G/4G)"
        lteVerified -> "4G LTE"
        else -> "Unverified"
    }
    val mainRsrp = if (nrVerified) snapshot.nrRsrp else snapshot.lteRsrp
    val mainSinr = if (nrVerified) snapshot.nrSinr else snapshot.lteSinr
    val quality = signalQuality(mainRsrp)

    val shape = RoundedCornerShape(spec.cardRadiusDp.dp)
    Card(
        modifier = modifier
            .height(spec.networkCardHeightDp.dp)
            .shadow(9.dp, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F9FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
    ) {
        Box(Modifier.fillMaxSize()) {
            HaiNetworkBackdrop(Modifier.fillMaxSize())
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⌂", color = HaiInk, fontSize = hs(spec, 13))
                    Spacer(Modifier.width(7.dp))
                    Text("Network Status", color = HaiInk, fontSize = hs(spec, 13), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(mainLabel, color = HaiInk, fontSize = hs(spec, 48), fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    HaiSignalBars(level = signalBars(mainRsrp), modifier = Modifier.padding(bottom = 9.dp))
                }
                Text(subLabel, color = HaiInk, fontSize = hs(spec, 15), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (quality.first) Color(0xFF10B872) else Color(0xFFE6A23C))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(quality.second, color = Color.White, fontSize = hs(spec, 9), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (snapshot.networkType != null) HaiGreen else HaiRed))
                    Spacer(Modifier.width(5.dp))
                    Text(if (snapshot.networkType != null) "Verified live radio" else "Not verified", color = HaiInk, fontSize = hs(spec, 9))
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HaiInlineMetric("RSRP", mainRsrp, "dBm", spec)
                    HaiDivider()
                    HaiInlineMetric("SINR", mainSinr, "dB", spec)
                    HaiDivider()
                    HaiInlineMetric("RSRQ", snapshot.lteRsrq, "dB", spec)
                }
            }
        }
    }
}

@Composable
private fun HaiNetworkBackdrop(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFFF4FAFF), Color(0xFFE8F4FF), Color(0xFFDFECF8))))

        val mountain = Path().apply {
            moveTo(0f, h * 0.57f)
            lineTo(w * 0.18f, h * 0.52f)
            lineTo(w * 0.28f, h * 0.59f)
            lineTo(w * 0.43f, h * 0.45f)
            lineTo(w * 0.52f, h * 0.54f)
            lineTo(w * 0.67f, h * 0.39f)
            lineTo(w * 0.82f, h * 0.55f)
            lineTo(w, h * 0.47f)
            lineTo(w, h * 0.76f)
            lineTo(0f, h * 0.76f)
            close()
        }
        drawPath(mountain, Color(0xFFBBD7F2).copy(alpha = 0.65f))
        val skylineY = h * 0.74f
        repeat(11) { i ->
            val x = w * (0.08f + i * 0.075f)
            val bh = h * (0.035f + (i % 4) * 0.012f)
            drawRect(Color(0xFFAFC8DF).copy(alpha = 0.35f), topLeft = Offset(x, skylineY - bh), size = Size(w * 0.035f, bh))
        }

        val tx = w * 0.82f
        val baseY = h * 0.78f
        val topY = h * 0.25f
        drawLine(Color(0xFF6A788B), Offset(tx, baseY), Offset(tx, topY), 3f)
        drawLine(Color(0xFF7B8794), Offset(tx - 18f, baseY), Offset(tx, topY + 22f), 2f)
        drawLine(Color(0xFF7B8794), Offset(tx + 18f, baseY), Offset(tx, topY + 22f), 2f)
        repeat(3) { i ->
            val r = 20f + i * 14f
            drawCircle(Color.White.copy(alpha = 0.72f), radius = r, center = Offset(tx, topY + 5f), style = Stroke(1.6f))
        }
    }
}

@Composable
private fun HaiSignalBars(level: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        (1..4).forEach { i ->
            Box(
                Modifier
                    .width(7.dp)
                    .height((10 + i * 5).dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (i <= level) if (i >= 3) HaiGreen else HaiCyan else Color(0xFFC9D5E7))
            )
        }
    }
}

@Composable
private fun HaiInlineMetric(label: String, value: Double?, unit: String, spec: HaiLayoutSpec) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = HaiInk, fontSize = hs(spec, 8), fontWeight = FontWeight.SemiBold)
        Text(
            value?.let(::fmt1) ?: "—",
            color = Color(0xFF10182B),
            fontSize = hs(spec, 14),
            fontWeight = FontWeight.Bold
        )
        Text(unit, color = HaiMuted, fontSize = hs(spec, 8))
    }
}

@Composable
private fun HaiDivider() {
    Box(Modifier.width(1.dp).height(36.dp).background(Color(0xFFD7DEEA)))
}

@Composable
private fun HaiSpeedTestCard(
    spec: HaiLayoutSpec,
    performance: NetworkPerformance?,
    busy: Boolean,
    onSpeedTest: () -> Unit,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(spec.cardRadiusDp.dp)
    val speed = performance?.downloadMbps
    val gaugeFraction = ((speed ?: 0.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
    HaiCardShell(
        spec = spec,
        modifier = modifier.height(spec.speedCardHeightDp.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text("Speed Test", color = HaiInk, fontSize = hs(spec, 13), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(150.dp)) {
                    val stroke = 14f
                    val arcSize = Size(size.width * 0.84f, size.height * 0.84f)
                    val topLeft = Offset(size.width * 0.08f, size.height * 0.12f)
                    drawArc(Color(0xFFDDE4EC), 160f, 220f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(
                        Brush.sweepGradient(listOf(HaiBlue, HaiCyan, HaiMint)),
                        160f,
                        220f * gaugeFraction,
                        false,
                        topLeft,
                        arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(speed?.let(::fmt0) ?: "—", color = HaiInk, fontSize = hs(spec, 30), fontWeight = FontWeight.Black)
                    Text("Mb/s", color = HaiInk, fontSize = hs(spec, 11), fontWeight = FontWeight.Medium)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                HaiSpeedMini("↓", speed?.let(::fmt1) ?: "—", "Download")
                HaiSpeedMini("◷", performance?.latencyMs?.let(::fmt1) ?: "—", "ms latency")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSpeedTest,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(HaiBlue, Color(0xFF18D8C0))))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (busy) "Testing…" else "▶  Start Test", color = Color.White, fontSize = hs(spec, 11), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HaiSpeedMini(icon: String, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = if (icon == "↓") HaiBlue else HaiGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(value, color = HaiInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(label, color = HaiMuted, fontSize = 7.sp)
        }
    }
}

@Composable
private fun HaiHomeBody(
    spec: HaiLayoutSpec,
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    nearbyCells: List<NearbyCell>,
    telemetrySamples: List<SafeTelemetrySample>,
    onSection: (HaiSection) -> Unit,
    onOptimizeNow: () -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    onScanCells: () -> Unit
) {
    if (spec.twoColumn) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1.58f), verticalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)) {
                HaiQuickActions(spec, onSection, onOptimizeNow)
                HaiBandsCard(
                    spec, snapshot, capabilities, selectedLte, selectedNr,
                    onLteToggle, onNrToggle, onApplyLte, onApplyNr,
                    busy = false,
                    modifier = Modifier.fillMaxWidth(),
                    expanded = false
                )
            }
            Column(Modifier.weight(0.92f), verticalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)) {
                HaiTowerMapCard(spec, snapshot, nearbyCells, false, onScanCells, Modifier.fillMaxWidth())
                HaiSignalMonitorCard(spec, snapshot, telemetrySamples, Modifier.fillMaxWidth())
                HaiActiveConnectionsCard(spec, snapshot, Modifier.fillMaxWidth())
                HaiQuickInfoCard(spec, snapshot, Modifier.fillMaxWidth())
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)) {
            HaiQuickActions(spec, onSection, onOptimizeNow)
            HaiBandsCard(
                spec, snapshot, capabilities, selectedLte, selectedNr,
                onLteToggle, onNrToggle, onApplyLte, onApplyNr,
                busy = false,
                modifier = Modifier.fillMaxWidth(),
                expanded = false
            )
            HaiTowerMapCard(spec, snapshot, nearbyCells, false, onScanCells, Modifier.fillMaxWidth())
            HaiSignalMonitorCard(spec, snapshot, telemetrySamples, Modifier.fillMaxWidth())
            HaiActiveConnectionsCard(spec, snapshot, Modifier.fillMaxWidth())
            HaiQuickInfoCard(spec, snapshot, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HaiQuickActions(spec: HaiLayoutSpec, onSection: (HaiSection) -> Unit, onOptimizeNow: () -> Unit) {
    HaiCardShell(spec, Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            HaiQuickAction("▣", "Lock Bands", Modifier.weight(1f)) { onSection(HaiSection.BANDS) }
            HaiQuickAction("≡", "Band Selector", Modifier.weight(1f)) { onSection(HaiSection.BANDS) }
            HaiQuickAction("⌁", "Network Mode", Modifier.weight(1f)) { onSection(HaiSection.NETWORK) }
            HaiQuickAction("◴", "Optimize", Modifier.weight(1f), onOptimizeNow)
            HaiQuickAction("⚒", "Diagnostics", Modifier.weight(1f)) { onSection(HaiSection.TOOLS) }
        }
    }
}

@Composable
private fun HaiQuickAction(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F8FD))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, color = HaiInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = HaiInk, fontSize = 7.sp, maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HaiBandsCard(
    spec: HaiLayoutSpec,
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    busy: Boolean,
    modifier: Modifier,
    expanded: Boolean
) {
    var filter by rememberSaveable { mutableStateOf(HaiBandFilter.ALL) }
    val rows = remember(snapshot.cells, capabilities, selectedLte, selectedNr) {
        buildBandRows(snapshot, capabilities, selectedLte, selectedNr)
    }
    val filtered = rows.filter {
        when (filter) {
            HaiBandFilter.ALL -> true
            HaiBandFilter.NR -> it.nr
            HaiBandFilter.LTE -> !it.nr
        }
    }
    val displayed = if (expanded) filtered else filtered.take(12)

    HaiCardShell(spec, modifier) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("↟", color = HaiInk, fontSize = 15.sp)
                Spacer(Modifier.width(5.dp))
                Text("All Frequencies (Bands)", color = HaiInk, fontSize = hs(spec, 11), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("☷", color = HaiBlue, fontSize = 14.sp)
            }
            Spacer(Modifier.height(7.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                HaiFilterChip("All", rows.size, filter == HaiBandFilter.ALL) { filter = HaiBandFilter.ALL }
                HaiFilterChip("5G", rows.count { it.nr }, filter == HaiBandFilter.NR) { filter = HaiBandFilter.NR }
                HaiFilterChip("4G", rows.count { !it.nr }, filter == HaiBandFilter.LTE) { filter = HaiBandFilter.LTE }
            }
            Spacer(Modifier.height(8.dp))
            HaiBandHeader()
            displayed.forEach { row ->
                HaiBandTableRow(row) {
                    if (row.nr) onNrToggle(row.band) else onLteToggle(row.band)
                }
            }
            if (!expanded && filtered.size > displayed.size) {
                Text(
                    "+${filtered.size - displayed.size} more bands • open Bands for all",
                    color = HaiBlue,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(top = 6.dp).clickable { },
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onApplyLte,
                    enabled = selectedLte.isNotEmpty() && !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = HaiBlue)
                ) { Text("Apply 4G + verify", fontSize = 8.sp, maxLines = 1) }
                Button(
                    onClick = onApplyNr,
                    enabled = selectedNr.isNotEmpty() && !busy && capabilities.supportsNrBandLock,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = HaiBlueDeep)
                ) { Text("Apply 5G + verify", fontSize = 8.sp, maxLines = 1) }
            }
            Text(
                "Switches are requested/selected bands. Active status is shown only from live verified carriers.",
                color = HaiMuted,
                fontSize = 7.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun HaiFilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Brush.horizontalGradient(listOf(Color(0xFF38B9F7), HaiBlue)) else Brush.linearGradient(listOf(Color(0xFFF3F6FB), Color(0xFFF3F6FB))))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (selected) Color.White else HaiInk, fontSize = 9.sp)
        Spacer(Modifier.width(5.dp))
        Box(
            Modifier
                .clip(CircleShape)
                .background(if (selected) Color.White.copy(alpha = 0.18f) else Color(0xFFE8EEF8))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(count.toString(), color = if (selected) Color.White else HaiBlue, fontSize = 7.sp)
        }
    }
}

@Composable
private fun HaiBandHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF6F8FC))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        HaiTableText("Status", 0.7f, header = true)
        HaiTableText("Band", 0.85f, header = true)
        HaiTableText("ARFCN", 1.15f, header = true)
        HaiTableText("Bandwidth", 1.2f, header = true)
        HaiTableText("Tech", 0.9f, header = true)
        HaiTableText("Select", 0.9f, header = true)
    }
}

@Composable
private fun HaiBandTableRow(row: HaiBandRow, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(0.5.dp, HaiBorder)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(0.7f), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (row.active) HaiGreen else Color(0xFF8AA0B8)))
        }
        Box(Modifier.weight(0.85f)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (row.nr) Color(0xFFE8D9FF) else Color(0xFFDDF4E9))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text("${if (row.nr) "N" else "B"}${row.band}", color = if (row.nr) HaiPurple else Color(0xFF187A57), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        HaiTableText(row.arfcn?.toString() ?: "—", 1.15f)
        HaiTableText(row.bandwidthMhz?.let { "${fmt0(it)} MHz" } ?: "—", 1.2f)
        HaiTableText(if (row.nr) "5G" else "4G", 0.9f, color = HaiBlue)
        Box(Modifier.weight(0.9f), contentAlignment = Alignment.Center) {
            Switch(
                checked = row.selected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(36.dp, 22.dp),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = HaiMint,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFD6DEE8),
                    uncheckedThumbColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun RowScope.HaiTableText(text: String, weight: Float, header: Boolean = false, color: Color = HaiInk) {
    Text(
        text = text,
        color = if (header) HaiMuted else color,
        fontSize = if (header) 6.5.sp else 7.5.sp,
        fontWeight = if (header) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier.weight(weight),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun HaiTowerMapCard(
    spec: HaiLayoutSpec,
    snapshot: RouterSnapshot,
    cells: List<NearbyCell>,
    scanBusy: Boolean,
    onScan: () -> Unit,
    modifier: Modifier
) {
    HaiCardShell(spec, modifier) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("⌾", color = HaiInk, fontSize = 13.sp)
                Spacer(Modifier.width(5.dp))
                Text("Nearby Cells", color = HaiInk, fontSize = hs(spec, 11), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    if (scanBusy) "Scanning…" else "View / Scan ›",
                    color = HaiBlue,
                    fontSize = 8.sp,
                    modifier = Modifier.clickable(enabled = !scanBusy, onClick = onScan)
                )
            }
            Spacer(Modifier.height(7.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(spec.mapCardHeightDp.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF3F7F3))
            ) {
                HaiMapBackdrop(cells, Modifier.fillMaxSize())
                val best = cells.firstOrNull()
                if (best != null) {
                    Column(
                        Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.96f))
                            .shadow(3.dp, RoundedCornerShape(14.dp))
                            .padding(8.dp)
                    ) {
                        Text("Cell #1", color = HaiInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("PCI ${best.pci ?: "—"}", color = HaiInk, fontSize = 7.sp)
                        Text("${best.band ?: "—"} • ARFCN ${best.arfcn ?: "—"}", color = HaiMuted, fontSize = 7.sp)
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(HaiBlue)
                        .border(4.dp, Color.White, CircleShape)
                )
            }
            Text(
                "Radio scan view • no geographic distance is invented",
                color = HaiMuted,
                fontSize = 6.5.sp,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}

@Composable
private fun HaiMapBackdrop(cells: List<NearbyCell>, modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFFF2F6F1))
        val street = Color.White
        repeat(5) { i ->
            val y = size.height * (0.15f + i * 0.18f)
            drawLine(street, Offset(0f, y), Offset(size.width, y - size.height * 0.18f), strokeWidth = 10f)
            drawLine(Color(0xFFDCE6DD), Offset(0f, y), Offset(size.width, y - size.height * 0.18f), strokeWidth = 1.5f)
        }
        repeat(4) { i ->
            val x = size.width * (0.18f + i * 0.22f)
            drawLine(street, Offset(x, 0f), Offset(x + size.width * 0.16f, size.height), strokeWidth = 9f)
        }
        cells.take(5).forEachIndexed { index, cell ->
            val angle = (index * 1.8 + 0.5)
            val x = size.width * (0.22f + ((cos(angle) + 1.0) * 0.29).toFloat())
            val y = size.height * (0.20f + ((sin(angle) + 1.0) * 0.28).toFloat())
            drawCircle(Color.White, radius = 13f, center = Offset(x, y))
            drawCircle(if (index == 0) HaiGreen else HaiBlue, radius = 8f, center = Offset(x, y), style = Stroke(3f))
            drawLine(if (index == 0) HaiGreen else HaiBlue, Offset(x, y + 8f), Offset(x, y + 18f), 2f)
        }
    }
}

@Composable
private fun HaiSignalMonitorCard(
    spec: HaiLayoutSpec,
    snapshot: RouterSnapshot,
    samples: List<SafeTelemetrySample>,
    modifier: Modifier
) {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val current = if (nrVerified) snapshot.nrRsrp else snapshot.lteRsrp
    HaiCardShell(spec, modifier) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⌾", color = HaiInk, fontSize = 12.sp)
                Spacer(Modifier.width(5.dp))
                Text("Signal Monitor", color = HaiInk, fontSize = hs(spec, 10), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(7.dp).clip(CircleShape).background(HaiGreen))
                Spacer(Modifier.width(4.dp))
                Text("Live", color = HaiBlue, fontSize = 7.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HaiSignalPlot(samples, Modifier.weight(1f).height((spec.signalCardHeightDp - 60).coerceAtLeast(90).dp))
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(current?.let(::fmt0) ?: "—", color = HaiInk, fontSize = hs(spec, 22), fontWeight = FontWeight.Black)
                    Text("dBm", color = HaiInk, fontSize = 9.sp)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(HaiSoftMint).padding(horizontal = 7.dp, vertical = 4.dp)) {
                        Text(signalQuality(current).second, color = Color(0xFF14945B), fontSize = 7.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HaiSignalPlot(samples: List<SafeTelemetrySample>, modifier: Modifier) {
    Canvas(modifier) {
        repeat(4) { i ->
            val y = size.height * i / 3f
            drawLine(Color(0xFFDCE5F2), Offset(0f, y), Offset(size.width, y), 1f)
        }
        repeat(6) { i ->
            val x = size.width * i / 5f
            drawLine(Color(0xFFE7EDF6), Offset(x, 0f), Offset(x, size.height), 1f)
        }
        if (samples.size < 2) return@Canvas
        fun value(sample: SafeTelemetrySample): Double? = if (sample.nrVerified) sample.nrRsrp else sample.lteRsrp
        fun point(index: Int, v: Double): Offset {
            val x = if (samples.size == 1) 0f else size.width * index / (samples.size - 1f)
            val normalized = ((v + 120.0) / 60.0).coerceIn(0.0, 1.0).toFloat()
            return Offset(x, size.height * (1f - normalized))
        }
        for (i in 0 until samples.lastIndex) {
            val a = value(samples[i])
            val b = value(samples[i + 1])
            if (a != null && b != null) {
                drawLine(HaiBlue, point(i, a), point(i + 1, b), 3f, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun HaiActiveConnectionsCard(spec: HaiLayoutSpec, snapshot: RouterSnapshot, modifier: Modifier) {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val caVerified = snapshot.raw["_zte_ca_verified"].equals("true", true) && snapshot.caActive
    val mode = snapshot.networkType.orEmpty()
    val bands = snapshot.cells.mapNotNull { it.band }.distinct().joinToString(" + ").ifBlank {
        listOfNotNull(snapshot.lteBand, snapshot.nrBand).distinct().joinToString(" + ").ifBlank { "—" }
    }
    HaiCardShell(spec, modifier) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⌂", color = HaiInk, fontSize = 11.sp)
                Spacer(Modifier.width(5.dp))
                Text("Active Connections", color = HaiInk, fontSize = hs(spec, 10), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (nrVerified) HaiStatusChip(if (mode.contains("SA", true) && !mode.contains("NSA", true)) "5G SA" else "5G NSA", HaiBlue, Color.White)
                if (snapshot.raw["_zte_lte_active_verified"].equals("true", true)) HaiStatusChip("4G LTE", Color(0xFFD9F5E7), Color(0xFF168557))
                if (caVerified) HaiStatusChip("LTE CA", Color(0xFFEBD7FF), HaiPurple)
            }
            Spacer(Modifier.height(7.dp))
            Text(bands, color = HaiInk, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        }
    }
}

@Composable
private fun HaiStatusChip(text: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Text(text, color = fg, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HaiQuickInfoCard(spec: HaiLayoutSpec, snapshot: RouterSnapshot, modifier: Modifier) {
    HaiCardShell(spec, modifier) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Quick Info", color = HaiInk, fontSize = hs(spec, 9), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("⌃", color = HaiInk, fontSize = 12.sp)
            }
            Spacer(Modifier.height(7.dp))
            HaiInfoRow("Cell ID", snapshot.cellId?.toString() ?: "—")
            HaiInfoRow("PCI", snapshot.pci?.toString() ?: "—")
            HaiInfoRow("EARFCN", snapshot.earfcn?.toString() ?: "—")
            HaiInfoRow("LTE Band", snapshot.lteBand ?: "—")
            HaiInfoRow("NR Band", snapshot.nrBand ?: "—")
        }
    }
}

@Composable
private fun HaiInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = HaiInk, fontSize = 7.5.sp, modifier = Modifier.weight(1f))
        Text(value, color = HaiInk, fontSize = 7.5.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f), maxLines = 1)
    }
}

@Composable
private fun HaiNetworkToolsCard(
    spec: HaiLayoutSpec,
    snapshot: RouterSnapshot,
    busy: Boolean,
    traffic: TrafficTelemetry?,
    thermal: ThermalTelemetry?,
    onSetNetworkMode: (String) -> Unit
) {
    HaiCardShell(spec, Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Network Mode", color = HaiInk, fontSize = hs(spec, 13), fontWeight = FontWeight.Bold)
            Text("Requested mode is never presented as active until router read-back verifies it.", color = HaiMuted, fontSize = 8.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HaiActionPill("4G only", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_LTE") }
                HaiActionPill("4G + 5G", !busy, Modifier.weight(1f)) { onSetNetworkMode("LTE_AND_5G") }
                HaiActionPill("5G only", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_5G") }
            }
            Spacer(Modifier.height(12.dp))
            Text("Router telemetry", color = HaiInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HaiMiniDataCard("↓ Live", TrafficTelemetryFormatter.rateMbps(traffic?.rxBytesPerSecond), "Mb/s", Modifier.weight(1f))
                HaiMiniDataCard("↑ Live", TrafficTelemetryFormatter.rateMbps(traffic?.txBytesPerSecond), "Mb/s", Modifier.weight(1f))
                HaiMiniDataCard("Thermal", thermal?.highestObserved?.let { ThermalTelemetryFormatter.celsius(it.celsius) } ?: "—", thermal?.highestObserved?.label ?: "sensor", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HaiMiniDataCard(title: String, value: String, suffix: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF5F8FD))
            .padding(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = HaiMuted, fontSize = 7.sp)
        Text(value, color = HaiInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(suffix, color = HaiBlue, fontSize = 6.5.sp, maxLines = 1)
    }
}

@Composable
private fun HaiTowerControlCard(
    spec: HaiLayoutSpec,
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
    val recommendation = remember(cells, snapshot.pci, snapshot.earfcn) {
        TowerRecommendationEngine.recommend(cells, snapshot.pci, snapshot.earfcn)
    }
    HaiCardShell(spec, Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tower / Cell Control", color = HaiInk, fontSize = hs(spec, 13), fontWeight = FontWeight.Bold)
                    Text("LTE lock = PCI + EARFCN with read-back and live multi-sample verification.", color = HaiMuted, fontSize = 8.sp)
                }
                Switch(
                    checked = guardEnabled,
                    onCheckedChange = onGuardChange,
                    enabled = target != null && !busy,
                    colors = SwitchDefaults.colors(checkedTrackColor = HaiMint)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HaiActionPill(if (scanBusy) "Scanning…" else "Scan cells", !scanBusy && !busy, Modifier.weight(1f), onScan)
                HaiActionPill("Verify + lock current", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
                HaiActionPill("Clear lock", !busy, Modifier.weight(1f), onClear)
            }
            target?.let {
                Text("Target: PCI ${it.pci} • EARFCN ${it.earfcn}", color = HaiBlue, fontSize = 8.sp, modifier = Modifier.padding(top = 7.dp))
            }
            guardStatus?.let {
                Text(it.message, color = if (it.match == TowerMatch.MATCHED) Color(0xFF178958) else HaiMuted, fontSize = 7.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(9.dp))
            when (recommendation.decision) {
                TowerRecommendationDecision.RECOMMEND_CANDIDATE -> Text("Recommended scan candidate: ${recommendation.reason}", color = Color(0xFF178958), fontSize = 8.sp)
                TowerRecommendationDecision.KEEP_CURRENT -> Text("Recommendation: keep current cell • ${recommendation.reason}", color = HaiBlueDeep, fontSize = 8.sp)
                TowerRecommendationDecision.INSUFFICIENT_EVIDENCE -> Text("No decisive recommendation • ${recommendation.reason}", color = HaiMuted, fontSize = 8.sp)
            }
            cells.take(16).forEachIndexed { index, cell ->
                HaiCellRow(index + 1, cell, snapshot, busy, onLockCell)
            }
        }
    }
}

@Composable
private fun HaiCellRow(index: Int, cell: NearbyCell, snapshot: RouterSnapshot, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val current = cell.rat == "LTE" && cell.pci == snapshot.pci && cell.arfcn == snapshot.earfcn
    val lockable = cell.rat == "LTE" && cell.pci != null && cell.arfcn != null
    Row(
        Modifier.fillMaxWidth().border(0.5.dp, HaiBorder).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (current) HaiSoftMint else HaiSoftBlue), contentAlignment = Alignment.Center) {
            Text(index.toString(), color = if (current) Color(0xFF178958) else HaiBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text("${cell.rat} ${cell.band ?: "—"} • PCI ${cell.pci ?: "—"} • ARFCN ${cell.arfcn ?: "—"}${if (current) " • CURRENT" else ""}", color = HaiInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("RSRP ${cell.rsrp?.let(::fmt1) ?: "—"} • evidence ${cell.evidenceScore ?: "—"}/100 • ${cell.presencePercent}%", color = HaiMuted, fontSize = 7.sp)
            Text(
                when (cell.confidence) {
                    CellConfidence.HIGH -> "High confidence"
                    CellConfidence.MEDIUM -> "Medium confidence"
                    CellConfidence.LOW -> "Low confidence"
                    null -> "Confidence incomplete"
                },
                color = when (cell.confidence) {
                    CellConfidence.HIGH -> Color(0xFF178958)
                    CellConfidence.LOW -> Color(0xFFB26A22)
                    else -> HaiMuted
                },
                fontSize = 6.5.sp
            )
        }
        if (lockable) {
            Text(
                "Verify + Lock",
                color = if (busy) HaiMuted else HaiBlue,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = !busy) { onLock(cell) }
                    .padding(7.dp)
            )
        } else {
            Text("Read only", color = HaiMuted, fontSize = 7.sp)
        }
    }
}

@Composable
private fun HaiToolsScreen(
    spec: HaiLayoutSpec,
    runtime: RuntimeCapabilityReport?,
    stability: ConnectionStabilityReport,
    thermal: ThermalTelemetry?,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    backupAvailable: Boolean,
    capabilities: RouterCapabilities,
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
    Column(verticalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)) {
        HaiCardShell(spec, Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AI Optimize", color = HaiInk, fontSize = hs(spec, 13), fontWeight = FontWeight.Bold)
                        Text("Uses verified LTE controls only; configured state is not treated as active state.", color = HaiMuted, fontSize = 8.sp)
                    }
                    Switch(checked = smartMode, onCheckedChange = onSmartModeChange, colors = SwitchDefaults.colors(checkedTrackColor = HaiMint))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OptimizationGoal.entries.forEach { goal ->
                        HaiChoicePill(goal.name, smartGoal == goal, Modifier.weight(1f)) { onSmartGoalChange(goal) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HaiActionPill(if (smartBusy) "Optimizing…" else "Optimize now", !smartBusy && !busy, Modifier.fillMaxWidth(), onOptimizeNow)
                smartReport?.let { Text(it.message, color = HaiMuted, fontSize = 7.sp, modifier = Modifier.padding(top = 6.dp)) }
            }
        }

        HaiCardShell(spec, Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Placement Assistant", color = HaiInk, fontSize = hs(spec, 12), fontWeight = FontWeight.Bold)
                        Text("Live radio guidance while you move the router.", color = HaiMuted, fontSize = 8.sp)
                    }
                    Switch(checked = placementMode, onCheckedChange = { onPlacementToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = HaiMint))
                }
                placementReading?.let { Text(it.guidance.name, color = HaiBlue, fontSize = 8.sp, modifier = Modifier.padding(top = 6.dp)) }
            }
        }

        HaiCardShell(spec, Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Firmware Diagnostics", color = HaiInk, fontSize = hs(spec, 12), fontWeight = FontWeight.Bold)
                if (runtime == null) {
                    Text("Runtime capability probe is not available yet.", color = HaiMuted, fontSize = 8.sp)
                } else {
                    HaiRuntimeLine("4G bands", runtime.lteBandControl)
                    HaiRuntimeLine("5G bands", runtime.nrBandControl)
                    HaiRuntimeLine("Cell lock", runtime.cellLock)
                    HaiRuntimeLine("Network mode", runtime.networkMode)
                    HaiRuntimeLine("Neighbor scan", runtime.neighborScan)
                    HaiRuntimeLine("Antenna", runtime.antennaControl)
                    HaiRuntimeLine("Carrier aggregation", runtime.carrierAggregationTelemetry)
                }
                thermal?.takeIf { it.hasAnyEvidence }?.let {
                    Text("Thermal: ${ThermalTelemetryFormatter.summary(it)}", color = HaiMuted, fontSize = 7.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HaiActionPill("Copy report", true, Modifier.weight(1f), onCopyDiagnostics)
                    HaiActionPill("Share report", true, Modifier.weight(1f), onShareDiagnostics)
                }
            }
        }

        HaiCardShell(spec, Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Safety / Restore", color = HaiInk, fontSize = hs(spec, 12), fontWeight = FontWeight.Bold)
                Text(if (backupAvailable) "A verified local safety backup is available." else "No safety backup has been captured yet.", color = HaiMuted, fontSize = 8.sp)
                Spacer(Modifier.height(8.dp))
                HaiActionPill("Restore + verify", backupAvailable && !busy, Modifier.fillMaxWidth(), onRestore)
            }
        }

        if (capabilities.supportsAntennaControl) {
            HaiCardShell(spec, Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Antenna Control", color = HaiInk, fontSize = hs(spec, 12), fontWeight = FontWeight.Bold)
                    Text("Visible only when runtime safety gating permits the operation.", color = HaiMuted, fontSize = 8.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..3).forEach { state ->
                            HaiActionPill("Position $state", !busy, Modifier.weight(1f)) { onAntennaState(state) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HaiRuntimeLine(label: String, evidence: RuntimeCapabilityEvidence) {
    val status = when (evidence.state) {
        RuntimeCapabilityState.SAFE_TO_ATTEMPT -> "SAFE TO ATTEMPT"
        RuntimeCapabilityState.READ_ONLY -> "READ ONLY"
        RuntimeCapabilityState.PROFILE_ONLY -> "PROFILE ONLY"
        RuntimeCapabilityState.UNAVAILABLE -> "UNAVAILABLE"
    }
    val color = when (evidence.state) {
        RuntimeCapabilityState.SAFE_TO_ATTEMPT -> Color(0xFF178958)
        RuntimeCapabilityState.READ_ONLY -> HaiBlue
        RuntimeCapabilityState.PROFILE_ONLY -> Color(0xFFB26A22)
        RuntimeCapabilityState.UNAVAILABLE -> HaiMuted
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = HaiInk, fontSize = 8.sp, modifier = Modifier.weight(1f))
        Text(status, color = color, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HaiActionPill(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) HaiSoftBlue else Color(0xFFF2F3F6))
            .border(1.dp, if (enabled) Color(0xFFDCE8FA) else Color(0xFFE8EAEE), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) HaiInk else HaiMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun HaiChoicePill(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) HaiBlue else Color(0xFFF2F5FA))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text.lowercase().replaceFirstChar { it.uppercase() }, color = if (selected) Color.White else HaiInk, fontSize = 7.sp, maxLines = 1)
    }
}

@Composable
private fun HaiOperationStrip(spec: HaiLayoutSpec, message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEEF5FF))
            .border(1.dp, Color(0xFFDCE8FA), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(message, color = HaiBlueDeep, fontSize = hs(spec, 8), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HaiBottomNav(
    spec: HaiLayoutSpec,
    selected: HaiSection,
    onSelect: (HaiSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp, bottomStart = 26.dp, bottomEnd = 26.dp)
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(spec.bottomBarHeightDp.dp)
            .shadow(12.dp, shape)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.96f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HaiBottomItem("▦", "Dashboard", selected == HaiSection.HOME, Modifier.weight(1f)) { onSelect(HaiSection.HOME) }
        HaiBottomItem("⌾", "Map", selected == HaiSection.TOWERS, Modifier.weight(1f)) { onSelect(HaiSection.TOWERS) }
        Box(
            Modifier
                .size((spec.bottomBarHeightDp - 8).dp)
                .shadow(9.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF1B78FF), Color(0xFF031C6A))))
                .border(2.dp, Color(0xFF42EAF0), CircleShape)
                .clickable { onSelect(HaiSection.TOOLS) },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▮▮▮", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text("AI", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        HaiBottomItem("▦", "Tools", selected == HaiSection.TOOLS, Modifier.weight(1f)) { onSelect(HaiSection.TOOLS) }
        HaiBottomItem("⚙", "Settings", false, Modifier.weight(1f)) { onSelect(HaiSection.NETWORK) }
    }
}

@Composable
private fun HaiBottomItem(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, color = if (selected) HaiBlue else HaiMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (selected) HaiBlue else HaiMuted, fontSize = 7.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        if (selected) Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(HaiBlue))
    }
}

@Composable
private fun HaiCardShell(spec: HaiLayoutSpec, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(spec.cardRadiusDp.dp)
    Card(
        modifier = modifier.shadow(7.dp, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = HaiCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, HaiBorder)
    ) { content() }
}

private fun buildBandRows(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>
): List<HaiBandRow> {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", true)

    val nrRows = capabilities.supportedNrBands.sorted().map { band ->
        val carrier = snapshot.cells.firstOrNull {
            it.role == CellRole.NR && bandNumber(it.band) == band && nrVerified
        }
        HaiBandRow(
            nr = true,
            band = band,
            active = carrier != null,
            selected = band in selectedNr,
            arfcn = carrier?.arfcn,
            bandwidthMhz = carrier?.bandwidthMhz
        )
    }
    val lteRows = capabilities.supportedLteBands.sorted().map { band ->
        val carrier = snapshot.cells.firstOrNull {
            it.role != CellRole.NR && bandNumber(it.band) == band && lteVerified
        }
        HaiBandRow(
            nr = false,
            band = band,
            active = carrier != null,
            selected = band in selectedLte,
            arfcn = carrier?.arfcn,
            bandwidthMhz = carrier?.bandwidthMhz
        )
    }
    return nrRows + lteRows
}

private fun bandNumber(value: String?): Int? = value
    ?.trim()
    ?.removePrefix("n")
    ?.removePrefix("N")
    ?.removePrefix("b")
    ?.removePrefix("B")
    ?.toIntOrNull()

private fun signalQuality(rsrp: Double?): Pair<Boolean, String> = when {
    rsrp == null -> false to "Unknown"
    rsrp >= -85 -> true to "Excellent"
    rsrp >= -95 -> true to "Good"
    rsrp >= -105 -> false to "Fair"
    else -> false to "Weak"
}

private fun signalBars(rsrp: Double?): Int = when {
    rsrp == null -> 0
    rsrp >= -85 -> 4
    rsrp >= -95 -> 3
    rsrp >= -105 -> 2
    else -> 1
}

private fun fmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
private fun fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun hs(spec: HaiLayoutSpec, base: Int) = (base * spec.textScale).sp
