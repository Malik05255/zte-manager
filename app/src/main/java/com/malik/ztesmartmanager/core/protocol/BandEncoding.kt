package com.malik.ztesmartmanager.core.protocol

object BandEncoding {
    fun lteMask(bands: Set<Int>): String {
        require(bands.isNotEmpty()) { "At least one LTE band is required" }
        require(bands.all { it in 1..63 }) { "LTE band outside supported mask range" }

        var mask = 0L
        bands.forEach { band -> mask = mask or (1L shl (band - 1)) }
        return "0x${mask.toString(16).uppercase()}"
    }

    fun nrMask(bands: Set<Int>): String {
        require(bands.isNotEmpty()) { "At least one NR band is required" }
        return bands.sorted().joinToString(",")
    }

    fun decodeLteMask(maskText: String, knownBands: Set<Int>): Set<Int> {
        val cleaned = maskText.trim().removePrefix("0x").removePrefix("0X")
        val mask = cleaned.toLongOrNull(16) ?: return emptySet()
        return knownBands.filterTo(linkedSetOf()) { band -> (mask and (1L shl (band - 1))) != 0L }
    }
}
