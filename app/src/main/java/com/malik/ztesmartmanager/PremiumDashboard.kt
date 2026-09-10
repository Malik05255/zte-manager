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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.NetworkQualityEngine
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementGuidance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import kotlinx.coroutines.delay

val PremiumBackground = Color(0xFFF2EEE6)
private val PremiumSurface = Color(0xFFF8F5EF)
private val PremiumSurfaceAlt = Color(0xFFE9E4DB)
private val PremiumGold = Color(0xFFC69A47)
private val PremiumGoldDark = Color(0xFF8A6528)
private val PremiumInk = Color(0xFF2D2923)
private val PremiumMuted = Color(0xFF777066)
private val PremiumLine = Color(0xFFD9D1C5)
private val PremiumSuccess = Color(0xFF3B725B)
private val PremiumWarning = Color(0xFFAD6B31)

val PremiumLightColors = lightColorScheme(
    primary = PremiumGoldDark,
    onPrimary = Color.White,
    secondary = PremiumGold,
    background = PremiumBackground,
    surface = PremiumSurface,
    surfaceVariant = PremiumSurfaceAlt,
    onSurface = PremiumInk,
    onSurfaceVariant = PremiumMuted,
    outline = PremiumLine
)

@Composable
fun PremiumLoginScreen(
    routerAddress: String,
    onRouterAddressChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    status: String,
    busy: Boolean,
    onConnect: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(PremiumBackground).padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        PremiumCard(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ZTE Smart HAI", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = PremiumInk)
                Text("لوحة التحكم الشبكية", fontSize = 18.sp, color = PremiumGoldDark)
                Spacer(Modifier.height(26.dp))
                OutlinedTextField(
                    value = routerAddress,
                    onValueChange = onRouterAddressChange,
                    label = { Text("عنوان الراوتر") },
                    supportingText = { Text("مثال: 192.168.0.1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("كلمة مرور الإدارة") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(18.dp))
                PremiumPrimaryButton(
                    text = if (busy) "جاري الاتصال..." else "اتصال بالراوتر",
                    enabled = !busy && password.isNotBlank(),
                    onClick = onConnect
                )
                Spacer(Modifier.height(12.dp))
                Text(status, color = PremiumMuted, fontSize = 13.sp)
                Text("الاتصال وكلمة المرور يبقيان محليًا بين الهاتف والراوتر.", color = PremiumMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PremiumDashboard(
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
    if (placementMode && placementReading != null) PremiumPlacementAudio(placementReading)

    var showBands by rememberSaveable { mutableStateOf(false) }
    var showNetwork by rememberSaveable { mutableStateOf(false) }
    var showDiagnostics by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(PremiumBackground)) {
        val compact = maxWidth < 390.dp
        val narrow = maxWidth < 460.dp
        val horizontalPadding = if (compact) 12.dp else 18.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PremiumHeader(status = status, onDisconnect = onDisconnect)
            }

            snapshot?.let { data ->
                item {
                    if (narrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            NetworkStatusCard(data, Modifier.fillMaxWidth())
                            SpeedTestCard(lastPerformance, speedBusy, onSpeedTest, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NetworkStatusCard(data, Modifier.weight(1.05f))
                            SpeedTestCard(lastPerformance, speedBusy, onSpeedTest, Modifier.weight(0.95f))
                        }
                    }
                }

                item {
                    SmartPerformanceCard(
                        snapshot = data,
                        enabled = smartMode,
                        goal = smartGoal,
                        busy = smartBusy,
                        report = smartReport,
                        onEnabledChange = onSmartModeChange,
                        onGoalChange = onSmartGoalChange,
                        onOptimizeNow = onOptimizeNow
                    )
                }

                item {
                    if (narrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlacementAssistantCard(placementMode, placementReading, onPlacementToggle, Modifier.fillMaxWidth())
                            TowerMapCard(data, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PlacementAssistantCard(placementMode, placementReading, onPlacementToggle, Modifier.weight(1.08f))
                            TowerMapCard(data, Modifier.weight(0.92f))
                        }
                    }
                }

                if (operationMessage.isNotBlank()) {
                    item { PremiumStatusStrip(operationMessage) }
                }

                item {
                    AdvancedControlCard(
                        snapshot = data,
                        capabilities = capabilities,
                        showBands = showBands,
                        showNetwork = showNetwork,
                        showDiagnostics = showDiagnostics,
                        onToggleBands = { showBands = !showBands },
                        onToggleNetwork = { showNetwork = !showNetwork },
                        onToggleDiagnostics = { showDiagnostics = !showDiagnostics },
                        onLockCurrentCell = onLockCurrentCell,
                        controlBusy = controlBusy
                    )
                }

                if (showBands) {
                    item {
                        BandControlCard(
                            title = "ترددات 4G LTE",
                            subtitle = "اختر الترددات المسموح بها. التطبيق يتحقق من النتيجة بعد الإرسال.",
                            bands = capabilities.supportedLteBands,
                            selected = selectedLte,
                            prefix = "B",
                            compact = compact,
                            busy = controlBusy,
                            onToggle = onLteToggle,
                            onApply = onApplyLte,
                            onAllowAll = onAllowAllLte
                        )
                    }
                    if (capabilities.supportsNrBandLock) {
                        item {
                            BandControlCard(
                                title = "ترددات 5G NR",
                                subtitle = "اختيار N78 أو غيره لا يعني أن NR نشط؛ الحالة الفعلية تظهر أعلى الشاشة.",
                                bands = capabilities.supportedNrBands,
                                selected = selectedNr,
                                prefix = "N",
                                compact = compact,
                                busy = controlBusy,
                                onToggle = onNrToggle,
                                onApply = onApplyNr,
                                onAllowAll = onAllowAllNr
                            )
                        }
                    }
                }

                if (showNetwork) {
                    item {
                        NetworkControlCard(
                            capabilities = capabilities,
                            busy = controlBusy,
                            onSetNetworkMode = onSetNetworkMode,
                            onAntennaState = onAntennaState
                        )
                    }
                }

                if (showDiagnostics) {
                    item { DiagnosticsCard(data) }
                }

                item { Spacer(Modifier.height(22.dp)) }
            } ?: item {
                PremiumCard(Modifier.fillMaxWidth()) {
                    Text("جاري قراءة بيانات الراوتر...", modifier = Modifier.padding(24.dp), color = PremiumMuted)
                }
            }
        }
    }
}

@Composable
private fun PremiumHeader(status: String, onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("لوحة التحكم الشبكية", color = PremiumInk, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(status, color = PremiumMuted, fontSize = 12.sp)
        }
        OutlinedButton(onClick = onDisconnect, shape = RoundedCornerShape(50)) {
            Text("فصل", color = PremiumInk)
        }
    }
}

@Composable
private fun NetworkStatusCard(snapshot: RouterSnapshot, modifier: Modifier) {
    val radio = premiumRadioState(snapshot)
    val active5g = snapshot.raw["_zte_nr_active"].equals("true", true)
    val rsrp = if (active5g) snapshot.nrRsrp ?: snapshot.lteRsrp else snapshot.lteRsrp
    val sinr = if (active5g) snapshot.nrSinr ?: snapshot.lteSinr else snapshot.lteSinr
    val rsrq = snapshot.lteRsrq

    PremiumCard(modifier = modifier, warm = true) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("حالة الشبكة", color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp)
                    Text(radio.primary, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(snapshot.operatorCode?.let { "Operator $it" } ?: "ZTE", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (active5g) "5G" else "4G", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBubble("RSRP", rsrp, "dBm", Modifier.weight(1f))
                MetricBubble("SINR", sinr, "dB", Modifier.weight(1f))
                MetricBubble("RSRQ", rsrq, "dB", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(radio.secondary, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MetricBubble(label: String, value: Double?, unit: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(vertical = 9.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value?.let(::premiumFormatNumber) ?: "—", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(unit, color = Color.White.copy(alpha = 0.74f), fontSize = 9.sp)
            Text(label, color = Color.White.copy(alpha = 0.88f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun SpeedTestCard(
    performance: NetworkPerformance?,
    busy: Boolean,
    onSpeedTest: () -> Unit,
    modifier: Modifier
) {
    PremiumCard(modifier = modifier) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("اختبار السرعة", color = PremiumMuted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(performance?.downloadMbps?.let(::premiumFormatNumber) ?: "—", color = PremiumGoldDark, fontSize = 39.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(5.dp))
                Text("Mb/s", color = PremiumGoldDark, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            performance?.latencyMs?.let { Text("Ping ${premiumFormatNumber(it)} ms", color = PremiumMuted, fontSize = 11.sp) }
            Spacer(Modifier.height(12.dp))
            PremiumPrimaryButton(text = if (busy) "جاري القياس..." else "قياس السرعة", enabled = !busy, onClick = onSpeedTest)
        }
    }
}

@Composable
private fun SmartPerformanceCard(
    snapshot: RouterSnapshot,
    enabled: Boolean,
    goal: OptimizationGoal,
    busy: Boolean,
    report: SmartOptimizationReport?,
    onEnabledChange: (Boolean) -> Unit,
    onGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit
) {
    val score = remember(snapshot) { NetworkQualityEngine().score(snapshot) }
    val combo = premiumActiveBandCombo(snapshot)

    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حالة السرعة والتوصيل", color = PremiumInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("المحرك الذكي يراقب الجودة ويغيّر فقط عند تدهور مستمر", color = PremiumMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PremiumGoldDark)
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الإعداد النشط", color = PremiumMuted, fontSize = 11.sp)
                    Text(combo, color = PremiumInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(premiumAggregationText(snapshot), color = PremiumGoldDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                QualityDial(score.total)
            }

            Spacer(Modifier.height(16.dp))
            Text("هدف التحسين", color = PremiumMuted, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OptimizationGoal.entries.forEach { item ->
                    FilterChip(
                        selected = goal == item,
                        onClick = { onGoalChange(item) },
                        label = { Text(premiumGoalLabel(item), fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PremiumGold.copy(alpha = 0.18f)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            PremiumPrimaryButton(text = if (busy) "جاري التحسين..." else "تحسين الآن", enabled = !busy, onClick = onOptimizeNow)

            report?.let {
                Spacer(Modifier.height(10.dp))
                Text("آخر نتيجة: ${premiumFormatBands(it.best.bands)} • ${it.best.score}/100", color = PremiumMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun QualityDial(score: Int) {
    Box(modifier = Modifier.size(78.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color = PremiumLine, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f))
            val sweep = score.coerceIn(0, 100) * 3.6f
            drawArc(
                color = PremiumGold,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), color = PremiumInk, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("/100", color = PremiumMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun PlacementAssistantCard(
    enabled: Boolean,
    reading: PlacementReading?,
    onToggle: () -> Unit,
    modifier: Modifier
) {
    PremiumCard(modifier) {
        Column(Modifier.padding(18.dp)) {
            Text("مساعد أفضل مكان", color = PremiumInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("حرّك الراوتر ببطء؛ الصوت يتسارع كلما تحسن الموضع.", color = PremiumMuted, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            val score = reading?.score?.total
            Text(score?.let { "$it / 100" } ?: "جاهز للقياس", color = PremiumGoldDark, fontSize = 28.sp, fontWeight = FontWeight.Black)
            if (enabled && reading != null) {
                Text(premiumPlacementGuidance(reading.guidance), color = if (reading.deltaFromBest < -5) PremiumWarning else PremiumSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("أفضل نقطة في الجلسة: ${reading.bestScore}/100", color = PremiumMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(12.dp))
            PremiumPrimaryButton(text = if (enabled) "إيقاف مساعد المكان" else "ابدأ البحث عن أفضل مكان", enabled = true, onClick = onToggle)
        }
    }
}

@Composable
private fun TowerMapCard(snapshot: RouterSnapshot, modifier: Modifier) {
    PremiumCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text("خريطة الأبراج", color = PremiumInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("نعرض الخلية المتصلة فقط حتى يتوفر مصدر موثوق لمواقع الأبراج.", color = PremiumMuted, fontSize = 10.sp)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFE7E1D7))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val grid = PremiumLine.copy(alpha = 0.7f)
                    for (i in 1..4) {
                        val x = size.width * i / 5f
                        drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
                    }
                    for (i in 1..3) {
                        val y = size.height * i / 4f
                        drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                    }
                    val center = Offset(size.width * 0.52f, size.height * 0.55f)
                    drawCircle(PremiumGold.copy(alpha = 0.22f), radius = 34f, center = center)
                    drawCircle(PremiumGoldDark, radius = 10f, center = center)
                    val towers = listOf(
                        Offset(size.width * 0.20f, size.height * 0.28f),
                        Offset(size.width * 0.80f, size.height * 0.24f),
                        Offset(size.width * 0.82f, size.height * 0.76f)
                    )
                    towers.forEach { point ->
                        drawLine(PremiumGoldDark, Offset(point.x, point.y + 16f), Offset(point.x, point.y - 12f), strokeWidth = 5f, cap = StrokeCap.Round)
                        drawCircle(PremiumGold, radius = 8f, center = Offset(point.x, point.y - 15f))
                    }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                    Text("PCI ${snapshot.pci ?: "—"}", color = PremiumInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Cell ${snapshot.cellId ?: "—"}", color = PremiumMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun AdvancedControlCard(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    showBands: Boolean,
    showNetwork: Boolean,
    showDiagnostics: Boolean,
    onToggleBands: () -> Unit,
    onToggleNetwork: () -> Unit,
    onToggleDiagnostics: () -> Unit,
    onLockCurrentCell: () -> Unit,
    controlBusy: Boolean
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("التحكم المتقدم", color = PremiumInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("الأدوات القوية مخفية حتى تبقى الصفحة الرئيسية نظيفة.", color = PremiumMuted, fontSize = 10.sp)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumActionPill("النطاقات", showBands, onToggleBands, Modifier.weight(1f))
                PremiumActionPill("الشبكة", showNetwork, onToggleNetwork, Modifier.weight(1f))
                PremiumActionPill("تشخيص", showDiagnostics, onToggleDiagnostics, Modifier.weight(1f))
            }
            if (capabilities.supportsCellLock && snapshot.pci != null && snapshot.earfcn != null) {
                Spacer(Modifier.height(9.dp))
                OutlinedButton(
                    onClick = onLockCurrentCell,
                    enabled = !controlBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("تثبيت الخلية الحالية • PCI ${snapshot.pci} / EARFCN ${snapshot.earfcn}", color = PremiumInk, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PremiumActionPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) PremiumGold.copy(alpha = 0.20f) else PremiumSurfaceAlt)
            .border(1.dp, if (selected) PremiumGold else PremiumLine, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = PremiumInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun BandControlCard(
    title: String,
    subtitle: String,
    bands: Set<Int>,
    selected: Set<Int>,
    prefix: String,
    compact: Boolean,
    busy: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit,
    onAllowAll: () -> Unit
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = PremiumInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = PremiumMuted, fontSize = 10.sp)
            Spacer(Modifier.height(12.dp))
            val columns = if (compact) 3 else 4
            bands.sorted().chunked(columns).forEach { rowBands ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    rowBands.forEach { band ->
                        FilterChip(
                            selected = band in selected,
                            onClick = { onToggle(band) },
                            label = { Text("$prefix$band", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PremiumGold.copy(alpha = 0.22f))
                        )
                    }
                    repeat(columns - rowBands.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApply,
                    enabled = selected.isNotEmpty() && !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGoldDark)
                ) { Text("تطبيق والتحقق", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = onAllowAll,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("الوضع التلقائي", fontSize = 11.sp, color = PremiumInk) }
            }
        }
    }
}

@Composable
private fun NetworkControlCard(
    capabilities: RouterCapabilities,
    busy: Boolean,
    onSetNetworkMode: (String) -> Unit,
    onAntennaState: (Int) -> Unit
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("وضع الشبكة", color = PremiumInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                PremiumSmallButton("4G فقط", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_LTE") }
                PremiumSmallButton("4G + 5G", !busy, Modifier.weight(1f)) { onSetNetworkMode("LTE_AND_5G") }
                PremiumSmallButton("5G فقط", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_5G") }
            }
            if (capabilities.supportsAntennaControl) {
                Spacer(Modifier.height(16.dp))
                Text("هوائي MC801A", color = PremiumMuted, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PremiumSmallButton("حالة 1", !busy, Modifier.weight(1f)) { onAntennaState(1) }
                    PremiumSmallButton("حالة 2", !busy, Modifier.weight(1f)) { onAntennaState(2) }
                    PremiumSmallButton("حالة 3", !busy, Modifier.weight(1f)) { onAntennaState(3) }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(snapshot: RouterSnapshot) {
    val keys = listOf(
        "_zte_radio_mode", "_zte_raw_network_type", "_zte_nr_active",
        "network_type", "current_network", "nRat", "wan_lte_ca", "Lte_ca_status",
        "wan_active_band", "lte_ca_pcell_band", "lte_multi_ca_scell_info",
        "nr5g_action_band", "nr5g_action_nsa_band", "ZCELLINFO_band",
        "Z5g_rsrp", "nr5g_rsrp", "Z5g_SINR", "nr5g_pci", "Z_PCI", "Z5g_dlEarfcn"
    )
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("تشخيص القراءة", color = PremiumInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("هذه القيم تساعدنا على مطابقة Firmware جهازك، ولا تتضمن كلمة مرور الراوتر.", color = PremiumMuted, fontSize = 10.sp)
            Spacer(Modifier.height(10.dp))
            keys.forEach { key ->
                val value = snapshot.raw[key].orEmpty()
                if (value.isNotBlank()) PremiumInfoRow(key, value)
            }
        }
    }
}

@Composable
private fun PremiumStatusStrip(message: String) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(PremiumGold.copy(alpha = 0.12f)).padding(13.dp)
    ) {
        Text(message, color = PremiumInk, fontSize = 11.sp)
    }
}

@Composable
private fun PremiumSmallButton(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 5.dp, vertical = 10.dp)) {
        Text(text, color = PremiumInk, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PremiumPrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PremiumGoldDark, disabledContainerColor = PremiumLine)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PremiumCard(modifier: Modifier = Modifier, warm: Boolean = false, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Card(
        modifier = modifier.shadow(9.dp, shape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = if (warm) PremiumGoldDark else PremiumSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) { content() }
}

@Composable
private fun PremiumInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PremiumMuted, fontSize = 9.sp, modifier = Modifier.weight(0.48f))
        Text(value, color = PremiumInk, fontSize = 9.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(0.52f))
    }
}

private data class PremiumRadioState(val primary: String, val secondary: String)

private fun premiumRadioState(snapshot: RouterSnapshot): PremiumRadioState {
    val mode = snapshot.raw["_zte_radio_mode"].orEmpty()
    val nrActive = snapshot.raw["_zte_nr_active"].equals("true", true)
    val ca = snapshot.caActive
    return when {
        mode == "NSA_ACTIVE" || (nrActive && premiumCurrentLteBands(snapshot).isNotEmpty()) -> PremiumRadioState("5G NSA", "NR نشط مع مرساة 4G")
        mode == "SA_ACTIVE" || (nrActive && premiumCurrentLteBands(snapshot).isEmpty()) -> PremiumRadioState("5G SA", "اتصال 5G مستقل")
        mode == "NSA_STANDBY" -> PremiumRadioState(if (ca) "4G+" else "4G", "5G NSA جاهز • NR غير نشط الآن")
        ca -> PremiumRadioState("4G+", "دمج ترددات LTE نشط")
        else -> PremiumRadioState("4G", snapshot.networkType ?: "LTE")
    }
}

private fun premiumActiveBandCombo(snapshot: RouterSnapshot): String {
    val lte = premiumCurrentLteBands(snapshot).sorted().map { "B$it" }
    val nr = premiumCurrentNrBands(snapshot).sorted().map { "N$it" }
    return (nr + lte).joinToString("+").ifBlank { "جاري القراءة" }
}

private fun premiumAggregationText(snapshot: RouterSnapshot): String {
    val lte = premiumCurrentLteBands(snapshot).sorted()
    val nr = premiumCurrentNrBands(snapshot).sorted()
    return when {
        nr.isNotEmpty() && lte.isNotEmpty() -> "5G NSA + ${lte.size} LTE carrier${if (lte.size > 1) "s" else ""}"
        nr.isNotEmpty() -> "5G NR نشط"
        snapshot.caActive && lte.size >= 2 -> "دمج 4G فعلي • ${lte.size}CA"
        snapshot.caActive -> "دمج 4G فعلي • CA"
        lte.isNotEmpty() -> "4G بدون دمج نشط"
        else -> "الحالة قيد التحقق"
    }
}

fun premiumCurrentLteBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
    premiumExtractBand(snapshot.lteBand)?.let(::add)
    snapshot.cells.filter { it.role != CellRole.NR }.forEach { premiumExtractBand(it.band)?.let(::add) }
}

fun premiumCurrentNrBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
    premiumExtractBand(snapshot.nrBand)?.let(::add)
    snapshot.cells.filter { it.role == CellRole.NR }.forEach { premiumExtractBand(it.band)?.let(::add) }
}

fun premiumToggleBand(current: Set<Int>, band: Int): Set<Int> = if (band in current) current - band else current + band

private fun premiumExtractBand(value: String?): Int? = Regex("\\d+").find(value.orEmpty())?.value?.toIntOrNull()
private fun premiumFormatBands(bands: Set<Int>): String = bands.sorted().joinToString("+") { "B$it" }.ifBlank { "تلقائي" }
private fun premiumFormatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

private fun premiumGoalLabel(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun premiumPlacementGuidance(guidance: PlacementGuidance): String = when (guidance) {
    PlacementGuidance.INITIAL -> "حرّك الراوتر ببطء عدة سنتيمترات"
    PlacementGuidance.MUCH_BETTER -> "تحسن واضح — استمر في هذا الاتجاه"
    PlacementGuidance.BETTER -> "أفضل — استمر ببطء"
    PlacementGuidance.STABLE -> "الفرق بسيط — جرّب حركة صغيرة"
    PlacementGuidance.WORSE -> "أسوأ — ارجع قليلًا"
    PlacementGuidance.RETURN_TO_BEST -> "ابتعدت عن أفضل نقطة — ارجع للمكان السابق"
    PlacementGuidance.CELL_CHANGED_WORSE -> "انتقلت لخلية أضعف — ارجع قليلًا"
    PlacementGuidance.EXCELLENT_HOLD -> "ممتاز — ثبّت الراوتر هنا"
    PlacementGuidance.BEST_SO_FAR -> "هذه أفضل نقطة حتى الآن"
}

@Composable
private fun PremiumPlacementAudio(reading: PlacementReading) {
    val haptic = LocalHapticFeedback.current
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 48) }
    DisposableEffect(Unit) { onDispose { tone.release() } }

    LaunchedEffect(reading.guidance) {
        when (reading.guidance) {
            PlacementGuidance.BEST_SO_FAR, PlacementGuidance.EXCELLENT_HOLD -> {
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 100)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            PlacementGuidance.WORSE, PlacementGuidance.RETURN_TO_BEST, PlacementGuidance.CELL_CHANGED_WORSE -> tone.startTone(ToneGenerator.TONE_PROP_NACK, 90)
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
