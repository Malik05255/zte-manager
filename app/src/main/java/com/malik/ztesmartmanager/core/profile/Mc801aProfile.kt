package com.malik.ztesmartmanager.core.profile

import com.malik.ztesmartmanager.core.model.RouterCapabilities

object Mc801aProfile : RouterProfile {
    override val id: String = "zte-mc801a"

    override val capabilities = RouterCapabilities(
        modelFamily = "MC801A / MC801A1",
        supportsLteBandLock = true,
        supportsNrBandLock = true,
        supportsCellLock = true,
        supportsCarrierAggregationRead = true,
        supportsAntennaControl = true,
        supportedLteBands = setOf(1, 2, 3, 4, 5, 7, 8, 12, 17, 20, 25, 26, 28, 30, 32, 34, 38, 39, 40, 41, 42, 46, 48),
        supportedNrBands = setOf(1, 28, 41, 78, 79)
    )

    override val statusFields: Set<String> = linkedSetOf(
        "device_name", "model_name", "product_name",
        "hardware_version", "web_version", "wa_inner_version", "cr_version", "RD",
        "network_type", "rmcc", "rmnc",
        "lte_rsrp", "lte_rsrq", "lte_rssi", "lte_snr",
        "Z5g_rsrp", "Z5g_SINR",
        "lte_pci", "wan_active_channel", "wan_active_band", "cell_id",
        "nr5g_pci", "nr5g_action_channel", "nr5g_action_band",
        "wan_lte_ca", "lte_multi_ca_scell_info",
        "lte_ca_pcell_band", "lte_ca_pcell_bandwidth", "lte_ca_pcell_arfcn",
        "lte_ca_scell_band", "lte_ca_scell_bandwidth", "lte_ca_scell_arfcn",
        "pm_sensor_mdm", "pm_modem_5g",
        "lte_band_lock", "nr5g_sa_band_lock", "nr5g_nsa_band_lock",
        "lte_pci_lock", "lte_earfcn_lock", "loginfo"
    )

    override fun matches(model: String?, hardwareVersion: String?, firmware: String?): Boolean {
        val haystack = listOfNotNull(model, hardwareVersion, firmware).joinToString(" ").uppercase()
        return "MC801A" in haystack || "MC801A1" in haystack
    }
}
