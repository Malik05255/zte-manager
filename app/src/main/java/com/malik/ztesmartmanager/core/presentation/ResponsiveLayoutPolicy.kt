package com.malik.ztesmartmanager.core.presentation

data class ResponsiveLayoutProfile(
    val compact: Boolean,
    val ultraCompact: Boolean,
    val capabilityOuterVerticalDp: Int,
    val capabilityInnerVerticalDp: Int,
    val trafficInnerVerticalDp: Int,
    val signalInnerVerticalDp: Int,
    val signalPlotHeightDp: Int,
    val detailsMaxHeightDp: Int
)

object ResponsiveLayoutPolicy {
    fun resolve(widthDp: Int, heightDp: Int): ResponsiveLayoutProfile {
        val ultraCompact = widthDp < 330 || heightDp < 620
        val compact = ultraCompact || widthDp < 360 || heightDp < 720

        return when {
            ultraCompact -> ResponsiveLayoutProfile(
                compact = true,
                ultraCompact = true,
                capabilityOuterVerticalDp = 2,
                capabilityInnerVerticalDp = 4,
                trafficInnerVerticalDp = 3,
                signalInnerVerticalDp = 4,
                signalPlotHeightDp = 52,
                detailsMaxHeightDp = 140
            )
            compact -> ResponsiveLayoutProfile(
                compact = true,
                ultraCompact = false,
                capabilityOuterVerticalDp = 3,
                capabilityInnerVerticalDp = 5,
                trafficInnerVerticalDp = 4,
                signalInnerVerticalDp = 5,
                signalPlotHeightDp = 68,
                detailsMaxHeightDp = 185
            )
            else -> ResponsiveLayoutProfile(
                compact = false,
                ultraCompact = false,
                capabilityOuterVerticalDp = 6,
                capabilityInnerVerticalDp = 7,
                trafficInnerVerticalDp = 7,
                signalInnerVerticalDp = 8,
                signalPlotHeightDp = 92,
                detailsMaxHeightDp = 245
            )
        }
    }
}
