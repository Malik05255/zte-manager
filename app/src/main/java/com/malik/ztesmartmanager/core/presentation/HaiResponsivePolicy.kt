package com.malik.ztesmartmanager.core.presentation

enum class HaiSizeClass { COMPACT, STANDARD, LARGE }

data class HaiLayoutSpec(
    val sizeClass: HaiSizeClass,
    val scale: Float,
    val textScale: Float,
    val twoColumn: Boolean,
    val denseHeader: Boolean,
    val horizontalPaddingDp: Int,
    val sectionGapDp: Int,
    val cardRadiusDp: Int,
    val networkCardHeightDp: Int,
    val speedCardHeightDp: Int,
    val mapCardHeightDp: Int,
    val signalCardHeightDp: Int,
    val bottomBarHeightDp: Int
)

/**
 * Responsive policy for the HAI reference dashboard.
 * 430x932dp is the visual design reference. Values are bounded so small phones remain readable
 * instead of scaling typography and touch targets to unusable sizes.
 */
object HaiResponsivePolicy {
    fun resolve(widthDp: Int, heightDp: Int): HaiLayoutSpec {
        require(widthDp > 0)
        require(heightDp > 0)

        val widthScale = (widthDp / 430f).coerceIn(0.82f, 1.18f)
        val heightScale = (heightDp / 932f).coerceIn(0.84f, 1.18f)
        val scale = (widthScale * 0.72f + heightScale * 0.28f).coerceIn(0.84f, 1.16f)
        val textScale = (widthScale * 0.8f + heightScale * 0.2f).coerceIn(0.90f, 1.12f)

        val sizeClass = when {
            widthDp < 370 -> HaiSizeClass.COMPACT
            widthDp >= 460 -> HaiSizeClass.LARGE
            else -> HaiSizeClass.STANDARD
        }
        val denseHeader = heightDp < 760 || widthDp < 370
        val twoColumn = widthDp >= 390

        return HaiLayoutSpec(
            sizeClass = sizeClass,
            scale = scale,
            textScale = textScale,
            twoColumn = twoColumn,
            denseHeader = denseHeader,
            horizontalPaddingDp = when (sizeClass) {
                HaiSizeClass.COMPACT -> 10
                HaiSizeClass.STANDARD -> 12
                HaiSizeClass.LARGE -> 16
            },
            sectionGapDp = if (denseHeader) 8 else 10,
            cardRadiusDp = when (sizeClass) {
                HaiSizeClass.COMPACT -> 20
                HaiSizeClass.STANDARD -> 24
                HaiSizeClass.LARGE -> 27
            },
            networkCardHeightDp = when {
                widthDp < 370 -> 250
                heightDp < 800 -> 235
                else -> 268
            },
            speedCardHeightDp = when {
                widthDp < 370 -> 218
                heightDp < 800 -> 220
                else -> 268
            },
            mapCardHeightDp = if (denseHeader) 210 else 230,
            signalCardHeightDp = if (denseHeader) 170 else 188,
            bottomBarHeightDp = if (denseHeader) 76 else 88
        )
    }
}
