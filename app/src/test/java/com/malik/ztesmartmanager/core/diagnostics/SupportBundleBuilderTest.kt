package com.malik.ztesmartmanager.core.diagnostics

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportBundleBuilderTest {
    @Test
    fun history_isStrictlyBounded() {
        val history = SafeTelemetryHistory(capacity = 5)
        repeat(7) { index -> history.add(snapshot(), timestampEpochMs = index.toLong()) }

        val samples = history.snapshot()
        assertEquals(5, samples.size)
        assertEquals(2L, samples.first().timestampEpochMs)
        assertEquals(6L, samples.last().timestampEpochMs)
    }

    @Test
    fun supportBundle_capsHistoryAndIncludesObservedReadOnlyTelemetry() {
        val samples = (0 until 45).map { index ->
            SafeTelemetrySampleFactory.from(snapshot(), timestampEpochMs = index.toLong())
        }

        val bundle = SupportBundleBuilder.build(
            appVersion = "test",
            modelFamily = "MC801A",
            snapshot = snapshot(),
            runtime = null,
            history = samples,
            generatedAtEpochMs = 100L
        )
        val root = JSONObject(bundle)
        val array = root.getJSONArray("telemetry_history")

        assertEquals(3, root.getInt("schema_version"))
        assertEquals(30, array.length())
        assertEquals(15L, array.getJSONObject(0).getLong("timestamp_epoch_ms"))
        assertEquals(44L, array.getJSONObject(29).getLong("timestamp_epoch_ms"))
        assertTrue(root.getJSONObject("live").getBoolean("nr_verified"))
        assertTrue(root.getJSONObject("live").getBoolean("ca_verified"))

        val traffic = root.getJSONObject("traffic")
        assertEquals(1250000L, traffic.getLong("rx_bytes_per_second"))
        assertEquals(250000L, traffic.getLong("tx_bytes_per_second"))
        assertEquals(7340032000L, traffic.getLong("monthly_rx_bytes"))
        assertEquals(1048576000L, traffic.getLong("monthly_tx_bytes"))

        val thermal = root.getJSONObject("thermal")
        val readings = thermal.getJSONArray("readings")
        assertEquals(3, readings.length())
        assertEquals("PA1", thermal.getJSONObject("highest_observed").getString("label"))
        assertEquals(74.0, thermal.getJSONObject("highest_observed").getDouble("celsius"), 0.0)
        assertEquals("raw sensor readings only; no safety threshold inferred", thermal.getString("interpretation"))
    }

    @Test
    fun supportBundle_omitsOptionalTelemetryWhenRouterExposesNoEvidence() {
        val noOptionalTelemetry = snapshot().copy(
            raw = mapOf(
                "_zte_nr_active_verified" to "true",
                "_zte_ca_verified" to "true"
            )
        )

        val root = JSONObject(
            SupportBundleBuilder.build(
                appVersion = "test",
                modelFamily = "MC801A",
                snapshot = noOptionalTelemetry,
                runtime = null,
                history = emptyList(),
                generatedAtEpochMs = 100L
            )
        )

        assertFalse(root.has("traffic"))
        assertFalse(root.has("thermal"))
    }

    private fun snapshot() = RouterSnapshot(
        model = "MC801A",
        firmware = "BD_TEST",
        hardwareVersion = "HW1",
        networkType = "5G NSA",
        lteRsrp = -91.0,
        lteRsrq = -10.0,
        lteSinr = 13.0,
        nrRsrp = -86.0,
        nrSinr = 18.0,
        lteBand = "B3",
        nrBand = "N78",
        pci = 123,
        earfcn = 1300,
        caActive = true,
        raw = mapOf(
            "_zte_nr_active_verified" to "true",
            "_zte_ca_verified" to "true",
            "realtime_rx_thrpt" to "1250000",
            "realtime_tx_thrpt" to "250000",
            "realtime_rx_bytes" to "314572800",
            "realtime_tx_bytes" to "52428800",
            "realtime_time" to "3600",
            "monthly_rx_bytes" to "7340032000",
            "monthly_tx_bytes" to "1048576000",
            "pm_modem_5g" to "71",
            "pm_sensor_mdm" to "73",
            "pm_sensor_pa1" to "74"
        )
    )
}
