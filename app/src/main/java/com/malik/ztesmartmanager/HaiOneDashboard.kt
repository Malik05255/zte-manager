package com.malik.ztesmartmanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
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
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget

@Composable
fun ZteManagerDashboard(
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
        var screen by rememberSaveable { mutableStateOf(HaiScreen.HOME) }
        val wifi = remember(snapshot?.raw?.get("station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("station_list"), DeviceTransport.WIFI)
        }
        val lan = remember(snapshot?.raw?.get("lan_station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("lan_station_list"), DeviceTransport.LAN)
        }
        val devices = remember(wifi, lan) { ConnectedDeviceParser.merge(wifi, lan) }
        val quality = remember(snapshot) { snapshot?.let { NetworkQualityEngine().score(it).total } ?: 0 }

        BackHandler(enabled = screen != HaiScreen.HOME) { screen = HaiScreen.HOME }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = HaiBg,
            topBar = { HaiTopBar(snapshot != null) },
            bottomBar = { HaiBottomBar(screen) { screen = it } }
        ) { padding ->
            Box(Modifier.fillMaxSize().background(HaiBg).padding(padding)) {
                if (snapshot == null) {
                    HaiDisconnected(status.ifBlank { "جاري الاتصال" })
                } else when (screen) {
                    HaiScreen.HOME -> HaiHome(
                        snapshot = snapshot,
                        qualityScore = quality,
                        telemetrySamples = telemetrySamples,
                        lastPerformance = lastPerformance,
                        speedBusy = speedBusy,
                        placementMode = placementMode,
                        placementReading = placementReading,
                        devices = devices,
                        controlBusy = controlBusy,
                        scanBusy = scanBusy,
                        operationMessage = operationMessage,
                        onSpeedTest = onSpeedTest,
                        onPlacementToggle = onPlacementToggle,
                        onSetNetworkMode = onSetNetworkMode,
                        onScanCells = onScanCells,
                        onLockCurrentCell = onLockCurrentCell,
                        onOpenNetwork = { screen = HaiScreen.NETWORK },
                        onOpenTowers = { screen = HaiScreen.TOWERS },
                        onOpenDevices = { screen = HaiScreen.DEVICES },
                        onOpenTools = { screen = HaiScreen.TOOLS }
                    )
                    HaiScreen.NETWORK -> HaiNetworkScreen(snapshot, capabilities, selectedLte, selectedNr, controlBusy, onSetNetworkMode, onLteToggle, onNrToggle, onApplyLte, onApplyNr)
                    HaiScreen.TOWERS -> HaiTowersScreen(snapshot, nearbyCells, scanBusy, controlBusy, towerTarget, towerGuardEnabled, towerGuardStatus, onScanCells, onLockCurrentCell, onLockNearbyCell, onClearCellLock, onTowerGuardChange)
                    HaiScreen.DEVICES -> HaiDevicesScreen(devices)
                    HaiScreen.TOOLS -> HaiToolsScreen(
                        snapshot = snapshot,
                        placementMode = placementMode,
                        placementReading = placementReading,
                        smartMode = smartMode,
                        smartGoal = smartGoal,
                        smartBusy = smartBusy,
                        safetyBackupAvailable = safetyBackupAvailable,
                        controlBusy = controlBusy,
                        supportsAntennaControl = capabilities.supportsAntennaControl,
                        onPlacementToggle = onPlacementToggle,
                        onSmartModeChange = onSmartModeChange,
                        onSmartGoalChange = onSmartGoalChange,
                        onOptimizeNow = onOptimizeNow,
                        onAntennaState = onAntennaState,
                        onRestoreSafetyBackup = onRestoreSafetyBackup,
                        onCopyDiagnostics = onCopyDiagnostics,
                        onShareDiagnostics = onShareDiagnostics,
                        onDisconnect = onDisconnect
                    )
                }
            }
        }
    }
}

@Composable
private fun HaiDisconnected(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        HaiCard(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            androidx.compose.material3.Text(message, color = HaiInk, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
        }
    }
}
