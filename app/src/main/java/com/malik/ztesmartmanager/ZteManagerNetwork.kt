package com.malik.ztesmartmanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun ZteLiveNetworkSummary(snapshot: RouterSnapshot) {
    ZteCard {
        ZteSectionHeader("الشبكة الآن", "ملخص مباشر من الراوتر بلغة مفهومة")
        Spacer(Modifier.height(14.dp))
        Text(zteNetworkLabel(snapshot), color = ZteBlue, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(zteOperator(snapshot), color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Text(
            if (snapshot.caActive) "الراوتر يدمج ترددات الآن" else "لا يوجد دمج ترددات مؤكد الآن",
            color = if (snapshot.caActive) ZteGreen else ZteMuted,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = if (snapshot.caActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ZteAllNetworkModes(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    ZteCard {
        ZteSectionHeader("اختيار الجيل", "كل وضع في سطر مستقل حتى يبقى النص واضحًا")
        Spacer(Modifier.height(14.dp))
        ZteNetworkModeRow("تلقائي", "الراوتر يختار الأنسب تلقائيًا", false, !busy) { onMode("WL_AND_5G") }
        Spacer(Modifier.height(9.dp))
        ZteNetworkModeRow("5G + 4G", "يفضل الجيل الخامس مع الاستفادة من الرابع", zteIsFiveG(snapshot), !busy) { onMode("LTE_AND_5G") }
        Spacer(Modifier.height(9.dp))
        ZteNetworkModeRow("5G فقط", "استخدمه إذا كان الجيل الخامس ثابتًا في موقعك", snapshot.networkType.orEmpty().contains("Only_5G", true), !busy) { onMode("Only_5G") }
        Spacer(Modifier.height(9.dp))
        ZteNetworkModeRow("4G فقط", "يثبت الاتصال على الجيل الرابع", zteNetworkLabel(snapshot).contains("4G"), !busy) { onMode("Only_LTE") }
        Spacer(Modifier.height(9.dp))
        ZteNetworkModeRow("3G فقط", "خيار احتياطي عند الحاجة", snapshot.networkType.orEmpty().contains("WCDMA", true), !busy) { onMode("Only_WCDMA") }
        Spacer(Modifier.height(9.dp))
        ZteNetworkModeRow("2G فقط", "للحالات الخاصة فقط", snapshot.networkType.orEmpty().contains("GSM", true), !busy) { onMode("Only_GSM") }
    }
}

@Composable
private fun ZteNetworkModeRow(title: String, subtitle: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) ZteSoftBlue else ZteSurfaceAlt,
        border = BorderStroke(1.dp, if (selected) ZteBlue.copy(alpha = 0.45f) else ZteLine)
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = if (enabled) onClick else null)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp)
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
    var showLte by rememberSaveable { mutableStateOf(false) }
    var showNr by rememberSaveable { mutableStateOf(false) }

    ZteCard {
        ZteSectionHeader("قفل الترددات", "افتح القسم الذي تحتاجه فقط؛ الباقي يبقى مخفيًا")
        Spacer(Modifier.height(14.dp))

        ZteExpandableHeader("ترددات 4G", if (showLte) "إخفاء" else "فتح") { showLte = !showLte }
        AnimatedVisibility(showLte) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                if (capabilities.supportsLteBandLock && capabilities.supportedLteBands.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        capabilities.supportedLteBands.sorted().forEach { band ->
                            ZteBandChip("B$band", band in selectedLte, !controlBusy) { onLteToggle(band) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    ZtePrimaryButton("طبّق ترددات 4G المختارة", selectedLte.isNotEmpty() && !controlBusy, onApplyLte)
                } else {
                    ZteUnavailableText("هذا الراوتر لا يعلن دعمًا موثوقًا لقفل ترددات 4G.")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = ZteLine)
        Spacer(Modifier.height(12.dp))

        ZteExpandableHeader("ترددات 5G", if (showNr) "إخفاء" else "فتح") { showNr = !showNr }
        AnimatedVisibility(showNr) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                if (capabilities.supportsNrBandLock && capabilities.supportedNrBands.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        capabilities.supportedNrBands.sorted().forEach { band ->
                            ZteBandChip("n$band", band in selectedNr, !controlBusy) { onNrToggle(band) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    ZtePrimaryButton("طبّق ترددات 5G المختارة", selectedNr.isNotEmpty() && !controlBusy, onApplyNr)
                } else {
                    ZteUnavailableText("قفل ترددات 5G لن يظهر كتحكم فعلي إلا إذا أثبت الـFirmware دعمه.")
                }
            }
        }
    }
}

@Composable
private fun ZteExpandableHeader(title: String, action: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = ZteSurfaceAlt,
        border = BorderStroke(1.dp, ZteLine)
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(action, color = ZteBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ZteBandChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) ZteBlue else ZteSurfaceAlt,
        border = BorderStroke(1.dp, if (selected) ZteBlue else ZteLine)
    ) {
        Text(
            label,
            color = if (selected) Color.White else ZteInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 13.dp)
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
        ZteSectionHeader("البرج والخلية", "الخريطة أولًا، ثم أدوات التحكم بشكل منفصل")
        Spacer(Modifier.height(14.dp))
        ZteManagerMap(snapshot, Modifier.fillMaxWidth().height(320.dp))
        Spacer(Modifier.height(14.dp))

        ZteSecondaryButton(if (scanBusy) "جاري البحث عن الخلايا…" else "ابحث عن الخلايا القريبة", !scanBusy && !controlBusy, onScanCells)
        Spacer(Modifier.height(10.dp))
        if (capabilities.supportsCellLock) {
            ZtePrimaryButton("ثبّت الخلية الحالية", !controlBusy, onLockCurrentCell)
            Spacer(Modifier.height(10.dp))
            ZteSecondaryButton("إزالة التثبيت", !controlBusy, onClearCellLock)
        } else {
            ZteUnavailableText("تثبيت الخلية غير متاح لهذا الموديل أو الـFirmware حتى يتم التحقق من دعمه.")
        }

        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text("حارس الخلية", color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(
                    towerGuardStatus?.message ?: if (towerTarget != null) "يتحقق من بقاء الراوتر على الخلية التي ثبتها." else "ثبّت خلية موثقة أولًا ثم فعّل الحارس.",
                    color = ZteMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(12.dp))
                Switch(
                    checked = towerGuardEnabled,
                    onCheckedChange = onTowerGuardChange,
                    enabled = towerTarget != null && capabilities.supportsCellLock && !controlBusy
                )
            }
        }

        if (nearbyCells.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("الخلايا التي وجدها الراوتر", color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            nearbyCells.forEachIndexed { index, cell ->
                ZteNearbyCellCard(index + 1, cell, !controlBusy, onLockNearbyCell)
                if (index != nearbyCells.lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ZteNearbyCellCard(rank: Int, cell: NearbyCell, enabled: Boolean, onLock: (NearbyCell) -> Unit) {
    val lte = cell.rat.contains("LTE", true) || cell.rat.contains("4G", true)
    Surface(shape = RoundedCornerShape(20.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text("الخيار $rank", color = ZteBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(cell.rat, cell.band).joinToString(" • ").ifBlank { "خلية مرصودة" },
                color = ZteInk,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
            cell.rsrp?.let {
                Spacer(Modifier.height(4.dp))
                Text("قوة الإشارة ${zteMetricWord("RSRP", it)}", color = ZteMuted, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            ZteSecondaryButton(
                if (lte && cell.pci != null && cell.arfcn != null) "تثبيت هذه الخلية" else "التثبيت غير متاح",
                enabled && lte && cell.pci != null && cell.arfcn != null
            ) { onLock(cell) }
        }
    }
}

@Composable
private fun ZteTechnicalNetworkCard(snapshot: RouterSnapshot) {
    var open by rememberSaveable { mutableStateOf(false) }
    ZteCard {
        ZteSectionHeader("تفاصيل للمختصين", "مخفية افتراضيًا حتى لا تزاحم الشاشة")
        Spacer(Modifier.height(12.dp))
        ZteSecondaryButton(if (open) "إخفاء التفاصيل" else "عرض التفاصيل الفنية") { open = !open }
        AnimatedVisibility(open) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
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
    }
}

@Composable
private fun ZteTechnicalLine(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        color = ZteSurfaceAlt,
        border = BorderStroke(1.dp, ZteLine)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = ZteMuted, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ZteUnavailableText(message: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
        Text(message, color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(14.dp))
    }
}
