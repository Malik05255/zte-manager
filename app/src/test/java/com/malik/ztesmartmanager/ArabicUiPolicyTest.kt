package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicUiPolicyTest {

    @Test
    fun productionShell_isArabicAndForcesRtl() {
        val dashboard = source("ZteManagerDashboard.kt").readText()
        val runtime = source("RuntimeAwareFinalDashboard.kt").readText()
        val activity = source("FinalMainActivity.kt").readText()

        assertTrue(
            "واجهة الإنتاج يجب أن تفرض RTL حتى لو كانت لغة الهاتف مختلفة",
            dashboard.contains("LocalLayoutDirection provides LayoutDirection.Rtl")
        )
        assertTrue(
            "يجب فرض RTL على التطبيق كاملًا بما فيه شاشة الدخول",
            activity.contains("LocalLayoutDirection provides LayoutDirection.Rtl")
        )
        assertTrue(
            "غلاف الأمان يجب أن يستخدم واجهة ZTE Manager الجديدة",
            runtime.contains("ZteManagerDashboard(")
        )
        assertFalse(
            "لا يجوز إعادة أي واجهة تصميم قديمة إلى مسار الإنتاج",
            OLD_ROUTE_NAMES.any(runtime::contains)
        )
    }

    @Test
    fun oldDesignFiles_areActuallyDeleted() {
        val root = File("src/main/java/com/malik/ztesmartmanager")
        val leftovers = OLD_DESIGN_FILES.filter { File(root, it).exists() }
        assertTrue("بقيت ملفات تصميم قديمة: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun newUserFacingLiteralText_mustBeArabicOrTechnicalOnly() {
        val root = File("src/main/java/com/malik/ztesmartmanager")
        val violations = mutableListOf<String>()

        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                USER_FACING_PATTERNS.forEach { regex ->
                    regex.findAll(text).forEach { match ->
                        val literal = match.groupValues[1]
                        if (!isArabicOrTechnicalOnly(literal)) violations += "${file.name}: $literal"
                    }
                }
            }

        assertTrue(
            "وجدت نصوص واجهة إنجليزية غير معرّبة:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
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

    private fun isArabicOrTechnicalOnly(value: String): Boolean {
        if (value.any { it in '\u0600'..'\u06FF' }) return true
        var remaining = value
        TECHNICAL_TOKENS.sortedByDescending { it.length }
            .forEach { token -> remaining = remaining.replace(token, "", ignoreCase = false) }
        return remaining.none { it in 'A'..'Z' || it in 'a'..'z' }
    }

    companion object {
        private val USER_FACING_PATTERNS = listOf(
            Regex("Text\\(\\s*\"([^\"$]+)"),
            Regex("Toast\\.makeText\\([^,]+,\\s*\"([^\"$]+)"),
            Regex("createChooser\\([^,]+,\\s*\"([^\"$]+)"),
            Regex("newPlainText\\(\\s*\"([^\"$]+)")
        )

        private val TECHNICAL_TOKENS = listOf(
            "ZTE Smart HAI", "ZTE Manager", "MapLibre", "OpenStreetMap", "OpenFreeMap", "Cloudflare",
            "Tower Guard", "Cell Lock", "Hardware", "Firmware", "Runtime", "read-back",
            "ZTE", "5G", "4G", "3G", "2G", "LTE", "NR", "NSA", "SA", "CA",
            "RSRP", "RSRQ", "SINR", "PCI", "ARFCN", "EARFCN", "MHz", "Mb/s", "dBm", "dB", "ms",
            "STC", "Mobily", "Zain", "Wi‑Fi", "Wi", "LAN", "Ping", "Jitter", "Loss", "Band", "IP",
            "Cell ID", "QoS", "API"
        )

        private val OLD_ROUTE_NAMES = listOf(
            "GlassDashboard(", "PulseDashboard(", "NovaDashboard(", "ImmersiveDashboard(",
            "ReferenceExactDashboard(", "MasterpieceDashboard(", "ArabicHaiDashboard("
        )

        private val OLD_DESIGN_FILES = listOf(
            "GlassComponents.kt", "GlassDashboard.kt", "GlassDesignSystem.kt", "GlassHelpers.kt",
            "GlassHomeHero.kt", "GlassHomePrimaryCards.kt", "GlassHomeScreen.kt", "GlassHomeSecondaryCards.kt",
            "GlassMoreScreen.kt", "GlassNetworkComponents.kt", "GlassNetworkScreen.kt", "GlassQuality.kt",
            "GlassShell.kt", "GlassToolsScreen.kt", "GlassTowerControls.kt",
            "NovaDashboard.kt", "NovaEffects.kt", "NovaMap.kt",
            "PulseConnectedDeviceCompat.kt", "PulseDashboard.kt", "PulseNetworkMap.kt"
        )
    }
}
