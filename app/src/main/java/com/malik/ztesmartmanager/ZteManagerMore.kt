package com.malik.ztesmartmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.ConnectedDevice
import com.malik.ztesmartmanager.core.model.DeviceTransport
import com.malik.ztesmartmanager.core.model.RouterSnapshot

@Composable
internal fun ZteManagerMore(
    snapshot: RouterSnapshot,
    traffic: TrafficTelemetry?,
    devices: List<ConnectedDevice>,
    onDisconnect: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ZteRouterDetailsCard(snapshot, traffic) }
        item { ZteConnectedDevicesCard(devices) }
        item {
            ZteCard {
                ZteSectionHeader("إدارة الأجهزة", "الحظر وQoS والجدولة لا تُفعّل إلا بعد إثبات API خاص بالـFirmware")
                Spacer(Modifier.height(10.dp))
                Text(
                    "نعرض الأجهزة التي قرأها الراوتر فعلًا. لن نضع أزرار حظر أو تحديد سرعة شكلية إذا لم يكن أمرها وread-back موثقين لهذا الجهاز.",
                    color = ZteMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
        item {
            ZteCard {
                ZteSectionHeader("الاتصال بالراوتر")
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZteRed)
                ) {
                    Text("قطع الاتصال", fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun ZteRouterDetailsCard(snapshot: RouterSnapshot, traffic: TrafficTelemetry?) {
    ZteCard {
        ZteSectionHeader("معلومات الجهاز", "بيانات مباشرة من الراوتر")
        Spacer(Modifier.height(10.dp))
        ZteMoreLine("الموديل", snapshot.model ?: "—")
        ZteMoreLine("Firmware", snapshot.firmware ?: "—")
        ZteMoreLine("Hardware", snapshot.hardwareVersion ?: "—")
        ZteMoreLine("اسم الشبكة", zteOperator(snapshot))
        ZteMoreLine("نوع الاتصال", zteNetworkLabel(snapshot))
        ZteMoreLine("مدة التشغيل", zteDuration(traffic?.sessionSeconds))
        ZteMoreLine("التحميل اللحظي", traffic?.rxBytesPerSecond?.let { "${String.format("%.2f", it * 8 / 1_000_000.0)} Mb/s" } ?: "—")
        ZteMoreLine("الرفع اللحظي", traffic?.txBytesPerSecond?.let { "${String.format("%.2f", it * 8 / 1_000_000.0)} Mb/s" } ?: "—")
    }
}

@Composable
private fun ZteConnectedDevicesCard(devices: List<ConnectedDevice>) {
    ZteCard {
        ZteSectionHeader("الأجهزة المتصلة", "${devices.size} جهاز حسب آخر قراءة")
        Spacer(Modifier.height(10.dp))
        if (devices.isEmpty()) {
            Text(
                "الراوتر لم يرجع قائمة أجهزة في القراءة الحالية؛ لن نختلق أجهزة افتراضية.",
                color = ZteMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        } else {
            devices.forEachIndexed { index, device ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = if (device.transport == DeviceTransport.WIFI) ZteSoftBlue else ZteSoftGreen) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Text(if (device.transport == DeviceTransport.WIFI) "Wi" else "LAN", color = ZteInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.displayName, color = ZteInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(device.ipAddress ?: "IP غير متاح", color = ZteMuted, fontSize = 13.sp)
                        Text(device.macAddress, color = ZteMuted, fontSize = 12.sp)
                    }
                    Text(if (device.transport == DeviceTransport.WIFI) "Wi‑Fi" else "LAN", color = ZteBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                if (index != devices.lastIndex) HorizontalDivider(color = ZteLine)
            }
        }
    }
}

@Composable
private fun ZteMoreLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ZteMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}
