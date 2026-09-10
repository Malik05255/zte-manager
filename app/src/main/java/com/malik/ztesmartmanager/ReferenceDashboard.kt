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
import androidx.compose.foundation.layout.widthIn
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
private val RefIvory = Color(0xFFF7F3EC)
private val RefIvory2 = Color(0xFFE8E2D7)
private val RefGold = Color(0xFFC99B43)
private val RefGoldDeep = Color(0xFF8A6428)
private val RefInk = Color(0xFF27231F)
private val RefMuted = Color(0xFF766E63)
private val RefLine = Color(0xFFD5CDBF)
private val RefGood = Color(0xFF6F8E50)
private val RefBad = Color(0xFFAF6539)

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
    if (placementMode && placementReading != null) RefPlacementAudio(placementReading)
    var panel by rememberSaveable { mutableStateOf(RefPanel.NONE) }

    BoxWithConstraints(Modifier.fillMaxSize().background(RefBg)) {
        val tiny = maxWidth < 340.dp
        val compact = maxWidth < 390.dp
        val pagePad = if (compact) 10.dp else 14.dp
        val gap = if (compact) 8.dp else 10.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = pagePad, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            item { RefHeader(status, onDisconnect, compact) }

            val data = snapshot
            if (data == null) {
                item { RefCard(Modifier.fillMaxWidth()) { Text("جاري قراءة الراوتر...", Modifier.padding(22.dp), color = RefMuted) } }
            } else {
                item {
                    if (tiny) {
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            RefNetworkCard(data, Modifier.fillMaxWidth(), compact)
                            RefSpeedCard(lastPerformance, speedBusy, onSpeedTest, Modifier.fillMaxWidth(), compact)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            RefNetworkCard(data, Modifier.weight(1.08f), compact)
                            RefSpeedCard(lastPerformance, speedBusy, onSpeedTest, Modifier.weight(0.92f), compact)
                        }
                    }
                }

                item {
                    if (tiny) {
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            RefSmartCard(data, smartMode, smartGoal, smartBusy, smartReport, onSmartModeChange, onSmartGoalChange, onOptimizeNow, Modifier.fillMaxWidth(), compact)
                            RefLocatorCard(data, Modifier.fillMaxWidth(), compact)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.StretchVertically) {
                            RefSmartCard(data, smartMode, smartGoal, smartBusy, smartReport, onSmartModeChange, onSmartGoalChange, onOptimizeNow, Modifier.weight(1.18f), compact)
                            RefLocatorCard(data, Modifier.weight(0.82f), compact)
                        }
                    }
                }

                if (operationMessage.isNotBlank()) item { RefStatusStrip(operationMessage) }

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
                        item { RefBandCard("4G LTE", capabilities.supportedLteBands, selectedLte, "B", controlBusy, compact, onLteToggle, onApplyLte, onAllowAllLte) }
                        if (capabilities.supportsNrBandLock) {
                            item { RefBandCard("5G NR", capabilities.supportedNrBands, selectedNr, "N", controlBusy, compact, onNrToggle, onApplyNr, onAllowAllNr) }
                        }
                    }
                    RefPanel.NETWORK -> item {
                        RefNetworkTools(data, capabilities, controlBusy, onSetNetworkMode, onLockCurrentCell, onAntennaState)
                    }
                    RefPanel.PLACE -> item {
                        RefPlacementCard(placementMode, placementReading, onPlacementToggle)
                    }
                    RefPanel.DIAGNOSTICS -> item { RefDiagnostics(data) }
                    RefPanel.NONE -> Unit
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
}

@Composable
private fun RefHeader(status: String, onDisconnect: () -> Unit, compact: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onDisconnect, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            Text("فصل", color = RefInk, fontSize = if (compact) 12.sp else 13.sp)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("لوحة التحكم الشبكية", color = RefInk, fontSize = if (compact) 21.sp else 24.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(status, color = RefMuted, fontSize = if (compact) 9.sp else 10.sp, maxLines = 1)
        }
        Spacer(Modifier.size(if (compact) 58.dp else 68.dp))
    }
}

@Composable
private fun RefNetworkCard(snapshot: RouterSnapshot, modifier: Modifier, compact: Boolean) {
    val state = refRadio(snapshot)
    val activeNr = snapshot.raw["_zte_nr_active"].equals("true", true)
    val operator = snapshot.raw["network_provider_fullname"].orEmpty().ifBlank { snapshot.raw["network_provider"].orEmpty() }.ifBlank { snapshot.operatorCode ?: "ZTE" }
    val rsrp = if (activeNr) snapshot.nrRsrp ?: snapshot.lteRsrp else snapshot.lteRsrp
    val sinr = if (activeNr) snapshot.nrSinr ?: snapshot.lteSinr else snapshot.lteSinr
    val shape = RoundedCornerShape(topStart = 46.dp, topEnd = 30.dp, bottomEnd = 42.dp, bottomStart = 24.dp)
    Card(modifier.shadow(8.dp, shape), shape = shape, colors = CardDefaults.cardColors(containerColor = RefGoldDeep)) {
        Column(Modifier.padding(if (compact) 12.dp else 15.dp)) {
            Text("Network Stat", color = Color.White.copy(alpha = .78f), fontSize = 8.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.first, color = Color(0xFFFFE39A), fontSize = if (compact) 30.sp else 36.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.size(5.dp))
                Text("⌁", color = Color(0xFFFFD66D), fontSize = 24.sp)
            }
            Text(operator, color = Color.White, fontSize = if (compact) 9.sp else 10.sp, maxLines = 1)
            Text(state.second, color = Color.White.copy(alpha = .76f), fontSize = 8.sp, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RefMetric("RSRP", rsrp, "dBm", Modifier.weight(1f))
                RefMetric("SINR", sinr, "dB", Modifier.weight(1f))
                RefMetric("RSRQ", snapshot.lteRsrq, "dB", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RefMetric(label: String, value: Double?, unit: String, modifier: Modifier) {
    Box(modifier.clip(CircleShape).background(Color.White.copy(alpha = .10f)).border(1.dp, Color(0xFFFFC95B).copy(alpha=.45f), CircleShape).padding(vertical = 6.dp, horizontal = 2.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value?.let(::refNumber) ?: "—", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(unit, color = Color.White.copy(alpha=.68f), fontSize = 6.sp)
            Text(label, color = Color.White.copy(alpha=.78f), fontSize = 6.sp)
        }
    }
}

@Composable
private fun RefSpeedCard(performance: NetworkPerformance?, busy: Boolean, onSpeedTest: () -> Unit, modifier: Modifier, compact: Boolean) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 52.dp, bottomEnd = 26.dp, bottomStart = 46.dp)
    RefCard(modifier, shape) {
        Column(Modifier.padding(if (compact) 12.dp else 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Speed Test", color = RefInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("⚡", fontSize = 20.sp)
                Text(performance?.downloadMbps?.let(::refNumber) ?: "—", color = RefGold, fontSize = if (compact) 30.sp else 38.sp, fontWeight = FontWeight.Black)
                Text(" Mb/s", color = RefGoldDeep, fontSize = 9.sp, modifier = Modifier.padding(bottom = 5.dp))
            }
            performance?.latencyMs?.let { Text("Ping ${refNumber(it)} ms", color = RefMuted, fontSize = 8.sp) }
            Spacer(Modifier.height(7.dp))
            Button(onClick = onSpeedTest, enabled = !busy, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = RefIvory2), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text(if (busy) "جاري القياس" else "قياس السرعة", color = RefInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RefSmartCard(snapshot: RouterSnapshot, enabled: Boolean, goal: OptimizationGoal, busy: Boolean, report: SmartOptimizationReport?, onEnabledChange: (Boolean)->Unit, onGoalChange: (OptimizationGoal)->Unit, onOptimizeNow: ()->Unit, modifier: Modifier, compact: Boolean) {
    val score = remember(snapshot) { NetworkQualityEngine().score(snapshot).total }
    val combo = refCombo(snapshot)
    val shape = RoundedCornerShape(topStart = 54.dp, topEnd = 26.dp, bottomEnd = 52.dp, bottomStart = 34.dp)
    RefCard(modifier, shape) {
        Column(Modifier.padding(if (compact) 12.dp else 15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("حالة السرعة والتوصيل", color = RefInk, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Black)
                    Text(if (enabled) "التحسين الذكي يعمل تلقائيًا" else "المراقبة الذكية متوقفة", color = RefMuted, fontSize = 8.sp)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange, colors = SwitchDefaults.colors(checkedTrackColor = RefGoldDeep, checkedThumbColor = Color.White))
            }
            Spacer(Modifier.height(7.dp))
            Text("الإعداد النشط", color = RefMuted, fontSize = 8.sp)
            Text(combo, color = RefInk, fontSize = if (compact) 19.sp else 23.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(refAggregation(snapshot), color = RefGoldDeep, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RefMiniDial(score, Modifier.size(if (compact) 48.dp else 54.dp))
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("هدف التحسين", color = RefMuted, fontSize = 7.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        OptimizationGoal.entries.forEach { g ->
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (goal == g) RefGold.copy(alpha=.18f) else Color.Transparent).border(1.dp, if (goal == g) RefGold else RefLine, RoundedCornerShape(12.dp)).clickable { onGoalChange(g) }.padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                                Text(refGoal(g), color = RefInk, fontSize = 6.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
            report?.let { Text("الأفضل: ${it.best.bands.sorted().joinToString("+") { b -> "B$b" }} • ${it.best.score}/100", color = RefMuted, fontSize = 7.sp) }
            Spacer(Modifier.height(6.dp))
            Button(onClick = onOptimizeNow, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = RefIvory2), contentPadding = PaddingValues(vertical = 5.dp)) {
                Text(if (busy) "جاري التحسين..." else "تحسين الآن", color = RefInk, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RefLocatorCard(snapshot: RouterSnapshot, modifier: Modifier, compact: Boolean) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 58.dp, bottomEnd = 58.dp, bottomStart = 28.dp)
    RefCard(modifier, shape) {
        Column(Modifier.padding(if (compact) 10.dp else 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Cell Locator", color = RefInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("الخلية الحالية", color = RefMuted, fontSize = 7.sp)
            Box(Modifier.fillMaxWidth().height(if (compact) 112.dp else 126.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(if (compact) 104.dp else 116.dp)) {
                    val c = center
                    drawCircle(RefLine.copy(alpha=.55f), radius = size.minDimension*.48f, style = Stroke(2f))
                    drawCircle(RefLine.copy(alpha=.55f), radius = size.minDimension*.32f, style = Stroke(2f))
                    drawCircle(RefGold.copy(alpha=.20f), radius = size.minDimension*.18f, center = c)
                    drawCircle(RefGoldDeep, radius = 7f, center = c)
                    listOf(Offset(size.width*.25f,size.height*.28f),Offset(size.width*.76f,size.height*.23f),Offset(size.width*.80f,size.height*.74f)).forEach { p ->
                        drawLine(RefGoldDeep, Offset(p.x,p.y+11f), Offset(p.x,p.y-7f), strokeWidth=3f, cap=StrokeCap.Round)
                        drawCircle(RefGold, radius=5f, center=Offset(p.x,p.y-10f))
                    }
                }
                Text("PCI ${snapshot.pci ?: "—"}", color = RefInk, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun RefControlDock(panel: RefPanel, placementMode: Boolean, onBands:()->Unit, onNetwork:()->Unit, onPlacement:()->Unit, onDiagnostics:()->Unit, onOptimizeNow:()->Unit) {
    RefCard(Modifier.fillMaxWidth(), RoundedCornerShape(topStart=34.dp,topEnd=18.dp,bottomEnd=34.dp,bottomStart=18.dp)) {
        Column(Modifier.padding(10.dp)) {
            Text("Advanced Control", color=RefInk, fontSize=9.sp, fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                RefDockButton("قفل النطاقات", panel==RefPanel.BANDS, Modifier.weight(1f), onBands)
                RefDockButton("تحسين الشبكة", false, Modifier.weight(1f), onOptimizeNow)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                RefTool("شبكة", "☷", panel==RefPanel.NETWORK, Modifier.weight(1f), onNetwork)
                RefTool("المكان", "⌖", panel==RefPanel.PLACE || placementMode, Modifier.weight(1f), onPlacement)
                RefTool("تشخيص", "⚙", panel==RefPanel.DIAGNOSTICS, Modifier.weight(1f), onDiagnostics)
            }
        }
    }
}

@Composable private fun RefDockButton(text:String, selected:Boolean, modifier:Modifier,onClick:()->Unit){
    Box(modifier.clip(RoundedCornerShape(50)).background(if(selected) RefGold.copy(alpha=.18f) else RefIvory2).clickable(onClick=onClick).padding(vertical=8.dp),contentAlignment=Alignment.Center){Text(text,color=RefInk,fontSize=10.sp,fontWeight=FontWeight.Bold)}
}

@Composable private fun RefTool(label:String, icon:String, selected:Boolean, modifier:Modifier,onClick:()->Unit){
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(if(selected) RefGold.copy(alpha=.14f) else Color.Transparent).clickable(onClick=onClick).padding(vertical=7.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(icon,color=RefGoldDeep,fontSize=20.sp);Text(label,color=RefInk,fontSize=7.sp)}}
}

@Composable
private fun RefBandCard(title:String,bands:Set<Int>,selected:Set<Int>,prefix:String,busy:Boolean,compact:Boolean,onToggle:(Int)->Unit,onApply:()->Unit,onAll:()->Unit){
    RefCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)){
            Text("اختيار $title",color=RefInk,fontSize=15.sp,fontWeight=FontWeight.Bold)
            Text("اختر ما تريد ثم تطبيق؛ الحالة أعلى الصفحة تبقى قراءة فعلية من الراوتر.",color=RefMuted,fontSize=8.sp)
            Spacer(Modifier.height(8.dp))
            val columns=if(compact)4 else 5
            bands.sorted().chunked(columns).forEach{row->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    row.forEach{b->FilterChip(selected=b in selected,onClick={onToggle(b)},label={Text("$prefix$b",fontSize=8.sp,fontWeight=FontWeight.Bold)},modifier=Modifier.weight(1f),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=RefGold.copy(alpha=.22f)))}
                    repeat(columns-row.size){Spacer(Modifier.weight(1f))}
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Button(onClick=onApply,enabled=selected.isNotEmpty()&&!busy,modifier=Modifier.weight(1f),shape=RoundedCornerShape(50),colors=ButtonDefaults.buttonColors(containerColor=RefGoldDeep)){Text("تطبيق والتحقق",fontSize=9.sp)}
                OutlinedButton(onClick=onAll,enabled=!busy,modifier=Modifier.weight(1f),shape=RoundedCornerShape(50)){Text("تلقائي",color=RefInk,fontSize=9.sp)}
            }
        }
    }
}

@Composable
private fun RefNetworkTools(snapshot:RouterSnapshot,capabilities:RouterCapabilities,busy:Boolean,onMode:(String)->Unit,onLock:()->Unit,onAntenna:(Int)->Unit){
    RefCard(Modifier.fillMaxWidth()){
        Column(Modifier.padding(14.dp)){
            Text("التحكم بالشبكة",color=RefInk,fontSize=15.sp,fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                RefOutlined("4G فقط",!busy,Modifier.weight(1f)){onMode("Only_LTE")}
                RefOutlined("4G + 5G",!busy,Modifier.weight(1f)){onMode("LTE_AND_5G")}
                RefOutlined("5G فقط",!busy,Modifier.weight(1f)){onMode("Only_5G")}
            }
            if(capabilities.supportsCellLock && snapshot.pci!=null && snapshot.earfcn!=null){Spacer(Modifier.height(6.dp));RefOutlined("تثبيت الخلية • PCI ${snapshot.pci} / EARFCN ${snapshot.earfcn}",!busy,Modifier.fillMaxWidth(),onLock)}
            if(capabilities.supportsAntennaControl){Spacer(Modifier.height(8.dp));Text("هوائي MC801A",color=RefMuted,fontSize=8.sp);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){(1..3).forEach{s->RefOutlined("اتجاه $s",!busy,Modifier.weight(1f)){onAntenna(s)}}}}
        }
    }
}

@Composable private fun RefOutlined(text:String,enabled:Boolean,modifier:Modifier,onClick:()->Unit){OutlinedButton(onClick=onClick,enabled=enabled,modifier=modifier,shape=RoundedCornerShape(50),contentPadding=PaddingValues(horizontal=4.dp,vertical=6.dp)){Text(text,color=RefInk,fontSize=8.sp,textAlign=TextAlign.Center)}}

@Composable
private fun RefPlacementCard(enabled:Boolean,reading:PlacementReading?,onToggle:()->Unit){
    RefCard(Modifier.fillMaxWidth()){
        Column(Modifier.padding(14.dp)){
            Text("مساعد أفضل مكان",color=RefInk,fontSize=15.sp,fontWeight=FontWeight.Bold)
            Text("حرّك الراوتر سنتيمترات قليلة؛ النغمة تتسارع كلما اقتربت من أفضل نقطة.",color=RefMuted,fontSize=9.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                RefMiniDial(reading?.score?.total?:0,Modifier.size(62.dp));Spacer(Modifier.size(10.dp));Column(Modifier.weight(1f)){Text(if(enabled&&reading!=null) refGuidance(reading.guidance) else "جاهز للبحث",color=if(reading?.deltaFromBest?:0 < -5) RefBad else RefGood,fontWeight=FontWeight.Bold,fontSize=11.sp);Text("أفضل نقطة: ${reading?.bestScore ?: 0}/100",color=RefMuted,fontSize=8.sp)}
            }
            Spacer(Modifier.height(8.dp));Button(onClick=onToggle,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(50),colors=ButtonDefaults.buttonColors(containerColor=RefGoldDeep)){Text(if(enabled)"إيقاف مساعد المكان" else "ابدأ البحث عن أفضل مكان")}
        }
    }
}

@Composable
private fun RefDiagnostics(snapshot:RouterSnapshot){
    val keys=listOf("_zte_radio_mode","_zte_raw_network_type","_zte_nr_active","wan_lte_ca","lte_ca_pcell_band","lte_multi_ca_scell_info","nr5g_action_band","nr5g_action_nsa_band","ZCELLINFO_band","Z5g_rsrp","Z5g_SINR","nr5g_pci","Z5g_dlEarfcn")
    RefCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)){Text("تشخيص القراءة",color=RefInk,fontSize=15.sp,fontWeight=FontWeight.Bold);Text("حقول راديو فقط؛ لا تظهر كلمة المرور.",color=RefMuted,fontSize=8.sp);Spacer(Modifier.height(7.dp));keys.forEach{k->snapshot.raw[k]?.takeIf{it.isNotBlank()}?.let{v->Row(Modifier.fillMaxWidth().padding(vertical=2.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(k,color=RefMuted,fontSize=7.sp,modifier=Modifier.weight(1f));Text(v,color=RefInk,fontSize=7.sp,fontWeight=FontWeight.Medium,textAlign=TextAlign.End,modifier=Modifier.weight(1f))}}}} }
}

@Composable private fun RefStatusStrip(message:String){Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(RefGold.copy(alpha=.12f)).padding(horizontal=13.dp,vertical=7.dp)){Text(message,color=RefInk,fontSize=8.sp)}}

@Composable private fun RefCard(modifier:Modifier=Modifier,shape:RoundedCornerShape=RoundedCornerShape(28.dp),content:@Composable()->Unit){Card(modifier=modifier.shadow(7.dp,shape,ambientColor=Color.Black.copy(alpha=.08f),spotColor=Color.Black.copy(alpha=.08f)),shape=shape,colors=CardDefaults.cardColors(containerColor=RefIvory),elevation=CardDefaults.cardElevation(defaultElevation=0.dp)){content()}}

@Composable private fun RefMiniDial(score:Int,modifier:Modifier){Box(modifier,contentAlignment=Alignment.Center){Canvas(Modifier.fillMaxSize()){drawCircle(RefLine,style=Stroke(width=6f));drawArc(RefGold,startAngle=-90f,sweepAngle=score.coerceIn(0,100)*3.6f,useCenter=false,style=Stroke(width=7f,cap=StrokeCap.Round))};Column(horizontalAlignment=Alignment.CenterHorizontally){Text(score.toString(),color=RefInk,fontSize=15.sp,fontWeight=FontWeight.Black);Text("/100",color=RefMuted,fontSize=6.sp)}}}

private fun refRadio(s:RouterSnapshot):Pair<String,String>{val mode=s.raw["_zte_radio_mode"].orEmpty();val nr=s.raw["_zte_nr_active"].equals("true",true);val ca=s.caActive;return when{mode=="NSA_ACTIVE"||nr&&premiumCurrentLteBands(s).isNotEmpty()->"5G" to "NSA • NR نشط";mode=="SA_ACTIVE"||nr->"5G" to "SA • NR نشط";mode=="NSA_STANDBY"->(if(ca)"4G+" else "4G") to "5G NSA جاهز";ca->"4G+" to "LTE-A • دمج نشط";else->"4G" to "LTE"}}
private fun refCombo(s:RouterSnapshot):String=(premiumCurrentNrBands(s).sorted().map{"N$it"}+premiumCurrentLteBands(s).sorted().map{"B$it"}).joinToString("+").ifBlank{"جاري القراءة"}
private fun refAggregation(s:RouterSnapshot):String{val l=premiumCurrentLteBands(s);val n=premiumCurrentNrBands(s);return when{n.isNotEmpty()&&l.isNotEmpty()->"5G + ${l.size} LTE • اتصال فعلي";s.caActive&&l.size>1->"دمج 4G فعلي • ${l.size}CA";n.isNotEmpty()->"5G NR نشط";else->"بدون دمج نشط"}}
private fun refNumber(v:Double):String=if(v%1.0==0.0)v.toInt().toString() else "%.1f".format(v)
private fun refGoal(g:OptimizationGoal)=when(g){OptimizationGoal.BALANCED->"متوازن";OptimizationGoal.SPEED->"سرعة";OptimizationGoal.GAMING->"ألعاب";OptimizationGoal.STABILITY->"ثبات"}
private fun refGuidance(g:PlacementGuidance)=when(g){PlacementGuidance.INITIAL->"حرّك الراوتر ببطء";PlacementGuidance.MUCH_BETTER->"تحسن واضح — استمر";PlacementGuidance.BETTER->"أفضل — استمر قليلًا";PlacementGuidance.STABLE->"ثابت — جرّب حركة صغيرة";PlacementGuidance.WORSE->"أسوأ — ارجع قليلًا";PlacementGuidance.RETURN_TO_BEST->"ارجع إلى أفضل نقطة";PlacementGuidance.CELL_CHANGED_WORSE->"انتقلت لخلية أضعف";PlacementGuidance.EXCELLENT_HOLD->"ممتاز — ثبّت هنا";PlacementGuidance.BEST_SO_FAR->"أفضل نقطة حتى الآن"}

@Composable private fun RefPlacementAudio(reading:PlacementReading){val haptic=LocalHapticFeedback.current;val tone=remember{ToneGenerator(AudioManager.STREAM_MUSIC,45)};DisposableEffect(Unit){onDispose{tone.release()}};LaunchedEffect(reading.guidance){when(reading.guidance){PlacementGuidance.BEST_SO_FAR,PlacementGuidance.EXCELLENT_HOLD->{tone.startTone(ToneGenerator.TONE_PROP_ACK,90);haptic.performHapticFeedback(HapticFeedbackType.LongPress)};PlacementGuidance.WORSE,PlacementGuidance.RETURN_TO_BEST,PlacementGuidance.CELL_CHANGED_WORSE->tone.startTone(ToneGenerator.TONE_PROP_NACK,80);else->Unit}};LaunchedEffect(reading.score.total){while(true){tone.startTone(ToneGenerator.TONE_PROP_BEEP,35);delay((1250-reading.score.total*10L).coerceIn(200L,950L))}}}
