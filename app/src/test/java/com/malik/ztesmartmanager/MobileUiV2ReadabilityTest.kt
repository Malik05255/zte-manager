package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
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
    fun home_isScrollableSingleColumnAndAvoidsPhoneGrids() {
        val home = File("src/main/java/com/malik/ztesmartmanager/ZteManagerHome.kt").readText()
        assertTrue(home.contains("LazyColumn("))
        assertFalse(home.contains("BoxWithConstraints"))
        assertFalse(home.contains("maxWidth >= 700.dp"))
        assertTrue(home.contains("ZteHero(snapshot, qualityScore)"))
        assertTrue(home.contains("ZtePlacementCoach("))
        assertTrue(home.contains("ZteSpeedCard("))
        assertTrue(home.contains("ZteModeCard("))
        assertTrue(home.contains("ZteTowerMapCard("))
        assertTrue(home.contains("ZteSignalSummary("))
    }

    @Test
    fun humanFirstVisualLanguage_isPresent() {
        val home = File("src/main/java/com/malik/ztesmartmanager/ZteManagerHome.kt").readText()
        val theme = File("src/main/java/com/malik/ztesmartmanager/ZteManagerTheme.kt").readText()
        listOf(
            "أفضل مكان للراوتر",
            "بدون أرقام غامضة",
            "اختبار السرعة",
            "كل خيار في سطر مستقل",
            "الخريطة والبرج",
            "ملخص مفهوم بدل الأرقام الفنية",
            "ثلاثة مسارات واضحة بدل شبكة أزرار مزدحمة"
        ).forEach { phrase -> assertTrue("عنصر جديد مفقود: $phrase", home.contains(phrase)) }
        assertTrue(theme.contains("statusBarsPadding()"))
        assertTrue(theme.contains("navigationBarsPadding()"))
        assertTrue(theme.contains("RoundedCornerShape(28.dp)"))
        assertTrue(theme.contains("softWrap = false"))
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
    fun networkControls_areExpandedVerticallyNotCompressed() {
        val network = File("src/main/java/com/malik/ztesmartmanager/ZteManagerNetwork.kt").readText()
        assertTrue(network.contains("كل وضع في سطر مستقل"))
        assertTrue(network.contains("غير متاح"))
        assertTrue(network.contains("ZteNetworkModeRow("))
        assertTrue(network.contains("ZteNearbyCellCard("))
    }
}
