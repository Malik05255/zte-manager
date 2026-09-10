package com.malik.ztesmartmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityAnalyzer
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityLevel
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetryHistory
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.SupportBundleBuilder
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetryFormatter
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetryParser
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState
import com.malik.ztesmartmanager.core.presentation.ResponsiveLayoutPolicy
import com.malik.ztesmartmanager.core.presentation.RuntimeCapabilityDetail
import com.malik.ztesmartmanager.core.presentation.RuntimeCapabilityDetailsPresenter
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

/** Runtime-capability shell around the premium dashboard. */
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
    val configuration = LocalConfiguration.current
    val layout = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        ResponsiveLayoutPolicy.resolve(configuration.screenWidthDp, configuration.screenHeightDp)
    }
    val runtime = remember(snapshot?.raw, capabilities) {
        snapshot?.let { runtimeCapabilitiesFromSnapshot(capabilities, it.raw) }
    }
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }

    val telemetryHistory = remember { SafeTelemetryHistory(capacity = 30) }
    var telemetrySamples by remember { mutableStateOf<List<SafeTelemetrySample>>(emptyList()) }
    LaunchedEffect(snapshot) {
        snapshot?.let {
            telemetryHistory.add(it)
            telemetrySamples = telemetryHistory.snapshot()
        }
    }
    val stability = remember(telemetrySamples) { ConnectionStabilityAnalyzer.analyze(telemetrySamples) }
    val traffic = remember(snapshot?.raw) { snapshot?.let { TrafficTelemetryParser.parse(it.raw) } }

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
        clipboard.setPrimaryClip(ClipData.newPlainText("ZTE Manager diagnostics", supportBundle))
        Toast.makeText(context, "تم نسخ تقرير التشخيص الآمن", Toast.LENGTH_SHORT).show()
    }

    fun shareSupportBundle() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ZTE Manager ${BuildConfig.VERSION_NAME} diagnostics")
            putExtra(Intent.EXTRA_TEXT, supportBundle)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة تقرير التشخيص"))
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
        RuntimeCapabilityBar(
            report = runtime,
            stability = stability,
            expanded = detailsExpanded,
            compact = layout.compact,
            outerVerticalDp = layout.capabilityOuterVerticalDp,
            innerVerticalDp = layout.capabilityInnerVerticalDp,
            onToggle = { if (runtime != null) detailsExpanded = !detailsExpanded },
            modifier = Modifier.fillMaxWidth()
        )
        RuntimeTrafficStrip(
            traffic = traffic,
            compact = layout.compact,
            ultraCompact = layout.ultraCompact,
            innerVerticalDp = layout.trafficInnerVerticalDp,
            modifier = Modifier.fillMaxWidth()
        )
        SignalHistoryCard(
            samples = telemetrySamples,
            modifier = Modifier.fillMaxWidth(),
            compact = layout.compact,
            ultraCompact = layout.ultraCompact,
            plotHeightDp = layout.signalPlotHeightDp,
            innerVerticalDp = layout.signalInnerVerticalDp
        )
        if (detailsExpanded && runtime != null) {
            RuntimeCapabilityDetails(
                report = runtime,
                stability = stability,
                historyCount = telemetrySamples.size,
                maxHeightDp = layout.detailsMaxHeightDp,
                compact = layout.compact,
                onCopyBundle = ::copySupportBundle,
                onShareBundle = ::shareSupportBundle,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
                onRestoreSafetyBackup = onRestoreSafetyBackup
            )
        }
    }
}

@Composable
private fun RuntimeTrafficStrip(
    traffic: TrafficTelemetry?,
    compact: Boolean,
    ultraCompact: Boolean,
    innerVerticalDp: Int,
    modifier: Modifier = Modifier
) {
    val available = traffic?.hasAnyEvidence == true
    Card(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 1.dp),
        shape = RoundedCornerShape(if (compact) 18.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = RuntimeBarBg)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = innerVerticalDp.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حركة البيانات", color = RuntimeBarInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(
                        when {
                            !available -> "عدادات Traffic غير متاحة على هذا الـFirmware"
                            compact -> "عدادات الراوتر مباشرة • ليست Speed Test"
                            else -> "قراءة مباشرة من عدادات الراوتر • ليست Speed Test"
                        },
                        color = RuntimeBarMuted,
                        fontSize = 6.sp,
                        maxLines = 1
                    )
                }
                Text(
                    if (available) "READ ONLY" else "UNAVAILABLE",
                    color = if (available) RuntimeBarGood else RuntimeBarMuted,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Black
                )
            }

            if (available && traffic != null) {
                Spacer(Modifier.size(if (compact) 3.dp else 5.dp))
                if (ultraCompact) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TrafficMetric(
                            title = "↓ الآن",
                            value = TrafficTelemetryFormatter.rateMbps(traffic.rxBytesPerSecond),
                            suffix = "Mb/s",
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                        TrafficMetric(
                            title = "↑ الآن",
                            value = TrafficTelemetryFormatter.rateMbps(traffic.txBytesPerSecond),
                            suffix = "Mb/s",
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.size(3.dp))
                    TrafficMetric(
                        title = traffic.monthMarker?.let { "الشهر $it" } ?: "هذا الشهر",
                        value = "↓ ${TrafficTelemetryFormatter.bytes(traffic.monthlyRxBytes)}",
                        suffix = "↑ ${TrafficTelemetryFormatter.bytes(traffic.monthlyTxBytes)}",
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TrafficMetric(
                            title = "↓ الآن",
                            value = TrafficTelemetryFormatter.rateMbps(traffic.rxBytesPerSecond),
                            suffix = "Mb/s",
                            compact = compact,
                            modifier = Modifier.weight(1f)
                        )
                        TrafficMetric(
                            title = "↑ الآن",
                            value = TrafficTelemetryFormatter.rateMbps(traffic.txBytesPerSecond),
                            suffix = "Mb/s",
                            compact = compact,
                            modifier = Modifier.weight(1f)
                        )
                        TrafficMetric(
                            title = traffic.monthMarker?.let { "الشهر $it" } ?: "هذا الشهر",
                            value = "↓ ${TrafficTelemetryFormatter.bytes(traffic.monthlyRxBytes)}",
                            suffix = "↑ ${TrafficTelemetryFormatter.bytes(traffic.monthlyTxBytes)}",
                            compact = compact,
                            modifier = Modifier.weight(1.45f)
                        )
                    }
                }
                val sessionEvidence = traffic.sessionRxBytes != null || traffic.sessionTxBytes != null || traffic.sessionSeconds != null
                if (sessionEvidence) {
                    Text(
                        "الجلسة: ↓ ${TrafficTelemetryFormatter.bytes(traffic.sessionRxBytes)} • ↑ ${TrafficTelemetryFormatter.bytes(traffic.sessionTxBytes)} • ${TrafficTelemetryFormatter.duration(traffic.sessionSeconds)}",
                        color = RuntimeBarMuted,
                        fontSize = 6.sp,
                        modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TrafficMetric(
    title: String,
    value: String,
    suffix: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, RuntimeBarGoldDeep.copy(alpha = 0.22f), RoundedCornerShape(if (compact) 10.dp else 12.dp))
            .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = if (compact) 3.dp else 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = RuntimeBarMuted, fontSize = 6.sp, maxLines = 1)
        Text(value, color = RuntimeBarInk, fontSize = if (compact) 8.sp else 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(suffix, color = RuntimeBarGoldDeep, fontSize = 6.sp, maxLines = 1)
    }
}

@Composable
private fun RuntimeCapabilityBar(
    report: RuntimeCapabilityReport?,
    stability: ConnectionStabilityReport,
    expanded: Boolean,
    compact: Boolean,
    outerVerticalDp: Int,
    innerVerticalDp: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = outerVerticalDp.dp)
            .clickable(enabled = report != null, onClick = onToggle),
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        colors = CardDefaults.cardColors(containerColor = RuntimeBarBg)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = innerVerticalDp.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("جاهزية Firmware", color = RuntimeBarInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (report == null) {
                            "جاري جمع دليل read-back..."
                        } else if (compact) {
                            "${RuntimeCapabilityAccess.writeReadyCount(report)}/${RuntimeCapabilityAccess.writeActionCount()} جاهزة • ${runtimeStabilityHeadline(stability)} • ${if (expanded) "إخفاء" else "تفاصيل"}"
                        } else {
                            "${RuntimeCapabilityAccess.writeReadyCount(report)}/${RuntimeCapabilityAccess.writeActionCount()} مسارات كتابة جاهزة • ${runtimeStabilityHeadline(stability)} • ${if (expanded) "إخفاء التفاصيل" else "اضغط للتفاصيل"}"
                        },
                        color = RuntimeBarMuted,
                        fontSize = 7.sp,
                        maxLines = 1
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
                Spacer(Modifier.size(if (compact) 3.dp else 5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)) {
                    RuntimeStateChip("4G", report.lteBandControl.state, compact, Modifier.weight(1f))
                    RuntimeStateChip("5G", report.nrBandControl.state, compact, Modifier.weight(1f))
                    RuntimeStateChip("CELL", report.cellLock.state, compact, Modifier.weight(1f))
                    RuntimeStateChip("MODE", report.networkMode.state, compact, Modifier.weight(1f))
                    RuntimeStateChip("SCAN", report.neighborScan.state, compact, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RuntimeCapabilityDetails(
    report: RuntimeCapabilityReport,
    stability: ConnectionStabilityReport,
    historyCount: Int,
    maxHeightDp: Int,
    compact: Boolean,
    onCopyBundle: () -> Unit,
    onShareBundle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val details = remember(report) { RuntimeCapabilityDetailsPresenter.from(report) }
    Card(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 1.dp),
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        colors = CardDefaults.cardColors(containerColor = RuntimeBarBg)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = if (compact) 5.dp else 8.dp)) {
            Text("تفاصيل دليل الـFirmware", color = RuntimeBarInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(
                if (compact) "النجاح لا يعلن إلا بعد read-back مطابق. جميع التفاصيل أدناه قابلة للتمرير."
                else "«جاهز للمحاولة» لا يعني نجاح الأمر مسبقًا؛ قبل أي كتابة يعاد probe ثم لا يعلن النجاح إلا بعد read-back مطابق.",
                color = RuntimeBarMuted,
                fontSize = 7.sp
            )
            Spacer(Modifier.size(if (compact) 3.dp else 5.dp))
            RuntimeStabilitySummary(stability, compact)
            Spacer(Modifier.size(if (compact) 3.dp else 5.dp))
            Text(
                "تقرير الدعم منظم وآمن؛ لا ينسخ raw كاملًا ويحتفظ بأقصى حد 30 قراءة من الجلسة الحالية.",
                color = RuntimeBarMuted,
                fontSize = 7.sp,
                maxLines = if (compact) 1 else 2
            )
            Spacer(Modifier.size(if (compact) 3.dp else 5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RuntimeActionButton("نسخ التقرير • $historyCount قراءة", Modifier.weight(1f), onCopyBundle, compact)
                RuntimeActionButton("مشاركة التقرير", Modifier.weight(1f), onShareBundle, compact)
            }
            Spacer(Modifier.size(if (compact) 3.dp else 6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeightDp.dp),
                contentPadding = PaddingValues(bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp)
            ) {
                items(details, key = { it.key }) { detail -> RuntimeCapabilityDetailRow(detail, compact) }
            }
        }
    }
}

@Composable
private fun RuntimeStabilitySummary(report: ConnectionStabilityReport, compact: Boolean) {
    val color = when (report.level) {
        ConnectionStabilityLevel.STABLE -> RuntimeBarGood
        ConnectionStabilityLevel.VARIABLE -> RuntimeBarGoldDeep
        ConnectionStabilityLevel.UNSTABLE -> RuntimeBarWarn
        ConnectionStabilityLevel.INSUFFICIENT -> RuntimeBarMuted
    }
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(if (compact) 11.dp else 14.dp))
            .padding(horizontal = 8.dp, vertical = if (compact) 4.dp else 6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ثبات الجلسة", color = RuntimeBarInk, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(runtimeStabilityHeadline(report), color = color, fontSize = 7.sp, fontWeight = FontWeight.Black)
        }
        Text(report.summary, color = RuntimeBarMuted, fontSize = 6.sp, maxLines = if (compact) 1 else 2)
        if (report.score != null) {
            val primary = buildList {
                report.cellStabilityPercent?.let { add("الخلية $it%") }
                report.modeStabilityPercent?.let { add("الوضع $it%") }
                report.signalStabilityPercent?.let { add("الإشارة $it%") }
            }
            val presence = buildList {
                report.nrActivePercent?.let { add("5G نشط $it%") }
                report.caActivePercent?.let { add("CA نشط $it%") }
            }
            if (primary.isNotEmpty()) Text(primary.joinToString(" • "), color = RuntimeBarGoldDeep, fontSize = 6.sp, maxLines = 1)
            if (presence.isNotEmpty()) Text(presence.joinToString(" • "), color = RuntimeBarMuted, fontSize = 6.sp, maxLines = 1)
        }
    }
}

private fun runtimeStabilityHeadline(report: ConnectionStabilityReport): String = when (report.level) {
    ConnectionStabilityLevel.STABLE -> "ثابت ${report.score ?: "—"}/100"
    ConnectionStabilityLevel.VARIABLE -> "متذبذب ${report.score ?: "—"}/100"
    ConnectionStabilityLevel.UNSTABLE -> "غير مستقر ${report.score ?: "—"}/100"
    ConnectionStabilityLevel.INSUFFICIENT -> "الثبات: ${report.sampleCount}/5 قراءات"
}

@Composable
private fun RuntimeActionButton(text: String, modifier: Modifier, onClick: () -> Unit, compact: Boolean) {
    Box(
        modifier = modifier
            .border(1.dp, RuntimeBarGoldDeep.copy(alpha = 0.40f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = if (compact) 4.dp else 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = RuntimeBarGoldDeep, fontSize = 7.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun RuntimeCapabilityDetailRow(detail: RuntimeCapabilityDetail, compact: Boolean) {
    val color = runtimeStateColor(detail.state)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(if (compact) 11.dp else 14.dp))
            .padding(horizontal = 8.dp, vertical = if (compact) 4.dp else 6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(detail.title, color = RuntimeBarInk, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(detail.accessKind, color = RuntimeBarMuted, fontSize = 6.sp)
            Spacer(Modifier.size(6.dp))
            Text(detail.stateLabel, color = color, fontSize = 7.sp, fontWeight = FontWeight.Black)
        }
        Text(detail.reason, color = RuntimeBarMuted, fontSize = 6.sp, maxLines = if (compact) 1 else 2)
        if (detail.evidenceFields.isNotEmpty()) {
            Text(
                "الدليل: ${detail.evidenceFields.joinToString(" • ")}",
                color = RuntimeBarGoldDeep,
                fontSize = 6.sp,
                maxLines = if (compact) 1 else 2
            )
        }
    }
}

@Composable
private fun RuntimeStateChip(label: String, state: RuntimeCapabilityState, compact: Boolean, modifier: Modifier) {
    val text = when (state) {
        RuntimeCapabilityState.SAFE_TO_ATTEMPT -> "جاهز"
        RuntimeCapabilityState.READ_ONLY -> "قراءة"
        RuntimeCapabilityState.PROFILE_ONLY -> "Profile"
        RuntimeCapabilityState.UNAVAILABLE -> "غير متاح"
    }
    val color = runtimeStateColor(state)
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(if (compact) 9.dp else 12.dp))
            .padding(horizontal = 2.dp, vertical = if (compact) 2.dp else 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = RuntimeBarInk, fontSize = 6.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text, color = color, fontSize = 6.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

private fun runtimeStateColor(state: RuntimeCapabilityState): Color = when (state) {
    RuntimeCapabilityState.SAFE_TO_ATTEMPT -> RuntimeBarGood
    RuntimeCapabilityState.READ_ONLY -> RuntimeBarGoldDeep
    RuntimeCapabilityState.PROFILE_ONLY -> RuntimeBarWarn
    RuntimeCapabilityState.UNAVAILABLE -> RuntimeBarMuted
}
