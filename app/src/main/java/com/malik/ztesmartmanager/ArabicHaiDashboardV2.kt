package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
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

private val V2Background = Color(0xFFF6F8FC)
private val V2Surface = Color.White
private val V2Ink = Color(0xFF10275C)
private val V2Muted = Color(0xFF687694)
private val V2Blue = Color(0xFF1264F4)
private val V2BlueDark = Color(0xFF08358D)
private val V2Cyan = Color(0xFF20CFE3)
private val V2Mint = Color(0xFF19C99A)
private val V2Green = Color(0xFF16B86A)
private val V2Red = Color(0xFFE05263)
private val V2Amber = Color(0xFFE7A33E)
private val V2Border = Color(0xFFE5EAF3)
private val V2SoftBlue = Color(0xFFEEF4FF)
private val V2SoftGreen = Color(0xFFEAF9F3)
private val V2SoftPurple = Color(0xFFF3ECFF)

private enum class V2Section { HOME, NETWORK, TOWERS, BANDS, TOOLS }
private enum class V2BandFilter { ALL, NR, LTE }

private data class V2Layout(
    val spec: HaiLayoutSpec,
    val compact: Boolean,
    val wide: Boolean,
    val contentPadding: Int,
    val gap: Int,
    val bandColumns: Int
)

private data class V2BandRow(
    val nr: Boolean,
    val band: Int,
    val active: Boolean,
    val selected: Boolean,
    val arfcn: Int?,
    val bandwidthMhz: Double?
)

@Composable
fun ArabicHaiDashboardV2(
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
        var section by rememberSaveable { mutableStateOf(V2Section.HOME) }
        BoxWithConstraints(Modifier.fillMaxSize().background(V2Background)) {
            val widthDp = maxWidth.value.roundToInt().coerceAtLeast(1)
            val heightDp = maxHeight.value.roundToInt().coerceAtLeast(1)
            val base = remember(widthDp, heightDp) { HaiResponsivePolicy.resolve(widthDp, heightDp) }
            val layout = remember(widthDp, heightDp, base) {
                V2Layout(
                    spec = base,
                    compact = widthDp < 380,
                    wide = widthDp >= 600,
                    contentPadding = when {
                        widthDp < 360 -> 12
                        widthDp < 430 -> 16
                        else -> 20
                    },
                    gap = if (heightDp < 760) 12 else 16,
                    bandColumns = when {
                        widthDp < 350 -> 3
                        widthDp < 520 -> 4
                        else -> 5
                    }
                )
            }

            Column(Modifier.fillMaxSize()) {
                V2Header(layout, snapshot != null, onDisconnect)

                if (snapshot == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        V2Card(layout, Modifier.padding(layout.contentPadding.dp)) {
                            Column(
                                Modifier.padding(horizontal = 24.dp, vertical = 30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("ZTE Smart HAI", color = V2BlueDark, fontSize = v2sp(layout, 25), fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(10.dp))
                                Text("جاري قراءة الراوتر والتحقق من البيانات الحية…", color = V2Muted, fontSize = v2sp(layout, 13), textAlign = TextAlign.Center)
                                if (status.isNotBlank()) {
                                    Text(status, color = V2Muted, fontSize = v2sp(layout, 12), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                    V2BottomNav(layout, section, { section = it }, Modifier.fillMaxWidth())
                    return@Column
                }

                Box(Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = layout.contentPadding.dp,
                            end = layout.contentPadding.dp,
                            top = 6.dp,
                            bottom = 104.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(layout.gap.dp)
                    ) {
                        if (operationMessage.isNotBlank()) item { V2Message(layout, operationMessage) }

                        when (section) {
                            V2Section.HOME -> {
                                item { V2HomeHero(layout, snapshot, lastPerformance, speedBusy, onSpeedTest) }
                                item { V2QuickActions(layout, onSection = { section = it }, onOptimizeNow = onOptimizeNow) }
                                item { V2BandSummary(layout, snapshot, capabilities, selectedLte, selectedNr) { section = V2Section.BANDS } }
                                item { V2SignalSummary(layout, snapshot, telemetrySamples) }
                                item { V2CurrentCell(layout, snapshot, nearbyCells, scanBusy, onScanCells) { section = V2Section.TOWERS } }
                            }

                            V2Section.NETWORK -> {
                                item { V2NetworkHero(layout, snapshot) }
                                item { V2NetworkMode(layout, controlBusy, onSetNetworkMode) }
                                item { V2Traffic(layout, traffic) }
                                if (thermal?.hasAnyEvidence == true) item { V2Thermal(layout, thermal) }
                                item { V2ActiveConnections(layout, snapshot) }
                            }

                            V2Section.TOWERS -> {
                                item { V2TowerMap(layout, snapshot, nearbyCells, scanBusy, onScanCells) }
                                item {
                                    V2TowerControls(
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
                            }

                            V2Section.BANDS -> {
                                item {
                                    V2Bands(
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

                            V2Section.TOOLS -> {
                                item {
                                    V2Tools(
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
                                        onShareDiagnostics = onShareDiagnostics
                                    )
                                }
                            }
                        }
                    }

                    V2BottomNav(
                        layout = layout,
                        selected = section,
                        onSelect = { section = it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun V2Header(layout: V2Layout, connected: Boolean, onDisconnect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(
            horizontal = layout.contentPadding.dp,
            vertical = if (layout.compact) 10.dp else 13.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("ZTE Smart HAI", color = V2BlueDark, fontSize = v2sp(layout, 21), fontWeight = FontWeight.Black)
            Text("إدارة الشبكة الذكية", color = V2Muted, fontSize = v2sp(layout, 11), fontWeight = FontWeight.Medium)
        }
        Row(
            Modifier.clip(RoundedCornerShape(50))
                .background(V2Surface)
                .border(1.dp, V2Border, RoundedCornerShape(50))
                .clickable(enabled = connected, onClick = onDisconnect)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(if (connected) V2Green else V2Red))
            Spacer(Modifier.width(7.dp))
            Text(if (connected) "متصل" else "غير متصل", color = V2Ink, fontSize = v2sp(layout, 11), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V2HomeHero(layout: V2Layout, snapshot: RouterSnapshot, performance: NetworkPerformance?, speedBusy: Boolean, onSpeedTest: () -> Unit) {
    if (layout.wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
            V2NetworkHero(layout, snapshot, Modifier.weight(1.25f))
            V2SpeedCard(layout, performance, speedBusy, onSpeedTest, Modifier.weight(.75f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(layout.gap.dp)) {
            V2NetworkHero(layout, snapshot)
            V2SpeedCard(layout, performance, speedBusy, onSpeedTest)
        }
    }
}

@Composable
private fun V2NetworkHero(layout: V2Layout, snapshot: RouterSnapshot, modifier: Modifier = Modifier) {
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
    val quality = v2Quality(rsrp)
    val height = if (layout.compact) 255.dp else 270.dp
    val shape = RoundedCornerShape(30.dp)

    Card(
        modifier = modifier.fillMaxWidth().height(height).shadow(12.dp, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF7FF)),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Box(Modifier.fillMaxSize()) {
            V2HeroBackdrop(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("حالة الشبكة", color = V2Ink, fontSize = v2sp(layout, 15), fontWeight = FontWeight.Bold)
                        Text(if (nr || lte) "بيانات راديوية حية ومتحقق منها" else "لا توجد أدلة كافية لتأكيد الاتصال", color = V2Muted, fontSize = v2sp(layout, 11))
                    }
                    V2StatusPill(quality.second, quality.first)
                }

                Spacer(Modifier.height(12.dp))
                Text(network, color = V2Ink, fontSize = v2sp(layout, 52), fontWeight = FontWeight.Black)
                Text(mode, color = V2Ink, fontSize = v2sp(layout, 17), fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .94f)).padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    V2Metric("RSRP", rsrp, "dBm", layout)
                    V2VerticalDivider()
                    V2Metric("SINR", sinr, "dB", layout)
                    V2VerticalDivider()
                    V2Metric("RSRQ", snapshot.lteRsrq, "dB", layout)
                }
            }
        }
    }
}

@Composable
private fun V2HeroBackdrop(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFFF6FBFF), Color(0xFFE8F4FF), Color(0xFFDCEBFA))))
        val w = size.width
        val h = size.height
        val ridge = Path().apply {
            moveTo(0f, h * .58f)
            lineTo(w * .20f, h * .50f)
            lineTo(w * .36f, h * .60f)
            lineTo(w * .57f, h * .43f)
            lineTo(w * .73f, h * .57f)
            lineTo(w, h * .47f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(ridge, Color(0xFFB8D5F0).copy(alpha = .55f))
        val towerX = w * .82f
        drawLine(Color(0xFF75849A), Offset(towerX, h * .77f), Offset(towerX, h * .28f), 3f)
        repeat(3) { i ->
            drawCircle(Color.White.copy(alpha = .82f), 22f + i * 16f, Offset(towerX, h * .29f), style = Stroke(1.7f))
        }
    }
}

@Composable
private fun V2Metric(label: String, value: Double?, unit: String, layout: V2Layout) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = V2Muted, fontSize = v2sp(layout, 11), fontWeight = FontWeight.Bold)
        Text(value?.let(::v2Fmt1) ?: "—", color = V2Ink, fontSize = v2sp(layout, 18), fontWeight = FontWeight.Black)
        Text(unit, color = V2Muted, fontSize = v2sp(layout, 10))
    }
}

@Composable
private fun V2VerticalDivider() {
    Box(Modifier.width(1.dp).height(44.dp).background(Color(0xFFD5DDEA)))
}

@Composable
private fun V2StatusPill(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 12.dp, vertical = 7.dp)) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V2SpeedCard(layout: V2Layout, performance: NetworkPerformance?, busy: Boolean, onSpeedTest: () -> Unit, modifier: Modifier = Modifier) {
    val speed = performance?.downloadMbps
    val fraction = ((speed ?: 0.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
    V2Card(layout, modifier) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("اختبار السرعة", color = V2Ink, fontSize = v2sp(layout, 17), fontWeight = FontWeight.Black)
                    Text("قياس مستقل عن عدادات الراوتر", color = V2Muted, fontSize = v2sp(layout, 11))
                }
                Text(performance?.latencyMs?.let { "${v2Fmt0(it)} ms" } ?: "—", color = V2Blue, fontSize = v2sp(layout, 12), fontWeight = FontWeight.Bold)
            }

            Box(Modifier.fillMaxWidth().height(if (layout.compact) 152.dp else 168.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(if (layout.compact) 148.dp else 162.dp)) {
                    drawArc(Color(0xFFE2E8F1), 155f, 230f, false, style = Stroke(16f, cap = StrokeCap.Round))
                    drawArc(Brush.sweepGradient(listOf(V2Blue, V2Cyan, V2Mint)), 155f, 230f * fraction, false, style = Stroke(16f, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(speed?.let(::v2Fmt0) ?: "—", color = V2Ink, fontSize = v2sp(layout, 34), fontWeight = FontWeight.Black)
                    Text("Mb/s", color = V2Muted, fontSize = v2sp(layout, 12), fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = onSpeedTest,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = V2Blue)
            ) {
                Text(if (busy) "جاري القياس…" else "بدء اختبار السرعة", color = Color.White, fontSize = v2sp(layout, 13), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V2QuickActions(layout: V2Layout, onSection: (V2Section) -> Unit, onOptimizeNow: () -> Unit) {
    V2SectionTitle(layout, "اختصارات التحكم", "أكثر الأدوات استخدامًا")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            V2QuickTile("▦", "الترددات", "عرض واختيار 4G و5G", V2SoftBlue, Modifier.weight(1f)) { onSection(V2Section.BANDS) }
            V2QuickTile("⌾", "الأبراج", "مسح وتثبيت الخلية", V2SoftGreen, Modifier.weight(1f)) { onSection(V2Section.TOWERS) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            V2QuickTile("⌁", "وضع الشبكة", "4G أو 5G أو تلقائي", Color(0xFFFFF4E6), Modifier.weight(1f)) { onSection(V2Section.NETWORK) }
            V2QuickTile("✦", "تحسين ذكي", "اختبار إعداد موثّق", V2SoftPurple, Modifier.weight(1f), onOptimizeNow)
        }
    }
}

@Composable
private fun V2QuickTile(icon: String, title: String, subtitle: String, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(22.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .82f)), contentAlignment = Alignment.Center) {
            Text(icon, color = V2BlueDark, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = V2Ink, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(subtitle, color = V2Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun V2BandSummary(
    layout: V2Layout,
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    onOpen: () -> Unit
) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val ca = snapshot.raw["_zte_ca_verified"].equals("true", true) && snapshot.caActive
    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الترددات", color = V2Ink, fontSize = v2sp(layout, 17), fontWeight = FontWeight.Black)
                    Text("الحالة النشطة منفصلة عن الترددات المختارة", color = V2Muted, fontSize = v2sp(layout, 11))
                }
                Text("عرض الكل", color = V2Blue, fontSize = v2sp(layout, 12), fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOpen).padding(8.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V2SummaryBox("LTE", snapshot.lteBand ?: "—", "مختار ${selectedLte.size}/${capabilities.supportedLteBands.size}", V2SoftGreen, Modifier.weight(1f))
                V2SummaryBox("5G NR", if (nr) snapshot.nrBand ?: "نشط" else "غير مثبت", "مختار ${selectedNr.size}/${capabilities.supportedNrBands.size}", V2SoftBlue, Modifier.weight(1f))
                V2SummaryBox("CA", if (ca) "نشط" else "غير مثبت", "Carrier حي فقط", V2SoftPurple, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V2SummaryBox(title: String, value: String, subtitle: String, bg: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(bg).padding(12.dp)) {
        Text(title, color = V2Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = V2Ink, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = V2Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V2SignalSummary(layout: V2Layout, snapshot: RouterSnapshot, samples: List<SafeTelemetrySample>) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val current = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الإشارة الآن", color = V2Ink, fontSize = v2sp(layout, 17), fontWeight = FontWeight.Black)
                    Text(if (nr) "قياس 5G NR موثّق" else "قياس LTE موثّق", color = V2Muted, fontSize = v2sp(layout, 11))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(current?.let(::v2Fmt0) ?: "—", color = V2Ink, fontSize = v2sp(layout, 25), fontWeight = FontWeight.Black)
                    Text("dBm", color = V2Muted, fontSize = v2sp(layout, 11))
                }
            }
            Spacer(Modifier.height(12.dp))
            V2SignalPlot(samples, Modifier.fillMaxWidth().height(118.dp))
        }
    }
}

@Composable
private fun V2SignalPlot(samples: List<SafeTelemetrySample>, modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFFF8FAFE)).padding(10.dp)) {
        repeat(4) { i ->
            val y = size.height * i / 3f
            drawLine(Color(0xFFDDE5F0), Offset(0f, y), Offset(size.width, y), 1f)
        }
        if (samples.size < 2) return@Canvas
        fun value(s: SafeTelemetrySample): Double? = if (s.nrVerified) s.nrRsrp else s.lteRsrp
        fun point(i: Int, v: Double): Offset {
            val x = size.width * i / (samples.size - 1f)
            val normalized = ((v + 120.0) / 60.0).coerceIn(0.0, 1.0).toFloat()
            return Offset(x, size.height * (1f - normalized))
        }
        for (i in 0 until samples.lastIndex) {
            val a = value(samples[i])
            val b = value(samples[i + 1])
            if (a != null && b != null) drawLine(V2Blue, point(i, a), point(i + 1, b), 4f, StrokeCap.Round)
        }
    }
}

@Composable
private fun V2CurrentCell(layout: V2Layout, snapshot: RouterSnapshot, cells: List<NearbyCell>, scanBusy: Boolean, onScan: () -> Unit, onOpen: () -> Unit) {
    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الخلية الحالية", color = V2Ink, fontSize = v2sp(layout, 17), fontWeight = FontWeight.Black)
                    Text("هوية راديوية وليست موقعًا جغرافيًا", color = V2Muted, fontSize = v2sp(layout, 11))
                }
                Text(if (scanBusy) "جاري المسح…" else "مسح جديد", color = V2Blue, fontSize = v2sp(layout, 12), fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = !scanBusy, onClick = onScan).padding(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V2SummaryBox("PCI", snapshot.pci?.toString() ?: "—", "الخلية الحية", V2SoftBlue, Modifier.weight(1f))
                V2SummaryBox("EARFCN", snapshot.earfcn?.toString() ?: "—", "قناة LTE", V2SoftGreen, Modifier.weight(1f))
                V2SummaryBox("الخلايا", cells.size.toString(), "من آخر مسح", Color(0xFFFFF4E6), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            V2OutlineAction("فتح قسم الأبراج", true, Modifier.fillMaxWidth(), onOpen)
        }
    }
}

@Composable
private fun V2NetworkMode(layout: V2Layout, busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("وضع الشبكة", color = V2Ink, fontSize = v2sp(layout, 17), fontWeight = FontWeight.Black)
            Text("لا يعتبر الوضع مطبقًا إلا بعد read-back مطابق من الراوتر", color = V2Muted, fontSize = v2sp(layout, 11))
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                V2ModeButton("4G فقط", "تقييد الاتصال على LTE", !busy, Modifier.fillMaxWidth()) { onSetNetworkMode("Only_LTE") }
                V2ModeButton("4G + 5G", "السماح بالاتصال المختلط", !busy, Modifier.fillMaxWidth()) { onSetNetworkMode("LTE_AND_5G") }
                V2ModeButton("5G فقط", "يستخدم فقط إذا كان الـFirmware يدعمه بأمان", !busy, Modifier.fillMaxWidth()) { onSetNetworkMode("Only_5G") }
            }
        }
    }
}

@Composable
private fun V2ModeButton(title: String, subtitle: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(18.dp)).background(if (enabled) V2SoftBlue else Color(0xFFF2F3F6)).clickable(enabled = enabled, onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) V2Ink else V2Muted, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = V2Muted, fontSize = 11.sp)
        }
        Text("‹", color = if (enabled) V2Blue else V2Muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V2Traffic(layout: V2Layout, traffic: TrafficTelemetry?) {
    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("حركة البيانات", color = V2Ink, fontSize = v2sp(layout, 17), fontWeight = FontWeight.Black)
            Text("قراءة مباشرة من عدادات الراوتر وليست اختبار سرعة", color = V2Muted, fontSize = v2sp(layout, 11))
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V2SummaryBox("التنزيل الآن", TrafficTelemetryFormatter.rateMbps(traffic?.rxBytesPerSecond), "Mb/s", V2SoftBlue, Modifier.weight(1f))
                V2SummaryBox("الرفع الآن", TrafficTelemetryFormatter.rateMbps(traffic?.txBytesPerSecond), "Mb/s", V2SoftGreen, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V2Thermal(layout: V2Layout, thermal: ThermalTelemetry) {
    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("حرارة الراوتر", color = V2Ink, fontSize = v2sp(layout, 17), fontWeight = FontWeight.Black)
            Text("قراءة حساسات ZTE فقط بدون افتراض حد خطر", color = V2Muted, fontSize = v2sp(layout, 11))
            Spacer(Modifier.height(12.dp))
            val highest = thermal.highestObserved
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("أعلى قراءة مرصودة", color = V2Muted, fontSize = v2sp(layout, 12), modifier = Modifier.weight(1f))
                Text(highest?.let { "${it.label} ${ThermalTelemetryFormatter.celsius(it.celsius)}" } ?: "—", color = V2Ink, fontSize = v2sp(layout, 15), fontWeight = FontWeight.Black)
            }
            Text(ThermalTelemetryFormatter.summary(thermal), color = V2Muted, fontSize = v2sp(layout, 11), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun V2ActiveConnections(layout: V2Layout, snapshot: RouterSnapshot) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val ca = snapshot.raw["_zte_ca_verified"].equals("true", true) && snapshot.caActive
    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("الاتصالات النشطة", color = V2Ink, fontSize = v2sp(layout, 17), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (nr) V2Chip(if (snapshot.networkType.orEmpty().contains("SA", true) && !snapshot.networkType.orEmpty().contains("NSA", true)) "5G SA" else "5G NSA", V2Blue, Color.White)
                if (lte) V2Chip("4G LTE", V2SoftGreen, Color(0xFF12704D))
                if (ca) V2Chip("LTE CA", V2SoftPurple, Color(0xFF6F35B2))
                if (!nr && !lte) V2Chip("غير مؤكد", Color(0xFFF1F3F6), V2Muted)
            }
        }
    }
}

@Composable
private fun V2Chip(text: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 14.dp, vertical = 9.dp)) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V2TowerMap(layout: V2Layout, snapshot: RouterSnapshot, cells: List<NearbyCell>, scanBusy: Boolean, onScan: () -> Unit) {
    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الخلايا القريبة", color = V2Ink, fontSize = v2sp(layout, 18), fontWeight = FontWeight.Black)
                    Text("مخطط راديو فقط؛ لا يتم اختراع موقع جغرافي", color = V2Muted, fontSize = v2sp(layout, 11))
                }
                V2CompactAction(if (scanBusy) "جاري المسح…" else "مسح جديد", !scanBusy, onScan)
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFF0F6F2))) {
                Canvas(Modifier.fillMaxSize()) {
                    repeat(6) { i ->
                        val y = size.height * (.08f + i * .18f)
                        drawLine(Color.White, Offset(0f, y), Offset(size.width, y - size.height * .15f), 12f)
                    }
                    cells.take(8).forEachIndexed { index, _ ->
                        val angle = index * 1.7 + .4
                        val x = size.width * (.20f + ((cos(angle) + 1.0) * .30).toFloat())
                        val y = size.height * (.18f + ((sin(angle) + 1.0) * .30).toFloat())
                        drawCircle(Color.White, 18f, Offset(x, y))
                        drawCircle(if (index == 0) V2Green else V2Blue, 10f, Offset(x, y), style = Stroke(4f))
                    }
                    drawCircle(Color.White, 24f, Offset(size.width * .50f, size.height * .52f))
                    drawCircle(V2BlueDark, 13f, Offset(size.width * .50f, size.height * .52f))
                }
                Column(Modifier.align(Alignment.BottomCenter).padding(14.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(.95f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("الخلية الحالية", color = V2Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("PCI ${snapshot.pci ?: "—"} • EARFCN ${snapshot.earfcn ?: "—"}", color = V2Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun V2TowerControls(
    layout: V2Layout,
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
    val recommendation = remember(cells, snapshot.pci, snapshot.earfcn) {
        TowerRecommendationEngine.recommend(cells, snapshot.pci, snapshot.earfcn)
    }
    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("تثبيت الخلية", color = V2Ink, fontSize = v2sp(layout, 18), fontWeight = FontWeight.Black)
                    Text("LTE: PCI + EARFCN ثم read-back وتحقق حي متعدد العينات", color = V2Muted, fontSize = v2sp(layout, 11))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = guardEnabled,
                        onCheckedChange = onGuardChange,
                        enabled = target != null && !busy,
                        colors = SwitchDefaults.colors(checkedTrackColor = V2Mint)
                    )
                    Text("حارس البرج", color = V2Muted, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V2OutlineAction(if (scanBusy) "جاري المسح…" else "مسح الخلايا", !scanBusy && !busy, Modifier.weight(1f), onScan)
                V2PrimaryAction("تثبيت الحالية", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
            }
            Spacer(Modifier.height(8.dp))
            V2OutlineAction("إزالة القفل", !busy, Modifier.fillMaxWidth(), onClear)

            target?.let {
                Text("الهدف الموثّق: PCI ${it.pci} • EARFCN ${it.earfcn}", color = V2Blue, fontSize = v2sp(layout, 12), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            }
            guardStatus?.let {
                Text(it.message, color = if (it.match == TowerMatch.MATCHED) V2Green else V2Muted, fontSize = v2sp(layout, 11), modifier = Modifier.padding(top = 6.dp))
            }

            Spacer(Modifier.height(16.dp))
            val decision = when (recommendation.decision) {
                TowerRecommendationDecision.RECOMMEND_CANDIDATE -> "توجد خلية مرشحة أفضل وفق أدلة المسح"
                TowerRecommendationDecision.KEEP_CURRENT -> "التوصية: إبقاء الخلية الحالية"
                TowerRecommendationDecision.INSUFFICIENT_EVIDENCE -> "لا توجد أدلة كافية لترشيح خلية أخرى"
            }
            Text(decision, color = V2Ink, fontSize = v2sp(layout, 13), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))

            if (cells.isEmpty()) {
                Text("ابدأ مسح الخلايا لعرض النتائج الموثّقة هنا", color = V2Muted, fontSize = v2sp(layout, 12), modifier = Modifier.padding(vertical = 12.dp))
            } else {
                cells.take(16).forEachIndexed { index, cell ->
                    V2CellRow(index + 1, cell, snapshot, busy, onLockCell)
                    if (index != cells.take(16).lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun V2CellRow(index: Int, cell: NearbyCell, snapshot: RouterSnapshot, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val current = cell.rat == "LTE" && cell.pci == snapshot.pci && cell.arfcn == snapshot.earfcn
    val lockable = cell.rat == "LTE" && cell.pci != null && cell.arfcn != null
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (current) V2SoftGreen else Color(0xFFF8FAFD)).border(1.dp, V2Border, RoundedCornerShape(18.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (current) V2Green else V2SoftBlue), contentAlignment = Alignment.Center) {
            Text(index.toString(), color = if (current) Color.White else V2Blue, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("${cell.rat} ${cell.band ?: "—"} • PCI ${cell.pci ?: "—"}", color = V2Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text("ARFCN ${cell.arfcn ?: "—"} • RSRP ${cell.rsrp?.let(::v2Fmt1) ?: "—"}", color = V2Muted, fontSize = 11.sp)
            val confidence = when (cell.confidence) {
                CellConfidence.HIGH -> "ثقة عالية"
                CellConfidence.MEDIUM -> "ثقة متوسطة"
                CellConfidence.LOW -> "ثقة منخفضة"
                null -> "الثقة غير مكتملة"
            }
            Text("$confidence • الظهور ${cell.presencePercent}% • الدليل ${cell.evidenceScore ?: "—"}/100", color = V2Muted, fontSize = 10.sp)
        }
        if (lockable) {
            Text("ثبّت", color = if (!busy) V2Blue else V2Muted, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(50)).clickable(enabled = !busy) { onLock(cell) }.padding(10.dp))
        } else {
            Text("قراءة فقط", color = V2Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun V2Bands(
    layout: V2Layout,
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
    var filter by rememberSaveable { mutableStateOf(V2BandFilter.ALL) }
    val rows = remember(snapshot.cells, capabilities, selectedLte, selectedNr) {
        v2BuildBandRows(snapshot, capabilities, selectedLte, selectedNr)
    }
    val filtered = rows.filter {
        when (filter) {
            V2BandFilter.ALL -> true
            V2BandFilter.NR -> it.nr
            V2BandFilter.LTE -> !it.nr
        }
    }

    V2Card(layout) {
        Column(Modifier.padding(18.dp)) {
            Text("الترددات", color = V2Ink, fontSize = v2sp(layout, 20), fontWeight = FontWeight.Black)
            Text("اختر الترددات المطلوبة؛ النشط يظهر فقط من Carrier حي ومتحقق منه", color = V2Muted, fontSize = v2sp(layout, 11))
            Spacer(Modifier.height(14.dp))

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V2Filter("الكل", rows.size, filter == V2BandFilter.ALL) { filter = V2BandFilter.ALL }
                V2Filter("5G", rows.count { it.nr }, filter == V2BandFilter.NR) { filter = V2BandFilter.NR }
                V2Filter("4G", rows.count { !it.nr }, filter == V2BandFilter.LTE) { filter = V2BandFilter.LTE }
            }

            Spacer(Modifier.height(16.dp))
            filtered.chunked(layout.bandColumns).forEach { rowChunk ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    rowChunk.forEach { band ->
                        V2BandTile(band, busy, Modifier.weight(1f)) {
                            if (band.nr) onNrToggle(band.band) else onLteToggle(band.band)
                        }
                    }
                    repeat(layout.bandColumns - rowChunk.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(9.dp))
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V2PrimaryAction("تطبيق 4G والتحقق", selectedLte.isNotEmpty() && !busy, Modifier.weight(1f), onApplyLte)
                V2PrimaryAction("تطبيق 5G والتحقق", selectedNr.isNotEmpty() && !busy && capabilities.supportsNrBandLock, Modifier.weight(1f), onApplyNr)
            }
        }
    }
}

@Composable
private fun V2BandTile(row: V2BandRow, busy: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val bg = when {
        row.active -> V2SoftGreen
        row.selected -> V2SoftBlue
        else -> Color(0xFFF8FAFD)
    }
    val border = when {
        row.active -> V2Green
        row.selected -> V2Blue
        else -> V2Border
    }
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(bg).border(1.5.dp, border, RoundedCornerShape(18.dp)).clickable(enabled = !busy, onClick = onToggle).padding(vertical = 13.dp, horizontal = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${if (row.nr) "N" else "B"}${row.band}", color = V2Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(if (row.active) "نشط" else if (row.selected) "مختار" else "متاح", color = if (row.active) V2Green else if (row.selected) V2Blue else V2Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        row.bandwidthMhz?.let { Text("${v2Fmt0(it)} MHz", color = V2Muted, fontSize = 10.sp) }
    }
}

@Composable
private fun V2Filter(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) V2Blue else Color(0xFFF0F3F8)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (selected) Color.White else V2Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(7.dp))
        Text(count.toString(), color = if (selected) Color.White else V2Blue, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V2Tools(
    layout: V2Layout,
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
        V2Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("التحسين الذكي", color = V2Ink, fontSize = v2sp(layout, 18), fontWeight = FontWeight.Black)
                        Text("يستخدم فقط إعدادات يمكن التحقق منها واستعادتها", color = V2Muted, fontSize = v2sp(layout, 11))
                    }
                    Switch(checked = smartMode, onCheckedChange = onSmartModeChange, colors = SwitchDefaults.colors(checkedTrackColor = V2Mint))
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptimizationGoal.entries.forEach { goal ->
                        V2Choice(v2Goal(goal), smartGoal == goal) { onSmartGoalChange(goal) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                V2PrimaryAction(if (smartBusy) "جاري التحسين…" else "تحسين الآن", !smartBusy && !busy, Modifier.fillMaxWidth(), onOptimizeNow)
                smartReport?.let { Text(it.message, color = V2Muted, fontSize = v2sp(layout, 11), modifier = Modifier.padding(top = 10.dp)) }
            }
        }

        V2Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("مساعد مكان الراوتر", color = V2Ink, fontSize = v2sp(layout, 18), fontWeight = FontWeight.Black)
                        Text("راقب جودة الإشارة أثناء تحريك الراوتر", color = V2Muted, fontSize = v2sp(layout, 11))
                    }
                    Switch(checked = placementMode, onCheckedChange = { onPlacementToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = V2Mint))
                }
                if (placementReading != null) Text("المساعد يستقبل القياسات الحية ويحدّث التوجيه تلقائيًا", color = V2Blue, fontSize = v2sp(layout, 11), modifier = Modifier.padding(top = 10.dp))
            }
        }

        V2Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Text("تشخيص الـFirmware", color = V2Ink, fontSize = v2sp(layout, 18), fontWeight = FontWeight.Black)
                Text("حالة كل وظيفة حسب الأدلة التي أعادها الراوتر", color = V2Muted, fontSize = v2sp(layout, 11))
                Spacer(Modifier.height(12.dp))
                if (runtime == null) {
                    Text("فحص قدرات الـFirmware لم يكتمل بعد", color = V2Muted, fontSize = v2sp(layout, 12))
                } else {
                    V2RuntimeRow("ترددات 4G", runtime.lteBandControl)
                    V2RuntimeRow("ترددات 5G", runtime.nrBandControl)
                    V2RuntimeRow("قفل الخلية", runtime.cellLock)
                    V2RuntimeRow("وضع الشبكة", runtime.networkMode)
                    V2RuntimeRow("مسح الخلايا", runtime.neighborScan)
                    V2RuntimeRow("الهوائي", runtime.antennaControl)
                    V2RuntimeRow("تجميع الترددات CA", runtime.carrierAggregationTelemetry)
                }
                Spacer(Modifier.height(8.dp))
                Text("الثبات: ${stability.summary}", color = V2Muted, fontSize = v2sp(layout, 11))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    V2OutlineAction("نسخ التقرير", true, Modifier.weight(1f), onCopyDiagnostics)
                    V2OutlineAction("مشاركة التقرير", true, Modifier.weight(1f), onShareDiagnostics)
                }
            }
        }

        V2Card(layout) {
            Column(Modifier.padding(18.dp)) {
                Text("الأمان والاستعادة", color = V2Ink, fontSize = v2sp(layout, 18), fontWeight = FontWeight.Black)
                Text(if (backupAvailable) "توجد نسخة أمان محلية موثّقة قابلة للاستعادة" else "لا توجد نسخة أمان محفوظة حتى الآن", color = V2Muted, fontSize = v2sp(layout, 11))
                Spacer(Modifier.height(12.dp))
                V2PrimaryAction("استعادة والتحقق", backupAvailable && !busy, Modifier.fillMaxWidth(), onRestore)
            }
        }

        if (supportsAntenna) {
            V2Card(layout) {
                Column(Modifier.padding(18.dp)) {
                    Text("التحكم بالهوائي", color = V2Ink, fontSize = v2sp(layout, 18), fontWeight = FontWeight.Black)
                    Text("يظهر فقط عندما يثبت الـFirmware أن مسار الكتابة آمن", color = V2Muted, fontSize = v2sp(layout, 11))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { state -> V2OutlineAction("الوضع $state", !busy, Modifier.weight(1f)) { onAntennaState(state) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun V2RuntimeRow(label: String, evidence: RuntimeCapabilityEvidence) {
    val state = when (evidence.state) {
        RuntimeCapabilityState.SAFE_TO_ATTEMPT -> "جاهز للمحاولة"
        RuntimeCapabilityState.READ_ONLY -> "قراءة فقط"
        RuntimeCapabilityState.PROFILE_ONLY -> "دعم نظري"
        RuntimeCapabilityState.UNAVAILABLE -> "غير متاح"
    }
    val color = if (evidence.state == RuntimeCapabilityState.SAFE_TO_ATTEMPT) V2Green else V2Muted
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = V2Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(state, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V2Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (selected) V2Blue else Color(0xFFF0F3F8)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(text, color = if (selected) Color.White else V2Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V2SectionTitle(layout: V2Layout, title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
        Text(title, color = V2Ink, fontSize = v2sp(layout, 18), fontWeight = FontWeight.Black)
        Text(subtitle, color = V2Muted, fontSize = v2sp(layout, 11))
    }
}

@Composable
private fun V2Message(layout: V2Layout, message: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(V2SoftBlue).border(1.dp, Color(0xFFD9E5FA), RoundedCornerShape(18.dp)).padding(horizontal = 15.dp, vertical = 12.dp)) {
        Text(message, color = V2BlueDark, fontSize = v2sp(layout, 12), maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V2PrimaryAction(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) V2Blue else Color(0xFFE8EBF0)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.White else V2Muted, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V2OutlineAction(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) Color.White else Color(0xFFF2F3F6)).border(1.dp, if (enabled) V2Border else Color(0xFFE2E4E8), RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) V2Ink else V2Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V2CompactAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (enabled) V2SoftBlue else Color(0xFFF2F3F6)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp)) {
        Text(text, color = if (enabled) V2Blue else V2Muted, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V2Card(layout: V2Layout, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val radius = if (layout.compact) 24.dp else 28.dp
    Card(
        modifier = modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(radius)),
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = V2Surface),
        border = BorderStroke(1.dp, V2Border)
    ) { content() }
}

@Composable
private fun V2BottomNav(layout: V2Layout, selected: V2Section, onSelect: (V2Section) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    Row(
        modifier.navigationBarsPadding().height(82.dp).shadow(16.dp, shape).clip(shape).background(Color.White.copy(alpha = .98f)).padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        V2NavItem("⌂", "الرئيسية", selected == V2Section.HOME, Modifier.weight(1f)) { onSelect(V2Section.HOME) }
        V2NavItem("⌁", "الشبكة", selected == V2Section.NETWORK, Modifier.weight(1f)) { onSelect(V2Section.NETWORK) }
        V2NavItem("⌾", "الأبراج", selected == V2Section.TOWERS, Modifier.weight(1f)) { onSelect(V2Section.TOWERS) }
        V2NavItem("▦", "الترددات", selected == V2Section.BANDS, Modifier.weight(1f)) { onSelect(V2Section.BANDS) }
        V2NavItem("⚒", "الأدوات", selected == V2Section.TOOLS, Modifier.weight(1f)) { onSelect(V2Section.TOOLS) }
    }
}

@Composable
private fun V2NavItem(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected) V2Blue else V2Muted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (selected) V2Blue else V2Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(if (selected) 5.dp else 0.dp).clip(CircleShape).background(V2Blue))
    }
}

private fun v2BuildBandRows(snapshot: RouterSnapshot, capabilities: RouterCapabilities, selectedLte: Set<Int>, selectedNr: Set<Int>): List<V2BandRow> {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val nrRows = capabilities.supportedNrBands.sorted().map { band ->
        val carrier = snapshot.cells.firstOrNull { it.role == CellRole.NR && v2BandNumber(it.band) == band && nrVerified }
        V2BandRow(true, band, carrier != null, band in selectedNr, carrier?.arfcn, carrier?.bandwidthMhz)
    }
    val lteRows = capabilities.supportedLteBands.sorted().map { band ->
        val carrier = snapshot.cells.firstOrNull { it.role != CellRole.NR && v2BandNumber(it.band) == band && lteVerified }
        V2BandRow(false, band, carrier != null, band in selectedLte, carrier?.arfcn, carrier?.bandwidthMhz)
    }
    return nrRows + lteRows
}

private fun v2BandNumber(value: String?): Int? = value?.trim()?.removePrefix("n")?.removePrefix("N")?.removePrefix("b")?.removePrefix("B")?.toIntOrNull()

private fun v2Quality(rsrp: Double?): Pair<Color, String> = when {
    rsrp == null -> Color(0xFF8C96A8) to "غير معروف"
    rsrp >= -85 -> V2Green to "ممتاز"
    rsrp >= -95 -> Color(0xFF2B9C74) to "جيد"
    rsrp >= -105 -> V2Amber to "متوسط"
    else -> V2Red to "ضعيف"
}

private fun v2Goal(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun v2Fmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
private fun v2Fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun v2sp(layout: V2Layout, base: Int) = (base * layout.spec.textScale.coerceIn(1.0f, 1.14f)).sp
