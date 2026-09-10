package com.malik.ztesmartmanager.core.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveLayoutPolicyTest {
    @Test
    fun normalPhoneKeepsFullTelemetryLayout() {
        val profile = ResponsiveLayoutPolicy.resolve(widthDp = 412, heightDp = 915)

        assertFalse(profile.compact)
        assertFalse(profile.ultraCompact)
        assertEquals(92, profile.signalPlotHeightDp)
        assertEquals(245, profile.detailsMaxHeightDp)
    }

    @Test
    fun shortPhoneUsesCompactLayout() {
        val profile = ResponsiveLayoutPolicy.resolve(widthDp = 390, heightDp = 680)

        assertTrue(profile.compact)
        assertFalse(profile.ultraCompact)
        assertEquals(68, profile.signalPlotHeightDp)
        assertEquals(185, profile.detailsMaxHeightDp)
    }

    @Test
    fun veryShortPhoneUsesUltraCompactLayout() {
        val profile = ResponsiveLayoutPolicy.resolve(widthDp = 360, heightDp = 590)

        assertTrue(profile.compact)
        assertTrue(profile.ultraCompact)
        assertEquals(52, profile.signalPlotHeightDp)
        assertEquals(140, profile.detailsMaxHeightDp)
    }

    @Test
    fun narrowPhoneCompactsEvenWhenTall() {
        val compact = ResponsiveLayoutPolicy.resolve(widthDp = 350, heightDp = 820)
        val ultra = ResponsiveLayoutPolicy.resolve(widthDp = 320, heightDp = 820)

        assertTrue(compact.compact)
        assertFalse(compact.ultraCompact)
        assertTrue(ultra.ultraCompact)
    }
}
