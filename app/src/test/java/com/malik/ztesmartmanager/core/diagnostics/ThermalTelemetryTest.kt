package com.malik.ztesmartmanager.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalTelemetryTest {
    @Test
    fun parsesOnlyDirectPlausibleCelsiusEvidence() {
        val telemetry = ThermalTelemetryParser.parse(
            mapOf(
                "pm_modem_5g" to "71",
                "pm_sensor_5g" to "72.5°C",
                "pm_sensor_mdm" to "73℃",
                "pm_sensor_ambient" to "74",
                "pm_sensor_pa1" to "75",
                "wifi_chip_temp" to "60"
            )
        )

        assertEquals(6, telemetry.readings.size)
        assertEquals(75.0, telemetry.highestObserved?.celsius ?: 0.0, 0.0)
        assertTrue(ThermalTelemetryFormatter.summary(telemetry).contains("5G Modem 71°C"))
        assertTrue(ThermalTelemetryFormatter.summary(telemetry).contains("Wi‑Fi 60°C"))
    }

    @Test
    fun missingInvalidAndOutOfRangeValuesStayUnknown() {
        val telemetry = ThermalTelemetryParser.parse(
            mapOf(
                "pm_modem_5g" to "",
                "pm_sensor_5g" to "unknown",
                "pm_sensor_mdm" to "73000",
                "pm_sensor_ambient" to "126",
                "pm_sensor_pa1" to "-41",
                "wifi_chip_temp" to "60/61"
            )
        )

        assertFalse(telemetry.hasAnyEvidence)
        assertNull(telemetry.highestObserved)
    }

    @Test
    fun parserNeverGuessesScaleForLargeNumbers() {
        assertNull(ThermalTelemetryParser.parseCelsius("730"))
        assertNull(ThermalTelemetryParser.parseCelsius("73000"))
        assertEquals(73.0, ThermalTelemetryParser.parseCelsius("73") ?: 0.0, 0.0)
    }

    @Test
    fun formatterPreservesDecimalPrecisionWhenPresent() {
        assertEquals("71°C", ThermalTelemetryFormatter.celsius(71.0))
        assertEquals("71.5°C", ThermalTelemetryFormatter.celsius(71.5))
        assertEquals("—", ThermalTelemetryFormatter.celsius(null))
    }
}
