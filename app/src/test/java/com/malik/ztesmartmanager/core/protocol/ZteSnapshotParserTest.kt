package com.malik.ztesmartmanager.core.protocol

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZteSnapshotParserTest {

    @Test
    fun endcWithCompleteNrEvidence_isVerified5gNsa() {
        val snapshot = parse(
            "network_type" to "ENDC",
            "lte_ca_pcell_band" to "20",
            "lte_pci" to "1E6",
            "wan_active_channel" to "6300",
            "lte_rsrp" to "-87",
            "lte_rsrq" to "-12",
            "lte_snr" to "8.5",
            "nr5g_action_band" to "n78",
            "nr5g_action_channel" to "640000",
            "nr5g_pci" to "C7",
            "Z5g_rsrp" to "-101",
            "Z5g_SINR" to "14.0",
            "wan_lte_ca" to "ca_deactivated"
        )

        assertEquals("5G NSA / 4G", snapshot.networkType)
        assertEquals(486, snapshot.pci)
        assertEquals("N78", snapshot.nrBand)
        assertEquals("true", snapshot.raw["_zte_nr_active_verified"])
        assertNotNull(snapshot.nrRsrp)
    }

    @Test
    fun lteNsaWithStaleNrFields_doesNotClaim5g() {
        val snapshot = parse(
            "network_type" to "LTE-NSA",
            "lte_ca_pcell_band" to "3",
            "lte_pci" to "64",
            "wan_active_channel" to "1300",
            "lte_rsrp" to "-91",
            "nr5g_action_band" to "n78",
            "nr5g_action_channel" to "640000",
            "nr5g_pci" to "C7",
            "Z5g_rsrp" to "-95"
        )

        assertEquals("4G", snapshot.networkType)
        assertEquals(100, snapshot.pci)
        assertFalse(snapshot.raw["_zte_nr_active_verified"].toBoolean())
        assertEquals(null, snapshot.nrBand)
    }

    @Test
    fun configurationLikeStringContaining5g_isNotTreatedAsLiveRat() {
        val snapshot = parse(
            "network_type" to "LTE_AND_5G",
            "nr5g_action_band" to "n78",
            "nr5g_action_channel" to "640000",
            "nr5g_pci" to "C7",
            "Z5g_rsrp" to "-90"
        )

        assertEquals("غير مؤكد", snapshot.networkType)
        assertFalse(snapshot.raw["_zte_nr_active_verified"].toBoolean())
        assertEquals("UNKNOWN", snapshot.raw["_zte_radio_mode"])
    }

    @Test
    fun caActiveFlagWithoutCompleteSecondaryCarrier_doesNotClaimCa() {
        val snapshot = parse(
            "network_type" to "LTE",
            "lte_ca_pcell_band" to "20",
            "lte_pci" to "1E6",
            "wan_active_channel" to "6300",
            "lte_rsrp" to "-87",
            "wan_lte_ca" to "ca_activated"
        )

        assertEquals("4G", snapshot.networkType)
        assertFalse(snapshot.caActive)
        assertEquals("false", snapshot.raw["_zte_ca_verified"])
        assertEquals("false", snapshot.raw["_zte_ca_secondary_evidence"])
    }

    @Test
    fun caActiveWithCompleteSecondaryCarrier_isVerified() {
        val snapshot = parse(
            "network_type" to "LTE",
            "lte_ca_pcell_band" to "20",
            "lte_pci" to "1E6",
            "wan_active_channel" to "6300",
            "lte_rsrp" to "-87",
            "wan_lte_ca" to "ca_activated",
            "lte_multi_ca_scell_info" to "1,64,2,3,1525,15.0;"
        )

        assertEquals("4G+", snapshot.networkType)
        assertTrue(snapshot.caActive)
        assertEquals("true", snapshot.raw["_zte_ca_verified"])
        assertEquals(2, snapshot.cells.size)
        assertEquals("B3", snapshot.cells.last().band)
        assertEquals(100, snapshot.cells.last().pci)
        assertEquals(1525, snapshot.cells.last().arfcn)
    }

    @Test
    fun conflictingCaFlags_areUnverified() {
        val snapshot = parse(
            "network_type" to "LTE",
            "lte_ca_pcell_band" to "20",
            "lte_pci" to "1E6",
            "wan_active_channel" to "6300",
            "lte_rsrp" to "-87",
            "wan_lte_ca" to "ca_activated",
            "lte_ca_scell_ca_activated" to "0",
            "lte_multi_ca_scell_info" to "1,64,2,3,1525,15.0;"
        )

        assertFalse(snapshot.caActive)
        assertEquals("false", snapshot.raw["_zte_ca_verified"])
        assertEquals("true", snapshot.raw["_zte_ca_state_conflict"])
    }

    private fun parse(vararg values: Pair<String, String>) =
        ZteSnapshotParser.parse(JSONObject(values.toMap()))
}
