package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.RouterSnapshot

@Composable
internal fun GlassNetworkModeCard(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    GlassCard {
        Column(Modifier.padding(14.dp)) {
            Text("وضع الشبكة", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(glassModeWords(snapshot), color = GlassMuted, fontSize = 9.sp)
            Spacer(Modifier.height(10.dp))
            GlassModeButton("تلقائي", "الراوتر يختار الأنسب", !busy) { onMode("AUTO") }
            Spacer(Modifier.height(7.dp))
            GlassModeButton("5G فقط", "عندما يكون الجيل الخامس ثابتًا", !busy) { onMode("5G_ONLY") }
            Spacer(Modifier.height(7.dp))
            GlassModeButton("4G فقط", "إذا كان الجيل الخامس يتذبذب", !busy) { onMode("4G_ONLY") }
        }
    }
}

@Composable
internal fun GlassFrequencySummaryCard(snapshot: RouterSnapshot, onOpen: () -> Unit) {
    val active = buildList {
        snapshot.lteBand?.takeIf { it.isNotBlank() }?.let { add("4G $it") }
        snapshot.nrBand?.takeIf { it.isNotBlank() }?.let { add("5G $it") }
        snapshot.cells.mapNotNull { it.band?.takeIf(String::isNotBlank) }.distinct().forEach { band ->
            if (none { it.endsWith(band) }) add(band)
        }
    }
    GlassCard {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الترددات", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("تخصيص", color = GlassBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOpen))
            }
            Spacer(Modifier.height(9.dp))
            Text(
                if (active.isEmpty()) "الراوتر لم يعرض ترددات مؤكدة الآن" else active.joinToString(" • "),
                color = GlassInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (snapshot.caActive) "يوجد دمج ترددات فعلي الآن" else "لا يوجد دمج مؤكد الآن",
                color = if (snapshot.caActive) GlassMint else GlassMuted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
internal fun GlassSignalMonitorCard(samples: List<SafeTelemetrySample>, stability: ConnectionStabilityReport) {
    val values = remember(samples) { samples.mapNotNull { it.nrRsrp ?: it.lteRsrp }.takeLast(20) }
    GlassCard {
        Column(Modifier.padding(14.dp)) {
            Text("مراقبة الإشارة", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(stability.summary, color = GlassMuted, fontSize = 9.sp, lineHeight = 13.sp)
            Spacer(Modifier.height(10.dp))
            if (values.size < 2) {
                Box(
                    Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(14.dp)).background(androidx.compose.ui.graphics.Color(0xFFF5F8FC)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) { Text("نحتاج قراءات أكثر لرسم الحركة", color = GlassMuted, fontSize = 10.sp) }
            } else {
                Canvas(Modifier.fillMaxWidth().height(92.dp)) {
                    val min = -125.0
                    val max = -70.0
                    val stepX = size.width / (values.size - 1)
                    val path = Path()
                    values.forEachIndexed { index, value ->
                        val normalized = ((value - min) / (max - min)).coerceIn(0.0, 1.0).toFloat()
                        val x = index * stepX
                        val y = size.height - normalized * size.height
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = GlassBlue, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
    }
}

@Composable
internal fun GlassDeviceInfoCard(snapshot: RouterSnapshot, traffic: TrafficTelemetry?, deviceCount: Int, onOpenMore: () -> Unit) {
    GlassCard {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("معلومات سريعة", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("الأجهزة", color = GlassBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOpenMore))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                GlassInfoItem("الراوتر", snapshot.model ?: "ZTE", Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(36.dp).background(GlassLine))
                GlassInfoItem("الشبكة", glassOperator(snapshot), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(36.dp).background(GlassLine))
                GlassInfoItem("مدة التشغيل", glassDuration(traffic?.sessionSeconds), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text("$deviceCount جهاز متصل حسب القراءة الحالية من الراوتر", color = GlassMuted, fontSize = 9.sp)
        }
    }
}
