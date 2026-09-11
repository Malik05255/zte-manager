package com.malik.ztesmartmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityAnalyzer
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetryHistory
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.SupportBundleBuilder
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetryParser
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetryParser
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.RuntimeAction
import com.malik.ztesmartmanager.core.protocol.RuntimeCapabilityAccess
import com.malik.ztesmartmanager.core.protocol.runtimeCapabilitiesFromSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget

@Composable
fun RuntimeAwareFinalDashboard(
    snapshot: RouterSnapshot?,
    capabilities: RouterCapabilities,
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
    onRestoreSafetyBackup: () -> Unit
) {
    val context = LocalContext.current
    val runtime = remember(snapshot?.raw, capabilities) {
        snapshot?.let { runtimeCapabilitiesFromSnapshot(capabilities, it.raw) }
    }

    val history = remember { SafeTelemetryHistory(capacity = 30) }
    var telemetrySamples by remember { mutableStateOf<List<SafeTelemetrySample>>(emptyList()) }
    LaunchedEffect(snapshot) {
        snapshot?.let {
            history.add(it)
            telemetrySamples = history.snapshot()
        }
    }

    val stability = remember(telemetrySamples) { ConnectionStabilityAnalyzer.analyze(telemetrySamples) }
    val traffic = remember(snapshot?.raw) { snapshot?.let { TrafficTelemetryParser.parse(it.raw) } }
    val thermal = remember(snapshot?.raw) { snapshot?.let { ThermalTelemetryParser.parse(it.raw) } }
    val supportBundle = remember(snapshot, runtime, telemetrySamples, stability, capabilities.modelFamily) {
        SupportBundleBuilder.build(
            appVersion = BuildConfig.VERSION_NAME,
            modelFamily = capabilities.modelFamily,
            snapshot = snapshot,
            runtime = runtime,
            history = telemetrySamples,
            stability = stability
        )
    }

    fun copySupportBundle() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("تقرير ZTE Smart HAI", supportBundle))
        Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
    }

    fun shareSupportBundle() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "تقرير ZTE Smart HAI ${BuildConfig.VERSION_NAME}")
            putExtra(Intent.EXTRA_TEXT, supportBundle)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة التقرير"))
    }

    fun blocked(action: RuntimeAction): Boolean {
        val report = runtime ?: run {
            Toast.makeText(context, "الوظيفة غير جاهزة", Toast.LENGTH_SHORT).show()
            return true
        }
        val decision = RuntimeCapabilityAccess.decide(report, action)
        if (!decision.allowed) {
            Toast.makeText(context, decision.reason, Toast.LENGTH_LONG).show()
            return true
        }
        return false
    }

    val effectiveCapabilities = capabilities.copy(
        supportsAntennaControl = runtime?.antennaControl?.canAttemptWrite == true
    )

    ImmersiveDashboard(
        snapshot = snapshot,
        capabilities = effectiveCapabilities,
        runtime = runtime,
        traffic = traffic,
        thermal = thermal,
        telemetrySamples = telemetrySamples,
        stability = stability,
        status = status,
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
        speedBusy = speedBusy,
        lastPerformance = lastPerformance,
        safetyBackupAvailable = safetyBackupAvailable,
        nearbyCells = nearbyCells,
        scanBusy = scanBusy,
        towerTarget = towerTarget,
        towerGuardEnabled = towerGuardEnabled,
        towerGuardStatus = towerGuardStatus,
        onDisconnect = onDisconnect,
        onSpeedTest = onSpeedTest,
        onPlacementToggle = onPlacementToggle,
        onSmartModeChange = { enabled ->
            if (!enabled || !blocked(RuntimeAction.LTE_BAND_WRITE)) onSmartModeChange(enabled)
        },
        onSmartGoalChange = onSmartGoalChange,
        onOptimizeNow = { if (!blocked(RuntimeAction.LTE_BAND_WRITE)) onOptimizeNow() },
        onLteToggle = onLteToggle,
        onNrToggle = onNrToggle,
        onApplyLte = { if (!blocked(RuntimeAction.LTE_BAND_WRITE)) onApplyLte() },
        onApplyNr = { if (!blocked(RuntimeAction.NR_BAND_WRITE)) onApplyNr() },
        onSetNetworkMode = { mode ->
            if (!blocked(RuntimeAction.NETWORK_MODE_WRITE)) onSetNetworkMode(mode)
        },
        onAntennaState = { state ->
            if (!blocked(RuntimeAction.ANTENNA_WRITE)) onAntennaState(state)
        },
        onScanCells = { if (!blocked(RuntimeAction.NEIGHBOR_SCAN)) onScanCells() },
        onLockCurrentCell = { if (!blocked(RuntimeAction.CELL_LOCK_WRITE)) onLockCurrentCell() },
        onLockNearbyCell = { cell ->
            if (!blocked(RuntimeAction.CELL_LOCK_WRITE)) onLockNearbyCell(cell)
        },
        onClearCellLock = { if (!blocked(RuntimeAction.CELL_LOCK_WRITE)) onClearCellLock() },
        onTowerGuardChange = { enabled ->
            if (!enabled || !blocked(RuntimeAction.CELL_LOCK_WRITE)) onTowerGuardChange(enabled)
        },
        onRestoreSafetyBackup = onRestoreSafetyBackup,
        onCopyDiagnostics = ::copySupportBundle,
        onShareDiagnostics = ::shareSupportBundle
    )
}
