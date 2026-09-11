package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {

    @Test
    fun productionDashboard_keepsMicroLabelsReadable() {
        val sources = listOf(
            File("src/main/java/com/malik/ztesmartmanager/MasterpieceDashboard.kt"),
            File("src/main/java/com/malik/ztesmartmanager/MasterpieceWidgets.kt"),
            File("src/main/java/com/malik/ztesmartmanager/MasterpieceMap.kt")
        )
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tooTiny = sources.flatMap { file ->
            regex.findAll(file.readText())
                .map { match -> "${file.name}:${match.groupValues[1]}" }
                .filter { entry -> entry.substringAfter(':').toDouble() < 8.0 }
                .toList()
        }

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
    fun productionDashboard_usesAdaptiveLayoutAndMaterialTouchTargets() {
        val source = File("src/main/java/com/malik/ztesmartmanager/MasterpieceDashboard.kt").readText()
        assertTrue(source.contains("BoxWithConstraints"))
        assertTrue(source.contains("NavigationBar("))
        assertTrue(source.contains("Button("))
        assertTrue(source.contains("OutlinedButton("))
    }

    @Test
    fun homeDashboard_isScrollableAndDoesNotForceEverythingIntoViewport() {
        val source = File("src/main/java/com/malik/ztesmartmanager/MasterpieceDashboard.kt").readText()
        assertTrue(source.contains("private fun MpHome"))
        assertTrue(source.contains("LazyColumn("))
        assertTrue(source.contains("contentPadding = PaddingValues"))
        assertTrue(source.contains("verticalArrangement = Arrangement.spacedBy"))
    }

    @Test
    fun productionRoute_usesMasterpieceOnly() {
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        assertTrue(runtime.contains("MasterpieceDashboard("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV6("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV5("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV4("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV3("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV2("))
        assertTrue(!runtime.contains("ArabicHaiDashboard("))
    }

    @Test
    fun masterpieceDashboard_keepsTruthFirstLanguage() {
        val source = File("src/main/java/com/malik/ztesmartmanager/MasterpieceDashboard.kt").readText()
        val required = listOf(
            "بدون تخمين",
            "لا نختلق إحداثيات",
            "لا نختلق جهازًا",
            "read-back"
        )
        required.forEach { text -> assertTrue("سياسة Truth-First مفقودة: $text", source.contains(text)) }
    }
}
