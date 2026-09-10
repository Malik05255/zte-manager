package com.malik.ztesmartmanager.core.storage

import com.malik.ztesmartmanager.core.model.RouterSettingsBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterSettingsBackupCodecTest {
    @Test
    fun roundTrip_preservesExactValuesAndBlankState() {
        val source = RouterSettingsBackup(
            createdAtEpochMs = 123456789L,
            routerAddress = "192.168.0.1",
            profileId = "zte-mc801a",
            modelFamily = "MC801A / MC801A1",
            model = "MC801A",
            firmware = "MC801A_TEST",
            hardwareVersion = "HW1",
            lteBandLock = "0xA3E2AB0908DF",
            nrSaBandLock = "",
            nrNsaBandLock = "78",
            ltePciLock = "0",
            lteEarfcnLock = "",
            bearerPreference = "LTE_AND_5G"
        )

        val decoded = RouterSettingsBackupCodec.decodeHistory(
            RouterSettingsBackupCodec.encodeHistory(listOf(source))
        ).single()

        assertEquals(source, decoded)
        assertEquals("", decoded.nrSaBandLock)
        assertEquals("", decoded.lteEarfcnLock)
    }

    @Test
    fun roundTrip_keepsUnavailableFieldAsNull() {
        val source = RouterSettingsBackup(
            createdAtEpochMs = 1L,
            routerAddress = "192.168.0.1",
            profileId = "zte-mc801a",
            modelFamily = "MC801A / MC801A1",
            lteBandLock = null,
            nrSaBandLock = "0"
        )

        val decoded = RouterSettingsBackupCodec.decodeHistory(
            RouterSettingsBackupCodec.encodeHistory(listOf(source))
        ).single()

        assertNull(decoded.lteBandLock)
        assertEquals("0", decoded.nrSaBandLock)
        assertTrue(decoded.hasAnyRestorableRadioSetting)
    }

    @Test
    fun corruptHistory_returnsEmptyInsteadOfCrashing() {
        assertTrue(RouterSettingsBackupCodec.decodeHistory("not-json").isEmpty())
    }
}
