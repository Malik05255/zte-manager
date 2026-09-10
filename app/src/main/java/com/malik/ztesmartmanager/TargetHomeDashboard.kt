package com.malik.ztesmartmanager

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import java.util.Locale
import kotlin.math.max

private val THBg = Color(0xFFF7FAFE)
private val THCard = Color.White
private val THNavy = Color(0xFF0A2C67)
private val THBlue = Color(0xFF1478F8)
private val THCyan = Color(0xFF35C7E5)
private val THGreen = Color(0xFF15C986)
private val THSoftGreen = Color(0xFFEAFBF4)
private val THMuted = Color(0xFF71809A)
private val THSoft = Color(0xFFF2F7FD)
private val THBorder = Color(0xFFE7EDF5)
private val THPurple = Color(0xFF9F63F4)
private val THCardShape = RoundedCornerShape(18.dp)
private val THSmallShape = RoundedCornerShape(13.dp)

@Composable
fun TargetHomeDashboard(
    snapshot: RouterSnapshot,
    telemetrySamples: List<SafeTelemetrySample>,
    status: String,
    operationMessage: String,
    lastPerformance: NetworkPerformance?,
    speedBusy: Boolean,
    controlBusy: Boolean,
    smartBusy: Boolean,
    onDisconnect: () -> Unit,
    onSpeedTest: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onNavigateNetwork: () -> Unit,
    onNavigateTowers: () -> Unit,
    onNavigateBands: () -> Unit,
    onNavigateTools: () -> Unit,
    onNavigateLogs: () -> Unit,
    onNavigateMore: () -> Unit,
    onRefreshNow: () -> Unit,
    onOptimizeNow: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(Modifier.fillMaxSize().background(THBg)) {
            val compact = maxWidth < 365.dp
            val side = if (compact) 10.dp else 12.dp
            val gap = if (compact) 5.dp else 6.dp

            Column(Modifier.fillMaxSize()) {
                HaiSharedHeader(
                    connected = status.contains("متصل") || snapshot.networkType != null,
                    onDisconnect = onDisconnect,
                    onMenu = onNavigateMore,
                    onSettings = onNavigateTools,
                    onSearch = onNavigateTowers
                )

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = side)
                ) {
                    TargetHero(snapshot, Modifier.fillMaxWidth().weight(151f))
                    Spacer(Modifier.height(gap))
                    TargetSignalMetricsRow(snapshot, Modifier.fillMaxWidth().weight(56f))
                    Spacer(Modifier.height(gap))

                    Row(
                        modifier = Modifier.fillMaxWidth().weight(131f),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        TargetSpeedCard(
                            performance = lastPerformance,
                            busy = speedBusy,
                            onSpeedTest = onSpeedTest,
                            modifier = Modifier.weight(1.25f).fillMaxHeight()
                        )
                        TargetNetworkModeCard(
                            snapshot = snapshot,
                            busy = controlBusy,
                            onSetNetworkMode = onSetNetworkMode,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    Spacer(Modifier.height(gap))

                    Row(
                        modifier = Modifier.fillMaxWidth().weight(118f),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        TargetTowerCard(snapshot, onNavigateTowers, Modifier.weight(1.25f).fillMaxHeight())
                        TargetLiveSignalCard(snapshot, telemetrySamples, Modifier.weight(1f).fillMaxHeight())
                    }
                    Spacer(Modifier.height(gap))

                    Row(
                        modifier = Modifier.fillMaxWidth().weight(92f),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        TargetActiveBandsCard(snapshot, onNavigateBands, Modifier.weight(1.25f).fillMaxHeight())
                        TargetQuickToolsCard(
                            smartBusy = smartBusy,
                            onOptimize = onOptimizeNow,
                            onDiagnostics = onNavigateTools,
                            onBands = onNavigateBands,
                            onRefresh = onRefreshNow,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    if (operationMessage.isNotBlank()) {
                        Text(
                            text = operationMessage,
                            color = THMuted,
                            fontSize = 6.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        )
                    }
                }

                HaiSharedBottomNav(
                    selected = "home",
                    onHome = {},
                    onNetwork = onNavigateNetwork,
                    onTools = onNavigateTools,
                    onLogs = onNavigateLogs,
                    onMore = onNavigateMore
                )
            }
        }
    }
}

@Composable
private fun TargetHero(snapshot: RouterSnapshot, modifier: Modifier) {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    val network = when { nrVerified -> "5G"; lteVerified -> "4G"; else -> "—" }
    val mode = when {
        nrVerified && snapshot.networkType.orEmpty().contains("SA", true) &&
            !snapshot.networkType.orEmpty().contains("NSA", true) -> "SA"
        nrVerified -> "NSA"
        lteVerified -> "LTE"
        else -> "غير مؤكد"
    }
    val rsrp = if (nrVerified) snapshot.nrRsrp else snapshot.lteRsrp
    val bands = activeBandLabels(snapshot)
    val aggregation = aggregationSummary(snapshot, bands)

    Card(
        modifier = modifier,
        shape = THCardShape,
        colors = CardDefaults.cardColors(containerColor = THCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.fillMaxSize()) {
            TargetRouterIllustration(Modifier.fillMaxHeight().weight(.29f))
            Column(Modifier.fillMaxHeight().weight(.71f).padding(9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("حالة الشبكة", color = THNavy, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(4.dp))
                            TargetSmallBars(THBlue, Modifier.size(15.dp, 16.dp))
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(15.dp))
                                .background(if (rsrp != null) THSoftGreen else THSoft)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                targetQuality(rsrp),
                                color = if (rsrp != null) Color(0xFF079B64) else THMuted,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            TargetSmallBars(if (rsrp != null) THGreen else THMuted, Modifier.size(12.dp, 10.dp))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (nrVerified || lteVerified) "أنت متصل بالإنترنت" else "لم يتم إثبات اتصال الراديو",
                            color = THMuted,
                            fontSize = 7.4.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = network,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            style = androidx.compose.material3.LocalTextStyle.current.copy(
                                brush = Brush.linearGradient(listOf(THBlue, THCyan))
                            )
                        )
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFE9F2FF))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(mode, color = THBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (network == "—") "الشبكة الحالية غير مؤكدة" else "الشبكة الحالية: $network $mode",
                            color = THNavy,
                            fontSize = 6.7.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                        .clip(RoundedCornerShape(15.dp)).background(Color(0xFFFAFCFF))
                        .border(.7.dp, THBorder, RoundedCornerShape(15.dp)).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1.28f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الترددات المتصلة الآن", color = THNavy, fontSize = 8.2.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(3.dp)); Text("⌁", color = THBlue, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (bands.isEmpty()) {
                                TargetBandMiniChip("—", "غير مؤكد", Color(0xFFE8F2FF))
                            } else {
                                bands.take(3).forEachIndexed { index, band ->
                                    TargetBandMiniChip(
                                        band,
                                        if (band.startsWith("n", true)) "5G" else "LTE",
                                        listOf(Color(0xFFE5F2FF), Color(0xFFF3EDFF), Color(0xFFE8F8EF))[index % 3]
                                    )
                                }
                            }
                        }
                    }

                    Box(Modifier.width(1.dp).height(38.dp).background(THBorder))
                    Spacer(Modifier.width(5.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الدمج النشط", color = THNavy, fontSize = 8.2.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(3.dp)); Text("▰", color = THBlue, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFFEAF3FF)).padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                aggregation.carriers,
                                color = THNavy,
                                fontSize = 7.3.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            aggregation.badge?.let { badge ->
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    Modifier.clip(RoundedCornerShape(8.dp)).background(THBlue)
                                        .padding(horizontal = 5.dp, vertical = 3.dp)
                                ) {
                                    Text(badge, color = Color.White, fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetRouterIllustration(modifier: Modifier) {
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(Color(0xFF43A8FF), Color(0xFFE9F5FF)))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val cx = size.width * .50f
            val cy = size.height * .43f
            repeat(3) { index ->
                drawCircle(
                    color = Color.White.copy(alpha = .18f),
                    radius = 35f + index * 26f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
            }
            val routerW = size.width * .43f
            val routerH = size.height * .62f
            val left = cx - routerW / 2f
            val top = cy - routerH / 2f
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color.White, Color(0xFFDCE5EE))),
                topLeft = Offset(left, top),
                size = Size(routerW, routerH),
                cornerRadius = CornerRadius(18f)
            )
            repeat(4) { index ->
                drawCircle(
                    color = if (index < 3) THGreen else Color(0xFFB8C4D4),
                    radius = 3f,
                    center = Offset(left + routerW * .80f, top + routerH * (.35f + index * .09f))
                )
            }
        }
        Text("ZTE", color = Color(0xFF9CA9BB), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TargetBandMiniChip(title: String, subtitle: String, color: Color) {
    Column(
        modifier = Modifier.width(35.dp).clip(RoundedCornerShape(9.dp)).background(color).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = THNavy, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(subtitle, color = THBlue, fontSize = 6.2.sp, maxLines = 1)
    }
}

@Composable
private fun TargetSignalMetricsRow(snapshot: RouterSnapshot, modifier: Modifier) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val sinr = if (nr) snapshot.nrSinr else snapshot.lteSinr
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TargetMetricCard("RSRP", rsrp, "dBm", THBlue, Modifier.weight(1f).fillMaxHeight())
        TargetMetricCard("SINR", sinr, "dB", THGreen, Modifier.weight(1f).fillMaxHeight())
        TargetMetricCard("RSRQ", snapshot.lteRsrq, "dB", THBlue, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun TargetMetricCard(title: String, value: Double?, unit: String, iconColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = THSmallShape,
        colors = CardDefaults.cardColors(containerColor = THCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            TargetSmallBars(iconColor, Modifier.size(12.dp, 16.dp))
            Spacer(Modifier.width(7.dp))
            Column {
                Text(title, color = THMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                Text(value?.let(::tf1) ?: "—", color = THNavy, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(unit, color = THMuted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun TargetSpeedCard(performance: NetworkPerformance?, busy: Boolean, onSpeedTest: () -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = THCardShape,
        colors = CardDefaults.cardColors(containerColor = THCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(performance?.measuredAtEpochMs?.let(::relativeTestTime) ?: "لم يُجر اختبار بعد", color = THMuted, fontSize = 6.5.sp)
                    performance?.latencyMs?.let { Text("زمن الاستجابة ${tf0(it)} ms", color = THMuted, fontSize = 6.sp) }
                }
                Spacer(Modifier.weight(1f))
                Text("اختبار السرعة", color = THNavy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(4.dp)); Text("◴", color = THBlue, fontSize = 14.sp)
            }
            Spacer(Modifier.height(3.dp))
            Row(Modifier.weight(1f)) {
                TargetSpeedGauge(performance?.downloadMbps, Modifier.weight(1f).fillMaxHeight())
                Column(
                    modifier = Modifier.width(52.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("↥", color = THPurple, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(performance?.uploadMbps?.let(::tf1) ?: "—", color = THNavy, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text("Mb/s", color = THMuted, fontSize = 7.5.sp)
                    Text("رفع", color = THMuted, fontSize = 6.2.sp)
                }
            }
            Button(
                onClick = onSpeedTest,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(29.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = THBlue),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(if (busy) "جاري الاختبار…" else "▶  بدء الاختبار", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TargetSpeedGauge(downloadMbps: Double?, modifier: Modifier) {
    val fraction = ((downloadMbps ?: 0.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(67.dp)) {
            val stroke = max(10f, size.width * .055f)
            val arcSize = Size(size.width * .92f, size.height * 1.60f)
            val topLeft = Offset(size.width * .04f, size.height * .16f)
            drawArc(Color(0xFFDDE6F1), 180f, 180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                brush = Brush.sweepGradient(listOf(THBlue, Color(0xFF2E9AFB), Color(0xFF34D5CC))),
                startAngle = 180f,
                sweepAngle = 180f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 9.dp)) {
            Text("↓", color = THBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(downloadMbps?.let(::tf1) ?: "—", color = THNavy, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text("Mb/s تنزيل", color = THMuted, fontSize = 7.sp)
        }
    }
}

@Composable
private fun TargetNetworkModeCard(snapshot: RouterSnapshot, busy: Boolean, onSetNetworkMode: (String) -> Unit, modifier: Modifier) {
    val current = snapshot.raw["BearerPreference"].orEmpty()
    Card(
        modifier = modifier,
        shape = THCardShape,
        colors = CardDefaults.cardColors(containerColor = THCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("وضع الشبكة", color = THNavy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f)); Text("⌁", color = THBlue, fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TargetModeButton("3G فقط", current == "Only_WCDMA", false, busy, Modifier.weight(1f)) { onSetNetworkMode("Only_WCDMA") }
                TargetModeButton("4G فقط", current == "Only_LTE", false, busy, Modifier.weight(1f)) { onSetNetworkMode("Only_LTE") }
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TargetModeButton("5G فقط", current == "Only_5G", false, busy, Modifier.weight(1f)) { onSetNetworkMode("Only_5G") }
                TargetModeButton("تلقائي", current == "WL_AND_5G" || current == "LTE_AND_5G", true, busy, Modifier.weight(1f)) { onSetNetworkMode("WL_AND_5G") }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = when (current) {
                    "Only_WCDMA" -> "الوضع الحالي: 3G فقط"
                    "Only_LTE" -> "الوضع الحالي: 4G فقط"
                    "Only_5G" -> "الوضع الحالي: 5G فقط"
                    "WL_AND_5G", "LTE_AND_5G" -> "يتم اختيار أفضل شبكة تلقائيًا"
                    else -> "الوضع الحالي غير مقروء"
                },
                color = THMuted,
                fontSize = 5.8.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TargetModeButton(title: String, selected: Boolean, automatic: Boolean, busy: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) THBlue else THSoft,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(.5.dp, THBorder)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (automatic) {
                Box(Modifier.size(18.dp).border(1.3.dp, if (selected) Color.White else THMuted, CircleShape), contentAlignment = Alignment.Center) {
                    Text("A", color = if (selected) Color.White else THMuted, fontSize = 7.5.sp, fontWeight = FontWeight.Black)
                }
            } else {
                TargetSmallBars(if (selected) Color.White else THNavy, Modifier.size(15.dp, 14.dp))
            }
            Spacer(Modifier.height(3.dp))
            Text(title, color = if (selected) Color.White else THNavy, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TargetTowerCard(snapshot: RouterSnapshot, onOpen: () -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onOpen),
        shape = THCardShape,
        colors = CardDefaults.cardColors(containerColor = THCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("⌾", color = THBlue, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f)); Text("أقرب برج شبكة", color = THNavy, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.weight(1f)) {
                Column(Modifier.width(83.dp), verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.End) {
                    TargetInfoLine("معرّف الخلية", snapshot.cellId?.toString() ?: "—")
                    TargetInfoLine("PCI", snapshot.pci?.toString() ?: "—")
                    TargetInfoLine("EARFCN", snapshot.earfcn?.toString() ?: "—")
                }
                Spacer(Modifier.width(6.dp))
                TargetMiniMap(Modifier.weight(1f).fillMaxHeight(), snapshot.cellId?.toString() ?: "غير مؤكد")
            }
            Spacer(Modifier.height(4.dp))
            Surface(
                modifier = Modifier.align(Alignment.End).height(21.dp).width(108.dp),
                onClick = onOpen,
                shape = RoundedCornerShape(11.dp),
                color = Color(0xFFEEF5FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("عرض على الخريطة", color = THBlue, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TargetInfoLine(title: String, value: String) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = THMuted, fontSize = 6.1.sp)
        Text(value, color = THNavy, fontSize = 7.2.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TargetMiniMap(modifier: Modifier, label: String) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4F7F5))) {
        Canvas(Modifier.fillMaxSize()) {
            val road = Color(0xFFE2E6E8)
            repeat(6) { i -> drawLine(road, Offset(0f, size.height * (i + 1) / 7f), Offset(size.width, size.height * (i + 1) / 7f + 16f), 2.5f) }
            repeat(5) { i -> drawLine(road, Offset(size.width * (i + 1) / 6f, 0f), Offset(size.width * (i + 1) / 6f - 14f, size.height), 2.5f) }
            drawCircle(THBlue.copy(alpha = .18f), 34f, Offset(size.width * .55f, size.height * .50f))
            drawCircle(THBlue, 10f, Offset(size.width * .55f, size.height * .50f))
        }
        Text("⌾", color = THBlue, fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center).offset(y = (-8).dp))
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(label, color = THNavy, fontSize = 6.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TargetLiveSignalCard(snapshot: RouterSnapshot, samples: List<SafeTelemetrySample>, modifier: Modifier) {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val rsrp = if (nr) snapshot.nrRsrp else snapshot.lteRsrp
    val values = samples.mapNotNull { if (it.nrVerified) it.nrRsrp else it.lteRsrp }.takeLast(30)
    val stable = values.takeLast(6).let { it.size >= 3 && (it.maxOrNull()!! - it.minOrNull()!!) <= 8.0 }

    Card(
        modifier = modifier,
        shape = THCardShape,
        colors = CardDefaults.cardColors(containerColor = THCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("مراقبة الإشارة المباشرة", color = THNavy, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f)); Text("⌁", color = THBlue, fontSize = 14.sp)
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rsrp?.let { "${tf0(it)} dBm" } ?: "—", color = THNavy, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(if (stable) THSoftGreen else THSoft)
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (stable) "مستقر" else "حي", color = if (stable) Color(0xFF079B64) else THBlue, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp)); Box(Modifier.size(6.dp).background(if (stable) THGreen else THBlue, CircleShape))
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().weight(1f)) {
                TargetSignalGraph(values, Modifier.fillMaxSize().padding(end = 17.dp))
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("-60", color = THMuted, fontSize = 5.5.sp)
                    Text("-80", color = THMuted, fontSize = 5.5.sp)
                    Text("-100", color = THMuted, fontSize = 5.5.sp)
                    Text("-120", color = THMuted, fontSize = 5.5.sp)
                }
            }
            Text(
                if (values.isEmpty()) "بانتظار القراءات الحية" else "آخر ${values.size * 2} ثانية تقريبًا",
                color = THMuted,
                fontSize = 5.8.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun TargetSignalGraph(values: List<Double>, modifier: Modifier) {
    Canvas(modifier) {
        val grid = Color(0xFFE2EDF8)
        repeat(7) { i -> val x = size.width * i / 6f; drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f) }
        repeat(4) { i -> val y = size.height * i / 3f; drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f) }
        if (values.size < 2) return@Canvas
        val line = Path()
        val area = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.size - 1).coerceAtLeast(1)
            val y = size.height * ((-60.0 - value) / 60.0).coerceIn(0.0, 1.0).toFloat()
            if (index == 0) {
                line.moveTo(x, y); area.moveTo(x, size.height); area.lineTo(x, y)
            } else {
                line.lineTo(x, y); area.lineTo(x, y)
            }
            if (index == values.lastIndex) { area.lineTo(x, size.height); area.close() }
        }
        drawPath(area, Brush.verticalGradient(listOf(THBlue.copy(alpha = .17f), THBlue.copy(alpha = .01f))))
        drawPath(line, THBlue, style = Stroke(2.5f, cap = StrokeCap.Round))
    }
}

@Composable
private fun TargetActiveBandsCard(snapshot: RouterSnapshot, onOpen: () -> Unit, modifier: Modifier) {
    val bands = activeBandLabels(snapshot).take(6)
    Card(
        modifier = modifier,
        shape = THCardShape,
        colors = CardDefaults.cardColors(containerColor = THCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
                Text("⌁", color = THBlue, fontSize = 14.sp)
                Spacer(Modifier.weight(1f)); Text("الترددات النشطة", color = THNavy, fontSize = 9.8.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(5.dp)); Text("عرض الكل ‹", color = THBlue, fontSize = 6.8.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (bands.isEmpty()) {
                    TargetActiveBand("—", "غير مؤكد", Color(0xFFEAF1FF), Modifier.weight(1f))
                    repeat(5) { Spacer(Modifier.weight(1f)) }
                } else {
                    bands.forEachIndexed { index, band ->
                        TargetActiveBand(
                            band,
                            if (band.startsWith("n", true)) "5G" else "LTE",
                            listOf(Color(0xFFE5F2FF), Color(0xFFE8F9F4), Color(0xFFEAF9F0), Color(0xFFF4EDFF), Color(0xFFFFF3D9), Color(0xFFEAF1FF))[index % 6],
                            Modifier.weight(1f)
                        )
                    }
                    repeat(6 - bands.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TargetActiveBand(name: String, tech: String, bg: Color, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(bg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(name, color = THNavy, fontSize = 8.3.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(tech, color = if (tech == "5G") THBlue else THMuted, fontSize = 6.sp, maxLines = 1)
    }
}

@Composable
private fun TargetQuickToolsCard(
    smartBusy: Boolean,
    onOptimize: () -> Unit,
    onDiagnostics: () -> Unit,
    onBands: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = THCardShape,
        colors = CardDefaults.cardColors(containerColor = THCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f)); Text("أدوات سريعة", color = THNavy, fontSize = 9.8.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(4.dp)); Text("▦", color = THBlue, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TargetQuickTool("🚀", if (smartBusy) "جاري\nالتحسين" else "تحسين\nالأداء", Color(0xFFF6EDFF), Modifier.weight(1f), !smartBusy, onOptimize)
                TargetQuickTool("⌕", "تشخيص\nالشبكة", Color(0xFFE8FBF5), Modifier.weight(1f), true, onDiagnostics)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TargetQuickTool("▣", "قفل\nالترددات", Color(0xFFE8FAF1), Modifier.weight(1f), true, onBands)
                TargetQuickTool("↻", "تحديث\nالبيانات", Color(0xFFF5EEFF), Modifier.weight(1f), true, onRefresh)
            }
        }
    }
}

@Composable
private fun TargetQuickTool(icon: String, text: String, background: Color, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(9.dp)).background(background)
            .clickable(enabled = enabled, onClick = onClick).padding(3.dp)
    ) {
        Text(icon, color = THBlue, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterStart))
        Text(
            text = text,
            color = if (enabled) THNavy else THMuted,
            fontSize = 6.2.sp,
            lineHeight = 6.6.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun TargetSmallBars(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val gap = size.width * .08f
        val barWidth = (size.width - gap * 3f) / 4f
        listOf(.32f, .52f, .76f, 1f).forEachIndexed { index, factor ->
            val h = size.height * factor
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

private data class AggregationSummary(val carriers: String, val badge: String?)

private fun aggregationSummary(snapshot: RouterSnapshot, bands: List<String>): AggregationSummary {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val caVerified = snapshot.raw["_zte_ca_verified"].equals("true", true)
    val lteCarriers = snapshot.cells.filter { it.role != CellRole.NR }.distinctBy { Triple(it.band, it.pci, it.arfcn) }.size
    val nrCarriers = snapshot.cells.filter { it.role == CellRole.NR }.distinctBy { Triple(it.band, it.pci, it.arfcn) }.size
    val carriers = bands.take(4).joinToString(" + ").ifBlank { "غير مؤكد" }
    val badge = when {
        snapshot.caActive && caVerified && lteCarriers >= 2 && nrVerified && nrCarriers >= 1 -> "${lteCarriers}CA + NR"
        snapshot.caActive && caVerified && lteCarriers >= 2 -> "${lteCarriers}CA"
        nrVerified && lteCarriers >= 1 -> "NSA"
        else -> null
    }
    return AggregationSummary(carriers, badge)
}

private fun activeBandLabels(snapshot: RouterSnapshot): List<String> {
    val out = LinkedHashSet<String>()
    if (snapshot.raw["_zte_nr_active_verified"].equals("true", true)) normalizeBand(snapshot.nrBand, CellRole.NR)?.let(out::add)
    snapshot.cells.forEach { normalizeBand(it.band, it.role)?.let(out::add) }
    if (snapshot.raw["_zte_lte_active_verified"].equals("true", true)) normalizeBand(snapshot.lteBand, CellRole.PRIMARY)?.let(out::add)
    return out.toList()
}

private fun normalizeBand(raw: String?, role: CellRole): String? {
    val n = Regex("\\d+").find(raw.orEmpty())?.value ?: return null
    return if (role == CellRole.NR || raw.orEmpty().startsWith("n", true)) "n$n" else "B$n"
}

private fun targetQuality(rsrp: Double?): String = when {
    rsrp == null -> "غير مؤكد"
    rsrp >= -80 -> "إشارة ممتازة"
    rsrp >= -90 -> "إشارة جيدة جدًا"
    rsrp >= -100 -> "إشارة جيدة"
    rsrp >= -110 -> "إشارة ضعيفة"
    else -> "إشارة ضعيفة جدًا"
}

private fun relativeTestTime(epochMs: Long): String {
    val minutes = ((System.currentTimeMillis() - epochMs).coerceAtLeast(0L) / 60_000L).toInt()
    return when {
        minutes <= 0 -> "آخر اختبار: الآن"
        minutes == 1 -> "آخر اختبار: منذ دقيقة"
        minutes < 11 -> "آخر اختبار: منذ $minutes دقائق"
        else -> "آخر اختبار: منذ $minutes دقيقة"
    }
}

private fun tf1(v: Double) = String.format(Locale.US, "%.1f", v)
private fun tf0(v: Double) = String.format(Locale.US, "%.0f", v)
