package com.malik.ztesmartmanager

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementGuidance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerMatch
import com.malik.ztesmartmanager.core.tower.TowerTarget
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val FinalBg = Color(0xFFF0ECE4)
private val FinalIvory = Color(0xFFF7F2E9)
private val FinalIvoryDeep = Color(0xFFE4DDD1)
private val FinalGold = Color(0xFFD2A84D)
private val FinalGoldBright = Color(0xFFFFD96E)
private val FinalGoldDeep = Color(0xFF876126)
private val FinalInk = Color(0xFF2A241E)
private val FinalMuted = Color(0xFF776E63)
private val FinalLine = Color(0xFFD2C8B9)
private val FinalGood = Color(0xFF567D5B)
private val FinalWarn = Color(0xFFA96432)

private enum class FinalPanel { NONE, BANDS, TOWERS, NETWORK, SAFETY, PLACE, DIAGNOSTICS }

@Composable
fun FinalDashboard(
    snapshot: RouterSnapshot?,
    capabilities: RouterCapabilities,
    status: String,
    operationMessage: String,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    controlBusy: Boolean,
    speedBusy: Boolean,
    lastPerformance: NetworkPerformance?,
    safetyBackupAvailable: Boolean,
    nearbyCells: List<NearbyCell>,
    scanBusy: Boolean,
    towerTarget: TowerTarget?,
    towerGuardEnabled: Boolean,
    towerGuardStatus: TowerGuardStatus?,
    onDisconnect: () -> Unit,
    onSpeedTest: () -> Unit,
    onPlacementToggle: () -> Unit,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onAntennaState: (Int) -> Unit,
    onScanCells: () -> Unit,
    onLockCurrentCell: () -> Unit,
    onLockNearbyCell: (NearbyCell) -> Unit,
    onClearCellLock: () -> Unit,
    onTowerGuardChange: (Boolean) -> Unit,
    onRestoreSafetyBackup: () -> Unit
) {
    if (placementMode && placementReading != null) FinalPlacementAudio(placementReading)

    var panel by rememberSaveable { mutableStateOf(FinalPanel.NONE) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FinalBg)
    ) {
        val tiny = maxWidth < 350.dp
        val compact = maxWidth < 400.dp
        val pagePadding = if (compact) 10.dp else 15.dp
        val gap = if (compact) 9.dp else 12.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = pagePadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            item { FinalHeader(status, compact, onDisconnect) }

            val data = snapshot
            if (data == null) {
                item {
                    FinalCard(Modifier.fillMaxWidth()) {
                        Text("جاري قراءة الراوتر والتحقق من الاتصال...", Modifier.padding(24.dp), color = FinalMuted)
                    }
                }
            } else {
                item {
                    if (tiny) {
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            FinalNetworkCard(data, Modifier.fillMaxWidth(), compact)
                            FinalSpeedCard(lastPerformance, speedBusy, onSpeedTest, Modifier.fillMaxWidth(), compact)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            FinalNetworkCard(data, Modifier.weight(1.08f), compact)
                            FinalSpeedCard(lastPerformance, speedBusy, onSpeedTest, Modifier.weight(0.92f), compact)
                        }
                    }
                }

                item {
                    FinalFiveGPrimaryCard(
                        snapshot = data,
                        capabilities = capabilities,
                        selectedNr = selectedNr,
                        busy = controlBusy,
                        compact = compact,
                        onNrToggle = onNrToggle,
                        onApplyNr = onApplyNr,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    if (tiny) {
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            FinalSmartCard(
                                data, smartMode, smartGoal, smartBusy, smartReport,
                                onSmartModeChange, onSmartGoalChange, onOptimizeNow,
                                Modifier.fillMaxWidth(), compact
                            )
                            FinalRadioMapCard(data, nearbyCells, scanBusy, onScanCells, Modifier.fillMaxWidth(), compact)
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FinalSmartCard(
                                data, smartMode, smartGoal, smartBusy, smartReport,
                                onSmartModeChange, onSmartGoalChange, onOptimizeNow,
                                Modifier.weight(1.16f), compact
                            )
                            FinalRadioMapCard(data, nearbyCells, scanBusy, onScanCells, Modifier.weight(0.84f), compact)
                        }
                    }
                }

                if (operationMessage.isNotBlank()) item { FinalStatusStrip(operationMessage) }

                item {
                    FinalControlDock(
                        panel = panel,
                        onBands = { panel = panel.toggle(FinalPanel.BANDS) },
                        onOptimize = onOptimizeNow,
                        onTowers = { panel = panel.toggle(FinalPanel.TOWERS) },
                        onSafety = { panel = panel.toggle(FinalPanel.SAFETY) },
                        onDiagnostics = { panel = panel.toggle(FinalPanel.DIAGNOSTICS) }
                    )
                }

                when (panel) {
                    FinalPanel.BANDS -> {
                        item {
                            FinalBandCard(
                                title = "ترددات 4G LTE",
                                subtitle = "كل الترددات التي يعلن Profile جهازك دعمها. الاختيار وحده لا يُعرض كتردد نشط حتى يؤكده الراوتر.",
                                bands = capabilities.supportedLteBands,
                                selected = selectedLte,
                                prefix = "B",
                                busy = controlBusy,
                                compact = compact,
                                onToggle = onLteToggle,
                                onApply = onApplyLte
                            )
                        }
                        if (capabilities.supportsNrBandLock) {
                            item {
                                FinalBandCard(
                                    title = "ترددات 5G NR",
                                    subtitle = "السماح بـ N78 لا يعني أنك متصل به. بطاقة الشبكة تعرض 5G فقط عند وجود Carrier حي موثّق.",
                                    bands = capabilities.supportedNrBands,
                                    selected = selectedNr,
                                    prefix = "N",
                                    busy = controlBusy,
                                    compact = compact,
                                    onToggle = onNrToggle,
                                    onApply = onApplyNr
                                )
                            }
                        }
                    }

                    FinalPanel.TOWERS -> item {
                        FinalTowerPanel(
                            snapshot = data,
                            cells = nearbyCells,
                            scanBusy = scanBusy,
                            busy = controlBusy,
                            target = towerTarget,
                            guardEnabled = towerGuardEnabled,
                            guardStatus = towerGuardStatus,
                            onScan = onScanCells,
                            onLockCurrent = onLockCurrentCell,
                            onLockCell = onLockNearbyCell,
                            onClearLock = onClearCellLock,
                            onGuardChange = onTowerGuardChange
                        )
                    }

                    FinalPanel.NETWORK -> item {
                        FinalNetworkPanel(capabilities, controlBusy, onSetNetworkMode, onAntennaState)
                    }

                    FinalPanel.SAFETY -> item {
                        FinalSafetyPanel(safetyBackupAvailable, controlBusy, onRestoreSafetyBackup) {
                            panel = FinalPanel.NETWORK
                        }
                    }

                    FinalPanel.PLACE -> item {
                        FinalPlacementPanel(placementMode, placementReading, onPlacementToggle)
                    }

                    FinalPanel.DIAGNOSTICS -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            FinalQuickTools(
                                placementMode = placementMode,
                                onNetwork = { panel = FinalPanel.NETWORK },
                                onPlacement = { panel = FinalPanel.PLACE },
                                onTowers = { panel = FinalPanel.TOWERS }
                            )
                            FinalDiagnostics(data)
                        }
                    }

                    FinalPanel.NONE -> Unit
                }

                if (panel != FinalPanel.DIAGNOSTICS && panel != FinalPanel.NONE) {
                    item {
                        FinalQuickTools(
                            placementMode = placementMode,
                            onNetwork = { panel = FinalPanel.NETWORK },
                            onPlacement = { panel = FinalPanel.PLACE },
                            onTowers = { panel = FinalPanel.TOWERS }
                        )
                    }
                }

                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
}

private fun FinalPanel.toggle(target: FinalPanel): FinalPanel = if (this == target) FinalPanel.NONE else target

@Composable
private fun FinalHeader(status: String, compact: Boolean, onDisconnect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onDisconnect,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = if (compact) 12.dp else 16.dp, vertical = 5.dp)
        ) {
            Text("فصل", color = FinalInk, fontSize = if (compact) 10.sp else 11.sp)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "لوحة التحكم الشبكية",
                color = FinalInk,
                fontSize = if (compact) 20.sp else 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(status, color = FinalMuted, fontSize = if (compact) 8.sp else 9.sp, maxLines = 1)
        }
        Spacer(Modifier.size(if (compact) 54.dp else 64.dp))
    }
}

@Composable
private fun FinalNetworkCard(snapshot: RouterSnapshot, modifier: Modifier, compact: Boolean) {
    val verifiedNr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val label = snapshot.networkType ?: "غير مؤكد"
    val operator = finalOperator(snapshot)
    val rsrp = if (verifiedNr) snapshot.nrRsrp else snapshot.lteRsrp
    val sinr = if (verifiedNr) snapshot.nrSinr else snapshot.lteSinr
    val shape = RoundedCornerShape(topStart = 50.dp, topEnd = 28.dp, bottomEnd = 46.dp, bottomStart = 24.dp)

    Card(
        modifier = modifier.shadow(10.dp, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = FinalGoldDeep)
    ) {
        Column(Modifier.padding(if (compact) 12.dp else 16.dp)) {
            Text("Network Stat", color = Color.White.copy(alpha = 0.75f), fontSize = 7.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    color = FinalGoldBright,
                    fontSize = if (compact) 24.sp else 31.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Spacer(Modifier.size(4.dp))
                Text("⌁", color = FinalGoldBright, fontSize = 22.sp)
            }
            Text(operator, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (verifiedNr) "5G مثبت ببيانات Carrier حية" else finalLteStatus(snapshot),
                color = Color.White.copy(alpha = 0.75f), fontSize = 7.sp, maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FinalMetric("RSRP", rsrp, "dBm", Modifier.weight(1f))
                FinalMetric("SINR", sinr, "dB", Modifier.weight(1f))
                FinalMetric("RSRQ", snapshot.lteRsrq, "dB", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FinalMetric(label: String, value: Double?, unit: String, modifier: Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, FinalGoldBright.copy(alpha = 0.55f), CircleShape)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value?.let(::finalNumber) ?: "—", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(unit, color = Color.White.copy(alpha = 0.65f), fontSize = 6.sp)
            Text(label, color = Color.White.copy(alpha = 0.84f), fontSize = 6.sp)
        }
    }
}

@Composable
private fun FinalSpeedCard(
    performance: NetworkPerformance?,
    busy: Boolean,
    onSpeedTest: () -> Unit,
    modifier: Modifier,
    compact: Boolean
) {
    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 54.dp, bottomEnd = 27.dp, bottomStart = 47.dp)
    FinalCard(modifier, shape) {
        Column(
            Modifier.padding(if (compact) 12.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Speed Test", color = FinalInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("◆", color = FinalGold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 7.dp))
                Spacer(Modifier.size(3.dp))
                Text(
                    performance?.downloadMbps?.let(::finalNumber) ?: "—",
                    color = FinalGold,
                    fontSize = if (compact) 30.sp else 38.sp,
                    fontWeight = FontWeight.Black
                )
                Text(" Mb/s", color = FinalGoldDeep, fontSize = 8.sp, modifier = Modifier.padding(bottom = 7.dp))
            }
            if (performance != null) {
                Text(
                    "Ping ${performance.latencyMs?.let(::finalNumber) ?: "—"} ms • Jitter ${performance.jitterMs?.let(::finalNumber) ?: "—"}",
                    color = FinalMuted, fontSize = 7.sp, maxLines = 1
                )
            } else {
                Text("لا توجد نتيجة حتى تضغط قياس", color = FinalMuted, fontSize = 7.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSpeedTest,
                enabled = !busy,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = FinalIvoryDeep),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(if (busy) "جاري القياس" else "قياس السرعة", color = FinalInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FinalSmartCard(
    snapshot: RouterSnapshot,
    enabled: Boolean,
    goal: OptimizationGoal,
    busy: Boolean,
    report: SmartOptimizationReport?,
    onEnabledChange: (Boolean) -> Unit,
    onGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    modifier: Modifier,
    compact: Boolean
) {
    val score = remember(snapshot) { NetworkQualityEngine().score(snapshot).total }
    val shape = RoundedCornerShape(topStart = 58.dp, topEnd = 26.dp, bottomEnd = 55.dp, bottomStart = 34.dp)
    FinalCard(modifier, shape) {
        Column(Modifier.padding(if (compact) 12.dp else 15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حالة السرعة والتوصيل", color = FinalInk, fontSize = if (compact) 10.sp else 12.sp, fontWeight = FontWeight.Black)
                    Text(if (enabled) "المحرك الذكي يعمل" else "المحرك الذكي متوقف", color = FinalMuted, fontSize = 7.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = FinalGoldDeep, checkedThumbColor = Color.White)
                )
            }
            Spacer(Modifier.height(5.dp))
            Text("الإعداد النشط الموثّق", color = FinalMuted, fontSize = 7.sp)
            Text(finalBandCombo(snapshot), color = FinalInk, fontSize = if (compact) 18.sp else 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(finalAggregation(snapshot), color = FinalGoldDeep, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FinalMiniDial(score, Modifier.size(if (compact) 48.dp else 54.dp))
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("هدف التحسين", color = FinalMuted, fontSize = 6.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        OptimizationGoal.entries.forEach { item ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (goal == item) FinalGold.copy(alpha = 0.18f) else Color.Transparent)
                                    .border(1.dp, if (goal == item) FinalGold else FinalLine, RoundedCornerShape(12.dp))
                                    .clickable { onGoalChange(item) }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(finalGoal(item), color = FinalInk, fontSize = 6.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
            report?.let {
                Spacer(Modifier.height(4.dp))
                Text("آخر قرار: ${it.best.score}/100 • ${if (it.changed) "تغيير موثّق" else "الإعداد الأصلي"}", color = FinalMuted, fontSize = 7.sp)
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onOptimizeNow,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = FinalIvoryDeep),
                contentPadding = PaddingValues(vertical = 5.dp)
            ) {
                Text(if (busy) "جاري الاختبار..." else "تحسين الآن", color = FinalInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FinalRadioMapCard(
    snapshot: RouterSnapshot,
    cells: List<NearbyCell>,
    scanBusy: Boolean,
    onScan: () -> Unit,
    modifier: Modifier,
    compact: Boolean
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 58.dp, bottomEnd = 58.dp, bottomStart = 28.dp)
    FinalCard(modifier, shape) {
        Column(
            Modifier.padding(if (compact) 9.dp else 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Tower / Cell Scan", color = FinalInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("مخطط راديوي غير جغرافي", color = FinalMuted, fontSize = 6.sp)
            Box(
                Modifier.fillMaxWidth().height(if (compact) 110.dp else 124.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(if (compact) 102.dp else 114.dp)) {
                    val radius = size.minDimension * 0.46f
                    drawCircle(FinalLine.copy(alpha = 0.65f), radius, style = Stroke(2f))
                    drawCircle(FinalLine.copy(alpha = 0.55f), radius * 0.65f, style = Stroke(2f))
                    drawCircle(FinalGold.copy(alpha = 0.20f), radius * 0.28f, center = center)
                    drawCircle(FinalGoldDeep, 6f, center = center)

                    cells.take(6).forEachIndexed { index, cell ->
                        val angle = (2.0 * PI * index / cells.take(6).size.coerceAtLeast(1)) - PI / 2.0
                        val strength = ((cell.rsrp ?: -120.0) + 130.0).coerceIn(5.0, 55.0) / 55.0
                        val distance = radius * (0.82f - (strength.toFloat() * 0.22f))
                        val p = Offset(
                            center.x + cos(angle).toFloat() * distance,
                            center.y + sin(angle).toFloat() * distance
                        )
                        drawLine(FinalGoldDeep, Offset(p.x, p.y + 9f), Offset(p.x, p.y - 6f), 3f, StrokeCap.Round)
                        drawCircle(FinalGold, 5f, Offset(p.x, p.y - 9f))
                    }
                }
                Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PCI ${snapshot.pci ?: "—"} • ${cells.size} خلية", color = FinalInk, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(FinalIvoryDeep)
                    .clickable(enabled = !scanBusy, onClick = onScan)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(if (scanBusy) "مسح..." else "مسح الخلايا", color = FinalInk, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FinalControlDock(
    panel: FinalPanel,
    onBands: () -> Unit,
    onOptimize: () -> Unit,
    onTowers: () -> Unit,
    onSafety: () -> Unit,
    onDiagnostics: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 38.dp, topEnd = 20.dp, bottomEnd = 36.dp, bottomStart = 20.dp)
    FinalCard(Modifier.fillMaxWidth(), shape) {
        Column(Modifier.padding(10.dp)) {
            Text("Advanced Control", color = FinalInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FinalDockButton("قفل النطاقات", panel == FinalPanel.BANDS, Modifier.weight(1f), onBands)
                FinalDockButton("تحسين الشبكة", false, Modifier.weight(1f), onOptimize)
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                FinalTool("الأبراج", "⌁", panel == FinalPanel.TOWERS, Modifier.weight(1f), onTowers)
                FinalTool("الأمان", "◉", panel == FinalPanel.SAFETY, Modifier.weight(1f), onSafety)
                FinalTool("تشخيص", "⚙", panel == FinalPanel.DIAGNOSTICS, Modifier.weight(1f), onDiagnostics)
            }
        }
    }
}

@Composable
private fun FinalDockButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) FinalGold.copy(alpha = 0.20f) else FinalIvoryDeep)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = FinalInk, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun FinalTool(label: String, icon: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) FinalGold.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = FinalGoldDeep, fontSize = 18.sp)
            Text(label, color = FinalInk, fontSize = 7.sp)
        }
    }
}

@Composable
private fun FinalBandCard(
    title: String,
    subtitle: String,
    bands: Set<Int>,
    selected: Set<Int>,
    prefix: String,
    busy: Boolean,
    compact: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit
) {
    FinalCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = FinalInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = FinalMuted, fontSize = 8.sp)
            Spacer(Modifier.height(9.dp))

            if (bands.isEmpty()) {
                Text("لا توجد قائمة موثّقة لهذا Profile.", color = FinalWarn, fontSize = 9.sp)
            } else {
                val columns = if (compact) 4 else 5
                bands.sorted().chunked(columns).forEach { rowBands ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowBands.forEach { band ->
                            FilterChip(
                                selected = band in selected,
                                onClick = { onToggle(band) },
                                label = { Text("$prefix$band", fontSize = 8.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinalGold.copy(alpha = 0.24f))
                            )
                        }
                        repeat(columns - rowBands.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Button(
                    onClick = onApply,
                    enabled = selected.isNotEmpty() && !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FinalGoldDeep)
                ) {
                    Text("تطبيق والتحقق بالـ read-back", fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun FinalTowerPanel(
    snapshot: RouterSnapshot,
    cells: List<NearbyCell>,
    scanBusy: Boolean,
    busy: Boolean,
    target: TowerTarget?,
    guardEnabled: Boolean,
    guardStatus: TowerGuardStatus?,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onLockCell: (NearbyCell) -> Unit,
    onClearLock: () -> Unit,
    onGuardChange: (Boolean) -> Unit
) {
    FinalCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("الأبراج والخلايا", color = FinalInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "التثبيت يتم على PCI + EARFCN. لا نسمي الخلية برجًا جغرافيًا إلا إذا توفرت هوية موثوقة؛ لذلك لا توجد نقاط خريطة مختلقة.",
                color = FinalMuted, fontSize = 8.sp
            )
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FinalOutlined(if (scanBusy) "جاري المسح" else "مسح الخلايا", !scanBusy && !busy, Modifier.weight(1f), onScan)
                FinalOutlined("تثبيت الحالية", !busy && snapshot.pci != null && snapshot.earfcn != null, Modifier.weight(1f), onLockCurrent)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FinalOutlined("إزالة القفل", !busy, Modifier.weight(1f), onClearLock)
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Switch(
                        checked = guardEnabled,
                        onCheckedChange = onGuardChange,
                        enabled = target != null && !busy,
                        colors = SwitchDefaults.colors(checkedTrackColor = FinalGoldDeep)
                    )
                    Text("حارس البرج", color = FinalInk, fontSize = 8.sp)
                }
            }

            target?.let {
                Spacer(Modifier.height(7.dp))
                Text(
                    "الهدف: PCI ${it.pci} • EARFCN ${it.earfcn}${it.enodebId?.let { id -> " • eNodeB $id" } ?: ""}",
                    color = FinalGoldDeep, fontSize = 9.sp, fontWeight = FontWeight.Bold
                )
            }
            guardStatus?.let {
                Text(finalGuardText(it), color = finalGuardColor(it.match), fontSize = 8.sp)
            }

            Spacer(Modifier.height(10.dp))
            if (cells.isEmpty()) {
                Text("اضغط «مسح الخلايا». إذا لم يعرض Firmware الجيران ستبقى القائمة فارغة بدل اختلاق نتائج.", color = FinalMuted, fontSize = 8.sp)
            } else {
                cells.take(12).forEachIndexed { index, cell ->
                    FinalCellRow(index + 1, cell, busy, onLockCell)
                }
            }
        }
    }
}

@Composable
private fun FinalCellRow(index: Int, cell: NearbyCell, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val canLock = cell.rat == "LTE" && cell.pci != null && cell.arfcn != null
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(FinalGold.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(index.toString(), color = FinalGoldDeep, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${cell.rat} ${cell.band ?: ""} • PCI ${cell.pci ?: "—"} • ${if (cell.rat == "NR") "ARFCN" else "EARFCN"} ${cell.arfcn ?: "—"}",
                color = FinalInk, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1
            )
            Text(
                "RSRP ${cell.rsrp?.let(::finalNumber) ?: "—"} dBm • RSRQ ${cell.rsrq?.let(::finalNumber) ?: "—"} dB",
                color = FinalMuted, fontSize = 7.sp
            )
        }
        OutlinedButton(
            onClick = { onLock(cell) },
            enabled = canLock && !busy,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(if (cell.rat == "LTE") "تثبيت" else "قراءة", color = FinalInk, fontSize = 7.sp)
        }
    }
}

@Composable
private fun FinalNetworkPanel(
    capabilities: RouterCapabilities,
    busy: Boolean,
    onMode: (String) -> Unit,
    onAntenna: (Int) -> Unit
) {
    FinalCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("وضع الشبكة", color = FinalInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("الوضع المسموح لا يساوي الاتصال الفعلي؛ حالة الاتصال الحية تبقى في البطاقة الذهبية.", color = FinalMuted, fontSize = 8.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                FinalOutlined("4G فقط", !busy, Modifier.weight(1f)) { onMode("Only_LTE") }
                FinalOutlined("4G + 5G", !busy, Modifier.weight(1f)) { onMode("LTE_AND_5G") }
                FinalOutlined("5G فقط", !busy, Modifier.weight(1f)) { onMode("Only_5G") }
            }
            if (capabilities.supportsAntennaControl) {
                Spacer(Modifier.height(10.dp))
                Text("هوائي MC801A", color = FinalMuted, fontSize = 8.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (1..3).forEach { state ->
                        FinalOutlined("اتجاه $state", !busy, Modifier.weight(1f)) { onAntenna(state) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalSafetyPanel(
    backupAvailable: Boolean,
    busy: Boolean,
    onRestore: () -> Unit,
    onNetwork: () -> Unit
) {
    FinalCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("الأمان والاستعادة", color = FinalInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "قبل تغييرات التردد/الوضع/الخلية يحفظ التطبيق القيم الأصلية محليًا. لا تُحفظ كلمة المرور أو رموز الدخول أو IMSI/IMEI.",
                color = FinalMuted, fontSize = 8.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (backupAvailable) "آخر نسخة أمان متوفرة لهذا الراوتر" else "لا توجد نسخة أمان بعد",
                color = if (backupAvailable) FinalGood else FinalMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onRestore,
                    enabled = backupAvailable && !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FinalGoldDeep)
                ) { Text("استعادة النسخة الأصلية", fontSize = 8.sp) }
                FinalOutlined("أوضاع الشبكة", !busy, Modifier.weight(1f), onNetwork)
            }
        }
    }
}

@Composable
private fun FinalQuickTools(
    placementMode: Boolean,
    onNetwork: () -> Unit,
    onPlacement: () -> Unit,
    onTowers: () -> Unit
) {
    FinalCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FinalTool("الشبكة", "☷", false, Modifier.weight(1f), onNetwork)
            FinalTool("المكان", "⌖", placementMode, Modifier.weight(1f), onPlacement)
            FinalTool("الأبراج", "⌁", false, Modifier.weight(1f), onTowers)
        }
    }
}

@Composable
private fun FinalPlacementPanel(enabled: Boolean, reading: PlacementReading?, onToggle: () -> Unit) {
    FinalCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("مساعد أفضل مكان", color = FinalInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("حرّك الراوتر ببطء؛ النغمة تتسارع كلما تحسن القياس الحقيقي.", color = FinalMuted, fontSize = 8.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FinalMiniDial(reading?.score?.total ?: 0, Modifier.size(64.dp))
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (enabled && reading != null) finalGuidance(reading.guidance) else "جاهز للبحث",
                        color = if ((reading?.deltaFromBest ?: 0) < -5) FinalWarn else FinalGood,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text("أفضل نقطة في الجلسة: ${reading?.bestScore ?: 0}/100", color = FinalMuted, fontSize = 8.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = FinalGoldDeep)
            ) {
                Text(if (enabled) "إيقاف مساعد المكان" else "ابدأ البحث عن أفضل مكان")
            }
        }
    }
}

@Composable
private fun FinalDiagnostics(snapshot: RouterSnapshot) {
    val keys = listOf(
        "_zte_verified_network_type", "_zte_radio_mode", "_zte_nr_active_verified",
        "_zte_nr_structural_evidence", "_zte_nr_signal_evidence", "_zte_ca_active",
        "_zte_ca_verified", "_zte_ca_state_conflict", "wan_lte_ca", "lte_ca_pcell_band",
        "lte_multi_ca_scell_info", "wan_active_band", "wan_active_channel", "lte_pci",
        "nr5g_action_band", "nr5g_action_nsa_band", "nr5g_action_channel", "nr5g_pci",
        "Z5g_rsrp", "Z5g_SINR", "lte_band_lock", "nr5g_sa_band_lock", "nr5g_nsa_band_lock",
        "lte_pci_lock", "lte_earfcn_lock"
    )
    FinalCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("تشخيص Truth-First", color = FinalInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("هذه حقول راديو وإثبات فقط؛ لا تعرض كلمة المرور.", color = FinalMuted, fontSize = 8.sp)
            Spacer(Modifier.height(7.dp))
            keys.forEach { key ->
                snapshot.raw[key]?.takeIf { it.isNotBlank() }?.let { value ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(key, color = FinalMuted, fontSize = 7.sp, modifier = Modifier.weight(0.48f))
                        Text(value, color = FinalInk, fontSize = 7.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(0.52f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalOutlined(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(text, color = FinalInk, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun FinalStatusStrip(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(FinalGold.copy(alpha = 0.13f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(message, color = FinalInk, fontSize = 8.sp)
    }
}

@Composable
private fun FinalCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(30.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.09f),
            spotColor = Color.Black.copy(alpha = 0.09f)
        ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = FinalIvory),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) { content() }
}

@Composable
private fun FinalMiniDial(score: Int, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(FinalLine, style = Stroke(width = 6f))
            drawArc(
                color = FinalGold,
                startAngle = -90f,
                sweepAngle = 360f * score.coerceIn(0, 100) / 100f,
                useCenter = false,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
        }
        Text(score.toString(), color = FinalInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

private fun finalOperator(snapshot: RouterSnapshot): String {
    val named = snapshot.raw["network_provider_fullname"].orEmpty().trim()
        .ifBlank { snapshot.raw["network_provider"].orEmpty().trim() }
    if (named.isNotBlank()) return named
    return when (snapshot.operatorCode) {
        "42001" -> "stc ksa"
        "42003" -> "Mobily"
        "42004" -> "Zain KSA"
        else -> snapshot.operatorCode ?: "ZTE"
    }
}

private fun finalLteStatus(snapshot: RouterSnapshot): String = when {
    snapshot.networkType == "4G+" && snapshot.caActive -> "دمج LTE حي ومثبت"
    snapshot.networkType == "4G" -> "LTE حي بدون ادعاء دمج"
    else -> "الحالة غير مكتملة؛ لا يوجد تخمين"
}

private fun finalBandCombo(snapshot: RouterSnapshot): String {
    val nr = premiumCurrentNrBands(snapshot).sorted().map { "N$it" }
    val lte = premiumCurrentLteBands(snapshot).sorted().map { "B$it" }
    return (nr + lte).joinToString("+").ifBlank { "غير مؤكد" }
}

private fun finalAggregation(snapshot: RouterSnapshot): String {
    val nr = premiumCurrentNrBands(snapshot)
    val lte = premiumCurrentLteBands(snapshot)
    return when {
        nr.isNotEmpty() && lte.isNotEmpty() && snapshot.caActive && lte.size >= 2 ->
            "5G NSA موثّق + دمج ${lte.size} LTE carriers"
        nr.isNotEmpty() && lte.isNotEmpty() -> "5G NSA موثّق + LTE anchor"
        nr.isNotEmpty() -> "5G NR موثّق"
        snapshot.caActive && lte.size >= 2 -> "دمج LTE فعلي • ${lte.size}CA"
        lte.isNotEmpty() -> "LTE نشط بدون دمج موثّق"
        else -> "لا توجد Carrier كافية لإعلان الدمج"
    }
}

private fun finalGuardText(status: TowerGuardStatus): String = when (status.match) {
    TowerMatch.MATCHED -> "مطابق • الخلية ثابتة على الهدف"
    TowerMatch.RADIO_MATCH_ID_CHANGED -> "تنبيه • PCI/EARFCN متطابقان لكن هوية البرج تغيرت"
    TowerMatch.DRIFTED -> "انحراف عن الخلية المستهدفة${if (status.repaired) " • أُعيد القفل" else ""}"
    TowerMatch.UNKNOWN -> "لا توجد بيانات كافية للتحقق الآن"
}

private fun finalGuardColor(match: TowerMatch): Color = when (match) {
    TowerMatch.MATCHED -> FinalGood
    TowerMatch.UNKNOWN -> FinalMuted
    else -> FinalWarn
}

private fun finalGoal(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun finalGuidance(guidance: PlacementGuidance): String = when (guidance) {
    PlacementGuidance.INITIAL -> "حرّك الراوتر ببطء"
    PlacementGuidance.MUCH_BETTER -> "تحسن واضح — استمر"
    PlacementGuidance.BETTER -> "أفضل — استمر ببطء"
    PlacementGuidance.STABLE -> "الفرق بسيط — جرّب حركة صغيرة"
    PlacementGuidance.WORSE -> "أسوأ — ارجع قليلًا"
    PlacementGuidance.RETURN_TO_BEST -> "ارجع لأفضل نقطة"
    PlacementGuidance.CELL_CHANGED_WORSE -> "انتقلت لخلية أضعف"
    PlacementGuidance.EXCELLENT_HOLD -> "ممتاز — ثبّت الراوتر هنا"
    PlacementGuidance.BEST_SO_FAR -> "أفضل نقطة حتى الآن"
}

private fun finalNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

@Composable
private fun FinalPlacementAudio(reading: PlacementReading) {
    val haptic = LocalHapticFeedback.current
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    LaunchedEffect(reading.guidance) {
        when (reading.guidance) {
            PlacementGuidance.BEST_SO_FAR, PlacementGuidance.EXCELLENT_HOLD -> {
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 100)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            PlacementGuidance.WORSE, PlacementGuidance.RETURN_TO_BEST, PlacementGuidance.CELL_CHANGED_WORSE ->
                tone.startTone(ToneGenerator.TONE_PROP_NACK, 90)
            else -> Unit
        }
    }

    LaunchedEffect(reading.score.total) {
        while (true) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
            delay((1_300 - reading.score.total * 10L).coerceIn(220L, 1_000L))
        }
    }
}
