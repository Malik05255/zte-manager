package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import org.json.JSONObject

fun runtimeCapabilitiesFromSnapshot(
    capabilities: RouterCapabilities,
    raw: Map<String, String>
): RuntimeCapabilityReport {
    val json = JSONObject()
    raw.forEach { (key, value) -> json.put(key, value) }
    return RuntimeCapabilityProbe.evaluate(capabilities, json)
}
