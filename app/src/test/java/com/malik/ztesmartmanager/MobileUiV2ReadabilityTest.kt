package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {

    @Test
    fun productionDashboard_neverUsesTinyText() {
        val root = File("src/main/java/com/malik/ztesmartmanager")
        val files = root.listFiles()?.filter {
            it.extension == "kt" && (
                it.name.startsWith("ZteManager") ||
                    it.name == "ZteRouterLoginScreen.kt" ||
                    it.name == "ZteReferenceChrome.kt"
                )
        }.orEmpty()
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tooTiny = files.flatMap { source ->
            regex.findAll(source.readText())
                .map { it.groupValues[1].toDouble() }
                .filter { it < 13.0 }
                .map { "${source.name}: $it" }
                .toList()
        }
        assertTrue("واجهة الهاتف تحتوي خطًا أصغر من 13sp: $tooTiny", tooTiny.isEmpty())
    }

    @Test
    fun responsivePolicy_neverShrinksTypographyBelowDesignedSize() {
        val policy = File("src/main/java/com/malik/ztesmartmanager/core/presentation/HaiResponsivePolicy.kt").readText()
        assertTrue(policy.contains("coerceIn(1.00f, 1.14f)"))
    }

    @Test
    fun home_matchesReferenceStructureAndStacksInsteadOfShrinking() {
        val home = File("src/main/java/com/malik/ztesmartmanager/ZteManagerHome.kt").readText()
        assertTrue(home.contains("LazyColumn("))
        assertTrue(home.contains("BoxWithConstraints"))
        assertTrue(home.contains("maxWidth >= 700.dp"))
        assertTrue(home.contains("ZteAdaptiveTwoColumn("))
        assertTrue(home.contains("ZteAdaptiveThreeColumn("))
        assertTrue(home.contains("if (wide)"))
        assertTrue(home.contains("Column(Modifier.fillMaxWidth()"))
    }

    @Test
    fun suppliedReferenceVisualHierarchy_isPresent() {
        val home = File("src/main/java/com/malik/ztesmartmanager/ZteManagerHome.kt").readText()
        listOf(
            "حالة الشبكة",
            "اختبار السرعة",
            "وضع الشبكة",
            "الدمج النشط للترددات",
            "أقرب برج شبكة",
            "الترددات النشطة",
            "قفل الترددات",
            "أدوات التشخيص",
            "مراقبة الإشارة المباشرة",
            "أفضل مكان للراوتر"
        ).forEach { phrase -> assertTrue("عنصر مرجعي مفقود: $phrase", home.contains(phrase)) }
    }

    @Test
    fun phoneChrome_breaksIntoRowsRatherThanCrammingHeader() {
        val chrome = File("src/main/java/com/malik/ztesmartmanager/ZteReferenceChrome.kt").readText()
        val dashboard = File("src/main/java/com/malik/ztesmartmanager/ZteManagerDashboard.kt").readText()
        assertTrue(chrome.contains("maxWidth < 560.dp"))
        assertTrue(chrome.contains("statusBarsPadding()"))
        assertTrue(chrome.contains("navigationBarsPadding()"))
        assertTrue(chrome.contains("ZteConnectionCard"))
        assertTrue(chrome.contains("ZteReferenceBottomBar"))
        assertTrue(dashboard.contains("ZteReferenceTopBar("))
        assertTrue(dashboard.contains("ZteReferenceBottomBar("))
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
    fun networkControls_remainExpandedAndTruthFirst() {
        val network = File("src/main/java/com/malik/ztesmartmanager/ZteManagerNetwork.kt").readText()
        assertTrue(network.contains("كل وضع في سطر مستقل"))
        assertTrue(network.contains("غير متاح"))
        assertTrue(network.contains("ZteNetworkModeRow("))
        assertTrue(network.contains("ZteNearbyCellCard("))
    }
}
