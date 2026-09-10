package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.RuntimeCapabilityEvidence
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeCapabilityAccessTest {
    @Test
    fun writeActionsRequireSafeToAttempt() {
        val report = report(
            lte = RuntimeCapabilityState.SAFE_TO_ATTEMPT,
            nr = RuntimeCapabilityState.READ_ONLY,
            cell = RuntimeCapabilityState.PROFILE_ONLY,
            mode = RuntimeCapabilityState.UNAVAILABLE
        )

        assertTrue(RuntimeCapabilityAccess.decide(report, RuntimeAction.LTE_BAND_WRITE).allowed)
        assertFalse(RuntimeCapabilityAccess.decide(report, RuntimeAction.NR_BAND_WRITE).allowed)
        assertFalse(RuntimeCapabilityAccess.decide(report, RuntimeAction.CELL_LOCK_WRITE).allowed)
        assertFalse(RuntimeCapabilityAccess.decide(report, RuntimeAction.NETWORK_MODE_WRITE).allowed)
    }

    @Test
    fun readActionsAllowReadOnlyEvidence() {
        val report = report(
            neighbor = RuntimeCapabilityState.READ_ONLY,
            ca = RuntimeCapabilityState.READ_ONLY
        )

        val scan = RuntimeCapabilityAccess.decide(report, RuntimeAction.NEIGHBOR_SCAN)
        val ca = RuntimeCapabilityAccess.decide(report, RuntimeAction.CA_READ)

        assertTrue(scan.allowed)
        assertTrue(scan.readOnly)
        assertTrue(ca.allowed)
        assertTrue(ca.readOnly)
    }

    @Test
    fun antennaProfileOnlyNeverBecomesWritable() {
        val report = report(antenna = RuntimeCapabilityState.PROFILE_ONLY)
        assertFalse(RuntimeCapabilityAccess.decide(report, RuntimeAction.ANTENNA_WRITE).allowed)
    }

    @Test
    fun writeReadyCountCountsOnlyFourVerifiedWriteSurfaces() {
        val report = report(
            lte = RuntimeCapabilityState.SAFE_TO_ATTEMPT,
            nr = RuntimeCapabilityState.SAFE_TO_ATTEMPT,
            cell = RuntimeCapabilityState.READ_ONLY,
            mode = RuntimeCapabilityState.SAFE_TO_ATTEMPT,
            antenna = RuntimeCapabilityState.SAFE_TO_ATTEMPT
        )
        assertTrue(RuntimeCapabilityAccess.writeReadyCount(report) == 3)
        assertTrue(RuntimeCapabilityAccess.writeActionCount() == 4)
    }

    private fun report(
        lte: RuntimeCapabilityState = RuntimeCapabilityState.UNAVAILABLE,
        nr: RuntimeCapabilityState = RuntimeCapabilityState.UNAVAILABLE,
        cell: RuntimeCapabilityState = RuntimeCapabilityState.UNAVAILABLE,
        mode: RuntimeCapabilityState = RuntimeCapabilityState.UNAVAILABLE,
        neighbor: RuntimeCapabilityState = RuntimeCapabilityState.UNAVAILABLE,
        antenna: RuntimeCapabilityState = RuntimeCapabilityState.UNAVAILABLE,
        ca: RuntimeCapabilityState = RuntimeCapabilityState.UNAVAILABLE
    ) = RuntimeCapabilityReport(
        lteBandControl = evidence(lte),
        nrBandControl = evidence(nr),
        cellLock = evidence(cell),
        networkMode = evidence(mode),
        neighborScan = evidence(neighbor),
        antennaControl = evidence(antenna),
        carrierAggregationTelemetry = evidence(ca),
        probedAtEpochMs = 1L
    )

    private fun evidence(state: RuntimeCapabilityState) = RuntimeCapabilityEvidence(
        state = state,
        reason = state.name,
        evidenceFields = if (state == RuntimeCapabilityState.UNAVAILABLE) emptySet() else setOf("field")
    )
}
