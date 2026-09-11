package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {
    @Test
    fun productionDashboard_keepsMicroLabelsReadable() {
        val source = File("src/main/java/com/malik/ztesmartmanager/NovaDashboard.kt")
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tooTiny = regex.findAll(source.readText())
            .map { it.groupValues[1].toDouble() }
            .filter { it < 8.0 }
            .toList()
        assertTrue("واجهة الجوال تحتوي خطًا أصغر من 8sp: $tooTiny", tooTiny.isEmpty())
    }

    @Test
    fun responsivePolicy_neverShrinksTypographyBelowDesignedSize() {
        val policy = File("src/main/java/com/malik/ztesmartmanager/core/presentation/HaiResponsivePolicy.kt").readText()
        assertTrue(policy.contains("coerceIn(1.00f, 1.14f)"))
    }

    @Test
    fun novaDashboard_hasMotionAndDistinctInteractionPatterns() {
        val source = File("src/main/java/com/malik/ztesmartmanager/NovaDashboard.kt").readText()
        assertTrue(source.contains("rememberInfiniteTransition"))
        assertTrue(source.contains("animateFloatAsState"))
        assertTrue(source.contains("animateColorAsState"))
        assertTrue(source.contains("LazyRow("))
        assertTrue(source.contains("NovaRadar("))
        assertTrue(source.contains("RealNetworkMap("))
        assertTrue(source.contains("NovaBandMatrix("))
    }

    @Test
    fun homeDashboard_isScrollableAndUsesProgressiveDisclosure() {
        val source = File("src/main/java/com/malik/ztesmartmanager/NovaDashboard.kt").readText()
        assertTrue(source.contains("private fun NovaHome"))
        assertTrue(source.contains("LazyColumn("))
        assertTrue(source.contains("NovaQuickRail("))
        assertTrue(source.contains("NovaSpeedStudio("))
        assertTrue(source.contains("NovaModeRail("))
    }

    @Test
    fun productionRoute_usesNovaDashboard() {
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        assertTrue(runtime.contains("NovaDashboard("))
    }

    @Test
    fun productionDashboard_keepsTruthFirstLanguage() {
        val source = File("src/main/java/com/malik/ztesmartmanager/NovaDashboard.kt").readText()
        listOf("بدون أرقام تجريبية", "لا تُختلق", "read-back").forEach {
            assertTrue("سياسة الدقة مفقودة: $it", source.contains(it))
        }
    }
}
