package com.malik.ztesmartmanager

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.smart.PlacementGuidance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal val MpBg = Color(0xFFF4F8FD)
internal val MpCard = Color(0xFFFFFFFF)
internal val MpInk = Color(0xFF0A285C)
internal val MpMuted = Color(0xFF74839B)
internal val MpBlue = Color(0xFF1675F7)
internal val MpCyan = Color(0xFF19BCEB)
internal val MpGreen = Color(0xFF19B879)
internal val MpAmber = Color(0xFFF1A824)
internal val MpRed = Color(0xFFE55454)
internal val MpBorder = Color(0xFFDDE8F5)
internal val MpSoftBlue = Color(0xFFEBF4FF)
internal val MpSoftGreen = Color(0xFFEAF9F2)
internal val MpSoftPurple = Color(0xFFF3EEFF)
internal val MpSoftAmber = Color(0xFFFFF5E7)

@Composable
internal fun HaiSurface(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.shadow(3.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MpCard),
        border = BorderStroke(1.dp, MpBorder)
    ) {
        Box(Modifier.padding(padding.dp)) { content() }
    }
}

@Composable
internal fun HaiSectionTitle(title: String, subtitle: String? = null, badge: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MpInk, fontWeight = FontWeight.Black, fontSize = 18.sp)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = MpMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!badge.isNullOrBlank()) {
            Text(
                badge,
                color = MpBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(MpSoftBlue).padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
internal fun MetricPill(label: String, value: String, unit: String = "", accent: Color = MpBlue) {
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFFF7FAFE)).padding(horizontal = 11.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = MpMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = MpInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
            if (unit.isNotBlank()) {
                Spacer(Modifier.width(3.dp))
                Text(unit, color = MpMuted, fontSize = 9.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(38.dp).height(3.dp).clip(CircleShape).background(accent.copy(alpha = .22f))) {
            Box(Modifier.fillMaxWidth(.68f).height(3.dp).clip(CircleShape).background(accent))
        }
    }
}

@Composable
internal fun ActionTile(
    title: String,
    subtitle: String,
    glyph: String,
    background: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(21.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = .72f)),
                contentAlignment = Alignment.Center
            ) {
                Text(glyph, fontSize = 20.sp, color = MpInk, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (enabled) MpInk else MpMuted, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = MpMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun SpeedGauge(
    mbps: Double?,
    running: Boolean,
    modifier: Modifier = Modifier
) {
    val target = (mbps ?: 0.0).coerceAtMost(1000.0).toFloat()
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(900), label = "speed")
    val fraction = if (running && mbps == null) .28f else (animated / 500f).coerceIn(.03f, 1f)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(170.dp)) {
            val stroke = 16.dp.toPx()
            drawArc(
                color = Color(0xFFE0E8F2),
                startAngle = 145f,
                sweepAngle = 250f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(MpBlue, MpCyan, MpGreen, MpBlue)),
                startAngle = 145f,
                sweepAngle = 250f * fraction,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (running) "…" else if (mbps == null) "—" else String.format("%.1f", animated),
                color = MpInk,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black
            )
            Text("Mb/s", color = MpMuted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun PlacementScore(reading: PlacementReading?, modifier: Modifier = Modifier) {
    val score = reading?.score?.total ?: 0
    val progress by animateFloatAsState(score / 100f, tween(500), label = "placement")
    val color = qualityColor(score)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(210.dp)) {
            val stroke = 18.dp.toPx()
            drawCircle(Color(0xFFE5EDF6), style = Stroke(stroke))
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawCircle(color.copy(alpha = .08f), radius = size.minDimension * .36f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (reading == null) "—" else score.toString(), color = MpInk, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text(reading?.score?.label ?: "ابدأ القياس", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (reading != null) Text("ثقة ${reading.confidence}%", color = MpMuted, fontSize = 10.sp)
        }
    }
}

internal fun qualityColor(score: Int): Color = when {
    score >= 82 -> MpGreen
    score >= 58 -> MpBlue
    score >= 43 -> MpAmber
    score > 0 -> MpRed
    else -> MpMuted
}

internal fun placementGuidanceText(guidance: PlacementGuidance?): String = when (guidance) {
    PlacementGuidance.INITIAL -> "حرّك الراوتر ببطء، وسنحفظ أفضل نقطة تلقائيًا"
    PlacementGuidance.MUCH_BETTER -> "أفضل بكثير — استمر بهذا الاتجاه"
    PlacementGuidance.BETTER -> "أفضل — استمر قليلًا"
    PlacementGuidance.STABLE -> "الإشارة مستقرة في هذه النقطة"
    PlacementGuidance.WORSE -> "تراجعت الجودة — جرّب الاتجاه العكسي"
    PlacementGuidance.RETURN_TO_BEST -> "ارجع للنقطة السابقة؛ كانت أفضل"
    PlacementGuidance.CELL_CHANGED_WORSE -> "تغيّرت الخلية والجودة انخفضت"
    PlacementGuidance.EXCELLENT_HOLD -> "ممتاز — ثبت الراوتر هنا"
    PlacementGuidance.BEST_SO_FAR -> "هذه أفضل نقطة رصدناها حتى الآن"
    null -> "ابدأ المساعد ثم حرّك الراوتر ببطء داخل المكان"
}

@Composable
internal fun PlacementSoundEffect(enabled: Boolean, active: Boolean, reading: PlacementReading?) {
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull() }
    DisposableEffect(tone) {
        onDispose { runCatching { tone?.release() } }
    }

    val score = reading?.score?.total ?: 0
    LaunchedEffect(enabled, active, score, tone) {
        if (!enabled || !active || tone == null || reading == null) return@LaunchedEffect
        while (true) {
            val toneType = if (score >= 82) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
            tone.startTone(toneType, if (score >= 82) 90 else 55)
            val interval = when {
                score >= 90 -> 330L
                score >= 82 -> 500L
                score >= 70 -> 750L
                score >= 55 -> 1_100L
                else -> 1_550L
            }
            delay(interval)
        }
    }
}

internal fun Double?.metric(decimals: Int = 1): String {
    if (this == null || isNaN() || isInfinite()) return "—"
    return if (decimals == 0) roundToInt().toString() else String.format("%.${decimals}f", this)
}

@Composable
internal fun StatusBanner(message: String) {
    if (message.isBlank()) return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(MpSoftBlue).padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(MpBlue))
        Spacer(Modifier.width(8.dp))
        Text(message, color = MpInk, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun EmptyState(title: String, body: String, glyph: String = "•") {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 26.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(MpSoftBlue), contentAlignment = Alignment.Center) {
            Text(glyph, color = MpBlue, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, color = MpInk, fontWeight = FontWeight.Black, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(body, color = MpMuted, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
    }
}
