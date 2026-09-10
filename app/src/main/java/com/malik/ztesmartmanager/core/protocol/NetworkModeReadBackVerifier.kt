package com.malik.ztesmartmanager.core.protocol

/**
 * Exact verifier for ZTE BearerPreference.
 * A write acknowledgement is never enough. The requested mode is considered applied only when
 * the router exposes BearerPreference again and it matches after trimming/case normalization.
 */
object NetworkModeReadBackVerifier {
    fun matches(requested: String, actual: String?): Boolean {
        if (requested.isBlank() || actual == null) return false
        return normalize(requested) == normalize(actual)
    }

    fun normalize(value: String): String = value.trim().uppercase()
}
