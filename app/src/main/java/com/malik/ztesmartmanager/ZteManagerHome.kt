package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun ZteManagerHome(
    snapshot: RouterSnapshot,
    qualityScore: Int,
    traffic: TrafficTelemetry?,
    telemetrySamples: List<SafeTelemetrySample>,
    stability: ConnectionStabilityReport,
    lastPerformance: NetworkPerformance?,
    speedBusy: Boolean,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    deviceCount: Int,
    controlBusy: Boolean,
    operationMessage: String,
    status: String,
    onSpeedTest: () -> Unit,
    onPlacementToggle: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenMore: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 700.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ZteHero(snapshot, qualityScore, wideLayout) }

            item {
                ZteAdaptiveTwoColumn(wideLayout, 1.08f, 0.92f,
                    first = {
                        ZteSpeedCard(
                            performance = lastPerformance,
                            busy = speedBusy,
                            onRun = onSpeedTest
                        )
                    },
                    second = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ZteModeCard(snapshot, controlBusy, onSetNetworkMode)
                            ZteCarrierAggregationCard(snapshot, onOpenNetwork)
                        }
                    }
                )
            }

            item {
                ZteAdaptiveThreeColumn(
                    wide = wideLayout,
                    first = { ZteTowerMapCard(snapshot, onOpenNetwork) },
                    second = { ZteActiveBandsCard(snapshot, onOpenNetwork) },
                    third = { ZteBandLockCard(snapshot, onOpenNetwork) }
                )
            }

            item {
                ZteAdaptiveTwoColumn(
                    wide = wideLayout,
                    firstWeight = 0.90f,
                    secondWeight = 1.10f,
                    first = {
                        ZteDiagnosticsCard(
                            onOpenNetwork = onOpenNetwork,
                            onOpenTools = onOpenTools,
                            onOpenMore = onOpenMore
                        )
                    },
                    second = {
                        ZteSignalMonitorCard(snapshot, telemetrySamples, stability, qualityScore)
                    }
                )
            }

            item {
                ZtePlacementCoach(
                    active = placementMode,
                    reading = placementReading,
                    onToggle = onPlacementToggle
                )
            }

            item {
                ZteRouterInfoStrip(
                    snapshot = snapshot,
                    traffic = traffic,
                    deviceCount = deviceCount
                )
            }

            if (operationMessage.isNotBlank() || status.isNotBlank()) {
                item { ZteOperationBanner(operationMessage.ifBlank { status }) }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun ZteAdaptiveTwoColumn(
    wide: Boolean,
    firstWeight: Float = 1f,
    secondWeight: Float = 1f,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    if (wide) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.weight(firstWeight)) { first() }
            Box(Modifier.weight(secondWeight)) { second() }
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            first()
            second()
        }
    }
}

@Composable
private fun ZteAdaptiveThreeColumn(
    wide: Boolean,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit
) {
    if (wide) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.weight(1.12f)) { first() }
            Box(Modifier.weight(0.94f)) { second() }
            Box(Modifier.weight(0.94f)) { third() }
        }
    } else {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            first()
            second()
            third()
        }
    }
}

@Composable
private fun ZteHero(snapshot: RouterSnapshot, qualityScore: Int, wide: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFDDEAF9)),
        shadowElevation = 5.dp
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFF9FCFF),
                            Color(0xFFEAF6FF),
                            Color(0xFFF7FBFF)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            if (wide) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(0.95f)) {
                        ZteRouterScene(snapshot)
                    }
                    Box(Modifier.weight(1.55f)) {
                        ZteNetworkStatusCard(snapshot, qualityScore)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ZteRouterScene(snapshot)
                    ZteNetworkStatusCard(snapshot, qualityScore)
                }
            }
        }
    }
}

@Composable
private fun ZteRouterScene(snapshot: RouterSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 248.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFDFF2FF), Color(0xFFF6FBFF))
                    )
                )
                .padding(18.dp)
        ) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    color = Color(0xFF8CCBFF).copy(alpha = 0.13f),
                    radius = w * 0.46f,
                    center = Offset(w * 0.18f, h * 0.06f)
                )
                drawCircle(
                    color = Color(0xFF4B9DFF).copy(alpha = 0.08f),
                    radius = w * 0.34f,
                    center = Offset(w * 0.85f, h * 0.42f)
                )
                drawLine(
                    color = Color(0xFFB9DDFC),
                    start = Offset(0f, h * 0.70f),
                    end = Offset(w, h * 0.50f),
                    strokeWidth = 3.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFCDE7FA),
                    start = Offset(0f, h * 0.80f),
                    end = Offset(w, h * 0.62f),
                    strokeWidth = 3.dp.toPx()
                )
            }

            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                ZteGenerationBadge("5G", zteIsFiveG(snapshot))
                ZteGenerationBadge("4G", true)
                ZteGenerationBadge("LTE", true)
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ZteRouterIllustrationReference(snapshot.model ?: "ZTE")
                Spacer(Modifier.height(12.dp))
                Text(
                    snapshot.model ?: "ZTE",
                    color = ZteInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "ZTE 5G CPE",
                    color = ZteMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ZteRouterIllustrationReference(model: String) {
    Surface(
        modifier = Modifier.width(126.dp).height(176.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFDFEFF),
        border = BorderStroke(1.dp, Color(0xFFD7E4F3)),
        shadowElevation = 9.dp
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFF7FAFE), Color.White, Color(0xFFEFF5FB))
                    )
                )
                .padding(14.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .width(72.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1D2D45))
            )

            Text(
                "ZTE",
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF7587A2),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(5) { index ->
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    index < 3 -> ZteGreen
                                    index == 3 -> ZteAmber
                                    else -> ZteBlue
                                }
                            )
                    )
                }
            }

            Text(
                model.take(12),
                modifier = Modifier.align(Alignment.BottomCenter),
                color = ZteMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ZteGenerationBadge(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) ZteBlue else Color.White.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, if (active) ZteBlue else Color(0xFFBFD4EA))
    ) {
        Text(
            label,
            color = if (active) Color.White else ZteDeepBlue,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ZteNetworkStatusCard(snapshot: RouterSnapshot, qualityScore: Int) {
    val quality = zteQualityWord(qualityScore)
    val network = zteNetworkLabel(snapshot)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.White.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFFDDEAF9))
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("◉", color = ZteBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text(
                    "حالة الشبكة",
                    color = ZteInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(network, color = ZteBlue, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = ZteLine)
            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (network.contains("5G", ignoreCase = true)) "5G" else network,
                    color = ZteBlue,
                    fontSize = if (network.contains("5G", ignoreCase = true)) 68.sp else 38.sp,
                    lineHeight = 72.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                Column(Modifier.weight(1f)) {
                    Text(
                        if (qualityScore > 0) "اتصال $quality" else "جاري جمع القراءات",
                        color = if (qualityScore >= 70) ZteGreen else zteQualityColor(qualityScore),
                        fontSize = 19.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (qualityScore > 0) "الراوتر متصل ونقرأ المؤشرات الفعلية" else "لن نعرض حكمًا قبل توفر قياسات",
                        color = ZteMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Surface(shape = CircleShape, color = ZteSoftBlue) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (qualityScore >= 58) "✓" else "!",
                            color = if (qualityScore >= 58) ZteBlue else ZteAmber,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZteMetricGauge(Modifier.weight(1f), "RSRP", snapshot.nrRsrp ?: snapshot.lteRsrp, "dBm", ZteBlue)
                ZteMetricGauge(Modifier.weight(1f), "SINR", snapshot.nrSinr ?: snapshot.lteSinr, "dB", ZtePurple)
                ZteMetricGauge(Modifier.weight(1f), "RSRQ", snapshot.lteRsrq, "dB", ZteCyan)
            }
        }
    }
}

@Composable
private fun ZteMetricGauge(
    modifier: Modifier,
    label: String,
    value: Double?,
    unit: String,
    accent: Color
) {
    val normalized = when (label) {
        "RSRP" -> value?.let { ((it + 120.0) / 40.0).coerceIn(0.0, 1.0) } ?: 0.0
        "SINR" -> value?.let { ((it + 5.0) / 30.0).coerceIn(0.0, 1.0) } ?: 0.0
        "RSRQ" -> value?.let { ((it + 20.0) / 12.0).coerceIn(0.0, 1.0) } ?: 0.0
        else -> 0.0
    }.toFloat()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF9FBFE),
        border = BorderStroke(1.dp, ZteLine)
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(74.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    drawArc(ZteLine, 135f, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(accent, 135f, 270f * normalized, false, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = ZteInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        value?.let { "${it.roundToInt()}" } ?: "—",
                        color = ZteInk,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Text(unit, color = ZteInk, fontSize = 13.sp)
            Spacer(Modifier.height(3.dp))
            Text(zteMetricWord(label, value), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun ZteSpeedCard(performance: NetworkPerformance?, busy: Boolean, onRun: () -> Unit) {
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("◴", color = ZteBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("اختبار السرعة", color = ZteInk, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("SPEEDTEST", color = ZteMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))
        ZteSpeedGaugeReference(performance?.downloadMbps)

        Text(
            performance?.downloadMbps?.let { "${String.format("%.1f", it)} Mb/s" } ?: if (busy) "جاري القياس…" else "— Mb/s",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = ZteDeepBlue,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Black
        )

        if (performance != null) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ZteSmallMetricTile(Modifier.weight(1f), "Ping", performance.latencyMs?.let { "${it.roundToInt()} ms" } ?: "—")
                ZteSmallMetricTile(Modifier.weight(1f), "Jitter", performance.jitterMs?.let { "${it.roundToInt()} ms" } ?: "—")
                ZteSmallMetricTile(Modifier.weight(1f), "Loss", performance.packetLossPercent?.let { "${it.roundToInt()}%" } ?: "—")
            }
        }

        Spacer(Modifier.height(14.dp))
        ZtePrimaryButton(if (busy) "جاري الاختبار الحقيقي…" else "ابدأ الاختبار", !busy, onRun)
    }
}

@Composable
private fun ZteSpeedGaugeReference(speed: Double?) {
    val fraction = ((speed ?: 0.0) / 1000.0).coerceIn(0.0, 1.0).toFloat()
    Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(280.dp).height(155.dp)) {
            val stroke = 14.dp.toPx()
            drawArc(Color(0xFFE5EDF7), 180f, 180f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(ZteBlue, 180f, 180f * fraction, false, style = Stroke(stroke, cap = StrokeCap.Round))
            val center = Offset(size.width / 2f, size.height * 0.88f)
            val angle = Math.toRadians(180.0 + 180.0 * fraction)
            val radius = size.width * 0.34f
            val end = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
            drawLine(ZteBlue, center, end, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(ZteBlue, radius = 7.dp.toPx(), center = center)
        }
    }
}

@Composable
private fun ZteSmallMetricTile(modifier: Modifier, label: String, value: String) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = ZteMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(value, color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ZteModeCard(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    val current = zteNetworkLabel(snapshot)
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("◉", color = ZteBlue, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("وضع الشبكة", color = ZteInk, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ZteModeOptionReference("5G + 4G", "الوضع المفضل للجيل الخامس مع الرجوع للرابع", current.contains("5G", true), !busy) { onMode("LTE_AND_5G") }
        Spacer(Modifier.height(9.dp))
        ZteModeOptionReference("4G فقط", "تثبيت الراوتر على الجيل الرابع", current.contains("4G", true) && !current.contains("5G", true), !busy) { onMode("Only_LTE") }
        Spacer(Modifier.height(9.dp))
        ZteModeOptionReference("3G فقط", "استخدمه عند الحاجة فقط", snapshot.networkType.orEmpty().contains("WCDMA", true), !busy) { onMode("Only_WCDMA") }
        Spacer(Modifier.height(9.dp))
        ZteModeOptionReference("تلقائي", "الراوتر يختار أفضل وضع متاح", false, !busy) { onMode("WL_AND_5G") }
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = ZteSoftBlue) {
            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⚙", color = ZteBlue, fontSize = 21.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("الوضع الحالي", color = ZteMuted, fontSize = 13.sp)
                    Text(current, color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Text("›", color = ZteBlue, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ZteModeOptionReference(title: String, subtitle: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) ZteBlue else Color(0xFFF7FAFE),
        border = BorderStroke(1.dp, if (selected) ZteBlue else ZteLine)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(21.dp).clip(CircleShape).background(if (selected) Color.White else ZteLine), contentAlignment = Alignment.Center) {
                if (selected) Box(Modifier.size(9.dp).clip(CircleShape).background(ZteBlue))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (selected) Color.White else ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = if (selected) Color.White.copy(alpha = 0.88f) else ZteMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun ZteCarrierAggregationCard(snapshot: RouterSnapshot, onOpenNetwork: () -> Unit) {
    val bands = zteActiveBands(snapshot)
    val count = maxOf(bands.size, snapshot.cells.size)
    val verified = snapshot.raw["_zte_ca_verified"].equals("true", true)
    val active = snapshot.caActive && verified
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("▱", color = ZteBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("الدمج النشط للترددات", color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(14.dp), color = if (active) ZteBlue else ZteSurfaceAlt, border = BorderStroke(1.dp, if (active) ZteBlue else ZteLine), modifier = Modifier.clickable(onClick = onOpenNetwork)) {
                Text(if (active) "${count.coerceAtLeast(2)}CA" else "تحقق", color = if (active) Color.White else ZteBlue, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF6F9FD)) {
            Text(if (bands.isNotEmpty()) bands.joinToString(" + ") else "لا توجد ترددات موثقة للعرض", color = ZteDeepBlue, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth().padding(13.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ZteTowerMapCard(snapshot: RouterSnapshot, onOpenNetwork: () -> Unit) {
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("●", color = ZteBlue, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text("أقرب برج شبكة", color = ZteInk, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ZteManagerMap(snapshot, Modifier.fillMaxWidth().height(320.dp))
        Spacer(Modifier.height(12.dp))
        ZteSecondaryButton("عرض تفاصيل الأبراج", onClick = onOpenNetwork)
    }
}

@Composable
private fun ZteActiveBandsCard(snapshot: RouterSnapshot, onOpenNetwork: () -> Unit) {
    val cells = remember(snapshot.cells) { snapshot.cells.ifEmpty { fallbackCells(snapshot) } }
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("▥", color = ZteBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("الترددات النشطة", color = ZteInk, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        if (cells.isEmpty()) {
            Text("الراوتر لم يرسل ترددات نشطة موثقة.", color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp)
        } else {
            cells.take(6).forEachIndexed { index, cell ->
                ZteBandRow(cell)
                if (index != cells.take(6).lastIndex) HorizontalDivider(color = ZteLine)
            }
        }
        Spacer(Modifier.height(12.dp))
        ZteSecondaryButton("عرض جميع الترددات", onClick = onOpenNetwork)
    }
}

private fun fallbackCells(snapshot: RouterSnapshot): List<CarrierCell> = buildList {
    snapshot.nrBand?.takeIf { it.isNotBlank() }?.let { add(CarrierCell(CellRole.NR, it, null, null, null)) }
    snapshot.lteBand?.takeIf { it.isNotBlank() }?.let { add(CarrierCell(CellRole.PRIMARY, it, snapshot.pci, snapshot.earfcn, null)) }
}

@Composable
private fun ZteBandRow(cell: CarrierCell) {
    val technology = if (cell.role == CellRole.NR) "5G" else "LTE"
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).clip(CircleShape).background(if (cell.role == CellRole.NR) ZteGreen else ZteBlue))
        Spacer(Modifier.width(9.dp))
        Text(cell.band ?: "—", color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Text(cell.bandwidthMhz?.let { "${it.roundToInt()} MHz" } ?: "—", color = ZteMuted, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(10.dp), color = ZteSoftBlue) {
            Text(technology, color = ZteBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun ZteBandLockCard(snapshot: RouterSnapshot, onOpenNetwork: () -> Unit) {
    val bands = zteActiveBands(snapshot)
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("▣", color = ZteBlue, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("قفل الترددات", color = ZteInk, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ZteLockSummaryRow("القفل التلقائي", "حسب إعداد الراوتر")
        ZteLockSummaryRow("5G فقط", if (zteIsFiveG(snapshot)) "نشط الآن" else "غير نشط")
        ZteLockSummaryRow("الترددات الحالية", if (bands.isNotEmpty()) bands.joinToString(" + ") else "غير متاحة")
        ZteLockSummaryRow("4G فقط", if (zteNetworkLabel(snapshot).contains("4G")) "نشط الآن" else "غير نشط")
        Spacer(Modifier.height(12.dp))
        ZtePrimaryButton("فتح إعدادات القفل", onClick = onOpenNetwork)
    }
}

@Composable
private fun ZteLockSummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ZteInk, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = ZteBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = ZteLine)
}

@Composable
private fun ZteDiagnosticsCard(onOpenNetwork: () -> Unit, onOpenTools: () -> Unit, onOpenMore: () -> Unit) {
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("✦", color = ZteBlue, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("أدوات التشخيص", color = ZteInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(13.dp))
        ZteDiagnosticAction("فحص الشبكة", "قراءات الخلية والترددات", onOpenNetwork)
        Spacer(Modifier.height(9.dp))
        ZteDiagnosticAction("تحسين الأداء", "مساعد التحسين الذكي", onOpenTools)
        Spacer(Modifier.height(9.dp))
        ZteDiagnosticAction("سجل الاتصال", "معلومات الجهاز والجلسة", onOpenMore)
    }
}

@Composable
private fun ZteDiagnosticAction(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = ZteSoftBlue) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Text("›", color = ZteBlue, fontSize = 28.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ZteInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = ZteMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun ZteSignalMonitorCard(snapshot: RouterSnapshot, telemetrySamples: List<SafeTelemetrySample>, stability: ConnectionStabilityReport, qualityScore: Int) {
    val history = telemetrySamples.takeLast(18)
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("▥", color = ZteBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Text("مراقبة الإشارة المباشرة", color = ZteInk, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = ZteSoftGreen) {
                Text(if (qualityScore > 0) "مباشر" else "انتظار", color = ZteGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        ZteSignalHistoryChart(snapshot, history)
        Spacer(Modifier.height(10.dp))
        Text(stability.summary, color = ZteMuted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun ZteSignalHistoryChart(snapshot: RouterSnapshot, samples: List<SafeTelemetrySample>) {
    val rsrp = samples.mapNotNull { it.nrRsrp ?: it.lteRsrp }.ifEmpty { listOfNotNull(snapshot.nrRsrp ?: snapshot.lteRsrp) }
    val sinr = samples.mapNotNull { it.nrSinr ?: it.lteSinr }.ifEmpty { listOfNotNull(snapshot.nrSinr ?: snapshot.lteSinr) }
    val rsrq = samples.mapNotNull { it.lteRsrq }.ifEmpty { listOfNotNull(snapshot.lteRsrq) }

    Surface(modifier = Modifier.fillMaxWidth().height(190.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFFFAFCFF), border = BorderStroke(1.dp, ZteLine)) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val left = 8.dp.toPx(); val top = 8.dp.toPx(); val right = size.width - 8.dp.toPx(); val bottom = size.height - 8.dp.toPx()
            repeat(4) { i ->
                val y = top + (bottom - top) * (i / 3f)
                drawLine(ZteLine, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            }
            fun drawSeries(values: List<Double>, color: Color, min: Double, max: Double) {
                if (values.size < 2) return
                val dx = (right - left) / (values.size - 1).coerceAtLeast(1)
                values.zipWithNext().forEachIndexed { index, pair ->
                    val y1 = bottom - (((pair.first - min) / (max - min)).coerceIn(0.0, 1.0).toFloat() * (bottom - top))
                    val y2 = bottom - (((pair.second - min) / (max - min)).coerceIn(0.0, 1.0).toFloat() * (bottom - top))
                    drawLine(color, Offset(left + dx * index, y1), Offset(left + dx * (index + 1), y2), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            drawSeries(rsrp, ZteBlue, -120.0, -70.0)
            drawSeries(sinr, ZtePurple, -10.0, 30.0)
            drawSeries(rsrq, ZteCyan, -22.0, -6.0)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ZteLegendDot("RSRP", ZteBlue); ZteLegendDot("SINR", ZtePurple); ZteLegendDot("RSRQ", ZteCyan)
    }
}

@Composable
private fun ZteLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(5.dp)); Text(label, color = ZteMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ZtePlacementCoach(active: Boolean, reading: PlacementReading?, onToggle: () -> Unit) {
    val (title, body, color) = ztePlacementWords(reading)
    val score = reading?.score?.total ?: 0
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("◎", color = ZteBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("أفضل مكان للراوتر", color = ZteInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("حرّك الراوتر واتبع النتيجة الحية", color = ZteMuted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = color.copy(alpha = 0.09f)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text(title, color = color, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(body, color = ZteInk, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(11.dp))
                LinearProgressIndicator(progress = { score.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape), color = color, trackColor = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        ZtePrimaryButton(if (active) "إيقاف البحث عن أفضل مكان" else "ابدأ البحث عن أفضل مكان", onClick = onToggle)
    }
}

@Composable
private fun ZteRouterInfoStrip(snapshot: RouterSnapshot, traffic: TrafficTelemetry?, deviceCount: Int) {
    val uptime = snapshot.raw["realtime_time"]?.toLongOrNull() ?: snapshot.raw["uptime"]?.toLongOrNull()
    ZteCard {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            ZteInfoStripRow("رقم الجهاز", snapshot.raw["device_id"] ?: snapshot.raw["imei"] ?: "—")
            HorizontalDivider(color = ZteLine)
            ZteInfoStripRow("اسم الشبكة", zteOperator(snapshot))
            HorizontalDivider(color = ZteLine)
            ZteInfoStripRow("نوع الاتصال", zteNetworkLabel(snapshot))
            HorizontalDivider(color = ZteLine)
            ZteInfoStripRow("مدة التشغيل", zteDuration(uptime))
            HorizontalDivider(color = ZteLine)
            ZteInfoStripRow("الأجهزة المتصلة", deviceCount.toString())
            traffic?.let { HorizontalDivider(color = ZteLine); ZteInfoStripRow("استهلاك الجلسة", "بيانات مباشرة من الراوتر") }
        }
    }
}

@Composable
private fun ZteInfoStripRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ZteMuted, fontSize = 13.sp, modifier = Modifier.weight(0.45f))
        Text(value, color = ZteInk, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.55f), textAlign = TextAlign.End)
    }
}

@Composable
private fun ZteOperationBanner(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = ZteSoftBlue, border = BorderStroke(1.dp, Color(0xFFCCE0FA))) {
        Text(message, color = ZteDeepBlue, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(15.dp))
    }
}
