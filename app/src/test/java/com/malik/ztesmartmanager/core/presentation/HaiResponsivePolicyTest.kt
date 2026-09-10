package com.malik.ztesmartmanager.core.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class HaiResponsivePolicyTest {
    @Test
    fun compactPhone_keepsBoundedReadableScaleAndStacksColumns() {
        val spec = HaiResponsivePolicy.resolve(340, 720)
        assertEquals(HaiSizeClass.COMPACT, spec.sizeClass)
        assertFalse(spec.twoColumn)
        assertTrue(spec.denseHeader)
        assertTrue(spec.textScale >= 0.90f)
        assertTrue(spec.scale >= 0.84f)
    }

    @Test
    fun referencePhone_usesTwoColumnReferenceComposition() {
        val spec = HaiResponsivePolicy.resolve(430, 932)
        assertEquals(HaiSizeClass.STANDARD, spec.sizeClass)
        assertTrue(spec.twoColumn)
        assertFalse(spec.denseHeader)
        assertTrue(spec.scale in 0.99f..1.01f)
        assertTrue(spec.textScale in 0.99f..1.01f)
    }

    @Test
    fun largePhone_capsScalingInsteadOfOversizingContent() {
        val spec = HaiResponsivePolicy.resolve(520, 1100)
        assertEquals(HaiSizeClass.LARGE, spec.sizeClass)
        assertTrue(spec.twoColumn)
        assertTrue(spec.scale <= 1.16f)
        assertTrue(spec.textScale <= 1.12f)
    }
}
