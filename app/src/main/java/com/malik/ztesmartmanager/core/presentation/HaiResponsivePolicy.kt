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
 * Geometry contract for the HAI reference dashboard.
 *
 * The supplied artwork is a tall portrait composition. 430x932dp remains our phone reference for
 * typography/vertical rhythm, but the reference's two-column desktop-like split is only used when
 * there is enough real width for both cards. Narrow phones reflow instead of shrinking text or
 * allowing the speed gauge/table to overflow their cards.
 *
 * This is intentionally deterministic so preview/golden-style checks can exercise the exact same
 * breakpoints as production.
 */
object HaiResponsivePolicy {
    const val REFERENCE_WIDTH_DP = 430
    const val REFERENCE_HEIGHT_DP = 932
    const val TWO_COLUMN_MIN_WIDTH_DP = 520

    fun resolve(widthDp: Int, heightDp: Int): HaiLayoutSpec {
        require(widthDp > 0)
        require(heightDp > 0)

        val widthScale = (widthDp / REFERENCE_WIDTH_DP.toFloat()).coerceIn(0.82f, 1.18f)
        val heightScale = (heightDp / REFERENCE_HEIGHT_DP.toFloat()).coerceIn(0.84f, 1.18f)

        // Width drives the look more than height. Bounds prevent unreadable text on small phones
        // and oversized cards on large/foldable displays.
        val scale = (widthScale * 0.74f + heightScale * 0.26f).coerceIn(0.84f, 1.16f)
        val textScale = (widthScale * 0.82f + heightScale * 0.18f).coerceIn(0.92f, 1.10f)

        val sizeClass = when {
            widthDp < 370 -> HaiSizeClass.COMPACT
            widthDp >= 480 -> HaiSizeClass.LARGE
            else -> HaiSizeClass.STANDARD
        }
        val denseHeader = heightDp < 780 || widthDp < 370
        val twoColumn = widthDp >= TWO_COLUMN_MIN_WIDTH_DP

        val horizontalPadding = when (sizeClass) {
            HaiSizeClass.COMPACT -> 10
            HaiSizeClass.STANDARD -> 12
            HaiSizeClass.LARGE -> 16
        }
        val sectionGap = when {
            widthDp < 360 || heightDp < 720 -> 7
            denseHeader -> 8
            else -> 10
        }

        // In single-column phone mode each hero card gets enough vertical room for the same visual
        // hierarchy as the reference. In split mode the pair shares one calibrated height.
        val heroHeight = when {
            twoColumn && heightDp < 820 -> 230
            twoColumn -> 250
            widthDp < 370 -> 238
            heightDp < 820 -> 232
            else -> 246
        }

        return HaiLayoutSpec(
            sizeClass = sizeClass,
            scale = scale,
            textScale = textScale,
            twoColumn = twoColumn,
            denseHeader = denseHeader,
            horizontalPaddingDp = horizontalPadding,
            sectionGapDp = sectionGap,
            cardRadiusDp = when (sizeClass) {
                HaiSizeClass.COMPACT -> 20
                HaiSizeClass.STANDARD -> 24
                HaiSizeClass.LARGE -> 26
            },
            networkCardHeightDp = heroHeight,
            speedCardHeightDp = when {
                twoColumn -> heroHeight
                widthDp < 370 -> 214
                heightDp < 820 -> 214
                else -> 226
            },
            mapCardHeightDp = when {
                denseHeader -> 202
                sizeClass == HaiSizeClass.LARGE -> 224
                else -> 214
            },
            signalCardHeightDp = when {
                denseHeader -> 164
                sizeClass == HaiSizeClass.LARGE -> 184
                else -> 174
            },
            bottomBarHeightDp = when {
                heightDp < 720 -> 72
                denseHeader -> 78
                else -> 84
            }
        )
    }
}
