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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
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
import kotlin.math.roundToInt

private val M3Bg = Color(0xFFF6F8FB)
private val M3Card = Color.White
private val M3Ink = Color(0xFF10275C)
private val M3Muted = Color(0xFF6A7690)
private val M3Blue = Color(0xFF1464F4)
private val M3Green = Color(0xFF18B978)
private val M3Red = Color(0xFFE15363)
private val M3Border = Color(0xFFE5EAF2)
private val M3SoftBlue = Color(0xFFEEF4FF)
private val M3SoftGreen = Color(0xFFEAF8F3)
private val M3SoftOrange = Color(0xFFFFF4E8)

private enum class M3Section { HOME, NETWORK, TOWERS, BANDS, TOOLS }

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
                gap = if (height < 760) 12 else 15,
                scale = spec.textScale.coerceIn(1.0f, 1.12f),
                bandColumns = if (width < 350) 3 else 4
            )

            Column(Modifier.fillMaxSize()) {
                M3Header(layout, snapshot != null, onDisconnect)

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
                    M3BottomNav(section, { section = it })
                    return@Column
                }

                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = layout.padding.dp,
                            end = layout.padding.dp,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(layout.gap.dp)
                    ) {
                        compactOperationMessage(operationMessage).takeIf { it.isNotBlank() }?.let { msg ->
                            item { M3Message(layout, msg) }
                        }

                        when (section) {
                            M3Section.HOME -> {
                                item { M3NetworkCard(layout, snapshot) }
                                item { M3SpeedCard(layout, lastPerformance, speedBusy, onSpeedTest) }
                                item {
                                    M3QuickGrid(
                                        layout,
                                        onBands = { section = M3Section.BANDS },
                                        onTowers = { section = M3Section.TOWERS },
                                        onNetwork = { section = M3Section.NETWORK },
                                        onOptimize = onOptimizeNow
                                    )
                                }
                                item { M3SignalCard(layout, snapshot, telemetrySamples) }
                            }

                            M3Section.NETWORK -> {
                                item { M3NetworkCard(layout, snapshot) }
                                item { M3NetworkMode(layout, controlBusy, onSetNetworkMode) }
                                item { M3Traffic(layout, traffic) }
                                if (thermal?.hasAnyEvidence == true) item { M3Thermal(layout, thermal) }
                            }

                            M3Section.TOWERS -> {
                                item {
                                    M3TowerControls(
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

                            M3Section.BANDS -> {
                                item {
                                    M3Bands(
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

                            M3Section.TOOLS -> {
                                item {
                                    M3Tools(
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
                    M3BottomNav(section, { section = it }, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun M3Header(layout: M3Layout, connected: Boolean, onDisconnect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = layout.padding.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("ZTE Smart HAI", color = M3Ink, fontSize = m3sp(layout, 21), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(M3Card).border(1.dp, M3Border, RoundedCornerShape(50))
                .clickable(enabled = connected, onClick = onDisconnect).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) M3Green else M3Red))
            Spacer(Modifier.width(6.dp))
            Text(if (connected) "متصل" else "غير متصل", color = M3Ink, fontSize = m3sp(layout, 12), fontWeight = FontWeight.Bold)
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
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(network, color = M3Ink, fontSize = m3sp(layout, 48), fontWeight = FontWeight.Black)
                    Text(mode, color = M3Blue, fontSize = m3sp(layout, 16), fontWeight = FontWeight.Bold)
                }
                M3Pill(m3Quality(rsrp), if (rsrp == null) M3Muted else M3Green)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                M3Metric("RSRP", rsrp, "dBm", Modifier.weight(1f))
                M3Metric("SINR", sinr, "dB", Modifier.weight(1f))
                M3Metric("RSRQ", snapshot.lteRsrq, "dB", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun M3SpeedCard(layout: M3Layout, performance: NetworkPerformance?, busy: Boolean, onSpeedTest: () -> Unit) {
    M3Card(layout) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("السرعة", color = M3Ink, fontSize = m3sp(layout, 17), fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(performance?.downloadMbps?.let(::m3Fmt0) ?: "—", color = M3Ink, fontSize = m3sp(layout, 30), fontWeight = FontWeight.Black)
                    Text(" Mb/s", color = M3Muted, fontSize = m3sp(layout, 12), modifier = Modifier.padding(bottom = 4.dp))
                }
                performance?.latencyMs?.let { Text("${m3Fmt0(it)} ms", color = M3Muted, fontSize = m3sp(layout, 11)) }
            }
            Button(
                onClick = onSpeedTest,
                enabled = !busy,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = M3Blue)
            ) {
                Text(if (busy) "جاري…" else "اختبار", color = Color.White, fontSize = m3sp(layout, 13), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun M3QuickGrid(layout: M3Layout, onBands: () -> Unit, onTowers: () -> Unit, onNetwork: () -> Unit, onOptimize: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            M3Quick("▦", "الترددات", M3SoftBlue, Modifier.weight(1f), onBands)
            M3Quick("⌾", "الأبراج", M3SoftGreen, Modifier.weight(1f), onTowers)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            M3Quick("⌁", "الشبكة", M3SoftOrange, Modifier.weight(1f), onNetwork)
            M3Quick("✦", "تحسين", Color(0xFFF3EEFF), Modifier.weight(1f), onOptimize)
        }
    }
}

@Composable
private fun M3Quick(icon: String, title: String, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(20.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = M3Blue, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(10.dp))
        Text(title, color = M3Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun M3SignalCard(layout: M3Layout, snapshot: RouterSnapshot, samples: List<SafeTelemetrySample>) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    M3Card(layout) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("الإشارة", color = M3Ink, fontSize = m3sp(layout, 17), fontWeight = FontWeight.Black)
                Text(rsrp?.let { "${m3Fmt0(it)} dBm" } ?: "—", color = M3Blue, fontSize = m3sp(layout, 24), fontWeight = FontWeight.Black)
            }
            Text(if (samples.size >= 5) "${samples.size} قراءة" else "حي", color = M3Muted, fontSize = m3sp(layout, 12), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun M3NetworkMode(layout: M3Layout, busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    M3Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("وضع الشبكة", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            M3Action("4G فقط", !busy) { onSetNetworkMode("Only_LTE") }
            Spacer(Modifier.height(8.dp))
            M3Action("4G + 5G", !busy) { onSetNetworkMode("LTE_AND_5G") }
            Spacer(Modifier.height(8.dp))
            M3Action("5G فقط", !busy) { onSetNetworkMode("Only_5G") }
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
    val highest = thermal.highestObserved
    M3Card(layout) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("الحرارة", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(highest?.let { ThermalTelemetryFormatter.celsius(it.celsius) } ?: "—", color = M3Blue, fontSize = m3sp(layout, 20), fontWeight = FontWeight.Black)
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
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الأبراج", color = M3Ink, fontSize = m3sp(layout, 20), fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                M3CompactAction(if (scanBusy) "جاري…" else "مسح", !scanBusy && !busy, onScan)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                M3ValueBox("PCI", snapshot.pci?.toString() ?: "—", "", M3SoftBlue, Modifier.weight(1f))
                M3ValueBox("EARFCN", snapshot.earfcn?.toString() ?: "—", "", M3SoftGreen, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                M3Primary("تثبيت الحالية", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
                M3Outline("إزالة القفل", !busy, Modifier.weight(1f), onClear)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("حارس البرج", color = M3Ink, fontSize = m3sp(layout, 14), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(
                    checked = guardEnabled,
                    onCheckedChange = onGuardChange,
                    enabled = target != null && !busy,
                    colors = SwitchDefaults.colors(checkedTrackColor = M3Green)
                )
            }
            if (target != null) {
                Text("PCI ${target.pci} • EARFCN ${target.earfcn}", color = M3Blue, fontSize = m3sp(layout, 12), fontWeight = FontWeight.Bold)
            }
            guardStatus?.let {
                val short = if (it.match == TowerMatch.MATCHED) "مطابق" else compactOperationMessage(it.message)
                if (short.isNotBlank()) Text(short, color = if (it.match == TowerMatch.MATCHED) M3Green else M3Muted, fontSize = m3sp(layout, 11), modifier = Modifier.padding(top = 4.dp))
            }

            if (cells.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (current) M3SoftGreen else Color(0xFFF8FAFD)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(index.toString(), color = M3Blue, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = M3Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text("ARFCN ${cell.arfcn ?: "—"} • ${cell.rsrp?.let { "${m3Fmt0(it)} dBm" } ?: "—"}", color = M3Muted, fontSize = 11.sp)
        }
        if (lockable && !current) {
            Text("تثبيت", color = if (busy) M3Muted else M3Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = !busy) { onLock(cell) }.padding(8.dp))
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
            Spacer(Modifier.height(6.dp))
            M3Primary(
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
    onShareDiagnostics: () -> Unit
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
                Spacer(Modifier.height(10.dp))
                if (runtime == null) {
                    Text("غير جاهز", color = M3Muted, fontSize = m3sp(layout, 12))
                } else {
                    val ready = listOf(
                        runtime.lteBandControl,
                        runtime.nrBandControl,
                        runtime.cellLock,
                        runtime.networkMode,
                        runtime.neighborScan,
                        runtime.antennaControl
                    ).count { it.state == RuntimeCapabilityState.SAFE_TO_ATTEMPT }
                    Text("$ready وظائف جاهزة", color = M3Green, fontSize = m3sp(layout, 14), fontWeight = FontWeight.Bold)
                }
                Text(stability.summary, color = M3Muted, fontSize = m3sp(layout, 11), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    M3Outline("نسخ", true, Modifier.weight(1f), onCopyDiagnostics)
                    M3Outline("مشاركة", true, Modifier.weight(1f), onShareDiagnostics)
                }
            }
        }

        M3Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Text("الاستعادة", color = M3Ink, fontSize = m3sp(layout, 18), fontWeight = FontWeight.Black)
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
                        (1..3).forEach { state -> M3Outline(state.toString(), !busy, Modifier.weight(1f)) { onAntennaState(state) } }
                    }
                }
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
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) M3SoftBlue else Color(0xFFF1F3F6))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = if (enabled) M3Ink else M3Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("‹", color = if (enabled) M3Blue else M3Muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun M3Primary(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) M3Blue else Color(0xFFE7EAF0)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) Color.White else M3Muted, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun M3Outline(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(M3Card).border(1.dp, M3Border, RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) M3Ink else M3Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun M3CompactAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(M3SoftBlue).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(text, color = if (enabled) M3Blue else M3Muted, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun M3Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (selected) M3Blue else Color(0xFFF0F3F8)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp)) {
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
        modifier = Modifier.fillMaxWidth().shadow(7.dp, RoundedCornerShape(if (layout.compact) 22.dp else 26.dp)),
        shape = RoundedCornerShape(if (layout.compact) 22.dp else 26.dp),
        colors = CardDefaults.cardColors(containerColor = M3Card),
        border = BorderStroke(1.dp, M3Border)
    ) { content() }
}

@Composable
private fun M3BottomNav(selected: M3Section, onSelect: (M3Section) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().height(78.dp).background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        M3Nav("⌂", "الرئيسية", selected == M3Section.HOME, Modifier.weight(1f)) { onSelect(M3Section.HOME) }
        M3Nav("⌁", "الشبكة", selected == M3Section.NETWORK, Modifier.weight(1f)) { onSelect(M3Section.NETWORK) }
        M3Nav("⌾", "الأبراج", selected == M3Section.TOWERS, Modifier.weight(1f)) { onSelect(M3Section.TOWERS) }
        M3Nav("▦", "الترددات", selected == M3Section.BANDS, Modifier.weight(1f)) { onSelect(M3Section.BANDS) }
        M3Nav("⚒", "الأدوات", selected == M3Section.TOOLS, Modifier.weight(1f)) { onSelect(M3Section.TOOLS) }
    }
}

@Composable
private fun M3Nav(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected) M3Blue else M3Muted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (selected) M3Blue else M3Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, maxLines = 1)
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
