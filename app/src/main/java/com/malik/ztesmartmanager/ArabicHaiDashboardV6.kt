package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetry
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.CellRole
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
import java.util.Locale

internal val V6Bg = Color(0xFFF7F9FC)
internal val V6Card = Color.White
internal val V6Ink = Color(0xFF0A2C68)
internal val V6Muted = Color(0xFF7B879D)
internal val V6Blue = Color(0xFF0F73F6)
internal val V6Green = Color(0xFF15B978)
internal val V6Border = Color(0xFFDCE5EF)
internal val V6SoftBlue = Color(0xFFEEF6FF)
internal val V6SoftGreen = Color(0xFFEAF9F2)
private const val V6_REFERENCE_WIDTH = 841f
private const val V6_REFERENCE_HEIGHT = 1687f

internal enum class V6Section { HOME, NETWORK, TOWERS, BANDS, TOOLS }

@Composable
fun ArabicHaiDashboardV6(
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
        var section by rememberSaveable { mutableStateOf(V6Section.HOME) }
        if (section == V6Section.HOME) {
            V6ReferenceHome(
                snapshot = snapshot,
                status = status,
                performance = lastPerformance,
                speedBusy = speedBusy,
                controlBusy = controlBusy,
                scanBusy = scanBusy,
                onDisconnect = onDisconnect,
                onSpeedTest = onSpeedTest,
                onSetNetworkMode = onSetNetworkMode,
                onScanCells = onScanCells,
                onSelect = { section = it }
            )
        } else {
            V6FunctionalPage(
                section = section,
                snapshot = snapshot,
                capabilities = capabilities,
                runtime = runtime,
                stability = stability,
                telemetrySamples = telemetrySamples,
                operationMessage = operationMessage,
                placementMode = placementMode,
                placementReading = placementReading,
                smartMode = smartMode,
                smartGoal = smartGoal,
                smartBusy = smartBusy,
                smartReport = smartReport,
                selectedLte = selectedLte,
                selectedNr = selectedNr,
                controlBusy = controlBusy,
                safetyBackupAvailable = safetyBackupAvailable,
                nearbyCells = nearbyCells,
                scanBusy = scanBusy,
                towerTarget = towerTarget,
                towerGuardEnabled = towerGuardEnabled,
                towerGuardStatus = towerGuardStatus,
                onPlacementToggle = onPlacementToggle,
                onSmartModeChange = onSmartModeChange,
                onSmartGoalChange = onSmartGoalChange,
                onOptimizeNow = onOptimizeNow,
                onLteToggle = onLteToggle,
                onNrToggle = onNrToggle,
                onApplyLte = onApplyLte,
                onApplyNr = onApplyNr,
                onSetNetworkMode = onSetNetworkMode,
                onAntennaState = onAntennaState,
                onScanCells = onScanCells,
                onLockCurrentCell = onLockCurrentCell,
                onLockNearbyCell = onLockNearbyCell,
                onClearCellLock = onClearCellLock,
                onTowerGuardChange = onTowerGuardChange,
                onRestoreSafetyBackup = onRestoreSafetyBackup,
                onCopyDiagnostics = onCopyDiagnostics,
                onShareDiagnostics = onShareDiagnostics,
                onSelect = { section = it }
            )
        }
    }
}

@Composable
private fun V6ReferenceHome(
    snapshot: RouterSnapshot?,
    status: String,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    controlBusy: Boolean,
    scanBusy: Boolean,
    onDisconnect: () -> Unit,
    onSpeedTest: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onScanCells: () -> Unit,
    onSelect: (V6Section) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(V6Bg).navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth().statusBarsPadding()) {
                BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(V6_REFERENCE_WIDTH / V6_REFERENCE_HEIGHT)) {
                    Image(
                        painter = painterResource(R.drawable.home_reference_v6),
                        contentDescription = "الواجهة الرئيسية المرجعية",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    V6Hotspot(.735f, .024f, .180f, .056f, maxWidth, maxHeight, snapshot != null, onDisconnect)
                    V6Hotspot(.510f, .408f, .440f, .244f, maxWidth, maxHeight, !speedBusy, onSpeedTest)
                    V6Hotspot(.042f, .408f, .440f, .450f, maxWidth, maxHeight, !scanBusy, onScanCells)
                    V6Hotspot(.535f, .682f, .410f, .035f, maxWidth, maxHeight, !controlBusy) { onSetNetworkMode("WL_AND_5G") }
                    V6Hotspot(.535f, .716f, .410f, .034f, maxWidth, maxHeight, !controlBusy) { onSetNetworkMode("Only_GSM") }
                    V6Hotspot(.535f, .748f, .410f, .034f, maxWidth, maxHeight, !controlBusy) { onSetNetworkMode("Only_WCDMA") }
                    V6Hotspot(.535f, .780f, .410f, .035f, maxWidth, maxHeight, !controlBusy) { onSetNetworkMode("Only_LTE") }
                    V6Hotspot(.535f, .813f, .410f, .036f, maxWidth, maxHeight, !controlBusy) { onSetNetworkMode("Only_5G") }
                    V6Hotspot(.000f, .895f, .200f, .100f, maxWidth, maxHeight) { onSelect(V6Section.BANDS) }
                    V6Hotspot(.200f, .895f, .200f, .100f, maxWidth, maxHeight) { onSelect(V6Section.TOWERS) }
                    V6Hotspot(.400f, .895f, .200f, .100f, maxWidth, maxHeight) { onSelect(V6Section.TOOLS) }
                    V6Hotspot(.600f, .895f, .200f, .100f, maxWidth, maxHeight) { onSelect(V6Section.NETWORK) }
                    V6Hotspot(.800f, .895f, .200f, .100f, maxWidth, maxHeight) { onSelect(V6Section.HOME) }
                }
            }
        }
        item { V6LiveStatus(snapshot, status, performance) }
    }
}

@Composable
private fun V6Hotspot(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    referenceWidth: Dp,
    referenceHeight: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .offset(x = referenceWidth * x, y = referenceHeight * y)
            .size(referenceWidth * width, referenceHeight * height)
            .clickable(enabled = enabled, onClick = onClick)
    )
}

@Composable
private fun V6LiveStatus(snapshot: RouterSnapshot?, status: String, performance: NetworkPerformance?) {
    V6Card(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("البيانات الحية", color = V6Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            if (snapshot == null) {
                Text(status.ifBlank { "غير متصل" }, color = V6Muted, fontSize = 13.sp)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V6Value("الشبكة", v6Network(snapshot), Modifier.weight(1f))
                    V6Value("الترددات", v6ActiveBands(snapshot).joinToString(" + ").ifBlank { "—" }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V6Value("RSRP", v6Rsrp(snapshot)?.let { "${v6Fmt(it)} dBm" } ?: "—", Modifier.weight(1f))
                    V6Value("السرعة", performance?.downloadMbps?.let { "${v6Fmt(it)} Mb/s" } ?: "—", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun V6FunctionalPage(
    section: V6Section,
    snapshot: RouterSnapshot?,
    capabilities: RouterCapabilities,
    runtime: RuntimeCapabilityReport?,
    stability: ConnectionStabilityReport,
    telemetrySamples: List<SafeTelemetrySample>,
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
    safetyBackupAvailable: Boolean,
    nearbyCells: List<NearbyCell>,
    scanBusy: Boolean,
    towerTarget: TowerTarget?,
    towerGuardEnabled: Boolean,
    towerGuardStatus: TowerGuardStatus?,
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
    onShareDiagnostics: () -> Unit,
    onSelect: (V6Section) -> Unit
) {
    Column(Modifier.fillMaxSize().background(V6Bg)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().background(Color.White).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", color = V6Blue, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSelect(V6Section.HOME) }.padding(6.dp))
            Text(v6Title(section), color = V6Ink, fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(42.dp))
        }

        Box(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (operationMessage.isNotBlank()) item {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(V6SoftBlue).padding(12.dp)) {
                        Text(operationMessage, color = V6Ink, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (snapshot == null) {
                    item { V6Card { Text("الراوتر غير متصل", color = V6Muted, fontSize = 14.sp, modifier = Modifier.padding(18.dp)) } }
                } else when (section) {
                    V6Section.NETWORK -> item { V6Network(snapshot, controlBusy, onSetNetworkMode) }
                    V6Section.TOWERS -> item {
                        V6Towers(snapshot, nearbyCells, controlBusy, scanBusy, towerTarget, towerGuardEnabled, towerGuardStatus,
                            onScanCells, onLockCurrentCell, onLockNearbyCell, onClearCellLock, onTowerGuardChange)
                    }
                    V6Section.BANDS -> item { V6Bands(snapshot, capabilities, selectedLte, selectedNr, controlBusy, onLteToggle, onNrToggle, onApplyLte, onApplyNr) }
                    V6Section.TOOLS -> item {
                        V6Tools(runtime, stability, telemetrySamples.size, placementMode, placementReading, smartMode, smartGoal,
                            smartBusy, smartReport, safetyBackupAvailable, capabilities.supportsAntennaControl, controlBusy,
                            onPlacementToggle, onSmartModeChange, onSmartGoalChange, onOptimizeNow, onAntennaState,
                            onRestoreSafetyBackup, onCopyDiagnostics, onShareDiagnostics)
                    }
                    V6Section.HOME -> Unit
                }
            }
            V6BottomNav(section, onSelect, Modifier.align(Alignment.BottomCenter))
        }
    }
}
