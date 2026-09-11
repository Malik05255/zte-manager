package com.malik.ztesmartmanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Production sizing contract.
 *
 * Never override Android density or fontScale. The previous width-normalized Density changed the
 * meaning of dp/sp for the entire composition and could produce clipping, inconsistent card sizes,
 * and different geometry between pages on tall phones such as HONOR 200. We now keep the platform
 * density untouched and adapt only explicit layout metrics.
 */
@Immutable
data class HaiUiMetrics(
    val phone: Boolean,
    val compact: Boolean,
    val pagePadding: Dp,
    val sectionGap: Dp,
    val cardRadius: Dp,
    val smallRadius: Dp,
    val headerHeight: Dp,
    val bottomNavHeight: Dp,
    val chromeButtonSize: Dp,
    val minimumTouchTarget: Dp,
    val mapHeight: Dp
)

private val DefaultHaiUiMetrics = HaiUiMetrics(
    phone = true,
    compact = false,
    pagePadding = 15.dp,
    sectionGap = 10.dp,
    cardRadius = 20.dp,
    smallRadius = 14.dp,
    headerHeight = 72.dp,
    bottomNavHeight = 72.dp,
    chromeButtonSize = 38.dp,
    minimumTouchTarget = 48.dp,
    mapHeight = 270.dp
)

val LocalHaiUiMetrics = staticCompositionLocalOf { DefaultHaiUiMetrics }

@Composable
fun HaiUiScaleProvider(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.coerceAtLeast(1)
    val phone = width < 600

    val metrics = remember(width, phone) {
        when {
            !phone -> HaiUiMetrics(
                phone = false,
                compact = false,
                pagePadding = 22.dp,
                sectionGap = 14.dp,
                cardRadius = 22.dp,
                smallRadius = 15.dp,
                headerHeight = 76.dp,
                bottomNavHeight = 74.dp,
                chromeButtonSize = 40.dp,
                minimumTouchTarget = 50.dp,
                mapHeight = 320.dp
            )
            width < 360 -> DefaultHaiUiMetrics.copy(
                compact = true,
                pagePadding = 12.dp,
                sectionGap = 8.dp,
                cardRadius = 18.dp,
                headerHeight = 68.dp,
                bottomNavHeight = 70.dp,
                chromeButtonSize = 36.dp,
                mapHeight = 245.dp
            )
            width >= 420 -> DefaultHaiUiMetrics.copy(
                pagePadding = 18.dp,
                sectionGap = 12.dp,
                cardRadius = 22.dp,
                headerHeight = 76.dp,
                bottomNavHeight = 74.dp,
                chromeButtonSize = 40.dp,
                mapHeight = 290.dp
            )
            else -> DefaultHaiUiMetrics
        }
    }

    CompositionLocalProvider(LocalHaiUiMetrics provides metrics, content = content)
}

const val PHONE_DESIGN_WIDTH_DP = 393f
