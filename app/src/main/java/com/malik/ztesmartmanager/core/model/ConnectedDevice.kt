package com.malik.ztesmartmanager.core.model

import org.json.JSONArray
import org.json.JSONObject

enum class DeviceTransport { WIFI, LAN }

data class ConnectedDevice(
    val hostname: String?,
    val ipAddress: String?,
    val macAddress: String,
    val transport: DeviceTransport,
    val ssidIndex: String? = null
) {
    val displayName: String
        get() = hostname?.takeIf { it.isNotBlank() && !it.equals("unknown", true) }
            ?: when {
                transport == DeviceTransport.LAN -> "جهاز LAN"
                else -> "جهاز Wi‑Fi"
            }
}

/**
 * Parser for ZTE station_list / lan_station_list.
 * Firmware variants return these surfaces as a JSONArray, a JSON-encoded String, an empty String,
 * or occasionally as an object wrapping an array. Unknown shapes fail closed to an empty list.
 */
object ConnectedDeviceParser {
    fun parse(raw: JSONObject, field: String, transport: DeviceTransport): List<ConnectedDevice> {
        if (!raw.has(field) || raw.isNull(field)) return emptyList()
        return parseValue(raw.opt(field), transport)
    }

    fun parseValue(value: Any?, transport: DeviceTransport): List<ConnectedDevice> {
        val array = when (value) {
            is JSONArray -> value
            is JSONObject -> firstArray(value)
            is String -> parseString(value)
            else -> null
        } ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val mac = first(item, "mac_addr", "mac", "MAC", "macAddress")
                    ?.uppercase()
                    ?.takeIf { it.isNotBlank() }
                    ?: continue
                add(
                    ConnectedDevice(
                        hostname = first(item, "hostname", "host_name", "name", "device_name"),
                        ipAddress = first(item, "ip_addr", "ip", "ipAddress", "ipv4"),
                        macAddress = mac,
                        transport = transport,
                        ssidIndex = first(item, "ssid_index", "ssidIndex")
                    )
                )
            }
        }.distinctBy { it.macAddress }
    }

    fun merge(wifi: List<ConnectedDevice>, lan: List<ConnectedDevice>): List<ConnectedDevice> =
        (wifi + lan).distinctBy { it.macAddress }.sortedWith(
            compareBy<ConnectedDevice> { it.transport != DeviceTransport.WIFI }
                .thenBy { it.displayName.lowercase() }
                .thenBy { it.macAddress }
        )

    private fun parseString(raw: String): JSONArray? {
        val value = raw.trim()
        if (value.isBlank() || value == "[]" || value == "\"\"") return JSONArray()
        return runCatching {
            when {
                value.startsWith("[") -> JSONArray(value)
                value.startsWith("{") -> firstArray(JSONObject(value))
                else -> null
            }
        }.getOrNull()
    }

    private fun firstArray(json: JSONObject): JSONArray? {
        val candidates = listOf("station_list", "lan_station_list", "devices", "data", "list")
        for (key in candidates) {
            val direct = json.optJSONArray(key)
            if (direct != null) return direct
            val encoded = json.optString(key).trim()
            if (encoded.startsWith("[")) return runCatching { JSONArray(encoded) }.getOrNull()
        }
        return null
    }

    private fun first(json: JSONObject, vararg keys: String): String? = keys
        .asSequence()
        .map { json.optString(it).trim() }
        .firstOrNull { it.isNotBlank() && it != "---" && !it.equals("null", true) }
}
