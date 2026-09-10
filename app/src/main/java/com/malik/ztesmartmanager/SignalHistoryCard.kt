package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.SignalHistoryModel
import com.malik.ztesmartmanager.core.diagnostics.SignalHistoryPoint
import com.malik.ztesmartmanager.core.diagnostics.SignalHistoryPresenter
import com.malik.ztesmartmanager.core.diagnostics.SignalSeries
import com.malik.ztesmartmanager.core.diagnostics.SignalTrend
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val SignalCardBg = Color(0xFFF7F2E9)
private val SignalInk = Color(0xFF2A241E)
private val SignalMuted = Color(0xFF776E63)
private val SignalLte = Color(0xFF6E7B63)
private val SignalNr = Color(0xFF9A6A2F)
private val SignalGrid = Color(0xFFD8D0C4)

@Composable
fun SignalHistoryCard(
    samples: List<SafeTelemetrySample>,
    modifier: Modifier = Modifier
) {
    val model = remember(samples) { SignalHistoryPresenter.from(samples) }

    Card(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SignalCardBg)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("سجل الإشارة", color = SignalInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (model.hasAnySignal) "آخر ${model.sampleSlots.coerceAtMost(30)} قراءة • بدون ملء القيم المفقودة" else "بانتظار قياسات RSRP موثوقة",
                        color = SignalMuted,
                        fontSize = 6.sp
                    )
                }
                SignalLegend("LTE", SignalLte, model.lte)
                Spacer(Modifier.size(7.dp))
                SignalLegend("5G NR", SignalNr, model.nr)
            }

            Spacer(Modifier.size(6.dp))
            SignalPlot(model = model, modifier = Modifier.fillMaxWidth().height(92.dp))
            Spacer(Modifier.size(4.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SignalTrendText("LTE", model.lte, Modifier.weight(1f))
                SignalTrendText("5G", model.nr, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SignalLegend(label: String, color: Color, series: SignalSeries) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.size(3.dp))
        Text(
            "$label ${series.latestDbm?.let(::formatDbm) ?: "—"}",
            color = if (series.latestDbm != null) SignalInk else SignalMuted,
            fontSize = 6.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SignalTrendText(label: String, series: SignalSeries, modifier: Modifier) {
    val trend = when (series.trend) {
        SignalTrend.IMPROVING -> "يتحسن ${formatDelta(series.changeDb)}"
        SignalTrend.DECLINING -> "ينخفض ${formatDelta(series.changeDb)}"
        SignalTrend.FLAT -> "مستقر تقريبًا ${formatDelta(series.changeDb)}"
        SignalTrend.INSUFFICIENT -> "${series.points.size}/5 نقاط للاتجاه"
    }
    Text(
        "$label: $trend",
        modifier = modifier,
        color = SignalMuted,
        fontSize = 6.sp,
        maxLines = 1
    )
}

@Composable
private fun SignalPlot(model: SignalHistoryModel, modifier: Modifier = Modifier) {
    val allValues = remember(model) { (model.lte.points + model.nr.points).map { it.dbm } }
    val minDbm = if (allValues.isEmpty()) -120.0 else min(-60.0, allValues.minOrNull()!! - 4.0)
    val maxDbm = if (allValues.isEmpty()) -70.0 else max(-115.0, allValues.maxOrNull()!! + 4.0)
    val safeSpan = (maxDbm - minDbm).coerceAtLeast(10.0)

    Box(
        modifier = modifier
            .border(1.dp, SignalGrid.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
            .padding(6.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val left = 4.dp.toPx()
            val right = size.width - 4.dp.toPx()
            val top = 5.dp.toPx()
            val bottom = size.height - 5.dp.toPx()
            val width = (right - left).coerceAtLeast(1f)
            val height = (bottom - top).coerceAtLeast(1f)

            repeat(3) { row ->
                val y = top + height * row / 2f
                drawLine(
                    color = SignalGrid.copy(alpha = 0.55f),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            fun offset(point: SignalHistoryPoint): Offset {
                val denominator = (model.sampleSlots - 1).coerceAtLeast(1)
                val x = left + width * point.slot.toFloat() / denominator.toFloat()
                val normalized = ((point.dbm - minDbm) / safeSpan).coerceIn(0.0, 1.0)
                val y = bottom - height * normalized.toFloat()
                return Offset(x, y)
            }

            fun drawSeries(points: List<SignalHistoryPoint>, color: Color) {
                if (points.isEmpty()) return
                var segment = mutableListOf<SignalHistoryPoint>()

                fun flushSegment() {
                    if (segment.size >= 2) {
                        val path = Path()
                        segment.forEachIndexed { index, point ->
                            val p = offset(point)
                            if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                        }
                        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
                    }
                    segment.forEach { point ->
                        drawCircle(color = color, radius = 2.1.dp.toPx(), center = offset(point))
                    }
                    segment = mutableListOf()
                }

                points.forEach { point ->
                    val previous = segment.lastOrNull()
                    if (previous != null && point.slot != previous.slot + 1) flushSegment()
                    segment += point
                }
                flushSegment()
            }

            drawSeries(model.lte.points, SignalLte)
            drawSeries(model.nr.points, SignalNr)
        }
    }
}

private fun formatDbm(value: Double): String = String.format(Locale.US, "%.0f dBm", value)

private fun formatDelta(value: Double?): String {
    if (value == null) return ""
    val sign = if (value > 0) "+" else ""
    return String.format(Locale.US, "%s%.1f dB", sign, value)
}
