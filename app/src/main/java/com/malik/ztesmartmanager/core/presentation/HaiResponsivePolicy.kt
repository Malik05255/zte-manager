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
 * الهاتف يعتمد العرض أولًا بدل خلط العرض مع الارتفاع. الشاشات الطويلة مثل Honor 200 لا تكبّر
 * البطاقات أو الخطوط لمجرد أن الارتفاع أكبر؛ وهذا يحافظ على نفس الإحساس الهندسي بين الصفحات.
 */
object HaiResponsivePolicy {
    const val REFERENCE_WIDTH_DP = 393
    const val REFERENCE_HEIGHT_DP = 852
    const val TWO_COLUMN_MIN_WIDTH_DP = 600

    fun resolve(widthDp: Int, heightDp: Int): HaiLayoutSpec {
        require(widthDp > 0)
        require(heightDp > 0)

        val widthScale = (widthDp / REFERENCE_WIDTH_DP.toFloat()).coerceIn(0.90f, 1.12f)
        // Height may make a page scroll, but it must never inflate its geometry.
        val scale = widthScale.coerceIn(0.94f, 1.10f)
        val textScale = widthScale.coerceIn(1.00f, 1.08f)

        val sizeClass = when {
            widthDp < 370 -> HaiSizeClass.COMPACT
            widthDp >= 480 -> HaiSizeClass.LARGE
            else -> HaiSizeClass.STANDARD
        }
        val denseHeader = heightDp < 700 || widthDp < 350
        val twoColumn = widthDp >= TWO_COLUMN_MIN_WIDTH_DP

        val horizontalPadding = when (sizeClass) {
            HaiSizeClass.COMPACT -> 10
            HaiSizeClass.STANDARD -> 12
            HaiSizeClass.LARGE -> 18
        }
        val sectionGap = when (sizeClass) {
            HaiSizeClass.COMPACT -> 5
            HaiSizeClass.STANDARD -> 6
            HaiSizeClass.LARGE -> 10
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
                HaiSizeClass.COMPACT -> 16
                HaiSizeClass.STANDARD -> 18
                HaiSizeClass.LARGE -> 20
            },
            networkCardHeightDp = when {
                twoColumn -> 220
                sizeClass == HaiSizeClass.COMPACT -> 148
                else -> 156
            },
            speedCardHeightDp = when {
                twoColumn -> 220
                sizeClass == HaiSizeClass.COMPACT -> 126
                else -> 132
            },
            mapCardHeightDp = when {
                sizeClass == HaiSizeClass.LARGE -> 260
                else -> 235
            },
            signalCardHeightDp = when {
                sizeClass == HaiSizeClass.LARGE -> 190
                else -> 170
            },
            bottomBarHeightDp = when (sizeClass) {
                HaiSizeClass.LARGE -> 74
                else -> 70
            }
        )
    }
}
