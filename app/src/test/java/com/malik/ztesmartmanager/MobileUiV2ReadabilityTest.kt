package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {

    private fun source(name: String) = File("src/main/java/com/malik/ztesmartmanager/$name").readText()

    @Test
    fun productionV5_hasNoUnreadableDirectFontSizes() {
        val production = source("HaiHomeDashboardV5.kt") + "\n" + source("HaiDashboardV5.kt")
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tiny = regex.findAll(production)
            .map { it.groupValues[1].toDouble() }
            .filter { it < 9.0 }
            .toList()

        assertTrue("واجهة V5 تحتوي fontSize أصغر من 9sp: $tiny", tiny.isEmpty())
    }

    @Test
    fun sizingPolicy_isWidthDrivenAndDoesNotOverridePlatformDensity() {
        val sizing = source("HaiUiSizing.kt")
        assertTrue(sizing.contains("screenWidthDp"))
        assertTrue(sizing.contains("width < 600"))
        assertTrue(sizing.contains("PHONE_DESIGN_WIDTH_DP = 393f"))
        assertTrue(!sizing.contains("LocalDensity provides controlledDensity"))
        assertTrue(!sizing.contains("Density(density ="))
        assertTrue(!sizing.contains("fontScale ="))
    }

    @Test
    fun productionShell_usesOneSizingProviderWithoutGlobalDpScaling() {
        val sizing = source("HaiUiSizing.kt")
        val runtime = source("RuntimeAwareFinalDashboard.kt")
        val login = source("ZteRouterLoginScreen.kt")
        val chrome = source("HaiSharedChrome.kt")

        assertTrue(sizing.contains("LocalHaiUiMetrics provides metrics"))
        assertTrue(runtime.contains("HaiUiScaleProvider"))
        assertTrue(login.contains("HaiUiScaleProvider"))
        assertTrue(chrome.contains("LocalHaiUiMetrics.current"))
    }

    @Test
    fun rebuiltV5Screens_scrollContentAndKeepBottomNavOutsideTheList() {
        val shell = source("HaiDashboardV5.kt")
        val home = source("HaiHomeDashboardV5.kt")

        assertTrue(shell.contains("LazyColumn("))
        assertTrue(home.contains("LazyColumn("))
        assertTrue(shell.contains("D5BottomNav(section)"))
        assertTrue(home.contains("HaiSharedBottomNav("))
        assertTrue(shell.contains("modifier = Modifier.weight(1f)"))
        assertTrue(home.contains("modifier = Modifier.weight(1f).fillMaxWidth()"))
    }

    @Test
    fun rebuiltHomeV5_isTheProductionHome() {
        val shell = source("HaiDashboardV5.kt")
        val runtime = source("RuntimeAwareFinalDashboard.kt")
        val home = source("HaiHomeDashboardV5.kt")

        assertTrue(runtime.contains("HaiDashboardV5("))
        assertTrue(shell.contains("HaiHomeDashboardV5("))
        assertTrue(home.contains("V5NetworkHero("))
        assertTrue(home.contains("V5SignalGrid("))
        assertTrue(home.contains("V5BandsCard("))
        assertTrue(home.contains("V5SpeedCard("))
        assertTrue(home.contains("V5NetworkModeCard("))
        assertTrue(home.contains("V5TowerCard("))
        assertTrue(home.contains("V5QuickActions("))
    }

    @Test
    fun sharedChrome_hasSafeTouchTargetsAndNativeNavigationSpace() {
        val sizing = source("HaiUiSizing.kt")
        val chrome = source("HaiSharedChrome.kt")
        assertTrue(sizing.contains("minimumTouchTarget = 48.dp"))
        assertTrue(sizing.contains("bottomNavHeight = 72.dp"))
        assertTrue(chrome.contains("navigationBarsPadding()"))
    }

    @Test
    fun productionRoute_usesV5Only() {
        val runtime = source("RuntimeAwareFinalDashboard.kt")
        assertTrue(runtime.contains("HaiDashboardV5("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV3("))
        assertTrue(!runtime.contains("ArabicHaiDashboardV2("))
        assertTrue(!runtime.contains("ArabicHaiDashboard("))
        assertTrue(!runtime.contains("HaiAdaptiveDashboard("))
    }

    @Test
    fun v5DoesNotRestoreGlobalReferenceHeightOrDensityScaling() {
        val shell = source("HaiDashboardV5.kt")
        val home = source("HaiHomeDashboardV5.kt")
        val sizing = source("HaiUiSizing.kt")

        assertTrue(!shell.contains("designHeight"))
        assertTrue(!home.contains("designHeight"))
        assertTrue(!home.contains("H4.HeroHeight"))
        assertTrue(!sizing.contains("controlledDensity"))
    }
}
