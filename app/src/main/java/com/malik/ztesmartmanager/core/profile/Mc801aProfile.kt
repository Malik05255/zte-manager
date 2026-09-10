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

    // Read-only telemetry union based on fields observed across MC-series firmware.
    // Unknown fields are harmless: ZTE simply returns them empty/absent on firmware that does not expose them.
    override val statusFields: Set<String> = linkedSetOf(
        "device_name", "model_name", "product_name",
        "hardware_version", "web_version", "wa_inner_version", "cr_version", "RD",

        "network_type", "current_network", "current_network_mode", "nRat",
        "net_select", "net_select_mode", "m_netselect_save", "m_netselect_contents", "BearerPreference",
        "network_provider", "network_provider_fullname", "rmcc", "rmnc", "mdm_mcc", "mdm_mnc",
        "signalbar", "modem_main_state", "ppp_status",

        "rssi", "bandwidth", "tx_power",
        "lte_rsrp", "lte_rsrq", "lte_rssi", "lte_snr",
        "lte_rsrp_1", "lte_rsrp_2", "lte_rsrp_3", "lte_rsrp_4",
        "lte_snr_1", "lte_snr_2", "lte_snr_3", "lte_snr_4",
        "lte_pci", "wan_active_channel", "wan_active_band", "lte_band", "cell_id", "enodeb_id",

        "wan_lte_ca", "Lte_ca_status",
        "lte_ca_scell_present", "lte_ca_scell_ca_activated", "lte_ca_scell_pci",
        "lte_multi_ca_scell_info", "lte_ca_scell_info", "lte_multi_ca_scell_sig_info",
        "lte_ca_pcell_band", "lte_ca_pcell_bandwidth", "lte_ca_pcell_arfcn", "lte_ca_pcell_freq",
        "lte_ca_scell_band", "lte_ca_scell_bandwidth", "lte_ca_scell_arfcn", "lte_ca_scell_freq",

        "Z5g_rsrp", "Z5g_rsrq", "Z5g_SINR", "Z5g_snr", "Z5g_dlEarfcn", "Z5g_CELL_ID", "ZCELLINFO_band", "Z_PCI",
        "5g_rx0_rsrp", "5g_rx1_rsrp",
        "nr5g_rsrp", "nr5g_rsrq", "nr5g_rssi", "nr5g_sinr", "nr5g_snr",
        "nr5g_pci", "nr5g_cell_id", "nr5g_action_channel", "nr5g_action_band", "nr5g_action_nsa_band",
        "nr_ca_pcell_band", "nr_ca_pcell_freq", "nr_ca_pcell_bandwidth", "nr_multi_ca_scell_info",

        // Newer goform firmwares expose structured current/neighbor/locked cell arrays.
        // These are read-only probes; unsupported MC801A firmware simply leaves them empty.
        "current_cell_info", "neighbor_cell_info", "locked_cell_info",

        "pm_sensor_ambient", "pm_sensor_mdm", "pm_sensor_5g", "pm_modem_5g", "pm_sensor_pa1", "wifi_chip_temp",
        "lte_band_lock", "nr5g_sa_band_lock", "nr5g_nsa_band_lock",
        "lte_pci_lock", "lte_earfcn_lock", "loginfo"
    )

    override fun matches(model: String?, hardwareVersion: String?, firmware: String?): Boolean {
        val haystack = listOfNotNull(model, hardwareVersion, firmware).joinToString(" ").uppercase()
        return "MC801A" in haystack || "MC801A1" in haystack
    }
}
