package com.malik.ztesmartmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget

@Composable
internal fun V6Network(snapshot: RouterSnapshot, busy: Boolean, onSetNetworkMode: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        V6Card {
            Column(Modifier.padding(16.dp)) {
                Text("حالة الشبكة", color = V6Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V6Value("الشبكة", v6Network(snapshot), Modifier.weight(1f))
                    V6Value("RSRP", v6Rsrp(snapshot)?.let { "${v6Fmt(it)} dBm" } ?: "—", Modifier.weight(1f))
                    V6Value("RSRQ", snapshot.lteRsrq?.let { "${v6Fmt(it)} dB" } ?: "—", Modifier.weight(1f))
                }
            }
        }
        V6Card {
            Column(Modifier.padding(16.dp)) {
                Text("أوضاع الشبكة", color = V6Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "تلقائي" to "WL_AND_5G",
                    "2G فقط" to "Only_GSM",
                    "3G فقط" to "Only_WCDMA",
                    "4G فقط" to "Only_LTE",
                    "5G فقط" to "Only_5G"
                ).forEach { (label, mode) -> V6Action(label, !busy) { onSetNetworkMode(mode) } }
            }
        }
    }
}

@Composable
internal fun V6Towers(
    snapshot: RouterSnapshot,
    cells: List<NearbyCell>,
    busy: Boolean,
    scanBusy: Boolean,
    target: TowerTarget?,
    guardEnabled: Boolean,
    guardStatus: TowerGuardStatus?,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onLockCell: (NearbyCell) -> Unit,
    onClear: () -> Unit,
    onGuardChange: (Boolean) -> Unit
) {
    V6Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الأبراج والسجلات", color = V6Ink, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                V6Chip(if (scanBusy) "جاري المسح…" else "مسح", !scanBusy && !busy, onScan)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V6Value("PCI", snapshot.pci?.toString() ?: "—", Modifier.weight(1f))
                V6Value("EARFCN", snapshot.earfcn?.toString() ?: "—", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V6Primary("تثبيت الحالية", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
                V6Outline("إزالة القفل", !busy, Modifier.weight(1f), onClear)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حارس البرج", color = V6Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    target?.let { Text("PCI ${it.pci} • EARFCN ${it.earfcn}", color = V6Muted, fontSize = 11.sp) }
                    guardStatus?.let { Text(it.message, color = V6Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Switch(checked = guardEnabled, onCheckedChange = onGuardChange, enabled = target != null && !busy,
                    colors = SwitchDefaults.colors(checkedTrackColor = V6Green))
            }
            cells.take(10).forEach { cell ->
                val current = cell.rat == "LTE" && cell.pci == snapshot.pci && cell.arfcn == snapshot.earfcn
                Row(Modifier.fillMaxWidth().padding(top = 7.dp).clip(RoundedCornerShape(13.dp)).background(if (current) V6SoftGreen else Color(0xFFF4F6FA)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = V6Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("ARFCN ${cell.arfcn ?: "—"}", color = V6Muted, fontSize = 10.sp)
                    }
                    if (!current && cell.rat == "LTE" && cell.pci != null && cell.arfcn != null) V6Chip("تثبيت", !busy) { onLockCell(cell) }
                    else if (current) Text("الحالية", color = V6Green, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
internal fun V6Bands(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    busy: Boolean,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit
) {
    var nr by rememberSaveable { mutableStateOf(false) }
    val bands = if (nr) capabilities.supportedNrBands.sorted() else capabilities.supportedLteBands.sorted()
    val selected = if (nr) selectedNr else selectedLte
    val active = remember(snapshot.cells, nr) {
        snapshot.cells.filter { if (nr) it.role == CellRole.NR else it.role != CellRole.NR }.mapNotNull { v6BandNumber(it.band) }.toSet()
    }
    V6Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("الترددات", color = V6Ink, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                V6Chip("4G", !nr) { nr = false }
                Spacer(Modifier.width(6.dp))
                V6Chip("5G", nr) { nr = true }
            }
            Spacer(Modifier.height(12.dp))
            bands.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { band ->
                        val chosen = band in selected
                        val live = band in active
                        Box(
                            Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(13.dp))
                                .background(if (live) V6SoftGreen else if (chosen) V6SoftBlue else Color.White)
                                .border(1.dp, if (live) V6Green else if (chosen) V6Blue else V6Border, RoundedCornerShape(13.dp))
                                .clickable(enabled = !busy) { if (nr) onNrToggle(band) else onLteToggle(band) },
                            contentAlignment = Alignment.Center
                        ) { Text("${if (nr) "N" else "B"}$band", color = V6Ink, fontSize = 12.sp, fontWeight = FontWeight.Black) }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(7.dp))
            }
            V6Primary(if (nr) "تطبيق 5G" else "تطبيق 4G", selected.isNotEmpty() && !busy && (!nr || capabilities.supportsNrBandLock), Modifier.fillMaxWidth()) {
                if (nr) onApplyNr() else onApplyLte()
            }
        }
    }
}

@Composable
internal fun V6Tools(
    runtime: RuntimeCapabilityReport?,
    stability: ConnectionStabilityReport,
    samples: Int,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    backupAvailable: Boolean,
    supportsAntenna: Boolean,
    busy: Boolean,
    onPlacementToggle: () -> Unit,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onRestore: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        V6Card {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("التحسين الذكي", color = V6Ink, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Switch(checked = smartMode, onCheckedChange = onSmartModeChange, colors = SwitchDefaults.colors(checkedTrackColor = V6Green))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OptimizationGoal.entries.forEach { goal -> V6Choice(v6Goal(goal), smartGoal == goal, Modifier.weight(1f)) { onSmartGoalChange(goal) } }
                }
                Spacer(Modifier.height(8.dp))
                V6Primary(if (smartBusy) "جاري التحسين…" else "تحسين الآن", !smartBusy && !busy, Modifier.fillMaxWidth(), onOptimizeNow)
                smartReport?.let { Text("آخر نتيجة ${it.best.qualityScore}/100", color = V6Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)) }
            }
        }
        V6Card {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("أفضل مكان للراوتر", color = V6Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(placementReading?.let { "${it.score.total}/100" } ?: "لا توجد قراءة", color = V6Muted, fontSize = 11.sp)
                }
                Switch(checked = placementMode, onCheckedChange = { onPlacementToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = V6Green))
            }
        }
        V6Card {
            Column(Modifier.padding(16.dp)) {
                Text("التشخيص", color = V6Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("${if (runtime == null) 0 else 1} حالة تشغيل • $samples قراءات", color = V6Muted, fontSize = 11.sp)
                Text(stability.summary, color = V6Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V6Outline("نسخ التقرير", true, Modifier.weight(1f), onCopy)
                    V6Outline("مشاركة التقرير", true, Modifier.weight(1f), onShare)
                }
            }
        }
        V6Card { Column(Modifier.padding(16.dp)) { V6Primary("استعادة النسخة الآمنة", backupAvailable && !busy, Modifier.fillMaxWidth(), onRestore) } }
        if (supportsAntenna) V6Card {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { state -> V6Outline(state.toString(), !busy, Modifier.weight(1f)) { onAntennaState(state) } }
            }
        }
    }
}
