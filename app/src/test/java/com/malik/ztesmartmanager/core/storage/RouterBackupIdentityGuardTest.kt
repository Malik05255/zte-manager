package com.malik.ztesmartmanager.core.storage

import com.malik.ztesmartmanager.core.model.RouterSettingsBackup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterBackupIdentityGuardTest {
    @Test
    fun exactIdentityAllowsRestore() {
        val result = RouterBackupIdentityGuard.verify(
            backup = backup(),
            currentRouterAddress = "http://192.168.0.1/",
            currentProfileId = "zte-mc801a",
            currentModel = "MC801A",
            currentHardwareVersion = "HW1",
            currentFirmware = "BD_TEST"
        )

        assertTrue(result.allowed)
        assertTrue(result.matchedEvidence.containsAll(setOf("router_address", "profile_id", "model", "hardware", "firmware")))
    }

    @Test
    fun firmwareMismatchBlocksRestore() {
        val result = RouterBackupIdentityGuard.verify(
            backup = backup(),
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-mc801a",
            currentModel = "MC801A",
            currentHardwareVersion = "HW1",
            currentFirmware = "BD_NEW"
        )

        assertFalse(result.allowed)
        assertTrue(result.reason.contains("Firmware"))
    }

    @Test
    fun sameProfileAtDifferentRouterAddressIsBlocked() {
        val result = RouterBackupIdentityGuard.verify(
            backup = backup(),
            currentRouterAddress = "192.168.1.1",
            currentProfileId = "zte-mc801a",
            currentModel = "MC801A",
            currentHardwareVersion = "HW1",
            currentFirmware = "BD_TEST"
        )

        assertFalse(result.allowed)
    }

    @Test
    fun capturedIdentityMissingFromCurrentRouterIsBlocked() {
        val result = RouterBackupIdentityGuard.verify(
            backup = backup(),
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-mc801a",
            currentModel = "MC801A",
            currentHardwareVersion = null,
            currentFirmware = "BD_TEST"
        )

        assertFalse(result.allowed)
        assertTrue(result.reason.contains("Hardware"))
    }

    @Test
    fun profileAndAddressAloneNeverProveBackupIdentity() {
        val oldBackup = backup().copy(model = null, hardwareVersion = null, firmware = null)
        val result = RouterBackupIdentityGuard.verify(
            backup = oldBackup,
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-mc801a",
            currentModel = "MC801A",
            currentHardwareVersion = "HW1",
            currentFirmware = "BD_TEST"
        )

        assertFalse(result.allowed)
        assertTrue(result.reason.contains("هوية جهاز كافية"))
    }

    private fun backup() = RouterSettingsBackup(
        createdAtEpochMs = 1L,
        routerAddress = "192.168.0.1",
        profileId = "zte-mc801a",
        modelFamily = "MC801A / MC801A1",
        model = "MC801A",
        firmware = "BD_TEST",
        hardwareVersion = "HW1",
        lteBandLock = "3"
    )
}
