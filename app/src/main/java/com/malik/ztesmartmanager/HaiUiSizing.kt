package com.malik.ztesmartmanager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One sizing contract for the production application.
 *
 * Phones are rendered against a virtual 393dp design width. This keeps card geometry, spacing,
 * radii and typography stable across Honor 200 and other tall Android phones instead of letting
 * every screen derive a different scale from its height. Tablets keep their native density.
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
    pagePadding = 14.dp,
    sectionGap = 8.dp,
    cardRadius = 20.dp,
    smallRadius = 14.dp,
    headerHeight = 72.dp,
    bottomNavHeight = 78.dp,
    chromeButtonSize = 36.dp,
    minimumTouchTarget = 48.dp,
    mapHeight = 260.dp
)

val LocalHaiUiMetrics = staticCompositionLocalOf { DefaultHaiUiMetrics }

@Composable
fun HaiUiScaleProvider(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val parentDensity = LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp.coerceAtLeast(1)
    val phone = screenWidthDp < 600

    // Width-only normalization is deliberate. Tall screens must not inflate cards or typography.
    val densityFactor = if (phone) {
        (screenWidthDp / PHONE_DESIGN_WIDTH_DP).coerceIn(0.90f, 1.10f)
    } else {
        1f
    }
    val controlledFontScale = parentDensity.fontScale.coerceIn(1.00f, 1.15f)
    val controlledDensity = remember(parentDensity.density, controlledFontScale, densityFactor) {
        Density(
            density = parentDensity.density * densityFactor,
            fontScale = controlledFontScale
        )
    }

    val effectiveWidthDp = if (phone) PHONE_DESIGN_WIDTH_DP.toInt() else screenWidthDp
    val metrics = remember(phone, effectiveWidthDp) {
        if (phone) {
            DefaultHaiUiMetrics.copy(compact = effectiveWidthDp < 370)
        } else {
            HaiUiMetrics(
                phone = false,
                compact = false,
                pagePadding = 18.dp,
                sectionGap = 10.dp,
                cardRadius = 20.dp,
                smallRadius = 14.dp,
                headerHeight = 70.dp,
                bottomNavHeight = 74.dp,
                chromeButtonSize = 36.dp,
                minimumTouchTarget = 48.dp,
                mapHeight = 260.dp
            )
        }
    }

    CompositionLocalProvider(
        LocalDensity provides controlledDensity,
        LocalHaiUiMetrics provides metrics,
        content = content
    )
}

const val PHONE_DESIGN_WIDTH_DP = 393f
