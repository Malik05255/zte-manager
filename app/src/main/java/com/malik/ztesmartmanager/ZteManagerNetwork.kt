package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerMatch
import com.malik.ztesmartmanager.core.tower.TowerTarget
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ZteManagerNetwork(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    controlBusy: Boolean,
    scanBusy: Boolean,
    nearbyCells: List<NearbyCell>,
    towerTarget: TowerTarget?,
    towerGuardEnabled: Boolean,
    towerGuardStatus: TowerGuardStatus?,
    onSetNetworkMode: (String) -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    onScanCells: () -> Unit,
    onLockCurrentCell: () -> Unit,
    onLockNearbyCell: (NearbyCell) -> Unit,
    onClearCellLock: () -> Unit,
    onTowerGuardChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ZteLiveNetworkSummary(snapshot) }
        item { ZteAllNetworkModes(snapshot, controlBusy, onSetNetworkMode) }
        item {
            ZteBandLockCard(
                capabilities, selectedLte, selectedNr, controlBusy,
                onLteToggle, onNrToggle, onApplyLte, onApplyNr
            )
        }
        item {
            ZteTowerControlCard(
                snapshot, capabilities, scanBusy, controlBusy, nearbyCells, towerTarget,
                towerGuardEnabled, towerGuardStatus, onScanCells, onLockCurrentCell,
                onLockNearbyCell, onClearCellLock, onTowerGuardChange
            )
        }
        item { ZteTechnicalNetworkCard(snapshot) }
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = ZteSoftBlue) {
                Text(
                    "أي تغيير في وضع الشبكة أو الترددات لا يُعتبر ناجحًا بمجرد قبول الأمر. التطبيق يعتمد read-back من الراوتر، ويوقف الوظيفة إذا لم يكن مسار الرجوع الآمن متاحًا.",
                    color = ZteInk,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(15.dp)
                )
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun ZteLiveNetworkSummary(snapshot: RouterSnapshot) {
    ZteCard {
        ZteSectionHeader("الشبكة الحالية", "قراءة حيّة من الراوتر")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(20.dp), color = ZteSoftBlue) {
                Text(
                    zteNetworkLabel(snapshot),
                    color = ZteBlue,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(zteOperator(snapshot), color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    if (snapshot.caActive) "دمج ترددات مؤكد الآن" else "لا يوجد دمج مؤكد الآن",
                    color = if (snapshot.caActive) ZteGreen else ZteMuted,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ZteInfoTile("RSRP", zteMetricValue(snapshot.nrRsrp ?: snapshot.lteRsrp, "dBm"), Modifier.weight(1f))
            ZteInfoTile("SINR", zteMetricValue(snapshot.nrSinr ?: snapshot.lteSinr, "dB"), Modifier.weight(1f))
            ZteInfoTile("RSRQ", zteMetricValue(snapshot.lteRsrq, "dB"), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ZteAllNetworkModes(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    ZteCard {
        ZteSectionHeader("وضع الشبكة", "اختر الوضع المطلوب؛ التنفيذ يخضع للتحقق من الراوتر")
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZteChoiceChip("تلقائي", false, !busy, Modifier.weight(1f)) { onMode("WL_AND_5G") }
                ZteChoiceChip("5G + 4G", zteIsFiveG(snapshot), !busy, Modifier.weight(1f)) { onMode("LTE_AND_5G") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZteChoiceChip("5G فقط", snapshot.networkType.orEmpty().contains("Only_5G", true), !busy, Modifier.weight(1f)) { onMode("Only_5G") }
                ZteChoiceChip("4G فقط", zteNetworkLabel(snapshot).contains("4G"), !busy, Modifier.weight(1f)) { onMode("Only_LTE") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZteChoiceChip("3G فقط", snapshot.networkType.orEmpty().contains("WCDMA", true), !busy, Modifier.weight(1f)) { onMode("Only_WCDMA") }
                ZteChoiceChip("2G فقط", snapshot.networkType.orEmpty().contains("GSM", true), !busy, Modifier.weight(1f)) { onMode("Only_GSM") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZteBandLockCard(
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    controlBusy: Boolean,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit
) {
    ZteCard {
        ZteSectionHeader("قفل الترددات", "اختر الترددات ثم طبّقها. الاختيار لا يعني أنها أصبحت نشطة.")
        Spacer(Modifier.height(14.dp))

        Text("ترددات 4G", color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(9.dp))
        if (capabilities.supportsLteBandLock && capabilities.supportedLteBands.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                capabilities.supportedLteBands.sorted().forEach { band ->
                    ZteBandChip("B$band", band in selectedLte, !controlBusy) { onLteToggle(band) }
                }
            }
            Spacer(Modifier.height(12.dp))
            ZtePrimaryButton("طبّق ترددات 4G المختارة", selectedLte.isNotEmpty() && !controlBusy, onApplyLte)
        } else {
            ZteUnavailableText("هذا الراوتر لا يعلن دعمًا موثوقًا لقفل ترددات 4G.")
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = ZteLine)
        Spacer(Modifier.height(16.dp))

        Text("ترددات 5G", color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(9.dp))
        if (capabilities.supportsNrBandLock && capabilities.supportedNrBands.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                capabilities.supportedNrBands.sorted().forEach { band ->
                    ZteBandChip("n$band", band in selectedNr, !controlBusy) { onNrToggle(band) }
                }
            }
            Spacer(Modifier.height(12.dp))
            ZtePrimaryButton("طبّق ترددات 5G المختارة", selectedNr.isNotEmpty() && !controlBusy, onApplyNr)
        } else {
            ZteUnavailableText("قفل ترددات 5G غير مفعّل ما لم يثبت الـFirmware دعمه.")
        }
    }
}

@Composable
private fun ZteBandChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) ZteBlue else Color(0xFFF5F8FD),
        border = BorderStroke(1.dp, if (selected) ZteBlue else ZteLine)
    ) {
        Text(
            label,
            color = if (selected) Color.White else ZteInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp)
        )
    }
}

@Composable
private fun ZteTowerControlCard(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    scanBusy: Boolean,
    controlBusy: Boolean,
    nearbyCells: List<NearbyCell>,
    towerTarget: TowerTarget?,
    towerGuardEnabled: Boolean,
    towerGuardStatus: TowerGuardStatus?,
    onScanCells: () -> Unit,
    onLockCurrentCell: () -> Unit,
    onLockNearbyCell: (NearbyCell) -> Unit,
    onClearCellLock: () -> Unit,
    onTowerGuardChange: (Boolean) -> Unit
) {
    ZteCard {
        ZteSectionHeader("البرج والخلية", "التثبيت يعتمد PCI + EARFCN مع read-back")
        Spacer(Modifier.height(12.dp))
        ZteManagerMap(snapshot, Modifier.fillMaxWidth().height(300.dp))
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZteSecondaryButton(if (scanBusy) "جاري المسح…" else "مسح الخلايا", !scanBusy && !controlBusy, onScanCells)
        }
        Spacer(Modifier.height(9.dp))
        if (capabilities.supportsCellLock) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { ZtePrimaryButton("ثبّت الخلية الحالية", !controlBusy, onLockCurrentCell) }
                Box(Modifier.weight(1f)) { ZteSecondaryButton("إزالة التثبيت", !controlBusy, onClearCellLock) }
            }
        } else {
            ZteUnavailableText("تثبيت الخلية غير متاح لهذا الموديل/الـFirmware حتى يتم التحقق من دعمه.")
        }

        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFF6F9FE), border = BorderStroke(1.dp, ZteLine)) {
            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tower Guard", color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(
                        towerGuardStatus?.message ?: if (towerTarget != null) "يتحقق من بقاء الراوتر على الخلية المثبتة" else "فعّله بعد تثبيت خلية موثقة",
                        color = ZteMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                Switch(
                    checked = towerGuardEnabled,
                    onCheckedChange = onTowerGuardChange,
                    enabled = towerTarget != null && capabilities.supportsCellLock && !controlBusy
                )
            }
        }

        if (nearbyCells.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("الخلايا المرصودة فعليًا", color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(9.dp))
            nearbyCells.forEachIndexed { index, cell ->
                ZteNearbyCellRow(index + 1, cell, !controlBusy, onLockNearbyCell)
                if (index != nearbyCells.lastIndex) HorizontalDivider(color = ZteLine)
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text("لم يتم مسح خلايا قريبة بعد، أو أن الـFirmware لم يعرض هوية قابلة للتثبيت.", color = ZteMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ZteNearbyCellRow(rank: Int, cell: NearbyCell, enabled: Boolean, onLock: (NearbyCell) -> Unit) {
    val lte = cell.rat.contains("LTE", true) || cell.rat.contains("4G", true)
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(14.dp), color = ZteSoftBlue) {
            Text("#$rank", color = ZteBlue, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(cell.rat, cell.band, cell.pci?.let { "PCI $it" }).joinToString(" • "),
                color = ZteInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                listOfNotNull(cell.arfcn?.let { "ARFCN $it" }, cell.rsrp?.let { "${it.roundToInt()} dBm" }, cell.evidenceScore?.let { "ثقة $it%" }).joinToString(" • "),
                color = ZteMuted,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            modifier = Modifier.clickable(enabled = enabled && lte && cell.pci != null && cell.arfcn != null) { onLock(cell) },
            shape = RoundedCornerShape(14.dp),
            color = if (lte) ZteSoftBlue else Color(0xFFF2F4F7)
        ) {
            Text(
                if (lte) "تثبيت" else "غير متاح",
                color = if (lte) ZteBlue else ZteMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun ZteTechnicalNetworkCard(snapshot: RouterSnapshot) {
    ZteCard {
        ZteSectionHeader("التفاصيل الفنية", "قراءات مباشرة للمستخدم المتقدم")
        Spacer(Modifier.height(10.dp))
        ZteTechnicalLine("الموديل", snapshot.model ?: "—")
        ZteTechnicalLine("Firmware", snapshot.firmware ?: "—")
        ZteTechnicalLine("PCI", snapshot.pci?.toString() ?: "—")
        ZteTechnicalLine("EARFCN", snapshot.earfcn?.toString() ?: "—")
        ZteTechnicalLine("Cell ID", snapshot.cellId?.toString() ?: "—")
        ZteTechnicalLine("4G RSRP", zteMetricValue(snapshot.lteRsrp, "dBm"))
        ZteTechnicalLine("4G SINR", zteMetricValue(snapshot.lteSinr, "dB"))
        ZteTechnicalLine("5G RSRP", zteMetricValue(snapshot.nrRsrp, "dBm"))
        ZteTechnicalLine("5G SINR", zteMetricValue(snapshot.nrSinr, "dB"))
    }
}

@Composable
private fun ZteTechnicalLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ZteMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
private fun ZteUnavailableText(message: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF5F7FA)) {
        Text(message, color = ZteMuted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(13.dp))
    }
}
