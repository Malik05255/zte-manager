package com.malik.ztesmartmanager.core.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaiResponsivePolicyTest {
    @Test
    fun compactPhone_usesReferenceRhythmWithoutHeightInflation() {
        val spec = HaiResponsivePolicy.resolve(360, 800)
        assertEquals(HaiSizeClass.COMPACT, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertTrue(spec.textScale >= 1.00f)
        assertTrue(spec.scale >= 0.94f)
        assertEquals(14, spec.horizontalPaddingDp)
        assertEquals(7, spec.sectionGapDp)
        assertEquals(20, spec.cardRadiusDp)
        assertEquals(160, spec.networkCardHeightDp)
        assertEquals(140, spec.speedCardHeightDp)
        assertEquals(78, spec.bottomBarHeightDp)
    }

    @Test
    fun smallModernPhone_390x844_keepsReferenceGeometry() {
        val spec = HaiResponsivePolicy.resolve(390, 844)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertEquals(14, spec.horizontalPaddingDp)
        assertEquals(8, spec.sectionGapDp)
        assertEquals(20, spec.cardRadiusDp)
        assertEquals(172, spec.networkCardHeightDp)
        assertEquals(150, spec.speedCardHeightDp)
        assertEquals(78, spec.bottomBarHeightDp)
        assertTrue(spec.textScale in 1.08f..1.09f)
    }

    @Test
    fun honor200LikeTallPhone_widthControlsScale_notHeight() {
        val short = HaiResponsivePolicy.resolve(393, 780)
        val tall = HaiResponsivePolicy.resolve(393, 980)

        assertEquals(short.sizeClass, tall.sizeClass)
        assertEquals(short.scale, tall.scale, 0.0001f)
        assertEquals(short.textScale, tall.textScale, 0.0001f)
        assertEquals(short.horizontalPaddingDp, tall.horizontalPaddingDp)
        assertEquals(short.sectionGapDp, tall.sectionGapDp)
        assertEquals(short.cardRadiusDp, tall.cardRadiusDp)
        assertEquals(short.networkCardHeightDp, tall.networkCardHeightDp)
        assertEquals(short.speedCardHeightDp, tall.speedCardHeightDp)
        assertEquals(short.bottomBarHeightDp, tall.bottomBarHeightDp)
    }

    @Test
    fun widerPhone_430x932_scalesWidthWithinHardCaps() {
        val spec = HaiResponsivePolicy.resolve(430, 932)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertFalse(spec.denseHeader)
        assertTrue(spec.scale in 1.09f..1.10f)
        assertEquals(1.16f, spec.textScale, 0.0001f)
        assertEquals(14, spec.horizontalPaddingDp)
        assertEquals(8, spec.sectionGapDp)
        assertEquals(20, spec.cardRadiusDp)
        assertEquals(172, spec.networkCardHeightDp)
        assertEquals(150, spec.speedCardHeightDp)
        assertEquals(78, spec.bottomBarHeightDp)
    }

    @Test
    fun largePhone_412x915_keepsCardsProportional() {
        val spec = HaiResponsivePolicy.resolve(412, 915)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertTrue(spec.textScale in 1.12f..1.14f)
        assertTrue(spec.networkCardHeightDp > spec.speedCardHeightDp)
    }

    @Test
    fun wideFoldable_usesTwoColumnsOnlyWhenCardsHaveRoom() {
        val spec = HaiResponsivePolicy.resolve(600, 960)
        assertEquals(HaiSizeClass.LARGE, spec.sizeClass)
        assertTrue(spec.twoColumn)
        assertEquals(spec.networkCardHeightDp, spec.speedCardHeightDp)
        assertEquals(220, spec.networkCardHeightDp)
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
        assertTrue(spec.scale <= 1.10f)
        assertTrue(spec.textScale <= 1.16f)
    }
}
