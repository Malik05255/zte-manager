package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import java.util.Locale
import kotlin.math.max

private val D = HaiReferenceDesign

@Composable
fun TargetHomeDashboard(
    snapshot: RouterSnapshot,
    telemetrySamples: List<SafeTelemetrySample>,
    status: String,
    operationMessage: String,
    lastPerformance: NetworkPerformance?,
    speedBusy: Boolean,
    controlBusy: Boolean,
    smartBusy: Boolean,
    onDisconnect: () -> Unit,
    onSpeedTest: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onNavigateNetwork: () -> Unit,
    onNavigateTowers: () -> Unit,
    onNavigateBands: () -> Unit,
    onNavigateTools: () -> Unit,
    onNavigateLogs: () -> Unit,
    onNavigateMore: () -> Unit,
    onRefreshNow: () -> Unit,
    onOptimizeNow: () -> Unit
) {
    val ui = LocalHaiUiMetrics.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.fillMaxSize().background(D.Background)) {
            HaiSharedHeader(
                connected = status.contains("متصل") || snapshot.networkType != null,
                onDisconnect = onDisconnect,
                onMenu = onNavigateMore,
                onSettings = onNavigateTools,
                onSearch = onNavigateTowers
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ui.pagePadding)
            ) {
                HomeHeroCard(snapshot, Modifier.fillMaxWidth().height(D.HeroHeight))
                Spacer(Modifier.height(ui.sectionGap))
                HomeMetricRow(snapshot, Modifier.fillMaxWidth().height(D.MetricHeight))
                Spacer(Modifier.height(ui.sectionGap))

                Row(
                    modifier = Modifier.fillMaxWidth().height(D.MiddleRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(ui.sectionGap)
                ) {
                    HomeSpeedCard(
                        performance = lastPerformance,
                        busy = speedBusy,
                        onSpeedTest = onSpeedTest,
                        modifier = Modifier.weight(1.24f).fillMaxHeight()
                    )
                    HomeNetworkModeCard(
                        snapshot = snapshot,
                        busy = controlBusy,
                        onSetNetworkMode = onSetNetworkMode,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Spacer(Modifier.height(ui.sectionGap))

                Row(
                    modifier = Modifier.fillMaxWidth().height(D.TowerRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(ui.sectionGap)
                ) {
                    HomeTowerCard(snapshot, onNavigateTowers, Modifier.weight(1.24f).fillMaxHeight())
                    HomeSignalCard(snapshot, telemetrySamples, Modifier.weight(1f).fillMaxHeight())
                }
                Spacer(Modifier.height(ui.sectionGap))

                Row(
                    modifier = Modifier.fillMaxWidth().height(D.BottomRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(ui.sectionGap)
                ) {
                    HomeBandsCard(snapshot, onNavigateBands, Modifier.weight(1.24f).fillMaxHeight())
                    HomeQuickToolsCard(
                        smartBusy = smartBusy,
                        onOptimize = onOptimizeNow,
                        onDiagnostics = onNavigateTools,
                        onBands = onNavigateBands,
                        onRefresh = onRefreshNow,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }

                if (operationMessage.isNotBlank()) {
                    Text(
                        operationMessage,
                        color = D.Muted,
                        fontSize = 7.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
                    )
                } else {
                    Spacer(Modifier.height(ui.sectionGap))
                }
            }

            HaiSharedBottomNav(
                selected = "home",
                onHome = {},
                onNetwork = onNavigateNetwork,
                onTools = onNavigateTools,
                onLogs = onNavigateLogs,
                onMore = onNavigateMore
            )
        }
    }
}

@Composable
private fun HomeHeroCard(snapshot: RouterSnapshot, modifier: Modifier) {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val network = when { nrVerified -> "5G"; lteVerified -> "4G"; else -> "—" }
    val mode = verifiedRadioMode(snapshot, nrVerified, lteVerified)
    val rsrp = if (nrVerified) snapshot.nrRsrp else snapshot.lteRsrp
    val bands = activeBandLabels(snapshot)
    val aggregation = aggregationSummary(snapshot, bands)

    HaiReferenceCard(modifier) {
        Row(Modifier.fillMaxSize()) {
            RouterIllustration(Modifier.fillMaxHeight().weight(.30f))
            Column(Modifier.fillMaxHeight().weight(.70f).padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("حالة الشبكة", color = D.Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(4.dp))
                            SignalBars(D.Blue, Modifier.size(16.dp, 18.dp), 5)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(15.dp)).background(if (rsrp != null) D.SoftGreen else D.Soft)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(signalQuality(rsrp), color = if (rsrp != null) D.GreenInk else D.Muted, fontSize = 8.4.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(4.dp))
                            SignalBars(if (rsrp != null) D.Green else D.Muted, Modifier.size(12.dp, 11.dp), 4)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (nrVerified || lteVerified) "أنت متصل بالإنترنت" else "لم يتم إثبات اتصال الراديو",
                            color = D.Muted,
                            fontSize = 7.2.sp,
                            maxLines = 1
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(network, color = D.Blue, fontSize = 42.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black)
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(D.SoftBlue).padding(horizontal = 9.dp, vertical = 2.dp)) {
                            Text(mode, color = D.Blue, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (network == "—") "الشبكة الحالية غير مؤكدة" else "الشبكة الحالية: $network $mode",
                            color = D.Ink,
                            fontSize = 6.6.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFAFCFF)).border(.7.dp, D.Border, RoundedCornerShape(14.dp)).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1.18f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الترددات المتصلة الآن", color = D.Ink, fontSize = 8.4.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(3.dp)); RadioGlyph(Modifier.size(11.dp), D.Blue)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val visible = bands.take(3)
                            if (visible.isEmpty()) {
                                HeroBandChip("—", "غير مؤكد", D.SoftBlue)
                            } else {
                                visible.forEachIndexed { index, band ->
                                    HeroBandChip(
                                        band,
                                        if (band.startsWith("n", true)) "5G" else "LTE",
                                        listOf(Color(0xFFE5F2FF), D.SoftPurple, D.SoftGreen)[index % 3]
                                    )
                                }
                            }
                        }
                    }
                    Box(Modifier.width(1.dp).height(38.dp).background(D.Border))
                    Spacer(Modifier.width(5.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الدمج النشط", color = D.Ink, fontSize = 8.4.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(3.dp)); LayersGlyph(Modifier.size(11.dp), D.Blue)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(D.SoftBlue)
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                aggregation.carriers,
                                color = D.Ink,
                                fontSize = 7.3.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            aggregation.badge?.let {
                                Spacer(Modifier.width(4.dp))
                                Box(Modifier.clip(RoundedCornerShape(7.dp)).background(D.Blue).padding(horizontal = 5.dp, vertical = 3.dp)) {
                                    Text(it, color = Color.White, fontSize = 6.4.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouterIllustration(modifier: Modifier) {
    Box(modifier.background(Brush.linearGradient(listOf(Color(0xFF39A4FF), Color(0xFFBFE4FF), Color(0xFFF3F9FF)))), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * .50f, size.height * .47f)
            repeat(3) { i ->
                drawCircle(Color.White.copy(alpha = .20f - i * .03f), radius = size.width * (.22f + i * .12f), center = center, style = Stroke(2f))
            }
            val w = size.width * .42f
            val h = size.height * .67f
            val left = center.x - w / 2f
            val top = center.y - h / 2f
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color.White, Color(0xFFF5F8FB), Color(0xFFDCE5EE))),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(16f)
            )
            drawRoundRect(
                color = Color(0xFFD7E0E9),
                topLeft = Offset(left + w * .06f, top - 2f),
                size = Size(w * .88f, 4f),
                cornerRadius = CornerRadius(4f)
            )
            repeat(4) { i ->
                drawCircle(
                    if (i < 3) D.Green else Color(0xFFB8C4D4),
                    radius = 2.8f,
                    center = Offset(left + w * .82f, top + h * (.38f + i * .075f))
                )
            }
        }
        Text("ZTE", color = Color(0xFF9AA7B8), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 4.dp))
    }
}

@Composable
private fun HeroBandChip(name: String, tech: String, background: Color) {
    Column(
        modifier = Modifier.width(35.dp).clip(RoundedCornerShape(9.dp)).background(background).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(name, color = D.Ink, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(tech, color = D.Blue, fontSize = 6.1.sp, maxLines = 1)
    }
}

@Composable
private fun HomeMetricRow(snapshot: RouterSnapshot, modifier: Modifier) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val sinr = if (nr) snapshot.nrSinr else snapshot.lteSinr
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(LocalHaiUiMetrics.current.sectionGap)) {
        MetricCard("RSRP", rsrp, "dBm", D.Blue, Modifier.weight(1f).fillMaxHeight())
        MetricCard("SINR", sinr, "dB", D.Green, Modifier.weight(1f).fillMaxHeight())
        MetricCard("RSRQ", snapshot.lteRsrq, "dB", D.Blue, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun MetricCard(label: String, value: Double?, unit: String, accent: Color, modifier: Modifier) {
    HaiReferenceCard(modifier, radius = 13.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SignalBars(accent, Modifier.size(13.dp, 18.dp), 4)
            Spacer(Modifier.width(7.dp))
            Column {
                Text(label, color = D.Muted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                Text(value?.let(::f1) ?: "—", color = D.Ink, fontSize = 16.5.sp, fontWeight = FontWeight.Black)
                Text(unit, color = D.Muted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun HomeSpeedCard(performance: NetworkPerformance?, busy: Boolean, onSpeedTest: () -> Unit, modifier: Modifier) {
    HaiReferenceCard(modifier) {
        Column(Modifier.fillMaxSize().padding(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(performance?.measuredAtEpochMs?.let(::relativeTestTime) ?: "لم يُجر اختبار بعد", color = D.Muted, fontSize = 6.4.sp)
                    performance?.latencyMs?.let { Text("زمن الاستجابة ${f0(it)} ms", color = D.Muted, fontSize = 6.1.sp) }
                }
                Spacer(Modifier.weight(1f))
                Text("اختبار السرعة", color = D.Ink, fontSize = 11.2.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(4.dp)); SpeedGlyph(Modifier.size(14.dp), D.Blue)
            }
            Spacer(Modifier.height(2.dp))
            Row(Modifier.weight(1f)) {
                SpeedGauge(performance?.downloadMbps, Modifier.weight(1f).fillMaxHeight())
                Column(
                    modifier = Modifier.width(50.dp).fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(D.Soft)
                        .padding(vertical = 5.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("↑", color = D.Purple, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(performance?.uploadMbps?.let(::f1) ?: "—", color = D.Ink, fontSize = 13.5.sp, fontWeight = FontWeight.Black)
                    Text("Mb/s", color = D.Muted, fontSize = 7.3.sp)
                    Text("رفع", color = D.Muted, fontSize = 6.2.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSpeedTest,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(30.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = D.Blue, disabledContainerColor = Color(0xFFB7CBEA)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(if (busy) "جاري الاختبار…" else "▶  بدء الاختبار", color = Color.White, fontSize = 9.2.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpeedGauge(download: Double?, modifier: Modifier) {
    val fraction = ((download ?: 0.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(76.dp)) {
            val stroke = max(9f, size.width * .055f)
            val arcSize = Size(size.width * .90f, size.height * 1.52f)
            val topLeft = Offset(size.width * .05f, size.height * .14f)
            drawArc(Color(0xFFDDE6F1), 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (fraction > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(D.Blue, Color(0xFF2C97FA), Color(0xFF33D3CA))),
                    startAngle = 180f,
                    sweepAngle = 180f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            repeat(6) { i ->
                val x = size.width * (.08f + i * .168f)
                val y = when (i) { 0,5 -> size.height*.73f; 1,4 -> size.height*.43f; else -> size.height*.28f }
                drawCircle(D.Muted.copy(alpha=.32f), 1.5f, Offset(x, y))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 10.dp)) {
            Text("↓", color = D.Blue, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(download?.let(::f1) ?: "—", color = D.Ink, fontSize = 19.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Black)
            Text("Mb/s تنزيل", color = D.Muted, fontSize = 7.sp)
        }
    }
}

@Composable
private fun HomeNetworkModeCard(snapshot: RouterSnapshot, busy: Boolean, onSetNetworkMode: (String) -> Unit, modifier: Modifier) {
    val current = snapshot.raw["BearerPreference"].orEmpty()
    HaiReferenceCard(modifier) {
        Column(Modifier.fillMaxSize().padding(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("وضع الشبكة", color = D.Ink, fontSize = 11.2.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f)); RadioGlyph(Modifier.size(14.dp), D.Blue)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ModeTile("3G فقط", current == "Only_WCDMA", false, busy, Modifier.weight(1f)) { onSetNetworkMode("Only_WCDMA") }
                ModeTile("4G فقط", current == "Only_LTE", false, busy, Modifier.weight(1f)) { onSetNetworkMode("Only_LTE") }
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ModeTile("5G فقط", current == "Only_5G", false, busy, Modifier.weight(1f)) { onSetNetworkMode("Only_5G") }
                ModeTile("تلقائي", current == "WL_AND_5G" || current == "LTE_AND_5G", true, busy, Modifier.weight(1f)) { onSetNetworkMode("WL_AND_5G") }
            }
            Spacer(Modifier.height(3.dp))
            Text(networkModeCaption(current), color = D.Muted, fontSize = 5.9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun ModeTile(title: String, selected: Boolean, automatic: Boolean, busy: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(11.dp),
        color = if (selected) D.Blue else D.Soft,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(.5.dp, D.Border)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (automatic) {
                Box(Modifier.size(18.dp).border(1.2.dp, if (selected) Color.White else D.Muted, CircleShape), contentAlignment = Alignment.Center) {
                    Text("A", color = if (selected) Color.White else D.Muted, fontSize = 7.5.sp, fontWeight = FontWeight.Black)
                }
            } else {
                SignalBars(if (selected) Color.White else D.Ink, Modifier.size(16.dp, 15.dp), 4)
            }
            Spacer(Modifier.height(3.dp))
            Text(title, color = if (selected) Color.White else D.Ink, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeTowerCard(snapshot: RouterSnapshot, onOpen: () -> Unit, modifier: Modifier) {
    val network = when {
        snapshot.raw["_zte_nr_active_verified"].equals("true", true) -> snapshot.networkType ?: "5G"
        snapshot.raw["_zte_lte_active_verified"].equals("true", true) -> "4G LTE"
        else -> "غير مؤكد"
    }
    HaiReferenceCard(modifier) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioGlyph(Modifier.size(15.dp), D.Blue)
                Spacer(Modifier.weight(1f)); Text("أقرب برج شبكة", color = D.Ink, fontSize = 10.4.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(4.dp)); PinGlyph(Modifier.size(13.dp), D.Blue)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.weight(1f)) {
                Column(Modifier.width(82.dp), verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
                    InfoLine("المسافة", "غير مؤكدة")
                    InfoLine("الهوية", snapshot.cellId?.toString() ?: "—")
                    InfoLine("الشبكة", network)
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    MiniMap(snapshot.cellId?.toString() ?: "—", Modifier.weight(1f).fillMaxWidth())
                    Spacer(Modifier.height(3.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(22.dp),
                        onClick = onOpen,
                        shape = RoundedCornerShape(10.dp),
                        color = D.SoftBlue
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("عرض على الخريطة", color = D.Blue, fontSize = 7.4.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(title: String, value: String) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = D.Muted, fontSize = 6.1.sp)
        Text(value, color = D.Ink, fontSize = 7.2.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MiniMap(label: String, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(11.dp)).background(Color(0xFFF4F7F5))) {
        Canvas(Modifier.fillMaxSize()) {
            val road = Color(0xFFDDE3E7)
            val block = Color(0xFFEEF2EF)
            repeat(4) { i ->
                drawRoundRect(block, Offset(size.width*(.05f+i*.24f), size.height*.12f), Size(size.width*.18f,size.height*.22f), CornerRadius(4f))
                drawRoundRect(block, Offset(size.width*(.13f+i*.21f), size.height*.62f), Size(size.width*.15f,size.height*.22f), CornerRadius(4f))
            }
            repeat(4) { i ->
                val y = size.height * (.18f + i*.22f)
                drawLine(road, Offset(0f,y), Offset(size.width,y+12f), 2.5f)
            }
            repeat(3) { i ->
                val x = size.width * (.22f + i*.25f)
                drawLine(road, Offset(x,0f), Offset(x-10f,size.height), 2.5f)
            }
            val p = Offset(size.width*.57f,size.height*.46f)
            drawCircle(D.Blue.copy(alpha=.16f), size.width*.17f, p)
            drawCircle(D.Blue.copy(alpha=.24f), size.width*.10f, p)
            drawCircle(D.Blue, size.width*.045f, p)
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom=3.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal=6.dp,vertical=2.dp)) {
            Text(label, color = D.Ink, fontSize = 6.1.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun HomeSignalCard(snapshot: RouterSnapshot, samples: List<SafeTelemetrySample>, modifier: Modifier) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val values = samples.mapNotNull { if (it.nrVerified) it.nrRsrp else it.lteRsrp }.takeLast(30)
    val stable = values.takeLast(6).let { it.size >= 3 && (it.maxOrNull()!! - it.minOrNull()!!) <= 8.0 }

    HaiReferenceCard(modifier) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("مراقبة الإشارة المباشرة", color = D.Ink, fontSize = 9.6.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f)); PulseGlyph(Modifier.size(14.dp), D.Blue)
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rsrp?.let { "${f0(it)} dBm" } ?: "—", color = D.Ink, fontSize = 17.5.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(if (stable) D.SoftGreen else D.SoftBlue).padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (stable) "مستقر" else "حي", color = if (stable) D.GreenInk else D.Blue, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp)); Box(Modifier.size(6.dp).background(if (stable) D.Green else D.Blue, CircleShape))
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().weight(1f)) {
                SignalGraph(values, Modifier.fillMaxSize().padding(end = 18.dp))
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf("-60","-80","-100","-120").forEach { Text(it, color = D.Muted, fontSize = 5.5.sp) }
                }
            }
            Text(
                if (values.isEmpty()) "بانتظار القراءات الحية" else "آخر ${values.size * 2} ثانية تقريبًا",
                color = D.Muted,
                fontSize = 5.8.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun SignalGraph(values: List<Double>, modifier: Modifier) {
    Canvas(modifier) {
        val grid = Color(0xFFE2EDF8)
        repeat(7) { i -> val x = size.width*i/6f; drawLine(grid, Offset(x,0f), Offset(x,size.height), 1f) }
        repeat(4) { i -> val y = size.height*i/3f; drawLine(grid, Offset(0f,y), Offset(size.width,y), 1f) }
        if (values.size < 2) return@Canvas
        val line = Path()
        val area = Path()
        values.forEachIndexed { index, value ->
            val x = size.width*index/(values.size-1).coerceAtLeast(1)
            val y = size.height*((-60.0-value)/60.0).coerceIn(0.0,1.0).toFloat()
            if (index == 0) { line.moveTo(x,y); area.moveTo(x,size.height); area.lineTo(x,y) } else { line.lineTo(x,y); area.lineTo(x,y) }
            if (index == values.lastIndex) { area.lineTo(x,size.height); area.close() }
        }
        drawPath(area, Brush.verticalGradient(listOf(D.Blue.copy(alpha=.17f), D.Blue.copy(alpha=.01f))))
        drawPath(line, D.Blue, style=Stroke(2.5f, cap=StrokeCap.Round))
    }
}

@Composable
private fun HomeBandsCard(snapshot: RouterSnapshot, onOpen: () -> Unit, modifier: Modifier) {
    val bands = activeBandLabels(snapshot).take(6)
    HaiReferenceCard(modifier) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
                RadioGlyph(Modifier.size(14.dp), D.Blue)
                Spacer(Modifier.width(4.dp)); Text("الترددات النشطة", color = D.Ink, fontSize = 9.8.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f)); Text("عرض الكل ‹", color = D.Blue, fontSize = 6.8.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(6) { index ->
                    val band = bands.getOrNull(index)
                    BandTile(
                        name = band ?: "—",
                        tech = when { band == null -> ""; band.startsWith("n", true) -> "5G"; else -> "LTE" },
                        background = listOf(Color(0xFFE5F2FF), D.SoftGreen, Color(0xFFEAF9F0), D.SoftPurple, D.SoftGold, Color(0xFFEAF1FF))[index],
                        active = band != null,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun BandTile(name: String, tech: String, background: Color, active: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(if (active) background else D.Soft.copy(alpha=.65f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(name, color = if (active) D.Ink else D.Muted.copy(alpha=.55f), fontSize = 8.3.sp, fontWeight = FontWeight.Black, maxLines = 1)
        if (tech.isNotBlank()) Text(tech, color = if (tech == "5G") D.Blue else D.Muted, fontSize = 6.sp)
    }
}

private enum class ToolIcon { ROCKET, DIAGNOSE, LOCK, REFRESH }

@Composable
private fun HomeQuickToolsCard(
    smartBusy: Boolean,
    onOptimize: () -> Unit,
    onDiagnostics: () -> Unit,
    onBands: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier
) {
    HaiReferenceCard(modifier) {
        Column(Modifier.fillMaxSize().padding(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f)); Text("أدوات سريعة", color = D.Ink, fontSize = 9.8.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(4.dp)); GridGlyph(Modifier.size(12.dp), D.Blue)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ToolTile(ToolIcon.ROCKET, if (smartBusy) "جاري\nالتحسين" else "تحسين\nالأداء", D.SoftPurple, Modifier.weight(1f), !smartBusy, onOptimize)
                ToolTile(ToolIcon.DIAGNOSE, "تشخيص\nالشبكة", Color(0xFFE8FBF5), Modifier.weight(1f), true, onDiagnostics)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ToolTile(ToolIcon.LOCK, "قفل\nالترددات", Color(0xFFE8FAF1), Modifier.weight(1f), true, onBands)
                ToolTile(ToolIcon.REFRESH, "تحديث\nالبيانات", D.SoftPurple, Modifier.weight(1f), true, onRefresh)
            }
        }
    }
}

@Composable
private fun ToolTile(icon: ToolIcon, text: String, bg: Color, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(9.dp)).background(bg)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 5.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolGlyph(icon, if (enabled) D.Blue else D.Muted, Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, color = if (enabled) D.Ink else D.Muted, fontSize = 6.2.sp, lineHeight = 6.6.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ToolGlyph(icon: ToolIcon, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        when (icon) {
            ToolIcon.ROCKET -> {
                val p = Path().apply { moveTo(s*.25f,s*.72f); quadraticBezierTo(s*.45f,s*.20f,s*.80f,s*.16f); quadraticBezierTo(s*.82f,s*.53f,s*.30f,s*.78f); close() }
                drawPath(p,color)
                drawCircle(Color.White,s*.08f,Offset(s*.62f,s*.37f))
                drawLine(color,Offset(s*.27f,s*.70f),Offset(s*.12f,s*.86f),s*.10f,StrokeCap.Round)
            }
            ToolIcon.DIAGNOSE -> {
                drawCircle(color,s*.30f,Offset(s*.43f,s*.43f),style=Stroke(s*.10f))
                drawLine(color,Offset(s*.64f,s*.64f),Offset(s*.86f,s*.86f),s*.10f,StrokeCap.Round)
                drawLine(color,Offset(s*.15f,s*.43f),Offset(s*.30f,s*.43f),s*.07f)
            }
            ToolIcon.LOCK -> {
                drawRoundRect(color,Offset(s*.18f,s*.43f),Size(s*.64f,s*.48f),CornerRadius(s*.09f))
                drawArc(color,180f,180f,false,Offset(s*.30f,s*.12f),Size(s*.40f,s*.52f),style=Stroke(s*.10f,cap=StrokeCap.Round))
            }
            ToolIcon.REFRESH -> {
                drawArc(color,35f,285f,false,Offset(s*.16f,s*.16f),Size(s*.68f,s*.68f),style=Stroke(s*.11f,cap=StrokeCap.Round))
                val p=Path().apply{moveTo(s*.77f,s*.12f);lineTo(s*.92f,s*.30f);lineTo(s*.70f,s*.31f);close()};drawPath(p,color)
            }
        }
    }
}

@Composable
private fun SignalBars(color: Color, modifier: Modifier, count: Int = 4) {
    Canvas(modifier) {
        val gap = size.width * .07f
        val bw = (size.width - gap * (count - 1)) / count
        repeat(count) { index ->
            val factor = .30f + index * (.70f / (count - 1).coerceAtLeast(1))
            val h = size.height * factor
            drawRoundRect(color, Offset(index*(bw+gap), size.height-h), Size(bw,h), CornerRadius(bw/2f))
        }
    }
}

@Composable
private fun RadioGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val c = Offset(size.width*.50f,size.height*.58f)
        drawCircle(color,size.width*.08f,c)
        drawLine(color,c,Offset(c.x,size.height*.98f),size.width*.08f,StrokeCap.Round)
        repeat(2){i->val r=size.width*(.25f+i*.18f);drawArc(color,205f,130f,false,Offset(c.x-r,c.y-r),Size(r*2,r*2),style=Stroke(size.width*.07f,cap=StrokeCap.Round))}
    }
}

@Composable
private fun LayersGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        repeat(3){i->val y=size.height*(.18f+i*.23f);val p=Path().apply{moveTo(size.width*.50f,y);lineTo(size.width*.90f,y+size.height*.15f);lineTo(size.width*.50f,y+size.height*.30f);lineTo(size.width*.10f,y+size.height*.15f);close()};drawPath(p,color.copy(alpha=1f-i*.18f))}
    }
}

@Composable
private fun SpeedGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        drawArc(color,195f,150f,false,Offset(size.width*.08f,size.height*.18f),Size(size.width*.84f,size.height*.84f),style=Stroke(size.width*.09f,cap=StrokeCap.Round))
        drawLine(color,Offset(size.width*.50f,size.height*.60f),Offset(size.width*.72f,size.height*.35f),size.width*.08f,StrokeCap.Round)
        drawCircle(color,size.width*.08f,Offset(size.width*.50f,size.height*.60f))
    }
}

@Composable
private fun PinGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val p=Path().apply{moveTo(size.width*.50f,size.height*.94f);cubicTo(size.width*.18f,size.height*.60f,size.width*.14f,size.height*.38f,size.width*.50f,size.height*.12f);cubicTo(size.width*.86f,size.height*.38f,size.width*.82f,size.height*.60f,size.width*.50f,size.height*.94f);close()};drawPath(p,color);drawCircle(Color.White,size.width*.12f,Offset(size.width*.50f,size.height*.42f))
    }
}

@Composable
private fun PulseGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val p=Path().apply{moveTo(0f,size.height*.55f);lineTo(size.width*.22f,size.height*.55f);lineTo(size.width*.34f,size.height*.25f);lineTo(size.width*.47f,size.height*.82f);lineTo(size.width*.60f,size.height*.40f);lineTo(size.width*.72f,size.height*.55f);lineTo(size.width,size.height*.55f)};drawPath(p,color,style=Stroke(size.width*.08f,cap=StrokeCap.Round))
    }
}

@Composable
private fun GridGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val cell=size.width*.22f;repeat(3){r->repeat(3){c->drawRoundRect(color,Offset(size.width*(.07f+c*.31f),size.height*(.07f+r*.31f)),Size(cell,cell),CornerRadius(cell*.18f))}}
    }
}

private data class AggregationSummary(val carriers: String, val badge: String?)

private fun aggregationSummary(snapshot: RouterSnapshot, bands: List<String>): AggregationSummary {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val caVerified = snapshot.raw["_zte_ca_verified"].equals("true", true)
    val lteCarriers = snapshot.cells.filter { it.role != CellRole.NR }.distinctBy { Triple(it.band, it.pci, it.arfcn) }.size
    val nrCarriers = snapshot.cells.filter { it.role == CellRole.NR }.distinctBy { Triple(it.band, it.pci, it.arfcn) }.size
    val carriers = bands.take(4).joinToString(" + ").ifBlank { "غير مؤكد" }
    val badge = when {
        snapshot.caActive && caVerified && lteCarriers >= 2 && nrVerified && nrCarriers >= 1 -> "${lteCarriers}CA + NR"
        snapshot.caActive && caVerified && lteCarriers >= 2 -> "${lteCarriers}CA"
        nrVerified && lteCarriers >= 1 -> "NSA"
        else -> null
    }
    return AggregationSummary(carriers, badge)
}

private fun activeBandLabels(snapshot: RouterSnapshot): List<String> {
    val out = LinkedHashSet<String>()
    if (snapshot.raw["_zte_nr_active_verified"].equals("true", true)) normalizeBand(snapshot.nrBand, CellRole.NR)?.let(out::add)
    snapshot.cells.forEach { normalizeBand(it.band, it.role)?.let(out::add) }
    if (snapshot.raw["_zte_lte_active_verified"].equals("true", true)) normalizeBand(snapshot.lteBand, CellRole.PRIMARY)?.let(out::add)
    return out.toList()
}

private fun normalizeBand(raw: String?, role: CellRole): String? {
    val n = Regex("\\d+").find(raw.orEmpty())?.value ?: return null
    return if (role == CellRole.NR || raw.orEmpty().startsWith("n", true)) "n$n" else "B$n"
}

private fun verifiedRadioMode(snapshot: RouterSnapshot, nr: Boolean, lte: Boolean): String = when {
    nr && snapshot.networkType.orEmpty().contains("SA", true) && !snapshot.networkType.orEmpty().contains("NSA", true) -> "SA"
    nr -> "NSA"
    lte -> "LTE"
    else -> "غير مؤكد"
}

private fun signalQuality(rsrp: Double?): String = when {
    rsrp == null -> "غير مؤكد"
    rsrp >= -80 -> "إشارة ممتازة"
    rsrp >= -90 -> "إشارة جيدة جدًا"
    rsrp >= -100 -> "إشارة جيدة"
    rsrp >= -110 -> "إشارة ضعيفة"
    else -> "إشارة ضعيفة جدًا"
}

private fun networkModeCaption(current: String): String = when (current) {
    "Only_WCDMA" -> "الوضع الحالي: 3G فقط"
    "Only_LTE" -> "الوضع الحالي: 4G فقط"
    "Only_5G" -> "الوضع الحالي: 5G فقط"
    "WL_AND_5G", "LTE_AND_5G" -> "يتم اختيار أفضل شبكة تلقائيًا"
    else -> "الوضع الحالي غير مقروء"
}

private fun relativeTestTime(epochMs: Long): String {
    val minutes = ((System.currentTimeMillis() - epochMs).coerceAtLeast(0L) / 60_000L).toInt()
    return when {
        minutes <= 0 -> "آخر اختبار: الآن"
        minutes == 1 -> "آخر اختبار: منذ دقيقة"
        minutes < 11 -> "آخر اختبار: منذ $minutes دقائق"
        else -> "آخر اختبار: منذ $minutes دقيقة"
    }
}

private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)
private fun f0(v: Double) = String.format(Locale.US, "%.0f", v)
