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
import com.malik.ztesmartmanager.core.model.CellRole
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

private enum class HaiScreen { HOME, NETWORK, TOWERS, BANDS, CLIENTS, PLACEMENT, TOOLS }

@OptIn(ExperimentalLayoutApi::class)
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
        val history = remember { mutableStateListOf(HaiScreen.HOME) }
        val screen = history.last()
        val aliases = remember { mutableStateMapOf<String, String>() }
        var soundEnabled by rememberSaveable { mutableStateOf(true) }

        fun navigate(target: HaiScreen) {
            if (history.lastOrNull() == target) return
            history += target
            if (history.size > 8) history.removeAt(1)
        }

        fun root(target: HaiScreen) {
            history.clear()
            history += target
        }

        BackHandler(enabled = history.size > 1 || screen != HaiScreen.HOME) {
            if (history.size > 1) history.removeAt(history.lastIndex) else root(HaiScreen.HOME)
        }

        val wifiDevices = remember(snapshot?.raw?.get("station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("station_list"), DeviceTransport.WIFI)
        }
        val lanDevices = remember(snapshot?.raw?.get("lan_station_list")) {
            ConnectedDeviceParser.parseValue(snapshot?.raw?.get("lan_station_list"), DeviceTransport.LAN)
        }
        val devices = remember(wifiDevices, lanDevices) { ConnectedDeviceParser.merge(wifiDevices, lanDevices) }

        PlacementSoundEffect(soundEnabled, placementMode && screen == HaiScreen.PLACEMENT, placementReading)

        Box(Modifier.fillMaxSize().background(HaiBg)) {
            Column(Modifier.fillMaxSize()) {
                MasterpieceHeader(
                    snapshot = snapshot,
                    screen = screen,
                    onBack = { if (history.size > 1) history.removeAt(history.lastIndex) else root(HaiScreen.HOME) },
                    onNetwork = { navigate(HaiScreen.NETWORK) }
                )

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        snapshot == null -> EmptyState(
                            title = "جاري تجهيز الاتصال",
                            body = if (status.isBlank()) "بانتظار بيانات الراوتر…" else status,
                            glyph = "⌁"
                        )
                        screen == HaiScreen.HOME -> HomeScreen(
                            snapshot = snapshot,
                            status = status,
                            operationMessage = operationMessage,
                            performance = lastPerformance,
                            speedBusy = speedBusy,
                            controlBusy = controlBusy,
                            placementReading = placementReading,
                            devices = devices,
                            nearbyCells = nearbyCells,
                            onSpeedTest = onSpeedTest,
                            onMode = onSetNetworkMode,
                            onNetwork = { navigate(HaiScreen.NETWORK) },
                            onTowers = { navigate(HaiScreen.TOWERS) },
                            onBands = { navigate(HaiScreen.BANDS) },
                            onClients = { navigate(HaiScreen.CLIENTS) },
                            onPlacement = { navigate(HaiScreen.PLACEMENT) },
                            onTools = { navigate(HaiScreen.TOOLS) }
                        )
                        screen == HaiScreen.NETWORK -> NetworkScreen(snapshot, status, operationMessage, traffic, thermal, stability, controlBusy, onSetNetworkMode)
                        screen == HaiScreen.TOWERS -> TowersScreen(
                            snapshot, nearbyCells, scanBusy, controlBusy, towerTarget, towerGuardEnabled, towerGuardStatus,
                            onScanCells, onLockCurrentCell, onLockNearbyCell, onClearCellLock, onTowerGuardChange
                        )
                        screen == HaiScreen.BANDS -> BandsScreen(
                            snapshot, capabilities, selectedLte, selectedNr, controlBusy,
                            onLteToggle, onNrToggle, onApplyLte, onApplyNr
                        )
                        screen == HaiScreen.CLIENTS -> ClientsScreen(devices, aliases)
                        screen == HaiScreen.PLACEMENT -> PlacementScreen(
                            placementMode, placementReading, soundEnabled,
                            onSound = { soundEnabled = it }, onToggle = onPlacementToggle
                        )
                        screen == HaiScreen.TOOLS -> ToolsScreen(
                            runtime = runtime,
                            smartMode = smartMode,
                            smartGoal = smartGoal,
                            smartBusy = smartBusy,
                            smartReport = smartReport,
                            safetyBackupAvailable = safetyBackupAvailable,
                            capabilities = capabilities,
                            controlBusy = controlBusy,
                            onSmartModeChange = onSmartModeChange,
                            onSmartGoalChange = onSmartGoalChange,
                            onOptimizeNow = onOptimizeNow,
                            onRestoreSafetyBackup = onRestoreSafetyBackup,
                            onAntennaState = onAntennaState,
                            onCopyDiagnostics = onCopyDiagnostics,
                            onShareDiagnostics = onShareDiagnostics,
                            onDisconnect = onDisconnect
                        )
                    }
                }

                MasterpieceBottomBar(screen) { target -> root(target) }
            }
        }
    }
}

@Composable
private fun MasterpieceHeader(snapshot: RouterSnapshot?, screen: HaiScreen, onBack: () -> Unit, onNetwork: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (screen != HaiScreen.HOME) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("‹", color = HaiInk, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
        } else {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(HaiSoftBlue).clickable(onClick = onNetwork),
                contentAlignment = Alignment.Center
            ) { Text("H", color = HaiBlue, fontSize = 18.sp, fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(screenTitle(screen), color = HaiInk, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(
                if (screen == HaiScreen.HOME) "كل شبكتك أمامك — بدون تخمين" else "ZTE Smart HAI",
                color = HaiMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(if (snapshot != null) HaiSoftGreen else Color(0xFFFFEEEE))
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (snapshot != null) HaiGreen else HaiRed))
            Spacer(Modifier.width(6.dp))
            Text(if (snapshot != null) "متصل" else "غير متصل", color = HaiInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun screenTitle(screen: HaiScreen): String = when (screen) {
    HaiScreen.HOME -> "HAI Network"
    HaiScreen.NETWORK -> "الشبكة الآن"
    HaiScreen.TOWERS -> "الأبراج والخريطة"
    HaiScreen.BANDS -> "الترددات"
    HaiScreen.CLIENTS -> "المتواجدون الآن"
    HaiScreen.PLACEMENT -> "أفضل مكان للراوتر"
    HaiScreen.TOOLS -> "الأدوات الذكية"
}

@Composable
private fun MasterpieceBottomBar(screen: HaiScreen, onSelect: (HaiScreen) -> Unit) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple(HaiScreen.HOME, "الرئيسية", "⌂"),
            Triple(HaiScreen.TOWERS, "الأبراج", "⌁"),
            Triple(HaiScreen.BANDS, "الترددات", "≋"),
            Triple(HaiScreen.CLIENTS, "الأجهزة", "◉"),
            Triple(HaiScreen.TOOLS, "الأدوات", "✦")
        )
        items.forEach { (target, label, glyph) ->
            NavigationBarItem(
                selected = screen == target,
                onClick = { onSelect(target) },
                icon = { Text(glyph, fontSize = 19.sp, color = if (screen == target) HaiBlue else HaiMuted) },
                label = { Text(label, fontSize = 9.sp, fontWeight = if (screen == target) FontWeight.Black else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HaiBlue,
                    selectedTextColor = HaiBlue,
                    indicatorColor = HaiSoftBlue,
                    unselectedIconColor = HaiMuted,
                    unselectedTextColor = HaiMuted
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeScreen(
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
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { NetworkHero(snapshot, onNetwork) }
        item {
            BoxWithConstraints {
                val compact = maxWidth < 380.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpeedCard(performance, speedBusy, onSpeedTest)
                        NetworkModesCard(snapshot, controlBusy, onMode)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { SpeedCard(performance, speedBusy, onSpeedTest) }
                        Box(Modifier.weight(1f)) { NetworkModesCard(snapshot, controlBusy, onMode) }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { ActionTile("الأبراج", "مسح حقيقي واختيار الخلية", "⌁", HaiSoftGreen, onClick = onTowers) }
                Box(Modifier.weight(1f)) { ActionTile("أفضل مكان", placementReading?.score?.let { "آخر تقييم ${it.total}/100" } ?: "مساعد حي مع رنين", "◎", HaiSoftPurple, onClick = onPlacement) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { ActionTile("المتواجدون الآن", "${devices.size} جهاز ظاهر للراوتر", "◉", HaiSoftBlue, onClick = onClients) }
                Box(Modifier.weight(1f)) { ActionTile("الترددات", activeBandSummary(snapshot), "≋", HaiSoftAmber, onClick = onBands) }
            }
        }
        item { TowerQuickCard(snapshot, nearbyCells, onTowers) }
        item { LiveSignalCard(snapshot) }
        item { ActionTile("الأدوات الذكية", "تحسين موثّق • نسخة أمان • تشخيص", "✦", Color(0xFFE9F5FF), onClick = onTools) }
        if (operationMessage.isNotBlank()) item { StatusBanner(operationMessage) }
        else if (status.isNotBlank()) item { StatusBanner(status) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun NetworkHero(snapshot: RouterSnapshot, onClick: () -> Unit) {
    val operator = cleanRaw(snapshot.raw["network_provider_fullname"])
        ?: cleanRaw(snapshot.raw["network_provider"])
        ?: snapshot.operatorCode
        ?: "الشبكة"
    val technology = snapshot.networkType ?: "غير مؤكد"
    val bands = activeBands(snapshot)
    val mainRsrp = snapshot.nrRsrp ?: snapshot.lteRsrp
    val mainSinr = snapshot.nrSinr ?: snapshot.lteSinr

    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFFBFD9FF))
    ) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color.White, Color(0xFFF0F7FF), Color(0xFFE4F1FF)))
            ).padding(17.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(HaiGreen))
                        Spacer(Modifier.width(6.dp))
                        Text("LIVE • قراءة من الراوتر", color = HaiGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(operator, color = HaiInk, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(technology, color = HaiBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.size(84.dp).clip(RoundedCornerShape(25.dp)).background(Brush.linearGradient(listOf(HaiBlue, HaiCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(technologyLargeLabel(snapshot), color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Text(if (snapshot.caActive) "CA نشط" else "متصل", color = Color.White.copy(alpha = .86f), fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (bands.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    bands.forEachIndexed { index, band ->
                        val role = when {
                            band.startsWith("N", true) -> "5G"
                            index == 0 -> "أساسي"
                            else -> "مدمج"
                        }
                        Text(
                            "$band  $role",
                            color = if (band.startsWith("N", true)) Color(0xFF6A42D9) else HaiBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .88f)).padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.weight(1f)) { MetricPill("RSRP", mainRsrp.metric(), "dBm", qualityColor(signalScore(mainRsrp))) }
                Box(Modifier.weight(1f)) { MetricPill("SINR", mainSinr.metric(), "dB", qualityColor(sinrScore(mainSinr))) }
                Box(Modifier.weight(1f)) { MetricPill("RSRQ", snapshot.lteRsrq.metric(), "dB", qualityColor(rsrqScore(snapshot.lteRsrq))) }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PCI ${snapshot.pci ?: "—"}", color = HaiMuted, fontSize = 10.sp)
                Text("  •  EARFCN ${snapshot.earfcn ?: "—"}", color = HaiMuted, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text("التفاصيل  ←", color = HaiBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SpeedCard(performance: NetworkPerformance?, busy: Boolean, onTest: () -> Unit) {
    HaiSurface(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HaiSectionTitle("قياس السرعة", "قياس إنترنت فعلي عبر Cloudflare")
            SpeedGauge(performance?.downloadMbps, busy, Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ping", color = HaiMuted, fontSize = 9.sp)
                    Text("${performance?.latencyMs.metric()} ms", color = HaiInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Jitter", color = HaiMuted, fontSize = 9.sp)
                    Text("${performance?.jitterMs.metric()} ms", color = HaiInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Loss", color = HaiMuted, fontSize = 9.sp)
                    Text("${performance?.packetLossPercent.metric()}%", color = HaiInk, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onTest,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HaiBlue)
            ) { Text(if (busy) "جاري القياس…" else "▶ ابدأ القياس", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun NetworkModesCard(snapshot: RouterSnapshot, busy: Boolean, onMode: (String) -> Unit) {
    val raw = cleanRaw(snapshot.raw["BearerPreference"]) ?: cleanRaw(snapshot.raw["current_network_mode"])
    HaiSurface(Modifier.fillMaxWidth()) {
        Column {
            HaiSectionTitle("أوضاع الشبكة", "يُقرأ الوضع بعد التطبيق للتحقق")
            Spacer(Modifier.height(12.dp))
            val modes = listOf(
                Triple("تلقائي", "WL_AND_5G", "A"),
                Triple("5G فقط", "Only_5G", "5G"),
                Triple("4G فقط", "Only_LTE", "4G"),
                Triple("3G فقط", "Only_WCDMA", "3G")
            )
            modes.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (label, code, icon) ->
                        val selected = raw.equals(code, true)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(if (selected) HaiSoftBlue else Color(0xFFF6F9FC))
                                .clickable(enabled = !busy) { onMode(code) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(icon, color = if (selected) HaiBlue else HaiInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text(label, color = if (selected) HaiBlue else HaiInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(if (raw == null) "الوضع الحالي غير مكشوف من الـFirmware" else "الحالي: $raw", color = HaiMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun TowerQuickCard(snapshot: RouterSnapshot, cells: List<NearbyCell>, onOpen: () -> Unit) {
    val eci = snapshot.cellId
    val enb = eci?.ushr(8)
    val sector = eci?.and(0xFF)
    HaiSurface(Modifier.fillMaxWidth()) {
        Column {
            HaiSectionTitle("البرج المتصل", "هوية من Cell ID / PCI / EARFCN — الإحداثيات لا تُخمن", if (cells.isEmpty()) null else "${cells.size} خلية")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { MetricPill("eNB", enb?.toString() ?: "—") }
                Box(Modifier.weight(1f)) { MetricPill("Sector", sector?.toString() ?: "—") }
                Box(Modifier.weight(1f)) { MetricPill("PCI", snapshot.pci?.toString() ?: "—") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Text("⌁ افتح الخريطة واختر خلية", color = HaiBlue, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun LiveSignalCard(snapshot: RouterSnapshot) {
    val score = signalScore(snapshot.nrRsrp ?: snapshot.lteRsrp)
    HaiSurface(Modifier.fillMaxWidth()) {
        Column {
            HaiSectionTitle("مراقبة الإشارة المباشرة", "تتحدث تلقائيًا من الراوتر", "LIVE")
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${(snapshot.nrRsrp ?: snapshot.lteRsrp).metric()} dBm", color = HaiInk, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text(qualityLabel(score), color = qualityColor(score), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                    repeat(5) { index ->
                        Box(
                            Modifier.width(8.dp).height((15 + index * 7).dp).clip(RoundedCornerShape(4.dp))
                                .background(if (index < ((score / 20).coerceIn(0, 5))) qualityColor(score) else Color(0xFFE0E8F1))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkScreen(
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
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { NetworkHero(snapshot) {} }
        item { NetworkModesCard(snapshot, busy, onMode) }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("هوية الاتصال", "قيم مباشرة بدون استنتاج غير مؤكد")
                    Spacer(Modifier.height(12.dp))
                    InfoRow("الموديل", snapshot.model ?: "—")
                    InfoRow("Cell ID", snapshot.cellId?.toString() ?: "—")
                    InfoRow("eNB / Sector", enbSector(snapshot))
                    InfoRow("PCI / EARFCN", "${snapshot.pci ?: "—"} / ${snapshot.earfcn ?: "—"}")
                    InfoRow("MCC + MNC", snapshot.operatorCode ?: "—")
                    InfoRow("Carrier Aggregation", if (snapshot.caActive) "نشط ومتحقق" else "غير نشط / غير متحقق")
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("الصحة اللحظية", "تشخيص من القياسات الحالية")
                    Spacer(Modifier.height(12.dp))
                    InfoRow("RSRP", "${(snapshot.nrRsrp ?: snapshot.lteRsrp).metric()} dBm")
                    InfoRow("SINR", "${(snapshot.nrSinr ?: snapshot.lteSinr).metric()} dB")
                    InfoRow("RSRQ", "${snapshot.lteRsrq.metric()} dB")
                    InfoRow("حرارة 4G", "${snapshot.modem4gTemperature.metric()} °C")
                    InfoRow("حرارة 5G", "${snapshot.modem5gTemperature.metric()} °C")
                    InfoRow("الثبات", stability.toString().take(80))
                }
            }
        }
        if (traffic != null) item { StatusBanner("بيانات الحركة متاحة من الراوتر وتظهر في تقرير التشخيص التفصيلي") }
        if (thermal != null) item { StatusBanner("تم اكتشاف Telemetry حراري من الـFirmware") }
        if (operationMessage.isNotBlank()) item { StatusBanner(operationMessage) }
        else item { StatusBanner(status) }
    }
}

@Composable
private fun TowersScreen(
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
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth(), padding = 0) {
                Column {
                    Box(Modifier.fillMaxWidth().height(330.dp)) { RealNetworkMap(Modifier.fillMaxSize()) }
                    Column(Modifier.padding(14.dp)) {
                        HaiSectionTitle("الخريطة الفعلية", "لا نضع برجًا في مكان غير موثق")
                        Spacer(Modifier.height(7.dp))
                        Text("الهوية المتصلة: ${enbSector(snapshot)} • PCI ${snapshot.pci ?: "—"} • EARFCN ${snapshot.earfcn ?: "—"}", color = HaiMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("اختيار البرج / الخلية", "المسح يعتمد على الخلايا التي يعرضها الراوتر نفسه", if (cells.isEmpty()) null else "${cells.size} نتيجة")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onScan,
                            enabled = !scanBusy && !controlBusy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HaiBlue)
                        ) { Text(if (scanBusy) "يمسح…" else "↻ مسح الخلايا", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                        OutlinedButton(
                            onClick = onLockCurrent,
                            enabled = snapshot.pci != null && snapshot.earfcn != null && !controlBusy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(15.dp)
                        ) { Text("تثبيت الحالية", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                    }
                    if (target != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("مُثبت: PCI ${target.pci} • EARFCN ${target.earfcn}", color = HaiInk, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text(guardStatus?.message ?: "القفل موثق بالـread-back", color = HaiMuted, fontSize = 9.sp, maxLines = 2)
                            }
                            Switch(checked = guardEnabled, onCheckedChange = onGuard, enabled = !controlBusy)
                        }
                        TextButton(onClick = onClear, enabled = !controlBusy) { Text("إزالة التثبيت", color = HaiRed) }
                    }
                }
            }
        }
        if (cells.isEmpty()) {
            item { EmptyState("لا توجد نتائج بعد", "اضغط «مسح الخلايا». إذا لم يكشف الـFirmware الخلايا المجاورة سنعرض ذلك صراحةً.", "⌁") }
        } else {
            items(cells) { cell -> TowerCellRow(cell, controlBusy, onLockCell) }
        }
    }
}

@Composable
private fun TowerCellRow(cell: NearbyCell, busy: Boolean, onLock: (NearbyCell) -> Unit) {
    val isNr = cell.rat.contains("NR", true) || cell.rat.contains("5G", true) || cell.band.orEmpty().startsWith("N", true)
    HaiSurface(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(if (isNr) HaiSoftPurple else HaiSoftBlue), contentAlignment = Alignment.Center) {
                    Text(if (isNr) "5G" else "4G", color = if (isNr) Color(0xFF6A42D9) else HaiBlue, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${cell.band ?: cell.rat} • PCI ${cell.pci ?: "—"}", color = HaiInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("ARFCN ${cell.arfcn ?: "—"} • RSRP ${cell.rsrp.metric()} dBm", color = HaiMuted, fontSize = 10.sp)
                }
                cell.evidenceScore?.let {
                    Text("$it/100", color = qualityColor(it), fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onLock(cell) },
                enabled = !isNr && cell.pci != null && cell.arfcn != null && !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isNr) HaiMuted else HaiBlue)
            ) {
                Text(if (isNr) "تثبيت 5G غير متاح على هذا المسار" else "اختيار وتثبيت بضغطة", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BandsScreen(
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
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("المتصل فعليًا الآن", "هذه ليست قائمة إعدادات؛ بل الحوامل المرصودة")
                    Spacer(Modifier.height(10.dp))
                    if (snapshot.cells.isEmpty()) Text("لا توجد Carrier details موثقة", color = HaiMuted, fontSize = 11.sp)
                    else snapshot.cells.forEach { cell ->
                        InfoRow(
                            when (cell.role) { CellRole.PRIMARY -> "LTE أساسي"; CellRole.SECONDARY -> "LTE مدمج"; CellRole.NR -> "5G NR" },
                            "${cell.band ?: "—"} • PCI ${cell.pci ?: "—"} • ARFCN ${cell.arfcn ?: "—"}"
                        )
                    }
                }
            }
        }
        item {
            BandSelectorCard("4G LTE", capabilities.supportedLteBands.sorted(), selectedLte, busy, onLteToggle, onApplyLte)
        }
        item {
            BandSelectorCard("5G NR", capabilities.supportedNrBands.sorted(), selectedNr, busy, onNrToggle, onApplyNr, nr = true)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BandSelectorCard(
    title: String,
    bands: List<Int>,
    selected: Set<Int>,
    busy: Boolean,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit,
    nr: Boolean = false
) {
    HaiSurface(Modifier.fillMaxWidth()) {
        Column {
            HaiSectionTitle(title, "اختر ثم طبّق؛ التطبيق يتحقق من read-back قبل إعلان النجاح", if (selected.isEmpty()) null else "${selected.size} محدد")
            Spacer(Modifier.height(12.dp))
            if (bands.isEmpty()) {
                EmptyState("غير متاح", "هذا الـProfile لا يعلن ترددات قابلة للتثبيت.", "≋")
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    bands.forEach { band ->
                        val isSelected = band in selected
                        Text(
                            "${if (nr) "n" else "B"}$band",
                            color = if (isSelected) Color.White else HaiInk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.clip(RoundedCornerShape(13.dp))
                                .background(if (isSelected) (if (nr) Color(0xFF7454DE) else HaiBlue) else Color(0xFFF1F5FA))
                                .clickable(enabled = !busy) { onToggle(band) }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onApply,
                    enabled = selected.isNotEmpty() && !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (nr) Color(0xFF7454DE) else HaiBlue)
                ) { Text(if (busy) "جاري التحقق…" else "تطبيق الترددات المختارة", fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun ClientsScreen(devices: List<ConnectedDevice>, aliases: MutableMap<String, String>) {
    var editing by remember { mutableStateOf<ConnectedDevice?>(null) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("المتواجدون الآن", "station_list + lan_station_list عندما يكشفهما الراوتر", "${devices.size}")
                    Spacer(Modifier.height(8.dp))
                    Text("لا نختلق أسماء أو أجهزة. إذا أخفى الـFirmware القائمة فستظهر فارغة.", color = HaiMuted, fontSize = 10.sp)
                }
            }
        }
        if (devices.isEmpty()) {
            item { EmptyState("لا توجد أجهزة ظاهرة", "قد لا يكون هناك عملاء متصلون، أو قد لا يدعم الـFirmware إرجاع قائمة العملاء ضمن هذا الاستعلام.", "◉") }
        } else {
            items(devices, key = { it.macAddress }) { device ->
                DeviceRow(device, aliases[device.macAddress], onEdit = { editing = device })
            }
        }
    }

    editing?.let { device ->
        DeviceEditorDialog(
            device = device,
            currentAlias = aliases[device.macAddress],
            onSaveAlias = { alias -> aliases[device.macAddress] = alias },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun DeviceRow(device: ConnectedDevice, alias: String?, onEdit: () -> Unit) {
    HaiSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(17.dp)).background(if (device.transport == DeviceTransport.WIFI) HaiSoftBlue else HaiSoftAmber), contentAlignment = Alignment.Center) {
                Text(if (device.transport == DeviceTransport.WIFI) "Wi‑Fi" else "LAN", color = HaiInk, fontWeight = FontWeight.Black, fontSize = 9.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(alias?.takeIf { it.isNotBlank() } ?: device.displayName, color = HaiInk, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(device.ipAddress ?: "IP غير ظاهر", color = HaiMuted, fontSize = 10.sp)
                Text(device.macAddress, color = HaiMuted, fontSize = 9.sp)
            }
            OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp)) {
                Text("تعديل", color = HaiBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun DeviceEditorDialog(
    device: ConnectedDevice,
    currentAlias: String?,
    onSaveAlias: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var alias by remember(device.macAddress) { mutableStateOf(currentAlias ?: device.displayName) }
    var speed by remember(device.macAddress) { mutableStateOf(50f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة ${device.displayName}", color = HaiInk, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("${device.ipAddress ?: "—"} • ${device.macAddress}", color = HaiMuted, fontSize = 10.sp)
                Column {
                    Text("الاسم المحلي", color = HaiInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF3F6FA)).padding(10.dp)) {
                        Text(alias, color = HaiInk, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("اسم داخل التطبيق", color = HaiMuted, fontSize = 9.sp)
                    }
                }
                HorizontalDivider(color = HaiBorder)
                Text("تحديد السرعة", color = HaiInk, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 1f..200f, enabled = false)
                Text("غير متاح حتى يثبت الـFirmware وجود QoS لكل جهاز مع read-back موثوق.", color = HaiMuted, fontSize = 9.sp)
                FeatureUnavailableRow("حظر تطبيق معين", "لا يوجد API موثوق لكل جهاز حتى الآن")
                FeatureUnavailableRow("حظر بعد مدة", "يتطلب Block API موثوق + تنفيذ مجدول")
                FeatureUnavailableRow("حظر من الشبكة", "لن نرسل أمر Blacklist خاص بموديل آخر")
                Text("هذه القيود مقصودة: التطبيق لا يعرض زرًا فعالًا قبل أن يستطيع إثبات تنفيذ الراوتر للأمر.", color = HaiRed, fontSize = 9.sp, lineHeight = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveAlias(alias); onDismiss() }) { Text("حفظ الاسم", color = HaiBlue, fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("خروج", color = HaiMuted) } },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun FeatureUnavailableRow(title: String, reason: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = HaiInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(reason, color = HaiMuted, fontSize = 9.sp)
        }
        Text("غير متاح", color = HaiRed, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFEEEE)).padding(horizontal = 8.dp, vertical = 5.dp))
    }
}

@Composable
private fun PlacementScreen(
    active: Boolean,
    reading: PlacementReading?,
    soundEnabled: Boolean,
    onSound: (Boolean) -> Unit,
    onToggle: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HaiSectionTitle("الرادار المكاني", "حرّك الراوتر ببطء — التقييم يتغير من القراءات الحقيقية")
                    Spacer(Modifier.height(8.dp))
                    PlacementScore(reading)
                    Text(placementGuidanceText(reading?.guidance), color = HaiInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 19.sp)
                    Spacer(Modifier.height(6.dp))
                    if (reading != null) {
                        Text("أفضل نقطة ${reading.bestScore}/100 • الفرق ${reading.deltaFromBest}", color = HaiMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("الرنين الذكي", color = HaiInk, fontWeight = FontWeight.Black)
                            Text("كلما تحسنت النقطة يتسارع الرنين", color = HaiMuted, fontSize = 10.sp)
                        }
                        Switch(checked = soundEnabled, onCheckedChange = onSound)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onToggle,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (active) HaiRed else HaiBlue)
                    ) { Text(if (active) "■ إيقاف المساعد" else "▶ ابدأ البحث عن أفضل مكان", fontWeight = FontWeight.Black) }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("ما الذي يقيسه؟", "لا يعتمد على قوة الإشارة وحدها")
                    Spacer(Modifier.height(10.dp))
                    InfoRow("الإشارة", "LTE/NR RSRP")
                    InfoRow("النظافة", "SINR")
                    InfoRow("الجودة", "RSRQ")
                    InfoRow("الثبات", "تذبذب عدة عينات + تغير الخلية")
                    InfoRow("الثقة", "ترتفع مع اكتمال العينات والقياسات")
                }
            }
        }
    }
}

@Composable
private fun ToolsScreen(
    runtime: RuntimeCapabilityReport?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    safetyBackupAvailable: Boolean,
    capabilities: RouterCapabilities,
    controlBusy: Boolean,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onRestoreSafetyBackup: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onCopyDiagnostics: () -> Unit,
    onShareDiagnostics: () -> Unit,
    onDisconnect: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("التحسين الذكي", color = HaiInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text("يجرب مرشحين موثقين ويرجع للوضع الأصلي عند الفشل", color = HaiMuted, fontSize = 10.sp)
                        }
                        Switch(checked = smartMode, onCheckedChange = onSmartModeChange, enabled = !smartBusy && !controlBusy)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OptimizationGoal.entries.forEach { goal ->
                            val selected = smartGoal == goal
                            Text(
                                goalLabel(goal),
                                color = if (selected) Color.White else HaiInk,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(if (selected) HaiBlue else Color(0xFFF0F4F9))
                                    .clickable(enabled = !smartBusy) { onSmartGoalChange(goal) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onOptimizeNow,
                        enabled = !smartBusy && !controlBusy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HaiBlue)
                    ) { Text(if (smartBusy) "يختبر أفضل إعداد…" else "✦ حسّن الآن", fontWeight = FontWeight.Black) }
                    smartReport?.message?.let { Spacer(Modifier.height(8.dp)); Text(it, color = HaiMuted, fontSize = 10.sp) }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("الأمان والاستعادة", "نسخة قبل أي تغيير حساس")
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onRestoreSafetyBackup,
                        enabled = safetyBackupAvailable && !controlBusy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF173A70))
                    ) { Text("↶ استعادة آخر نسخة أمان", fontWeight = FontWeight.Black) }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("قدرات الراوتر", "الوظائف لا تتفعل إلا حسب الـProfile وRuntime Probe")
                    Spacer(Modifier.height(10.dp))
                    InfoRow("LTE Band Lock", yesNo(capabilities.supportsLteBandLock))
                    InfoRow("5G Band Lock", yesNo(capabilities.supportsNrBandLock))
                    InfoRow("Cell Lock", yesNo(capabilities.supportsCellLock))
                    InfoRow("Carrier Aggregation", yesNo(capabilities.supportsCarrierAggregationRead))
                    InfoRow("Runtime probe", if (runtime == null) "لم يكتمل" else "مكتمل")
                    if (capabilities.supportsAntennaControl) {
                        Spacer(Modifier.height(8.dp))
                        Text("الهوائي: الأمر متوقف إن لم يوجد read-back آمن", color = HaiMuted, fontSize = 9.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2, 3).forEach { state ->
                                OutlinedButton(onClick = { onAntennaState(state) }, enabled = !controlBusy, modifier = Modifier.weight(1f)) { Text("$state") }
                            }
                        }
                    }
                }
            }
        }
        item {
            HaiSurface(Modifier.fillMaxWidth()) {
                Column {
                    HaiSectionTitle("التشخيص", "مفيد عند اختلاف Firmware أو فشل أمر")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCopyDiagnostics, modifier = Modifier.weight(1f)) { Text("نسخ التقرير") }
                        OutlinedButton(onClick = onShareDiagnostics, modifier = Modifier.weight(1f)) { Text("مشاركة") }
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("قطع الاتصال بالراوتر", color = HaiRed, fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = HaiMuted, fontSize = 10.sp, modifier = Modifier.weight(.42f))
        Text(value, color = HaiInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(.58f))
    }
    HorizontalDivider(color = HaiBorder.copy(alpha = .6f))
}

private fun activeBands(snapshot: RouterSnapshot): List<String> {
    val fromCells = snapshot.cells.mapNotNull { it.band?.uppercase() }.distinct()
    if (fromCells.isNotEmpty()) return fromCells
    return listOfNotNull(snapshot.lteBand?.uppercase(), snapshot.nrBand?.uppercase()).distinct()
}

private fun activeBandSummary(snapshot: RouterSnapshot): String {
    val bands = activeBands(snapshot)
    return when {
        bands.isEmpty() -> "لا توجد باندات موثقة"
        snapshot.caActive -> bands.joinToString(" + ") + " • مدمجة"
        else -> bands.joinToString(" + ")
    }
}

private fun technologyLargeLabel(snapshot: RouterSnapshot): String {
    val type = snapshot.networkType.orEmpty().uppercase()
    return when {
        "5G" in type || snapshot.nrRsrp != null -> "5G"
        "4G" in type || snapshot.lteRsrp != null -> "4G"
        else -> "—"
    }
}

private fun enbSector(snapshot: RouterSnapshot): String {
    val id = snapshot.cellId ?: return "غير متاح"
    return "${id ushr 8} / ${id and 0xFF}"
}

private fun cleanRaw(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() && it != "---" && !it.equals("null", true) }

private fun signalScore(rsrp: Double?): Int = when {
    rsrp == null -> 0
    rsrp >= -85 -> 95
    rsrp >= -95 -> 80
    rsrp >= -105 -> 62
    rsrp >= -115 -> 42
    else -> 22
}

private fun sinrScore(sinr: Double?): Int = when {
    sinr == null -> 0
    sinr >= 20 -> 95
    sinr >= 13 -> 82
    sinr >= 5 -> 65
    sinr >= 0 -> 48
    else -> 25
}

private fun rsrqScore(rsrq: Double?): Int = when {
    rsrq == null -> 0
    rsrq >= -10 -> 92
    rsrq >= -15 -> 74
    rsrq >= -20 -> 50
    else -> 25
}

private fun qualityLabel(score: Int): String = when {
    score >= 90 -> "ممتاز"
    score >= 75 -> "جيد جدًا"
    score >= 58 -> "جيد"
    score >= 40 -> "متوسط"
    score > 0 -> "ضعيف"
    else -> "غير مؤكد"
}

private fun goalLabel(goal: OptimizationGoal): String = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}

private fun yesNo(value: Boolean): String = if (value) "مدعوم" else "غير متاح"
