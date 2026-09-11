package com.malik.ztesmartmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget

@Composable
internal fun GlassTowerControlCard(
    scanBusy: Boolean,
    controlBusy: Boolean,
    towerTarget: TowerTarget?,
    guardEnabled: Boolean,
    guardStatus: TowerGuardStatus?,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onClear: () -> Unit,
    onGuardChange: (Boolean) -> Unit
) {
    GlassCard {
        Column(Modifier.padding(14.dp)) {
            Text("البرج والخلية", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(
                if (towerTarget == null) "الراوتر يختار الخلية تلقائيًا" else "الخلية الحالية مثبتة وتم التحقق من الأمر",
                color = GlassMuted,
                fontSize = 9.sp
            )
            guardStatus?.message?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = GlassInk, fontSize = 9.sp, lineHeight = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            GlassActionButton(if (scanBusy) "جاري البحث…" else "ابحث عن الخلايا القريبة", !scanBusy, onScan)
            Spacer(Modifier.height(7.dp))
            if (towerTarget == null) {
                OutlinedButton(
                    onClick = onLockCurrent,
                    enabled = !controlBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                ) { Text("ثبّت الخلية الحالية", fontSize = 10.sp) }
            } else {
                OutlinedButton(
                    onClick = onClear,
                    enabled = !controlBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                ) { Text("إلغاء التثبيت", fontSize = 10.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حماية التثبيت", color = GlassInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("تعيد التحقق من الخلية بدل الثقة في رد الأمر وحده", color = GlassMuted, fontSize = 8.sp)
                }
                Switch(
                    checked = guardEnabled,
                    onCheckedChange = onGuardChange,
                    enabled = towerTarget != null && !controlBusy
                )
            }
        }
    }
}

@Composable
internal fun GlassNearbyCellCard(cell: NearbyCell, controlBusy: Boolean, onLock: () -> Unit) {
    GlassCard {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(glassNearbyTitle(cell), color = GlassInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
                val band = cell.band?.let { "تردد $it" } ?: "التردد غير ظاهر"
                val confidence = when (cell.confidence?.name) {
                    "HIGH" -> "دليل قوي"
                    "MEDIUM" -> "دليل متوسط"
                    "LOW" -> "دليل محدود"
                    else -> "الدليل غير مكتمل"
                }
                Text("$band • $confidence", color = GlassMuted, fontSize = 9.sp)
            }
            Text(
                "تثبيت",
                color = GlassBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !controlBusy, onClick = onLock)
            )
        }
    }
}
