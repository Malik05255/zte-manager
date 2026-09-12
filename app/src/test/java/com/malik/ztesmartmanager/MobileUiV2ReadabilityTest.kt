package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {

    @Test
    fun productionUi_hasNoTinyText() {
        val root = File("src/main/java/com/malik/ztesmartmanager")
        val files = root.listFiles()?.filter {
            it.extension == "kt" && (it.name.startsWith("HaiOne") || it.name == "ZteManagerMap.kt")
        }.orEmpty()
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tiny = files.flatMap { source ->
            regex.findAll(source.readText())
                .map { it.groupValues[1].toDouble() }
                .filter { it < 14.0 }
                .map { "${source.name}: $it" }
                .toList()
        }
        assertTrue("وجد خط أصغر من 14sp: $tiny", tiny.isEmpty())
    }

    @Test
    fun homeShowsTheCoreExperienceInOneScrollableFlow() {
        val home = source("HaiOneHome.kt")
        listOf(
            "HaiHero(snapshot, qualityScore)",
            "HaiSpeed(",
            "HaiModes(",
            "HaiBands(",
            "HaiPlacement(",
            "HaiTower(",
            "HaiSignalHistory(",
            "HaiDevices("
        ).forEach { assertTrue("عنصر رئيسي مفقود: $it", home.contains(it)) }
        assertTrue(home.contains("LazyColumn("))
    }

    @Test
    fun portraitAndLandscapeUseResponsiveMediaInsteadOfShrinkingContent() {
        val home = source("HaiOneHome.kt")
        val login = source("HaiOneLogin.kt")
        assertTrue(home.contains("BoxWithConstraints"))
        assertTrue(home.contains("fillMaxWidth().aspectRatio("))
        assertTrue(home.contains("maxWidth < 520.dp"))
        assertTrue(login.contains("BoxWithConstraints"))
        assertTrue(login.contains("fillMaxWidth().aspectRatio("))
        assertFalse(home.contains("graphicsLayer"))
        assertFalse(home.contains("scaleX"))
        assertFalse(home.contains("scaleY"))
    }

    @Test
    fun supersededScreenDesignFilesAreDeleted() {
        val root = File("src/main/java/com/malik/ztesmartmanager")
        val old = listOf(
            "ZteManagerDashboard.kt",
            "ZteManagerHome.kt",
            "ZteManagerMore.kt",
            "ZteManagerNetwork.kt",
            "ZteManagerTheme.kt",
            "ZteManagerTools.kt",
            "ZteReferenceChrome.kt",
            "ZteRouterLoginScreen.kt",
            "ZteSaveableCompat.kt"
        ).filter { File(root, it).exists() }
        assertTrue("بقيت ملفات تصميم قديمة: $old", old.isEmpty())
    }

    @Test
    fun mapEngineStillRequiresVerifiedTowerCoordinatesForTheLink() {
        val map = source("ZteManagerMap.kt")
        assertTrue(map.contains("zteVerifiedTowerCoordinate(snapshot)"))
        assertTrue(map.contains("LineLayer("))
        assertTrue(map.contains("ZTE_EMPTY_GEOJSON"))
    }

    private fun source(name: String) = File("src/main/java/com/malik/ztesmartmanager/$name").readText()
}
