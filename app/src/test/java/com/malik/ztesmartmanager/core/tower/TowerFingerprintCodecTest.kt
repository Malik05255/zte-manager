package com.malik.ztesmartmanager.core.tower

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TowerFingerprintCodecTest {
    @Test
    fun roundTrip_preservesRadioIdentityAndEvidence() {
        val original = TowerFingerprint(
            createdAtEpochMs = 123456789L,
            routerAddress = "192.168.0.1",
            profileId = "mc801a",
            pci = 100,
            earfcn = 1650,
            band = "B3",
            cellId = 0x12345L,
            enodebId = "291",
            evidenceScore = 82,
            presencePercent = 100,
            medianRsrp = -88.5
        )

        val decoded = TowerFingerprintCodec.decodeHistory(
            TowerFingerprintCodec.encodeHistory(listOf(original))
        ).single()

        assertEquals(original, decoded)
    }

    @Test
    fun invalidLtePci_isRejectedDuringDecode() {
        val json = JSONObject().apply {
            put("createdAtEpochMs", 1L)
            put("routerAddress", "192.168.0.1")
            put("profileId", "mc801a")
            put("pci", 900)
            put("earfcn", 1650)
        }
        val text = JSONArray().put(json).toString()

        val decoded = TowerFingerprintCodec.decodeHistory(text)

        assertTrue(decoded.isEmpty())
    }
}
