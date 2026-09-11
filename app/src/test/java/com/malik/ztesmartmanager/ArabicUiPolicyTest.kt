package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicUiPolicyTest {

    @Test
    fun productionShell_isArabicAndForcesRtl() {
        val dashboard = source("PulseDashboard.kt").readText()
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
            "غلاف الأمان يجب أن يستخدم واجهة Pulse الجديدة",
            runtime.contains("PulseDashboard(")
        )
        assertFalse(
            "لا يجوز إعادة واجهات التصميم القديمة إلى مسار الإنتاج",
            runtime.contains("NovaDashboard(") ||
                runtime.contains("ImmersiveDashboard(") ||
                runtime.contains("ReferenceExactDashboard(") ||
                runtime.contains("MasterpieceDashboard(") ||
                runtime.contains("ArabicHaiDashboardV6(") ||
                runtime.contains("ArabicHaiDashboardV5(") ||
                runtime.contains("ArabicHaiDashboard(")
        )
    }

    @Test
    fun newUserFacingLiteralText_mustBeArabicOrTechnicalOnly() {
        val root = File("src/main/java/com/malik/ztesmartmanager")
        val violations = mutableListOf<String>()

        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name !in LEGACY_INACTIVE_UI }
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
    fun uiGuard_allowsSymbolsAndTechnicalTerms_butRejectsEnglishWords() {
        assertTrue(isArabicOrTechnicalOnly("☰"))
        assertTrue(isArabicOrTechnicalOnly("5G NSA"))
        assertTrue(isArabicOrTechnicalOnly("RSRP"))
        assertTrue(isArabicOrTechnicalOnly("ZTE Smart HAI"))
        assertTrue(isArabicOrTechnicalOnly("MapLibre"))
        assertFalse(isArabicOrTechnicalOnly("Speed Test"))
        assertFalse(isArabicOrTechnicalOnly("Network Stat"))
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
            "ZTE Smart HAI", "HAI Network", "MapLibre", "OpenStreetMap", "OpenFreeMap", "Cloudflare",
            "HAI", "H", "ZTE", "5G", "4G", "3G", "2G", "LTE", "NR", "NSA", "SA", "CA",
            "RSRP", "RSRQ", "SINR", "PCI", "ARFCN", "EARFCN", "MHz", "Mb/s", "dBm", "dB", "ms",
            "STC", "Mobily", "Zain", "Wi‑Fi", "LAN", "Ping", "Jitter", "Loss", "Band", "IP", "Firmware",
            "read-back", "eNB", "LIVE", "OFF", "LINK", "Runtime", "Cell Lock", "NR Lock", "Cell ID",
            "WAN Telemetry", "Thermal Telemetry", "Carrier Aggregation", "QoS", "Blacklist"
        )

        private val LEGACY_INACTIVE_UI = setOf(
            "ArabicHaiDashboard.kt",
            "ArabicHaiDashboardV2.kt",
            "ArabicHaiDashboardV3.kt",
            "ArabicHaiDashboardV4.kt",
            "ArabicHaiDashboardV5.kt",
            "ArabicHaiDashboardV6.kt",
            "ArabicHaiDashboardV6Pages.kt",
            "ArabicHaiDashboardV6Ui.kt",
            "HaiAdaptiveDashboard.kt",
            "HaiPreviewMatrix.kt",
            "PremiumDashboard.kt",
            "FinalDashboard.kt",
            "FinalFiveGCard.kt",
            "CarrierMatrixCard.kt",
            "HomeActivity.kt",
            "MainActivity.kt",
            "ReferenceDashboard.kt",
            "ReferenceMainActivity.kt",
            "ReferenceExactDashboard.kt",
            "MasterpieceDashboard.kt",
            "ImmersiveDashboard.kt",
            "NovaDashboard.kt",
            "NovaMap.kt"
        )
    }
}
