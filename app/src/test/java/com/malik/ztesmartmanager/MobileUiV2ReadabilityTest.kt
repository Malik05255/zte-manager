package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {

    @Test
    fun productionDashboard_neverUsesTinyText() {
        val root = File("src/main/java/com/malik/ztesmartmanager")
        val files = root.listFiles()?.filter {
            it.extension == "kt" && (it.name.startsWith("ZteManager") || it.name == "ZteRouterLoginScreen.kt")
        }.orEmpty()
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tooTiny = files.flatMap { source ->
            regex.findAll(source.readText())
                .map { it.groupValues[1].toDouble() }
                .filter { it < 12.0 }
                .map { "${source.name}: $it" }
                .toList()
        }
        assertTrue("واجهة Honor 200 تحتوي خطًا أصغر من 12sp: $tooTiny", tooTiny.isEmpty())
    }

    @Test
    fun responsivePolicy_neverShrinksTypographyBelowDesignedSize() {
        val policy = File("src/main/java/com/malik/ztesmartmanager/core/presentation/HaiResponsivePolicy.kt").readText()
        assertTrue(policy.contains("coerceIn(1.00f, 1.14f)"))
    }

    @Test
    fun home_isScrollableAndStacksOnPhones() {
        val home = File("src/main/java/com/malik/ztesmartmanager/ZteManagerHome.kt").readText()
        assertTrue(home.contains("LazyColumn("))
        assertTrue(home.contains("BoxWithConstraints"))
        assertTrue(home.contains("maxWidth >= 700.dp"))
        assertTrue(home.contains("ZteHero(snapshot, qualityScore)"))
        assertTrue(home.contains("ZteSpeedCard("))
        assertTrue(home.contains("ZteModeCard("))
        assertTrue(home.contains("ZteTowerMapCard("))
        assertTrue(home.contains("ZteSignalMonitorCard("))
    }

    @Test
    fun referenceVisualLanguage_isPresent() {
        val home = File("src/main/java/com/malik/ztesmartmanager/ZteManagerHome.kt").readText()
        val theme = File("src/main/java/com/malik/ztesmartmanager/ZteManagerTheme.kt").readText()
        listOf(
            "حالة الشبكة",
            "اختبار السرعة",
            "وضع الشبكة",
            "الدمج النشط للترددات",
            "أقرب برج شبكة",
            "الترددات النشطة",
            "مراقبة الإشارة المباشرة"
        ).forEach { phrase -> assertTrue("عنصر مرجعي مفقود: $phrase", home.contains(phrase)) }
        assertTrue(theme.contains("ZteHeroBrush"))
        assertTrue(theme.contains("RoundedCornerShape(24.dp)"))
    }

    @Test
    fun mapDrawsLinkOnlyFromVerifiedTowerCoordinates() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ZteManagerMap.kt").readText()
        assertTrue(source.contains("zteVerifiedTowerCoordinate(snapshot)"))
        assertTrue(source.contains("LineLayer("))
        assertTrue(source.contains("لن نرسم خطًا وهميًا"))
        assertTrue(source.contains("zteLineGeoJson("))
    }

    @Test
    fun productionRoute_usesOnlyFreshDashboard() {
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        assertTrue(runtime.contains("ZteManagerDashboard("))
    }

    @Test
    fun networkControls_keepTruthFirstLanguage() {
        val network = File("src/main/java/com/malik/ztesmartmanager/ZteManagerNetwork.kt").readText()
        assertTrue(network.contains("read-back"))
        assertTrue(network.contains("الاختيار لا يعني أنها أصبحت نشطة"))
        assertTrue(network.contains("غير متاح"))
    }
}
