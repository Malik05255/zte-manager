package com.malik.ztesmartmanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
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
        var screen by remember { mutableStateOf(ZteScreen.HOME) }
        val wifi = remember(snapshot?.raw?.get("station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("station_list"), DeviceTransport.WIFI)
        }
        val lan = remember(snapshot?.raw?.get("lan_station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("lan_station_list"), DeviceTransport.LAN)
        }
        val devices = remember(wifi, lan) { ConnectedDeviceParser.merge(wifi, lan) }
        val quality = remember(snapshot) { snapshot?.let { NetworkQualityEngine().score(it) } }

        BackHandler(enabled = screen != ZteScreen.HOME) { screen = ZteScreen.HOME }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ZteBg,
            topBar = {
                ZteTopBar(
                    connected = snapshot != null,
                    title = when (screen) {
                        ZteScreen.HOME -> "ZTE Manager"
                        ZteScreen.NETWORK -> "الشبكة والترددات"
                        ZteScreen.TOOLS -> "الأدوات الذكية"
                        ZteScreen.MORE -> "المزيد"
                    }
                )
            },
            bottomBar = { ZteBottomBar(screen) { screen = it } }
        ) { padding ->
            Box(Modifier.fillMaxSize().background(ZteBg).padding(padding)) {
                if (snapshot == null) {
                    ZteDisconnectedState(status.ifBlank { "جاري الاتصال بالراوتر" })
                } else when (screen) {
                    ZteScreen.HOME -> ZteManagerHome(
                        snapshot = snapshot,
                        qualityScore = quality?.total ?: 0,
                        traffic = traffic,
                        telemetrySamples = telemetrySamples,
                        stability = stability,
                        lastPerformance = lastPerformance,
                        speedBusy = speedBusy,
                        placementMode = placementMode,
                        placementReading = placementReading,
                        deviceCount = devices.size,
                        controlBusy = controlBusy,
                        operationMessage = operationMessage,
                        status = status,
                        onSpeedTest = onSpeedTest,
                        onPlacementToggle = onPlacementToggle,
                        onSetNetworkMode = onSetNetworkMode,
                        onOpenNetwork = { screen = ZteScreen.NETWORK },
                        onOpenTools = { screen = ZteScreen.TOOLS },
                        onOpenMore = { screen = ZteScreen.MORE }
                    )
                    ZteScreen.NETWORK -> ZteManagerNetwork(
                        snapshot, capabilities, selectedLte, selectedNr, controlBusy, scanBusy,
                        nearbyCells, towerTarget, towerGuardEnabled, towerGuardStatus,
                        onSetNetworkMode, onLteToggle, onNrToggle, onApplyLte, onApplyNr,
                        onScanCells, onLockCurrentCell, onLockNearbyCell, onClearCellLock,
                        onTowerGuardChange
                    )
                    ZteScreen.TOOLS -> ZteManagerTools(
                        snapshot, capabilities, runtime, thermal, placementMode, placementReading,
                        smartMode, smartGoal, smartBusy, smartReport, safetyBackupAvailable, controlBusy,
                        onPlacementToggle, onSmartModeChange, onSmartGoalChange, onOptimizeNow,
                        onAntennaState, onRestoreSafetyBackup, onCopyDiagnostics, onShareDiagnostics
                    )
                    ZteScreen.MORE -> ZteManagerMore(snapshot, traffic, devices, onDisconnect)
                }
            }
        }
    }
}
