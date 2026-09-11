package com.malik.ztesmartmanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ZteHero(snapshot, qualityScore) }
        item { ZtePlacementCoach(placementMode, placementReading, onPlacementToggle) }
        item { ZteSpeedCard(lastPerformance, speedBusy, onSpeedTest) }
        item { ZteModeCard(snapshot, controlBusy, onSetNetworkMode) }
        item { ZteTowerMapCard(snapshot, onOpenNetwork) }
        item { ZteCarrierSummary(snapshot, onOpenNetwork) }
        item { ZteSignalSummary(snapshot, qualityScore) }
        item { ZteQuickToolsCard(onOpenNetwork, onOpenTools, onOpenMore) }
        item { ZteRouterInfoCard(snapshot, traffic, deviceCount) }
        if (operationMessage.isNotBlank() || status.isNotBlank()) {
            item { ZteOperationBanner(operationMessage.ifBlank { status }) }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun ZteHero(snapshot: RouterSnapshot, qualityScore: Int) {
    var showTechnical by rememberSaveable { mutableStateOf(false) }
    val quality = zteQualityWord(qualityScore)
    val qualityColor = zteQualityColor(qualityScore)

    ZteCard(contentPadding = PaddingValues(0.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ZteHeroBrush)
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ZteRouterIllustration(snapshot.model ?: "ZTE")
            Spacer(Modifier.height(16.dp))
            ZteStatusPill(quality, qualityScore >= 70)
            Spacer(Modifier.height(12.dp))
            Text(
                zteNetworkLabel(snapshot),
                color = ZteBlue,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(zteOperator(snapshot), color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text(
                if (qualityScore > 0) "الاتصال $quality" else "بانتظار قياسات كافية",
                color = qualityColor,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                zteHumanNetworkAdvice(qualityScore),
                color = ZteMuted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            ZteSignalBars(qualityScore, qualityColor)
            Spacer(Modifier.height(18.dp))
            ZteSecondaryButton(
                if (showTechnical) "إخفاء التفاصيل الفنية" else "عرض التفاصيل الفنية"
            ) { showTechnical = !showTechnical }
            AnimatedVisibility(showTechnical) {
                Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    ZteTechnicalLine("قوة الإشارة", snapshot.nrRsrp ?: snapshot.lteRsrp, "RSRP", "dBm")
                    ZteTechnicalLine("نظافة الإشارة", snapshot.nrSinr ?: snapshot.lteSinr, "SINR", "dB")
                    ZteTechnicalLine("جودة الإشارة", snapshot.lteRsrq, "RSRQ", "dB")
                }
            }
        }
    }
}

@Composable
private fun ZteRouterIllustration(model: String) {
    Surface(
        modifier = Modifier.width(104.dp).height(138.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 7.dp
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.width(46.dp).height(5.dp).clip(CircleShape).background(Color(0xFF2E405B)))
            Text("ZTE", color = ZteMuted, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { index ->
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (index < 3) ZteGreen else ZteBlue))
                }
            }
            Text(model, color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun ZteSignalBars(score: Int, color: Color) {
    val active = when {
        score >= 82 -> 5
        score >= 70 -> 4
        score >= 58 -> 3
        score >= 43 -> 2
        score > 0 -> 1
        else -> 0
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(5) { index ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .width(12.dp)
                    .height((18 + index * 8).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (index < active) color else ZteLine)
            )
        }
    }
}

@Composable
private fun ZteTechnicalLine(title: String, value: Double?, metric: String, unit: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.78f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ZteLine)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(
                "${zteMetricWord(metric, value)}${value?.let { " — ${it.roundToInt()} $unit" }.orEmpty()}",
                color = ZteMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ZtePlacementCoach(active: Boolean, reading: PlacementReading?, onToggle: () -> Unit) {
    val (title, body, color) = ztePlacementWords(reading)
    val score = reading?.score?.total ?: 0

    ZteCard {
        ZteSectionHeader("أفضل مكان للراوتر", "بدون أرقام غامضة: نقول لك ماذا تفعل مباشرة")
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = color.copy(alpha = 0.09f)) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Text(title, color = color, fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text(body, color = ZteInk, fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { (score / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = color,
                    trackColor = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(9.dp))
                Text(ztePlacementStepLabel(score), color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        ZtePrimaryButton(if (active) "إيقاف جولة تحسين المكان" else "ابدأ البحث عن أفضل مكان", true, onToggle)
    }
}

@Composable
private fun ZteSpeedCard(performance: NetworkPerformance?, busy: Boolean, onRun: () -> Unit) {
    ZteCard {
        ZteSectionHeader("اختبار السرعة", "قياس حقيقي للاتصال بالإنترنت")
        Spacer(Modifier.height(14.dp))
        ZteSpeedGauge(performance?.downloadMbps)
        Spacer(Modifier.height(4.dp))
        Text(
            performance?.downloadMbps?.let { "${String.format("%.1f", it)} Mb/s" }
                ?: if (busy) "جاري القياس…" else "لم يتم القياس بعد",
            color = ZteInk,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )
        if (performance != null) {
            Spacer(Modifier.height(14.dp))
            ZteInfoTile("الاستجابة", performance.latencyMs?.let { "${it.roundToInt()} ms" } ?: "غير متاحة")
            Spacer(Modifier.height(9.dp))
            ZteInfoTile("تذبذب الاستجابة", performance.jitterMs?.let { "${it.roundToInt()} ms" } ?: "غير متاح")
            Spacer(Modifier.height(9.dp))
            ZteInfoTile("الفقد", performance.packetLossPercent?.let { "${it.roundToInt()}%" } ?: "غير متاح")
        }
        Spacer(Modifier.height(14.dp))
        ZtePrimaryButton(if (busy) "جاري الاختبار…" else "ابدأ الاختبار الحقيقي", !busy, onRun)
    }
}

@Composable
private fun ZteSpeedGauge(speed: Double?) {
    val fraction = ((speed ?: 0.0) / 1000.0).coerceIn(0.0, 1.0).toFloat()
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(255.dp).height(138.dp)) {
            val stroke = 13.dp.toPx()
            drawArc(ZteLine, 180f, 180f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(ZteBlue, 180f, 180f * fraction, false, style = Stroke(stroke, cap = StrokeCap.Round))
            val center = Offset(size.width / 2f, size.height * 0.87f)
            val angle = Math.toRadians((180 + 180 * fraction).toDouble())
            val radius = size.width * 0.37f
            val end = Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
            drawLine(ZteDeepBlue, center, end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun ZteModeCard(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    ZteCard {
        ZteSectionHeader("وضع الشبكة", "كل خيار في سطر مستقل حتى لا تنضغط العناصر")
        Spacer(Modifier.height(14.dp))
        ZteModeOption("تلقائي", "يختار الراوتر أفضل جيل متاح", false, !busy) { onMode("WL_AND_5G") }
        Spacer(Modifier.height(9.dp))
        ZteModeOption("5G + 4G", "يفضل الجيل الخامس مع الرجوع للرابع", zteIsFiveG(snapshot), !busy) { onMode("LTE_AND_5G") }
        Spacer(Modifier.height(9.dp))
        ZteModeOption("4G فقط", "يثبت الاتصال على الجيل الرابع", zteNetworkLabel(snapshot).contains("4G"), !busy) { onMode("Only_LTE") }
        Spacer(Modifier.height(9.dp))
        ZteModeOption("3G فقط", "استخدمه فقط عند الحاجة", snapshot.networkType.orEmpty().contains("WCDMA", true), !busy) { onMode("Only_WCDMA") }
    }
}

@Composable
private fun ZteModeOption(title: String, subtitle: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) ZteSoftBlue else ZteSurfaceAlt,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) ZteBlue.copy(alpha = 0.5f) else ZteLine)
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(if (selected) ZteBlue else ZteLine),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Box(Modifier.size(9.dp).clip(CircleShape).background(Color.White))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ZteTowerMapCard(snapshot: RouterSnapshot, onOpenNetwork: () -> Unit) {
    ZteCard {
        ZteSectionHeader("الخريطة والبرج", "نعرض موقعك الحقيقي، ولا نرسم برجًا إلا عند وجود إحداثيات موثقة")
        Spacer(Modifier.height(14.dp))
        ZteManagerMap(snapshot, Modifier.fillMaxWidth().height(330.dp))
        Spacer(Modifier.height(14.dp))
        ZteSecondaryButton("فتح أدوات البرج", true, onOpenNetwork)
    }
}

@Composable
private fun ZteCarrierSummary(snapshot: RouterSnapshot, onOpen: () -> Unit) {
    var showBands by rememberSaveable { mutableStateOf(false) }
    val bands = zteActiveBands(snapshot)
    ZteCard {
        ZteSectionHeader("دمج الترددات")
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                snapshot.caActive && bands.size > 1 -> "الراوتر يدمج ${bands.size} ترددات الآن"
                snapshot.caActive -> "الراوتر يدمج ترددات الآن"
                else -> "لا يوجد دمج ترددات مؤكد حاليًا"
            },
            color = if (snapshot.caActive) ZteGreen else ZteInk,
            fontSize = 20.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (snapshot.caActive) "هذا يساعد الراوتر على الاستفادة من أكثر من قناة اتصال." else "قد يتغير ذلك تلقائيًا حسب البرج والشبكة.",
            color = ZteMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        if (bands.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ZteSecondaryButton(if (showBands) "إخفاء أسماء الترددات" else "عرض أسماء الترددات") { showBands = !showBands }
            AnimatedVisibility(showBands) {
                Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    bands.forEach { band ->
                        ZteInfoTile("تردد نشط", band)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ZteSecondaryButton("إدارة الترددات", true, onOpen)
    }
}

@Composable
private fun ZteSignalSummary(snapshot: RouterSnapshot, qualityScore: Int) {
    val quality = zteQualityWord(qualityScore)
    ZteCard {
        ZteSectionHeader("حالة الإشارة", "ملخص مفهوم بدل الأرقام الفنية")
        Spacer(Modifier.height(12.dp))
        Text("الإشارة $quality", color = zteQualityColor(qualityScore), fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(zteHumanNetworkAdvice(qualityScore), color = ZteMuted, fontSize = 15.sp, lineHeight = 22.sp)
        if (snapshot.caActive) {
            Spacer(Modifier.height(10.dp))
            Text("والراوتر يستخدم دمج ترددات الآن.", color = ZteGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ZteQuickToolsCard(onNetwork: () -> Unit, onTools: () -> Unit, onMore: () -> Unit) {
    ZteCard {
        ZteSectionHeader("وصول سريع", "ثلاثة مسارات واضحة بدل شبكة أزرار مزدحمة")
        Spacer(Modifier.height(14.dp))
        ZteActionRow("الشبكة والترددات", "الأبراج، الأوضاع، القفل والترددات", onNetwork)
        Spacer(Modifier.height(9.dp))
        ZteActionRow("الأدوات الذكية", "تحسين المكان، النسخة الآمنة والتشخيص", onTools)
        Spacer(Modifier.height(9.dp))
        ZteActionRow("الأجهزة والمعلومات", "الأجهزة المتصلة وتفاصيل الراوتر", onMore)
    }
}

@Composable
private fun ZteActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = ZteSurfaceAlt,
        border = androidx.compose.foundation.BorderStroke(1.dp, ZteLine)
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text("‹", color = ZteBlue, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ZteRouterInfoCard(snapshot: RouterSnapshot, traffic: TrafficTelemetry?, deviceCount: Int) {
    ZteCard {
        ZteSectionHeader("معلومات سريعة")
        Spacer(Modifier.height(12.dp))
        ZteInfoTile("الراوتر", snapshot.model ?: "غير معروف")
        Spacer(Modifier.height(9.dp))
        ZteInfoTile("مدة التشغيل", zteDuration(traffic?.sessionSeconds))
        Spacer(Modifier.height(9.dp))
        ZteInfoTile("الأجهزة المتصلة", "$deviceCount جهاز")
    }
}

@Composable
private fun ZteOperationBanner(message: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = ZteSoftBlue) {
        Text(
            message,
            color = ZteInk,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
}

private fun zteHumanNetworkAdvice(score: Int): String = when {
    score >= 92 -> "الوضع ممتاز جدًا. لا تحتاج لتغيير مكان الراوتر الآن."
    score >= 82 -> "الوضع ممتاز. ثبّت الراوتر إذا كانت السرعة مناسبة لك."
    score >= 70 -> "الوضع جيد جدًا. قد يتحسن أكثر بتحريك بسيط للراوتر."
    score >= 58 -> "الوضع جيد، لكن يوجد مجال واضح للتحسين."
    score >= 43 -> "الوضع مقبول. جرّب مكانًا أعلى أو أقرب للنافذة."
    score > 0 -> "الإشارة ضعيفة. غيّر مكان الراوتر ثم قارن النتيجة."
    else -> "نحتاج عدة قراءات قبل إعطاء حكم واضح."
}

private fun ztePlacementStepLabel(score: Int): String = when {
    score >= 90 -> "وصلت تقريبًا لأفضل مكان — ثبّت الراوتر هنا."
    score >= 75 -> "ممتاز، بقيت خطوة بسيطة إن أردت التحسين أكثر."
    score >= 60 -> "جيد، استمر قليلًا وجرب اتجاهًا واحدًا كل مرة."
    score >= 40 -> "مقبول، ما زال هناك فرق واضح يمكن الوصول إليه."
    score > 0 -> "ابدأ بتغيير المكان بوضوح ثم انتظر القراءة الجديدة."
    else -> "ابدأ الجولة وسنرشدك خطوة بخطوة."
}
