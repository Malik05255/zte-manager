package com.malik.ztesmartmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.ConnectedDevice
import com.malik.ztesmartmanager.core.model.DeviceTransport
import com.malik.ztesmartmanager.core.model.RouterSnapshot

@Composable
internal fun GlassMoreScreen(
    snapshot: RouterSnapshot,
    traffic: TrafficTelemetry?,
    devices: List<ConnectedDevice>,
    onDisconnect: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard {
                Column(Modifier.padding(14.dp)) {
                    Text("الراوتر", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    GlassTechnicalRow("الموديل", snapshot.model ?: "غير متاح")
                    GlassTechnicalRow("الإصدار", snapshot.firmware ?: "غير متاح")
                    GlassTechnicalRow("الشبكة", glassOperator(snapshot))
                    GlassTechnicalRow("مدة التشغيل", glassDuration(traffic?.sessionSeconds))
                }
            }
        }

        item { Text("الأجهزة المتصلة", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black) }
        if (devices.isEmpty()) {
            item { GlassNotice("الراوتر لم يعرض أجهزة متصلة الآن") }
        } else {
            items(devices) { device ->
                GlassCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFEAF3FF)),
                            contentAlignment = Alignment.Center
                        ) { Text("•", color = GlassBlue, fontSize = 22.sp) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(device.displayName, color = GlassInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(device.ipAddress ?: "العنوان غير ظاهر", color = GlassMuted, fontSize = 9.sp)
                            Text(
                                if (device.transport == DeviceTransport.WIFI) "متصل عبر Wi‑Fi" else "متصل عبر LAN",
                                color = GlassMuted,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text("قطع الاتصال بالراوتر", color = GlassRed, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
