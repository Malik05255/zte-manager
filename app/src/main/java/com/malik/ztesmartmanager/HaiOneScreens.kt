package com.malik.ztesmartmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.ConnectedDevice
import com.malik.ztesmartmanager.core.model.DeviceTransport
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget
import kotlin.math.roundToInt

@Composable
internal fun HaiNetworkScreen(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    controlBusy: Boolean,
    onSetNetworkMode: (String) -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HaiCard {
                HaiSectionTitle("وضع الشبكة", haiNetworkLabel(snapshot))
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HaiModeWide("تلقائي", false, !controlBusy) { onSetNetworkMode("WL_AND_5G") }
                    HaiModeWide("5G + 4G", haiIs5G(snapshot), !controlBusy) { onSetNetworkMode("LTE_AND_5G") }
                    HaiModeWide("4G فقط", !haiIs5G(snapshot) && haiNetworkLabel(snapshot).contains("4G"), !controlBusy) { onSetNetworkMode("Only_LTE") }
                    HaiModeWide("3G فقط", snapshot.networkType.orEmpty().contains("WCDMA", true), !controlBusy) { onSetNetworkMode("Only_WCDMA") }
                }
            }
        }
        item {
            HaiBandSelector("ترددات 4G", capabilities.supportedLteBands.sorted(), selectedLte, capabilities.supportsLteBandLock && !controlBusy, onLteToggle, onApplyLte)
        }
        item {
            HaiBandSelector("ترددات 5G", capabilities.supportedNrBands.sorted(), selectedNr, capabilities.supportsNrBandLock && !controlBusy, onNrToggle, onApplyNr)
        }
    }
}

@Composable
private fun HaiModeWide(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) HaiSoftBlue else Color(0xFFF8FBFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) HaiBlue else HaiLine)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(if (selected) HaiBlue else HaiLine), contentAlignment = Alignment.Center) {
                if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, color = HaiInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HaiBandSelector(
    title: String,
    bands: List<Int>,
    selected: Set<Int>,
    enabled: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit
) {
    HaiCard {
        HaiSectionTitle(title, selected.size.takeIf { it > 0 }?.toString())
        Spacer(Modifier.height(13.dp))
        if (bands.isEmpty()) {
            Text("غير متاح", color = HaiMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        } else {
            val rows = bands.chunked(3)
            rows.forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    row.forEach { band ->
                        HaiChoice(
                            label = if (title.contains("5G")) "n$band" else "B$band",
                            selected = band in selected,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            onClick = { onToggle(band) }
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                if (rowIndex != rows.lastIndex) Spacer(Modifier.height(9.dp))
            }
            Spacer(Modifier.height(13.dp))
            HaiPrimaryButton("تطبيق", enabled && selected.isNotEmpty(), onClick = onApply)
        }
    }
}

@Composable
internal fun HaiTowersScreen(
    snapshot: RouterSnapshot,
    nearbyCells: List<NearbyCell>,
    scanBusy: Boolean,
    controlBusy: Boolean,
    towerTarget: TowerTarget?,
    towerGuardEnabled: Boolean,
    towerGuardStatus: TowerGuardStatus?,
    onScanCells: () -> Unit,
    onLockCurrentCell: () -> Unit,
    onLockNearbyCell: (NearbyCell) -> Unit,
    onClearCellLock: () -> Unit,
    onTowerGuardChange: (Boolean) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HaiCard(padding = PaddingValues(14.dp)) {
                HaiSectionTitle("الخريطة")
                Spacer(Modifier.height(12.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val ratio = if (maxWidth < 520.dp) 0.94f else 1.65f
                    HaiOneMap(snapshot, Modifier.fillMaxWidth().aspectRatio(ratio))
                }
            }
        }
        item {
            HaiCard {
                HaiSectionTitle("البرج الحالي", towerTarget?.band)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    HaiPrimaryButton(if (scanBusy) "جاري المسح" else "مسح", !scanBusy && !controlBusy, Modifier.weight(1f), onScanCells)
                    HaiGhostButton("تثبيت الحالي", !controlBusy, Modifier.weight(1f), onLockCurrentCell)
                }
                if (towerTarget != null) {
                    Spacer(Modifier.height(10.dp))
                    HaiGhostButton("إلغاء التثبيت", !controlBusy, onClick = onClearCellLock)
                }
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = if (towerGuardEnabled) HaiSoftGreen else Color(0xFFF8FBFF)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("حارس البرج", color = HaiInk, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Switch(checked = towerGuardEnabled, onCheckedChange = onTowerGuardChange, enabled = towerTarget != null && !controlBusy)
                    }
                }
                towerGuardStatus?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(if (it.repaired) "تمت الاستعادة" else if (towerGuardEnabled) "الحماية فعالة" else "الحماية متوقفة", color = if (it.repaired || towerGuardEnabled) HaiGreen else HaiMuted, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        if (nearbyCells.isNotEmpty()) {
            item { HaiSectionTitle("الخلايا", nearbyCells.size.toString()) }
            items(nearbyCells.size) { index ->
                HaiNearbyCell(nearbyCells[index], !controlBusy) { onLockNearbyCell(nearbyCells[index]) }
            }
        }
    }
}

@Composable
private fun HaiNearbyCell(cell: NearbyCell, enabled: Boolean, onLock: () -> Unit) {
    val strength = haiMetricWord("RSRP", cell.rsrp)
    HaiCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(if (cell.rat == "LTE") HaiSoftBlue else HaiSoftPurple), contentAlignment = Alignment.Center) {
                Text(cell.band ?: cell.rat, color = if (cell.rat == "LTE") HaiBlue else HaiPurple, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(strength, color = HaiInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(listOfNotNull(cell.rsrp?.let { "${it.roundToInt()} dBm" }, cell.evidenceScore?.let { "$it%" }).joinToString(" • ").ifBlank { "—" }, color = HaiMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (cell.rat == "LTE" && cell.pci != null && cell.arfcn != null) {
            Spacer(Modifier.height(12.dp))
            HaiGhostButton("تثبيت", enabled, onClick = onLock)
        }
    }
}

@Composable
internal fun HaiDevicesScreen(devices: List<ConnectedDevice>) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HaiCard { HaiSectionTitle("الأجهزة المتصلة", devices.size.toString()) } }
        if (devices.isEmpty()) {
            item { HaiCard { Text("لا توجد أجهزة ظاهرة", color = HaiMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
        } else {
            items(devices.size) { index ->
                val device = devices[index]
                HaiCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(54.dp).clip(CircleShape).background(if (device.transport == DeviceTransport.LAN) HaiSoftPurple else HaiSoftBlue), contentAlignment = Alignment.Center) {
                            Text(if (device.transport == DeviceTransport.LAN) "LAN" else "Wi", color = if (device.transport == DeviceTransport.LAN) HaiPurple else HaiBlue, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(device.displayName, color = HaiInk, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            device.ipAddress?.let { Text(it, color = HaiMuted, fontSize = 14.sp, maxLines = 1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HaiToolsScreen(
    snapshot: RouterSnapshot,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    safetyBackupAvailable: Boolean,
    controlBusy: Boolean,
    supportsAntennaControl: Boolean,
    onPlacementToggle: () -> Unit,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onRestoreSafetyBackup: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onShareDiagnostics: () -> Unit,
    onDisconnect: () -> Unit
) {
    val placement = haiPlacementState(placementReading)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HaiCard {
                HaiSectionTitle("المكان", placement.first)
                Spacer(Modifier.height(12.dp))
                HaiPrimaryButton(if (placementMode) "إيقاف الجولة" else "ابدأ الجولة", onClick = onPlacementToggle)
            }
        }
        item {
            HaiCard {
                HaiSectionTitle("التحسين", if (smartMode) "تلقائي" else "يدوي")
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    HaiGoalRow("متوازن", smartGoal == OptimizationGoal.BALANCED) { onSmartGoalChange(OptimizationGoal.BALANCED) }
                    HaiGoalRow("سرعة", smartGoal == OptimizationGoal.SPEED) { onSmartGoalChange(OptimizationGoal.SPEED) }
                    HaiGoalRow("ألعاب", smartGoal == OptimizationGoal.GAMING) { onSmartGoalChange(OptimizationGoal.GAMING) }
                    HaiGoalRow("ثبات", smartGoal == OptimizationGoal.STABILITY) { onSmartGoalChange(OptimizationGoal.STABILITY) }
                }
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF8FBFF)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("تحسين تلقائي", color = HaiInk, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Switch(checked = smartMode, onCheckedChange = onSmartModeChange, enabled = !smartBusy && !controlBusy)
                    }
                }
                Spacer(Modifier.height(12.dp))
                HaiPrimaryButton(if (smartBusy) "جاري التحسين" else "حسّن الآن", !smartBusy && !controlBusy, onClick = onOptimizeNow)
            }
        }
        if (supportsAntennaControl) {
            item {
                HaiCard {
                    HaiSectionTitle("الهوائي")
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        HaiChoice("تلقائي", false, !controlBusy, Modifier.weight(1f)) { onAntennaState(0) }
                        HaiChoice("خارجي", false, !controlBusy, Modifier.weight(1f)) { onAntennaState(1) }
                    }
                }
            }
        }
        item {
            HaiCard {
                HaiSectionTitle("الأمان")
                Spacer(Modifier.height(12.dp))
                HaiGhostButton("استعادة النسخة", safetyBackupAvailable && !controlBusy, onClick = onRestoreSafetyBackup)
            }
        }
        item {
            HaiCard {
                HaiSectionTitle("التشخيص")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    HaiGhostButton("نسخ", modifier = Modifier.weight(1f), onClick = onCopyDiagnostics)
                    HaiGhostButton("مشاركة", modifier = Modifier.weight(1f), onClick = onShareDiagnostics)
                }
            }
        }
        item {
            HaiCard {
                HaiSectionTitle("الجهاز", snapshot.model ?: "ZTE")
                Spacer(Modifier.height(12.dp))
                HaiGhostButton("قطع الاتصال", onClick = onDisconnect)
            }
        }
    }
}

@Composable
private fun HaiGoalRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) HaiSoftBlue else Color(0xFFF8FBFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) HaiBlue else HaiLine)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(if (selected) HaiBlue else HaiLine))
            Spacer(Modifier.width(11.dp))
            Text(label, color = HaiInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}
