package com.malik.ztesmartmanager.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStabilityAnalyzerTest {
    @Test
    fun fewerThanFiveSamples_isInsufficient() {
        val report = ConnectionStabilityAnalyzer.analyze(
            List(4) { sample(timestamp = it.toLong()) }
        )

        assertEquals(ConnectionStabilityLevel.INSUFFICIENT, report.level)
        assertNull(report.score)
    }

    @Test
    fun repeatedCellModeAndSignal_isStable() {
        val rsrp = listOf(-90.0, -90.5, -91.0, -90.0, -89.5, -90.0)
        val report = ConnectionStabilityAnalyzer.analyze(
            rsrp.mapIndexed { index, value -> sample(index.toLong(), lteRsrp = value) }
        )

        assertEquals(ConnectionStabilityLevel.STABLE, report.level)
        assertEquals(100, report.cellStabilityPercent)
        assertEquals(100, report.modeStabilityPercent)
        assertTrue((report.score ?: 0) >= 90)
    }

    @Test
    fun alternatingCellAndMode_isUnstable() {
        val samples = listOf(
            sample(0, pci = 10, earfcn = 100, mode = "4G", lteRsrp = -80.0),
            sample(1, pci = 20, earfcn = 200, mode = "5G NSA", lteRsrp = -105.0),
            sample(2, pci = 10, earfcn = 100, mode = "4G", lteRsrp = -82.0),
            sample(3, pci = 20, earfcn = 200, mode = "5G NSA", lteRsrp = -107.0),
            sample(4, pci = 10, earfcn = 100, mode = "4G", lteRsrp = -81.0),
            sample(5, pci = 20, earfcn = 200, mode = "5G NSA", lteRsrp = -106.0)
        )
        val report = ConnectionStabilityAnalyzer.analyze(samples)

        assertEquals(ConnectionStabilityLevel.UNSTABLE, report.level)
        assertTrue(report.cellSwitches >= 5)
        assertTrue(report.modeSwitches >= 5)
        assertTrue((report.score ?: 100) < 65)
    }

    @Test
    fun missingRfMetrics_doesNotInventSignalStability() {
        val report = ConnectionStabilityAnalyzer.analyze(
            List(6) { sample(timestamp = it.toLong(), lteRsrp = null) }
        )

        assertNull(report.signalStabilityPercent)
        assertEquals(100, report.cellStabilityPercent)
        assertEquals(100, report.modeStabilityPercent)
    }

    @Test
    fun caPresenceUsesOnlyVerifiedCaSamples() {
        val samples = listOf(
            sample(0, caVerified = true, caActive = true),
            sample(1, caVerified = true, caActive = false),
            sample(2, caVerified = true, caActive = true),
            sample(3, caVerified = false, caActive = true),
            sample(4, caVerified = false, caActive = true),
            sample(5, caVerified = false, caActive = true)
        )
        val report = ConnectionStabilityAnalyzer.analyze(samples)

        assertEquals(67, report.caActivePercent)
    }

    private fun sample(
        timestamp: Long,
        pci: Int? = 10,
        earfcn: Int? = 100,
        mode: String? = "4G",
        lteRsrp: Double? = -90.0,
        caVerified: Boolean = true,
        caActive: Boolean = false
    ) = SafeTelemetrySample(
        timestampEpochMs = timestamp,
        networkType = mode,
        lteBand = "B3",
        nrBand = null,
        lteRsrp = lteRsrp,
        lteRsrq = -10.0,
        lteSinr = 12.0,
        nrRsrp = null,
        nrSinr = null,
        pci = pci,
        earfcn = earfcn,
        caActive = caActive,
        carrierCount = 1,
        nrVerified = false,
        caVerified = caVerified
    )
}
