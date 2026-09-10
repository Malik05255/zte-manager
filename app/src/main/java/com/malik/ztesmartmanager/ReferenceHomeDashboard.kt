package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val RefNavy = Color(0xFF0A2C67)
private val RefBlue = Color(0xFF1478F8)
private val RefBlue2 = Color(0xFF39C8E8)
private val RefGreen = Color(0xFF15C986)
private val RefSoftGreen = Color(0xFFEAFBF4)
private val RefText2 = Color(0xFF71809A)
private val RefSurface = Color.White
private val RefPage = Color(0xFFF7FAFE)
private val RefSoftBlue = Color(0xFFF2F7FD)
private val RefBorder = Color(0xFFE8EEF6)
private val RefPurple = Color(0xFF9F63F4)
private val RefOrange = Color(0xFFF7A64E)

private val RefCardShape = RoundedCornerShape(18.dp)
private val RefSmallShape = RoundedCornerShape(13.dp)

@Composable
fun ReferenceHomeDashboard(
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
    onOptimizeNow: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(RefPage).statusBarsPadding().navigationBarsPadding()
    ) {
        val designWidth = 393.dp
        val designHeight = 699.dp
        val scaleX = maxWidth.value / designWidth.value
        val scaleY = maxHeight.value / designHeight.value
        val uiScale = scaleX

        Box(
            modifier = Modifier
                .size(designWidth, designHeight)
                .align(Alignment.TopCenter)
                .scale(uiScale)
        ) {
            ReferenceDashboardContent(
                snapshot = snapshot,
                telemetrySamples = telemetrySamples,
                status = status,
                operationMessage = operationMessage,
                lastPerformance = lastPerformance,
                speedBusy = speedBusy,
                controlBusy = controlBusy,
                smartBusy = smartBusy,
                onDisconnect = onDisconnect,
                onSpeedTest = onSpeedTest,
                onSetNetworkMode = onSetNetworkMode,
                onNavigateNetwork = onNavigateNetwork,
                onNavigateTowers = onNavigateTowers,
                onNavigateBands = onNavigateBands,
                onNavigateTools = onNavigateTools,
                onOptimizeNow = onOptimizeNow
            )
        }
    }
}

@Composable
private fun ReferenceDashboardContent(
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
    onOptimizeNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RefPage)
            .padding(horizontal = 10.dp)
    ) {
        ReferenceHeader(
            connected = status.contains("متصل") || snapshot.networkType != null,
            onDisconnect = onDisconnect,
            onTools = onNavigateTools,
            onSearch = onNavigateTowers,
            onMenu = onNavigateBands
        )

        Spacer(Modifier.height(5.dp))

        ReferenceNetworkHeroCard(snapshot)

        Spacer(Modifier.height(5.dp))

        ReferenceSignalMetricsRow(snapshot)

        Spacer(Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(131.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReferenceSpeedCard(
                modifier = Modifier.weight(1.25f),
                performance = lastPerformance,
                busy = speedBusy,
                onSpeedTest = onSpeedTest
            )
            ReferenceNetworkModeCard(
                modifier = Modifier.weight(1f),
                snapshot = snapshot,
                busy = controlBusy,
                onSetNetworkMode = onSetNetworkMode
            )
        }

        Spacer(Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(118.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReferenceTowerCard(
                modifier = Modifier.weight(1.25f),
                snapshot = snapshot,
                onOpenTowers = onNavigateTowers
            )
            ReferenceLiveSignalCard(
                modifier = Modifier.weight(1f),
                snapshot = snapshot,
                telemetrySamples = telemetrySamples
            )
        }

        Spacer(Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(92.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReferenceActiveBandsCard(
                modifier = Modifier.weight(1.25f),
                snapshot = snapshot,
                onOpenBands = onNavigateBands
            )
            ReferenceQuickToolsCard(
                modifier = Modifier.weight(1f),
                smartBusy = smartBusy,
                onOptimize = onOptimizeNow,
                onDiagnostics = onNavigateTools,
                onBands = onNavigateBands,
                onNetwork = onNavigateNetwork
            )
        }

        if (operationMessage.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = operationMessage,
                color = RefText2,
                fontSize = 6.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(1f))

        ReferenceBottomNavigation(
            onNetwork = onNavigateNetwork,
            onTools = onNavigateTools,
            onTowers = onNavigateTowers,
            onBands = onNavigateBands
        )
    }
}

@Composable
private fun ReferenceHeader(
    connected: Boolean,
    onDisconnect: () -> Unit,
    onTools: () -> Unit,
    onSearch: () -> Unit,
    onMenu: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ReferenceHeaderCircle("⚙", onTools)
            ReferenceHeaderCircle("⌕", onSearch)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ZTE Smart HAI",
                color = RefNavy,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Text(
                text = "إدارة شبكتك ... بكل سهولة",
                color = RefText2,
                fontSize = 8.5.sp
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .height(29.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (connected) RefSoftGreen else Color(0xFFFFEEEE))
                    .clickable(enabled = connected, onClick = onDisconnect)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier.size(7.dp).background(
                        if (connected) RefGreen else Color(0xFFE15363),
                        CircleShape
                    )
                )
                Text(
                    text = if (connected) "متصل" else "غير متصل",
                    color = RefNavy,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(7.dp))
            Text(
                text = "☰",
                color = RefNavy,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable(onClick = onMenu).padding(2.dp)
            )
        }
    }
}

@Composable
private fun ReferenceHeaderCircle(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(33.dp)
            .clip(CircleShape)
            .background(Color(0xFFF0F5FB))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = RefNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReferenceNetworkHeroCard(snapshot: RouterSnapshot) {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", ignoreCase = true)
    val lteVerified = snapshot.raw["_zte_lte_active_verified"].equals("true", ignoreCase = true)
    val network = when {
        nrVerified -> "5G"
        lteVerified -> "4G"
        else -> "—"
    }
    val mode = when {
        nrVerified && snapshot.networkType.orEmpty().contains("SA", true) && !snapshot.networkType.orEmpty().contains("NSA", true) -> "SA"
        nrVerified -> "NSA"
        lteVerified -> "LTE"
        else -> "غير مؤكد"
    }
    val rsrp = if (nrVerified) snapshot.nrRsrp else snapshot.lteRsrp
    val bands = connectedBandLabels(snapshot)
    val caVerified = snapshot.raw["_zte_ca_verified"].equals("true", ignoreCase = true)
    val caBands = if (caVerified || snapshot.caActive) bands else emptyList()

    Card(
        modifier = Modifier.fillMaxWidth().height(151.dp),
        shape = RefCardShape,
        colors = CardDefaults.cardColors(containerColor = RefSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            ReferenceRouterIllustration(
                modifier = Modifier.fillMaxHeight().width(105.dp)
            )

            Column(modifier = Modifier.weight(1f).padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("حالة الشبكة", color = RefNavy, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (rsrp != null) RefSoftGreen else RefSoftBlue)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = qualityLabel(rsrp),
                                color = if (rsrp != null) Color(0xFF009B63) else RefText2,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            ReferenceSignalBars(
                                color = if (rsrp != null) RefGreen else RefText2,
                                width = 13.dp,
                                height = 10.dp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (nrVerified || lteVerified) "أنت متصل بالإنترنت" else "لم يتم إثبات اتصال الراديو",
                            color = RefText2,
                            fontSize = 8.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = network,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            style = androidx.compose.material3.LocalTextStyle.current.copy(
                                brush = Brush.linearGradient(listOf(RefBlue, RefBlue2))
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE9F2FF))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(mode, color = RefBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(7.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(49.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFFAFCFF))
                        .border(0.7.dp, RefBorder, RoundedCornerShape(15.dp))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.35f)) {
                        Text("الترددات المتصلة الآن", color = RefNavy, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (bands.isEmpty()) {
                                ReferenceBandMiniChip("—", "غير مؤكد", RefSoftBlue)
                            } else {
                                bands.take(3).forEachIndexed { index, band ->
                                    ReferenceBandMiniChip(
                                        title = band,
                                        subtitle = if (band.startsWith("N", true)) "5G" else "LTE",
                                        color = when (index) {
                                            0 -> Color(0xFFE6F2FF)
                                            1 -> Color(0xFFF4EAFE)
                                            else -> Color(0xFFE9FAF0)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box(Modifier.width(1.dp).height(37.dp).background(RefBorder))
                    Spacer(Modifier.width(5.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("الدمج النشط", color = RefNavy, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEAF3FF))
                                .padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = caBands.take(3).joinToString(" + ").ifBlank { "غير مؤكد" },
                                color = RefNavy,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 7.8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (caBands.size >= 2) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RefBlue)
                                        .padding(horizontal = 5.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${caBands.size}CA",
                                        color = Color.White,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
                                    )
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
private fun ReferenceRouterIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFF4CA8FF), Color(0xFFE9F5FF)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val cx = size.width * .50f
            val cy = size.height * .43f
            repeat(3) { index ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = 35f + index * 26f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f)
                )
            }
            val routerW = size.width * .42f
            val routerH = size.height * .60f
            val left = cx - routerW / 2
            val top = cy - routerH / 2
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color.White, Color(0xFFDDE6EF))),
                topLeft = Offset(left, top),
                size = Size(routerW, routerH),
                cornerRadius = CornerRadius(18f)
            )
            repeat(4) { i ->
                drawCircle(
                    color = if (i < 3) RefGreen else Color(0xFFB8C4D4),
                    radius = 3f,
                    center = Offset(left + routerW * .80f, top + routerH * (.35f + i * .09f))
                )
            }
        }
        Text("ZTE", color = Color(0xFF9CA9BB), fontWeight = FontWeight.Bold, fontSize = 8.sp)
    }
}

@Composable
private fun ReferenceBandMiniChip(title: String, subtitle: String, color: Color) {
    Column(
        modifier = Modifier.width(35.dp).clip(RoundedCornerShape(9.dp)).background(color).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = RefNavy, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(subtitle, color = RefBlue, fontSize = 6.2.sp, maxLines = 1)
    }
}

@Composable
private fun ReferenceSignalMetricsRow(snapshot: RouterSnapshot) {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", ignoreCase = true)
    val rsrp = if (nrVerified) snapshot.nrRsrp else snapshot.lteRsrp
    val sinr = if (nrVerified) snapshot.nrSinr else snapshot.lteSinr
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ReferenceMetricCard(Modifier.weight(1f), "RSRP", rsrp, "dBm", RefBlue)
        ReferenceMetricCard(Modifier.weight(1f), "SINR", sinr, "dB", RefGreen)
        ReferenceMetricCard(Modifier.weight(1f), "RSRQ", snapshot.lteRsrq, "dB", RefBlue)
    }
}

@Composable
private fun ReferenceMetricCard(
    modifier: Modifier,
    name: String,
    value: Double?,
    unit: String,
    iconColor: Color
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = RefSurface),
        shape = RefSmallShape,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReferenceSignalBars(iconColor, 12.dp, 16.dp)
            Spacer(Modifier.width(7.dp))
            Column {
                Text(name, color = RefText2, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = value?.let { format1(it) } ?: "—",
                    color = RefNavy,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(unit, color = RefText2, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun ReferenceSpeedCard(
    modifier: Modifier = Modifier,
    performance: NetworkPerformance?,
    busy: Boolean,
    onSpeedTest: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RefCardShape,
        colors = CardDefaults.cardColors(containerColor = RefSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = performance?.latencyMs?.let { "Ping ${format0(it)} ms" } ?: "لم يُجر اختبار بعد",
                    color = RefText2,
                    fontSize = 6.5.sp
                )
                Spacer(Modifier.weight(1f))
                Text("اختبار السرعة", color = RefNavy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(4.dp))
                Text("◴", color = RefBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Row(modifier = Modifier.weight(1f)) {
                ReferenceSpeedGauge(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    downloadMbps = performance?.downloadMbps
                )
                Column(
                    modifier = Modifier.width(52.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("↥", color = RefPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("—", color = RefNavy, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Mb/s", color = RefText2, fontSize = 8.sp)
                    Text("رفع غير مقاس", color = RefText2, fontSize = 6.2.sp)
                }
            }
            Button(
                onClick = onSpeedTest,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(28.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RefBlue),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(if (busy) "جاري الاختبار…" else "▶  بدء الاختبار", fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ReferenceSpeedGauge(modifier: Modifier = Modifier, downloadMbps: Double?) {
    val clamped = ((downloadMbps ?: 0.0) / 500.0).coerceIn(0.0, 1.0).toFloat()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(65.dp)) {
            val stroke = 12f
            val arcSize = Size(size.width * .92f, size.height * 1.65f)
            val topLeft = Offset(size.width * .04f, size.height * .15f)
            drawArc(
                color = Color(0xFFDDE6F1),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(RefBlue, Color(0xFF2E9AFB), Color(0xFF34D5CC))),
                startAngle = 180f,
                sweepAngle = 180f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 10.dp)) {
            Text("↓", color = RefBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = downloadMbps?.let(::format1) ?: "—",
                color = RefNavy,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
            )
            Text("Mb/s تنزيل", color = RefText2, fontSize = 7.2.sp)
        }
    }
}

@Composable
private fun ReferenceNetworkModeCard(
    modifier: Modifier = Modifier,
    snapshot: RouterSnapshot,
    busy: Boolean,
    onSetNetworkMode: (String) -> Unit
) {
    val current = snapshot.raw["BearerPreference"].orEmpty()
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RefCardShape,
        colors = CardDefaults.cardColors(containerColor = RefSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("وضع الشبكة", color = RefNavy, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Text("⌁", color = RefBlue, fontSize = 14.sp)
            }
            Spacer(Modifier.height(7.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ReferenceNetworkModeButton("3G فقط", current == "Only_WCDMA", Modifier.weight(1f), busy) { onSetNetworkMode("Only_WCDMA") }
                ReferenceNetworkModeButton("4G فقط", current == "Only_LTE", Modifier.weight(1f), busy) { onSetNetworkMode("Only_LTE") }
            }
            Spacer(Modifier.height(5.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ReferenceNetworkModeButton("5G فقط", current == "Only_5G", Modifier.weight(1f), busy) { onSetNetworkMode("Only_5G") }
                ReferenceNetworkModeButton("تلقائي", current == "WL_AND_5G" || current == "LTE_AND_5G", Modifier.weight(1f), busy) { onSetNetworkMode("WL_AND_5G") }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (current.isBlank()) "الوضع الحالي غير مقروء" else "القيمة الحالية: $current",
                color = RefText2,
                fontSize = 5.8.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReferenceNetworkModeButton(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    busy: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(13.dp),
        color = if (selected) RefBlue else RefSoftBlue,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(0.5.dp, RefBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReferenceSignalBars(if (selected) Color.White else RefNavy, 15.dp, 14.dp)
            Spacer(Modifier.height(3.dp))
            Text(title, color = if (selected) Color.White else RefNavy, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReferenceTowerCard(
    modifier: Modifier = Modifier,
    snapshot: RouterSnapshot,
    onOpenTowers: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RefCardShape,
        colors = CardDefaults.cardColors(containerColor = RefSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("⌾", color = RefBlue, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("البرج الحالي", color = RefNavy, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(5.dp))
            Row(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.width(83.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReferenceInfoLine("Cell ID", snapshot.cellId?.toString() ?: "—")
                    ReferenceInfoLine("PCI", snapshot.pci?.toString() ?: "—")
                    ReferenceInfoLine("EARFCN", snapshot.earfcn?.toString() ?: "—")
                }
                Spacer(Modifier.width(6.dp))
                ReferenceMiniMap(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = snapshot.cellId?.toString() ?: "موقع غير مؤكد"
                )
            }
            Spacer(Modifier.height(4.dp))
            Surface(
                modifier = Modifier.align(Alignment.End).height(21.dp).width(105.dp),
                onClick = onOpenTowers,
                shape = RoundedCornerShape(11.dp),
                color = Color(0xFFEEF5FF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("عرض على الخريطة", color = RefBlue, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReferenceInfoLine(title: String, value: String) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = RefText2, fontSize = 6.2.sp)
        Text(value, color = RefNavy, fontSize = 7.3.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ReferenceMiniMap(modifier: Modifier = Modifier, label: String) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4F7F5))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val road = Color(0xFFE2E6E8)
            repeat(6) { i ->
                drawLine(road, Offset(0f, size.height * (i + 1) / 7), Offset(size.width, size.height * (i + 1) / 7 + 18f), 3f)
            }
            repeat(5) { i ->
                drawLine(road, Offset(size.width * (i + 1) / 6, 0f), Offset(size.width * (i + 1) / 6 - 16f, size.height), 3f)
            }
            drawCircle(RefBlue.copy(alpha = .18f), 36f, Offset(size.width * .55f, size.height * .50f))
            drawCircle(RefBlue, 11f, Offset(size.width * .55f, size.height * .50f))
        }
        Text("⌾", color = RefBlue, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center).offset(y = (-10).dp))
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).clip(RoundedCornerShape(9.dp)).background(Color.White).padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(label, color = RefNavy, fontSize = 6.2.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ReferenceLiveSignalCard(
    modifier: Modifier = Modifier,
    snapshot: RouterSnapshot,
    telemetrySamples: List<SafeTelemetrySample>
) {
    val nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", ignoreCase = true)
    val rsrp = if (nrVerified) snapshot.nrRsrp else snapshot.lteRsrp
    val values = telemetrySamples.mapNotNull { sample -> if (sample.nrVerified) sample.nrRsrp else sample.lteRsrp }.takeLast(30)
    val stable = values.takeLast(6).let { tail ->
        tail.size >= 3 && (tail.maxOrNull()!! - tail.minOrNull()!!) <= 8.0
    }
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RefCardShape,
        colors = CardDefaults.cardColors(containerColor = RefSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(9.dp)) {
            Text("مراقبة الإشارة المباشرة", color = RefNavy, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rsrp?.let { "${format0(it)} dBm" } ?: "—",
                    color = RefNavy,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (stable) RefSoftGreen else RefSoftBlue).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (stable) "مستقر" else "حي",
                        color = if (stable) Color(0xFF079B64) else RefBlue,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier.size(6.dp).background(if (stable) RefGreen else RefBlue, CircleShape))
                }
            }
            Spacer(Modifier.height(4.dp))
            ReferenceSignalGraph(modifier = Modifier.fillMaxWidth().weight(1f), values = values)
            Text("آخر ${values.size} قراءة", color = RefText2, fontSize = 6.sp, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
private fun ReferenceSignalGraph(modifier: Modifier = Modifier, values: List<Double>) {
    Canvas(modifier = modifier) {
        val grid = Color(0xFFE2EDF8)
        repeat(7) { i ->
            val x = size.width * i / 6f
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f)
        }
        repeat(4) { i ->
            val y = size.height * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
        }
        if (values.size < 2) return@Canvas
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.size - 1).coerceAtLeast(1)
            val normalized = ((-60.0 - value) / 60.0).coerceIn(0.0, 1.0).toFloat()
            val y = size.height * normalized
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = RefBlue, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}

@Composable
private fun ReferenceActiveBandsCard(
    modifier: Modifier = Modifier,
    snapshot: RouterSnapshot,
    onOpenBands: () -> Unit
) {
    val bands = connectedBandLabels(snapshot)
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RefCardShape,
        colors = CardDefaults.cardColors(containerColor = RefSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenBands),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⌁", color = RefBlue, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("الترددات النشطة", color = RefNavy, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(5.dp))
                Text("عرض الكل ‹", color = RefBlue, fontSize = 7.sp)
            }
            Spacer(Modifier.height(7.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val shown = bands.take(6)
                if (shown.isEmpty()) {
                    ReferenceActiveBand("—", "غير مؤكد", Color(0xFFEAF1FF), Modifier.weight(1f))
                    repeat(5) { Spacer(Modifier.weight(1f)) }
                } else {
                    shown.forEachIndexed { index, band ->
                        ReferenceActiveBand(
                            band,
                            if (band.startsWith("N", true)) "5G" else "LTE",
                            listOf(
                                Color(0xFFE5F2FF), Color(0xFFE8F9F4), Color(0xFFEAF9F0),
                                Color(0xFFF4EDFF), Color(0xFFFFF3D9), Color(0xFFEAF1FF)
                            )[index % 6],
                            Modifier.weight(1f)
                        )
                    }
                    repeat(6 - shown.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ReferenceActiveBand(name: String, tech: String, bg: Color, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(bg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(name, color = RefNavy, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(tech, color = if (tech == "5G") RefBlue else RefText2, fontSize = 6.2.sp, maxLines = 1)
    }
}

@Composable
private fun ReferenceQuickToolsCard(
    modifier: Modifier = Modifier,
    smartBusy: Boolean,
    onOptimize: () -> Unit,
    onDiagnostics: () -> Unit,
    onBands: () -> Unit,
    onNetwork: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RefCardShape,
        colors = CardDefaults.cardColors(containerColor = RefSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text("أدوات سريعة", color = RefNavy, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.End))
            Spacer(Modifier.height(5.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ReferenceQuickTool("🚀", if (smartBusy) "جاري\nالتحسين" else "تحسين\nالأداء", Color(0xFFF6EDFF), Modifier.weight(1f), onOptimize)
                ReferenceQuickTool("⌕", "تشخيص\nالشبكة", Color(0xFFE8FBF5), Modifier.weight(1f), onDiagnostics)
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ReferenceQuickTool("▣", "قفل\nالترددات", Color(0xFFE8FAF1), Modifier.weight(1f), onBands)
                ReferenceQuickTool("↻", "بيانات\nالشبكة", Color(0xFFF5EEFF), Modifier.weight(1f), onNetwork)
            }
        }
    }
}

@Composable
private fun ReferenceQuickTool(
    icon: String,
    text: String,
    background: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(3.dp)
    ) {
        Text(icon, color = RefBlue, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterStart))
        Text(
            text = text,
            color = RefNavy,
            fontSize = 6.6.sp,
            lineHeight = 7.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun ReferenceBottomNavigation(
    onNetwork: () -> Unit,
    onTools: () -> Unit,
    onTowers: () -> Unit,
    onBands: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(55.dp),
        shape = RoundedCornerShape(topStart = 19.dp, topEnd = 19.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReferenceBottomItem("▦", "الترددات", false, onBands)
            ReferenceBottomItem("⌾", "الأبراج", false, onTowers)
            ReferenceBottomItem("⚒", "الأدوات", false, onTools)
            ReferenceBottomItem("▥", "الشبكة", false, onNetwork)
            ReferenceBottomItem("⌂", "الرئيسية", true, {})
        }
    }
}

@Composable
private fun ReferenceBottomItem(icon: String, title: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(58.dp).clickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, color = if (selected) RefBlue else Color(0xFF64748F), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            title,
            color = if (selected) RefBlue else Color(0xFF64748F),
            fontSize = 7.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ReferenceSignalBars(color: Color, width: Dp, height: Dp) {
    Canvas(modifier = Modifier.size(width, height)) {
        val gap = size.width * .08f
        val barWidth = (size.width - gap * 3) / 4
        val heights = listOf(.32f, .52f, .76f, 1f)
        heights.forEachIndexed { index, factor ->
            val h = size.height * factor
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

private fun connectedBandLabels(snapshot: RouterSnapshot): List<String> {
    val ordered = LinkedHashSet<String>()
    snapshot.cells.forEach { cell ->
        normalizeBand(cell.band, cell.role)?.let(ordered::add)
    }
    if (snapshot.raw["_zte_nr_active_verified"].equals("true", ignoreCase = true)) {
        normalizeBand(snapshot.nrBand, CellRole.NR)?.let(ordered::add)
    }
    if (snapshot.raw["_zte_lte_active_verified"].equals("true", ignoreCase = true)) {
        normalizeBand(snapshot.lteBand, CellRole.PRIMARY)?.let(ordered::add)
    }
    return ordered.toList()
}

private fun normalizeBand(raw: String?, role: CellRole): String? {
    val n = Regex("\\d+").find(raw.orEmpty())?.value ?: return null
    return if (role == CellRole.NR || raw.orEmpty().startsWith("n", true)) "n$n" else "B$n"
}

private fun qualityLabel(rsrp: Double?): String = when {
    rsrp == null -> "غير مؤكد"
    rsrp >= -80 -> "إشارة ممتازة"
    rsrp >= -90 -> "إشارة جيدة جدًا"
    rsrp >= -100 -> "إشارة جيدة"
    rsrp >= -110 -> "إشارة ضعيفة"
    else -> "إشارة ضعيفة جدًا"
}

private fun format1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun format0(value: Double): String = String.format(Locale.US, "%.0f", value)
