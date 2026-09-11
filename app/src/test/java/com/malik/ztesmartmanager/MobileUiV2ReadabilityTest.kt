package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {
    @Test
    fun productionDashboard_keepsMicroLabelsReadable() {
        val root = File("src/main/java/com/malik/ztesmartmanager")
        val files = root.listFiles()?.filter { it.name.startsWith("Glass") && it.extension == "kt" }.orEmpty() +
            File(root, "PulseNetworkMap.kt")
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tooTiny = files.flatMap { source ->
            regex.findAll(source.readText())
                .map { it.groupValues[1].toDouble() }
                .filter { it < 8.0 }
                .toList()
        }
        assertTrue("واجهة الجوال تحتوي خطًا أصغر من 8sp: $tooTiny", tooTiny.isEmpty())
    }

    @Test
    fun responsivePolicy_neverShrinksTypographyBelowDesignedSize() {
        val policy = File("src/main/java/com/malik/ztesmartmanager/core/presentation/HaiResponsivePolicy.kt").readText()
        assertTrue(policy.contains("coerceIn(1.00f, 1.14f)"))
    }

    @Test
    fun glassDashboard_isHomeFirstAndAvoidsPhoneCramming() {
        val home = File("src/main/java/com/malik/ztesmartmanager/GlassHomeScreen.kt").readText()
        val hero = File("src/main/java/com/malik/ztesmartmanager/GlassHomeHero.kt").readText()
        assertTrue(home.contains("LazyColumn("))
        assertTrue(home.contains("BoxWithConstraints"))
        assertTrue(home.contains("maxWidth >= 620.dp"))
        assertTrue(home.contains("GlassHeroSection(snapshot)"))
        assertTrue(home.contains("GlassPlacementCard("))
        assertTrue(home.contains("GlassSpeedTestCard("))
        assertTrue(home.contains("GlassMapCard("))
        assertTrue(hero.contains("GlassRouterVisual("))
    }

    @Test
    fun homeExplainsQualityInArabicWordsInsteadOfOnlyScores() {
        val helpers = File("src/main/java/com/malik/ztesmartmanager/GlassHelpers.kt").readText()
        listOf(
            "ممتاز جدًا",
            "جيد جدًا",
            "بقيت خطوة بسيطة",
            "ثبّت الراوتر هنا",
            "غيّر مكان الراوتر"
        ).forEach { phrase ->
            assertTrue("الوصف العربي المبسط مفقود: $phrase", helpers.contains(phrase))
        }
    }

    @Test
    fun mapDrawsLinkOnlyFromVerifiedTowerCoordinates() {
        val source = File("src/main/java/com/malik/ztesmartmanager/PulseNetworkMap.kt").readText()
        assertTrue(source.contains("verifiedTowerCoordinate(snapshot)"))
        assertTrue(source.contains("LineLayer("))
        assertTrue(source.contains("لن نرسم خطًا وهميًا"))
        assertTrue(source.contains("lineGeoJson("))
    }

    @Test
    fun productionRoute_usesGlassDashboard() {
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        assertTrue(runtime.contains("GlassDashboard("))
    }

    @Test
    fun productionDashboard_keepsTruthFirstLanguage() {
        val bands = File("src/main/java/com/malik/ztesmartmanager/GlassNetworkComponents.kt").readText()
        val map = File("src/main/java/com/malik/ztesmartmanager/PulseNetworkMap.kt").readText()
        assertTrue(bands.contains("التطبيق يتحقق بعد التنفيذ"))
        assertTrue(map.contains("لن نرسم خطًا وهميًا"))
    }
}
