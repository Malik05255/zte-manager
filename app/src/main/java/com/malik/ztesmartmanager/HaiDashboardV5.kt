package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

private val D5Bg = Color(0xFFF4F7FB)
private val D5Card = Color.White
private val D5Ink = Color(0xFF10275C)
private val D5Muted = Color(0xFF69758A)
private val D5Blue = Color(0xFF1268F3)
private val D5BlueSoft = Color(0xFFEAF2FF)
private val D5Green = Color(0xFF16A86B)
private val D5GreenSoft = Color(0xFFE9F8F1)
private val D5Border = Color(0xFFE2E8F0)
private val D5Panel = Color(0xFFF8FAFD)
private val D5Danger = Color(0xFFC44747)

private enum class D5Section { HOME, NETWORK, TOOLS, LOGS, MORE, TOWERS, BANDS }

@Composable
fun HaiDashboardV5(
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
        var section by rememberSaveable { mutableStateOf(D5Section.HOME) }

        if (snapshot != null && section == D5Section.HOME) {
            HaiHomeDashboardV5(
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
                onNavigateNetwork = { section = D5Section.NETWORK },
                onNavigateTowers = { section = D5Section.TOWERS },
                onNavigateBands = { section = D5Section.BANDS },
                onNavigateTools = { section = D5Section.TOOLS },
                onNavigateLogs = { section = D5Section.LOGS },
                onNavigateMore = { section = D5Section.MORE },
                onRefreshNow = onRefreshSnapshot,
                onOptimizeNow = onOptimizeNow
            )
        } else {
            Column(Modifier.fillMaxSize().background(D5Bg)) {
                HaiSharedHeader(
                    connected = snapshot != null,
                    onDisconnect = onDisconnect,
                    onMenu = { section = D5Section.MORE },
                    onSettings = { section = D5Section.TOOLS },
                    onSearch = { section = D5Section.TOWERS }
                )

                if (snapshot == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (status.isBlank() || status == "غير متصل") "جاري انتظار قراءة الراوتر…" else status,
                            color = D5Muted,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    D5ScreenContent(
                        modifier = Modifier.weight(1f),
                        section = section,
                        snapshot = snapshot,
                        capabilities = capabilities,
                        runtime = runtime,
                        traffic = traffic,
                        thermal = thermal,
                        telemetrySamples = telemetrySamples,
                        stability = stability,
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
                        lastPerformance = lastPerformance,
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
                        onRefreshSnapshot = onRefreshSnapshot,
                        onNavigate = { section = it }
                    )
                }

                D5BottomNav(section) { section = it }
            }
        }
    }
}

@Composable
private fun D5ScreenContent(
    modifier: Modifier,
    section: D5Section,
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    runtime: RuntimeCapabilityReport?,
    traffic: TrafficTelemetry?,
    thermal: ThermalTelemetry?,
    telemetrySamples: List<SafeTelemetrySample>,
    stability: ConnectionStabilityReport,
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
    lastPerformance: NetworkPerformance?,
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
    onRefreshSnapshot: () -> Unit,
    onNavigate: (D5Section) -> Unit
) {
    val ui = LocalHaiUiMetrics.current
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = ui.pagePadding,
            end = ui.pagePadding,
            top = 4.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(ui.sectionGap)
    ) {
        if (operationMessage.isNotBlank()) {
            item { D5Message(operationMessage) }
        }

        when (section) {
            D5Section.HOME -> Unit
            D5Section.NETWORK -> {
                item { D5PageTitle("الشبكة", "تفاصيل الاتصال الفعلي ووضع الشبكة") }
                item { D5NetworkOverview(snapshot) }
                item { D5NetworkMode(snapshot, controlBusy, onSetNetworkMode) }
                item { D5TrafficCard(traffic) }
                if (thermal?.hasAnyEvidence == true) item { D5ThermalCard(thermal) }
            }
            D5Section.TOOLS -> {
                item { D5PageTitle("الأدوات", "تحسين، تشخيص، استعادة وإعدادات موثقة") }
                item {
                    D5SmartCard(
                        smartMode = smartMode,
                        smartGoal = smartGoal,
                        smartBusy = smartBusy,
                        smartReport = smartReport,
                        onSmartModeChange = onSmartModeChange,
                        onSmartGoalChange = onSmartGoalChange,
                        onOptimizeNow = onOptimizeNow
                    )
                }
                item { D5PlacementCard(placementMode, placementReading, onPlacementToggle) }
                item { D5DiagnosticsCard(runtime, stability, controlBusy, onCopyDiagnostics, onShareDiagnostics, onRefreshSnapshot) }
                item { D5RestoreCard(safetyBackupAvailable, controlBusy, onRestoreSafetyBackup) }
                if (capabilities.supportsAntennaControl) {
                    item { D5AntennaCard(controlBusy, onAntennaState) }
                }
            }
            D5Section.LOGS -> {
                item { D5PageTitle("السجلات الحية", "آخر القراءات التي استلمها التطبيق من الراوتر") }
                lastPerformance?.let { item { D5PerformanceSummary(it) } }
                if (telemetrySamples.isEmpty()) {
                    item { D5EmptyCard("لا توجد قراءات بعد") }
                } else {
                    items(telemetrySamples.asReversed().take(30), key = { it.timestampEpochMs }) { sample ->
                        D5LogRow(sample)
                    }
                }
            }
            D5Section.MORE -> {
                item { D5PageTitle("المزيد", snapshot.model ?: "ZTE Smart HAI") }
                item { D5RouterInfo(snapshot) }
                item { D5NavTile("الأبراج والخريطة", "مسح الخلايا والقفل الموثق") { onNavigate(D5Section.TOWERS) } }
                item { D5NavTile("الترددات", "4G / 5G وقفل النطاقات") { onNavigate(D5Section.BANDS) } }
                item { D5NavTile("تفاصيل الشبكة", "الإشارة، الحركة ووضع الشبكة") { onNavigate(D5Section.NETWORK) } }
                item { D5NavTile("الأدوات والتشخيص", "التحسين، الاستعادة والتقارير") { onNavigate(D5Section.TOOLS) } }
            }
            D5Section.TOWERS -> {
                item { D5PageTitle("الأبراج والخلايا", "لا يتم وصف خلية بأنها حالية أو مقفلة إلا بعد قراءة فعلية") }
                item {
                    D5TowerControlCard(
                        snapshot = snapshot,
                        busy = controlBusy,
                        scanBusy = scanBusy,
                        target = towerTarget,
                        guardEnabled = towerGuardEnabled,
                        guardStatus = towerGuardStatus,
                        onScan = onScanCells,
                        onLockCurrent = onLockCurrentCell,
                        onClear = onClearCellLock,
                        onGuardChange = onTowerGuardChange
                    )
                }
                item {
                    VerifiedTowerMapPanel(
                        snapshot = snapshot,
                        cells = nearbyCells,
                        busy = controlBusy,
                        onLockCell = onLockNearbyCell
                    )
                }
                if (nearbyCells.isNotEmpty()) {
                    item { D5SectionLabel("الخلايا المكتشفة") }
                    items(nearbyCells.take(16)) { cell ->
                        D5CellRow(snapshot, cell, controlBusy, onLockNearbyCell)
                    }
                }
            }
            D5Section.BANDS -> {
                item { D5PageTitle("الترددات", "الاختيار لا يعني نجاح القفل؛ النجاح يعتمد على قراءة الراوتر بعد التطبيق") }
                item {
                    D5BandsCard(
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
    }
}

@Composable
private fun D5NetworkOverview(snapshot: RouterSnapshot) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val type = when { nr -> "5G"; lte -> "4G"; else -> "غير مؤكد" }
    val rsrp = if (nr) snapshot.nrRsrp else if (lte) snapshot.lteRsrp else null
    val sinr = if (nr) snapshot.nrSinr else if (lte) snapshot.lteSinr else null
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(type, color = if (nr || lte) D5Blue else D5Muted, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Text(snapshot.networkType ?: "نوع الشبكة غير مؤكد", color = D5Muted, fontSize = 12.sp)
                }
                D5Pill(if (nr || lte) "قراءة موثقة" else "غير مؤكد", nr || lte)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                D5Metric("RSRP", rsrp?.let(::d5Fmt1) ?: "—", "dBm", Modifier.weight(1f))
                D5Metric("SINR", sinr?.let(::d5Fmt1) ?: "—", "dB", Modifier.weight(1f))
                D5Metric("RSRQ", snapshot.lteRsrq?.let(::d5Fmt1) ?: "—", "dB", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                D5Metric("PCI", snapshot.pci?.toString() ?: "—", "", Modifier.weight(1f))
                D5Metric("EARFCN", snapshot.earfcn?.toString() ?: "—", "", Modifier.weight(1f))
                D5Metric("CELL", snapshot.cellId?.toString() ?: "—", "", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun D5NetworkMode(snapshot: RouterSnapshot, busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    val current = snapshot.raw["BearerPreference"].orEmpty()
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Text("وضع الشبكة", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                D5ModeButton("تلقائي", current == "WL_AND_5G" || current == "LTE_AND_5G", !busy, Modifier.weight(1f)) { onSetNetworkMode("WL_AND_5G") }
                D5ModeButton("5G فقط", current == "Only_5G", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_5G") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                D5ModeButton("4G فقط", current == "Only_LTE", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_LTE") }
                D5ModeButton("3G فقط", current == "Only_WCDMA", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_WCDMA") }
            }
        }
    }
}

@Composable
private fun D5TrafficCard(traffic: TrafficTelemetry?) {
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Text("حركة البيانات", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                D5Value("تنزيل", TrafficTelemetryFormatter.rateMbps(traffic?.rxBytesPerSecond), "Mb/s", Modifier.weight(1f))
                D5Value("رفع", TrafficTelemetryFormatter.rateMbps(traffic?.txBytesPerSecond), "Mb/s", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun D5ThermalCard(thermal: ThermalTelemetry) {
    D5Card {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("حرارة المودم", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("أعلى قراءة ظاهرة من الراوتر", color = D5Muted, fontSize = 11.sp)
            }
            Text(
                thermal.highestObserved?.let { ThermalTelemetryFormatter.celsius(it.celsius) } ?: "—",
                color = D5Blue,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun D5SmartCard(
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit
) {
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("التحسين الذكي", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("لا يثبت نتيجة إلا بعد اختبار وقراءة راجعة", color = D5Muted, fontSize = 11.sp)
                }
                Switch(
                    checked = smartMode,
                    onCheckedChange = onSmartModeChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = D5Green)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptimizationGoal.entries.forEach { goal ->
                    D5Choice(d5Goal(goal), smartGoal == goal) { onSmartGoalChange(goal) }
                }
            }
            Spacer(Modifier.height(12.dp))
            D5Primary(if (smartBusy) "جاري الاختبار…" else "تحسين الآن", !smartBusy, Modifier.fillMaxWidth(), onOptimizeNow)
            smartReport?.let {
                Text("آخر نتيجة موثقة: ${it.best.qualityScore}/100", color = D5Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun D5PlacementCard(placementMode: Boolean, reading: PlacementReading?, onToggle: () -> Unit) {
    D5Card {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("أفضل مكان للراوتر", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    reading?.let { "التقييم الحالي ${it.score.total}/100" } ?: "فعّل القياس أثناء تحريك الراوتر",
                    color = if (reading != null) D5Blue else D5Muted,
                    fontSize = 12.sp,
                    fontWeight = if (reading != null) FontWeight.Bold else FontWeight.Normal
                )
            }
            Switch(checked = placementMode, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = D5Green))
        }
    }
}

@Composable
private fun D5DiagnosticsCard(
    runtime: RuntimeCapabilityReport?,
    stability: ConnectionStabilityReport,
    busy: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRefresh: () -> Unit
) {
    val ready = runtime?.let {
        listOf(it.lteBandControl, it.nrBandControl, it.cellLock, it.networkMode, it.neighborScan, it.antennaControl)
            .count { capability -> capability.state == RuntimeCapabilityState.SAFE_TO_ATTEMPT }
    }
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Text("التشخيص", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Text(
                ready?.let { "$it وظائف جاهزة للمحاولة وفق فحص الـRuntime" } ?: "فحص القدرات غير جاهز",
                color = if (ready != null) D5Green else D5Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(stability.summary, color = D5Muted, fontSize = 11.sp, lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                D5Outline("نسخ التقرير", true, Modifier.weight(1f), onCopy)
                D5Outline("مشاركة التقرير", true, Modifier.weight(1f), onShare)
            }
            Spacer(Modifier.height(8.dp))
            D5Outline("تحديث القراءة من الراوتر", !busy, Modifier.fillMaxWidth(), onRefresh)
        }
    }
}

@Composable
private fun D5RestoreCard(available: Boolean, busy: Boolean, onRestore: () -> Unit) {
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Text("الاستعادة الآمنة", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("الرجوع لآخر حالة محفوظة قبل التغيير", color = D5Muted, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            D5Primary("استعادة النسخة", available && !busy, Modifier.fillMaxWidth(), onRestore)
        }
    }
}

@Composable
private fun D5AntennaCard(busy: Boolean, onAntennaState: (Int) -> Unit) {
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Text("الهوائي", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { state ->
                    D5Outline("وضع $state", !busy, Modifier.weight(1f)) { onAntennaState(state) }
                }
            }
        }
    }
}

@Composable
private fun D5PerformanceSummary(performance: NetworkPerformance) {
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Text("آخر اختبار سرعة", color = D5Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                D5Metric("تنزيل", performance.downloadMbps?.let(::d5Fmt1) ?: "—", "Mb/s", Modifier.weight(1f))
                D5Metric("رفع", performance.uploadMbps?.let(::d5Fmt1) ?: "—", "Mb/s", Modifier.weight(1f))
                D5Metric("Ping", performance.latencyMs?.let(::d5Fmt0) ?: "—", "ms", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun D5LogRow(sample: SafeTelemetrySample) {
    D5Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${sample.networkType ?: "غير مؤكد"} • ${sample.nrBand ?: sample.lteBand ?: "لا يوجد تردد موثّق"}",
                    color = D5Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "RSRP ${sample.nrRsrp ?: sample.lteRsrp ?: "—"} • SINR ${sample.nrSinr ?: sample.lteSinr ?: "—"} • PCI ${sample.pci ?: "—"}",
                    color = D5Muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Text("EARFCN ${sample.earfcn ?: "—"}", color = D5Muted, fontSize = 11.sp)
            }
            Text(d5Time(sample.timestampEpochMs), color = D5Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun D5RouterInfo(snapshot: RouterSnapshot) {
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Text(snapshot.model ?: "ZTE", color = D5Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            snapshot.firmware?.let { Text("Firmware: $it", color = D5Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            snapshot.hardwareVersion?.let { Text("Hardware: $it", color = D5Muted, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun D5TowerControlCard(
    snapshot: RouterSnapshot,
    busy: Boolean,
    scanBusy: Boolean,
    target: TowerTarget?,
    guardEnabled: Boolean,
    guardStatus: TowerGuardStatus?,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onClear: () -> Unit,
    onGuardChange: (Boolean) -> Unit
) {
    D5Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الخلية الحالية", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("PCI ${snapshot.pci ?: "—"} • EARFCN ${snapshot.earfcn ?: "—"}", color = D5Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                D5Primary(if (scanBusy) "جاري المسح…" else "مسح الخلايا", !scanBusy && !busy, Modifier.width(118.dp), onScan)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                D5Primary("تثبيت الحالية", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
                D5Outline("إزالة القفل", !busy, Modifier.weight(1f), onClear)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حارس البرج", color = D5Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text("يعمل فقط عند وجود هدف تم التحقق منه", color = D5Muted, fontSize = 10.sp)
                }
                Switch(
                    checked = guardEnabled,
                    onCheckedChange = onGuardChange,
                    enabled = target != null && !busy,
                    colors = SwitchDefaults.colors(checkedTrackColor = D5Green)
                )
            }
            target?.let {
                Text("الهدف: PCI ${it.pci} • EARFCN ${it.earfcn}", color = D5Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            guardStatus?.let {
                Text(
                    if (it.match == TowerMatch.MATCHED) "القفل مطابق للقراءة الحية" else it.message,
                    color = if (it.match == TowerMatch.MATCHED) D5Green else D5Muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun D5CellRow(snapshot: RouterSnapshot, cell: NearbyCell, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val current = cell.rat == "LTE" && cell.pci == snapshot.pci && cell.arfcn == snapshot.earfcn
    val lockable = cell.rat == "LTE" && cell.pci != null && cell.arfcn != null
    D5Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(if (current) D5Green else D5Blue.copy(alpha = .55f), CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = D5Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("ARFCN ${cell.arfcn ?: "—"} • RSRP ${cell.rsrp?.let(::d5Fmt0) ?: "—"}", color = D5Muted, fontSize = 11.sp)
            }
            when {
                current -> D5Pill("الحالية", true)
                lockable -> D5InlineAction("تثبيت", !busy) { onLock(cell) }
                else -> Text("غير قابلة للقفل", color = D5Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun D5BandsCard(
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
    var nr by rememberSaveable { mutableStateOf(false) }
    val bands = if (nr) capabilities.supportedNrBands.sorted() else capabilities.supportedLteBands.sorted()
    val selected = if (nr) selectedNr else selectedLte
    val active = remember(snapshot.cells, nr) {
        snapshot.cells.filter { if (nr) it.role == CellRole.NR else it.role != CellRole.NR }
            .mapNotNull { d5BandNumber(it.band) }.toSet()
    }

    D5Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("اختيار النطاقات", color = D5Ink, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    D5Choice("4G", !nr) { nr = false }
                    D5Choice("5G", nr) { nr = true }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (bands.isEmpty()) {
                Text("هذا الـProfile لم يعلن نطاقات قابلة للتحكم هنا", color = D5Muted, fontSize = 12.sp)
            } else {
                bands.chunked(4).forEach { chunk ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunk.forEach { band ->
                            D5BandButton(
                                text = "${if (nr) "N" else "B"}$band",
                                selected = band in selected,
                                active = band in active,
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                            ) { if (nr) onNrToggle(band) else onLteToggle(band) }
                        }
                        repeat(4 - chunk.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            D5Primary(
                text = if (nr) "تطبيق اختيار 5G" else "تطبيق اختيار 4G",
                enabled = selected.isNotEmpty() && !busy && (!nr || capabilities.supportsNrBandLock),
                modifier = Modifier.fillMaxWidth()
            ) { if (nr) onApplyNr() else onApplyLte() }
        }
    }
}

@Composable
private fun D5PageTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)) {
        Text(title, color = D5Ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = D5Muted, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun D5SectionLabel(text: String) {
    Text(text, color = D5Ink, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
}

@Composable
private fun D5NavTile(title: String, subtitle: String, onClick: () -> Unit) {
    D5Card {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).background(D5Blue, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = D5Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = D5Muted, fontSize = 11.sp)
            }
            Text("‹", color = D5Blue, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun D5EmptyCard(text: String) {
    D5Card {
        Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
            Text(text, color = D5Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun D5Message(text: String) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(D5BlueSoft)
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(text, color = D5Ink, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun D5Card(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalHaiUiMetrics.current.cardRadius),
        colors = CardDefaults.cardColors(containerColor = D5Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, D5Border)
    ) { content() }
}

@Composable
private fun D5Metric(label: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(D5Panel).padding(vertical = 11.dp, horizontal = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = D5Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = D5Ink, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (unit.isNotBlank()) Text(unit, color = D5Muted, fontSize = 9.sp)
    }
}

@Composable
private fun D5Value(title: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(D5Panel).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = D5Muted, fontSize = 11.sp)
        Text(value, color = D5Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(unit, color = D5Muted, fontSize = 10.sp)
    }
}

@Composable
private fun D5Primary(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(15.dp))
            .background(if (enabled) D5Blue else Color(0xFFE6E9EF))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.White else D5Muted, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun D5Outline(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(15.dp)).background(D5Card)
            .border(1.dp, D5Border, RoundedCornerShape(15.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) D5Ink else D5Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun D5ModeButton(text: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(15.dp))
            .background(if (selected) D5Blue else if (enabled) D5Panel else Color(0xFFEDEFF3))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else if (enabled) D5Ink else D5Muted, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun D5Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) D5Blue else D5Panel)
            .border(1.dp, if (selected) D5Blue else D5Border, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text, color = if (selected) Color.White else D5Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun D5BandButton(text: String, selected: Boolean, active: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(54.dp).clip(RoundedCornerShape(15.dp))
            .background(if (active) D5GreenSoft else if (selected) D5BlueSoft else D5Panel)
            .border(1.dp, if (active) D5Green else if (selected) D5Blue else D5Border, RoundedCornerShape(15.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, color = D5Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
            if (active) Text("نشط", color = D5Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun D5Pill(text: String, good: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (good) D5GreenSoft else D5Panel)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(if (good) D5Green else D5Muted, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (good) D5Green else D5Muted, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun D5InlineAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (enabled) D5Blue else D5Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(8.dp)
    )
}

@Composable
private fun D5BottomNav(selected: D5Section, onSelect: (D5Section) -> Unit) {
    HaiSharedBottomNav(
        selected = when (selected) {
            D5Section.HOME -> "home"
            D5Section.NETWORK -> "network"
            D5Section.TOOLS -> "tools"
            D5Section.LOGS -> "logs"
            D5Section.MORE, D5Section.TOWERS, D5Section.BANDS -> "more"
        },
        onHome = { onSelect(D5Section.HOME) },
        onNetwork = { onSelect(D5Section.NETWORK) },
        onTools = { onSelect(D5Section.TOOLS) },
        onLogs = { onSelect(D5Section.LOGS) },
        onMore = { onSelect(D5Section.MORE) }
    )
}

private fun d5Goal(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun d5BandNumber(value: String?): Int? = value?.trim()
    ?.removePrefix("n")?.removePrefix("N")?.removePrefix("b")?.removePrefix("B")?.toIntOrNull()

private fun d5Fmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
private fun d5Fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun d5Time(epochMs: Long): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(epochMs))
