package com.malik.ztesmartmanager.core.presentation

/** Pure policy used by the login screen before adopting Android's detected Wi-Fi gateway. */
object RouterLoginGatewayPolicy {
    const val DEFAULT_ADDRESS = "192.168.0.1"

    fun shouldAdoptDetectedGateway(
        currentAddress: String,
        detectedGateway: String?,
        userEditedAddress: Boolean
    ): Boolean {
        if (userEditedAddress) return false
        val detected = normalizeHost(detectedGateway) ?: return false
        if (!isPrivateIpv4(detected)) return false

        val current = normalizeHost(currentAddress)
        return current.isNullOrBlank() || (current == DEFAULT_ADDRESS && current != detected)
    }

    fun normalizeHost(value: String?): String? {
        val cleaned = value
            ?.trim()
            ?.removePrefix("http://")
            ?.removePrefix("https://")
            ?.substringBefore('/')
            ?.substringBefore(':')
            ?.trim()
            .orEmpty()
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun isPrivateIpv4(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false

        return when {
            octets[0] == 10 -> true
            octets[0] == 172 && octets[1] in 16..31 -> true
            octets[0] == 192 && octets[1] == 168 -> true
            else -> false
        }
    }
}
