package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaiReferenceDesignTest {

    @Test
    fun homeDashboard_keepsApprovedReferenceStructure() {
        val entry = File("src/main/java/com/malik/ztesmartmanager/TargetHomeDashboard.kt").readText()
        val source = File("src/main/java/com/malik/ztesmartmanager/HaiHomeDashboardV4.kt").readText()

        assertTrue("مسار الإنتاج يجب أن يفتح واجهة V4 الجديدة", entry.contains("HaiHomeDashboardV4("))
        listOf(
            "H4Hero",
            "H4Metrics",
            "H4Speed",
            "H4NetworkMode",
            "H4Tower",
            "H4Signal",
            "H4Bands",
            "H4Tools"
        ).forEach { name -> assertTrue("العنصر المرجعي مفقود من V4: $name", source.contains(name)) }
    }

    @Test
    fun homeDashboard_doesNotHardcodeReferenceNetworkFacts() {
        val source = File("src/main/java/com/malik/ztesmartmanager/HaiHomeDashboardV4.kt").readText()

        listOf("512837", "286.7", "48.3", "1.2 كم").forEach { fake ->
            assertFalse("لا يجوز تثبيت قيمة مرجعية وهمية في الإنتاج: $fake", source.contains(fake))
        }

        assertTrue(source.contains("_zte_nr_active_verified"))
        assertTrue(source.contains("_zte_lte_active_verified"))
        assertTrue(source.contains("h4Bands(snapshot)"))
        assertTrue(source.contains("h4Aggregation(snapshot, bands)"))
    }

    @Test
    fun sharedChrome_andHome_useSameReferenceDesignSystem() {
        val design = File("src/main/java/com/malik/ztesmartmanager/HaiReferenceDesign.kt").readText()
        val chrome = File("src/main/java/com/malik/ztesmartmanager/HaiSharedChrome.kt").readText()
        val entry = File("src/main/java/com/malik/ztesmartmanager/TargetHomeDashboard.kt").readText()
        val home = File("src/main/java/com/malik/ztesmartmanager/HaiHomeDashboardV4.kt").readText()

        assertTrue(design.contains("object HaiReferenceDesign"))
        assertTrue(design.contains("HeroHeight"))
        assertTrue(chrome.contains("HaiReferenceDesign"))
        assertTrue(entry.contains("HaiHomeDashboardV4("))
        assertTrue(home.contains("private val H4 = HaiReferenceDesign"))
        assertTrue(home.contains("HaiReferenceCard"))
    }
}
