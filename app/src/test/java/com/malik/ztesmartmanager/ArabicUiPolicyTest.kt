package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicUiPolicyTest {

    @Test
    fun productionShell_isArabicAndForcesRtl() {
        val dashboard = source("HaiOneDashboard.kt").readText()
        val runtime = source("RuntimeAwareFinalDashboard.kt").readText()
        val activity = source("FinalMainActivity.kt").readText()

        assertTrue(
            "واجهة الإنتاج يجب أن تفرض RTL",
            dashboard.contains("LocalLayoutDirection provides LayoutDirection.Rtl")
        )
        assertTrue(
            "يجب فرض RTL على التطبيق كاملًا بما فيه الدخول",
            activity.contains("LocalLayoutDirection provides LayoutDirection.Rtl")
        )
        assertTrue(
            "غلاف الأمان يجب أن يستدعي الواجهة الجديدة عبر العقدة الإنتاجية",
            runtime.contains("ZteManagerDashboard(")
        )
    }

    @Test
    fun supersededVisualDesignFiles_areActuallyDeleted() {
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
    fun freshVisualSystem_isTheOnlyScreenFamily() {
        val root = File("src/main/java/com/malik/ztesmartmanager")
        listOf(
            "HaiOneTheme.kt",
            "HaiOneHome.kt",
            "HaiOneScreens.kt",
            "HaiOneDashboard.kt",
            "HaiOneLogin.kt",
            "HaiOneMap.kt"
        ).forEach { name -> assertTrue("ملف الواجهة الجديدة مفقود: $name", File(root, name).exists()) }

        val dashboard = source("HaiOneDashboard.kt").readText()
        assertTrue(dashboard.contains("HaiHome("))
        assertTrue(dashboard.contains("HaiNetworkScreen("))
        assertTrue(dashboard.contains("HaiTowersScreen("))
        assertTrue(dashboard.contains("HaiDevicesScreen("))
        assertTrue(dashboard.contains("HaiToolsScreen("))
    }

    @Test
    fun freshUi_avoidsLongExplanatoryCopy() {
        val files = listOf("HaiOneTheme.kt", "HaiOneHome.kt", "HaiOneScreens.kt", "HaiOneLogin.kt")
            .map(::source)
        val banned = listOf(
            "بدون أرقام غامضة",
            "كل خيار في سطر مستقل",
            "واجهة واضحة بدون حشر أو نص صغير",
            "لا نعرض بيانات وهمية",
            "قياس حقيقي للاتصال بالإنترنت"
        )
        files.forEach { file ->
            val text = file.readText()
            banned.forEach { phrase -> assertFalse("وجد شرح قديم زائد: $phrase في ${file.name}", text.contains(phrase)) }
        }
    }

    @Test
    fun manifestAndDefaultName_keepArabicRtlContract() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val strings = File("src/main/res/values/strings.xml").readText()
        assertTrue(manifest.contains("android:supportsRtl=\"true\""))
        assertTrue(manifest.contains("android:label=\"@string/app_name\""))
        assertTrue(manifest.contains("android:localeConfig=\"@xml/locales_config\""))
        assertTrue(strings.contains("<string name=\"app_name\">ZTE Smart HAI</string>"))
    }

    private fun source(name: String) = File("src/main/java/com/malik/ztesmartmanager/$name")
}
