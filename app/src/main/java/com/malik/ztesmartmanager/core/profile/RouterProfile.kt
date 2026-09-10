package com.malik.ztesmartmanager.core.profile

import com.malik.ztesmartmanager.core.model.RouterCapabilities

/** How digit-only ZTE radio identifiers (PCI/Cell ID) are encoded by a profile. */
enum class RadioIdEncoding {
    /** Known legacy ZTE goform behavior such as MC801A: digit-only tokens are hexadecimal first. */
    HEX_FIRST,

    /** Reserved for a firmware profile that is positively known to expose decimal identifiers. */
    DECIMAL_FIRST,

    /** Unknown firmware: accept only unambiguous values, otherwise return unknown instead of guessing. */
    SAFE_AUTO
}

interface RouterProfile {
    val id: String
    val capabilities: RouterCapabilities
    val statusFields: Set<String>

    /** Generic/unknown firmware must fail closed on radix ambiguity. */
    val radioIdEncoding: RadioIdEncoding
        get() = RadioIdEncoding.SAFE_AUTO

    fun matches(model: String?, hardwareVersion: String?, firmware: String?): Boolean
}
