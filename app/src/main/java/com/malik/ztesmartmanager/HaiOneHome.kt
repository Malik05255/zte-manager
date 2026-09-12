package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.model.ConnectedDevice
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import kotlin.math.roundToInt

@Composable
internal fun HaiHome(
    snapshot: RouterSnapshot,
    qualityScore: Int,
    telemetrySamples: List<SafeTelemetrySample>,
    lastPerformance: NetworkPerformance?,
    speedBusy: Boolean,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    devices: List<ConnectedDevice>,
    controlBusy: Boolean,
    scanBusy: Boolean,
    operationMessage: String,
    onSpeedTest: () -> Unit,
    onPlacementToggle: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onScanCells: () -> Unit,
    onLockCurrentCell: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenTowers: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenTools: () -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HaiHero(snapshot, qualityScore) }
        item { HaiPulseStrip(snapshot, lastPerformance, placementReading, devices.size) }
        item { HaiSpeed(lastPerformance, speedBusy, onSpeedTest) }
        item { HaiModes(snapshot, controlBusy, onSetNetworkMode) }
        item { HaiBands(snapshot, onOpenNetwork) }
        item { HaiPlacement(placementMode, placementReading, onPlacementToggle) }
        item { HaiTower(snapshot, scanBusy, controlBusy, onScanCells, onLockCurrentCell, onOpenTowers) }
        item { HaiSignalHistory(snapshot, qualityScore, telemetrySamples) }
        item { HaiDevices(devices, onOpenDevices) }
        item { HaiActions(onOpenNetwork, onOpenTowers, onOpenTools) }
        if (operationMessage.isNotBlank()) item { HaiOperation(operationMessage) }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun HaiHero(snapshot: RouterSnapshot, qualityScore: Int) {
    val qualityColor = haiQualityColor(qualityScore)
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(HaiHeroBrush)) {
        val ratio = if (maxWidth < 520.dp) 0.82f else 1.65f
        val networkFontSize = if (maxWidth < 390.dp) 42.sp else 52.sp
        Box(Modifier.fillMaxWidth().aspectRatio(ratio)) {
            HaiDigitalBackdrop(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f)) {
                        Text(haiOperator(snapshot), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), maxLines = 1)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(HaiGreen))
                    Spacer(Modifier.width(8.dp))
                    Text(haiQualityWord(qualityScore), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.weight(0.45f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HaiRouterSilhouette(snapshot.model ?: "ZTE", Modifier.weight(0.36f).fillMaxHeight(0.62f))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(0.64f)) {
                        Text(haiNetworkLabel(snapshot), color = Color.White, fontSize = networkFontSize, lineHeight = 56.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Text(if (snapshot.caActive) "${maxOf(snapshot.cells.size, 2)}CA" else "اتصال مباشر", color = HaiCyan, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                }
                Spacer(Modifier.weight(0.35f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HaiHeroMetric("قوة", haiMetricWord("RSRP", snapshot.nrRsrp ?: snapshot.lteRsrp), haiRounded(snapshot.nrRsrp ?: snapshot.lteRsrp, "dBm"), HaiBlue2, Modifier.weight(1f))
                    HaiHeroMetric("نظافة", haiMetricWord("SINR", snapshot.nrSinr ?: snapshot.lteSinr), haiRounded(snapshot.nrSinr ?: snapshot.lteSinr, "dB"), HaiPurple, Modifier.weight(1f))
                    HaiHeroMetric("جودة", haiMetricWord("RSRQ", snapshot.lteRsrq), haiRounded(snapshot.lteRsrq, "dB"), HaiCyan, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { (qualityScore / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = qualityColor, trackColor = Color.White.copy(alpha = 0.14f))
            }
        }
    }
}

@Composable
private fun HaiDigitalBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val grid = 44.dp.toPx()
        var x = 0f
        while (x <= size.width) { drawLine(Color.White.copy(alpha = 0.035f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += grid }
        var y = 0f
        while (y <= size.height) { drawLine(Color.White.copy(alpha = 0.035f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += grid }
        val c = Offset(size.width * 0.17f, size.height * 0.42f)
        repeat(4) { i -> drawCircle(HaiBlue2.copy(alpha = 0.07f - i * 0.01f), size.minDimension * (0.18f + i * 0.08f), c, style = Stroke(width = 2.dp.toPx())) }
        drawCircle(HaiCyan.copy(alpha = 0.08f), size.minDimension * 0.24f, Offset(size.width * 0.92f, size.height * 0.04f))
    }
}

@Composable
private fun HaiRouterSilhouette(model: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Surface(modifier = Modifier.fillMaxHeight().aspectRatio(0.58f), shape = RoundedCornerShape(26.dp), color = Color.White.copy(alpha = 0.96f), shadowElevation = 10.dp) {
            Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.width(42.dp).height(5.dp).clip(CircleShape).background(Color(0xFF263A55)))
                Text("ZTE", color = Color(0xFF667B96), fontSize = 20.sp, fontWeight = FontWeight.Black)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { repeat(5) { index -> Box(Modifier.size(6.dp).clip(CircleShape).background(if (index < 4) HaiGreen else HaiBlue)) } }
                Text(model.take(10), color = HaiInk, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HaiHeroMetric(title: String, word: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.10f), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color.White.copy(alpha = 0.78f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(word, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun HaiPulseStrip(snapshot: RouterSnapshot, performance: NetworkPerformance?, placement: PlacementReading?, deviceCount: Int) {
    val speed = performance?.downloadMbps?.let { "${it.roundToInt()}" } ?: "—"
    val place = haiPlacementState(placement).first
    val ca = if (snapshot.caActive) "${maxOf(snapshot.cells.size, 2)}CA" else "—"
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HaiMiniStat("سرعة", speed, "Mb/s")
        HaiMiniStat("دمج", ca, "")
        HaiMiniStat("المكان", place, "")
        HaiMiniStat("أجهزة", deviceCount.toString(), "")
    }
}

@Composable
private fun HaiMiniStat(title: String, value: String, suffix: String) {
    Surface(modifier = Modifier.widthIn(min = 116.dp).heightIn(min = 84.dp), shape = RoundedCornerShape(22.dp), color = HaiPaper, border = androidx.compose.foundation.BorderStroke(1.dp, HaiLine)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = HaiMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = HaiInk, fontSize = 23.sp, fontWeight = FontWeight.Black, maxLines = 1)
                if (suffix.isNotBlank()) { Spacer(Modifier.width(4.dp)); Text(suffix, color = HaiMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun HaiSpeed(performance: NetworkPerformance?, busy: Boolean, onRun: () -> Unit) {
    HaiCard {
        HaiSectionTitle("السرعة", if (busy) "يُقاس الآن" else null)
        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gaugeHeight = if (maxWidth < 390.dp) 150.dp else 170.dp
            Box(Modifier.fillMaxWidth().height(gaugeHeight), contentAlignment = Alignment.Center) {
                HaiSpeedGauge(performance?.downloadMbps, Modifier.fillMaxWidth(0.86f).fillMaxHeight())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(performance?.downloadMbps?.let { String.format("%.1f", it) } ?: "—", color = HaiInk, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Text("Mb/s", color = HaiMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (performance != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                HaiSmallValue("استجابة", performance.latencyMs?.roundToInt()?.toString() ?: "—", "ms", Modifier.weight(1f))
                HaiSmallValue("تذبذب", performance.jitterMs?.roundToInt()?.toString() ?: "—", "ms", Modifier.weight(1f))
                HaiSmallValue("فقد", performance.packetLossPercent?.roundToInt()?.toString() ?: "—", "%", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        HaiPrimaryButton(if (busy) "جاري القياس" else "ابدأ القياس", !busy, onClick = onRun)
    }
}

@Composable
private fun HaiSpeedGauge(speed: Double?, modifier: Modifier = Modifier) {
    val fraction = ((speed ?: 0.0) / 1000.0).coerceIn(0.0, 1.0).toFloat()
    Canvas(modifier) {
        val stroke = 14.dp.toPx()
        val inset = stroke / 2f
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height * 1.72f - stroke)
        drawArc(HaiLine, 180f, 180f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(brush = androidx.compose.ui.graphics.Brush.sweepGradient(listOf(HaiCyan, HaiBlue, HaiPurple)), startAngle = 180f, sweepAngle = 180f * fraction, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun HaiSmallValue(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(17.dp), color = Color(0xFFF7FAFE)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = HaiMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("$value $unit", color = HaiInk, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun HaiModes(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    HaiCard {
        HaiSectionTitle("وضع الشبكة", haiNetworkLabel(snapshot))
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HaiChoice("تلقائي", false, !busy, Modifier.weight(1f)) { onMode("WL_AND_5G") }
                HaiChoice("5G + 4G", haiIs5G(snapshot), !busy, Modifier.weight(1f)) { onMode("LTE_AND_5G") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HaiChoice("4G فقط", !haiIs5G(snapshot) && haiNetworkLabel(snapshot).contains("4G"), !busy, Modifier.weight(1f)) { onMode("Only_LTE") }
                HaiChoice("3G فقط", snapshot.networkType.orEmpty().contains("WCDMA", true), !busy, Modifier.weight(1f)) { onMode("Only_WCDMA") }
            }
        }
    }
}

@Composable
private fun HaiBands(snapshot: RouterSnapshot, onOpen: () -> Unit) {
    val bands = haiActiveBands(snapshot)
    HaiCard {
        HaiSectionTitle("الترددات", if (snapshot.caActive) "${maxOf(snapshot.cells.size, 2)}CA" else null)
        Spacer(Modifier.height(13.dp))
        if (bands.isEmpty()) {
            Text("—", color = HaiMuted, fontSize = 18.sp, fontWeight = FontWeight.Black)
        } else {
            val rows = bands.chunked(3)
            rows.forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    row.forEach { band ->
                        Surface(modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp), color = HaiSoftBlue) {
                            Box(contentAlignment = Alignment.Center) { Text(band, color = HaiBlue, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1) }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                if (rowIndex != rows.lastIndex) Spacer(Modifier.height(9.dp))
            }
        }
        Spacer(Modifier.height(13.dp))
        HaiGhostButton("إدارة الترددات", onClick = onOpen)
    }
}

@Composable
private fun HaiPlacement(active: Boolean, reading: PlacementReading?, onToggle: () -> Unit) {
    val (state, color) = haiPlacementState(reading)
    val score = reading?.score?.total ?: 0
    HaiCard {
        HaiSectionTitle("أفضل مكان", if (active) "مباشر" else null)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(HaiLine, style = Stroke(9.dp.toPx()))
                    drawArc(color, -90f, 360f * (score.coerceIn(0, 100) / 100f), false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                }
                Text(if (score > 0) "$score%" else "—", color = HaiInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(state, color = color, fontSize = 23.sp, fontWeight = FontWeight.Black)
                if (reading != null) { Spacer(Modifier.height(5.dp)); Text(haiQualityWord(score), color = HaiMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(14.dp))
        HaiPrimaryButton(if (active) "إيقاف" else "ابدأ الجولة", onClick = onToggle)
    }
}

@Composable
private fun HaiTower(snapshot: RouterSnapshot, scanBusy: Boolean, controlBusy: Boolean, onScan: () -> Unit, onLock: () -> Unit, onOpen: () -> Unit) {
    HaiCard(padding = PaddingValues(14.dp)) {
        HaiSectionTitle("البرج")
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val mapRatio = if (maxWidth < 520.dp) 1.08f else 1.75f
            HaiOneMap(snapshot, Modifier.fillMaxWidth().aspectRatio(mapRatio))
        }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 370.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    HaiPrimaryButton(if (scanBusy) "جاري المسح" else "مسح الأبراج", !scanBusy && !controlBusy, onClick = onScan)
                    HaiGhostButton("تثبيت الحالي", !controlBusy, onClick = onLock)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    HaiPrimaryButton(if (scanBusy) "جاري المسح" else "مسح الأبراج", !scanBusy && !controlBusy, Modifier.weight(1f), onScan)
                    HaiGhostButton("تثبيت الحالي", !controlBusy, Modifier.weight(1f), onLock)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("إدارة البرج", color = HaiBlue, fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.End).clickable(onClick = onOpen).padding(8.dp))
    }
}

@Composable
private fun HaiSignalHistory(snapshot: RouterSnapshot, qualityScore: Int, samples: List<SafeTelemetrySample>) {
    HaiCard {
        HaiSectionTitle("الإشارة", haiQualityWord(qualityScore))
        Spacer(Modifier.height(12.dp))
        val values = samples.takeLast(24).mapNotNull { it.nrRsrp ?: it.lteRsrp }
        Box(Modifier.fillMaxWidth().height(130.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                if (values.size >= 2) {
                    val min = (values.minOrNull() ?: -120.0) - 2.0
                    val max = (values.maxOrNull() ?: -75.0) + 2.0
                    val range = (max - min).takeIf { it > 0.0 } ?: 1.0
                    for (i in 0 until values.lastIndex) {
                        val x1 = size.width * i / values.lastIndex
                        val x2 = size.width * (i + 1) / values.lastIndex
                        val y1 = size.height - (((values[i] - min) / range).toFloat() * size.height)
                        val y2 = size.height - (((values[i + 1] - min) / range).toFloat() * size.height)
                        drawLine(HaiBlue, Offset(x1, y1), Offset(x2, y2), 4.dp.toPx(), StrokeCap.Round)
                    }
                } else drawLine(HaiLine, Offset(0f, size.height * 0.55f), Offset(size.width, size.height * 0.55f), 3.dp.toPx())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HaiSmallValue("قوة", haiMetricWord("RSRP", snapshot.nrRsrp ?: snapshot.lteRsrp), "", Modifier.weight(1f))
            HaiSmallValue("نظافة", haiMetricWord("SINR", snapshot.nrSinr ?: snapshot.lteSinr), "", Modifier.weight(1f))
            HaiSmallValue("جودة", haiMetricWord("RSRQ", snapshot.lteRsrq), "", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HaiDevices(devices: List<ConnectedDevice>, onOpen: () -> Unit) {
    HaiCard {
        HaiSectionTitle("الأجهزة", devices.size.toString())
        Spacer(Modifier.height(12.dp))
        if (devices.isEmpty()) Text("لا توجد أجهزة ظاهرة", color = HaiMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold) else {
            devices.take(3).forEachIndexed { index, device ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(HaiSoftBlue), contentAlignment = Alignment.Center) { Text(if (device.transport.name == "LAN") "L" else "W", color = HaiBlue, fontSize = 16.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(device.displayName, color = HaiInk, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1); device.ipAddress?.let { Text(it, color = HaiMuted, fontSize = 14.sp, maxLines = 1) } }
                }
                if (index < minOf(devices.lastIndex, 2)) HorizontalDivider(color = HaiLine)
            }
        }
        Spacer(Modifier.height(8.dp))
        HaiGhostButton("كل الأجهزة", onClick = onOpen)
    }
}

@Composable
private fun HaiActions(onNetwork: () -> Unit, onTowers: () -> Unit, onTools: () -> Unit) {
    HaiCard {
        HaiSectionTitle("الوصول السريع")
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            HaiActionRow("الشبكة والترددات", "◫", HaiSoftBlue, HaiBlue, onNetwork)
            HaiActionRow("الأبراج والخريطة", "⌖", HaiSoftGreen, HaiGreen, onTowers)
            HaiActionRow("الأدوات", "✦", HaiSoftPurple, HaiPurple, onTools)
        }
    }
}

@Composable
private fun HaiActionRow(title: String, symbol: String, bg: Color, color: Color, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), color = Color(0xFFF8FBFF), border = androidx.compose.foundation.BorderStroke(1.dp, HaiLine)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Text(symbol, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(12.dp))
            Text(title, color = HaiInk, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("‹", color = HaiMuted, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HaiOperation(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = HaiSoftBlue) {
        Text(message, color = HaiInk, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
    }
}
