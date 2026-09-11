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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val H4 = HaiReferenceDesign
private val H4Blue2 = Color(0xFF54B9FF)
private val H4Panel = Color(0xFFF8FBFF)
private val H4Line = Color(0xFFDDE8F5)

@Composable
fun HaiHomeDashboardV4(
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
        Column(Modifier.fillMaxSize().background(H4.Background)) {
            HaiSharedHeader(
                connected = status.contains("متصل") || snapshot.networkType != null,
                onDisconnect = onDisconnect,
                onMenu = onNavigateMore,
                onSettings = onNavigateTools,
                onSearch = onNavigateTowers
            )

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ui.pagePadding)
            ) {
                H4Hero(snapshot, Modifier.fillMaxWidth().height(H4.HeroHeight))
                Spacer(Modifier.height(ui.sectionGap))
                H4Metrics(snapshot, Modifier.fillMaxWidth().height(H4.MetricHeight))
                Spacer(Modifier.height(ui.sectionGap))

                Row(
                    Modifier.fillMaxWidth().height(H4.MiddleRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(ui.sectionGap)
                ) {
                    H4Speed(
                        performance = lastPerformance,
                        busy = speedBusy,
                        onSpeedTest = onSpeedTest,
                        modifier = Modifier.weight(1.18f).fillMaxHeight()
                    )
                    H4NetworkMode(
                        snapshot = snapshot,
                        busy = controlBusy,
                        onSetNetworkMode = onSetNetworkMode,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Spacer(Modifier.height(ui.sectionGap))

                Row(
                    Modifier.fillMaxWidth().height(H4.TowerRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(ui.sectionGap)
                ) {
                    H4Tower(snapshot, onNavigateTowers, Modifier.weight(1.18f).fillMaxHeight())
                    H4Signal(snapshot, telemetrySamples, Modifier.weight(1f).fillMaxHeight())
                }
                Spacer(Modifier.height(ui.sectionGap))

                Row(
                    Modifier.fillMaxWidth().height(H4.BottomRowHeight),
                    horizontalArrangement = Arrangement.spacedBy(ui.sectionGap)
                ) {
                    H4Bands(snapshot, onNavigateBands, Modifier.weight(1.18f).fillMaxHeight())
                    H4Tools(
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
                        color = H4.Muted,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
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
private fun H4Hero(snapshot: RouterSnapshot, modifier: Modifier) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val network = when { nr -> "5G"; lte -> "4G"; else -> "—" }
    val mode = h4Mode(snapshot, nr, lte)
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val bands = h4Bands(snapshot)
    val ca = h4Aggregation(snapshot, bands)

    HaiReferenceCard(modifier, radius = 20.dp) {
        Row(Modifier.fillMaxSize()) {
            H4RouterArt(Modifier.fillMaxHeight().weight(.34f))
            Column(Modifier.fillMaxHeight().weight(.66f).padding(10.dp)) {
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("حالة الشبكة", color = H4.Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(5.dp))
                            H4Bars(H4.Blue, Modifier.size(18.dp, 20.dp), 5)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.clip(RoundedCornerShape(18.dp))
                                .background(if (rsrp != null) H4.SoftGreen else H4.Soft)
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(h4Quality(rsrp), color = if (rsrp != null) H4.GreenInk else H4.Muted, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(5.dp))
                            H4Bars(if (rsrp != null) H4.Green else H4.Muted, Modifier.size(14.dp, 12.dp), 4)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (nr || lte) "أنت متصل بالإنترنت" else "الاتصال الراديوي غير مؤكد",
                            color = H4.Muted,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(network, color = H4.Blue, fontSize = 48.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black)
                        Surface(shape = RoundedCornerShape(9.dp), color = H4.SoftBlue) {
                            Text(mode, color = H4.Blue, fontSize = 9.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (network == "—") "الشبكة الحالية غير مؤكدة" else "الشبكة الحالية: $network $mode",
                            color = H4.Ink,
                            fontSize = 7.6.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().height(62.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(H4Panel)
                        .border(.8.dp, H4.Border, RoundedCornerShape(15.dp))
                        .padding(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1.2f)) {
                        Text("الترددات المتصلة الآن", color = H4.Ink, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val visible = bands.take(3)
                            if (visible.isEmpty()) H4HeroBand("—", "غير مؤكد", H4.SoftBlue)
                            else visible.forEachIndexed { index, band ->
                                H4HeroBand(
                                    band,
                                    if (band.startsWith("n", true)) "5G" else "LTE",
                                    listOf(Color(0xFFE7F3FF), H4.SoftPurple, H4.SoftGreen)[index % 3]
                                )
                            }
                        }
                    }
                    Box(Modifier.width(1.dp).height(42.dp).background(H4.Border))
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("الدمج النشط", color = H4.Ink, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(5.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(H4.SoftBlue).padding(horizontal = 7.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(ca.first, color = H4.Ink, fontSize = 7.8.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            ca.second?.let {
                                Spacer(Modifier.width(5.dp))
                                Surface(shape = RoundedCornerShape(8.dp), color = H4.Blue) {
                                    Text(it, color = Color.White, fontSize = 6.8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp))
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
private fun H4RouterArt(modifier: Modifier) {
    Box(
        modifier.background(Brush.verticalGradient(listOf(Color(0xFF31A3FF), Color(0xFF80C9FF), Color(0xFFDCEEFF)))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * .50f, size.height * .48f)
            repeat(4) { i ->
                drawCircle(Color.White.copy(alpha = .23f - i * .035f), size.width * (.19f + i * .105f), c, style = Stroke(2f))
            }
            val rw = size.width * .48f
            val rh = size.height * .70f
            val left = c.x - rw / 2f
            val top = c.y - rh / 2f
            drawRoundRect(Color(0xFFCFD9E4).copy(alpha = .45f), Offset(left + 5f, top + 7f), Size(rw, rh), CornerRadius(18f))
            drawRoundRect(
                Brush.linearGradient(listOf(Color.White, Color(0xFFF8FAFC), Color(0xFFDCE4EC))),
                Offset(left, top), Size(rw, rh), CornerRadius(18f)
            )
            drawRoundRect(Color(0xFFE4E9EF), Offset(left + rw * .08f, top + rh * .08f), Size(rw * .84f, 4f), CornerRadius(4f))
            repeat(4) { i ->
                drawCircle(if (i < 3) H4.Green else Color(0xFFB9C5D2), 3f, Offset(left + rw * .80f, top + rh * (.42f + i * .07f)))
            }
            drawLine(Color(0xFFCBD5E1), Offset(left + rw * .18f, top + rh * .72f), Offset(left + rw * .82f, top + rh * .72f), 2f)
        }
        Text("ZTE", color = Color(0xFF97A5B5), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 5.dp))
    }
}

@Composable
private fun H4HeroBand(name: String, tech: String, background: Color) {
    Column(
        Modifier.width(38.dp).clip(RoundedCornerShape(10.dp)).background(background).padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(name, color = H4.Ink, fontSize = 8.8.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(tech, color = H4.Blue, fontSize = 6.6.sp, maxLines = 1)
    }
}

@Composable
private fun H4Metrics(snapshot: RouterSnapshot, modifier: Modifier) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val sinr = if (nr) snapshot.nrSinr else snapshot.lteSinr
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(LocalHaiUiMetrics.current.sectionGap)) {
        H4Metric("RSRP", rsrp, "dBm", H4.Blue, Modifier.weight(1f).fillMaxHeight())
        H4Metric("SINR", sinr, "dB", H4.Green, Modifier.weight(1f).fillMaxHeight())
        H4Metric("RSRQ", snapshot.lteRsrq, "dB", H4Blue2, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun H4Metric(label: String, value: Double?, unit: String, accent: Color, modifier: Modifier) {
    HaiReferenceCard(modifier, radius = 14.dp) {
        Row(Modifier.fillMaxSize().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            H4Bars(accent, Modifier.size(15.dp, 21.dp), 4)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, color = H4.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(value?.let(::h4f1) ?: "—", color = H4.Ink, fontSize = 18.sp, lineHeight = 18.sp, fontWeight = FontWeight.Black)
                Text(unit, color = H4.Muted, fontSize = 8.3.sp)
            }
        }
    }
}

@Composable
private fun H4Speed(performance: NetworkPerformance?, busy: Boolean, onSpeedTest: () -> Unit, modifier: Modifier) {
    HaiReferenceCard(modifier, radius = 20.dp) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("اختبار السرعة", color = H4.Ink, fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                    Text(
                        performance?.measuredAtEpochMs?.let { "آخر قياس مسجل" } ?: "لم يُجر اختبار بعد",
                        color = H4.Muted,
                        fontSize = 7.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                H4SpeedIcon(Modifier.size(18.dp), H4.Blue)
            }
            Spacer(Modifier.height(2.dp))
            H4Gauge(performance?.downloadMbps, Modifier.fillMaxWidth().height(72.dp))
            Spacer(Modifier.height(3.dp))
            Row(Modifier.fillMaxWidth().height(35.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                H4SpeedValue("رفع", performance?.uploadMbps, "↑", H4.Purple, Modifier.weight(1f))
                H4SpeedValue("زمن الاستجابة", performance?.latencyMs, "◷", H4.Green, Modifier.weight(1f), unit = "ms")
            }
            Spacer(Modifier.height(5.dp))
            Button(
                onClick = onSpeedTest,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(31.dp),
                shape = RoundedCornerShape(13.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = H4.Blue, disabledContainerColor = Color(0xFFB6CAE8))
            ) {
                Text(if (busy) "جاري الاختبار…" else "بدء الاختبار", color = Color.White, fontSize = 9.6.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun H4Gauge(download: Double?, modifier: Modifier) {
    val fraction = ((download ?: 0.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(horizontal = 7.dp)) {
            val stroke = max(9f, size.width * .052f)
            val arcSize = Size(size.width * .88f, size.height * 1.55f)
            val topLeft = Offset(size.width * .06f, size.height * .16f)
            drawArc(Color(0xFFE0E8F2), 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (fraction > 0f) {
                drawArc(H4.Blue, 180f, 180f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            val center = Offset(size.width / 2f, size.height * .82f)
            repeat(9) { i ->
                val angle = Math.toRadians((180.0 + i * 22.5))
                val r1 = size.width * .34f
                val r2 = size.width * .38f
                val a = Offset(center.x + cos(angle).toFloat() * r1, center.y + sin(angle).toFloat() * r1)
                val b = Offset(center.x + cos(angle).toFloat() * r2, center.y + sin(angle).toFloat() * r2)
                drawLine(H4.Muted.copy(alpha = .45f), a, b, 1.5f)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 8.dp)) {
            Text(download?.let(::h4f1) ?: "—", color = H4.Ink, fontSize = 21.sp, lineHeight = 21.sp, fontWeight = FontWeight.Black)
            Text("Mb/s", color = H4.Muted, fontSize = 7.5.sp)
            Text("تنزيل", color = H4.Blue, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun H4SpeedValue(title: String, value: Double?, icon: String, accent: Color, modifier: Modifier, unit: String = "Mb/s") {
    Row(
        modifier.clip(RoundedCornerShape(11.dp)).background(H4.Soft).padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(5.dp))
        Column {
            Text(title, color = H4.Muted, fontSize = 6.7.sp)
            Text("${value?.let(::h4f1) ?: "—"} $unit", color = H4.Ink, fontSize = 8.2.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun H4NetworkMode(snapshot: RouterSnapshot, busy: Boolean, onSetNetworkMode: (String) -> Unit, modifier: Modifier) {
    val current = snapshot.raw["BearerPreference"].orEmpty()
    HaiReferenceCard(modifier, radius = 20.dp) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("وضع الشبكة", color = H4.Ink, fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                H4RadioIcon(Modifier.size(17.dp), H4.Blue)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                H4ModeTile("3G فقط", current == "Only_WCDMA", busy, Modifier.weight(1f)) { onSetNetworkMode("Only_WCDMA") }
                H4ModeTile("4G فقط", current == "Only_LTE", busy, Modifier.weight(1f)) { onSetNetworkMode("Only_LTE") }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                H4ModeTile("5G فقط", current == "Only_5G", busy, Modifier.weight(1f)) { onSetNetworkMode("Only_5G") }
                H4ModeTile("تلقائي", current == "WL_AND_5G" || current == "LTE_AND_5G", busy, Modifier.weight(1f), automatic = true) { onSetNetworkMode("WL_AND_5G") }
            }
            Spacer(Modifier.height(5.dp))
            Text(h4ModeCaption(current), color = H4.Muted, fontSize = 6.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun H4ModeTile(title: String, selected: Boolean, busy: Boolean, modifier: Modifier, automatic: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(13.dp),
        color = if (selected) H4.Blue else H4.Soft,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(.6.dp, H4.Border)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (automatic) {
                Box(Modifier.size(20.dp).border(1.3.dp, if (selected) Color.White else H4.Muted, CircleShape), contentAlignment = Alignment.Center) {
                    Text("A", color = if (selected) Color.White else H4.Muted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            } else H4Bars(if (selected) Color.White else H4.Ink, Modifier.size(17.dp, 17.dp), 4)
            Spacer(Modifier.height(4.dp))
            Text(title, color = if (selected) Color.White else H4.Ink, fontSize = 8.7.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun H4Tower(snapshot: RouterSnapshot, onOpen: () -> Unit, modifier: Modifier) {
    val network = when {
        snapshot.raw["_zte_nr_active_verified"].equals("true", true) -> snapshot.networkType ?: "5G"
        snapshot.raw["_zte_lte_active_verified"].equals("true", true) -> "4G LTE"
        else -> "غير مؤكد"
    }
    HaiReferenceCard(modifier, radius = 20.dp) {
        Column(Modifier.fillMaxSize().padding(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                H4Pin(Modifier.size(15.dp), H4.Blue)
                Spacer(Modifier.width(4.dp))
                Text("أقرب برج شبكة", color = H4.Ink, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                H4RadioIcon(Modifier.size(15.dp), H4.Blue)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.weight(1f)) {
                H4MiniMap(snapshot.cellId?.toString() ?: "—", Modifier.weight(1.05f).fillMaxHeight())
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(.95f).fillMaxHeight(), horizontalAlignment = Alignment.End) {
                    H4Info("المسافة", "غير مؤكدة")
                    Spacer(Modifier.height(2.dp))
                    H4Info("معرف الخلية", snapshot.cellId?.toString() ?: "—")
                    Spacer(Modifier.height(2.dp))
                    H4Info("الشبكة", network)
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(25.dp),
                        onClick = onOpen,
                        shape = RoundedCornerShape(10.dp),
                        color = H4.SoftBlue
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("عرض على الخريطة", color = H4.Blue, fontSize = 7.8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun H4MiniMap(label: String, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(13.dp)).background(Color(0xFFF3F7F4))) {
        Canvas(Modifier.fillMaxSize()) {
            val road = Color(0xFFD7E0E5)
            val road2 = Color.White
            val park = Color(0xFFDDF1DE)
            val block = Color(0xFFE9EEEA)
            drawRoundRect(park, Offset(size.width*.08f, size.height*.08f), Size(size.width*.27f, size.height*.24f), CornerRadius(6f))
            repeat(5) { i ->
                drawRoundRect(block, Offset(size.width*(.10f + (i%3)*.28f), size.height*(.42f + (i/3)*.28f)), Size(size.width*.18f, size.height*.16f), CornerRadius(4f))
            }
            drawLine(road, Offset(0f,size.height*.34f), Offset(size.width,size.height*.48f), 8f, StrokeCap.Round)
            drawLine(road2, Offset(0f,size.height*.34f), Offset(size.width,size.height*.48f), 3f, StrokeCap.Round)
            drawLine(road, Offset(size.width*.54f,0f), Offset(size.width*.43f,size.height), 8f, StrokeCap.Round)
            drawLine(road2, Offset(size.width*.54f,0f), Offset(size.width*.43f,size.height), 3f, StrokeCap.Round)
            val p = Offset(size.width*.58f, size.height*.46f)
            drawCircle(H4.Blue.copy(alpha=.13f), size.width*.20f, p)
            drawCircle(H4.Blue.copy(alpha=.20f), size.width*.12f, p)
            drawCircle(H4.Blue, size.width*.052f, p)
            drawCircle(Color.White, size.width*.020f, p)
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = .95f)
        ) {
            Text(label, color = H4.Ink, fontSize = 6.6.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), maxLines = 1)
        }
    }
}

@Composable
private fun H4Info(title: String, value: String) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = H4.Muted, fontSize = 6.5.sp)
        Text(value, color = H4.Ink, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun H4Signal(snapshot: RouterSnapshot, samples: List<SafeTelemetrySample>, modifier: Modifier) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val values = samples.mapNotNull { if (it.nrVerified) it.nrRsrp else it.lteRsrp }.takeLast(30)
    val stable = values.takeLast(6).let { it.size >= 3 && (it.maxOrNull()!! - it.minOrNull()!!) <= 8.0 }

    HaiReferenceCard(modifier, radius = 20.dp) {
        Column(Modifier.fillMaxSize().padding(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("مراقبة الإشارة المباشرة", color = H4.Ink, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                H4Pulse(Modifier.size(15.dp), H4.Blue)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rsrp?.let { "${h4f0(it)} dBm" } ?: "—", color = H4.Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(13.dp), color = if (stable) H4.SoftGreen else H4.SoftBlue) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (stable) "مستقر" else "حي", color = if (stable) H4.GreenInk else H4.Blue, fontSize = 7.5.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(4.dp))
                        Box(Modifier.size(6.dp).background(if (stable) H4.Green else H4.Blue, CircleShape))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().weight(1f)) {
                H4Graph(values, Modifier.fillMaxSize().padding(end = 20.dp))
                Column(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                    listOf("-60", "-80", "-100", "-120").forEach { Text(it, color = H4.Muted, fontSize = 5.8.sp) }
                }
            }
            Text(if (values.isEmpty()) "بانتظار القراءات" else "آخر ${values.size * 2} ثانية تقريبًا", color = H4.Muted, fontSize = 6.2.sp, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun H4Graph(values: List<Double>, modifier: Modifier) {
    Canvas(modifier) {
        repeat(7) { i ->
            val x = size.width * i / 6f
            drawLine(H4Line, Offset(x, 0f), Offset(x, size.height), 1f)
        }
        repeat(4) { i ->
            val y = size.height * i / 3f
            drawLine(H4Line, Offset(0f, y), Offset(size.width, y), 1f)
        }
        if (values.size < 2) return@Canvas
        val line = Path()
        val area = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.size - 1).coerceAtLeast(1)
            val y = size.height * ((-60.0 - value) / 60.0).coerceIn(0.0, 1.0).toFloat()
            if (index == 0) {
                line.moveTo(x, y)
                area.moveTo(x, size.height)
                area.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                area.lineTo(x, y)
            }
            if (index == values.lastIndex) {
                area.lineTo(x, size.height)
                area.close()
            }
        }
        drawPath(area, Brush.verticalGradient(listOf(H4.Blue.copy(alpha = .18f), H4.Blue.copy(alpha = .01f))))
        drawPath(line, H4.Blue, style = Stroke(2.8f, cap = StrokeCap.Round))
    }
}

@Composable
private fun H4Bands(snapshot: RouterSnapshot, onOpen: () -> Unit, modifier: Modifier) {
    val bands = h4Bands(snapshot).take(6)
    HaiReferenceCard(modifier, radius = 20.dp) {
        Column(Modifier.fillMaxSize().padding(9.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
                H4RadioIcon(Modifier.size(15.dp), H4.Blue)
                Spacer(Modifier.width(4.dp))
                Text("الترددات النشطة", color = H4.Ink, fontSize = 10.6.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("عرض الكل ‹", color = H4.Blue, fontSize = 7.4.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(6) { index ->
                    val band = bands.getOrNull(index)
                    H4BandTile(
                        band ?: "—",
                        if (band == null) "" else if (band.startsWith("n", true)) "5G" else "LTE",
                        listOf(Color(0xFFE4F2FF), Color(0xFFE5F9F0), Color(0xFFEAF9F4), H4.SoftPurple, H4.SoftGold, Color(0xFFEAF1FF))[index],
                        band != null,
                        Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun H4BandTile(name: String, tech: String, background: Color, active: Boolean, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (active) background else H4.Soft.copy(alpha = .65f)).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(name, color = if (active) H4.Ink else H4.Muted.copy(alpha = .5f), fontSize = 9.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
        if (tech.isNotBlank()) Text(tech, color = if (tech == "5G") H4.Blue else H4.Muted, fontSize = 6.8.sp, fontWeight = FontWeight.Bold)
        if (active) Box(Modifier.padding(top = 3.dp).size(5.dp).background(H4.Green, CircleShape))
    }
}

private enum class H4Tool { OPTIMIZE, DIAGNOSE, BANDS, REFRESH }

@Composable
private fun H4Tools(
    smartBusy: Boolean,
    onOptimize: () -> Unit,
    onDiagnostics: () -> Unit,
    onBands: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier
) {
    HaiReferenceCard(modifier, radius = 20.dp) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("أدوات سريعة", color = H4.Ink, fontSize = 10.6.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                H4Grid(Modifier.size(14.dp), H4.Blue)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                H4ToolTile(H4Tool.OPTIMIZE, if (smartBusy) "جاري التحسين" else "تحسين الأداء", H4.SoftPurple, !smartBusy, Modifier.weight(1f), onOptimize)
                H4ToolTile(H4Tool.DIAGNOSE, "تشخيص الشبكة", Color(0xFFE7FAF4), true, Modifier.weight(1f), onDiagnostics)
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                H4ToolTile(H4Tool.BANDS, "قفل الترددات", Color(0xFFE8F8F0), true, Modifier.weight(1f), onBands)
                H4ToolTile(H4Tool.REFRESH, "تحديث البيانات", Color(0xFFEAF2FF), true, Modifier.weight(1f), onRefresh)
            }
        }
    }
}

@Composable
private fun H4ToolTile(tool: H4Tool, label: String, background: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(background).clickable(enabled = enabled, onClick = onClick).padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        H4ToolIcon(tool, if (enabled) H4.Blue else H4.Muted, Modifier.size(16.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (enabled) H4.Ink else H4.Muted, fontSize = 6.8.sp, lineHeight = 7.4.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun H4Bars(color: Color, modifier: Modifier, count: Int) {
    Canvas(modifier) {
        val gap = size.width * .07f
        val width = (size.width - gap * (count - 1)) / count
        repeat(count) { i ->
            val factor = .28f + i * (.72f / (count - 1).coerceAtLeast(1))
            val h = size.height * factor
            drawRoundRect(color, Offset(i * (width + gap), size.height - h), Size(width, h), CornerRadius(width / 2f))
        }
    }
}

@Composable
private fun H4RadioIcon(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val c = Offset(size.width * .50f, size.height * .60f)
        drawCircle(color, size.width * .08f, c)
        drawLine(color, c, Offset(c.x, size.height), size.width * .075f, StrokeCap.Round)
        repeat(2) { i ->
            val r = size.width * (.24f + i * .19f)
            drawArc(color, 205f, 130f, false, Offset(c.x-r, c.y-r), Size(r*2, r*2), style = Stroke(size.width*.07f, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun H4SpeedIcon(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        drawArc(color, 195f, 150f, false, Offset(size.width*.08f,size.height*.18f), Size(size.width*.84f,size.height*.84f), style=Stroke(size.width*.09f, cap=StrokeCap.Round))
        drawLine(color, Offset(size.width*.50f,size.height*.60f), Offset(size.width*.75f,size.height*.33f), size.width*.08f, StrokeCap.Round)
        drawCircle(color, size.width*.08f, Offset(size.width*.50f,size.height*.60f))
    }
}

@Composable
private fun H4Pin(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val p = Path().apply {
            moveTo(size.width*.50f,size.height*.94f)
            cubicTo(size.width*.18f,size.height*.60f,size.width*.14f,size.height*.38f,size.width*.50f,size.height*.12f)
            cubicTo(size.width*.86f,size.height*.38f,size.width*.82f,size.height*.60f,size.width*.50f,size.height*.94f)
            close()
        }
        drawPath(p,color)
        drawCircle(Color.White,size.width*.12f,Offset(size.width*.50f,size.height*.42f))
    }
}

@Composable
private fun H4Pulse(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val p = Path().apply {
            moveTo(0f,size.height*.55f)
            lineTo(size.width*.22f,size.height*.55f)
            lineTo(size.width*.34f,size.height*.22f)
            lineTo(size.width*.47f,size.height*.84f)
            lineTo(size.width*.60f,size.height*.38f)
            lineTo(size.width*.72f,size.height*.55f)
            lineTo(size.width,size.height*.55f)
        }
        drawPath(p,color,style=Stroke(size.width*.08f,cap=StrokeCap.Round))
    }
}

@Composable
private fun H4Grid(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val cell = size.width*.22f
        repeat(3) { r -> repeat(3) { c ->
            drawRoundRect(color, Offset(size.width*(.07f+c*.31f),size.height*(.07f+r*.31f)), Size(cell,cell), CornerRadius(cell*.18f))
        } }
    }
}

@Composable
private fun H4ToolIcon(tool: H4Tool, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        when (tool) {
            H4Tool.OPTIMIZE -> {
                val p=Path().apply{moveTo(s*.25f,s*.72f);quadraticBezierTo(s*.45f,s*.20f,s*.80f,s*.16f);quadraticBezierTo(s*.82f,s*.53f,s*.30f,s*.78f);close()}
                drawPath(p,color); drawCircle(Color.White,s*.08f,Offset(s*.62f,s*.37f))
            }
            H4Tool.DIAGNOSE -> {
                drawCircle(color,s*.30f,Offset(s*.43f,s*.43f),style=Stroke(s*.10f)); drawLine(color,Offset(s*.64f,s*.64f),Offset(s*.86f,s*.86f),s*.10f,StrokeCap.Round)
            }
            H4Tool.BANDS -> {
                drawRoundRect(color,Offset(s*.18f,s*.43f),Size(s*.64f,s*.48f),CornerRadius(s*.09f)); drawArc(color,180f,180f,false,Offset(s*.30f,s*.12f),Size(s*.40f,s*.52f),style=Stroke(s*.10f,cap=StrokeCap.Round))
            }
            H4Tool.REFRESH -> {
                drawArc(color,35f,285f,false,Offset(s*.16f,s*.16f),Size(s*.68f,s*.68f),style=Stroke(s*.11f,cap=StrokeCap.Round)); val p=Path().apply{moveTo(s*.77f,s*.12f);lineTo(s*.92f,s*.30f);lineTo(s*.70f,s*.31f);close()};drawPath(p,color)
            }
        }
    }
}

private fun h4Bands(snapshot: RouterSnapshot): List<String> {
    val out = LinkedHashSet<String>()
    if (snapshot.raw["_zte_nr_active_verified"].equals("true", true)) h4Band(snapshot.nrBand, CellRole.NR)?.let(out::add)
    snapshot.cells.forEach { h4Band(it.band, it.role)?.let(out::add) }
    if (snapshot.raw["_zte_lte_active_verified"].equals("true", true)) h4Band(snapshot.lteBand, CellRole.PRIMARY)?.let(out::add)
    return out.toList()
}

private fun h4Band(raw: String?, role: CellRole): String? {
    val n = Regex("\\d+").find(raw.orEmpty())?.value ?: return null
    return if (role == CellRole.NR || raw.orEmpty().startsWith("n", true)) "n$n" else "B$n"
}

private fun h4Aggregation(snapshot: RouterSnapshot, bands: List<String>): Pair<String, String?> {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val caVerified = snapshot.raw["_zte_ca_verified"].equals("true", true)
    val lteCarriers = snapshot.cells.filter { it.role != CellRole.NR }.distinctBy { Triple(it.band, it.pci, it.arfcn) }.size
    val carriers = bands.take(4).joinToString(" + ").ifBlank { "غير مؤكد" }
    val badge = when {
        snapshot.caActive && caVerified && lteCarriers >= 2 && nr -> "${lteCarriers}CA + NR"
        snapshot.caActive && caVerified && lteCarriers >= 2 -> "${lteCarriers}CA"
        nr && lteCarriers >= 1 -> "NSA"
        else -> null
    }
    return carriers to badge
}

private fun h4Mode(snapshot: RouterSnapshot, nr: Boolean, lte: Boolean): String = when {
    nr && snapshot.networkType.orEmpty().contains("SA", true) && !snapshot.networkType.orEmpty().contains("NSA", true) -> "SA"
    nr -> "NSA"
    lte -> "LTE"
    else -> "غير مؤكد"
}

private fun h4Quality(rsrp: Double?): String = when {
    rsrp == null -> "غير مؤكد"
    rsrp >= -80 -> "إشارة ممتازة"
    rsrp >= -90 -> "إشارة جيدة جدًا"
    rsrp >= -100 -> "إشارة جيدة"
    rsrp >= -110 -> "إشارة ضعيفة"
    else -> "إشارة ضعيفة جدًا"
}

private fun h4ModeCaption(current: String): String = when (current) {
    "Only_WCDMA" -> "الوضع الحالي: 3G فقط"
    "Only_LTE" -> "الوضع الحالي: 4G فقط"
    "Only_5G" -> "الوضع الحالي: 5G فقط"
    "WL_AND_5G", "LTE_AND_5G" -> "اختيار أفضل شبكة تلقائيًا"
    else -> "الوضع الحالي غير مقروء"
}

private fun h4f1(value: Double) = String.format(Locale.US, "%.1f", value)
private fun h4f0(value: Double) = String.format(Locale.US, "%.0f", value)
