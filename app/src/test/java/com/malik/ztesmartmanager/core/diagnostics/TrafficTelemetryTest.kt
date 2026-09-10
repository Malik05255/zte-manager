package com.malik.ztesmartmanager.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrafficTelemetryTest {
    @Test
    fun parsesDocumentedTrafficFieldsWithoutInventingMissingValues() {
        val traffic = TrafficTelemetryParser.parse(
            mapOf(
                "realtime_rx_thrpt" to "1580",
                "realtime_tx_thrpt" to "3024",
                "realtime_rx_bytes" to "1116739479",
                "realtime_tx_bytes" to "99545339",
                "realtime_time" to "63821",
                "monthly_rx_bytes" to "23120021145",
                "monthly_tx_bytes" to "2332584668",
                "date_month" to "202609"
            )
        )

        assertTrue(traffic.hasAnyEvidence)
        assertEquals(1580L, traffic.rxBytesPerSecond)
        assertEquals(3024L, traffic.txBytesPerSecond)
        assertEquals(1116739479L, traffic.sessionRxBytes)
        assertEquals(99545339L, traffic.sessionTxBytes)
        assertEquals(63821L, traffic.sessionSeconds)
        assertEquals(23120021145L, traffic.monthlyRxBytes)
        assertEquals(2332584668L, traffic.monthlyTxBytes)
        assertEquals("202609", traffic.monthMarker)
        assertNull(traffic.monthlySeconds)
    }

    @Test
    fun supportsKnownLegacyWanCurrentFallbacks() {
        val traffic = TrafficTelemetryParser.parse(
            mapOf(
                "wan_curr_rx_bytes" to "4096",
                "wan_curr_tx_bytes" to "2048",
                "wan_curr_conn_time" to "3600"
            )
        )

        assertEquals(4096L, traffic.sessionRxBytes)
        assertEquals(2048L, traffic.sessionTxBytes)
        assertEquals(3600L, traffic.sessionSeconds)
    }

    @Test
    fun invalidBlankAndNegativeValuesRemainUnknown() {
        val traffic = TrafficTelemetryParser.parse(
            mapOf(
                "realtime_rx_thrpt" to "",
                "realtime_tx_thrpt" to "not-a-number",
                "monthly_rx_bytes" to "-1"
            )
        )

        assertFalse(traffic.hasAnyEvidence)
        assertNull(traffic.rxBytesPerSecond)
        assertNull(traffic.txBytesPerSecond)
        assertNull(traffic.monthlyRxBytes)
    }

    @Test
    fun formatterUsesBytesPerSecondAsMegabitsPerSecond() {
        assertEquals("8.00", TrafficTelemetryFormatter.rateMbps(1_000_000L))
        assertEquals("1.00 GB", TrafficTelemetryFormatter.bytes(1024L * 1024L * 1024L))
        assertEquals("1h 1m", TrafficTelemetryFormatter.duration(3660L))
        assertEquals("—", TrafficTelemetryFormatter.rateMbps(null))
    }
}
