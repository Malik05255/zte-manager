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
import kotlinx.coroutines.delay

private val RefBg = Color(0xFFF1EEE7)
private val RefIvory = Color(0xFFF8F4ED)
private val RefIvory2 = Color(0xFFE8E1D6)
private val RefGold = Color(0xFFC89B45)
private val RefGoldDeep = Color(0xFF8A6428)
private val RefInk = Color(0xFF28231E)
private val RefMuted = Color(0xFF766E63)
private val RefLine = Color(0xFFD6CEC1)
private val RefGood = Color(0xFF668B58)
private val RefBad = Color(0xFFAD633C)

private enum class RefPanel { NONE, BANDS, NETWORK, PLACE, DIAGNOSTICS }

@Composable
fun ReferenceDashboard(
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
    onDisconnect: () -> Unit,
    onSpeedTest: () -> Unit,
    onPlacementToggle: () -> Unit,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onAllowAllLte: () -> Unit,
    onApplyNr: () -> Unit,
    onAllowAllNr: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onLockCurrentCell: () -> Unit,
    onAntennaState: (Int) -> Unit
) {
    if (placementMode && placementReading != null) {
        RefPlacementAudio(placementReading)
    }

    var panel by rememberSaveable { mutableStateOf(RefPanel.NONE) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(RefBg)
    ) {
        val tiny = maxWidth < 340.dp
        val compact = maxWidth < 390.dp
        val pagePadding = if (compact) 10.dp else 14.dp
        val gap = if (compact) 8.dp else 10.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = pagePadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            item { RefHeader(status = status, compact = compact, onDisconnect = onDisconnect) }

            val data = snapshot
            if (data == null) {
                item {
                    RefCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "جاري قراءة الراوتر...",
                            modifier = Modifier.padding(22.dp),
                            color = RefMuted
                        )
                    }
                }
            } else {
                item {
                    if (tiny) {
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            RefNetworkCard(data, Modifier.fillMaxWidth(), compact)
                            RefSpeedCard(lastPerformance, speedBusy, onSpeedTest, Modifier.fillMaxWidth(), compact)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            RefNetworkCard(data, Modifier.weight(1.08f), compact)
                            RefSpeedCard(lastPerformance, speedBusy, onSpeedTest, Modifier.weight(0.92f), compact)
                        }
                    }
                }

                item {
                    if (tiny) {
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            RefSmartCard(
                                snapshot = data,
                                enabled = smartMode,
                                goal = smartGoal,
                                busy = smartBusy,
                                report = smartReport,
                                onEnabledChange = onSmartModeChange,
                                onGoalChange = onSmartGoalChange,
                                onOptimizeNow = onOptimizeNow,
                                modifier = Modifier.fillMaxWidth(),
                                compact = compact
                            )
                            RefLocatorCard(data, Modifier.fillMaxWidth(), compact)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RefSmartCard(
                                snapshot = data,
                                enabled = smartMode,
                                goal = smartGoal,
                                busy = smartBusy,
                                report = smartReport,
                                onEnabledChange = onSmartModeChange,
                                onGoalChange = onSmartGoalChange,
                                onOptimizeNow = onOptimizeNow,
                                modifier = Modifier.weight(1.18f),
                                compact = compact
                            )
                            RefLocatorCard(data, Modifier.weight(0.82f), compact)
                        }
                    }
                }

                if (operationMessage.isNotBlank()) {
                    item { RefStatusStrip(operationMessage) }
                }

                item {
                    RefControlDock(
                        panel = panel,
                        placementMode = placementMode,
                        onBands = { panel = if (panel == RefPanel.BANDS) RefPanel.NONE else RefPanel.BANDS },
                        onNetwork = { panel = if (panel == RefPanel.NETWORK) RefPanel.NONE else RefPanel.NETWORK },
                        onPlacement = { panel = if (panel == RefPanel.PLACE) RefPanel.NONE else RefPanel.PLACE },
                        onDiagnostics = { panel = if (panel == RefPanel.DIAGNOSTICS) RefPanel.NONE else RefPanel.DIAGNOSTICS },
                        onOptimizeNow = onOptimizeNow
                    )
                }

                when (panel) {
                    RefPanel.BANDS -> {
                        item {
                            RefBandCard(
                                title = "4G LTE",
                                bands = capabilities.supportedLteBands,
                                selected = selectedLte,
                                prefix = "B",
                                busy = controlBusy,
                                compact = compact,
                                onToggle = onLteToggle,
                                onApply = onApplyLte,
                                onAll = onAllowAllLte
                            )
                        }
                        if (capabilities.supportsNrBandLock) {
                            item {
                                RefBandCard(
                                    title = "5G NR",
                                    bands = capabilities.supportedNrBands,
                                    selected = selectedNr,
                                    prefix = "N",
                                    busy = controlBusy,
                                    compact = compact,
                                    onToggle = onNrToggle,
                                    onApply = onApplyNr,
                                    onAll = onAllowAllNr
                                )
                            }
                        }
                    }

                    RefPanel.NETWORK -> item {
                        RefNetworkTools(
                            snapshot = data,
                            capabilities = capabilities,
                            busy = controlBusy,
                            onMode = onSetNetworkMode,
                            onLock = onLockCurrentCell,
                            onAntenna = onAntennaState
                        )
                    }

                    RefPanel.PLACE -> item {
                        RefPlacementCard(
                            enabled = placementMode,
                            reading = placementReading,
                            onToggle = onPlacementToggle
                        )
                    }

                    RefPanel.DIAGNOSTICS -> item { RefDiagnostics(data) }
                    RefPanel.NONE -> Unit
                }

                item { Spacer(modifier = Modifier.height(18.dp)) }
            }
        }
    }
}

@Composable
private fun RefHeader(status: String, compact: Boolean, onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onDisconnect,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 5.dp)
        ) {
            Text("فصل", color = RefInk, fontSize = if (compact) 11.sp else 12.sp)
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "لوحة التحكم الشبكية",
                color = RefInk,
                fontSize = if (compact) 20.sp else 23.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = status,
                color = RefMuted,
                fontSize = if (compact) 8.sp else 9.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.size(if (compact) 58.dp else 66.dp))
    }
}

@Composable
private fun RefNetworkCard(snapshot: RouterSnapshot, modifier: Modifier, compact: Boolean) {
    val radio = refRadio(snapshot)
    val activeNr = snapshot.raw["_zte_nr_active"].equals("true", true)
    val operator = snapshot.raw["network_provider_fullname"].orEmpty()
        .ifBlank { snapshot.raw["network_provider"].orEmpty() }
        .ifBlank { snapshot.operatorCode ?: "ZTE" }
    val rsrp = if (activeNr) snapshot.nrRsrp ?: snapshot.lteRsrp else snapshot.lteRsrp
    val sinr = if (activeNr) snapshot.nrSinr ?: snapshot.lteSinr else snapshot.lteSinr
    val shape = RoundedCornerShape(
        topStart = 46.dp,
        topEnd = 30.dp,
        bottomEnd = 42.dp,
        bottomStart = 24.dp
    )

    Card(
        modifier = modifier.shadow(8.dp, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = RefGoldDeep)
    ) {
        Column(modifier = Modifier.padding(if (compact) 11.dp else 14.dp)) {
            Text("Network Stat", color = Color.White.copy(alpha = 0.76f), fontSize = 7.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = radio.first,
                    color = Color(0xFFFFE39A),
                    fontSize = if (compact) 28.sp else 34.sp,
                    fontWeight = FontWeight.Black
                )
                Text("⌁", color = Color(0xFFFFD66D), fontSize = 22.sp)
            }
            Text(operator, color = Color.White, fontSize = 9.sp, maxLines = 1)
            Text(radio.second, color = Color.White.copy(alpha = 0.76f), fontSize = 7.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RefMetric("RSRP", rsrp, "dBm", Modifier.weight(1f))
                RefMetric("SINR", sinr, "dB", Modifier.weight(1f))
                RefMetric("RSRQ", snapshot.lteRsrq, "dB", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RefMetric(label: String, value: Double?, unit: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color(0xFFFFC95B).copy(alpha = 0.45f), CircleShape)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value?.let(::refNumber) ?: "—", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(unit, color = Color.White.copy(alpha = 0.68f), fontSize = 6.sp)
            Text(label, color = Color.White.copy(alpha = 0.80f), fontSize = 6.sp)
        }
    }
}

@Composable
private fun RefSpeedCard(
    performance: NetworkPerformance?,
    busy: Boolean,
    onSpeedTest: () -> Unit,
    modifier: Modifier,
    compact: Boolean
) {
    val shape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 52.dp,
        bottomEnd = 26.dp,
        bottomStart = 46.dp
    )

    RefCard(modifier = modifier, shape = shape) {
        Column(
            modifier = Modifier.padding(if (compact) 11.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Speed Test", color = RefInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("⚡", fontSize = 18.sp)
                Text(
                    text = performance?.downloadMbps?.let(::refNumber) ?: "—",
                    color = RefGold,
                    fontSize = if (compact) 28.sp else 36.sp,
                    fontWeight = FontWeight.Black
                )
                Text(" Mb/s", color = RefGoldDeep, fontSize = 8.sp, modifier = Modifier.padding(bottom = 5.dp))
            }
            performance?.latencyMs?.let {
                Text("Ping ${refNumber(it)} ms", color = RefMuted, fontSize = 7.sp)
            }
            Spacer(modifier = Modifier.height(7.dp))
            Button(
                onClick = onSpeedTest,
                enabled = !busy,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = RefIvory2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(if (busy) "جاري القياس" else "قياس السرعة", color = RefInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RefSmartCard(
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
    val combo = refCombo(snapshot)
    val shape = RoundedCornerShape(
        topStart = 54.dp,
        topEnd = 26.dp,
        bottomEnd = 52.dp,
        bottomStart = 34.dp
    )

    RefCard(modifier = modifier, shape = shape) {
        Column(modifier = Modifier.padding(if (compact) 11.dp else 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "حالة السرعة والتوصيل",
                        color = RefInk,
                        fontSize = if (compact) 10.sp else 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = if (enabled) "المحرك الذكي يعمل" else "المحرك الذكي متوقف",
                        color = RefMuted,
                        fontSize = 7.sp,
                        maxLines = 1
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = RefGoldDeep,
                        checkedThumbColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(5.dp))
            Text("الإعداد النشط", color = RefMuted, fontSize = 7.sp)
            Text(
                text = combo,
                color = RefInk,
                fontSize = if (compact) 18.sp else 21.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(refAggregation(snapshot), color = RefGoldDeep, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RefMiniDial(score, Modifier.size(if (compact) 46.dp else 52.dp))
                Spacer(modifier = Modifier.size(7.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("هدف التحسين", color = RefMuted, fontSize = 6.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        OptimizationGoal.entries.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (goal == item) RefGold.copy(alpha = 0.18f) else Color.Transparent)
                                    .border(1.dp, if (goal == item) RefGold else RefLine, RoundedCornerShape(12.dp))
                                    .clickable { onGoalChange(item) }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(refGoal(item), color = RefInk, fontSize = 6.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            report?.let {
                Text(
                    text = "الأفضل: ${it.best.bands.sorted().joinToString("+") { band -> "B$band" }} • ${it.best.score}/100",
                    color = RefMuted,
                    fontSize = 7.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(5.dp))
            Button(
                onClick = onOptimizeNow,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = RefIvory2),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text(if (busy) "جاري التحسين..." else "تحسين الآن", color = RefInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RefLocatorCard(snapshot: RouterSnapshot, modifier: Modifier, compact: Boolean) {
    val shape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 58.dp,
        bottomEnd = 58.dp,
        bottomStart = 28.dp
    )

    RefCard(modifier = modifier, shape = shape) {
        Column(
            modifier = Modifier.padding(if (compact) 9.dp else 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Cell Locator", color = RefInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("الخلية الحالية", color = RefMuted, fontSize = 6.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 108.dp else 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(if (compact) 98.dp else 110.dp)) {
                    val c = center
                    drawCircle(RefLine.copy(alpha = 0.55f), radius = size.minDimension * 0.48f, style = Stroke(2f))
                    drawCircle(RefLine.copy(alpha = 0.55f), radius = size.minDimension * 0.32f, style = Stroke(2f))
                    drawCircle(RefGold.copy(alpha = 0.20f), radius = size.minDimension * 0.18f, center = c)
                    drawCircle(RefGoldDeep, radius = 7f, center = c)

                    val points = listOf(
                        Offset(size.width * 0.25f, size.height * 0.28f),
                        Offset(size.width * 0.76f, size.height * 0.23f),
                        Offset(size.width * 0.80f, size.height * 0.74f)
                    )
                    points.forEach { point ->
                        drawLine(
                            color = RefGoldDeep,
                            start = Offset(point.x, point.y + 11f),
                            end = Offset(point.x, point.y - 7f),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(RefGold, radius = 5f, center = Offset(point.x, point.y - 10f))
                    }
                }
                Text(
                    text = "PCI ${snapshot.pci ?: "—"}",
                    color = RefInk,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun RefControlDock(
    panel: RefPanel,
    placementMode: Boolean,
    onBands: () -> Unit,
    onNetwork: () -> Unit,
    onPlacement: () -> Unit,
    onDiagnostics: () -> Unit,
    onOptimizeNow: () -> Unit
) {
    val shape = RoundedCornerShape(
        topStart = 34.dp,
        topEnd = 18.dp,
        bottomEnd = 34.dp,
        bottomStart = 18.dp
    )

    RefCard(modifier = Modifier.fillMaxWidth(), shape = shape) {
        Column(modifier = Modifier.padding(9.dp)) {
            Text("Advanced Control", color = RefInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RefDockButton("قفل النطاقات", panel == RefPanel.BANDS, Modifier.weight(1f), onBands)
                RefDockButton("تحسين الشبكة", false, Modifier.weight(1f), onOptimizeNow)
            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                RefTool("شبكة", "☷", panel == RefPanel.NETWORK, Modifier.weight(1f), onNetwork)
                RefTool("المكان", "⌖", panel == RefPanel.PLACE || placementMode, Modifier.weight(1f), onPlacement)
                RefTool("تشخيص", "⚙", panel == RefPanel.DIAGNOSTICS, Modifier.weight(1f), onDiagnostics)
            }
        }
    }
}

@Composable
private fun RefDockButton(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) RefGold.copy(alpha = 0.18f) else RefIvory2)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = RefInk, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun RefTool(
    label: String,
    icon: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) RefGold.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = RefGoldDeep, fontSize = 18.sp)
            Text(label, color = RefInk, fontSize = 7.sp)
        }
    }
}

@Composable
private fun RefBandCard(
    title: String,
    bands: Set<Int>,
    selected: Set<Int>,
    prefix: String,
    busy: Boolean,
    compact: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit,
    onAll: () -> Unit
) {
    RefCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("اختيار $title", color = RefInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "اختر الترددات ثم طبّق. أعلى الصفحة يعرض الاتصال النشط فعليًا، لا مجرد الاختيار.",
                color = RefMuted,
                fontSize = 8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            val columns = if (compact) 4 else 5
            bands.sorted().chunked(columns).forEach { rowBands ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowBands.forEach { band ->
                        FilterChip(
                            selected = band in selected,
                            onClick = { onToggle(band) },
                            label = { Text("$prefix$band", fontSize = 8.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RefGold.copy(alpha = 0.22f)
                            )
                        )
                    }
                    repeat(columns - rowBands.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onApply,
                    enabled = selected.isNotEmpty() && !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = RefGoldDeep)
                ) {
                    Text("تطبيق والتحقق", fontSize = 9.sp)
                }
                OutlinedButton(
                    onClick = onAll,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("تلقائي", color = RefInk, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun RefNetworkTools(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    busy: Boolean,
    onMode: (String) -> Unit,
    onLock: () -> Unit,
    onAntenna: (Int) -> Unit
) {
    RefCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("التحكم بالشبكة", color = RefInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                RefOutlined("4G فقط", !busy, Modifier.weight(1f)) { onMode("Only_LTE") }
                RefOutlined("4G + 5G", !busy, Modifier.weight(1f)) { onMode("LTE_AND_5G") }
                RefOutlined("5G فقط", !busy, Modifier.weight(1f)) { onMode("Only_5G") }
            }

            if (capabilities.supportsCellLock && snapshot.pci != null && snapshot.earfcn != null) {
                Spacer(modifier = Modifier.height(6.dp))
                RefOutlined(
                    text = "تثبيت الخلية • PCI ${snapshot.pci} / EARFCN ${snapshot.earfcn}",
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onLock
                )
            }

            if (capabilities.supportsAntennaControl) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("هوائي MC801A", color = RefMuted, fontSize = 8.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    (1..3).forEach { state ->
                        RefOutlined("اتجاه $state", !busy, Modifier.weight(1f)) { onAntenna(state) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RefOutlined(
    text: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(text, color = RefInk, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RefPlacementCard(
    enabled: Boolean,
    reading: PlacementReading?,
    onToggle: () -> Unit
) {
    RefCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("مساعد أفضل مكان", color = RefInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "حرّك الراوتر سنتيمترات قليلة؛ النغمة تتسارع كلما اقتربت من أفضل نقطة.",
                color = RefMuted,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RefMiniDial(reading?.score?.total ?: 0, Modifier.size(62.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (enabled && reading != null) refGuidance(reading.guidance) else "جاهز للبحث",
                        color = if ((reading?.deltaFromBest ?: 0) < -5) RefBad else RefGood,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text("أفضل نقطة: ${reading?.bestScore ?: 0}/100", color = RefMuted, fontSize = 8.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = RefGoldDeep)
            ) {
                Text(if (enabled) "إيقاف مساعد المكان" else "ابدأ البحث عن أفضل مكان")
            }
        }
    }
}

@Composable
private fun RefDiagnostics(snapshot: RouterSnapshot) {
    val keys = listOf(
        "_zte_radio_mode",
        "_zte_raw_network_type",
        "_zte_nr_active",
        "wan_lte_ca",
        "lte_ca_pcell_band",
        "lte_multi_ca_scell_info",
        "nr5g_action_band",
        "nr5g_action_nsa_band",
        "ZCELLINFO_band",
        "Z5g_rsrp",
        "Z5g_SINR",
        "nr5g_pci",
        "Z5g_dlEarfcn"
    )

    RefCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("تشخيص القراءة", color = RefInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("حقول راديو فقط؛ لا تظهر كلمة المرور.", color = RefMuted, fontSize = 8.sp)
            Spacer(modifier = Modifier.height(7.dp))
            keys.forEach { key ->
                snapshot.raw[key]?.takeIf { it.isNotBlank() }?.let { value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(key, color = RefMuted, fontSize = 7.sp, modifier = Modifier.weight(1f))
                        Text(
                            value,
                            color = RefInk,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RefStatusStrip(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(RefGold.copy(alpha = 0.12f))
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(message, color = RefInk, fontSize = 8.sp)
    }
}

@Composable
private fun RefCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.shadow(
            elevation = 7.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.08f)
        ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = RefIvory),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
private fun RefMiniDial(score: Int, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(RefLine, style = Stroke(width = 6f))
            drawArc(
                color = RefGold,
                startAngle = -90f,
                sweepAngle = score.coerceIn(0, 100) * 3.6f,
                useCenter = false,
                style = Stroke(width = 7f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), color = RefInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text("/100", color = RefMuted, fontSize = 6.sp)
        }
    }
}

private fun refRadio(snapshot: RouterSnapshot): Pair<String, String> {
    val mode = snapshot.raw["_zte_radio_mode"].orEmpty()
    val nrActive = snapshot.raw["_zte_nr_active"].equals("true", true)
    val ca = snapshot.caActive
    return when {
        mode == "NSA_ACTIVE" || (nrActive && premiumCurrentLteBands(snapshot).isNotEmpty()) -> "5G" to "NSA • NR نشط"
        mode == "SA_ACTIVE" || nrActive -> "5G" to "SA • NR نشط"
        mode == "NSA_STANDBY" -> (if (ca) "4G+" else "4G") to "5G NSA جاهز"
        ca -> "4G+" to "LTE-A • دمج نشط"
        else -> "4G" to "LTE"
    }
}

private fun refCombo(snapshot: RouterSnapshot): String {
    val nr = premiumCurrentNrBands(snapshot).sorted().map { "N$it" }
    val lte = premiumCurrentLteBands(snapshot).sorted().map { "B$it" }
    return (nr + lte).joinToString("+").ifBlank { "جاري القراءة" }
}

private fun refAggregation(snapshot: RouterSnapshot): String {
    val lte = premiumCurrentLteBands(snapshot)
    val nr = premiumCurrentNrBands(snapshot)
    return when {
        nr.isNotEmpty() && lte.isNotEmpty() -> "5G + ${lte.size} LTE • اتصال فعلي"
        snapshot.caActive && lte.size > 1 -> "دمج 4G فعلي • ${lte.size}CA"
        nr.isNotEmpty() -> "5G NR نشط"
        else -> "بدون دمج نشط"
    }
}

private fun refNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

private fun refGoal(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun refGuidance(guidance: PlacementGuidance): String = when (guidance) {
    PlacementGuidance.INITIAL -> "حرّك الراوتر ببطء"
    PlacementGuidance.MUCH_BETTER -> "تحسن واضح — استمر"
    PlacementGuidance.BETTER -> "أفضل — استمر قليلًا"
    PlacementGuidance.STABLE -> "ثابت — جرّب حركة صغيرة"
    PlacementGuidance.WORSE -> "أسوأ — ارجع قليلًا"
    PlacementGuidance.RETURN_TO_BEST -> "ارجع إلى أفضل نقطة"
    PlacementGuidance.CELL_CHANGED_WORSE -> "انتقلت لخلية أضعف"
    PlacementGuidance.EXCELLENT_HOLD -> "ممتاز — ثبّت هنا"
    PlacementGuidance.BEST_SO_FAR -> "أفضل نقطة حتى الآن"
}

@Composable
private fun RefPlacementAudio(reading: PlacementReading) {
    val haptic = LocalHapticFeedback.current
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }

    DisposableEffect(Unit) {
        onDispose { tone.release() }
    }

    LaunchedEffect(reading.guidance) {
        when (reading.guidance) {
            PlacementGuidance.BEST_SO_FAR,
            PlacementGuidance.EXCELLENT_HOLD -> {
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 90)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            PlacementGuidance.WORSE,
            PlacementGuidance.RETURN_TO_BEST,
            PlacementGuidance.CELL_CHANGED_WORSE -> {
                tone.startTone(ToneGenerator.TONE_PROP_NACK, 80)
            }

            else -> Unit
        }
    }

    LaunchedEffect(reading.score.total) {
        while (true) {
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
            delay((1250 - reading.score.total * 10L).coerceIn(200L, 950L))
        }
    }
}
