package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val M3Bg = Color(0xFFF7FAFE)
private val M3CardColor = Color.White
private val M3Ink = Color(0xFF0A2C67)
private val M3Muted = Color(0xFF6A7690)
private val M3Blue = Color(0xFF1478F8)
private val M3Green = Color(0xFF18B978)
private val M3Border = Color(0xFFE5EAF2)
private val M3SoftBlue = Color(0xFFEEF4FF)
private val M3SoftGreen = Color(0xFFEAF8F3)

private enum class M3Section { HOME, NETWORK, TOOLS, LOGS, MORE, TOWERS, BANDS }

private data class M3Layout(
    val compact: Boolean,
    val padding: Int,
    val gap: Int,
    val scale: Float,
    val bandColumns: Int
)

@Composable
fun ArabicHaiDashboardV3(
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
    onRefreshSnapshot: () -> Unit,
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
        var section by rememberSaveable { mutableStateOf(M3Section.HOME) }
        BoxWithConstraints(Modifier.fillMaxSize().background(M3Bg)) {
            val width = maxWidth.value.roundToInt().coerceAtLeast(1)
            val height = maxHeight.value.roundToInt().coerceAtLeast(1)
            val spec = remember(width, height) { HaiResponsivePolicy.resolve(width, height) }
            val layout = M3Layout(
                compact = width < 380,
                padding = if (width < 360) 12 else if (width < 430) 16 else 20,
                gap = if (height < 760) 10 else 13,
                scale = spec.textScale.coerceIn(1.0f, 1.10f),
                bandColumns = if (width < 350) 3 else 4
            )

            if (snapshot != null && section == M3Section.HOME) {
                TargetHomeDashboard(
                    snapshot = snapshot,
                    telemetrySamples = telemetrySamples,
                    status = status,
                    operationMessage = operationMessage,
                    lastPerformance = lastPerformance,
                    speedBusy = speedBusy,
                    controlBusy = controlBusy,
                    smartBusy = smartBusy,
                    onDisconnect = onDisconnect,
                    onSpeedTest = onSpeedTest,
                    onSetNetworkMode = onSetNetworkMode,
                    onNavigateNetwork = { section = M3Section.NETWORK },
                    onNavigateTowers = { section = M3Section.TOWERS },
                    onNavigateBands = { section = M3Section.BANDS },
                    onNavigateTools = { section = M3Section.TOOLS },
                    onNavigateLogs = { section = M3Section.LOGS },
                    onNavigateMore = { section = M3Section.MORE },
                    onRefreshNow = onRefreshSnapshot,
                    onOptimizeNow = onOptimizeNow
                )
                return@BoxWithConstraints
            }

            Column(Modifier.fillMaxSize()) {
                HaiSharedHeader(
                    connected = snapshot != null,
                    onDisconnect = onDisconnect,
                    onMenu = { section = M3Section.MORE },
                    onSettings = { section = M3Section.TOOLS },
                    onSearch = { section = M3Section.TOWERS }
                )

                if (snapshot == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (status.isNotBlank() && status != "غير متصل") status else "جاري الاتصال…",
                            color = M3Muted,
                            fontSize = m3sp(layout, 14),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    M3BottomNav(section) { section = it }
                    return@Column
                }

                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = layout.padding.dp,
                            end = layout.padding.dp,
                            top = 4.dp,
                            bottom = 82.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(layout.gap.dp)
                    ) {
                        compactOperationMessage(operationMessage).takeIf { it.isNotBlank() }?.let { msg ->
                            item { M3Message(layout, msg) }
                        }

                        when (section) {
                            M3Section.HOME -> Unit
                            M3Section.NETWORK -> {
                                item { M3NetworkCard(layout, snapshot) }
                                item { M3NetworkMode(layout, snapshot, controlBusy, onSetNetworkMode) }
                                item { M3Traffic(layout, traffic) }
                                if (thermal?.hasAnyEvidence == true) item { M3Thermal(layout, thermal) }
                            }
                            M3Section.TOOLS -> {
                                item {
                                    M3Tools(
                                        layout = layout,
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
                                        onShareDiagnostics = onShareDiagnostics,
                                        onRefresh = onRefreshSnapshot
                                    )
                                }
                            }
                            M3Section.LOGS -> item { M3Logs(layout, telemetrySamples, lastPerformance) }
                            M3Section.MORE -> item {
                                M3More(
                                    layout = layout,
                                    snapshot = snapshot,
                                    onTowers = { section = M3Section.TOWERS },
                                    onBands = { section = M3Section.BANDS },
                                    onNetwork = { section = M3Section.NETWORK },
                                    onTools = { section = M3Section.TOOLS }
                                )
                            }
                            M3Section.TOWERS -> item {
                                M3TowerControls(
                                    layout = layout,
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
                            M3Section.BANDS -> item {
                                M3Bands(
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
                    }
                    M3BottomNav(section, { section = it }, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun M3NetworkCard(layout: M3Layout, snapshot: RouterSnapshot) {
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
    M3Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حالة الشبكة", color = M3Ink, fontSize = m3sp(layout, 17), fontWeight = FontWeight.Black)
                    Text("$network $mode", color = M3Blue, fontSize = m3sp(layout, 30), fontWeight = FontWeight.Black)
                }
                M3Pill(m3Quality(rsrp), if (rsrp == null) M3Muted else M3Green)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                M3Metric("RSRP", rsrp, "dBm", Modifier.weight(1f))
                M3Metric("SINR", sinr, "dB", Modifier.weight(1f))
                M3Metric("RSRQ", snapshot.lteRsrq, "dB", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun M3NetworkMode(layout: M3Layout, snapshot: RouterSnapshot, busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    val current = snapshot.raw["BearerPreference"].orEmpty()
    M3Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("وضع الشبكة", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            M3Action("3G فقط${if (current == "Only_WCDMA") " • الحالي" else ""}", !busy) { onSetNetworkMode("Only_WCDMA") }
            Spacer(Modifier.height(8.dp))
            M3Action("4G فقط${if (current == "Only_LTE") " • الحالي" else ""}", !busy) { onSetNetworkMode("Only_LTE") }
            Spacer(Modifier.height(8.dp))
            M3Action("5G فقط${if (current == "Only_5G") " • الحالي" else ""}", !busy) { onSetNetworkMode("Only_5G") }
            Spacer(Modifier.height(8.dp))
            M3Action("تلقائي${if (current == "WL_AND_5G" || current == "LTE_AND_5G") " • الحالي" else ""}", !busy) { onSetNetworkMode("WL_AND_5G") }
        }
    }
}

@Composable
private fun M3Traffic(layout: M3Layout, traffic: TrafficTelemetry?) {
    M3Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("حركة البيانات", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                M3ValueBox("تنزيل", TrafficTelemetryFormatter.rateMbps(traffic?.rxBytesPerSecond), "Mb/s", M3SoftBlue, Modifier.weight(1f))
                M3ValueBox("رفع", TrafficTelemetryFormatter.rateMbps(traffic?.txBytesPerSecond), "Mb/s", M3SoftGreen, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun M3Thermal(layout: M3Layout, thermal: ThermalTelemetry) {
    M3Card(layout) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("الحرارة", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(thermal.highestObserved?.let { ThermalTelemetryFormatter.celsius(it.celsius) } ?: "—", color = M3Blue, fontSize = m3sp(layout, 20), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun M3TowerControls(
    layout: M3Layout,
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
    M3Card(layout) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الأبراج والخلايا", color = M3Ink, fontSize = m3sp(layout, 20), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                M3CompactAction(if (scanBusy) "جاري…" else "مسح فعلي", !scanBusy && !busy, onScan)
            }
            Spacer(Modifier.height(10.dp))
            VerifiedTowerMapPanel(snapshot = snapshot, cells = cells, busy = busy, onLockCell = onLockCell)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                M3ValueBox("PCI", snapshot.pci?.toString() ?: "—", "", M3SoftBlue, Modifier.weight(1f))
                M3ValueBox("EARFCN", snapshot.earfcn?.toString() ?: "—", "", M3SoftGreen, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                M3Primary("تثبيت الحالية", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
                M3Outline("إزالة القفل", !busy, Modifier.weight(1f), onClear)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("حارس البرج", color = M3Ink, fontSize = m3sp(layout, 14), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(
                    checked = guardEnabled,
                    onCheckedChange = onGuardChange,
                    enabled = target != null && !busy,
                    colors = SwitchDefaults.colors(checkedTrackColor = M3Green)
                )
            }
            target?.let { Text("PCI ${it.pci} • EARFCN ${it.earfcn}", color = M3Blue, fontSize = m3sp(layout, 12), fontWeight = FontWeight.Bold) }
            guardStatus?.let {
                Text(
                    if (it.match == TowerMatch.MATCHED) "القفل مطابق فعليًا" else compactOperationMessage(it.message),
                    color = if (it.match == TowerMatch.MATCHED) M3Green else M3Muted,
                    fontSize = m3sp(layout, 11),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (cells.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                cells.take(12).forEachIndexed { index, cell ->
                    M3CellRow(index + 1, cell, snapshot, busy, onLockCell)
                    if (index != cells.take(12).lastIndex) Spacer(Modifier.height(7.dp))
                }
            }
        }
    }
}

@Composable
private fun M3CellRow(index: Int, cell: NearbyCell, snapshot: RouterSnapshot, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val current = cell.rat == "LTE" && cell.pci == snapshot.pci && cell.arfcn == snapshot.earfcn
    val lockable = cell.rat == "LTE" && cell.pci != null && cell.arfcn != null
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (current) M3SoftGreen else Color(0xFFF8FAFD)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(index.toString(), color = M3Blue, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = M3Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text("ARFCN ${cell.arfcn ?: "—"} • ${cell.rsrp?.let { "${m3Fmt0(it)} dBm" } ?: "—"}", color = M3Muted, fontSize = 11.sp)
        }
        if (lockable && !current) {
            Text("انتقل", color = if (busy) M3Muted else M3Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = !busy) { onLock(cell) }.padding(8.dp))
        } else if (current) {
            M3Pill("الحالية", M3Green)
        }
    }
}

@Composable
private fun M3Bands(
    layout: M3Layout,
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
            .mapNotNull { m3BandNumber(it.band) }.toSet()
    }
    M3Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الترددات", color = M3Ink, fontSize = m3sp(layout, 20), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    M3Choice("4G", !showNr) { showNr = false }
                    M3Choice("5G", showNr) { showNr = true }
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
                                .background(if (isActive) M3SoftGreen else if (isSelected) M3SoftBlue else Color(0xFFF8FAFD))
                                .border(1.dp, if (isActive) M3Green else if (isSelected) M3Blue else M3Border, RoundedCornerShape(16.dp))
                                .clickable(enabled = !busy) { if (showNr) onNrToggle(band) else onLteToggle(band) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${if (showNr) "N" else "B"}$band", color = M3Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    repeat(layout.bandColumns - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
            M3Primary(
                if (showNr) "تطبيق 5G" else "تطبيق 4G",
                selected.isNotEmpty() && !busy && (!showNr || capabilities.supportsNrBandLock),
                Modifier.fillMaxWidth()
            ) { if (showNr) onApplyNr() else onApplyLte() }
        }
    }
}

@Composable
private fun M3Tools(
    layout: M3Layout,
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
    onShareDiagnostics: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
        M3Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("التحسين الذكي", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Switch(checked = smartMode, onCheckedChange = onSmartModeChange, colors = SwitchDefaults.colors(checkedTrackColor = M3Green))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OptimizationGoal.entries.forEach { goal -> M3Choice(m3Goal(goal), smartGoal == goal) { onSmartGoalChange(goal) } }
                }
                Spacer(Modifier.height(10.dp))
                M3Primary(if (smartBusy) "جاري…" else "تحسين الآن", !smartBusy && !busy, Modifier.fillMaxWidth(), onOptimizeNow)
                smartReport?.let { Text("آخر نتيجة: ${it.best.qualityScore}/100", color = M3Muted, fontSize = m3sp(layout, 11), modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        M3Card(layout) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("أفضل مكان", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black)
                    placementReading?.let { Text("${it.score.total}/100", color = M3Blue, fontSize = m3sp(layout, 20), fontWeight = FontWeight.Black) }
                }
                Switch(checked = placementMode, onCheckedChange = { onPlacementToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = M3Green))
            }
        }
        M3Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Text("التشخيص", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                if (runtime == null) {
                    Text("غير جاهز", color = M3Muted, fontSize = m3sp(layout, 12))
                } else {
                    val ready = listOf(runtime.lteBandControl, runtime.nrBandControl, runtime.cellLock, runtime.networkMode, runtime.neighborScan, runtime.antennaControl)
                        .count { it.state == RuntimeCapabilityState.SAFE_TO_ATTEMPT }
                    Text("$ready وظائف جاهزة", color = M3Green, fontSize = m3sp(layout, 14), fontWeight = FontWeight.Bold)
                }
                Text(stability.summary, color = M3Muted, fontSize = m3sp(layout, 11), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    M3Outline("نسخ التقرير", true, Modifier.weight(1f), onCopyDiagnostics)
                    M3Outline("مشاركة التقرير", true, Modifier.weight(1f), onShareDiagnostics)
                }
                Spacer(Modifier.height(8.dp))
                M3Outline("تحديث البيانات الآن", !busy, Modifier.fillMaxWidth(), onRefresh)
            }
        }
        M3Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Text("الاستعادة الآمنة", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                M3Primary("استعادة النسخة", backupAvailable && !busy, Modifier.fillMaxWidth(), onRestore)
            }
        }
        if (supportsAntenna) {
            M3Card(layout) {
                Column(Modifier.padding(18.dp)) {
                    Text("الهوائي", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { state -> M3Outline("وضع $state", !busy, Modifier.weight(1f)) { onAntennaState(state) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun M3Logs(layout: M3Layout, samples: List<SafeTelemetrySample>, performance: NetworkPerformance?) {
    M3Card(layout) {
        Column(Modifier.padding(16.dp)) {
            Text("السجلات الحية", color = M3Ink, fontSize = m3sp(layout, 20), fontWeight = FontWeight.Black)
            Text("آخر القراءات التي استلمها التطبيق فعلًا من الراوتر", color = M3Muted, fontSize = m3sp(layout, 11))
            performance?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    "آخر اختبار سرعة: تنزيل ${it.downloadMbps?.let(::m3Fmt1) ?: "—"} Mb/s • رفع ${it.uploadMbps?.let(::m3Fmt1) ?: "—"} Mb/s • ${it.latencyMs?.let(::m3Fmt0) ?: "—"} ms",
                    color = M3Blue,
                    fontSize = m3sp(layout, 11),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            if (samples.isEmpty()) {
                Text("لا توجد قراءات بعد", color = M3Muted, fontSize = m3sp(layout, 12))
            } else {
                samples.asReversed().take(30).forEach { sample ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8FAFD)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${sample.networkType ?: "غير مؤكد"} • ${sample.nrBand ?: sample.lteBand ?: "لا يوجد تردد موثّق"}",
                                color = M3Ink,
                                fontSize = m3sp(layout, 11),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "RSRP ${sample.nrRsrp ?: sample.lteRsrp ?: "—"} • SINR ${sample.nrSinr ?: sample.lteSinr ?: "—"} • PCI ${sample.pci ?: "—"} • EARFCN ${sample.earfcn ?: "—"}",
                                color = M3Muted,
                                fontSize = m3sp(layout, 9)
                            )
                        }
                        Text(m3Time(sample.timestampEpochMs), color = M3Muted, fontSize = m3sp(layout, 9))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun M3More(layout: M3Layout, snapshot: RouterSnapshot, onTowers: () -> Unit, onBands: () -> Unit, onNetwork: () -> Unit, onTools: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
        M3Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Text("المزيد", color = M3Ink, fontSize = m3sp(layout, 20), fontWeight = FontWeight.Black)
                Text(snapshot.model ?: "طراز الراوتر غير مقروء", color = M3Muted, fontSize = m3sp(layout, 12))
                snapshot.firmware?.let { Text("إصدار الراوتر: $it", color = M3Muted, fontSize = m3sp(layout, 10), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.height(12.dp))
                M3Action("الأبراج والخريطة", true, onTowers)
                Spacer(Modifier.height(8.dp))
                M3Action("الترددات وقفل النطاقات", true, onBands)
                Spacer(Modifier.height(8.dp))
                M3Action("تفاصيل الشبكة", true, onNetwork)
                Spacer(Modifier.height(8.dp))
                M3Action("الأدوات والتشخيص", true, onTools)
            }
        }
    }
}

@Composable
private fun M3Metric(label: String, value: Double?, unit: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF8FAFD)).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = M3Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value?.let(::m3Fmt1) ?: "—", color = M3Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(unit, color = M3Muted, fontSize = 10.sp)
    }
}

@Composable
private fun M3ValueBox(title: String, value: String, unit: String, bg: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(bg).padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = M3Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = M3Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
        if (unit.isNotBlank()) Text(unit, color = M3Muted, fontSize = 10.sp)
    }
}

@Composable
private fun M3Action(text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp))
            .background(if (enabled) M3SoftBlue else Color(0xFFF1F3F6))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = if (enabled) M3Ink else M3Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("‹", color = if (enabled) M3Blue else M3Muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun M3Primary(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(16.dp))
            .background(if (enabled) M3Blue else Color(0xFFE7EAF0)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.White else M3Muted, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun M3Outline(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(M3CardColor)
            .border(1.dp, M3Border, RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) M3Ink else M3Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun M3CompactAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(M3SoftBlue)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text, color = if (enabled) M3Blue else M3Muted, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun M3Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) M3Blue else Color(0xFFF0F3F8))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text, color = if (selected) Color.White else M3Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun M3Pill(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .12f)).padding(horizontal = 11.dp, vertical = 7.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun M3Message(layout: M3Layout, text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(M3SoftBlue).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(text, color = M3Ink, fontSize = m3sp(layout, 12), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun M3Card(layout: M3Layout, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (layout.compact) 20.dp else 22.dp),
        colors = CardDefaults.cardColors(containerColor = M3CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, M3Border)
    ) { content() }
}

@Composable
private fun M3BottomNav(selected: M3Section, onSelect: (M3Section) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        HaiSharedBottomNav(
            selected = when (selected) {
                M3Section.HOME -> "home"
                M3Section.NETWORK -> "network"
                M3Section.TOOLS -> "tools"
                M3Section.LOGS -> "logs"
                M3Section.MORE, M3Section.TOWERS, M3Section.BANDS -> "more"
            },
            onHome = { onSelect(M3Section.HOME) },
            onNetwork = { onSelect(M3Section.NETWORK) },
            onTools = { onSelect(M3Section.TOOLS) },
            onLogs = { onSelect(M3Section.LOGS) },
            onMore = { onSelect(M3Section.MORE) }
        )
    }
}

private fun compactOperationMessage(message: String): String {
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

private fun m3Quality(rsrp: Double?): String = when {
    rsrp == null -> "غير معروف"
    rsrp >= -85 -> "ممتاز"
    rsrp >= -95 -> "جيد"
    rsrp >= -105 -> "متوسط"
    else -> "ضعيف"
}

private fun m3Goal(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun m3BandNumber(value: String?): Int? = value?.trim()?.removePrefix("n")?.removePrefix("N")?.removePrefix("b")?.removePrefix("B")?.toIntOrNull()
private fun m3Fmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
private fun m3Fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun m3sp(layout: M3Layout, base: Int) = (base * layout.scale).sp
private fun m3Time(epochMs: Long): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(epochMs))
