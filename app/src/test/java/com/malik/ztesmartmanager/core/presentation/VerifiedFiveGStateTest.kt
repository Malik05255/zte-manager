package com.malik.ztesmartmanager.core.presentation

import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedFiveGStateTest {
    @Test
    fun verifiedNsa_exposesNrAndLteAnchor() {
        val snapshot = RouterSnapshot(
            nrBand = "N78",
            nrRsrp = -91.0,
            nrSinr = 18.0,
            lteBand = "B3",
            pci = 101,
            earfcn = 1650,
            cells = listOf(CarrierCell(CellRole.NR, "N78", 321, 640000, 100.0)),
            raw = mapOf(
                "_zte_nr_active_verified" to "true",
                "_zte_lte_active_verified" to "true",
                "_zte_radio_mode" to "NSA_ACTIVE_VERIFIED"
            )
        )

        val state = VerifiedFiveGPresenter.from(snapshot)

        assertTrue(state.verified)
        assertEquals(VerifiedFiveGMode.NSA, state.mode)
        assertEquals("5G NSA", state.headline)
        assertEquals("N78", state.band)
        assertEquals(321, state.pci)
        assertEquals(640000, state.arfcn)
        assertEquals("B3", state.lteAnchorBand)
        assertEquals(101, state.lteAnchorPci)
        assertEquals(1650, state.lteAnchorArfcn)
    }

    @Test
    fun verifiedSa_doesNotInventLteAnchor() {
        val snapshot = RouterSnapshot(
            nrBand = "N78",
            nrRsrp = -88.0,
            cells = listOf(CarrierCell(CellRole.NR, "N78", 44, 636666, null)),
            raw = mapOf(
                "_zte_nr_active_verified" to "true",
                "_zte_lte_active_verified" to "true",
                "_zte_radio_mode" to "SA_ACTIVE_VERIFIED"
            )
        )

        val state = VerifiedFiveGPresenter.from(snapshot)

        assertEquals(VerifiedFiveGMode.SA, state.mode)
        assertEquals("5G SA", state.headline)
        assertNull(state.lteAnchorBand)
        assertNull(state.lteAnchorPci)
        assertNull(state.lteAnchorArfcn)
    }

    @Test
    fun staleNrFields_withoutVerifiedFlag_areHidden() {
        val snapshot = RouterSnapshot(
            nrBand = "N78",
            nrRsrp = -75.0,
            nrSinr = 25.0,
            cells = listOf(CarrierCell(CellRole.NR, "N78", 500, 640000, null)),
            raw = mapOf(
                "_zte_nr_active_verified" to "false",
                "_zte_nr_structural_evidence" to "true",
                "_zte_nr_signal_evidence" to "true",
                "_zte_radio_mode" to "UNKNOWN"
            )
        )

        val state = VerifiedFiveGPresenter.from(snapshot)

        assertFalse(state.verified)
        assertEquals(VerifiedFiveGMode.NOT_VERIFIED, state.mode)
        assertEquals("5G غير مُثبت", state.headline)
        assertNull(state.band)
        assertNull(state.pci)
        assertNull(state.arfcn)
        assertNull(state.rsrp)
        assertNull(state.sinr)
    }

    @Test
    fun explicit5gState_withoutCarrierEvidence_remainsUnverified() {
        val snapshot = RouterSnapshot(
            raw = mapOf(
                "_zte_nr_active_verified" to "false",
                "_zte_nr_explicit_state" to "true",
                "_zte_nr_structural_evidence" to "false",
                "_zte_nr_signal_evidence" to "false",
                "_zte_radio_mode" to "NR_STATE_UNVERIFIED"
            )
        )

        val state = VerifiedFiveGPresenter.from(snapshot)

        assertFalse(state.verified)
        assertTrue(state.evidenceMessage.contains("هوية الـCarrier غير مكتملة"))
    }
}
