package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val V6Bg = Color(0xFFF3F6FA)
private val V6Card = Color.White
private val V6Ink = Color(0xFF10275C)
private val V6Muted = Color(0xFF6E7A8F)
private val V6Blue = Color(0xFF1268F3)
private val V6BlueSoft = Color(0xFFEAF2FF)
private val V6Green = Color(0xFF16A86B)
private val V6GreenSoft = Color(0xFFE8F8F1)
private val V6Border = Color(0xFFE2E8F0)
private val V6Panel = Color(0xFFF8FAFD)
private val V6Amber = Color(0xFFE59A22)
private val V6Red = Color(0xFFC44747)

private data class V6Radio(
    val verified: Boolean,
    val generation: String,
    val mode: String,
    val rsrp: Double?,
    val sinr: Double?,
    val rsrq: Double?,
    val bands: List<String>
)

@Composable
fun HaiHomeDashboardV5(
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
    val radio = v6Radio(snapshot)

    Column(Modifier.fillMaxSize().background(V6Bg)) {
        HaiSharedHeader(
            connected = radio.verified || status.contains("متصل"),
            onDisconnect = onDisconnect,
            onMenu = onNavigateMore,
            onSettings = onNavigateTools,
            onSearch = onNavigateTowers
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = ui.pagePadding,
                end = ui.pagePadding,
                top = 6.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ui.sectionGap)
        ) {
            item { V6Hero(snapshot, radio) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    V6Speed(
                        performance = lastPerformance,
                        busy = speedBusy,
                        modifier = Modifier.weight(1f),
                        onClick = onSpeedTest
                    )
                    V6Map(
                        snapshot = snapshot,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateTowers
                    )
                }
            }
            item { V6Metrics(snapshot, radio) }
            item { V6Bands(snapshot, radio, onNavigateBands) }
            item { V6Modes(snapshot, controlBusy, onSetNetworkMode, onNavigateNetwork) }
            item {
                V6Actions(
                    smartBusy = smartBusy,
                    controlBusy = controlBusy,
                    onOptimize = onOptimizeNow,
                    onBands = onNavigateBands,
                    onRefresh = onRefreshNow,
                    onTools = onNavigateTools,
                    onLogs = onNavigateLogs
                )
            }
            if (telemetrySamples.isNotEmpty()) item { V6Live(telemetrySamples.last(), onNavigateLogs) }
            if (operationMessage.isNotBlank()) item { V6Message(operationMessage) }
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

@Composable
private fun V6Hero(snapshot: RouterSnapshot, radio: V6Radio) {
    V6CardBox {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(78.dp).clip(RoundedCornerShape(24.dp))
                        .background(if (radio.verified) V6Blue else Color(0xFFE8ECF2)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            radio.generation,
                            color = if (radio.verified) Color.White else V6Muted,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            radio.mode,
                            color = if (radio.verified) Color.White.copy(alpha = .8f) else V6Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("حالة الشبكة", color = V6Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(
                        snapshot.model ?: "ZTE",
                        color = V6Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    snapshot.operatorCode?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = V6Muted, fontSize = 10.sp, maxLines = 1)
                    }
                }
                Box(
                    Modifier.size(34.dp).clip(CircleShape)
                        .background(if (radio.verified) V6GreenSoft else Color(0xFFF0F2F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (radio.verified) "✓" else "!",
                        color = if (radio.verified) V6Green else V6Muted,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(V6Panel).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                V6Bars(radio.rsrp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(v6Quality(radio.rsrp), color = v6QualityColor(radio.rsrp), fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(
                        radio.rsrp?.let { "${v6Fmt(it)} dBm" } ?: "لا توجد قراءة RSRP",
                        color = V6Muted,
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (radio.verified) "موثّق" else "غير مؤكد", color = if (radio.verified) V6Green else V6Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (radio.verified) V6Green else V6Muted))
                }
            }
        }
    }
}

@Composable
private fun V6Bars(rsrp: Double?) {
    val level = when {
        rsrp == null -> 0
        rsrp >= -85 -> 4
        rsrp >= -95 -> 3
        rsrp >= -105 -> 2
        else -> 1
    }
    val color = v6QualityColor(rsrp)
    Row(Modifier.height(42.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(12.dp, 20.dp, 29.dp, 38.dp).forEachIndexed { index, h ->
            Box(
                Modifier.width(7.dp).height(h).clip(RoundedCornerShape(4.dp))
                    .background(if (index < level) color else Color(0xFFDCE2EA))
            )
        }
    }
}

@Composable
private fun V6Speed(performance: NetworkPerformance?, busy: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val download = performance?.downloadMbps
    val normalized = ((download ?: 0.0) / 1000.0).coerceIn(0.0, 1.0).toFloat()

    V6CardBox(modifier) {
        Column(
            Modifier.clickable(enabled = !busy, onClick = onClick).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("السرعة", color = V6Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("◉", color = V6Blue, fontSize = 14.sp)
            }
            Box(Modifier.fillMaxWidth().height(105.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val diameter = minOf(size.width - 22f, size.height * 1.58f)
                    val topLeft = Offset((size.width - diameter) / 2f, 8f)
                    val arc = Size(diameter, diameter)
                    drawArc(Color(0xFFE4EAF2), 150f, 240f, false, topLeft, arc, style = Stroke(11f, cap = StrokeCap.Round))
                    drawArc(V6Blue, 150f, 240f * normalized, false, topLeft, arc, style = Stroke(11f, cap = StrokeCap.Round))
                    val angle = Math.toRadians(150.0 + 240.0 * normalized)
                    val center = Offset(size.width / 2f, topLeft.y + diameter / 2f)
                    val length = diameter * .29f
                    val end = Offset(center.x + cos(angle).toFloat() * length, center.y + sin(angle).toFloat() * length)
                    drawLine(V6Ink, center, end, 4f, StrokeCap.Round)
                    drawCircle(V6Ink, 7f, center)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 28.dp)) {
                    Text(download?.let(::v6Fmt) ?: "—", color = V6Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Mb/s", color = V6Muted, fontSize = 10.sp)
                }
            }
            Text(if (busy) "جاري القياس…" else "اضغط للقياس", color = if (busy) V6Muted else V6Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun V6Map(snapshot: RouterSnapshot, modifier: Modifier, onClick: () -> Unit) {
    V6CardBox(modifier) {
        Column(Modifier.clickable(onClick = onClick).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الخريطة", color = V6Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("⌖", color = V6Blue, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(108.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFEEF4EC))) {
                Canvas(Modifier.fillMaxSize()) {
                    val grid = Color(0xFFD8E5D7)
                    for (i in 1..4) {
                        val y = size.height * i / 5f
                        drawLine(grid, Offset(0f, y), Offset(size.width, y + 4f), 2f)
                    }
                    for (i in 1..3) {
                        val x = size.width * i / 4f
                        drawLine(grid, Offset(x, 0f), Offset(x - 7f, size.height), 2f)
                    }
                    val road = Path().apply {
                        moveTo(-10f, size.height * .72f)
                        cubicTo(size.width * .2f, size.height * .45f, size.width * .52f, size.height * .9f, size.width + 10f, size.height * .3f)
                    }
                    drawPath(road, Color.White, style = Stroke(13f, cap = StrokeCap.Round))
                    val c = Offset(size.width * .57f, size.height * .46f)
                    drawCircle(V6Blue.copy(alpha = .12f), 35f, c)
                    drawCircle(V6Blue.copy(alpha = .18f), 24f, c)
                    drawCircle(V6Blue, 9f, c)
                    drawLine(V6Blue, Offset(c.x, c.y + 7f), Offset(c.x, c.y + 28f), 5f, StrokeCap.Round)
                }
                Box(
                    Modifier.align(Alignment.BottomStart).padding(7.dp).clip(RoundedCornerShape(9.dp))
                        .background(Color.White.copy(alpha = .92f)).padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text("PCI ${snapshot.pci ?: "—"}", color = V6Ink, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("الموقع غير مؤكد", color = V6Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun V6Metrics(snapshot: RouterSnapshot, radio: V6Radio) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        V6Metric("RSRP", radio.rsrp?.let(::v6Fmt) ?: "—", "dBm", Modifier.weight(1f))
        V6Metric("SINR", radio.sinr?.let(::v6Fmt) ?: "—", "dB", Modifier.weight(1f))
        V6Metric("RSRQ", radio.rsrq?.let(::v6Fmt) ?: "—", "dB", Modifier.weight(1f))
        V6Metric("خلية", snapshot.cellId?.toString() ?: "—", "رقم", Modifier.weight(1f))
    }
}

@Composable
private fun V6Metric(label: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(V6Card).border(1.dp, V6Border, RoundedCornerShape(15.dp)).padding(vertical = 9.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(V6BlueSoft), contentAlignment = Alignment.Center) {
            Text("•", color = V6Blue, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        Text(label, color = V6Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, color = V6Ink, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(unit, color = V6Muted, fontSize = 10.sp)
    }
}

@Composable
private fun V6Bands(snapshot: RouterSnapshot, radio: V6Radio, onClick: () -> Unit) {
    V6CardBox {
        Column(Modifier.clickable(onClick = onClick).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(V6BlueSoft), contentAlignment = Alignment.Center) {
                    Text("⌁", color = V6Blue, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("الترددات", color = V6Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(if (snapshot.caActive) "CA نشط وموثّق" else "CA غير مثبت", color = if (snapshot.caActive) V6Green else V6Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text("‹", color = V6Blue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (radio.bands.isEmpty()) V6BandChip("غير مؤكد", false)
                else radio.bands.forEach { V6BandChip(it, true) }
            }
        }
    }
}

@Composable
private fun V6BandChip(text: String, active: Boolean) {
    Column(
        Modifier.width(72.dp).clip(RoundedCornerShape(16.dp))
            .background(if (active) V6BlueSoft else V6Panel)
            .border(1.dp, if (active) V6Blue.copy(alpha = .3f) else V6Border, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (active) V6Green else V6Muted))
        Spacer(Modifier.height(6.dp))
        Text(text, color = if (active) V6Blue else V6Muted, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun V6Modes(snapshot: RouterSnapshot, busy: Boolean, onSet: (String) -> Unit, onDetails: () -> Unit) {
    val current = snapshot.raw["BearerPreference"].orEmpty()
    V6CardBox {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("وضع الشبكة", color = V6Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("الإعدادات", color = V6Blue, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.clickable(onClick = onDetails).padding(6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                V6Mode("تلقائي", current == "WL_AND_5G" || current == "LTE_AND_5G", !busy, Modifier.weight(1f)) { onSet("WL_AND_5G") }
                V6Mode("5G فقط", current == "Only_5G", !busy, Modifier.weight(1f)) { onSet("Only_5G") }
                V6Mode("4G فقط", current == "Only_LTE", !busy, Modifier.weight(1f)) { onSet("Only_LTE") }
                V6Mode("3G فقط", current == "Only_WCDMA", !busy, Modifier.weight(1f)) { onSet("Only_WCDMA") }
            }
        }
    }
}

@Composable
private fun V6Mode(text: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(62.dp).clip(RoundedCornerShape(15.dp))
            .background(if (selected) V6Blue else if (enabled) V6Panel else Color(0xFFEDEFF3))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (selected) Color.White else V6Muted))
        Spacer(Modifier.height(6.dp))
        Text(text, color = if (selected) Color.White else V6Ink, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun V6Actions(
    smartBusy: Boolean,
    controlBusy: Boolean,
    onOptimize: () -> Unit,
    onBands: () -> Unit,
    onRefresh: () -> Unit,
    onTools: () -> Unit,
    onLogs: () -> Unit
) {
    V6CardBox {
        Column(Modifier.padding(14.dp)) {
            Text("أدوات سريعة", color = V6Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V6Action("⚡", "تحسين", !smartBusy && !controlBusy, Modifier.weight(1f), onOptimize)
                V6Action("⌁", "ترددات", !controlBusy, Modifier.weight(1f), onBands)
                V6Action("↻", "تحديث", !controlBusy, Modifier.weight(1f), onRefresh)
                V6Action("⚙", "أدوات", true, Modifier.weight(1f), onTools)
                V6Action("≡", "سجل", true, Modifier.weight(1f), onLogs)
            }
        }
    }
}

@Composable
private fun V6Action(icon: String, title: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(if (enabled) V6Panel else Color(0xFFEFF1F4))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 9.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(if (enabled) V6BlueSoft else Color(0xFFE2E5EA)), contentAlignment = Alignment.Center) {
            Text(icon, color = if (enabled) V6Blue else V6Muted, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(5.dp))
        Text(title, color = if (enabled) V6Ink else V6Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun V6Live(sample: SafeTelemetrySample, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(V6Card)
            .border(1.dp, V6Border, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(V6GreenSoft), contentAlignment = Alignment.Center) {
            Text("●", color = V6Green, fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("قراءة حية", color = V6Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(
                "${sample.networkType ?: "غير مؤكد"} • ${sample.nrBand ?: sample.lteBand ?: "بدون تردد"}",
                color = V6Muted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("‹", color = V6Blue, fontSize = 25.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V6Message(message: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(V6BlueSoft).padding(12.dp)) {
        Text(message, color = V6Ink, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V6CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalHaiUiMetrics.current.cardRadius),
        colors = CardDefaults.cardColors(containerColor = V6Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, V6Border)
    ) { content() }
}

private fun v6Radio(snapshot: RouterSnapshot): V6Radio {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", ignoreCase = true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", ignoreCase = true)
    val type = snapshot.networkType.orEmpty()
    val generation = when {
        nr -> "5G"
        lte -> "4G"
        else -> "—"
    }
    val mode = when {
        nr && type.contains("SA", true) && !type.contains("NSA", true) -> "SA"
        nr -> "NSA"
        lte -> "LTE"
        else -> "غير مؤكد"
    }
    val bands = buildList {
        snapshot.cells.mapNotNullTo(this) { it.band?.trim()?.takeIf(String::isNotBlank) }
        if (isEmpty() && lte) snapshot.lteBand?.trim()?.takeIf(String::isNotBlank)?.let(::add)
        if (nr) snapshot.nrBand?.trim()?.takeIf(String::isNotBlank)?.let { if (it !in this) add(it) }
    }.distinct()
    return V6Radio(
        verified = nr || lte,
        generation = generation,
        mode = mode,
        rsrp = if (nr) snapshot.nrRsrp else if (lte) snapshot.lteRsrp else null,
        sinr = if (nr) snapshot.nrSinr else if (lte) snapshot.lteSinr else null,
        rsrq = if (nr || lte) snapshot.lteRsrq else null,
        bands = bands
    )
}

private fun v6Quality(rsrp: Double?): String = when {
    rsrp == null -> "غير معروف"
    rsrp >= -85 -> "ممتاز"
    rsrp >= -95 -> "جيد"
    rsrp >= -105 -> "متوسط"
    else -> "ضعيف"
}

private fun v6QualityColor(rsrp: Double?): Color = when {
    rsrp == null -> V6Muted
    rsrp >= -95 -> V6Green
    rsrp >= -105 -> V6Amber
    else -> V6Red
}

private fun v6Fmt(value: Double): String = String.format(Locale.US, "%.1f", value)
