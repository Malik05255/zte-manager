package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiV2ReadabilityTest {

    @Test
    fun productionDashboard_hasNoTinyDirectFontSizes() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV2.kt").readText()
        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tiny = regex.findAll(source)
            .map { it.groupValues[1].toDouble() }
            .filter { it < 10.0 }
            .toList()

        assertTrue("واجهة الجوال تحتوي fontSize أصغر من 10sp: $tiny", tiny.isEmpty())
    }

    @Test
    fun responsivePolicy_neverShrinksTypographyBelowDesignedSize() {
        val policy = File("src/main/java/com/malik/ztesmartmanager/core/presentation/HaiResponsivePolicy.kt").readText()
        assertTrue(
            "يجب أن يبقى الحد الأدنى لـ textScale مساويًا 1.00",
            policy.contains("coerceIn(1.00f, 1.14f)")
        )
    }

    @Test
    fun primaryTouchTargets_areAtLeastFiftyDp() {
        val source = File("src/main/java/com/malik/ztesmartmanager/ArabicHaiDashboardV2.kt").readText()
        assertTrue(source.contains("private fun V2PrimaryAction"))
        assertTrue(source.contains("modifier.height(50.dp)"))
        assertTrue(source.contains("modifier = Modifier.fillMaxWidth().height(50.dp)"))
    }

    @Test
    fun productionRoute_usesV2Only() {
        val runtime = File("src/main/java/com/malik/ztesmartmanager/RuntimeAwareFinalDashboard.kt").readText()
        assertTrue(runtime.contains("ArabicHaiDashboardV2("))
        assertTrue(!runtime.contains("ArabicHaiDashboard("))
    }
}
