package com.malik.ztesmartmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import java.util.Locale

private val V5HomeBg = Color(0xFFF4F7FB)
private val V5HomeCard = Color.White
private val V5HomeInk = Color(0xFF10275C)
private val V5HomeMuted = Color(0xFF69758A)
private val V5HomeBlue = Color(0xFF1268F3)
private val V5HomeBlueSoft = Color(0xFFEAF2FF)
private val V5HomeGreen = Color(0xFF16A86B)
private val V5HomeGreenSoft = Color(0xFFE9F8F1)
private val V5HomeBorder = Color(0xFFE2E8F0)
private val V5HomePanel = Color(0xFFF8FAFD)

private data class V5RadioState(
    val verified: Boolean,
    val network: String,
    val mode: String,
    val rsrp: Double?,
    val sinr: Double?,
    val rsrq: Double?,
    val bands: List<String>
)

@Composable
fun HaiHomeDashboardV5Legacy(
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
    val ui = LocalHaiUiMetrics.current
    val radio = v5RadioState(snapshot)

    Column(Modifier.fillMaxSize().background(V5HomeBg)) {
        HaiSharedHeader(
            connected = radio.verified || status.contains("متصل"),
            onDisconnect = onDisconnect,
            onMenu = onNavigateMore,
            onSettings = onNavigateTools,
            onSearch = onNavigateTowers
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                start = ui.pagePadding,
                end = ui.pagePadding,
                top = 4.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ui.sectionGap)
        ) {
            item { V5NetworkHero(snapshot, radio) }
            item { V5SignalGrid(snapshot, radio) }
            item { V5BandsCard(snapshot, radio, onNavigateBands) }
            item {
                V5SpeedCard(
                    performance = lastPerformance,
                    busy = speedBusy,
                    onSpeedTest = onSpeedTest
                )
            }
            item {
                V5NetworkModeCard(
                    snapshot = snapshot,
                    busy = controlBusy,
                    onSetNetworkMode = onSetNetworkMode,
                    onDetails = onNavigateNetwork
                )
            }
            item { V5TowerCard(snapshot, onNavigateTowers) }
            item {
                V5QuickActions(
                    smartBusy = smartBusy,
                    controlBusy = controlBusy,
                    onOptimize = onOptimizeNow,
                    onBands = onNavigateBands,
                    onRefresh = onRefreshNow,
                    onTools = onNavigateTools
                )
            }
            if (telemetrySamples.isNotEmpty()) {
                item { V5LiveReadingCard(telemetrySamples.last(), onNavigateLogs) }
            }
            if (operationMessage.isNotBlank()) {
                item { V5OperationMessage(operationMessage) }
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

@Composable
private fun V5NetworkHero(snapshot: RouterSnapshot, radio: V5RadioState) {
    V5Card {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حالة الشبكة الآن", color = V5HomeInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (radio.verified) "قراءة حية موثقة من الراوتر" else "الاتصال الراديوي غير مثبت بالقراءة الحالية",
                        color = V5HomeMuted,
                        fontSize = 12.sp
                    )
                }
                V5StatusPill(
                    text = if (radio.verified) "موثّق" else "غير مؤكد",
                    good = radio.verified
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        radio.network,
                        color = if (radio.verified) V5HomeBlue else V5HomeMuted,
                        fontSize = 54.sp,
                        lineHeight = 56.sp,
                        fontWeight = FontWeight.Black
                    )
                    Surface(
                        color = if (radio.verified) V5HomeBlueSoft else V5HomePanel,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            radio.mode,
                            color = if (radio.verified) V5HomeBlue else V5HomeMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("جودة الإشارة", color = V5HomeMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        v5Quality(radio.rsrp),
                        color = if (radio.rsrp != null) V5HomeGreen else V5HomeMuted,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        radio.rsrp?.let { "${v5Fmt1(it)} dBm" } ?: "لا توجد قراءة RSRP",
                        color = V5HomeInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(V5HomePanel).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("الراوتر", color = V5HomeMuted, fontSize = 11.sp)
                    Text(snapshot.model ?: "ZTE", color = V5HomeInk, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
                Box(Modifier.width(1.dp).height(34.dp).background(V5HomeBorder))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("نوع الاتصال", color = V5HomeMuted, fontSize = 11.sp)
                    Text(
                        snapshot.networkType ?: if (radio.verified) "${radio.network} ${radio.mode}" else "غير مؤكد",
                        color = V5HomeInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun V5SignalGrid(snapshot: RouterSnapshot, radio: V5RadioState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V5MetricTile("RSRP", radio.rsrp?.let(::v5Fmt1) ?: "—", "dBm", Modifier.weight(1f))
            V5MetricTile("SINR", radio.sinr?.let(::v5Fmt1) ?: "—", "dB", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V5MetricTile("RSRQ", radio.rsrq?.let(::v5Fmt1) ?: "—", "dB", Modifier.weight(1f))
            V5MetricTile("CELL ID", snapshot.cellId?.toString() ?: "—", "خلية", Modifier.weight(1f))
        }
    }
}

@Composable
private fun V5BandsCard(snapshot: RouterSnapshot, radio: V5RadioState, onBands: () -> Unit) {
    V5Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الترددات والدمج الفعلي", color = V5HomeInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("نعرض فقط ما ظهر في القراءة الحية", color = V5HomeMuted, fontSize = 11.sp)
                }
                V5SmallLink("إدارة الترددات", onBands)
            }
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (radio.bands.isEmpty()) {
                    V5BandChip("لا يوجد تردد موثّق", false)
                } else {
                    radio.bands.forEach { V5BandChip(it, true) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (snapshot.caActive) V5HomeGreenSoft else V5HomePanel)
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Carrier Aggregation", color = V5HomeMuted, fontSize = 11.sp)
                    Text(
                        if (snapshot.caActive) "الدمج مثبت كحالة نشطة" else "الدمج غير مثبت في القراءة الحالية",
                        color = if (snapshot.caActive) V5HomeGreen else V5HomeInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                V5StatusPill(if (snapshot.caActive) "CA نشط" else "غير مثبت", snapshot.caActive)
            }
        }
    }
}

@Composable
private fun V5SpeedCard(performance: NetworkPerformance?, busy: Boolean, onSpeedTest: () -> Unit) {
    V5Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("اختبار السرعة", color = V5HomeInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("قياس فعلي للإنترنت من الهاتف", color = V5HomeMuted, fontSize = 11.sp)
                }
                V5PrimaryButton(if (busy) "جاري القياس…" else "قياس الآن", !busy, onSpeedTest)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5ValuePanel("تنزيل", performance?.downloadMbps?.let(::v5Fmt1) ?: "—", "Mb/s", Modifier.weight(1f))
                V5ValuePanel("رفع", performance?.uploadMbps?.let(::v5Fmt1) ?: "—", "Mb/s", Modifier.weight(1f))
                V5ValuePanel("Ping", performance?.latencyMs?.let(::v5Fmt0) ?: "—", "ms", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V5NetworkModeCard(
    snapshot: RouterSnapshot,
    busy: Boolean,
    onSetNetworkMode: (String) -> Unit,
    onDetails: () -> Unit
) {
    val current = snapshot.raw["BearerPreference"].orEmpty()
    V5Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("وضع الشبكة", color = V5HomeInk, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                V5SmallLink("التفاصيل", onDetails)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5ModeButton("تلقائي", current == "WL_AND_5G" || current == "LTE_AND_5G", !busy, Modifier.weight(1f)) { onSetNetworkMode("WL_AND_5G") }
                V5ModeButton("5G فقط", current == "Only_5G", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_5G") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5ModeButton("4G فقط", current == "Only_LTE", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_LTE") }
                V5ModeButton("3G فقط", current == "Only_WCDMA", !busy, Modifier.weight(1f)) { onSetNetworkMode("Only_WCDMA") }
            }
        }
    }
}

@Composable
private fun V5TowerCard(snapshot: RouterSnapshot, onTowers: () -> Unit) {
    V5Card {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الخلية والبرج الحالي", color = V5HomeInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text("القيم التالية من الراوتر مباشرة", color = V5HomeMuted, fontSize = 11.sp)
                }
                V5SmallLink("الأبراج", onTowers)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5MiniInfo("PCI", snapshot.pci?.toString() ?: "—", Modifier.weight(1f))
                V5MiniInfo("EARFCN", snapshot.earfcn?.toString() ?: "—", Modifier.weight(1f))
                V5MiniInfo("CELL", snapshot.cellId?.toString() ?: "—", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V5QuickActions(
    smartBusy: Boolean,
    controlBusy: Boolean,
    onOptimize: () -> Unit,
    onBands: () -> Unit,
    onRefresh: () -> Unit,
    onTools: () -> Unit
) {
    V5Card {
        Column(Modifier.padding(16.dp)) {
            Text("اختصارات", color = V5HomeInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5ActionTile("تحسين موثّق", "اختبار الإعدادات", !smartBusy && !controlBusy, Modifier.weight(1f), onOptimize)
                V5ActionTile("الترددات", "قفل وإدارة", !controlBusy, Modifier.weight(1f), onBands)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V5ActionTile("تحديث الآن", "قراءة جديدة", !controlBusy, Modifier.weight(1f), onRefresh)
                V5ActionTile("الأدوات", "تشخيص واستعادة", true, Modifier.weight(1f), onTools)
            }
        }
    }
}

@Composable
private fun V5LiveReadingCard(sample: SafeTelemetrySample, onLogs: () -> Unit) {
    V5Card {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("آخر قراءة حية", color = V5HomeInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(
                    "${sample.networkType ?: "غير مؤكد"} • ${sample.nrBand ?: sample.lteBand ?: "لا يوجد تردد"}",
                    color = V5HomeBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "RSRP ${sample.nrRsrp ?: sample.lteRsrp ?: "—"} • SINR ${sample.nrSinr ?: sample.lteSinr ?: "—"}",
                    color = V5HomeMuted,
                    fontSize = 11.sp
                )
            }
            V5SmallLink("السجلات", onLogs)
        }
    }
}

@Composable
private fun V5OperationMessage(message: String) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(V5HomeBlueSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            message,
            color = V5HomeInk,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun V5MetricTile(label: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(V5HomeCard)
            .border(1.dp, V5HomeBorder, RoundedCornerShape(18.dp)).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = V5HomeMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = V5HomeInk, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(unit, color = V5HomeMuted, fontSize = 10.sp)
    }
}

@Composable
private fun V5ValuePanel(title: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(V5HomePanel).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = V5HomeMuted, fontSize = 10.sp)
        Text(value, color = V5HomeInk, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(unit, color = V5HomeMuted, fontSize = 9.sp)
    }
}

@Composable
private fun V5MiniInfo(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(V5HomePanel).padding(vertical = 11.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = V5HomeMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = V5HomeInk, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V5BandChip(text: String, active: Boolean) {
    Box(
        Modifier.clip(RoundedCornerShape(14.dp))
            .background(if (active) V5HomeBlueSoft else V5HomePanel)
            .border(1.dp, if (active) V5HomeBlue.copy(alpha = .35f) else V5HomeBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, color = if (active) V5HomeBlue else V5HomeMuted, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V5ModeButton(text: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(15.dp))
            .background(if (selected) V5HomeBlue else if (enabled) V5HomePanel else Color(0xFFEDEFF3))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else if (enabled) V5HomeInk else V5HomeMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun V5ActionTile(title: String, subtitle: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (enabled) V5HomePanel else Color(0xFFEFF1F4))
            .clickable(enabled = enabled, onClick = onClick).padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(if (enabled) V5HomeBlue else V5HomeMuted, CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(title, color = if (enabled) V5HomeInk else V5HomeMuted, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = V5HomeMuted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun V5PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(42.dp).clip(RoundedCornerShape(14.dp))
            .background(if (enabled) V5HomeBlue else Color(0xFFE6E9EF))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.White else V5HomeMuted, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun V5SmallLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = V5HomeBlue,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp)
    )
}

@Composable
private fun V5StatusPill(text: String, good: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(if (good) V5HomeGreenSoft else V5HomePanel)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(if (good) V5HomeGreen else V5HomeMuted, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (good) V5HomeGreen else V5HomeMuted, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V5Card(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalHaiUiMetrics.current.cardRadius),
        colors = CardDefaults.cardColors(containerColor = V5HomeCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, V5HomeBorder)
    ) { content() }
}

private fun v5RadioState(snapshot: RouterSnapshot): V5RadioState {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", ignoreCase = true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", ignoreCase = true)
    val network = when {
        nr -> "5G"
        lte -> "4G"
        else -> "—"
    }
    val type = snapshot.networkType.orEmpty()
    val mode = when {
        nr && type.contains("SA", ignoreCase = true) && !type.contains("NSA", ignoreCase = true) -> "SA"
        nr -> "NSA"
        lte -> "LTE"
        else -> "غير مؤكد"
    }
    val bands = buildList {
        snapshot.cells.mapNotNullTo(this) { cell -> cell.band?.trim()?.takeIf { it.isNotBlank() } }
        if (isEmpty() && lte) snapshot.lteBand?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
        if (nr) snapshot.nrBand?.trim()?.takeIf { it.isNotBlank() }?.let { band -> if (band !in this) add(band) }
    }.distinct()

    return V5RadioState(
        verified = nr || lte,
        network = network,
        mode = mode,
        rsrp = if (nr) snapshot.nrRsrp else if (lte) snapshot.lteRsrp else null,
        sinr = if (nr) snapshot.nrSinr else if (lte) snapshot.lteSinr else null,
        rsrq = if (lte || nr) snapshot.lteRsrq else null,
        bands = bands
    )
}

private fun v5Quality(rsrp: Double?): String = when {
    rsrp == null -> "غير معروف"
    rsrp >= -85 -> "ممتاز"
    rsrp >= -95 -> "جيد"
    rsrp >= -105 -> "متوسط"
    else -> "ضعيف"
}

private fun v5Fmt0(value: Double): String = String.format(Locale.US, "%.0f", value)
private fun v5Fmt1(value: Double): String = String.format(Locale.US, "%.1f", value)
