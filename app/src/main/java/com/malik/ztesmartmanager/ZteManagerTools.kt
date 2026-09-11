package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetry
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.smart.SmartOptimizationReport

@Composable
internal fun ZteManagerTools(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    runtime: RuntimeCapabilityReport?,
    thermal: ThermalTelemetry?,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    smartMode: Boolean,
    smartGoal: OptimizationGoal,
    smartBusy: Boolean,
    smartReport: SmartOptimizationReport?,
    safetyBackupAvailable: Boolean,
    controlBusy: Boolean,
    onPlacementToggle: () -> Unit,
    onSmartModeChange: (Boolean) -> Unit,
    onSmartGoalChange: (OptimizationGoal) -> Unit,
    onOptimizeNow: () -> Unit,
    onAntennaState: (Int) -> Unit,
    onRestoreSafetyBackup: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    onShareDiagnostics: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ZtePlacementToolCard(placementMode, placementReading, onPlacementToggle) }
        item {
            ZteSmartOptimizationCard(
                smartMode, smartGoal, smartBusy, smartReport,
                onSmartModeChange, onSmartGoalChange, onOptimizeNow
            )
        }
        item { ZteCapabilityCard(capabilities, runtime) }
        if (thermal?.hasAnyEvidence == true) item { ZteThermalCard(thermal) }
        if (capabilities.supportsAntennaControl) item { ZteAntennaCard(controlBusy, onAntennaState) }
        item { ZteSafetyCard(safetyBackupAvailable, controlBusy, onRestoreSafetyBackup) }
        item { ZteDiagnosticsCard(onCopyDiagnostics, onShareDiagnostics) }
        item { ZteCurrentSessionCard(snapshot) }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun ZtePlacementToolCard(active: Boolean, reading: PlacementReading?, onToggle: () -> Unit) {
    val (title, body, color) = ztePlacementWords(reading)
    ZteCard {
        ZteSectionHeader("مساعد مكان الراوتر", "تحريك مباشر مع تقييم مستمر للإشارة")
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.10f)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = color) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Text(reading?.score?.total?.toString() ?: "—", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(body, color = ZteMuted, fontSize = 14.sp, lineHeight = 19.sp)
                    if (reading != null) {
                        Text("الثقة ${reading.confidence}% • أفضل نتيجة ${reading.bestScore}%", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ZtePrimaryButton(if (active) "إيقاف المساعد" else "ابدأ جولة تحسين المكان", true, onToggle)
    }
}

@Composable
private fun ZteSmartOptimizationCard(
    enabled: Boolean,
    goal: OptimizationGoal,
    busy: Boolean,
    report: SmartOptimizationReport?,
    onEnabled: (Boolean) -> Unit,
    onGoal: (OptimizationGoal) -> Unit,
    onRun: () -> Unit
) {
    ZteCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("التحسين الذكي", color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("يختبر إعدادات مدعومة ويقارنها ثم يعيد الإعداد الآمن عند الفشل", color = ZteMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Switch(checked = enabled, onCheckedChange = onEnabled, enabled = !busy)
        }
        Spacer(Modifier.height(14.dp))
        Text("هدف التحسين", color = ZteInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ZteChoiceChip("متوازن", goal == OptimizationGoal.BALANCED, !busy, Modifier.weight(1f)) { onGoal(OptimizationGoal.BALANCED) }
            ZteChoiceChip("سرعة", goal == OptimizationGoal.SPEED, !busy, Modifier.weight(1f)) { onGoal(OptimizationGoal.SPEED) }
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ZteChoiceChip("ألعاب", goal == OptimizationGoal.GAMING, !busy, Modifier.weight(1f)) { onGoal(OptimizationGoal.GAMING) }
            ZteChoiceChip("ثبات", goal == OptimizationGoal.STABILITY, !busy, Modifier.weight(1f)) { onGoal(OptimizationGoal.STABILITY) }
        }
        Spacer(Modifier.height(12.dp))
        ZtePrimaryButton(if (busy) "جاري التقييم…" else "شغّل التحسين الآن", !busy, onRun)
        if (report != null) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(17.dp), color = ZteSoftBlue) {
                Column(Modifier.padding(13.dp)) {
                    Text(if (report.changed) "تم اختيار إعداد أفضل" else "لم نغيّر الإعداد الحالي", color = ZteInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(report.message, color = ZteMuted, fontSize = 13.sp, lineHeight = 18.sp)
                    Text("الأساس ${report.baseline.score}% • الأفضل ${report.best.score}%", color = ZteBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ZteCapabilityCard(capabilities: RouterCapabilities, runtime: RuntimeCapabilityReport?) {
    ZteCard {
        ZteSectionHeader("قدرات الراوتر", "الوظائف التي ثبتت لهذا الموديل والـFirmware")
        Spacer(Modifier.height(12.dp))
        val rows = listOf(
            "قفل 4G" to (runtime?.lteBandControl?.canAttemptWrite ?: capabilities.supportsLteBandLock),
            "قفل 5G" to (runtime?.nrBandControl?.canAttemptWrite ?: capabilities.supportsNrBandLock),
            "Cell Lock" to (runtime?.cellLock?.canAttemptWrite ?: capabilities.supportsCellLock),
            "وضع الشبكة" to (runtime?.networkMode?.canAttemptWrite == true),
            "مسح الخلايا" to (runtime?.neighborScan?.hasRuntimeEvidence == true),
            "الهوائي" to (runtime?.antennaControl?.canAttemptWrite ?: capabilities.supportsAntennaControl)
        )
        rows.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                pair.forEach { (title, available) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(17.dp),
                        color = if (available) ZteSoftGreen else Color(0xFFF5F7FA),
                        border = BorderStroke(1.dp, ZteLine)
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(title, color = ZteInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text(if (available) "متاح" else "غير مؤكد", color = if (available) ZteGreen else ZteMuted, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
        }
    }
}

@Composable
private fun ZteThermalCard(thermal: ThermalTelemetry) {
    ZteCard {
        ZteSectionHeader("حرارة الراوتر", "القيم المباشرة التي أرسلها الراوتر فقط")
        Spacer(Modifier.height(12.dp))
        thermal.readings.forEachIndexed { index, reading ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(reading.label, color = ZteMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${reading.celsius}°C", color = ZteInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            if (index != thermal.readings.lastIndex) HorizontalDivider(color = ZteLine)
        }
    }
}

@Composable
private fun ZteAntennaCard(controlBusy: Boolean, onAntennaState: (Int) -> Unit) {
    ZteCard {
        ZteSectionHeader("الهوائي", "لن يتاح التحكم إلا عند وجود دليل Runtime يسمح بالمحاولة")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ZteChoiceChip("تلقائي", false, !controlBusy, Modifier.weight(1f)) { onAntennaState(1) }
            ZteChoiceChip("داخلي", false, !controlBusy, Modifier.weight(1f)) { onAntennaState(2) }
            ZteChoiceChip("خارجي", false, !controlBusy, Modifier.weight(1f)) { onAntennaState(3) }
        }
    }
}

@Composable
private fun ZteSafetyCard(backupAvailable: Boolean, controlBusy: Boolean, onRestore: () -> Unit) {
    ZteCard {
        ZteSectionHeader("النسخة الآمنة", "استعادة آخر إعدادات حُفظت قبل عملية تحكم")
        Spacer(Modifier.height(12.dp))
        ZtePrimaryButton("استعادة النسخة الآمنة", backupAvailable && !controlBusy, onRestore)
        if (!backupAvailable) {
            Spacer(Modifier.height(8.dp))
            Text("لا توجد نسخة آمنة قابلة للاستعادة حتى الآن.", color = ZteMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ZteDiagnosticsCard(onCopy: () -> Unit, onShare: () -> Unit) {
    ZteCard {
        ZteSectionHeader("تقرير التشخيص", "تقرير تقني لتتبع مشاكل الاتصال")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { ZteSecondaryButton("نسخ التقرير", true, onCopy) }
            Box(Modifier.weight(1f)) { ZtePrimaryButton("مشاركة التقرير", true, onShare) }
        }
    }
}

@Composable
private fun ZteCurrentSessionCard(snapshot: RouterSnapshot) {
    ZteCard {
        ZteSectionHeader("الجلسة الحالية", "هوية الراوتر والاتصال الحالي")
        Spacer(Modifier.height(10.dp))
        ZteToolLine("الموديل", snapshot.model ?: "—")
        ZteToolLine("Firmware", snapshot.firmware ?: "—")
        ZteToolLine("الشبكة", zteOperator(snapshot))
        ZteToolLine("الوضع", zteNetworkLabel(snapshot))
    }
}

@Composable
private fun ZteToolLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ZteMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}
