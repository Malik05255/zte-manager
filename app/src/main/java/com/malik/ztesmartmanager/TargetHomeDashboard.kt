package com.malik.ztesmartmanager

import androidx.compose.runtime.Composable
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance

/**
 * Stable production entry point retained for compatibility with ArabicHaiDashboardV3.
 * The actual approved home screen is rebuilt in HaiHomeDashboardV4.
 */
@Composable
fun TargetHomeDashboard(
    snapshot: RouterSnapshot,
    telemetrySamples: List<SafeTelemetrySample>,
    status: String,
    operationMessage: String,
    lastPerformance: NetworkPerformance?,
    speedBusy: Boolean,
    controlBusy: Boolean,
    smartBusy: Boolean,
    onDisconnect: () -> Unit,
    onSpeedTest: () -> Unit,
    onSetNetworkMode: (String) -> Unit,
    onNavigateNetwork: () -> Unit,
    onNavigateTowers: () -> Unit,
    onNavigateBands: () -> Unit,
    onNavigateTools: () -> Unit,
    onNavigateLogs: () -> Unit,
    onNavigateMore: () -> Unit,
    onRefreshNow: () -> Unit,
    onOptimizeNow: () -> Unit
) {
    HaiHomeDashboardV4(
        snapshot = snapshot,
        telemetrySamples = telemetrySamples,
        status = status,
        operationMessage = operationMessage,
        lastPerformance = lastPerformance,
        speedBusy = speedBusy,
        controlBusy = controlBusy,
        smartBusy = smartBusy,
        onDisconnect = onDisconnect,
        onSpeedTest = onSpeedTest,
        onSetNetworkMode = onSetNetworkMode,
        onNavigateNetwork = onNavigateNetwork,
        onNavigateTowers = onNavigateTowers,
        onNavigateBands = onNavigateBands,
        onNavigateTools = onNavigateTools,
        onNavigateLogs = onNavigateLogs,
        onNavigateMore = onNavigateMore,
        onRefreshNow = onRefreshNow,
        onOptimizeNow = onOptimizeNow
    )
}
