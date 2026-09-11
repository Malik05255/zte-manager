package com.malik.ztesmartmanager

import androidx.compose.ui.graphics.Color
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.PlacementGuidance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import com.malik.ztesmartmanager.core.tower.NearbyCell

internal fun glassOperator(snapshot: RouterSnapshot): String =
    snapshot.raw["network_provider_fullname"]?.takeIf { it.isNotBlank() }
        ?: snapshot.raw["network_provider"]?.takeIf { it.isNotBlank() }
        ?: snapshot.operatorCode?.takeIf { it.isNotBlank() }
        ?: "شبكتك"

internal fun glassIsFiveG(snapshot: RouterSnapshot): Boolean =
    snapshot.nrRsrp != null || snapshot.nrSinr != null ||
        snapshot.networkType.orEmpty().contains("5G", true) ||
        snapshot.networkType.orEmpty().contains("NR", true)

internal fun glassQualityWord(score: Int): String = when {
    score >= 92 -> "ممتاز جدًا"
    score >= 82 -> "ممتاز"
    score >= 70 -> "جيد جدًا"
    score >= 58 -> "جيد"
    score >= 43 -> "مقبول"
    score > 0 -> "ضعيف"
    else -> "غير مؤكد"
}

internal fun glassQualityAdvice(score: Int): String = when {
    score >= 92 -> "اتصال قوي جدًا؛ لا حاجة لتغيير مكان الراوتر"
    score >= 82 -> "المكان ممتاز، والتحسين المتبقي بسيط"
    score >= 70 -> "الوضع جيد جدًا؛ قد يفيد تعديل بسيط للمكان"
    score >= 58 -> "الاتصال جيد، لكن يمكن تحسين المكان أكثر"
    score >= 43 -> "جرّب مكانًا أعلى أو أقرب للنافذة"
    score > 0 -> "الإشارة ضعيفة؛ غيّر مكان الراوتر"
    else -> "نحتاج قراءة كافية قبل الحكم"
}

internal fun glassComponentWord(score: Int): String = when {
    score >= 85 -> "ممتاز"
    score >= 70 -> "جيد جدًا"
    score >= 55 -> "جيد"
    score >= 40 -> "مقبول"
    score > 0 -> "ضعيف"
    else -> "غير معروف"
}

internal fun glassQualityColor(score: Int): Color = when {
    score >= 82 -> GlassMint
    score >= 58 -> GlassBlue
    score >= 43 -> GlassAmber
    score > 0 -> GlassRed
    else -> GlassMuted
}

internal fun glassPlacementWords(reading: PlacementReading?): Triple<String, String, Color> {
    if (reading == null) return Triple("ابدأ الجولة", "حرّك الراوتر وسنخبرك أين يكون الاتصال أفضل.", GlassBlue)
    return when (reading.guidance) {
        PlacementGuidance.EXCELLENT_HOLD -> Triple("ممتاز — ثبّت الراوتر هنا", "هذا المكان من أفضل ما قسناه.", GlassMint)
        PlacementGuidance.BEST_SO_FAR -> Triple("أفضل مكان حتى الآن", "هذا أفضل موقع مررت به في الجولة.", GlassMint)
        PlacementGuidance.MUCH_BETTER -> Triple("تحسن كبير", "استمر خطوة صغيرة في نفس الاتجاه.", GlassMint)
        PlacementGuidance.BETTER -> Triple("أصبح أفضل", "أنت تتجه للمكان الصحيح.", GlassBlue)
        PlacementGuidance.RETURN_TO_BEST -> Triple("ارجع للمكان السابق", "ابتعدت عن أفضل نقطة سجلناها.", GlassAmber)
        PlacementGuidance.CELL_CHANGED_WORSE -> Triple("الاتصال تغيّر للأسوأ", "انتقل الراوتر لخلية أضعف.", GlassRed)
        PlacementGuidance.WORSE -> Triple("النتيجة تراجعت", "ارجع خطوة للخلف.", GlassAmber)
        PlacementGuidance.STABLE -> Triple(
            if (reading.score.total >= 82) "ممتاز — بقيت خطوة بسيطة" else "المكان ثابت",
            if (reading.score.total >= 82) "يمكنك تثبيت الراوتر هنا." else "حرّكه قليلًا وقارن.",
            if (reading.score.total >= 82) GlassMint else GlassBlue
        )
        PlacementGuidance.INITIAL -> Triple(glassQualityWord(reading.score.total), "هذه أول قراءة؛ حرّك الراوتر قليلًا للمقارنة.", glassQualityColor(reading.score.total))
    }
}

internal fun glassSpeedWord(speed: Double?): String = when {
    speed == null -> "غير مقاس"
    speed >= 300 -> "سرعة ممتازة جدًا"
    speed >= 150 -> "سرعة ممتازة"
    speed >= 75 -> "سرعة جيدة جدًا"
    speed >= 30 -> "سرعة جيدة"
    speed >= 10 -> "سرعة مقبولة"
    else -> "سرعة ضعيفة"
}

internal fun glassLatencyWord(latency: Double): String = when {
    latency <= 20 -> "استجابة ممتازة"
    latency <= 40 -> "استجابة جيدة جدًا"
    latency <= 70 -> "استجابة جيدة"
    latency <= 120 -> "استجابة مقبولة"
    else -> "الاستجابة بطيئة"
}

internal fun glassModeWords(snapshot: RouterSnapshot): String = when {
    glassIsFiveG(snapshot) -> "أنت متصل بالجيل الخامس الآن"
    snapshot.networkType.orEmpty().contains("LTE", true) || snapshot.networkType.orEmpty().contains("4G", true) -> "أنت متصل بالجيل الرابع الآن"
    snapshot.networkType.isNullOrBlank() -> "نوع الشبكة غير ظاهر من الراوتر"
    else -> "الوضع الحالي: ${snapshot.networkType}"
}

internal fun glassDuration(seconds: Long?): String {
    if (seconds == null) return "غير متاحة"
    val days = seconds / 86_400
    val hours = (seconds % 86_400) / 3_600
    val minutes = (seconds % 3_600) / 60
    return when {
        days > 0 -> "$days يوم ${hours} س"
        hours > 0 -> "$hours س ${minutes} د"
        else -> "$minutes دقيقة"
    }
}

internal fun glassNearbyTitle(cell: NearbyCell): String = when {
    (cell.evidenceScore ?: 0) >= 80 -> "خلية قريبة قوية جدًا"
    (cell.evidenceScore ?: 0) >= 65 -> "خلية قريبة جيدة"
    (cell.evidenceScore ?: 0) >= 45 -> "خلية قريبة مقبولة"
    cell.evidenceScore != null -> "خلية قريبة ضعيفة"
    else -> "خلية قريبة"
}
