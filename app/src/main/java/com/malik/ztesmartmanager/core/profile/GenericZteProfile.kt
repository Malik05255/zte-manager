package com.malik.ztesmartmanager.core.profile

import com.malik.ztesmartmanager.core.model.RouterCapabilities

object GenericZteProfile : RouterProfile {
    override val id: String = "zte-generic"

    override val capabilities = RouterCapabilities(
        modelFamily = "ZTE Generic",
        supportsLteBandLock = false,
        supportsNrBandLock = false,
        supportsCellLock = false,
        supportsCarrierAggregationRead = true,
        supportsAntennaControl = false,
        supportedLteBands = emptySet(),
        supportedNrBands = emptySet()
    )

    override val statusFields: Set<String> = linkedSetOf(
        "device_name",
        "hardware_version",
        "web_version",
        "wa_inner_version",
        "cr_version",
        "RD",
        "network_type",
        "rmcc",
        "rmnc",
        "lte_rsrp",
        "lte_rsrq",
        "lte_rssi",
        "lte_snr",
        "Z5g_rsrp",
        "Z5g_SINR",
        "lte_pci",
        "wan_active_channel",
        "wan_active_band",
        "cell_id",
        "nr5g_pci",
        "nr5g_action_channel",
        "nr5g_action_band",
        "wan_lte_ca",
        "lte_multi_ca_scell_info",
        "lte_ca_pcell_band",
        "lte_ca_pcell_bandwidth",
        "pm_sensor_mdm",
        "pm_modem_5g",
        "loginfo"
    )

    override fun matches(model: String?, hardwareVersion: String?, firmware: String?): Boolean = true
}

object RouterProfileRegistry {
    private val profiles: List<RouterProfile> = listOf(Mc801aProfile)

    fun resolve(model: String?, hardwareVersion: String?, firmware: String?): RouterProfile =
        profiles.firstOrNull { it.matches(model, hardwareVersion, firmware) } ?: GenericZteProfile
}
