package com.malik.ztesmartmanager.core.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaiResponsivePolicyTest {
    @Test
    fun compactPhone_neverShrinksTypographyAndStacksContent() {
        val spec = HaiResponsivePolicy.resolve(360, 800)
        assertEquals(HaiSizeClass.COMPACT, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertTrue(spec.textScale >= 1.00f)
        assertTrue(spec.scale >= 0.88f)
        assertTrue(spec.networkCardHeightDp >= 250)
        assertTrue(spec.bottomBarHeightDp >= 76)
    }

    @Test
    fun smallModernPhone_390x844_reflowsInsteadOfCrushingContent() {
        val spec = HaiResponsivePolicy.resolve(390, 844)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertTrue(spec.networkCardHeightDp >= 260)
        assertTrue(spec.speedCardHeightDp >= 250)
        assertTrue(spec.textScale >= 1.00f)
    }

    @Test
    fun referencePhone_430x932_usesReadableMobileRhythm() {
        val spec = HaiResponsivePolicy.resolve(430, 932)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertFalse(spec.denseHeader)
        assertTrue(spec.scale in 0.99f..1.01f)
        assertTrue(spec.textScale in 0.99f..1.01f)
        assertEquals(16, spec.horizontalPaddingDp)
        assertEquals(16, spec.sectionGapDp)
        assertEquals(28, spec.cardRadiusDp)
        assertEquals(270, spec.networkCardHeightDp)
        assertEquals(264, spec.speedCardHeightDp)
        assertEquals(84, spec.bottomBarHeightDp)
    }

    @Test
    fun largePhone_412x915_usesAvailableSpaceWithoutTinyText() {
        val spec = HaiResponsivePolicy.resolve(412, 915)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertTrue(spec.textScale in 1.00f..1.14f)
        assertTrue(spec.networkCardHeightDp >= spec.speedCardHeightDp)
    }

    @Test
    fun wideFoldable_usesTwoColumnsOnlyWhenCardsHaveRoom() {
        val spec = HaiResponsivePolicy.resolve(600, 960)
        assertEquals(HaiSizeClass.LARGE, spec.sizeClass)
        assertTrue(spec.twoColumn)
        assertEquals(spec.networkCardHeightDp, spec.speedCardHeightDp)
        assertTrue(spec.networkCardHeightDp >= 248)
    }

    @Test
    fun exactSplitBoundary_isDeterministic() {
        assertFalse(HaiResponsivePolicy.resolve(HaiResponsivePolicy.TWO_COLUMN_MIN_WIDTH_DP - 1, 900).twoColumn)
        assertTrue(HaiResponsivePolicy.resolve(HaiResponsivePolicy.TWO_COLUMN_MIN_WIDTH_DP, 900).twoColumn)
    }

    @Test
    fun veryLargeDisplay_capsScalingInsteadOfOversizingContent() {
        val spec = HaiResponsivePolicy.resolve(720, 1280)
        assertEquals(HaiSizeClass.LARGE, spec.sizeClass)
        assertTrue(spec.twoColumn)
        assertTrue(spec.scale <= 1.18f)
        assertTrue(spec.textScale <= 1.14f)
    }
}
