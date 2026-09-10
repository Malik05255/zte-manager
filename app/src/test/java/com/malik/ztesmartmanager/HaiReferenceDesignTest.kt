package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaiReferenceDesignTest {

    @Test
    fun homeDashboard_keepsApprovedReferenceStructure() {
        val source = File("src/main/java/com/malik/ztesmartmanager/TargetHomeDashboard.kt").readText()
        listOf(
            "HomeHeroCard",
            "HomeMetricRow",
            "HomeSpeedCard",
            "HomeNetworkModeCard",
            "HomeTowerCard",
            "HomeSignalCard",
            "HomeBandsCard",
            "HomeQuickToolsCard"
        ).forEach { name -> assertTrue("العنصر المرجعي مفقود: $name", source.contains(name)) }
    }

    @Test
    fun homeDashboard_doesNotHardcodeReferenceNetworkFacts() {
        val source = File("src/main/java/com/malik/ztesmartmanager/TargetHomeDashboard.kt").readText()
        listOf("512837", "286.7", "48.3", "1.2 كم").forEach { fake ->
            assertFalse("لا يجوز تثبيت قيمة مرجعية وهمية في الإنتاج: $fake", source.contains(fake))
        }
        assertTrue(source.contains("_zte_nr_active_verified"))
        assertTrue(source.contains("_zte_lte_active_verified"))
        assertTrue(source.contains("activeBandLabels(snapshot)"))
        assertTrue(source.contains("aggregationSummary(snapshot, bands)"))
    }

    @Test
    fun sharedChrome_andHome_useSameReferenceDesignSystem() {
        val design = File("src/main/java/com/malik/ztesmartmanager/HaiReferenceDesign.kt").readText()
        val chrome = File("src/main/java/com/malik/ztesmartmanager/HaiSharedChrome.kt").readText()
        val home = File("src/main/java/com/malik/ztesmartmanager/TargetHomeDashboard.kt").readText()

        assertTrue(design.contains("object HaiReferenceDesign"))
        assertTrue(design.contains("HeroHeight"))
        assertTrue(chrome.contains("HaiReferenceDesign"))
        assertTrue(home.contains("HaiReferenceCard"))
    }
}
