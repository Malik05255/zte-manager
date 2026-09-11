package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GlassBandControlCard(
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    controlBusy: Boolean,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit
) {
    GlassCard {
        Column(Modifier.padding(14.dp)) {
            Text("قفل الترددات", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text("لا نعتبر التردد نشطًا لمجرد أنه مسموح؛ التطبيق يتحقق بعد التنفيذ", color = GlassMuted, fontSize = 9.sp)
            Spacer(Modifier.height(10.dp))
            if (capabilities.supportsLteBandLock) {
                Text("الجيل الرابع", color = GlassInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    capabilities.supportedLteBands.sorted().forEach { band ->
                        GlassBandChip("B$band", band in selectedLte) { onLteToggle(band) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlassActionButton("طبّق ترددات 4G المختارة", selectedLte.isNotEmpty() && !controlBusy, onApplyLte)
            } else {
                Text("هذا الراوتر لا يعلن دعم قفل ترددات 4G", color = GlassMuted, fontSize = 10.sp)
            }

            if (capabilities.supportsNrBandLock) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = GlassLine)
                Spacer(Modifier.height(14.dp))
                Text("الجيل الخامس", color = GlassInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    capabilities.supportedNrBands.sorted().forEach { band ->
                        GlassBandChip("n$band", band in selectedNr) { onNrToggle(band) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlassActionButton("طبّق ترددات 5G المختارة", selectedNr.isNotEmpty() && !controlBusy, onApplyNr)
            }
        }
    }
}

@Composable
private fun GlassBandChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) GlassBlue else androidx.compose.ui.graphics.Color(0xFFF3F6FA),
        border = BorderStroke(1.dp, if (selected) GlassBlue else GlassLine)
    ) {
        Text(
            label,
            color = if (selected) androidx.compose.ui.graphics.Color.White else GlassInk,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        )
    }
}

@Composable
internal fun GlassTechnicalReadings(snapshot: RouterSnapshot) {
    var open by remember { mutableStateOf(false) }
    GlassCard {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth().clickable { open = !open }, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("التفاصيل الفنية", color = GlassInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("للمختصين فقط", color = GlassMuted, fontSize = 9.sp)
                }
                Text(if (open) "إخفاء" else "عرض", color = GlassBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (open) {
                Spacer(Modifier.height(10.dp))
                GlassTechnicalRow("قوة 4G", snapshot.lteRsrp?.let { "${it.roundToInt()} dBm" } ?: "غير متاحة")
                GlassTechnicalRow("نظافة 4G", snapshot.lteSinr?.let { "${it.roundToInt()} dB" } ?: "غير متاحة")
                GlassTechnicalRow("قوة 5G", snapshot.nrRsrp?.let { "${it.roundToInt()} dBm" } ?: "غير متاحة")
                GlassTechnicalRow("نظافة 5G", snapshot.nrSinr?.let { "${it.roundToInt()} dB" } ?: "غير متاحة")
                GlassTechnicalRow("PCI", snapshot.pci?.toString() ?: "غير متاح")
                GlassTechnicalRow("EARFCN", snapshot.earfcn?.toString() ?: "غير متاح")
            }
        }
    }
}
