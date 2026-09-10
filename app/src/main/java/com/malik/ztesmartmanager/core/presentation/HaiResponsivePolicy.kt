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
 * عقد قياسات واجهة HAI المتجاوبة.
 *
 * المرجع 430x932dp، لكن الهاتف لا يُعامل كنسخة Desktop مصغرة. النص لا ينكمش تحت
 * المقياس الأساسي مطلقًا؛ عند ضيق العرض تعيد المكونات ترتيب نفسها بدل تصغير الخط.
 */
object HaiResponsivePolicy {
    const val REFERENCE_WIDTH_DP = 430
    const val REFERENCE_HEIGHT_DP = 932
    const val TWO_COLUMN_MIN_WIDTH_DP = 600

    fun resolve(widthDp: Int, heightDp: Int): HaiLayoutSpec {
        require(widthDp > 0)
        require(heightDp > 0)

        val widthScale = (widthDp / REFERENCE_WIDTH_DP.toFloat()).coerceIn(0.84f, 1.20f)
        val heightScale = (heightDp / REFERENCE_HEIGHT_DP.toFloat()).coerceIn(0.86f, 1.20f)

        val scale = (widthScale * 0.72f + heightScale * 0.28f).coerceIn(0.88f, 1.18f)
        // قاعدة جديدة: لا تصغير للنص تحت حجمه المصمم. المساحة تُحل بإعادة التدفق.
        val textScale = (widthScale * 0.80f + heightScale * 0.20f).coerceIn(1.00f, 1.14f)

        val sizeClass = when {
            widthDp < 370 -> HaiSizeClass.COMPACT
            widthDp >= 480 -> HaiSizeClass.LARGE
            else -> HaiSizeClass.STANDARD
        }
        val denseHeader = heightDp < 760 || widthDp < 360
        val twoColumn = widthDp >= TWO_COLUMN_MIN_WIDTH_DP

        val horizontalPadding = when (sizeClass) {
            HaiSizeClass.COMPACT -> 12
            HaiSizeClass.STANDARD -> 16
            HaiSizeClass.LARGE -> 20
        }
        val sectionGap = when {
            widthDp < 350 || heightDp < 700 -> 12
            denseHeader -> 14
            else -> 16
        }

        val heroHeight = when {
            twoColumn && heightDp < 820 -> 248
            twoColumn -> 270
            widthDp < 370 -> 255
            heightDp < 820 -> 260
            else -> 270
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
                HaiSizeClass.COMPACT -> 24
                HaiSizeClass.STANDARD -> 28
                HaiSizeClass.LARGE -> 30
            },
            networkCardHeightDp = heroHeight,
            speedCardHeightDp = when {
                twoColumn -> heroHeight
                widthDp < 370 -> 250
                heightDp < 820 -> 252
                else -> 264
            },
            mapCardHeightDp = when {
                denseHeader -> 230
                sizeClass == HaiSizeClass.LARGE -> 260
                else -> 246
            },
            signalCardHeightDp = when {
                denseHeader -> 180
                sizeClass == HaiSizeClass.LARGE -> 205
                else -> 192
            },
            bottomBarHeightDp = when {
                heightDp < 700 -> 76
                denseHeader -> 80
                else -> 84
            }
        )
    }
}
