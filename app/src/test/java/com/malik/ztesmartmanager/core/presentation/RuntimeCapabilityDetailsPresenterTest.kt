package com.malik.ztesmartmanager.core.presentation

import com.malik.ztesmartmanager.core.model.RuntimeCapabilityEvidence
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeCapabilityDetailsPresenterTest {
    @Test
    fun detailsKeepStableOrderAndTruthfulAccessLabels() {
        val details = RuntimeCapabilityDetailsPresenter.from(report())
        assertEquals(listOf("lte", "nr", "cell", "mode", "scan", "antenna", "ca"), details.map { it.key })
        assertEquals("جاهز للمحاولة", details.first { it.key == "lte" }.stateLabel)
        assertEquals("قراءة فقط", details.first { it.key == "scan" }.stateLabel)
        assertEquals("كتابة مشروطة", details.first { it.key == "mode" }.accessKind)
        assertEquals("قراءة", details.first { it.key == "ca" }.accessKind)
    }

    @Test
    fun detailsNeverExposeSensitiveEvidenceFieldNames() {
        val risky = RuntimeCapabilityEvidence(
            RuntimeCapabilityState.SAFE_TO_ATTEMPT,
            "test",
            setOf("lte_band_lock", "admin_password", "auth_token", "modem_imei", "sim_pin_code")
        )
        val details = RuntimeCapabilityDetailsPresenter.from(report(lte = risky))
        val fields = details.first { it.key == "lte" }.evidenceFields
        assertTrue("lte_band_lock" in fields)
        assertFalse(fields.any { "password" in it.lowercase() })
        assertFalse(fields.any { "token" in it.lowercase() })
        assertFalse(fields.any { "imei" in it.lowercase() })
        assertFalse(fields.any { "pin" in it.lowercase() })
    }

    private fun report(
        lte: RuntimeCapabilityEvidence = evidence(RuntimeCapabilityState.SAFE_TO_ATTEMPT, "lte_band_lock"),
        nr: RuntimeCapabilityEvidence = evidence(RuntimeCapabilityState.READ_ONLY, "nr5g_sa_band_lock"),
        cell: RuntimeCapabilityEvidence = evidence(RuntimeCapabilityState.SAFE_TO_ATTEMPT, "lte_pci_lock"),
        mode: RuntimeCapabilityEvidence = evidence(RuntimeCapabilityState.SAFE_TO_ATTEMPT, "BearerPreference"),
        scan: RuntimeCapabilityEvidence = evidence(RuntimeCapabilityState.READ_ONLY, "neighbor_cell_info"),
        antenna: RuntimeCapabilityEvidence = evidence(RuntimeCapabilityState.PROFILE_ONLY),
        ca: RuntimeCapabilityEvidence = evidence(RuntimeCapabilityState.READ_ONLY, "wan_lte_ca")
    ) = RuntimeCapabilityReport(
        lteBandControl = lte,
        nrBandControl = nr,
        cellLock = cell,
        networkMode = mode,
        neighborScan = scan,
        antennaControl = antenna,
        carrierAggregationTelemetry = ca,
        probedAtEpochMs = 1L
    )

    private fun evidence(state: RuntimeCapabilityState, vararg fields: String) = RuntimeCapabilityEvidence(
        state = state,
        reason = state.name,
        evidenceFields = fields.toSet()
    )
}
