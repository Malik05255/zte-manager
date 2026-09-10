package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.profile.RadioIdEncoding

/**
 * Decodes ZTE radio identifiers without assuming that digit-only values are always hex or decimal.
 * Channel numbers/ARFCNs are deliberately outside this parser and remain decimal channel fields.
 */
object ZteRadioIdParser {
    fun parseInt(value: String?, max: Int, encoding: RadioIdEncoding): Int? =
        parseLong(value, max.toLong(), encoding)?.toInt()

    fun parseLong(value: String?, max: Long = Long.MAX_VALUE, encoding: RadioIdEncoding): Long? {
        if (max < 0) return null
        val original = clean(value) ?: return null
        val explicitHex = original.startsWith("0x", true)
        val token = original.removePrefix("0x").removePrefix("0X")
        if (token.isBlank() || token.startsWith('-')) return null
        if (!token.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null

        val hasHexLetters = token.any { it.lowercaseChar() in 'a'..'f' }
        val hex = token.toLongOrNull(16)?.takeIf { it in 0..max }
        val decimal = token.toLongOrNull(10)?.takeIf { it in 0..max }

        if (explicitHex || hasHexLetters) return hex

        return when (encoding) {
            RadioIdEncoding.HEX_FIRST -> hex ?: decimal
            RadioIdEncoding.DECIMAL_FIRST -> decimal ?: hex
            RadioIdEncoding.SAFE_AUTO -> when {
                decimal == null -> hex
                hex == null -> decimal
                decimal == hex -> decimal
                else -> null // digit-only token is valid in both radices but means two values.
            }
        }
    }

    private fun clean(value: String?): String? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (
            text.equals("null", true) || text == "--" || text == "-" ||
            text.equals("N/A", true) || text.equals("undefined", true)
        ) return null
        return text
    }
}
