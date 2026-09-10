package com.malik.ztesmartmanager.core.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaiResponsivePolicyTest {
    @Test
    fun compactPhone_keepsBoundedReadableScaleAndStacksColumns() {
        val spec = HaiResponsivePolicy.resolve(360, 800)
        assertEquals(HaiSizeClass.COMPACT, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertTrue(spec.textScale >= 0.92f)
        assertTrue(spec.scale >= 0.84f)
        assertTrue(spec.networkCardHeightDp >= 230)
        assertTrue(spec.bottomBarHeightDp >= 72)
    }

    @Test
    fun smallModernPhone_390x844_reflowsInsteadOfCrushingReferenceColumns() {
        val spec = HaiResponsivePolicy.resolve(390, 844)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertTrue(spec.networkCardHeightDp >= 232)
        assertTrue(spec.speedCardHeightDp >= 214)
        assertTrue(spec.textScale >= 0.92f)
    }

    @Test
    fun referencePhone_430x932_preservesReferenceTypographyWithoutForcedSplit() {
        val spec = HaiResponsivePolicy.resolve(430, 932)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertFalse(spec.denseHeader)
        assertTrue(spec.scale in 0.99f..1.01f)
        assertTrue(spec.textScale in 0.99f..1.01f)
        assertEquals(12, spec.horizontalPaddingDp)
        assertEquals(10, spec.sectionGapDp)
        assertEquals(24, spec.cardRadiusDp)
        assertEquals(246, spec.networkCardHeightDp)
        assertEquals(226, spec.speedCardHeightDp)
        assertEquals(84, spec.bottomBarHeightDp)
    }

    @Test
    fun largePhone_412x915_usesSpaceButStillAvoidsUnsafeTwoColumnCompression() {
        val spec = HaiResponsivePolicy.resolve(412, 915)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertTrue(spec.textScale in 0.92f..1.10f)
        assertTrue(spec.networkCardHeightDp >= spec.speedCardHeightDp)
    }

    @Test
    fun wideFoldable_usesReferenceTwoColumnCompositionOnlyWhenCardsHaveRoom() {
        val spec = HaiResponsivePolicy.resolve(600, 960)
        assertEquals(HaiSizeClass.LARGE, spec.sizeClass)
        assertTrue(spec.twoColumn)
        assertEquals(spec.networkCardHeightDp, spec.speedCardHeightDp)
        assertTrue(spec.networkCardHeightDp >= 230)
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
        assertTrue(spec.scale <= 1.16f)
        assertTrue(spec.textScale <= 1.10f)
    }
}
