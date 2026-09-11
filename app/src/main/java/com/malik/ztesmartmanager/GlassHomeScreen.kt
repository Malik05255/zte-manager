package com.malik.ztesmartmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityReport
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.ConnectedDevice
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.PlacementReading

@Composable
internal fun GlassHomeScreen(
    snapshot: RouterSnapshot,
    traffic: TrafficTelemetry?,
    telemetrySamples: List<SafeTelemetrySample>,
    stability: ConnectionStabilityReport,
    performance: NetworkPerformance?,
    speedBusy: Boolean,
    placementMode: Boolean,
    placementReading: PlacementReading?,
    controlBusy: Boolean,
    devices: List<ConnectedDevice>,
    operationMessage: String,
    status: String,
    onSpeedTest: () -> Unit,
    onPlacementToggle: () -> Unit,
    onMode: (String) -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenMore: () -> Unit
) {
    val message = operationMessage.ifBlank { status.takeIf { it.isNotBlank() && it != "متصل" }.orEmpty() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { GlassHeroSection(snapshot) }
        item { GlassPlacementCard(placementMode, placementReading, onPlacementToggle) }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 620.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            GlassSpeedTestCard(performance, speedBusy, onSpeedTest)
                            GlassMapCard(snapshot, onOpenNetwork)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            GlassNetworkModeCard(snapshot, controlBusy, onMode)
                            GlassFrequencySummaryCard(snapshot, onOpenNetwork)
                            GlassSignalMonitorCard(telemetrySamples, stability)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        GlassSpeedTestCard(performance, speedBusy, onSpeedTest)
                        GlassMapCard(snapshot, onOpenNetwork)
                        GlassNetworkModeCard(snapshot, controlBusy, onMode)
                        GlassFrequencySummaryCard(snapshot, onOpenNetwork)
                        GlassSignalMonitorCard(telemetrySamples, stability)
                    }
                }
            }
        }
        item { GlassDeviceInfoCard(snapshot, traffic, devices.size, onOpenMore) }
        if (message.isNotBlank()) item { GlassNotice(message) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
