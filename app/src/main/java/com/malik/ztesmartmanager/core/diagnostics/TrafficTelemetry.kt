package com.malik.ztesmartmanager.core.diagnostics

import java.util.Locale

data class TrafficTelemetry(
    val rxBytesPerSecond: Long?,
    val txBytesPerSecond: Long?,
    val sessionRxBytes: Long?,
    val sessionTxBytes: Long?,
    val sessionSeconds: Long?,
    val monthlyRxBytes: Long?,
    val monthlyTxBytes: Long?,
    val monthlySeconds: Long?,
    val monthMarker: String?
) {
    val hasAnyEvidence: Boolean
        get() = listOf(
            rxBytesPerSecond,
            txBytesPerSecond,
            sessionRxBytes,
            sessionTxBytes,
            sessionSeconds,
            monthlyRxBytes,
            monthlyTxBytes,
            monthlySeconds
        ).any { it != null }
}

object TrafficTelemetryParser {
    fun parse(raw: Map<String, String>): TrafficTelemetry = TrafficTelemetry(
        rxBytesPerSecond = nonNegativeLong(raw, "realtime_rx_thrpt"),
        txBytesPerSecond = nonNegativeLong(raw, "realtime_tx_thrpt"),
        sessionRxBytes = nonNegativeLong(raw, "realtime_rx_bytes", "wan_curr_rx_bytes"),
        sessionTxBytes = nonNegativeLong(raw, "realtime_tx_bytes", "wan_curr_tx_bytes"),
        sessionSeconds = nonNegativeLong(raw, "realtime_time", "wan_curr_conn_time"),
        monthlyRxBytes = nonNegativeLong(raw, "monthly_rx_bytes"),
        monthlyTxBytes = nonNegativeLong(raw, "monthly_tx_bytes"),
        monthlySeconds = nonNegativeLong(raw, "monthly_time"),
        monthMarker = raw["date_month"]?.trim()?.takeIf { it.isNotEmpty() }
    )

    private fun nonNegativeLong(raw: Map<String, String>, vararg keys: String): Long? {
        for (key in keys) {
            val parsed = raw[key]?.trim()?.toLongOrNull()
            if (parsed != null && parsed >= 0L) return parsed
        }
        return null
    }
}

object TrafficTelemetryFormatter {
    fun rateMbps(bytesPerSecond: Long?): String {
        if (bytesPerSecond == null) return "—"
        val mbps = bytesPerSecond.toDouble() * 8.0 / 1_000_000.0
        return when {
            mbps >= 100.0 -> String.format(Locale.US, "%.0f", mbps)
            mbps >= 10.0 -> String.format(Locale.US, "%.1f", mbps)
            else -> String.format(Locale.US, "%.2f", mbps)
        }
    }

    fun bytes(value: Long?): String {
        if (value == null) return "—"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var scaled = value.toDouble()
        var unit = 0
        while (scaled >= 1024.0 && unit < units.lastIndex) {
            scaled /= 1024.0
            unit++
        }
        val number = when {
            unit == 0 -> String.format(Locale.US, "%.0f", scaled)
            scaled >= 100.0 -> String.format(Locale.US, "%.0f", scaled)
            scaled >= 10.0 -> String.format(Locale.US, "%.1f", scaled)
            else -> String.format(Locale.US, "%.2f", scaled)
        }
        return "$number ${units[unit]}"
    }

    fun duration(seconds: Long?): String {
        if (seconds == null) return "—"
        val days = seconds / 86_400
        val hours = (seconds % 86_400) / 3_600
        val minutes = (seconds % 3_600) / 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
