package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import kotlin.math.roundToInt

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
        val wide = maxWidth >= 700.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ZteHero(snapshot, qualityScore) }
            if (wide) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ZteSpeedCard(lastPerformance, speedBusy, onSpeedTest)
                            ZteTowerMapCard(snapshot, onOpenNetwork)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            ZteModeCard(snapshot, controlBusy, onSetNetworkMode)
                            ZteCarrierAggregationCard(snapshot, onOpenNetwork)
                            ZteActiveBandsCard(snapshot, onOpenNetwork)
                            ZteSignalMonitorCard(telemetrySamples, stability)
                        }
                    }
                }
            } else {
                item { ZteSpeedCard(lastPerformance, speedBusy, onSpeedTest) }
                item { ZteModeCard(snapshot, controlBusy, onSetNetworkMode) }
                item { ZteCarrierAggregationCard(snapshot, onOpenNetwork) }
                item { ZtePlacementCard(placementMode, placementReading, onPlacementToggle) }
                item { ZteTowerMapCard(snapshot, onOpenNetwork) }
                item { ZteActiveBandsCard(snapshot, onOpenNetwork) }
                item { ZteSignalMonitorCard(telemetrySamples, stability) }
            }
            if (wide) item { ZtePlacementCard(placementMode, placementReading, onPlacementToggle) }
            item { ZteQuickToolsCard(onOpenTools) }
            item { ZteRouterInfoStrip(snapshot, traffic, deviceCount, onOpenMore) }
            if (operationMessage.isNotBlank() || status.isNotBlank()) {
                item { ZteOperationBanner(operationMessage.ifBlank { status }) }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun ZteHero(snapshot: RouterSnapshot, qualityScore: Int) {
    ZteCard(contentPadding = PaddingValues(0.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ZteHeroBrush)
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("حالة الشبكة", color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(10.dp))
                        ZteStatusPill(zteQualityWord(qualityScore), qualityScore >= 70)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(zteNetworkLabel(snapshot), color = ZteBlue, fontSize = 46.sp, fontWeight = FontWeight.Black)
                    Text(zteOperator(snapshot), color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (qualityScore > 0) "اتصال ${zteQualityWord(qualityScore)} حسب القياسات الحالية" else "بانتظار قياسات كافية للحكم",
                        color = ZteMuted,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                ZteRouterIllustration(snapshot.model ?: "ZTE CPE")
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZteMetricDial("RSRP", snapshot.nrRsrp ?: snapshot.lteRsrp, "dBm", ZteBlue, Modifier.weight(1f))
                ZteMetricDial("SINR", snapshot.nrSinr ?: snapshot.lteSinr, "dB", ZtePurple, Modifier.weight(1f))
                ZteMetricDial("RSRQ", snapshot.lteRsrq, "dB", ZteCyan, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ZteRouterIllustration(model: String) {
    Surface(
        modifier = Modifier.width(118.dp).height(166.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.88f),
        shadowElevation = 6.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.width(70.dp).height(124.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF9FBFE),
                shadowElevation = 8.dp
            ) {
                Column(
                    Modifier.fillMaxSize().padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(Modifier.width(42.dp).height(4.dp).clip(CircleShape).background(Color(0xFF27364E)))
                    Text("ZTE", color = ZteMuted, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(4) { index ->
                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (index < 3) ZteGreen else ZteBlue))
                        }
                    }
                }
            }
            Text(model, color = ZteInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp))
        }
    }
}

@Composable
private fun ZteMetricDial(label: String, value: Double?, unit: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(19.dp), color = Color.White.copy(alpha = 0.93f)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    drawArc(Color(0xFFE3EAF4), 135f, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    val fraction = when (label) {
                        "RSRP" -> value?.let { ((it + 125.0) / 55.0).coerceIn(0.0, 1.0) } ?: 0.0
                        "SINR" -> value?.let { ((it + 5.0) / 35.0).coerceIn(0.0, 1.0) } ?: 0.0
                        else -> value?.let { ((it + 20.0) / 14.0).coerceIn(0.0, 1.0) } ?: 0.0
                    }.toFloat()
                    drawArc(color, 135f, 270f * fraction, false, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text(value?.roundToInt()?.toString() ?: "—", color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Text(label, color = ZteMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(unit, color = ZteMuted, fontSize = 12.sp)
            Text(zteMetricWord(label, value), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ZteSpeedCard(performance: NetworkPerformance?, busy: Boolean, onRun: () -> Unit) {
    ZteCard {
        ZteSectionHeader(
            title = "اختبار السرعة",
            subtitle = "قياس فعلي للتحميل والاستجابة من الإنترنت"
        )
        Spacer(Modifier.height(10.dp))
        ZteSpeedGauge(performance?.downloadMbps)
        Text(
            performance?.downloadMbps?.let { "${String.format("%.1f", it)} Mb/s" } ?: if (busy) "جاري القياس…" else "لم يتم القياس بعد",
            color = ZteInk,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        if (performance != null) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZteInfoTile("Ping", performance.latencyMs?.let { "${it.roundToInt()} ms" } ?: "—", Modifier.weight(1f))
                ZteInfoTile("Jitter", performance.jitterMs?.let { "${it.roundToInt()} ms" } ?: "—", Modifier.weight(1f))
                ZteInfoTile("Loss", performance.packetLossPercent?.let { "${it.roundToInt()}%" } ?: "—", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        ZtePrimaryButton(if (busy) "جاري الاختبار…" else "ابدأ الاختبار الحقيقي", !busy, onRun)
    }
}

@Composable
private fun ZteSpeedGauge(speed: Double?) {
    val fraction = ((speed ?: 0.0) / 1000.0).coerceIn(0.0, 1.0).toFloat()
    Box(Modifier.fillMaxWidth().height(142.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(250.dp).height(132.dp)) {
            val stroke = 12.dp.toPx()
            drawArc(Color(0xFFE3EAF4), 180f, 180f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(ZteBlue, 180f, 180f * fraction, false, style = Stroke(stroke, cap = StrokeCap.Round))
            val center = Offset(size.width / 2f, size.height * 0.86f)
            val angle = Math.toRadians((180 + 180 * fraction).toDouble())
            val radius = size.width * 0.38f
            val end = Offset(center.x + kotlin.math.cos(angle).toFloat() * radius, center.y + kotlin.math.sin(angle).toFloat() * radius)
            drawLine(ZteDeepBlue, center, end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun ZteModeCard(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    ZteCard {
        ZteSectionHeader("وضع الشبكة", "الوضع الحالي: ${zteNetworkLabel(snapshot)}")
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZteChoiceChip("5G NSA", zteNetworkLabel(snapshot).contains("5G"), !busy, Modifier.weight(1f)) { onMode("LTE_AND_5G") }
                ZteChoiceChip("4G LTE", zteNetworkLabel(snapshot).contains("4G"), !busy, Modifier.weight(1f)) { onMode("Only_LTE") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZteChoiceChip("3G", snapshot.networkType.orEmpty().contains("WCDMA", true), !busy, Modifier.weight(1f)) { onMode("Only_WCDMA") }
                ZteChoiceChip("تلقائي", false, !busy, Modifier.weight(1f)) { onMode("WL_AND_5G") }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("لن نعلن نجاح التغيير قبل أن يقرأ الراوتر BearerPreference مرة أخرى.", color = ZteMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ZteCarrierAggregationCard(snapshot: RouterSnapshot, onOpen: () -> Unit) {
    val bands = zteActiveBands(snapshot)
    ZteCard {
        ZteSectionHeader("الدمج النشط للترددات", action = "إدارة", onAction = onOpen)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = if (snapshot.caActive) ZteSoftBlue else Color(0xFFF5F7FB)) {
                Text(
                    if (snapshot.caActive) "${snapshot.cells.size.coerceAtLeast(2)}CA" else "غير مفعّل",
                    color = if (snapshot.caActive) ZteBlue else ZteMuted,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (snapshot.caActive) "دمج فعلي مؤكد الآن" else "لا يوجد دمج مؤكد الآن",
                    color = ZteInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(if (bands.isEmpty()) "لا توجد ترددات نشطة مؤكدة" else bands.joinToString(" + "), color = ZteMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ZtePlacementCard(active: Boolean, reading: PlacementReading?, onToggle: () -> Unit) {
    val (title, body, color) = ztePlacementWords(reading)
    ZteCard {
        ZteSectionHeader("أفضل مكان للراوتر", "ميزة حيّة تساعدك أثناء تحريك الراوتر")
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(19.dp), color = color.copy(alpha = 0.10f)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(body, color = ZteMuted, fontSize = 14.sp, lineHeight = 19.sp)
                }
                if (reading != null) {
                    Text("${reading.score.total}%", color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ZtePrimaryButton(if (active) "إيقاف جولة القياس" else "ابدأ البحث عن أفضل مكان", true, onToggle)
    }
}

@Composable
private fun ZteTowerMapCard(snapshot: RouterSnapshot, onOpenNetwork: () -> Unit) {
    ZteCard {
        ZteSectionHeader("أقرب برج شبكة", "الخريطة لا تختلق موقع برج غير موثق", "أدوات البرج", onOpenNetwork)
        Spacer(Modifier.height(12.dp))
        ZteManagerMap(snapshot, Modifier.fillMaxWidth().height(330.dp))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZteInfoTile("PCI", snapshot.pci?.toString() ?: "—", Modifier.weight(1f))
            ZteInfoTile("EARFCN", snapshot.earfcn?.toString() ?: "—", Modifier.weight(1f))
            ZteInfoTile("Cell ID", snapshot.cellId?.toString() ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ZteActiveBandsCard(snapshot: RouterSnapshot, onOpen: () -> Unit) {
    val bands = zteActiveBands(snapshot)
    ZteCard {
        ZteSectionHeader("الترددات النشطة", "نعرض فقط ما ظهر في القياسات الحية", "قفل الترددات", onOpen)
        Spacer(Modifier.height(10.dp))
        if (bands.isEmpty()) {
            Text("الراوتر لم يعرض ترددات مؤكدة في هذه القراءة.", color = ZteMuted, fontSize = 14.sp)
        } else {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                bands.forEachIndexed { index, band ->
                    Surface(
                        shape = RoundedCornerShape(15.dp),
                        color = if (index % 2 == 0) ZteSoftGreen else ZteSoftBlue,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZteLine)
                    ) {
                        Text(band.uppercase(), color = ZteInk, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ZteSignalMonitorCard(samples: List<SafeTelemetrySample>, stability: ConnectionStabilityReport) {
    val recent = remember(samples) { samples.takeLast(24) }
    ZteCard {
        ZteSectionHeader("مراقبة الإشارة المباشرة", stability.summary)
        Spacer(Modifier.height(12.dp))
        if (recent.size < 2) {
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF5F8FD)), contentAlignment = Alignment.Center) {
                Text("نحتاج قراءات أكثر لرسم الحركة", color = ZteMuted, fontSize = 14.sp)
            }
        } else {
            ZteSignalChart(recent)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ZteLegendDot(ZteBlue, "RSRP")
            ZteLegendDot(ZtePurple, "SINR")
            ZteLegendDot(ZteCyan, "RSRQ")
        }
    }
}

@Composable
private fun ZteSignalChart(samples: List<SafeTelemetrySample>) {
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        fun path(values: List<Double?>, min: Double, max: Double): Path? {
            val available = values.mapIndexedNotNull { index, value -> value?.let { index to it } }
            if (available.size < 2) return null
            val p = Path()
            available.forEachIndexed { pointIndex, (sourceIndex, value) ->
                val x = if (values.size <= 1) 0f else sourceIndex.toFloat() / (values.size - 1).toFloat() * size.width
                val normalized = ((value - min) / (max - min)).coerceIn(0.0, 1.0).toFloat()
                val y = size.height - normalized * size.height
                if (pointIndex == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            return p
        }
        repeat(4) { i ->
            val y = size.height * i / 3f
            drawLine(Color(0xFFE5ECF7), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        path(samples.map { it.nrRsrp ?: it.lteRsrp }, -125.0, -70.0)?.let { drawPath(it, ZteBlue, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) }
        path(samples.map { it.nrSinr ?: it.lteSinr }, -5.0, 30.0)?.let { drawPath(it, ZtePurple, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) }
        path(samples.map { it.lteRsrq }, -22.0, -6.0)?.let { drawPath(it, ZteCyan, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) }
    }
}

@Composable
private fun ZteLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = ZteMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ZteQuickToolsCard(onOpenTools: () -> Unit) {
    ZteCard {
        ZteSectionHeader("أدوات التشخيص", "أدوات مساعدة لا تغيّر إعدادات الراوتر من تلقاء نفسها")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("فحص الشبكة", "تحسين الأداء", "سجل الأحداث").forEach { label ->
                Surface(
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenTools),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF5F9FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZteLine)
                ) {
                    Text(label, color = ZteInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 7.dp, vertical = 18.dp))
                }
            }
        }
    }
}

@Composable
private fun ZteRouterInfoStrip(snapshot: RouterSnapshot, traffic: TrafficTelemetry?, deviceCount: Int, onOpenMore: () -> Unit) {
    ZteCard(modifier = Modifier.clickable(onClick = onOpenMore)) {
        ZteSectionHeader("معلومات الراوتر", "اضغط لعرض التفاصيل والأجهزة")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ZteInfoTile("الجهاز", snapshot.model ?: "ZTE", Modifier.weight(1f))
            ZteInfoTile("الشبكة", zteOperator(snapshot), Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ZteInfoTile("نوع الاتصال", zteNetworkLabel(snapshot), Modifier.weight(1f))
            ZteInfoTile("مدة التشغيل", zteDuration(traffic?.sessionSeconds), Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        Text("$deviceCount جهاز متصل حسب آخر قراءة من الراوتر", color = ZteMuted, fontSize = 13.sp)
    }
}

@Composable
private fun ZteOperationBanner(message: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = ZteSoftBlue) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(ZteBlue))
            Spacer(Modifier.width(9.dp))
            Text(message, color = ZteInk, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
        }
    }
}
