package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeCapabilityProbeTest {
    private val capabilities = RouterCapabilities(
        modelFamily = "MC801A test",
        supportsLteBandLock = true,
        supportsNrBandLock = true,
        supportsCellLock = true,
        supportsCarrierAggregationRead = true,
        supportsAntennaControl = true,
        supportedLteBands = setOf(1, 3, 7),
        supportedNrBands = setOf(78)
    )

    @Test
    fun fullRollbackAndReadbackSurfacesAreSafeToAttempt() {
        val raw = JSONObject()
            .put("lte_band_lock", "0x45")
            .put("nr5g_nsa_band_lock", "78")
            .put("nr5g_sa_band_lock", "78")
            .put("lte_pci_lock", "")
            .put("lte_earfcn_lock", "")
            .put("BearerPreference", "LTE_AND_5G")
            .put("neighbor_cell_info", "[]")
            .put("wan_lte_ca", "0")

        val report = RuntimeCapabilityProbe.evaluate(capabilities, raw)

        assertEquals(RuntimeCapabilityState.SAFE_TO_ATTEMPT, report.lteBandControl.state)
        assertEquals(RuntimeCapabilityState.SAFE_TO_ATTEMPT, report.nrBandControl.state)
        assertEquals(RuntimeCapabilityState.SAFE_TO_ATTEMPT, report.cellLock.state)
        assertEquals(RuntimeCapabilityState.SAFE_TO_ATTEMPT, report.networkMode.state)
        assertEquals(RuntimeCapabilityState.READ_ONLY, report.neighborScan.state)
        assertEquals(RuntimeCapabilityState.READ_ONLY, report.carrierAggregationTelemetry.state)
        assertEquals(RuntimeCapabilityState.PROFILE_ONLY, report.antennaControl.state)
        assertEquals(4, report.safeWriteCount)
    }

    @Test
    fun blankBandMasksDoNotBecomeSafeWrites() {
        val raw = JSONObject()
            .put("lte_band_lock", "")
            .put("nr5g_band_lock", "0")

        val report = RuntimeCapabilityProbe.evaluate(capabilities, raw)

        assertEquals(RuntimeCapabilityState.READ_ONLY, report.lteBandControl.state)
        assertEquals(RuntimeCapabilityState.READ_ONLY, report.nrBandControl.state)
        assertFalse(report.lteBandControl.canAttemptWrite)
        assertFalse(report.nrBandControl.canAttemptWrite)
    }

    @Test
    fun conflictingSaAndNsaMasksAreNotSafeToRestore() {
        val raw = JSONObject()
            .put("nr5g_sa_band_lock", "78")
            .put("nr5g_nsa_band_lock", "41,78")

        val report = RuntimeCapabilityProbe.evaluate(capabilities, raw)

        assertEquals(RuntimeCapabilityState.READ_ONLY, report.nrBandControl.state)
        assertFalse(report.nrBandControl.canAttemptWrite)
    }

    @Test
    fun unlockedCellPairIsACompleteRestorableState() {
        val raw = JSONObject()
            .put("lte_pci_lock", "0")
            .put("lte_earfcn_lock", "0x0")

        val report = RuntimeCapabilityProbe.evaluate(capabilities, raw)

        assertEquals(RuntimeCapabilityState.SAFE_TO_ATTEMPT, report.cellLock.state)
        assertTrue(report.cellLock.canAttemptWrite)
    }

    @Test
    fun partialOrMalformedCellStateIsReadOnly() {
        val partial = RuntimeCapabilityProbe.evaluate(
            capabilities,
            JSONObject().put("lte_pci_lock", "100")
        )
        val malformed = RuntimeCapabilityProbe.evaluate(
            capabilities,
            JSONObject().put("lte_pci_lock", "9999").put("lte_earfcn_lock", "1300")
        )

        assertEquals(RuntimeCapabilityState.READ_ONLY, partial.cellLock.state)
        assertEquals(RuntimeCapabilityState.READ_ONLY, malformed.cellLock.state)
    }

    @Test
    fun missingRuntimeFieldsNeverPromoteProfileSupportToSafeWrite() {
        val report = RuntimeCapabilityProbe.evaluate(capabilities, JSONObject())

        assertEquals(RuntimeCapabilityState.PROFILE_ONLY, report.lteBandControl.state)
        assertEquals(RuntimeCapabilityState.PROFILE_ONLY, report.nrBandControl.state)
        assertEquals(RuntimeCapabilityState.PROFILE_ONLY, report.cellLock.state)
        assertEquals(RuntimeCapabilityState.UNAVAILABLE, report.networkMode.state)
        assertEquals(RuntimeCapabilityState.UNAVAILABLE, report.neighborScan.state)
        assertEquals(0, report.safeWriteCount)
    }
}
