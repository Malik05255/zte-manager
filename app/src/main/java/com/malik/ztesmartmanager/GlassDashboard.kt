package com.malik.ztesmartmanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetry
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
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

@Composable
fun GlassDashboard(
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
        var screen by remember { mutableStateOf(GlassScreen.HOME) }
        val wifi = remember(snapshot?.raw?.get("station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("station_list"), DeviceTransport.WIFI)
        }
        val lan = remember(snapshot?.raw?.get("lan_station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("lan_station_list"), DeviceTransport.LAN)
        }
        val devices = remember(wifi, lan) { ConnectedDeviceParser.merge(wifi, lan) }

        BackHandler(enabled = screen != GlassScreen.HOME) { screen = GlassScreen.HOME }

        Column(Modifier.fillMaxSize().background(GlassBg)) {
            GlassTopNavigationBar(
                connected = snapshot != null,
                title = when (screen) {
                    GlassScreen.HOME -> "ZTE Manager"
                    GlassScreen.NETWORK -> "الشبكة والبرج"
                    GlassScreen.TOOLS -> "الأدوات"
                    GlassScreen.MORE -> "المزيد"
                }
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (snapshot == null) {
                    GlassEmpty(status.ifBlank { "جاري الاتصال بالراوتر" })
                } else when (screen) {
                    GlassScreen.HOME -> GlassHomeScreen(
                        snapshot, traffic, telemetrySamples, stability, lastPerformance, speedBusy,
                        placementMode, placementReading, controlBusy, devices, operationMessage, status,
                        onSpeedTest, onPlacementToggle, onSetNetworkMode,
                        onOpenNetwork = { screen = GlassScreen.NETWORK },
                        onOpenMore = { screen = GlassScreen.MORE }
                    )
                    GlassScreen.NETWORK -> GlassNetworkScreen(
                        snapshot, capabilities, selectedLte, selectedNr, controlBusy, scanBusy,
                        nearbyCells, towerTarget, towerGuardEnabled, towerGuardStatus,
                        onSetNetworkMode, onLteToggle, onNrToggle, onApplyLte, onApplyNr,
                        onScanCells, onLockCurrentCell, onLockNearbyCell, onClearCellLock,
                        onTowerGuardChange
                    )
                    GlassScreen.TOOLS -> GlassToolsScreen(
                        capabilities, runtime, thermal, smartMode, smartGoal, smartBusy, smartReport,
                        safetyBackupAvailable, controlBusy, onSmartModeChange, onSmartGoalChange,
                        onOptimizeNow, onAntennaState, onRestoreSafetyBackup,
                        onCopyDiagnostics, onShareDiagnostics
                    )
                    GlassScreen.MORE -> GlassMoreScreen(snapshot, traffic, devices, onDisconnect)
                }
            }

            GlassBottomNavigationBar(screen) { screen = it }
        }
    }
}
