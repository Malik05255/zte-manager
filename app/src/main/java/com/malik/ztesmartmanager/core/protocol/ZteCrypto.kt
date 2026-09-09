package com.malik.ztesmartmanager.core.protocol

import java.security.MessageDigest

object ZteCrypto {
    fun loginPassword(adminPassword: String, ld: String): String {
        val first = digest("SHA-256", adminPassword).uppercase()
        return digest("SHA-256", first + ld).uppercase()
    }

    fun adValue(waInnerVersion: String, crVersion: String, rd: String): String {
        val first = digest("MD5", waInnerVersion + crVersion)
        return digest("MD5", first + rd)
    }

    private fun digest(algorithm: String, value: String): String =
        MessageDigest.getInstance(algorithm)
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }
}
