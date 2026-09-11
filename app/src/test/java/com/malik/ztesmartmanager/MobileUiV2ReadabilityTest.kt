package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {

    @Test
    fun productionDashboard_keepsMicroLabelsReadable() {
        val sources = listOf(
            File("src/main/java/com/malik/ztesmartmanager/ImmersiveDashboard.kt"),
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
    fun immersiveDashboard_hasRealMotionAndHorizontalDiscovery() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ImmersiveDashboard.kt").readText()
        assertTrue(source.contains("rememberInfiniteTransition"))
        assertTrue(source.contains("animateFloatAsState"))
        assertTrue(source.contains("animateColorAsState"))
        assertTrue(source.contains("LazyRow("))
        assertTrue(source.contains("shadow("))
    }

    @Test
    fun homeDashboard_isScrollableAndAvoidsDenseTwoColumnPhoneLayout() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ImmersiveDashboard.kt").readText()
        assertTrue(source.contains("private fun IxHome"))
        assertTrue(source.contains("LazyColumn("))
        assertTrue(source.contains("verticalArrangement = Arrangement.spacedBy(17.dp)"))
        assertTrue(source.contains("IxSpeedCard(performance, speedBusy, onSpeedTest)"))
        assertTrue(source.contains("IxModeStrip(snapshot, controlBusy, onMode)"))
    }

    @Test
    fun productionRoute_usesImmersiveDashboard() {
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        assertTrue(runtime.contains("ImmersiveDashboard("))
        assertTrue(!runtime.contains("ReferenceExactDashboard("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV6("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV5("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV4("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV3("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV2("))
        assertTrue(!runtime.contains("ArabicHaiDashboard("))
    }

    @Test
    fun productionDashboard_keepsTruthFirstLanguage() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ImmersiveDashboard.kt").readText()
        val required = listOf(
            "بدون تخمين",
            "لا نختلق إحداثيات",
            "لا نختلق جهازًا",
            "read-back"
        )
        required.forEach { text -> assertTrue("سياسة Truth-First مفقودة: $text", source.contains(text)) }
    }
}
