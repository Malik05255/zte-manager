package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityEvidence
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState
import com.malik.ztesmartmanager.core.presentation.HaiLayoutSpec
import com.malik.ztesmartmanager.core.presentation.HaiResponsivePolicy
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.CellConfidence
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerMatch
import com.malik.ztesmartmanager.core.tower.TowerRecommendationDecision
import com.malik.ztesmartmanager.core.tower.TowerRecommendationEngine
import com.malik.ztesmartmanager.core.tower.TowerTarget
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val ArBg = Color(0xFFF8FAFE)
private val ArCard = Color.White
private val ArInk = Color(0xFF09245D)
private val ArMuted = Color(0xFF657397)
private val ArBlue = Color(0xFF1665F5)
private val ArDeep = Color(0xFF073C9D)
private val ArMint = Color(0xFF17DCA3)
private val ArGreen = Color(0xFF13C966)
private val ArRed = Color(0xFFE3515C)
private val ArBorder = Color(0xFFE8EDF6)
private val ArSoft = Color(0xFFF0F5FF)
private val ArPurple = Color(0xFF9A50E8)

private enum class ArSection { HOME, NETWORK, TOWERS, BANDS, TOOLS }
private enum class ArBandFilter { ALL, NR, LTE }

private data class ArBandRow(
    val nr: Boolean,
    val band: Int,
    val active: Boolean,
    val selected: Boolean,
    val arfcn: Int?,
    val bandwidthMhz: Double?
)

@Composable
fun ArabicHaiDashboard(
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
        var section by rememberSaveable { mutableStateOf(ArSection.HOME) }
        BoxWithConstraints(Modifier.fillMaxSize().background(ArBg)) {
            val widthDp = maxWidth.value.roundToInt().coerceAtLeast(1)
            val heightDp = maxHeight.value.roundToInt().coerceAtLeast(1)
            val spec = remember(widthDp, heightDp) { HaiResponsivePolicy.resolve(widthDp, heightDp) }

            Column(Modifier.fillMaxSize()) {
                ArabicHeader(spec, snapshot != null, onDisconnect)
                ArabicTabs(spec, section, snapshot != null) { section = it }

                if (snapshot == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ArabicCard(spec, Modifier.padding(spec.horizontalPaddingDp.dp)) {
                            Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ZTE Smart HAI", color = ArDeep, fontSize = ars(spec, 26), fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(8.dp))
                                Text("جاري قراءة الراوتر والتحقق من البيانات الحية…", color = ArMuted, fontSize = ars(spec, 12), textAlign = TextAlign.Center)
                                if (status.isNotBlank()) Text(status, color = ArMuted, fontSize = ars(spec, 9), modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                    ArabicBottomNav(spec, section) { section = it }
                    return@Column
                }

                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = spec.horizontalPaddingDp.dp,
                            end = spec.horizontalPaddingDp.dp,
                            top = spec.sectionGapDp.dp,
                            bottom = (spec.bottomBarHeightDp + 20).dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)
                    ) {
                        if (operationMessage.isNotBlank()) item { ArabicMessageStrip(spec, operationMessage) }
                        when (section) {
                            ArSection.HOME -> {
                                item { ArabicTopCards(spec, snapshot, lastPerformance, speedBusy, onSpeedTest) }
                                item { ArabicQuickActions(spec, onSection = { section = it }, onOptimizeNow = onOptimizeNow) }
                                item {
                                    ArabicBandsCard(
                                        spec, snapshot, capabilities, selectedLte, selectedNr,
                                        onLteToggle, onNrToggle, onApplyLte, onApplyNr,
                                        busy = controlBusy, expanded = false
                                    )
                                }
                                item { ArabicTowerSummary(spec, snapshot, nearbyCells, scanBusy, onScanCells) }
                                item { ArabicSignalCard(spec, snapshot, telemetrySamples) }
                                item { ArabicConnectionsCard(spec, snapshot) }
                                item { ArabicQuickInfo(spec, snapshot) }
                            }
                            ArSection.NETWORK -> {
                                item { ArabicTopCards(spec, snapshot, lastPerformance, speedBusy, onSpeedTest) }
                                item { ArabicNetworkControls(spec, traffic, thermal, controlBusy, onSetNetworkMode) }
                                item { ArabicConnectionsCard(spec, snapshot) }
                            }
                            ArSection.TOWERS -> {
                                item { ArabicTowerSummary(spec, snapshot, nearbyCells, scanBusy, onScanCells) }
                                item {
                                    ArabicTowerControls(
                                        spec, snapshot, nearbyCells, controlBusy, scanBusy,
                                        towerTarget, towerGuardEnabled, towerGuardStatus,
                                        onScanCells, onLockCurrentCell, onLockNearbyCell,
                                        onClearCellLock, onTowerGuardChange
                                    )
                                }
                            }
                            ArSection.BANDS -> item {
                                ArabicBandsCard(
                                    spec, snapshot, capabilities, selectedLte, selectedNr,
                                    onLteToggle, onNrToggle, onApplyLte, onApplyNr,
                                    busy = controlBusy, expanded = true
                                )
                            }
                            ArSection.TOOLS -> item {
                                ArabicTools(
                                    spec, runtime, stability, thermal,
                                    placementMode, placementReading,
                                    smartMode, smartGoal, smartBusy, smartReport,
                                    safetyBackupAvailable, capabilities, controlBusy,
                                    onPlacementToggle, onSmartModeChange, onSmartGoalChange,
                                    onOptimizeNow, onRestoreSafetyBackup, onAntennaState,
                                    onCopyDiagnostics, onShareDiagnostics
                                )
                            }
                        }
                    }
                    ArabicBottomNav(spec, section, { section = it }, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

@Composable
private fun ArabicHeader(spec: HaiLayoutSpec, connected: Boolean, onDisconnect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(
            horizontal = spec.horizontalPaddingDp.dp,
            vertical = if (spec.denseHeader) 5.dp else 8.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).clickable { }, contentAlignment = Alignment.Center) {
            Text("☰", color = ArInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text("ZTE Smart HAI", color = ArDeep, fontSize = ars(spec, 20), fontWeight = FontWeight.Black)
            Text("إدارة الشبكة الذكية", color = ArMuted, fontSize = ars(spec, 7))
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(ArCard).border(1.dp, ArBorder, RoundedCornerShape(50))
                .clickable(enabled = connected, onClick = onDisconnect).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) ArGreen else ArRed))
            Spacer(Modifier.width(5.dp))
            Text(if (connected) "متصل" else "غير متصل", color = ArInk, fontSize = ars(spec, 8), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ArabicTabs(spec: HaiLayoutSpec, selected: ArSection, connected: Boolean, onSelect: (ArSection) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = spec.horizontalPaddingDp.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ArabicTab("⌂", "الرئيسية", selected == ArSection.HOME) { onSelect(ArSection.HOME) }
        ArabicTab("⌁", "الشبكة", selected == ArSection.NETWORK) { onSelect(ArSection.NETWORK) }
        ArabicTab("♜", "الأبراج", selected == ArSection.TOWERS) { onSelect(ArSection.TOWERS) }
        ArabicTab("▦", "الترددات", selected == ArSection.BANDS) { onSelect(ArSection.BANDS) }
        ArabicTab("⚒", "الأدوات", selected == ArSection.TOOLS) { onSelect(ArSection.TOOLS) }
    }
}

@Composable
private fun ArabicTab(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape)
            .background(if (selected) ArBlue else ArCard)
            .border(1.dp, if (selected) Color.Transparent else ArBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = if (selected) Color.White else ArInk, fontSize = 12.sp)
        Spacer(Modifier.width(5.dp))
        Text(label, color = if (selected) Color.White else ArInk, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ArabicTopCards(spec: HaiLayoutSpec, snapshot: RouterSnapshot, performance: NetworkPerformance?, speedBusy: Boolean, onSpeedTest: () -> Unit) {
    if (spec.twoColumn) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)) {
            ArabicNetworkStatus(spec, snapshot, Modifier.weight(1.58f))
            ArabicSpeed(spec, performance, speedBusy, onSpeedTest, Modifier.weight(0.92f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)) {
            ArabicNetworkStatus(spec, snapshot, Modifier.fillMaxWidth())
            ArabicSpeed(spec, performance, speedBusy, onSpeedTest, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ArabicNetworkStatus(spec: HaiLayoutSpec, snapshot: RouterSnapshot, modifier: Modifier) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val type = snapshot.networkType.orEmpty()
    val network = when { nr -> "5G"; lte -> "4G"; else -> "—" }
    val mode = when {
        nr && type.contains("SA", true) && !type.contains("NSA", true) -> "5G مستقل SA"
        nr -> "5G غير مستقل NSA"
        lte -> "4G LTE"
        else -> "غير مؤكد"
    }
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val sinr = if (nr) snapshot.nrSinr else snapshot.lteSinr
    val quality = arabicQuality(rsrp)
    val shape = RoundedCornerShape(spec.cardRadiusDp.dp)
    Card(
        modifier.height(spec.networkCardHeightDp.dp).shadow(9.dp, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F9FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
    ) {
        Box(Modifier.fillMaxSize()) {
            ArabicNetworkBackdrop(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 15.dp)) {
                Text("حالة الشبكة", color = ArInk, fontSize = ars(spec, 13), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(network, color = ArInk, fontSize = ars(spec, 48), fontWeight = FontWeight.Black)
                Text(mode, color = ArInk, fontSize = ars(spec, 14), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(quality.first).padding(horizontal = 11.dp, vertical = 5.dp)) {
                    Text(quality.second, color = Color.White, fontSize = ars(spec, 8), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(if (nr || lte) "بيانات راديوية حية ومتحقق منها" else "لا توجد أدلة كافية لتأكيد الاتصال", color = ArInk, fontSize = ars(spec, 8))
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.93f)).padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ArabicMetric("RSRP", rsrp, "dBm", spec)
                    ArabicDivider()
                    ArabicMetric("SINR", sinr, "dB", spec)
                    ArabicDivider()
                    ArabicMetric("RSRQ", snapshot.lteRsrq, "dB", spec)
                }
            }
        }
    }
}

@Composable
private fun ArabicNetworkBackdrop(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFFF4FAFF), Color(0xFFE8F4FF), Color(0xFFDFECF8))))
        val mountain = Path().apply {
            moveTo(0f, h * 0.58f); lineTo(w * 0.22f, h * 0.50f); lineTo(w * 0.34f, h * 0.59f)
            lineTo(w * 0.55f, h * 0.43f); lineTo(w * 0.70f, h * 0.56f); lineTo(w, h * 0.46f)
            lineTo(w, h * 0.78f); lineTo(0f, h * 0.78f); close()
        }
        drawPath(mountain, Color(0xFFBBD7F2).copy(alpha = 0.65f))
        val tx = w * 0.80f
        drawLine(Color(0xFF6A788B), Offset(tx, h * 0.78f), Offset(tx, h * 0.25f), 3f)
        repeat(3) { i -> drawCircle(Color.White.copy(alpha = 0.75f), 20f + i * 14f, Offset(tx, h * 0.27f), style = Stroke(1.6f)) }
    }
}

@Composable
private fun ArabicMetric(label: String, value: Double?, unit: String, spec: HaiLayoutSpec) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = ArInk, fontSize = ars(spec, 8), fontWeight = FontWeight.SemiBold)
        Text(value?.let(::arFmt1) ?: "—", color = Color(0xFF10182B), fontSize = ars(spec, 14), fontWeight = FontWeight.Bold)
        Text(unit, color = ArMuted, fontSize = ars(spec, 8))
    }
}

@Composable
private fun ArabicDivider() { Box(Modifier.width(1.dp).height(36.dp).background(Color(0xFFD7DEEA))) }

@Composable
private fun ArabicSpeed(spec: HaiLayoutSpec, performance: NetworkPerformance?, busy: Boolean, onSpeedTest: () -> Unit, modifier: Modifier) {
    val speed = performance?.downloadMbps
    val fraction = ((speed ?: 0.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
    ArabicCard(spec, modifier.height(spec.speedCardHeightDp.dp)) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text("اختبار السرعة", color = ArInk, fontSize = ars(spec, 13), fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(150.dp)) {
                    val arc = Size(size.width * 0.84f, size.height * 0.84f)
                    val top = Offset(size.width * 0.08f, size.height * 0.12f)
                    drawArc(Color(0xFFDDE4EC), 160f, 220f, false, top, arc, style = Stroke(14f, cap = StrokeCap.Round))
                    drawArc(Brush.sweepGradient(listOf(ArBlue, Color(0xFF13C9EE), ArMint)), 160f, 220f * fraction, false, top, arc, style = Stroke(14f, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(speed?.let(::arFmt0) ?: "—", color = ArInk, fontSize = ars(spec, 30), fontWeight = FontWeight.Black)
                    Text("Mb/s", color = ArInk, fontSize = ars(spec, 10))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(speed?.let(::arFmt1) ?: "—", color = ArInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("التنزيل", color = ArMuted, fontSize = 7.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(performance?.latencyMs?.let(::arFmt1) ?: "—", color = ArInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("زمن الاستجابة ms", color = ArMuted, fontSize = 7.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSpeedTest, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ArBlue)
            ) { Text(if (busy) "جاري القياس…" else "بدء الاختبار", color = Color.White, fontSize = ars(spec, 10), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ArabicQuickActions(spec: HaiLayoutSpec, onSection: (ArSection) -> Unit, onOptimizeNow: () -> Unit) {
    ArabicCard(spec) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            ArabicQuickAction("▣", "قفل الترددات", Modifier.weight(1f)) { onSection(ArSection.BANDS) }
            ArabicQuickAction("≡", "اختيار التردد", Modifier.weight(1f)) { onSection(ArSection.BANDS) }
            ArabicQuickAction("⌁", "وضع الشبكة", Modifier.weight(1f)) { onSection(ArSection.NETWORK) }
            ArabicQuickAction("◴", "تحسين ذكي", Modifier.weight(1f), onOptimizeNow)
            ArabicQuickAction("⚒", "التشخيص", Modifier.weight(1f)) { onSection(ArSection.TOOLS) }
        }
    }
}

@Composable
private fun ArabicQuickAction(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F8FD)).clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, color = ArInk, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = ArInk, fontSize = 7.sp, maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ArabicBandsCard(
    spec: HaiLayoutSpec,
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    busy: Boolean,
    expanded: Boolean
) {
    var filter by rememberSaveable { mutableStateOf(ArBandFilter.ALL) }
    val rows = remember(snapshot.cells, capabilities, selectedLte, selectedNr) { arBuildBandRows(snapshot, capabilities, selectedLte, selectedNr) }
    val filtered = rows.filter { when (filter) { ArBandFilter.ALL -> true; ArBandFilter.NR -> it.nr; ArBandFilter.LTE -> !it.nr } }
    val shown = if (expanded) filtered else filtered.take(10)
    ArabicCard(spec) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("جميع الترددات", color = ArInk, fontSize = ars(spec, 11), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("الحالة الحية فقط", color = ArMuted, fontSize = ars(spec, 7))
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ArabicFilter("الكل", rows.size, filter == ArBandFilter.ALL) { filter = ArBandFilter.ALL }
                ArabicFilter("5G", rows.count { it.nr }, filter == ArBandFilter.NR) { filter = ArBandFilter.NR }
                ArabicFilter("4G", rows.count { !it.nr }, filter == ArBandFilter.LTE) { filter = ArBandFilter.LTE }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().background(Color(0xFFF6F8FC)).padding(6.dp)) {
                ArabicTableText("الحالة", .7f, true); ArabicTableText("النطاق", .9f, true); ArabicTableText("ARFCN", 1.1f, true)
                ArabicTableText("العرض", 1.1f, true); ArabicTableText("التقنية", .9f, true); ArabicTableText("اختيار", .9f, true)
            }
            shown.forEach { row ->
                Row(Modifier.fillMaxWidth().border(.5.dp, ArBorder).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(.7f)) { Box(Modifier.size(8.dp).clip(CircleShape).background(if (row.active) ArGreen else Color(0xFF8AA0B8))) }
                    Text("${if (row.nr) "N" else "B"}${row.band}", color = if (row.nr) ArPurple else Color(0xFF187A57), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(.9f))
                    ArabicTableText(row.arfcn?.toString() ?: "—", 1.1f)
                    ArabicTableText(row.bandwidthMhz?.let { "${arFmt0(it)} MHz" } ?: "—", 1.1f)
                    ArabicTableText(if (row.nr) "5G" else "4G", .9f)
                    Box(Modifier.weight(.9f), contentAlignment = Alignment.Center) {
                        Switch(
                            checked = row.selected,
                            onCheckedChange = { if (!busy) { if (row.nr) onNrToggle(row.band) else onLteToggle(row.band) } },
                            enabled = !busy,
                            colors = SwitchDefaults.colors(checkedTrackColor = ArMint)
                        )
                    }
                }
            }
            if (!expanded && filtered.size > shown.size) {
                Text("يوجد ${filtered.size - shown.size} نطاقات إضافية في قسم الترددات", color = ArBlue, fontSize = 8.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onApplyLte, enabled = selectedLte.isNotEmpty() && !busy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = ArBlue)) {
                    Text("تطبيق 4G والتحقق", fontSize = 8.sp, maxLines = 1)
                }
                Button(onClick = onApplyNr, enabled = selectedNr.isNotEmpty() && !busy && capabilities.supportsNrBandLock, modifier = Modifier.weight(1f), shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = ArDeep)) {
                    Text("تطبيق 5G والتحقق", fontSize = 8.sp, maxLines = 1)
                }
            }
            Text("الاختيار إعداد مطلوب فقط؛ الحالة النشطة لا تظهر إلا من Carrier حي ومتحقق منه.", color = ArMuted, fontSize = 7.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun ArabicFilter(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) ArBlue else Color(0xFFF3F6FB)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (selected) Color.White else ArInk, fontSize = 9.sp)
        Spacer(Modifier.width(5.dp))
        Text(count.toString(), color = if (selected) Color.White else ArBlue, fontSize = 7.sp)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ArabicTableText(text: String, weight: Float, header: Boolean = false) {
    Text(text, color = if (header) ArMuted else ArInk, fontSize = if (header) 6.5.sp else 7.5.sp, fontWeight = if (header) FontWeight.Medium else FontWeight.Normal, modifier = Modifier.weight(weight), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ArabicTowerSummary(spec: HaiLayoutSpec, snapshot: RouterSnapshot, cells: List<NearbyCell>, scanBusy: Boolean, onScan: () -> Unit) {
    ArabicCard(spec) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الخلايا القريبة", color = ArInk, fontSize = ars(spec, 11), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(if (scanBusy) "جاري المسح…" else "مسح جديد", color = ArBlue, fontSize = 8.sp, modifier = Modifier.clickable(enabled = !scanBusy, onClick = onScan))
            }
            Spacer(Modifier.height(7.dp))
            Box(Modifier.fillMaxWidth().height(spec.mapCardHeightDp.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF3F7F3))) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0xFFF2F6F1))
                    repeat(5) { i ->
                        val y = size.height * (.15f + i * .18f)
                        drawLine(Color.White, Offset(0f, y), Offset(size.width, y - size.height * .18f), 10f)
                    }
                    cells.take(6).forEachIndexed { index, _ ->
                        val angle = index * 1.8 + .5
                        val x = size.width * (.22f + ((cos(angle) + 1.0) * .29).toFloat())
                        val y = size.height * (.20f + ((sin(angle) + 1.0) * .28).toFloat())
                        drawCircle(Color.White, 13f, Offset(x, y))
                        drawCircle(if (index == 0) ArGreen else ArBlue, 8f, Offset(x, y), style = Stroke(3f))
                    }
                }
                cells.firstOrNull()?.let { best ->
                    Column(Modifier.align(Alignment.Center).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.96f)).padding(8.dp)) {
                        Text("الخلية رقم 1", color = ArInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("PCI ${best.pci ?: "—"}", color = ArInk, fontSize = 7.sp)
                        Text("${best.band ?: "—"} • ARFCN ${best.arfcn ?: "—"}", color = ArMuted, fontSize = 7.sp)
                    }
                }
            }
            Text("مخطط راديو فقط؛ لا يتم اختراع موقع أو مسافة جغرافية للبرج.", color = ArMuted, fontSize = 6.5.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun ArabicSignalCard(spec: HaiLayoutSpec, snapshot: RouterSnapshot, samples: List<SafeTelemetrySample>) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val current = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    ArabicCard(spec) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("مراقبة الإشارة", color = ArInk, fontSize = ars(spec, 10), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("حي", color = ArBlue, fontSize = 7.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.weight(1f).height((spec.signalCardHeightDp - 60).coerceAtLeast(90).dp)) {
                    repeat(4) { i -> drawLine(Color(0xFFDCE5F2), Offset(0f, size.height * i / 3f), Offset(size.width, size.height * i / 3f), 1f) }
                    if (samples.size >= 2) {
                        fun value(s: SafeTelemetrySample): Double? = if (s.nrVerified) s.nrRsrp else s.lteRsrp
                        fun point(i: Int, v: Double): Offset {
                            val x = size.width * i / (samples.size - 1f)
                            val n = ((v + 120.0) / 60.0).coerceIn(0.0, 1.0).toFloat()
                            return Offset(x, size.height * (1f - n))
                        }
                        for (i in 0 until samples.lastIndex) {
                            val a = value(samples[i]); val b = value(samples[i + 1])
                            if (a != null && b != null) drawLine(ArBlue, point(i, a), point(i + 1, b), 3f, StrokeCap.Round)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(current?.let(::arFmt0) ?: "—", color = ArInk, fontSize = ars(spec, 22), fontWeight = FontWeight.Black)
                    Text("dBm", color = ArInk, fontSize = 9.sp)
                    Text(arabicQuality(current).second, color = Color(0xFF14945B), fontSize = 7.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ArabicConnectionsCard(spec: HaiLayoutSpec, snapshot: RouterSnapshot) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val ca = snapshot.raw["_zte_ca_verified"].equals("true", true) && snapshot.caActive
    ArabicCard(spec) {
        Column(Modifier.padding(10.dp)) {
            Text("الاتصالات النشطة", color = ArInk, fontSize = ars(spec, 10), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (nr) ArabicChip(if (snapshot.networkType.orEmpty().contains("SA", true) && !snapshot.networkType.orEmpty().contains("NSA", true)) "5G SA" else "5G NSA", ArBlue, Color.White)
                if (lte) ArabicChip("4G LTE", Color(0xFFD9F5E7), Color(0xFF168557))
                if (ca) ArabicChip("LTE CA", Color(0xFFEBD7FF), ArPurple)
                if (!nr && !lte) ArabicChip("غير مؤكد", Color(0xFFF1F2F5), ArMuted)
            }
        }
    }
}

@Composable
private fun ArabicChip(text: String, bg: Color, fg: Color) { Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 5.dp)) { Text(text, color = fg, fontSize = 7.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun ArabicQuickInfo(spec: HaiLayoutSpec, snapshot: RouterSnapshot) {
    ArabicCard(spec) {
        Column(Modifier.padding(10.dp)) {
            Text("معلومات سريعة", color = ArInk, fontSize = ars(spec, 9), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            ArabicInfo("معرّف الخلية", snapshot.cellId?.toString() ?: "—")
            ArabicInfo("PCI", snapshot.pci?.toString() ?: "—")
            ArabicInfo("EARFCN", snapshot.earfcn?.toString() ?: "—")
            ArabicInfo("نطاق LTE", snapshot.lteBand ?: "—")
            ArabicInfo("نطاق NR", snapshot.nrBand ?: "—")
        }
    }
}

@Composable
private fun ArabicInfo(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) { Text(label, color = ArInk, fontSize = 7.5.sp, modifier = Modifier.weight(1f)); Text(value, color = ArInk, fontSize = 7.5.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f), maxLines = 1) } }

@Composable
private fun ArabicNetworkControls(spec: HaiLayoutSpec, traffic: TrafficTelemetry?, thermal: ThermalTelemetry?, busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    ArabicCard(spec) {
        Column(Modifier.padding(14.dp)) {
            Text("وضع الشبكة", color = ArInk, fontSize = ars(spec, 13), fontWeight = FontWeight.Bold)
            Text("لا يعتبر الوضع مطبقًا إلا بعد أن يعيد الراوتر نفس القيمة في read-back.", color = ArMuted, fontSize = 8.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ArabicAction("4G فقط", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_LTE") }
                ArabicAction("4G + 5G", !busy, Modifier.weight(1f)) { onSetNetworkMode("LTE_AND_5G") }
                ArabicAction("5G فقط", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_5G") }
            }
            Spacer(Modifier.height(12.dp))
            Text("بيانات الراوتر الحية", color = ArInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ArabicMini("التنزيل الآن", TrafficTelemetryFormatter.rateMbps(traffic?.rxBytesPerSecond), "Mb/s", Modifier.weight(1f))
                ArabicMini("الرفع الآن", TrafficTelemetryFormatter.rateMbps(traffic?.txBytesPerSecond), "Mb/s", Modifier.weight(1f))
                ArabicMini("الحرارة", thermal?.highestObserved?.let { ThermalTelemetryFormatter.celsius(it.celsius) } ?: "—", "أعلى حساس", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ArabicMini(title: String, value: String, suffix: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5F8FD)).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = ArMuted, fontSize = 7.sp); Text(value, color = ArInk, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(suffix, color = ArBlue, fontSize = 6.5.sp)
    }
}

@Composable
private fun ArabicTowerControls(
    spec: HaiLayoutSpec,
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
    val recommendation = remember(cells, snapshot.pci, snapshot.earfcn) { TowerRecommendationEngine.recommend(cells, snapshot.pci, snapshot.earfcn) }
    ArabicCard(spec) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("التحكم بالخلية", color = ArInk, fontSize = ars(spec, 13), fontWeight = FontWeight.Bold)
                    Text("تثبيت LTE يعتمد على PCI + EARFCN مع read-back وتحقق حي متعدد العينات.", color = ArMuted, fontSize = 8.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(checked = guardEnabled, onCheckedChange = onGuardChange, enabled = target != null && !busy, colors = SwitchDefaults.colors(checkedTrackColor = ArMint))
                    Text("حارس البرج", color = ArMuted, fontSize = 7.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ArabicAction(if (scanBusy) "جاري المسح…" else "مسح الخلايا", !scanBusy && !busy, Modifier.weight(1f), onScan)
                ArabicAction("تحقق وثبّت الحالية", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
                ArabicAction("إزالة القفل", !busy, Modifier.weight(1f), onClear)
            }
            target?.let { Text("الهدف: PCI ${it.pci} • EARFCN ${it.earfcn}", color = ArBlue, fontSize = 8.sp, modifier = Modifier.padding(top = 7.dp)) }
            guardStatus?.let { Text(it.message, color = if (it.match == TowerMatch.MATCHED) Color(0xFF178958) else ArMuted, fontSize = 7.sp, modifier = Modifier.padding(top = 4.dp)) }
            Spacer(Modifier.height(8.dp))
            val recommendationText = when (recommendation.decision) {
                TowerRecommendationDecision.RECOMMEND_CANDIDATE -> "توجد خلية مرشحة أفضل وفق أدلة المسح"
                TowerRecommendationDecision.KEEP_CURRENT -> "التوصية الحالية: إبقاء الخلية الحالية"
                TowerRecommendationDecision.INSUFFICIENT_EVIDENCE -> "لا توجد أدلة كافية لترشيح خلية أخرى"
            }
            Text(recommendationText, color = ArInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            cells.take(16).forEachIndexed { index, cell -> ArabicCellRow(index + 1, cell, snapshot, busy, onLockCell) }
        }
    }
}

@Composable
private fun ArabicCellRow(index: Int, cell: NearbyCell, snapshot: RouterSnapshot, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val current = cell.rat == "LTE" && cell.pci == snapshot.pci && cell.arfcn == snapshot.earfcn
    val lockable = cell.rat == "LTE" && cell.pci != null && cell.arfcn != null
    Row(Modifier.fillMaxWidth().border(.5.dp, ArBorder).padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (current) Color(0xFFECFBF5) else ArSoft), contentAlignment = Alignment.Center) { Text(index.toString(), color = if (current) Color(0xFF178958) else ArBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text("${cell.rat} ${cell.band ?: "—"} • PCI ${cell.pci ?: "—"} • ARFCN ${cell.arfcn ?: "—"}${if (current) " • الحالية" else ""}", color = ArInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("RSRP ${cell.rsrp?.let(::arFmt1) ?: "—"} • الدليل ${cell.evidenceScore ?: "—"}/100 • الظهور ${cell.presencePercent}%", color = ArMuted, fontSize = 7.sp)
            Text(when (cell.confidence) { CellConfidence.HIGH -> "ثقة عالية"; CellConfidence.MEDIUM -> "ثقة متوسطة"; CellConfidence.LOW -> "ثقة منخفضة"; null -> "الثقة غير مكتملة" }, color = ArMuted, fontSize = 6.5.sp)
        }
        Text(if (lockable) "تحقق وثبّت" else "قراءة فقط", color = if (lockable && !busy) ArBlue else ArMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(50)).clickable(enabled = lockable && !busy) { onLock(cell) }.padding(7.dp))
    }
}

@Composable
private fun ArabicTools(
    spec: HaiLayoutSpec,
    runtime: RuntimeCapabilityReport?,
    stability: ConnectionStabilityReport,
    thermal: ThermalTelemetry?,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    backupAvailable: Boolean,
    capabilities: RouterCapabilities,
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
    Column(verticalArrangement = Arrangement.spacedBy(spec.sectionGapDp.dp)) {
        ArabicCard(spec) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("التحسين الذكي", color = ArInk, fontSize = ars(spec, 13), fontWeight = FontWeight.Bold)
                        Text("لا يستخدم إلا أوامر LTE التي يمكن التحقق منها واستعادتها بأمان.", color = ArMuted, fontSize = 8.sp)
                    }
                    Switch(checked = smartMode, onCheckedChange = onSmartModeChange, colors = SwitchDefaults.colors(checkedTrackColor = ArMint))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OptimizationGoal.entries.forEach { goal -> ArabicChoice(arGoal(goal), smartGoal == goal, Modifier.weight(1f)) { onSmartGoalChange(goal) } }
                }
                Spacer(Modifier.height(8.dp))
                ArabicAction(if (smartBusy) "جاري التحسين…" else "تحسين الآن", !smartBusy && !busy, Modifier.fillMaxWidth(), onOptimizeNow)
                smartReport?.let { Text(it.message, color = ArMuted, fontSize = 7.sp, modifier = Modifier.padding(top = 6.dp)) }
            }
        }

        ArabicCard(spec) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("مساعد اختيار مكان الراوتر", color = ArInk, fontSize = ars(spec, 12), fontWeight = FontWeight.Bold)
                        Text("يعتمد على قراءات الإشارة الحية أثناء تحريك الراوتر.", color = ArMuted, fontSize = 8.sp)
                    }
                    Switch(checked = placementMode, onCheckedChange = { onPlacementToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = ArMint))
                }
                if (placementReading != null) Text("المساعد يستقبل القراءة الحية ويحدّث التوجيه تلقائيًا.", color = ArBlue, fontSize = 8.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }

        ArabicCard(spec) {
            Column(Modifier.padding(14.dp)) {
                Text("تشخيص الـFirmware", color = ArInk, fontSize = ars(spec, 12), fontWeight = FontWeight.Bold)
                if (runtime == null) {
                    Text("فحص قدرات الـFirmware لم يكتمل بعد.", color = ArMuted, fontSize = 8.sp)
                } else {
                    ArabicRuntime("ترددات 4G", runtime.lteBandControl)
                    ArabicRuntime("ترددات 5G", runtime.nrBandControl)
                    ArabicRuntime("قفل الخلية", runtime.cellLock)
                    ArabicRuntime("وضع الشبكة", runtime.networkMode)
                    ArabicRuntime("مسح الخلايا", runtime.neighborScan)
                    ArabicRuntime("الهوائي", runtime.antennaControl)
                    ArabicRuntime("تجميع الترددات CA", runtime.carrierAggregationTelemetry)
                }
                Text("الثبات: ${stability.summary}", color = ArMuted, fontSize = 7.sp, modifier = Modifier.padding(top = 6.dp))
                thermal?.takeIf { it.hasAnyEvidence }?.let { Text("الحرارة: ${ThermalTelemetryFormatter.summary(it)}", color = ArMuted, fontSize = 7.sp, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ArabicAction("نسخ التقرير", true, Modifier.weight(1f), onCopyDiagnostics)
                    ArabicAction("مشاركة التقرير", true, Modifier.weight(1f), onShareDiagnostics)
                }
            }
        }

        ArabicCard(spec) {
            Column(Modifier.padding(14.dp)) {
                Text("الأمان والاستعادة", color = ArInk, fontSize = ars(spec, 12), fontWeight = FontWeight.Bold)
                Text(if (backupAvailable) "توجد نسخة أمان محلية موثّقة قابلة للاستعادة." else "لا توجد نسخة أمان محفوظة حتى الآن.", color = ArMuted, fontSize = 8.sp)
                Spacer(Modifier.height(8.dp))
                ArabicAction("استعادة والتحقق", backupAvailable && !busy, Modifier.fillMaxWidth(), onRestore)
            }
        }

        if (capabilities.supportsAntennaControl) {
            ArabicCard(spec) {
                Column(Modifier.padding(14.dp)) {
                    Text("التحكم بالهوائي", color = ArInk, fontSize = ars(spec, 12), fontWeight = FontWeight.Bold)
                    Text("لا يتاح إلا عندما يثبت فحص الـFirmware أن مسار الكتابة آمن وقابل للتحقق.", color = ArMuted, fontSize = 8.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..3).forEach { state -> ArabicAction("الوضع $state", !busy, Modifier.weight(1f)) { onAntennaState(state) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArabicRuntime(label: String, evidence: RuntimeCapabilityEvidence) {
    val state = when (evidence.state) {
        RuntimeCapabilityState.SAFE_TO_ATTEMPT -> "جاهز للمحاولة الآمنة"
        RuntimeCapabilityState.READ_ONLY -> "قراءة فقط"
        RuntimeCapabilityState.PROFILE_ONLY -> "دعم نظري فقط"
        RuntimeCapabilityState.UNAVAILABLE -> "غير متاح"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = ArInk, fontSize = 8.sp, modifier = Modifier.weight(1f))
        Text(state, color = if (evidence.state == RuntimeCapabilityState.SAFE_TO_ATTEMPT) Color(0xFF178958) else ArMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ArabicAction(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(50)).background(if (enabled) ArSoft else Color(0xFFF2F3F6)).border(1.dp, ArBorder, RoundedCornerShape(50)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) ArInk else ArMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ArabicChoice(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(50)).background(if (selected) ArBlue else Color(0xFFF2F5FA)).clickable(onClick = onClick).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (selected) Color.White else ArInk, fontSize = 7.sp, maxLines = 1)
    }
}

@Composable
private fun ArabicMessageStrip(spec: HaiLayoutSpec, message: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFEEF5FF)).border(1.dp, Color(0xFFDCE8FA), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(message, color = ArDeep, fontSize = ars(spec, 8), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ArabicBottomNav(spec: HaiLayoutSpec, selected: ArSection, onSelect: (ArSection) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp, bottomStart = 26.dp, bottomEnd = 26.dp)
    Row(
        modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 6.dp).fillMaxWidth().height(spec.bottomBarHeightDp.dp).shadow(12.dp, shape).clip(shape).background(Color.White.copy(.96f)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ArabicBottomItem("▦", "الرئيسية", selected == ArSection.HOME, Modifier.weight(1f)) { onSelect(ArSection.HOME) }
        ArabicBottomItem("⌾", "الأبراج", selected == ArSection.TOWERS, Modifier.weight(1f)) { onSelect(ArSection.TOWERS) }
        Box(Modifier.size((spec.bottomBarHeightDp - 8).dp).shadow(9.dp, CircleShape).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF1B78FF), Color(0xFF031C6A)))).border(2.dp, Color(0xFF42EAF0), CircleShape).clickable { onSelect(ArSection.TOOLS) }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("HAI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black); Text("ذكي", color = Color.White, fontSize = 7.sp) }
        }
        ArabicBottomItem("⚒", "الأدوات", selected == ArSection.TOOLS, Modifier.weight(1f)) { onSelect(ArSection.TOOLS) }
        ArabicBottomItem("⌁", "الشبكة", selected == ArSection.NETWORK, Modifier.weight(1f)) { onSelect(ArSection.NETWORK) }
    }
}

@Composable
private fun ArabicBottomItem(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected) ArBlue else ArMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (selected) ArBlue else ArMuted, fontSize = 7.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        if (selected) Box(Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape).background(ArBlue))
    }
}

@Composable
private fun ArabicCard(spec: HaiLayoutSpec, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(spec.cardRadiusDp.dp)
    Card(modifier = modifier.fillMaxWidth().shadow(7.dp, shape), shape = shape, colors = CardDefaults.cardColors(containerColor = ArCard), border = androidx.compose.foundation.BorderStroke(1.dp, ArBorder)) { content() }
}

private fun arBuildBandRows(snapshot: RouterSnapshot, capabilities: RouterCapabilities, selectedLte: Set<Int>, selectedNr: Set<Int>): List<ArBandRow> {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val nrRows = capabilities.supportedNrBands.sorted().map { band ->
        val carrier = snapshot.cells.firstOrNull { it.role == CellRole.NR && arBandNumber(it.band) == band && nrVerified }
        ArBandRow(true, band, carrier != null, band in selectedNr, carrier?.arfcn, carrier?.bandwidthMhz)
    }
    val lteRows = capabilities.supportedLteBands.sorted().map { band ->
        val carrier = snapshot.cells.firstOrNull { it.role != CellRole.NR && arBandNumber(it.band) == band && lteVerified }
        ArBandRow(false, band, carrier != null, band in selectedLte, carrier?.arfcn, carrier?.bandwidthMhz)
    }
    return nrRows + lteRows
}

private fun arBandNumber(value: String?): Int? = value?.trim()?.removePrefix("n")?.removePrefix("N")?.removePrefix("b")?.removePrefix("B")?.toIntOrNull()
private fun arabicQuality(rsrp: Double?): Pair<Color, String> = when {
    rsrp == null -> Color(0xFF8A94A6) to "غير معروف"
    rsrp >= -85 -> Color(0xFF10B872) to "ممتاز"
    rsrp >= -95 -> Color(0xFF2C9D73) to "جيد"
    rsrp >= -105 -> Color(0xFFE6A23C) to "متوسط"
    else -> Color(0xFFE3515C) to "ضعيف"
}
private fun arGoal(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}
private fun arFmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
private fun arFmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun ars(spec: HaiLayoutSpec, base: Int) = (base * spec.textScale).sp
