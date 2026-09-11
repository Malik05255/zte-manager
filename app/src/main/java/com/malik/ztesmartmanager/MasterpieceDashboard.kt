package com.malik.ztesmartmanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetry
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.ConnectedDevice
import com.malik.ztesmartmanager.core.model.ConnectedDeviceParser
import com.malik.ztesmartmanager.core.model.DeviceTransport
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget

private enum class MpScreen { HOME, NETWORK, TOWERS, BANDS, CLIENTS, PLACEMENT, TOOLS }

@Composable
fun MasterpieceDashboard(
    snapshot: RouterSnapshot?,
    capabilities: RouterCapabilities,
    runtime: RuntimeCapabilityReport?,
    traffic: TrafficTelemetry?,
    thermal: ThermalTelemetry?,
    telemetrySamples: List<SafeTelemetrySample>,
    stability: ConnectionStabilityReport,
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
    onRestoreSafetyBackup: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onShareDiagnostics: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val history = remember { mutableStateListOf(MpScreen.HOME) }
        val screen = history.last()
        val aliases = remember { mutableStateMapOf<String, String>() }
        var soundEnabled by rememberSaveable { mutableStateOf(true) }

        fun navigate(target: MpScreen) {
            if (history.lastOrNull() != target) history += target
        }
        fun root(target: MpScreen) {
            history.clear()
            history += target
        }

        BackHandler(enabled = history.size > 1 || screen != MpScreen.HOME) {
            if (history.size > 1) history.removeAt(history.lastIndex) else root(MpScreen.HOME)
        }

        val wifi = remember(snapshot?.raw?.get("station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("station_list"), DeviceTransport.WIFI)
        }
        val lan = remember(snapshot?.raw?.get("lan_station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("lan_station_list"), DeviceTransport.LAN)
        }
        val devices = remember(wifi, lan) { ConnectedDeviceParser.merge(wifi, lan) }

        PlacementSoundEffect(soundEnabled, placementMode && screen == MpScreen.PLACEMENT, placementReading)

        Column(Modifier.fillMaxSize().background(MpBg)) {
            MpHeader(
                snapshot = snapshot,
                screen = screen,
                onBack = { if (history.size > 1) history.removeAt(history.lastIndex) else root(MpScreen.HOME) }
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (snapshot == null) {
                    EmptyState("جاري تجهيز الاتصال", status.ifBlank { "بانتظار بيانات الراوتر…" }, "⌁")
                } else when (screen) {
                    MpScreen.HOME -> MpHome(
                        snapshot, status, operationMessage, lastPerformance, speedBusy, controlBusy,
                        placementReading, devices, nearbyCells, onSpeedTest, onSetNetworkMode,
                        onNetwork = { navigate(MpScreen.NETWORK) },
                        onTowers = { navigate(MpScreen.TOWERS) },
                        onBands = { navigate(MpScreen.BANDS) },
                        onClients = { navigate(MpScreen.CLIENTS) },
                        onPlacement = { navigate(MpScreen.PLACEMENT) },
                        onTools = { navigate(MpScreen.TOOLS) }
                    )
                    MpScreen.NETWORK -> MpNetwork(snapshot, status, operationMessage, traffic, thermal, stability, controlBusy, onSetNetworkMode)
                    MpScreen.TOWERS -> MpTowers(
                        snapshot, nearbyCells, scanBusy, controlBusy, towerTarget, towerGuardEnabled, towerGuardStatus,
                        onScanCells, onLockCurrentCell, onLockNearbyCell, onClearCellLock, onTowerGuardChange
                    )
                    MpScreen.BANDS -> MpBands(snapshot, capabilities, selectedLte, selectedNr, controlBusy, onLteToggle, onNrToggle, onApplyLte, onApplyNr)
                    MpScreen.CLIENTS -> MpClients(devices, aliases)
                    MpScreen.PLACEMENT -> MpPlacement(placementMode, placementReading, soundEnabled, { soundEnabled = it }, onPlacementToggle)
                    MpScreen.TOOLS -> MpTools(
                        runtime, telemetrySamples.size, stability, smartMode, smartGoal, smartBusy, smartReport,
                        safetyBackupAvailable, capabilities, controlBusy, onSmartModeChange, onSmartGoalChange,
                        onOptimizeNow, onRestoreSafetyBackup, onAntennaState, onCopyDiagnostics, onShareDiagnostics, onDisconnect
                    )
                }
            }

            MpBottomBar(screen) { root(it) }
        }
    }
}

@Composable
private fun MpHeader(snapshot: RouterSnapshot?, screen: MpScreen, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(if (screen == MpScreen.HOME) MpSoftBlue else Color.White)
                .clickable(enabled = screen != MpScreen.HOME, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text(if (screen == MpScreen.HOME) "H" else "‹", color = MpBlue, fontSize = if (screen == MpScreen.HOME) 18.sp else 29.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(mpTitle(screen), color = MpInk, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(if (screen == MpScreen.HOME) "كل الشبكة أمامك — بدون تخمين" else "ZTE Smart HAI", color = MpMuted, fontSize = 10.sp)
        }
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(if (snapshot != null) MpSoftGreen else Color(0xFFFFEEEE)).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (snapshot != null) MpGreen else MpRed))
            Spacer(Modifier.width(6.dp))
            Text(if (snapshot != null) "متصل" else "غير متصل", color = MpInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun mpTitle(screen: MpScreen) = when (screen) {
    MpScreen.HOME -> "HAI Network"
    MpScreen.NETWORK -> "الشبكة الآن"
    MpScreen.TOWERS -> "الأبراج والخريطة"
    MpScreen.BANDS -> "الترددات"
    MpScreen.CLIENTS -> "المتواجدون الآن"
    MpScreen.PLACEMENT -> "أفضل مكان للراوتر"
    MpScreen.TOOLS -> "الأدوات الذكية"
}

@Composable
private fun MpBottomBar(screen: MpScreen, onSelect: (MpScreen) -> Unit) {
    NavigationBar(modifier = Modifier.navigationBarsPadding(), containerColor = Color.White, tonalElevation = 8.dp) {
        listOf(
            Triple(MpScreen.HOME, "الرئيسية", "⌂"),
            Triple(MpScreen.TOWERS, "الأبراج", "⌁"),
            Triple(MpScreen.BANDS, "الترددات", "≋"),
            Triple(MpScreen.CLIENTS, "الأجهزة", "◉"),
            Triple(MpScreen.TOOLS, "الأدوات", "✦")
        ).forEach { (target, label, glyph) ->
            NavigationBarItem(
                selected = screen == target,
                onClick = { onSelect(target) },
                icon = { Text(glyph, fontSize = 18.sp, color = if (screen == target) MpBlue else MpMuted) },
                label = { Text(label, fontSize = 9.sp, fontWeight = if (screen == target) FontWeight.Black else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MpBlue, selectedTextColor = MpBlue, indicatorColor = MpSoftBlue,
                    unselectedIconColor = MpMuted, unselectedTextColor = MpMuted
                )
            )
        }
    }
}

@Composable
private fun MpHome(
    snapshot: RouterSnapshot,
    status: String,
    operationMessage: String,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    controlBusy: Boolean,
    placementReading: PlacementReading?,
    devices: List<ConnectedDevice>,
    nearbyCells: List<NearbyCell>,
    onSpeedTest: () -> Unit,
    onMode: (String) -> Unit,
    onNetwork: () -> Unit,
    onTowers: () -> Unit,
    onBands: () -> Unit,
    onClients: () -> Unit,
    onPlacement: () -> Unit,
    onTools: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MpNetworkHero(snapshot, onNetwork) }
        item {
            BoxWithConstraints {
                if (maxWidth < 390.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MpSpeedCard(performance, speedBusy, onSpeedTest)
                        MpModeCard(snapshot, controlBusy, onMode)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { MpSpeedCard(performance, speedBusy, onSpeedTest) }
                        Box(Modifier.weight(1f)) { MpModeCard(snapshot, controlBusy, onMode) }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { ActionTile("الأبراج", "اختيار خلية حقيقية بضغطة", "⌁", MpSoftGreen, onClick = onTowers) }
                Box(Modifier.weight(1f)) { ActionTile("أفضل مكان", placementReading?.score?.let { "آخر تقييم ${it.total}/100" } ?: "رادار حي مع رنين", "◎", MpSoftPurple, onClick = onPlacement) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { ActionTile("المتواجدون الآن", "${devices.size} جهاز ظاهر للراوتر", "◉", MpSoftBlue, onClick = onClients) }
                Box(Modifier.weight(1f)) { ActionTile("الترددات", mpBandSummary(snapshot), "≋", MpSoftAmber, onClick = onBands) }
            }
        }
        item { MpTowerSummary(snapshot, nearbyCells.size, onTowers) }
        item { MpSignalCard(snapshot) }
        item { ActionTile("الأدوات الذكية", "تحسين موثق • استعادة • تشخيص", "✦", Color(0xFFE9F5FF), onClick = onTools) }
        item { StatusBanner(operationMessage.ifBlank { status }) }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun MpNetworkHero(snapshot: RouterSnapshot, onClick: () -> Unit) {
    val operator = mpClean(snapshot.raw["network_provider_fullname"])
        ?: mpClean(snapshot.raw["network_provider"])
        ?: snapshot.operatorCode
        ?: "الشبكة"
    val tech = snapshot.networkType ?: "غير مؤكد"
    val bands = mpBands(snapshot)
    val rsrp = snapshot.nrRsrp ?: snapshot.lteRsrp
    val sinr = snapshot.nrSinr ?: snapshot.lteSinr

    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFFBED9FF))
    ) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color.White, Color(0xFFF1F7FF), Color(0xFFE6F2FF)))).padding(17.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MpGreen))
                        Spacer(Modifier.width(6.dp))
                        Text("LIVE • من الراوتر", color = MpGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(operator, color = MpInk, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(tech, color = MpBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.size(84.dp).clip(RoundedCornerShape(25.dp)).background(Brush.linearGradient(listOf(MpBlue, MpCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(mpGeneration(snapshot), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Text(if (snapshot.caActive) "CA نشط" else "متصل", color = Color.White.copy(alpha = .85f), fontSize = 10.sp)
                    }
                }
            }
            if (bands.isNotEmpty()) {
                Spacer(Modifier.height(13.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    bands.forEachIndexed { index, band ->
                        val role = if (band.startsWith("N", true)) "5G" else if (index == 0) "أساسي" else "مدمج"
                        Text(
                            "$band  $role", color = if (band.startsWith("N", true)) Color(0xFF6C49D8) else MpBlue,
                            fontSize = 10.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .9f)).padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.weight(1f)) { MetricPill("RSRP", rsrp.metric(), "dBm", qualityColor(mpSignalScore(rsrp))) }
                Box(Modifier.weight(1f)) { MetricPill("SINR", sinr.metric(), "dB", qualityColor(mpSinrScore(sinr))) }
                Box(Modifier.weight(1f)) { MetricPill("RSRQ", snapshot.lteRsrq.metric(), "dB", qualityColor(mpRsrqScore(snapshot.lteRsrq))) }
            }
            Spacer(Modifier.height(9.dp))
            Text("PCI ${snapshot.pci ?: "—"} • EARFCN ${snapshot.earfcn ?: "—"} • ${mpEnbSector(snapshot)}", color = MpMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun MpSpeedCard(performance: NetworkPerformance?, busy: Boolean, onTest: () -> Unit) {
    HaiSurface(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HaiSectionTitle("قياس السرعة", "اختبار إنترنت فعلي عبر Cloudflare")
            SpeedGauge(performance?.downloadMbps, busy, Modifier.padding(top = 5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MpTinyMetric("Ping", "${performance?.latencyMs.metric()} ms")
                MpTinyMetric("Jitter", "${performance?.jitterMs.metric()} ms")
                MpTinyMetric("Loss", "${performance?.packetLossPercent.metric()}%")
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onTest, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MpBlue)
            ) { Text(if (busy) "جاري القياس…" else "▶ ابدأ القياس", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun MpTinyMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MpMuted, fontSize = 9.sp)
        Text(value, color = MpInk, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MpModeCard(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    val current = mpClean(snapshot.raw["BearerPreference"]) ?: mpClean(snapshot.raw["current_network_mode"])
    HaiSurface(Modifier.fillMaxWidth()) {
        Column {
            HaiSectionTitle("أوضاع الشبكة", "يتم التحقق بعد التطبيق")
            Spacer(Modifier.height(10.dp))
            listOf(
                Triple("تلقائي", "WL_AND_5G", "A"),
                Triple("5G فقط", "Only_5G", "5G"),
                Triple("4G فقط", "Only_LTE", "4G"),
                Triple("3G فقط", "Only_WCDMA", "3G")
            ).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, mode, glyph) ->
                        val selected = current.equals(mode, true)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                                .background(if (selected) MpSoftBlue else Color(0xFFF5F8FC))
                                .clickable(enabled = !busy) { onMode(mode) }.padding(vertical = 11.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(glyph, color = if (selected) MpBlue else MpInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text(label, color = if (selected) MpBlue else MpInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(current?.let { "الحالي: $it" } ?: "الوضع الحالي غير مكشوف من الـFirmware", color = MpMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun MpTowerSummary(snapshot: RouterSnapshot, nearbyCount: Int, onOpen: () -> Unit) {
    HaiSurface(Modifier.fillMaxWidth()) {
        Column {
            HaiSectionTitle("البرج المتصل", "هوية خلوية حقيقية — لا نختلق إحداثيات", if (nearbyCount > 0) "$nearbyCount خلية" else null)
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { MetricPill("eNB", snapshot.cellId?.ushr(8)?.toString() ?: "—") }
                Box(Modifier.weight(1f)) { MetricPill("Sector", snapshot.cellId?.and(0xFF)?.toString() ?: "—") }
                Box(Modifier.weight(1f)) { MetricPill("PCI", snapshot.pci?.toString() ?: "—") }
            }
            Spacer(Modifier.height(11.dp))
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                Text("⌁ افتح الخريطة واختر خلية", color = MpBlue, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun MpSignalCard(snapshot: RouterSnapshot) {
    val rsrp = snapshot.nrRsrp ?: snapshot.lteRsrp
    val score = mpSignalScore(rsrp)
    HaiSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("مراقبة الإشارة", color = MpInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("${rsrp.metric()} dBm", color = MpInk, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(mpQualityLabel(score), color = qualityColor(score), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                repeat(5) { i ->
                    Box(
                        Modifier.width(8.dp).height((15 + i * 7).dp).clip(RoundedCornerShape(4.dp))
                            .background(if (i < (score / 20).coerceIn(0, 5)) qualityColor(score) else Color(0xFFE0E8F1))
                    )
                }
            }
        }
    }
}

@Composable
private fun MpNetwork(
    snapshot: RouterSnapshot,
    status: String,
    operationMessage: String,
    traffic: TrafficTelemetry?,
    thermal: ThermalTelemetry?,
    stability: ConnectionStabilityReport,
    busy: Boolean,
    onMode: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MpNetworkHero(snapshot) {} }
        item { MpModeCard(snapshot, busy, onMode) }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("هوية الاتصال", "القيم التي يعرضها الراوتر")
                    Spacer(Modifier.height(9.dp))
                    MpInfo("الموديل", snapshot.model ?: "—")
                    MpInfo("Cell ID", snapshot.cellId?.toString() ?: "—")
                    MpInfo("eNB / Sector", mpEnbSector(snapshot))
                    MpInfo("PCI / EARFCN", "${snapshot.pci ?: "—"} / ${snapshot.earfcn ?: "—"}")
                    MpInfo("MCC + MNC", snapshot.operatorCode ?: "—")
                    MpInfo("Carrier Aggregation", if (snapshot.caActive) "نشط ومتحقق" else "غير نشط / غير متحقق")
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("جودة الاتصال", "تشخيص حي من عينات متتابعة")
                    Spacer(Modifier.height(9.dp))
                    MpInfo("RSRP", "${(snapshot.nrRsrp ?: snapshot.lteRsrp).metric()} dBm")
                    MpInfo("SINR", "${(snapshot.nrSinr ?: snapshot.lteSinr).metric()} dB")
                    MpInfo("RSRQ", "${snapshot.lteRsrq.metric()} dB")
                    MpInfo("الثبات", stability.summary)
                    if (traffic != null) MpInfo("حركة WAN", "Telemetry متاح")
                    if (thermal != null) MpInfo("الحرارة", "Telemetry متاح")
                }
            }
        }
        item { StatusBanner(operationMessage.ifBlank { status }) }
    }
}

@Composable
private fun MpTowers(
    snapshot: RouterSnapshot,
    cells: List<NearbyCell>,
    scanBusy: Boolean,
    controlBusy: Boolean,
    target: TowerTarget?,
    guardEnabled: Boolean,
    guardStatus: TowerGuardStatus?,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onLockCell: (NearbyCell) -> Unit,
    onClear: () -> Unit,
    onGuard: (Boolean) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth(), padding = 0) {
                Column {
                    Box(Modifier.fillMaxWidth().height(330.dp)) { RealNetworkMap(Modifier.fillMaxSize()) }
                    Column(Modifier.padding(14.dp)) {
                        HaiSectionTitle("الخريطة الفعلية", "OpenStreetMap / OpenFreeMap — لا نضع برجًا في موقع غير موثق")
                        Spacer(Modifier.height(5.dp))
                        Text("المتصل: ${mpEnbSector(snapshot)} • PCI ${snapshot.pci ?: "—"} • EARFCN ${snapshot.earfcn ?: "—"}", color = MpMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("اختيار الخلية", "المسح من الراوتر نفسه", if (cells.isEmpty()) null else "${cells.size} نتيجة")
                    Spacer(Modifier.height(11.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onScan, enabled = !scanBusy && !controlBusy, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = MpBlue)
                        ) { Text(if (scanBusy) "يمسح…" else "↻ مسح الخلايا", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                        OutlinedButton(
                            onClick = onLockCurrent, enabled = snapshot.pci != null && snapshot.earfcn != null && !controlBusy,
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)
                        ) { Text("تثبيت الحالية", fontSize = 10.sp, fontWeight = FontWeight.Black) }
                    }
                    if (target != null) {
                        Spacer(Modifier.height(11.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("مُثبت: PCI ${target.pci} • EARFCN ${target.earfcn}", color = MpInk, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text(guardStatus?.message ?: "تم التحقق من القفل", color = MpMuted, fontSize = 9.sp, maxLines = 2)
                            }
                            Switch(checked = guardEnabled, onCheckedChange = onGuard, enabled = !controlBusy)
                        }
                        TextButton(onClick = onClear, enabled = !controlBusy) { Text("إزالة التثبيت", color = MpRed) }
                    }
                }
            }
        }
        if (cells.isEmpty()) {
            item { EmptyState("لا توجد نتائج بعد", "اضغط «مسح الخلايا». إذا أخفى الـFirmware الخلايا المجاورة سنعرض ذلك كما هو.", "⌁") }
        } else {
            items(cells) { cell -> MpCellRow(cell, controlBusy, onLockCell) }
        }
    }
}

@Composable
private fun MpCellRow(cell: NearbyCell, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val isNr = cell.rat.contains("NR", true) || cell.rat.contains("5G", true) || cell.band.orEmpty().startsWith("N", true)
    HaiSurface(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(if (isNr) MpSoftPurple else MpSoftBlue),
                    contentAlignment = Alignment.Center
                ) { Text(if (isNr) "5G" else "4G", color = if (isNr) Color(0xFF6D49D8) else MpBlue, fontSize = 11.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = MpInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("ARFCN ${cell.arfcn ?: "—"} • RSRP ${cell.rsrp.metric()} dBm • SINR ${cell.sinr.metric()} dB", color = MpMuted, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Button(
                onClick = { onLock(cell) },
                enabled = !isNr && cell.pci != null && cell.arfcn != null && !busy,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isNr) MpMuted else MpBlue)
            ) {
                Text(if (isNr) "تثبيت 5G غير متاح على هذا المسار" else "اختيار وتثبيت بضغطة", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MpBands(
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
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("المتصل فعليًا الآن", "حاملات الراديو التي أمكن قراءتها")
                    Spacer(Modifier.height(9.dp))
                    if (snapshot.cells.isEmpty()) Text("لا توجد Carrier details موثقة", color = MpMuted, fontSize = 11.sp)
                    else snapshot.cells.forEach { cell -> MpInfo(cell.role.toString(), "${cell.band ?: "—"} • PCI ${cell.pci ?: "—"} • ARFCN ${cell.arfcn ?: "—"}") }
                }
            }
        }
        item { MpBandSelector("4G LTE", capabilities.supportedLteBands.sorted(), selectedLte, busy, false, onLteToggle, onApplyLte) }
        item { MpBandSelector("5G NR", capabilities.supportedNrBands.sorted(), selectedNr, busy, true, onNrToggle, onApplyNr) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MpBandSelector(
    title: String,
    bands: List<Int>,
    selected: Set<Int>,
    busy: Boolean,
    nr: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit
) {
    HaiSurface(Modifier.fillMaxWidth()) {
        Column {
            HaiSectionTitle(title, "اختر ثم طبّق؛ النجاح لا يظهر قبل read-back مطابق", if (selected.isEmpty()) null else "${selected.size} محدد")
            Spacer(Modifier.height(11.dp))
            if (bands.isEmpty()) {
                EmptyState("غير متاح", "هذا الـProfile لا يعلن ترددات قابلة للتثبيت.", "≋")
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    bands.forEach { band ->
                        val checked = band in selected
                        Text(
                            "${if (nr) "n" else "B"}$band",
                            color = if (checked) Color.White else MpInk, fontSize = 11.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.clip(RoundedCornerShape(13.dp))
                                .background(if (checked) (if (nr) Color(0xFF7454DE) else MpBlue) else Color(0xFFF1F5FA))
                                .clickable(enabled = !busy) { onToggle(band) }.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
                Spacer(Modifier.height(13.dp))
                Button(
                    onClick = onApply, enabled = selected.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = if (nr) Color(0xFF7454DE) else MpBlue)
                ) { Text(if (busy) "جاري التحقق…" else "تطبيق الترددات المختارة", fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun MpClients(devices: List<ConnectedDevice>, aliases: MutableMap<String, String>) {
    var editing by remember { mutableStateOf<ConnectedDevice?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("المتواجدون الآن", "station_list + lan_station_list عندما يكشفهما الراوتر", "${devices.size}")
                    Spacer(Modifier.height(6.dp))
                    Text("الأسماء وIP وMAC تأتي من الراوتر؛ لا نختلق جهازًا مفقودًا.", color = MpMuted, fontSize = 10.sp)
                }
            }
        }
        if (devices.isEmpty()) {
            item { EmptyState("لا توجد أجهزة ظاهرة", "إما لا يوجد عملاء الآن، أو أن هذا الـFirmware لا يعيد قائمة العملاء في هذا الاستعلام.", "◉") }
        } else {
            items(devices, key = { it.macAddress }) { device ->
                MpDeviceRow(device, aliases[device.macAddress]) { editing = device }
            }
        }
    }

    editing?.let { device ->
        MpDeviceDialog(
            device = device,
            initialAlias = aliases[device.macAddress] ?: device.displayName,
            onSave = { aliases[device.macAddress] = it },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun MpDeviceRow(device: ConnectedDevice, alias: String?, onEdit: () -> Unit) {
    HaiSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).background(if (device.transport == DeviceTransport.WIFI) MpSoftBlue else MpSoftAmber),
                contentAlignment = Alignment.Center
            ) { Text(if (device.transport == DeviceTransport.WIFI) "Wi‑Fi" else "LAN", color = MpInk, fontSize = 9.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(alias?.takeIf { it.isNotBlank() } ?: device.displayName, color = MpInk, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(device.ipAddress ?: "IP غير ظاهر", color = MpMuted, fontSize = 10.sp)
                Text(device.macAddress, color = MpMuted, fontSize = 9.sp)
            }
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp)) {
                Text("تعديل", color = MpBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun MpDeviceDialog(device: ConnectedDevice, initialAlias: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var alias by remember(device.macAddress) { mutableStateOf(initialAlias) }
    var speed by remember(device.macAddress) { mutableStateOf(50f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة الجهاز", color = MpInk, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${device.ipAddress ?: "—"} • ${device.macAddress}", color = MpMuted, fontSize = 10.sp)
                OutlinedTextField(
                    value = alias, onValueChange = { alias = it.take(40) }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, label = { Text("اسم الجهاز داخل التطبيق") }
                )
                HorizontalDivider(color = MpBorder)
                Text("تحديد السرعة", color = MpInk, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 1f..200f, enabled = false)
                Text("غير متاح حتى يثبت الـFirmware QoS لكل جهاز مع read-back موثوق.", color = MpMuted, fontSize = 9.sp)
                MpUnavailable("حظر تطبيق معين", "لا يوجد API موثوق لكل جهاز حتى الآن")
                MpUnavailable("حظر بعد مدة", "يتطلب Block API موثوق + تنفيذ مجدول")
                MpUnavailable("حظر من الشبكة", "لن نرسل Blacklist خاصًا بموديل آخر")
                Text("لن يتحول أي خيار إلى زر فعلي قبل أن يستطيع التطبيق إثبات أن الراوتر نفذه.", color = MpRed, fontSize = 9.sp, lineHeight = 13.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(alias); onDismiss() }) { Text("حفظ", color = MpBlue, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("خروج", color = MpMuted) } },
        containerColor = Color.White, shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun MpUnavailable(title: String, reason: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MpInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(reason, color = MpMuted, fontSize = 9.sp)
        }
        Text("غير متاح", color = MpRed, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFEEEE)).padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

@Composable
private fun MpPlacement(active: Boolean, reading: PlacementReading?, soundEnabled: Boolean, onSound: (Boolean) -> Unit, onToggle: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HaiSectionTitle("الرادار المكاني", "حرّك الراوتر ببطء — التقييم يتغير من القراءات الحقيقية")
                    Spacer(Modifier.height(8.dp))
                    PlacementScore(reading)
                    Text(placementGuidanceText(reading?.guidance), color = MpInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 19.sp)
                    if (reading != null) {
                        Spacer(Modifier.height(5.dp))
                        Text("أفضل نقطة ${reading.bestScore}/100 • الفرق ${reading.deltaFromBest}", color = MpMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("الرنين الذكي", color = MpInk, fontWeight = FontWeight.Black)
                            Text("كلما تحسنت النقطة يتسارع الرنين", color = MpMuted, fontSize = 10.sp)
                        }
                        Switch(checked = soundEnabled, onCheckedChange = onSound)
                    }
                    Spacer(Modifier.height(11.dp))
                    Button(
                        onClick = onToggle, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (active) MpRed else MpBlue)
                    ) { Text(if (active) "■ إيقاف المساعد" else "▶ ابدأ البحث عن أفضل مكان", fontWeight = FontWeight.Black) }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("ما الذي يقيسه؟", "ليس قوة الإشارة وحدها")
                    Spacer(Modifier.height(8.dp))
                    MpInfo("الإشارة", "LTE/NR RSRP")
                    MpInfo("النظافة", "SINR")
                    MpInfo("الجودة", "RSRQ")
                    MpInfo("الثبات", "عدة عينات + تغير الخلية")
                    MpInfo("الثقة", "ترتفع مع اكتمال القراءات")
                }
            }
        }
    }
}

@Composable
private fun MpTools(
    runtime: RuntimeCapabilityReport?,
    sampleCount: Int,
    stability: ConnectionStabilityReport,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    backupAvailable: Boolean,
    capabilities: RouterCapabilities,
    busy: Boolean,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onRestore: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDisconnect: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("التحسين الذكي", color = MpInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text("يجرب إعدادات موثقة ويحتفظ بمسار رجوع", color = MpMuted, fontSize = 10.sp)
                        }
                        Switch(checked = smartMode, onCheckedChange = onSmartModeChange, enabled = !smartBusy && !busy)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OptimizationGoal.entries.forEach { goal ->
                            val selected = goal == smartGoal
                            Text(
                                mpGoal(goal), color = if (selected) Color.White else MpInk, fontSize = 10.sp, fontWeight = FontWeight.Black,
                                modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(if (selected) MpBlue else Color(0xFFF0F4F9))
                                    .clickable(enabled = !smartBusy) { onSmartGoalChange(goal) }.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(11.dp))
                    Button(
                        onClick = onOptimizeNow, enabled = !smartBusy && !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpBlue)
                    ) { Text(if (smartBusy) "يختبر أفضل إعداد…" else "✦ حسّن الآن", fontWeight = FontWeight.Black) }
                    smartReport?.message?.let { Spacer(Modifier.height(7.dp)); Text(it, color = MpMuted, fontSize = 10.sp) }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("الأمان والاستعادة", "نسخة أمان قبل التغييرات الحساسة")
                    Spacer(Modifier.height(9.dp))
                    Button(
                        onClick = onRestore, enabled = backupAvailable && !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF173A70))
                    ) { Text("↶ استعادة آخر نسخة أمان", fontWeight = FontWeight.Black) }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("قدرات الراوتر", "حسب Profile + Runtime probe")
                    Spacer(Modifier.height(8.dp))
                    MpInfo("LTE Band Lock", mpSupported(capabilities.supportsLteBandLock))
                    MpInfo("5G Band Lock", mpSupported(capabilities.supportsNrBandLock))
                    MpInfo("Cell Lock", mpSupported(capabilities.supportsCellLock))
                    MpInfo("Carrier Aggregation", mpSupported(capabilities.supportsCarrierAggregationRead))
                    MpInfo("Runtime probe", if (runtime == null) "لم يكتمل" else "مكتمل")
                    MpInfo("عينات الثبات", sampleCount.toString())
                    MpInfo("الاستقرار", stability.summary)
                    if (capabilities.supportsAntennaControl) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..3).forEach { state ->
                                OutlinedButton(onClick = { onAntennaState(state) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("هوائي $state", fontSize = 9.sp) }
                            }
                        }
                    }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("التشخيص", "انسخ التقرير عند اختلاف Firmware أو فشل وظيفة")
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) { Text("نسخ التقرير") }
                        OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) { Text("مشاركة") }
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("قطع الاتصال بالراوتر", color = MpRed, fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun MpInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = MpMuted, fontSize = 10.sp, modifier = Modifier.weight(.42f))
        Text(value, color = MpInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(.58f))
    }
    HorizontalDivider(color = MpBorder.copy(alpha = .62f))
}

private fun mpClean(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() && it != "---" && !it.equals("null", true) }

private fun mpBands(snapshot: RouterSnapshot): List<String> {
    val cells = snapshot.cells.mapNotNull { it.band?.uppercase() }.distinct()
    if (cells.isNotEmpty()) return cells
    return listOfNotNull(snapshot.lteBand?.uppercase(), snapshot.nrBand?.uppercase()).distinct()
}

private fun mpBandSummary(snapshot: RouterSnapshot): String {
    val bands = mpBands(snapshot)
    return if (bands.isEmpty()) "لا توجد باندات موثقة" else bands.joinToString(" + ") + if (snapshot.caActive) " • مدمجة" else ""
}

private fun mpGeneration(snapshot: RouterSnapshot): String {
    val type = snapshot.networkType.orEmpty().uppercase()
    return when {
        "5G" in type || snapshot.nrRsrp != null -> "5G"
        "4G" in type || snapshot.lteRsrp != null -> "4G"
        else -> "—"
    }
}

private fun mpEnbSector(snapshot: RouterSnapshot): String {
    val id = snapshot.cellId ?: return "eNB غير متاح"
    return "eNB ${id ushr 8} / قطاع ${id and 0xFF}"
}

private fun mpSignalScore(rsrp: Double?): Int = when {
    rsrp == null -> 0
    rsrp >= -85 -> 95
    rsrp >= -95 -> 80
    rsrp >= -105 -> 62
    rsrp >= -115 -> 42
    else -> 22
}

private fun mpSinrScore(sinr: Double?): Int = when {
    sinr == null -> 0
    sinr >= 20 -> 95
    sinr >= 13 -> 82
    sinr >= 5 -> 65
    sinr >= 0 -> 48
    else -> 25
}

private fun mpRsrqScore(rsrq: Double?): Int = when {
    rsrq == null -> 0
    rsrq >= -10 -> 92
    rsrq >= -15 -> 74
    rsrq >= -20 -> 50
    else -> 25
}

private fun mpQualityLabel(score: Int) = when {
    score >= 90 -> "ممتاز"
    score >= 75 -> "جيد جدًا"
    score >= 58 -> "جيد"
    score >= 40 -> "متوسط"
    score > 0 -> "ضعيف"
    else -> "غير مؤكد"
}

private fun mpGoal(goal: OptimizationGoal) = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun mpSupported(value: Boolean) = if (value) "مدعوم" else "غير متاح"
