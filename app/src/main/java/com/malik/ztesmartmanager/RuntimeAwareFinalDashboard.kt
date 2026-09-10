package com.malik.ztesmartmanager

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState
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

private val RuntimeBarBg = Color(0xFFF7F2E9)
private val RuntimeBarInk = Color(0xFF2A241E)
private val RuntimeBarMuted = Color(0xFF776E63)
private val RuntimeBarGoldDeep = Color(0xFF876126)
private val RuntimeBarGood = Color(0xFF567D5B)
private val RuntimeBarWarn = Color(0xFFA96432)

/**
 * Runtime-capability shell around the premium dashboard.
 *
 * UI callbacks are blocked unless the current snapshot exposes the required firmware evidence.
 * The protocol client still repeats its own fresh probe immediately before every radio write.
 */
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

    fun blocked(action: RuntimeAction): Boolean {
        val report = runtime ?: run {
            Toast.makeText(context, "لم يكتمل فحص Firmware بعد؛ لم يتم إرسال أي أمر", Toast.LENGTH_SHORT).show()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0ECE4))
    ) {
        RuntimeCapabilityBar(runtime, Modifier.fillMaxWidth())
        Box(Modifier.weight(1f)) {
            FinalDashboard(
                snapshot = snapshot,
                capabilities = effectiveCapabilities,
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
                onOptimizeNow = {
                    if (!blocked(RuntimeAction.LTE_BAND_WRITE)) onOptimizeNow()
                },
                onLteToggle = onLteToggle,
                onNrToggle = onNrToggle,
                onApplyLte = {
                    if (!blocked(RuntimeAction.LTE_BAND_WRITE)) onApplyLte()
                },
                onApplyNr = {
                    if (!blocked(RuntimeAction.NR_BAND_WRITE)) onApplyNr()
                },
                onSetNetworkMode = { mode ->
                    if (!blocked(RuntimeAction.NETWORK_MODE_WRITE)) onSetNetworkMode(mode)
                },
                onAntennaState = { state ->
                    if (!blocked(RuntimeAction.ANTENNA_WRITE)) onAntennaState(state)
                },
                onScanCells = {
                    if (!blocked(RuntimeAction.NEIGHBOR_SCAN)) onScanCells()
                },
                onLockCurrentCell = {
                    if (!blocked(RuntimeAction.CELL_LOCK_WRITE)) onLockCurrentCell()
                },
                onLockNearbyCell = { cell ->
                    if (!blocked(RuntimeAction.CELL_LOCK_WRITE)) onLockNearbyCell(cell)
                },
                onClearCellLock = {
                    if (!blocked(RuntimeAction.CELL_LOCK_WRITE)) onClearCellLock()
                },
                onTowerGuardChange = { enabled ->
                    if (!enabled || !blocked(RuntimeAction.CELL_LOCK_WRITE)) onTowerGuardChange(enabled)
                },
                onRestoreSafetyBackup = onRestoreSafetyBackup
            )
        }
    }
}

@Composable
private fun RuntimeCapabilityBar(report: RuntimeCapabilityReport?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = RuntimeBarBg)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("جاهزية Firmware", color = RuntimeBarInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (report == null) {
                            "جاري جمع دليل read-back..."
                        } else {
                            "${RuntimeCapabilityAccess.writeReadyCount(report)}/${RuntimeCapabilityAccess.writeActionCount()} مسارات كتابة آمنة"
                        },
                        color = RuntimeBarMuted,
                        fontSize = 7.sp
                    )
                }
                Text(
                    if (report == null) "PROBING" else if (report.safeWriteCount > 0) "RUNTIME VERIFIED" else "READ ONLY",
                    color = if ((report?.safeWriteCount ?: 0) > 0) RuntimeBarGood else RuntimeBarWarn,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Black
                )
            }

            if (report != null) {
                Spacer(Modifier.size(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RuntimeStateChip("4G", report.lteBandControl.state, Modifier.weight(1f))
                    RuntimeStateChip("5G", report.nrBandControl.state, Modifier.weight(1f))
                    RuntimeStateChip("CELL", report.cellLock.state, Modifier.weight(1f))
                    RuntimeStateChip("MODE", report.networkMode.state, Modifier.weight(1f))
                    RuntimeStateChip("SCAN", report.neighborScan.state, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RuntimeStateChip(label: String, state: RuntimeCapabilityState, modifier: Modifier) {
    val (text, color) = when (state) {
        RuntimeCapabilityState.SAFE_TO_ATTEMPT -> "آمن" to RuntimeBarGood
        RuntimeCapabilityState.READ_ONLY -> "قراءة" to RuntimeBarGoldDeep
        RuntimeCapabilityState.PROFILE_ONLY -> "Profile" to RuntimeBarWarn
        RuntimeCapabilityState.UNAVAILABLE -> "غير متاح" to RuntimeBarMuted
    }
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 2.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = RuntimeBarInk, fontSize = 6.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text, color = color, fontSize = 6.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}
