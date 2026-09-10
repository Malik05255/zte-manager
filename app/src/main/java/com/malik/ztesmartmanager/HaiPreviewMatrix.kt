package com.malik.ztesmartmanager

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.malik.ztesmartmanager.core.diagnostics.ConnectionStabilityAnalyzer
import com.malik.ztesmartmanager.core.diagnostics.SafeTelemetrySample
import com.malik.ztesmartmanager.core.diagnostics.ThermalReading
import com.malik.ztesmartmanager.core.diagnostics.ThermalTelemetry
import com.malik.ztesmartmanager.core.diagnostics.TrafficTelemetry
import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.NetworkPerformance
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import com.malik.ztesmartmanager.core.tower.CellConfidence
import com.malik.ztesmartmanager.core.tower.NearbyCell

/**
 * Visual QA matrix for the supplied HAI reference direction.
 *
 * These fixtures exist only for Compose Preview. Production never reads these values; live screens
 * still receive truth-first router data from FinalMainActivity/RuntimeAwareFinalDashboard.
 */
@Preview(name = "HAI • 360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HaiPreview360x800() = HaiReferenceFixture()

@Preview(name = "HAI • 390x844", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun HaiPreview390x844() = HaiReferenceFixture()

@Preview(name = "HAI • 412x915", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun HaiPreview412x915() = HaiReferenceFixture()

@Preview(name = "HAI • Reference 430x932", widthDp = 430, heightDp = 932, showBackground = true)
@Composable
private fun HaiPreview430x932() = HaiReferenceFixture()

@Preview(name = "HAI • Wide 600x960", widthDp = 600, heightDp = 960, showBackground = true)
@Composable
private fun HaiPreview600x960() = HaiReferenceFixture()

@Composable
private fun HaiReferenceFixture() {
    val samples = previewSamples()
    val snapshot = previewSnapshot()

    HaiAdaptiveDashboard(
        snapshot = snapshot,
        capabilities = RouterCapabilities(
            modelFamily = "ZTE MC801A",
            supportsLteBandLock = true,
            supportsNrBandLock = true,
            supportsCellLock = true,
            supportsCarrierAggregationRead = true,
            supportsAntennaControl = false,
            supportedLteBands = setOf(1, 3, 5, 7, 8, 20, 28, 38, 40),
            supportedNrBands = setOf(1, 28, 77, 78)
        ),
        runtime = null,
        traffic = TrafficTelemetry(
            rxBytesPerSecond = 31_125_000,
            txBytesPerSecond = 9_375_000,
            sessionRxBytes = 3_221_225_472,
            sessionTxBytes = 811_597_824,
            sessionSeconds = 4_380,
            monthlyRxBytes = 93_415_473_152,
            monthlyTxBytes = 15_716_466_688,
            monthlySeconds = null,
            monthMarker = "09"
        ),
        thermal = ThermalTelemetry(
            readings = listOf(
                ThermalReading("pm_sensor_mdm", "Modem", 51.0),
                ThermalReading("pm_sensor_5g", "5G RF", 54.0)
            )
        ),
        telemetrySamples = samples,
        stability = ConnectionStabilityAnalyzer.analyze(samples),
        status = "Connected • verified live radio",
        operationMessage = "",
        placementMode = false,
        placementReading = null,
        smartMode = false,
        smartGoal = OptimizationGoal.BALANCED,
        smartBusy = false,
        smartReport = null,
        selectedLte = setOf(1, 3, 8),
        selectedNr = setOf(78),
        controlBusy = false,
        speedBusy = false,
        lastPerformance = NetworkPerformance(
            latencyMs = 18.0,
            jitterMs = 2.3,
            packetLossPercent = 0.0,
            downloadMbps = 249.0
        ),
        safetyBackupAvailable = true,
        nearbyCells = previewNearbyCells(),
        scanBusy = false,
        towerTarget = null,
        towerGuardEnabled = false,
        towerGuardStatus = null,
        onDisconnect = {},
        onSpeedTest = {},
        onPlacementToggle = {},
        onSmartModeChange = {},
        onSmartGoalChange = {},
        onOptimizeNow = {},
        onLteToggle = {},
        onNrToggle = {},
        onApplyLte = {},
        onApplyNr = {},
        onSetNetworkMode = {},
        onAntennaState = {},
        onScanCells = {},
        onLockCurrentCell = {},
        onLockNearbyCell = {},
        onClearCellLock = {},
        onTowerGuardChange = {},
        onRestoreSafetyBackup = {},
        onCopyDiagnostics = {},
        onShareDiagnostics = {}
    )
}

private fun previewSnapshot() = RouterSnapshot(
    model = "MC801A",
    firmware = "preview-only",
    hardwareVersion = "preview-only",
    networkType = "5G NSA",
    operatorCode = null,
    lteRsrp = -88.0,
    lteRsrq = -11.5,
    lteRssi = -61.0,
    lteSinr = 16.0,
    nrRsrp = -85.0,
    nrSinr = 18.4,
    lteBand = "B3",
    nrBand = "N78",
    pci = 232,
    earfcn = 1300,
    cellId = 42083,
    caActive = true,
    cells = listOf(
        CarrierCell(CellRole.PRIMARY, "B3", 232, 1300, 20.0),
        CarrierCell(CellRole.SECONDARY, "B1", 113, 300, 20.0),
        CarrierCell(CellRole.NR, "N78", 419, 620000, 100.0)
    ),
    raw = mapOf(
        "_zte_nr_active_verified" to "true",
        "_zte_lte_active_verified" to "true",
        "_zte_ca_verified" to "true"
    )
)

private fun previewNearbyCells() = listOf(
    NearbyCell("LTE", "B3", 232, 1300, -88.0, -11.5, 16.0, 5, 5, 100, 96, 91, CellConfidence.HIGH),
    NearbyCell("LTE", "B1", 113, 300, -93.0, -12.0, 12.5, 5, 5, 100, 89, 82, CellConfidence.HIGH),
    NearbyCell("LTE", "B8", 77, 3450, -99.0, -14.0, 8.0, 4, 5, 80, 81, 71, CellConfidence.MEDIUM),
    NearbyCell("NR", "N78", 419, 620000, -85.0, -10.0, 18.4, 5, 5, 100, 94, 93, CellConfidence.HIGH)
)

private fun previewSamples(): List<SafeTelemetrySample> = List(12) { index ->
    SafeTelemetrySample(
        timestampEpochMs = index * 2_000L,
        networkType = "5G NSA",
        lteBand = "B3",
        nrBand = "N78",
        lteRsrp = -90.0 + (index % 3),
        lteRsrq = -11.5,
        lteSinr = 15.0 + (index % 2),
        nrRsrp = -88.0 + (index % 5) * 0.8,
        nrSinr = 16.5 + (index % 4) * 0.6,
        pci = 232,
        earfcn = 1300,
        caActive = true,
        carrierCount = 3,
        nrVerified = true,
        caVerified = true
    )
}
