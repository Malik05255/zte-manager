package com.malik.ztesmartmanager.core.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterLoginGatewayPolicyTest {
    @Test
    fun defaultAddress_adoptsDetectedPrivateGateway() {
        assertTrue(
            RouterLoginGatewayPolicy.shouldAdoptDetectedGateway(
                currentAddress = "192.168.0.1",
                detectedGateway = "192.168.1.1",
                userEditedAddress = false
            )
        )
    }

    @Test
    fun manuallyEditedAddress_isNeverOverwritten() {
        assertFalse(
            RouterLoginGatewayPolicy.shouldAdoptDetectedGateway(
                currentAddress = "192.168.0.1",
                detectedGateway = "192.168.1.1",
                userEditedAddress = true
            )
        )
    }

    @Test
    fun publicOrMalformedGateway_isRejected() {
        assertFalse(RouterLoginGatewayPolicy.shouldAdoptDetectedGateway("192.168.0.1", "8.8.8.8", false))
        assertFalse(RouterLoginGatewayPolicy.shouldAdoptDetectedGateway("192.168.0.1", "not-an-ip", false))
    }

    @Test
    fun customAddress_isNotReplaced() {
        assertFalse(
            RouterLoginGatewayPolicy.shouldAdoptDetectedGateway(
                currentAddress = "192.168.8.1",
                detectedGateway = "192.168.1.1",
                userEditedAddress = false
            )
        )
    }

    @Test
    fun httpPrefix_isNormalizedSafely() {
        assertTrue(RouterLoginGatewayPolicy.normalizeHost("http://192.168.1.1/") == "192.168.1.1")
        assertTrue(RouterLoginGatewayPolicy.normalizeHost("https://192.168.8.1:443/") == "192.168.8.1")
    }
}
