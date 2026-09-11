package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {
    @Test
    fun productionDashboard_keepsMicroLabelsReadable() {
        val files = listOf(
            File("src/main/java/com/malik/ztesmartmanager/PulseDashboard.kt"),
            File("src/main/java/com/malik/ztesmartmanager/PulseNetworkMap.kt")
        )
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
    fun pulseDashboard_isHomeFirstAndAvoidsCardCramming() {
        val source = File("src/main/java/com/malik/ztesmartmanager/PulseDashboard.kt").readText()
        assertTrue(source.contains("private fun PulseHome"))
        assertTrue(source.contains("LazyColumn("))
        assertTrue(source.contains("PulseHero(snapshot)"))
        assertTrue(source.contains("PulsePlacementCard("))
        assertTrue(source.contains("PulseNetworkMap(snapshot"))
        assertTrue(source.contains("PulseSpeedCard("))
        assertTrue(source.contains("PulseModeCard("))
    }

    @Test
    fun homeExplainsQualityInArabicWordsInsteadOfOnlyScores() {
        val source = File("src/main/java/com/malik/ztesmartmanager/PulseDashboard.kt").readText()
        listOf(
            "ممتاز جدًا",
            "جيد جدًا",
            "بقيت خطوة بسيطة",
            "ثبّت الراوتر هنا",
            "غيّر المكان"
        ).forEach { phrase ->
            assertTrue("الوصف العربي المبسط مفقود: $phrase", source.contains(phrase))
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
    fun productionRoute_usesPulseDashboard() {
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        assertTrue(runtime.contains("PulseDashboard("))
    }

    @Test
    fun productionDashboard_keepsTruthFirstLanguage() {
        val dashboard = File("src/main/java/com/malik/ztesmartmanager/PulseDashboard.kt").readText()
        val map = File("src/main/java/com/malik/ztesmartmanager/PulseNetworkMap.kt").readText()
        assertTrue(dashboard.contains("لا يُرسم إلا إذا كان موثقًا"))
        assertTrue(dashboard.contains("لن نعلن نجاح التغيير قبل أن يقرأه الراوتر مرة أخرى"))
        assertTrue(map.contains("لا نرسم خطًا وهميًا"))
    }
}
