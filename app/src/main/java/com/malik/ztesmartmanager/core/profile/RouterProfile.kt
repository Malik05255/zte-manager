package com.malik.ztesmartmanager.core.profile

import com.malik.ztesmartmanager.core.model.RouterCapabilities

interface RouterProfile {
    val id: String
    val capabilities: RouterCapabilities
    val statusFields: Set<String>

    fun matches(model: String?, hardwareVersion: String?, firmware: String?): Boolean
}
