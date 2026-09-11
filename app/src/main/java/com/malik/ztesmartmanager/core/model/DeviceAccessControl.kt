package com.malik.ztesmartmanager.core.model

/**
 * Read-only view of ZTE Wi-Fi access-control state.
 *
 * This model deliberately does not assign semantics to ACL_mode yet. ZTE firmware families use
 * different values/modes, so seeing a MAC in a blacklist is evidence that the router stores it in
 * that list, not proof that traffic is currently blocked. Writes stay disabled until mode semantics
 * and a read-back-safe mutation path are proven for the active firmware.
 */
data class DeviceAccessControlState(
    val legacySurfaceObserved: Boolean,
    val modeRaw: String?,
    val blacklistedMacs: Set<String>,
    val whitelistedMacs: Set<String>,
    val adminIpAddress: String?,
    val blacklistRaw: String?,
    val whitelistRaw: String?
) {
    fun isListedInBlacklist(macAddress: String): Boolean =
        canonicalMac(macAddress)?.let(blacklistedMacs::contains) == true

    fun isListedInWhitelist(macAddress: String): Boolean =
        canonicalMac(macAddress)?.let(whitelistedMacs::contains) == true

    fun isCurrentAdminDevice(device: ConnectedDevice): Boolean {
        val adminIp = adminIpAddress?.trim()?.takeIf { it.isNotBlank() } ?: return false
        return device.ipAddress?.trim() == adminIp
    }

    /** Runtime read evidence exists, but this is intentionally not a write-capability flag. */
    val hasReadEvidence: Boolean get() = legacySurfaceObserved
}

object DeviceAccessControlParser {
    private val legacyKeys = setOf(
        "ACL_mode",
        "wifi_mac_black_list",
        "wifi_mac_white_list",
        "wifi_hostname_black_list",
        "wifi_hostname_white_list",
        "user_ip_addr"
    )

    private val separatedMac = Regex("(?i)(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}")

    fun parse(raw: Map<String, String>): DeviceAccessControlState {
        val blacklistRaw = raw["wifi_mac_black_list"]
        val whitelistRaw = raw["wifi_mac_white_list"]
        return DeviceAccessControlState(
            legacySurfaceObserved = legacyKeys.any(raw::containsKey),
            modeRaw = clean(raw["ACL_mode"]),
            blacklistedMacs = extractMacs(blacklistRaw),
            whitelistedMacs = extractMacs(whitelistRaw),
            adminIpAddress = clean(raw["user_ip_addr"]),
            blacklistRaw = blacklistRaw,
            whitelistRaw = whitelistRaw
        )
    }

    internal fun extractMacs(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return separatedMac.findAll(raw)
            .mapNotNull { canonicalMac(it.value) }
            .toCollection(linkedSetOf())
    }

    private fun clean(value: String?): String? = value
        ?.trim()
        ?.takeIf { it.isNotBlank() && it != "---" && !it.equals("null", true) }
}

internal fun canonicalMac(value: String?): String? {
    val hex = value.orEmpty().filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
    if (hex.length != 12) return null
    return hex.chunked(2).joinToString(":") { it.uppercase() }
}
