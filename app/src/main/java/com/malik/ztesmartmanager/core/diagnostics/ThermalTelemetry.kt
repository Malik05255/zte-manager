package com.malik.ztesmartmanager.core.diagnostics

import java.util.Locale

data class ThermalReading(
    val key: String,
    val label: String,
    val celsius: Double
)

data class ThermalTelemetry(
    val readings: List<ThermalReading>
) {
    val hasAnyEvidence: Boolean get() = readings.isNotEmpty()
    val highestObserved: ThermalReading? get() = readings.maxByOrNull { it.celsius }
}

/**
 * Read-only parser for ZTE MC-series thermal fields.
 *
 * Values are accepted only when the router exposes a direct Celsius-like number in the physically
 * plausible -40..125 range. We deliberately do not divide/multiply out-of-range values because
 * guessing a firmware-specific scale could turn an unknown sensor into a false temperature.
 */
object ThermalTelemetryParser {
    private data class Field(val key: String, val label: String)

    private val fields = listOf(
        Field("pm_modem_5g", "5G Modem"),
        Field("pm_sensor_5g", "5G RF"),
        Field("pm_sensor_mdm", "Modem"),
        Field("pm_sensor_ambient", "Ambient"),
        Field("pm_sensor_pa1", "PA1"),
        Field("wifi_chip_temp", "Wi‑Fi")
    )

    fun parse(raw: Map<String, String>): ThermalTelemetry {
        val readings = fields.mapNotNull { field ->
            val value = parseCelsius(raw[field.key]) ?: return@mapNotNull null
            ThermalReading(field.key, field.label, value)
        }
        return ThermalTelemetry(readings)
    }

    internal fun parseCelsius(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
            .replace("℃", "", ignoreCase = true)
            .replace("°C", "", ignoreCase = true)
            .trim()
        if (!DIRECT_NUMBER.matches(cleaned)) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (!value.isFinite() || value !in MIN_CELSIUS..MAX_CELSIUS) return null
        return value
    }

    private val DIRECT_NUMBER = Regex("^[+-]?\\d+(?:\\.\\d+)?$")
    private const val MIN_CELSIUS = -40.0
    private const val MAX_CELSIUS = 125.0
}

object ThermalTelemetryFormatter {
    fun celsius(value: Double?): String = value?.let {
        if (it % 1.0 == 0.0) String.format(Locale.US, "%.0f°C", it)
        else String.format(Locale.US, "%.1f°C", it)
    } ?: "—"

    fun summary(telemetry: ThermalTelemetry): String = telemetry.readings.joinToString(" • ") {
        "${it.label} ${celsius(it.celsius)}"
    }
}
