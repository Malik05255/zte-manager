package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        ZteSectionHeader("مساعد مكان الراوتر", "نصيحة مباشرة بدل أرقام تحتاج تفسير")
        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(22.dp), color = color.copy(alpha = 0.10f)) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Text(title, color = color, fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text(body, color = ZteInk, fontSize = 15.sp, lineHeight = 22.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
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
        ZteSectionHeader("التحسين الذكي", "يجرّب فقط الإعدادات التي يسمح بها الراوتر ويرجع للخلف عند الفشل")
        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text(if (enabled) "التحسين التلقائي مفعّل" else "التحسين التلقائي متوقف", color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Switch(checked = enabled, onCheckedChange = onEnabled, enabled = !busy)
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("اختر الهدف", color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        ZteChoiceChip("متوازن", goal == OptimizationGoal.BALANCED, !busy) { onGoal(OptimizationGoal.BALANCED) }
        Spacer(Modifier.height(9.dp))
        ZteChoiceChip("أفضل سرعة", goal == OptimizationGoal.SPEED, !busy) { onGoal(OptimizationGoal.SPEED) }
        Spacer(Modifier.height(9.dp))
        ZteChoiceChip("ألعاب واستجابة", goal == OptimizationGoal.GAMING, !busy) { onGoal(OptimizationGoal.GAMING) }
        Spacer(Modifier.height(9.dp))
        ZteChoiceChip("أعلى ثبات", goal == OptimizationGoal.STABILITY, !busy) { onGoal(OptimizationGoal.STABILITY) }

        Spacer(Modifier.height(14.dp))
        ZtePrimaryButton(if (busy) "جاري التقييم…" else "شغّل التحسين الآن", !busy, onRun)
        if (report != null) {
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = ZteSoftBlue) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Text(if (report.changed) "وجدنا إعدادًا أفضل" else "الإعداد الحالي هو الأفضل في الاختبار", color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    Text(report.message, color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun ZteCapabilityCard(capabilities: RouterCapabilities, runtime: RuntimeCapabilityReport?) {
    ZteCard {
        ZteSectionHeader("ما الذي يدعمه راوترُك؟", "بدل مصطلحات كثيرة: متاح أو غير مؤكد")
        Spacer(Modifier.height(14.dp))
        val rows = listOf(
            "قفل ترددات 4G" to (runtime?.lteBandControl?.canAttemptWrite ?: capabilities.supportsLteBandLock),
            "قفل ترددات 5G" to (runtime?.nrBandControl?.canAttemptWrite ?: capabilities.supportsNrBandLock),
            "تثبيت الخلية" to (runtime?.cellLock?.canAttemptWrite ?: capabilities.supportsCellLock),
            "تغيير وضع الشبكة" to (runtime?.networkMode?.canAttemptWrite == true),
            "البحث عن الخلايا" to (runtime?.neighborScan?.hasRuntimeEvidence == true),
            "التحكم بالهوائي" to (runtime?.antennaControl?.canAttemptWrite ?: capabilities.supportsAntennaControl)
        )
        rows.forEachIndexed { index, (title, available) ->
            ZteCapabilityRow(title, available)
            if (index != rows.lastIndex) Spacer(Modifier.height(9.dp))
        }
    }
}

@Composable
private fun ZteCapabilityRow(title: String, available: Boolean) {
    Surface(shape = RoundedCornerShape(19.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(title, color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(if (available) "متاح على هذا الراوتر" else "غير مؤكد لهذا الراوتر", color = if (available) ZteGreen else ZteMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ZteThermalCard(thermal: ThermalTelemetry) {
    ZteCard {
        ZteSectionHeader("حرارة الراوتر", "نعرض القيم التي أرسلها الراوتر فقط")
        Spacer(Modifier.height(14.dp))
        thermal.readings.forEachIndexed { index, reading ->
            Surface(shape = RoundedCornerShape(18.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(reading.label, color = ZteMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("${reading.celsius}°C", color = ZteInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            if (index != thermal.readings.lastIndex) Spacer(Modifier.height(9.dp))
        }
    }
}

@Composable
private fun ZteAntennaCard(controlBusy: Boolean, onAntennaState: (Int) -> Unit) {
    ZteCard {
        ZteSectionHeader("اختيار الهوائي", "كل اختيار في سطر مستقل")
        Spacer(Modifier.height(14.dp))
        ZteSecondaryButton("تلقائي", !controlBusy) { onAntennaState(1) }
        Spacer(Modifier.height(9.dp))
        ZteSecondaryButton("الهوائي الداخلي", !controlBusy) { onAntennaState(2) }
        Spacer(Modifier.height(9.dp))
        ZteSecondaryButton("الهوائي الخارجي", !controlBusy) { onAntennaState(3) }
    }
}

@Composable
private fun ZteSafetyCard(backupAvailable: Boolean, controlBusy: Boolean, onRestore: () -> Unit) {
    ZteCard {
        ZteSectionHeader("النسخة الآمنة", "ترجع لآخر إعدادات محفوظة قبل عملية تحكم")
        Spacer(Modifier.height(14.dp))
        ZtePrimaryButton("استعادة النسخة الآمنة", backupAvailable && !controlBusy, onRestore)
        if (!backupAvailable) {
            Spacer(Modifier.height(9.dp))
            Text("لا توجد نسخة قابلة للاستعادة حتى الآن.", color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun ZteDiagnosticsCard(onCopy: () -> Unit, onShare: () -> Unit) {
    ZteCard {
        ZteSectionHeader("تقرير التشخيص", "للمشاركة عند تتبع مشكلة في الاتصال")
        Spacer(Modifier.height(14.dp))
        ZteSecondaryButton("نسخ التقرير", true, onCopy)
        Spacer(Modifier.height(9.dp))
        ZtePrimaryButton("مشاركة التقرير", true, onShare)
    }
}

@Composable
private fun ZteCurrentSessionCard(snapshot: RouterSnapshot) {
    ZteCard {
        ZteSectionHeader("الجلسة الحالية")
        Spacer(Modifier.height(12.dp))
        ZteSessionLine("الموديل", snapshot.model ?: "غير معروف")
        Spacer(Modifier.height(9.dp))
        ZteSessionLine("الشبكة", zteOperator(snapshot))
        Spacer(Modifier.height(9.dp))
        ZteSessionLine("الاتصال", zteNetworkLabel(snapshot))
    }
}

@Composable
private fun ZteSessionLine(label: String, value: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = ZteSurfaceAlt, border = BorderStroke(1.dp, ZteLine)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, color = ZteMuted, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, color = ZteInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
