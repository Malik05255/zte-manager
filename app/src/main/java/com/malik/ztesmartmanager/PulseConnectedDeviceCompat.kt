package com.malik.ztesmartmanager

import com.malik.ztesmartmanager.core.model.ConnectedDevice

/**
 * Small presentation aliases used only by the Pulse UI.
 * They map directly to the parsed router fields; no synthetic device data is introduced.
 */
internal val ConnectedDevice.mac: String
    get() = macAddress

internal val ConnectedDevice.ip: String?
    get() = ipAddress

internal val ConnectedDevice.name: String?
    get() = hostname
