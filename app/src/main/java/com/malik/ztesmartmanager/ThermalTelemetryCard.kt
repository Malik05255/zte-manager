package com.malik.ztesmartmanager

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetry
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetryFormatter

private val ThermalBg = Color(0xFFF7F2E9)
private val ThermalInk = Color(0xFF2A241E)
private val ThermalMuted = Color(0xFF776E63)
private val ThermalAccent = Color(0xFF876126)
private val ThermalLine = Color(0xFFD2C8B9)

@Composable
fun ThermalTelemetryCard(
    telemetry: ThermalTelemetry,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    if (!telemetry.hasAnyEvidence) return
    val highest = telemetry.highestObserved

    Card(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 1.dp),
        shape = RoundedCornerShape(if (compact) 18.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = ThermalBg)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = if (compact) 4.dp else 6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حرارة الراوتر", color = ThermalInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text("قراءة حساسات ZTE فقط • بدون استنتاج حد خطر", color = ThermalMuted, fontSize = 6.sp, maxLines = 1)
                }
                highest?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("أعلى قراءة مرصودة", color = ThermalMuted, fontSize = 6.sp)
                        Text(
                            "${it.label} ${ThermalTelemetryFormatter.celsius(it.celsius)}",
                            color = ThermalAccent,
                            fontSize = if (compact) 9.sp else 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.size(if (compact) 3.dp else 5.dp))
            Text(
                ThermalTelemetryFormatter.summary(telemetry),
                color = ThermalInk,
                fontSize = if (compact) 6.sp else 7.sp,
                lineHeight = if (compact) 8.sp else 9.sp,
                maxLines = if (compact) 2 else 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ThermalLine.copy(alpha = 0.65f), RoundedCornerShape(11.dp))
                    .padding(horizontal = 7.dp, vertical = if (compact) 3.dp else 4.dp)
            )
        }
    }
}
