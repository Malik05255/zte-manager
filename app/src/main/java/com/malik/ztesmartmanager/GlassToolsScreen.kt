package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetry
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport
import kotlin.math.roundToInt

@Composable
internal fun GlassToolsScreen(
    capabilities: RouterCapabilities,
    runtime: RuntimeCapabilityReport?,
    thermal: ThermalTelemetry?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    safetyBackupAvailable: Boolean,
    controlBusy: Boolean,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onRestoreSafetyBackup: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onShareDiagnostics: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard {
                Column(Modifier.padding(14.dp)) {
                    Text("التحسين الذكي", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text("لا يغيّر شيئًا قبل أخذ نسخة آمنة، ويتحقق بعد كل خطوة", color = GlassMuted, fontSize = 9.sp)
                    Spacer(Modifier.height(9.dp))
                    Row {
                        Text(if (smartMode) "مفعّل" else "متوقف", color = GlassInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(checked = smartMode, onCheckedChange = onSmartModeChange)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("هدف التحسين", color = GlassMuted, fontSize = 9.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassGoalChip("متوازن", OptimizationGoal.BALANCED, smartGoal, Modifier.weight(1f), onSmartGoalChange)
                        GlassGoalChip("سرعة", OptimizationGoal.SPEED, smartGoal, Modifier.weight(1f), onSmartGoalChange)
                        GlassGoalChip("ألعاب", OptimizationGoal.GAMING, smartGoal, Modifier.weight(1f), onSmartGoalChange)
                        GlassGoalChip("ثبات", OptimizationGoal.STABILITY, smartGoal, Modifier.weight(1f), onSmartGoalChange)
                    }
                    Spacer(Modifier.height(10.dp))
                    GlassActionButton(if (smartBusy) "جاري التحسين…" else "حسّن الشبكة الآن", !smartBusy, onOptimizeNow)
                    smartReport?.message?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = GlassMuted, fontSize = 9.sp, lineHeight = 13.sp)
                    }
                }
            }
        }

        if (capabilities.supportsAntennaControl) {
            item {
                GlassCard {
                    Column(Modifier.padding(14.dp)) {
                        Text("وضع الهوائي", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text("لا يظهر هذا التحكم إلا إذا أثبت الراوتر دعمه وقت التشغيل", color = GlassMuted, fontSize = 9.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onAntennaState(0) }, enabled = !controlBusy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("تلقائي", fontSize = 9.sp) }
                            OutlinedButton(onClick = { onAntennaState(1) }, enabled = !controlBusy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("داخلي", fontSize = 9.sp) }
                            OutlinedButton(onClick = { onAntennaState(2) }, enabled = !controlBusy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("خارجي", fontSize = 9.sp) }
                        }
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(Modifier.padding(14.dp)) {
                    Text("حالة الراوتر", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(
                        runtime?.let { "تم فحص وظائف الراوتر الفعلية وقت التشغيل" } ?: "لم تكتمل قراءة الوظائف وقت التشغيل",
                        color = GlassMuted,
                        fontSize = 9.sp
                    )
                    thermal?.highestObserved?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("أعلى حرارة مقروءة: ${it.celsius.roundToInt()}°C", color = GlassInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(Modifier.padding(14.dp)) {
                    Text("النسخة الآمنة والتشخيص", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (safetyBackupAvailable) "توجد نسخة يمكن الرجوع إليها إذا سبّب تغيير مشكلة" else "لا توجد نسخة استعادة جاهزة الآن",
                        color = GlassMuted,
                        fontSize = 9.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRestoreSafetyBackup,
                        enabled = safetyBackupAvailable && !controlBusy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("استعادة الإعدادات الآمنة", fontSize = 10.sp) }
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCopyDiagnostics, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("نسخ التقرير", fontSize = 9.sp) }
                        OutlinedButton(onClick = onShareDiagnostics, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("مشاركة التقرير", fontSize = 9.sp) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun GlassGoalChip(
    label: String,
    value: OptimizationGoal,
    selected: OptimizationGoal,
    modifier: Modifier,
    onSelect: (OptimizationGoal) -> Unit
) {
    val active = value == selected
    Surface(
        modifier = modifier.clickable { onSelect(value) },
        shape = RoundedCornerShape(12.dp),
        color = if (active) GlassBlue else androidx.compose.ui.graphics.Color(0xFFF3F6FA),
        border = BorderStroke(1.dp, if (active) GlassBlue else GlassLine)
    ) {
        Box(Modifier.padding(vertical = 9.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (active) androidx.compose.ui.graphics.Color.White else GlassInk)
        }
    }
}
