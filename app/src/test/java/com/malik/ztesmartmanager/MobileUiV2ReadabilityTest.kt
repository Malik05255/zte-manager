package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {

    @Test
    fun productionDashboard_keepsReferenceMicroLabelsReadable() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV5.kt").readText()
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tooTiny = regex.findAll(source)
            .map { it.groupValues[1].toDouble() }
            .filter { it < 8.0 }
            .toList()

        assertTrue("واجهة الجوال تحتوي fontSize أصغر من 8sp: $tooTiny", tooTiny.isEmpty())
    }

    @Test
    fun responsivePolicy_neverShrinksTypographyBelowDesignedSize() {
        val policy = File("src/main/java/com/malik/ztesmartmanager/core/presentation/HaiResponsivePolicy.kt").readText()
        assertTrue(
            "يجب أن يبقى الحد الأدنى لـ textScale مساويًا 1.00",
            policy.contains("coerceIn(1.00f, 1.14f)")
        )
    }

    @Test
    fun primaryTouchTargets_areAtLeastFiftyDp() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV5.kt").readText()
        assertTrue(source.contains("private fun V5ActionButton"))
        assertTrue(source.contains("modifier.height(50.dp)"))
    }

    @Test
    fun homeDashboard_isScrollableAndDoesNotForceEverythingIntoViewport() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV5.kt").readText()
        assertTrue(source.contains("LazyColumn("))
        assertTrue(source.contains("bottom = 100.dp"))
        assertTrue(source.contains("gridHeight ="))
    }

    @Test
    fun productionRoute_usesV5Only() {
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        assertTrue(runtime.contains("ArabicHaiDashboardV5("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV4("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV3("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV2("))
        assertTrue(!runtime.contains("ArabicHaiDashboard("))
    }

    @Test
    fun minimalDashboard_doesNotRestoreExplanatorySectionSubtitles() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV5.kt").readText()
        val banned = listOf(
            "أكثر الأدوات استخدامًا",
            "الحالة النشطة منفصلة عن الترددات المختارة",
            "مخطط راديو فقط",
            "لا يعتبر الوضع مطبقًا إلا بعد",
            "قراءة مباشرة من عدادات الراوتر",
            "يستخدم فقط إعدادات يمكن التحقق منها واستعادتها",
            "راقب جودة الإشارة أثناء تحريك الراوتر"
        )
        banned.forEach { text -> assertTrue("عاد شرح زائد للواجهة: $text", !source.contains(text)) }
    }
}
