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
        "device_name", "model_name", "product_name",
        "hardware_version", "web_version", "wa_inner_version", "cr_version", "RD",

        "network_type", "current_network", "current_network_mode", "nRat",
        "network_provider", "network_provider_fullname", "rmcc", "rmnc", "mdm_mcc", "mdm_mnc",
        "signalbar", "modem_main_state", "ppp_status",

        // Safe read-only WAN traffic fields observed across ZTE web firmware.
        "realtime_tx_thrpt", "realtime_rx_thrpt",
        "realtime_tx_bytes", "realtime_rx_bytes", "realtime_time",
        "monthly_tx_bytes", "monthly_rx_bytes", "monthly_time", "date_month",
        "wan_curr_tx_bytes", "wan_curr_rx_bytes", "wan_curr_conn_time",

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

        "pm_sensor_ambient", "pm_sensor_mdm", "pm_sensor_5g", "pm_modem_5g", "pm_sensor_pa1", "wifi_chip_temp",
        "loginfo"
    )

    override fun matches(model: String?, hardwareVersion: String?, firmware: String?): Boolean = true
}

object RouterProfileRegistry {
    private val profiles: List<RouterProfile> = listOf(Mc801aProfile)

    fun resolve(model: String?, hardwareVersion: String?, firmware: String?): RouterProfile =
        profiles.firstOrNull { it.matches(model, hardwareVersion, firmware) } ?: GenericZteProfile
}
