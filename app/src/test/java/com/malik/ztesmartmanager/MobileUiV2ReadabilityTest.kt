package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {

    @Test
    fun productionDashboard_hasNoTinyDirectFontSizes() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV3.kt").readText()
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tiny = regex.findAll(source)
            .map { it.groupValues[1].toDouble() }
            .filter { it < 10.0 }
            .toList()

        assertTrue("واجهة الجوال تحتوي fontSize أصغر من 10sp: $tiny", tiny.isEmpty())
    }

    @Test
    fun responsivePolicy_isWidthDrivenAndDoesNotInflateTallPhones() {
        val policy = File("src/main/java/com/malik/ztesmartmanager/core/presentation/HaiResponsivePolicy.kt").readText()
        assertTrue(policy.contains("REFERENCE_WIDTH_DP = 393"))
        assertTrue(policy.contains("widthScale.coerceIn(1.00f, 1.08f)"))
        assertTrue(
            "يجب ألا يدخل heightScale في تكبير واجهة الهاتف الطويل",
            !policy.contains("val heightScale")
        )
    }

    @Test
    fun productionShell_usesOneSizingProvider() {
        val sizing = File("src/main/java/com/malik/ztesmartmanager/HaiUiSizing.kt").readText()
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        val login = File("src/main/java/com/malik/ztesmartmanager/ZteRouterLoginScreen.kt").readText()
        val chrome = File("src/main/java/com/malik/ztesmartmanager/HaiSharedChrome.kt").readText()

        assertTrue(sizing.contains("PHONE_DESIGN_WIDTH_DP = 393f"))
        assertTrue(sizing.contains("LocalDensity provides controlledDensity"))
        assertTrue(runtime.contains("HaiUiScaleProvider"))
        assertTrue(login.contains("HaiUiScaleProvider"))
        assertTrue(chrome.contains("LocalHaiUiMetrics.current"))
    }

    @Test
    fun v3Sections_takePaddingGapAndRadiusFromResponsiveContract() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV3.kt").readText()
        assertTrue(source.contains("padding = spec.horizontalPaddingDp"))
        assertTrue(source.contains("gap = spec.sectionGapDp"))
        assertTrue(source.contains("cardRadius = spec.cardRadiusDp"))
        assertTrue(source.contains("bottomBarHeight = spec.bottomBarHeightDp"))
        assertTrue(source.contains("shape = RoundedCornerShape(layout.cardRadius.dp)"))
        assertTrue(!source.contains("gap = if (height < 760)"))
    }

    @Test
    fun homeRows_useReferenceHeightsInsteadOfStretchingWithPhoneHeight() {
        val source = File("src/main/java/com/malik/ztesmartmanager/TargetHomeDashboard.kt").readText()
        assertTrue(source.contains("verticalScroll(rememberScrollState())"))
        assertTrue(source.contains("height(151.dp)"))
        assertTrue(source.contains("height(56.dp)"))
        assertTrue(source.contains("height(131.dp)"))
        assertTrue(source.contains("height(118.dp)"))
        assertTrue(source.contains("height(92.dp)"))
        assertTrue(!source.contains("weight(151f)"))
        assertTrue(!source.contains("weight(131f)"))
        assertTrue(!source.contains("weight(118f)"))
        assertTrue(!source.contains("weight(92f)"))
    }

    @Test
    fun primaryTouchTargets_areAtLeastFiftyDp() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV3.kt").readText()
        assertTrue(source.contains("private fun M3Primary"))
        assertTrue(source.contains("modifier.height(50.dp)"))
    }

    @Test
    fun productionRoute_usesV3Only() {
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        assertTrue(runtime.contains("ArabicHaiDashboardV3("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV2("))
        assertTrue(!runtime.contains("ArabicHaiDashboard("))
    }

    @Test
    fun minimalDashboard_doesNotRestoreExplanatorySectionSubtitles() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV3.kt").readText()
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
