package com.malik.ztesmartmanager.core.protocol

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModeReadBackVerifierTest {
    @Test
    fun exactMode_matchesIgnoringCaseAndOuterWhitespace() {
        assertTrue(NetworkModeReadBackVerifier.matches("LTE_AND_5G", "  lte_and_5g "))
    }

    @Test
    fun missingReadBack_neverCountsAsVerified() {
        assertFalse(NetworkModeReadBackVerifier.matches("Only_5G", null))
    }

    @Test
    fun differentMode_neverCountsAsVerified() {
        assertFalse(NetworkModeReadBackVerifier.matches("Only_5G", "LTE_AND_5G"))
    }

    @Test
    fun blankRequestedMode_neverCountsAsVerified() {
        assertFalse(NetworkModeReadBackVerifier.matches("  ", "Only_LTE"))
    }
}
