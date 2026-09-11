package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ZteRouterDetailsCard(snapshot, traffic) }
        item { ZteConnectedDevicesCard(devices) }
        item { ZteDeviceControlTruthCard() }
        item { ZteDisconnectCard(onDisconnect) }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun ZteRouterDetailsCard(snapshot: RouterSnapshot, traffic: TrafficTelemetry?) {
    ZteCard {
        ZteSectionHeader("معلومات الراوتر", "كل معلومة في سطر مستقل حتى تبقى واضحة")
        Spacer(Modifier.height(14.dp))
        ZteMoreLine("الموديل", snapshot.model ?: "غير معروف")
        Spacer(Modifier.height(9.dp))
        ZteMoreLine("اسم الشبكة", zteOperator(snapshot))
        Spacer(Modifier.height(9.dp))
        ZteMoreLine("نوع الاتصال", zteNetworkLabel(snapshot))
        Spacer(Modifier.height(9.dp))
        ZteMoreLine("مدة التشغيل", zteDuration(traffic?.sessionSeconds))
        Spacer(Modifier.height(9.dp))
        ZteMoreLine("Firmware", snapshot.firmware ?: "غير متاح")
        Spacer(Modifier.height(9.dp))
        ZteMoreLine("Hardware", snapshot.hardwareVersion ?: "غير متاح")
    }
}

@Composable
private fun ZteConnectedDevicesCard(devices: List<ConnectedDevice>) {
    ZteCard {
        ZteSectionHeader("الأجهزة المتصلة", if (devices.isEmpty()) "لا توجد قائمة أجهزة في آخر قراءة" else "${devices.size} جهاز حسب آخر قراءة")
        Spacer(Modifier.height(14.dp))
        if (devices.isEmpty()) {
            Text(
                "الراوتر لم يرجع قائمة أجهزة في القراءة الحالية، لذلك لن نعرض أجهزة افتراضية.",
                color = ZteMuted,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        } else {
            devices.forEachIndexed { index, device ->
                ZteDeviceCard(device)
                if (index != devices.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ZteDeviceCard(device: ConnectedDevice) {
    Surface(shape = RoundedCornerShape(20.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = if (device.transport == DeviceTransport.WIFI) ZteSoftBlue else ZteSoftGreen) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Text(if (device.transport == DeviceTransport.WIFI) "Wi" else "LAN", color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(device.displayName, color = ZteInk, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(2.dp))
                    Text(if (device.transport == DeviceTransport.WIFI) "متصل عبر Wi‑Fi" else "متصل عبر الكيبل", color = ZteMuted, fontSize = 14.sp)
                }
            }
            device.ipAddress?.let {
                Spacer(Modifier.height(10.dp))
                Text("العنوان داخل الشبكة: $it", color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ZteDeviceControlTruthCard() {
    ZteCard {
        ZteSectionHeader("التحكم بالأجهزة")
        Spacer(Modifier.height(10.dp))
        Text(
            "الحظر وتحديد السرعة والجدولة لن تظهر كأزرار فعّالة إلا بعد إثبات أن Firmware الراوتر يدعم أوامرها ويمكن التحقق من النتيجة بعد التنفيذ.",
            color = ZteMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun ZteDisconnectCard(onDisconnect: () -> Unit) {
    ZteCard {
        ZteSectionHeader("الاتصال بالراوتر")
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ZteRed)
        ) {
            Text("قطع الاتصال", fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ZteMoreLine(label: String, value: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, color = ZteMuted, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = ZteInk, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}
