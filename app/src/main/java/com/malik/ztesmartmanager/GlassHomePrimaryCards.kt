package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import kotlin.math.roundToInt

@Composable
internal fun GlassPlacementCard(active: Boolean, reading: PlacementReading?, onToggle: () -> Unit) {
    val (title, body, color) = glassPlacementWords(reading)
    GlassCard {
        Column(Modifier.padding(16.dp)) {
            Text("أفضل مكان للراوتر", color = GlassInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text("حرّك الراوتر وسنخبرك بالكلام، لا بالأرقام", color = GlassMuted, fontSize = 10.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.10f)).padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(11.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = GlassInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(body, color = GlassMuted, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            Spacer(Modifier.height(11.dp))
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlassBlue)
            ) { Text(if (active) "إيقاف البحث" else "ابدأ البحث عن أفضل مكان", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
internal fun GlassSpeedTestCard(performance: NetworkPerformance?, busy: Boolean, onRun: () -> Unit) {
    GlassCard {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("اختبار السرعة", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(if (performance == null) "لم يُقَس بعد" else glassSpeedWord(performance.downloadMbps), color = if (performance == null) GlassMuted else GlassBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            GlassSpeedGauge(performance?.downloadMbps)
            Text(
                performance?.downloadMbps?.let { "${it.roundToInt()} ميجابت/ث" } ?: if (busy) "جاري القياس…" else "اضغط لبدء قياس حقيقي",
                color = GlassInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            performance?.latencyMs?.let { Text(glassLatencyWord(it), color = GlassMuted, fontSize = 9.sp) }
            Spacer(Modifier.height(10.dp))
            GlassActionButton(if (busy) "جاري القياس…" else "ابدأ الاختبار", !busy, onRun)
        }
    }
}

@Composable
private fun GlassSpeedGauge(speed: Double?) {
    val fraction = ((speed ?: 0.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
    Box(Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp, 88.dp)) {
            val stroke = 9.dp.toPx()
            drawArc(Color(0xFFE7EDF6), 180f, 180f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(GlassBlue, 180f, 180f * fraction, false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(speed?.roundToInt()?.toString() ?: "—", color = GlassDeepBlue, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 31.dp))
    }
}

@Composable
internal fun GlassMapCard(snapshot: RouterSnapshot, onOpenNetwork: () -> Unit) {
    GlassCard {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("أنت والبرج", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text("موقعك حقيقي، والخط لا يظهر إلا للبرج الموثق", color = GlassMuted, fontSize = 9.sp)
                }
                Text("أدوات البرج", color = GlassBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOpenNetwork))
            }
            Spacer(Modifier.height(10.dp))
            PulseNetworkMap(snapshot, Modifier.fillMaxWidth().height(330.dp))
        }
    }
}
