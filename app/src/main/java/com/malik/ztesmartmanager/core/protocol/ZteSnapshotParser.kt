package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import org.json.JSONObject

object ZteSnapshotParser {
    fun parse(json: JSONObject): RouterSnapshot {
        val raw = buildMap {
            json.keys().forEach { key -> put(key, json.optString(key, "")) }
        }

        val primaryBand = cleanBand(firstNonBlank(raw, "lte_ca_pcell_band", "wan_active_band", "lte_band"))
        val primaryArfcn = parseSmartInt(firstNonBlank(raw, "lte_ca_pcell_arfcn", "wan_active_channel"))

        val secondaryCells = buildList {
            addAll(parseSecondaryCells(firstNonBlank(raw, "lte_multi_ca_scell_info", "lte_ca_scell_info")))

            if (isEmpty()) {
                val singleBand = cleanBand(firstNonBlank(raw, "lte_ca_scell_band"))
                val singleActive = isPositiveFlag(raw["lte_ca_scell_ca_activated"]) ||
                    isPositiveFlag(raw["lte_ca_scell_present"])
                if (singleBand != null && singleActive) {
                    add(
                        CarrierCell(
                            role = CellRole.SECONDARY,
                            band = singleBand,
                            pci = parseSmartInt(raw["lte_ca_scell_pci"]),
                            arfcn = parseSmartInt(firstNonBlank(raw, "lte_ca_scell_arfcn")),
                            bandwidthMhz = parseNumber(raw["lte_ca_scell_bandwidth"])
                        )
                    )
                }
            }
        }

        val nrBand = cleanBand(
            firstNonBlank(
                raw,
                "nr5g_action_band",
                "nr5g_action_nsa_band",
                "ZCELLINFO_band",
                "nr_ca_pcell_band"
            )
        )
        val nrArfcn = parseSmartInt(firstNonBlank(raw, "nr5g_action_channel", "Z5g_dlEarfcn"))
        val nrPci = parseSmartInt(raw["nr5g_pci"])

        val cells = buildList {
            if (primaryBand != null) {
                add(
                    CarrierCell(
                        role = CellRole.PRIMARY,
                        band = primaryBand,
                        pci = parseSmartInt(raw["lte_pci"]),
                        arfcn = primaryArfcn,
                        bandwidthMhz = parseNumber(raw["lte_ca_pcell_bandwidth"])
                    )
                )
            }
            addAll(secondaryCells)
            if (nrBand != null) {
                add(
                    CarrierCell(
                        role = CellRole.NR,
                        band = nrBand,
                        pci = nrPci,
                        arfcn = nrArfcn,
                        bandwidthMhz = parseNumber(firstNonBlank(raw, "nr_ca_pcell_bandwidth"))
                    )
                )
            }
        }

        val mcc = raw["rmcc"].orEmpty().trim()
        val mnc = raw["rmnc"].orEmpty().trim()

        val caActive = secondaryCells.isNotEmpty() ||
            isPositiveFlag(raw["wan_lte_ca"]) ||
            isPositiveFlag(raw["lte_ca_scell_ca_activated"])

        return RouterSnapshot(
            model = firstNonBlank(raw, "device_name", "model_name", "product_name"),
            firmware = firstNonBlank(raw, "wa_inner_version", "web_version", "cr_version"),
            hardwareVersion = firstNonBlank(raw, "hardware_version"),
            networkType = firstNonBlank(raw, "network_type"),
            operatorCode = (mcc + mnc).takeUnless { it.isBlank() },
            lteRsrp = parseNumber(raw["lte_rsrp"]),
            lteRsrq = parseNumber(raw["lte_rsrq"]),
            lteRssi = parseNumber(raw["lte_rssi"]),
            lteSinr = parseNumber(raw["lte_snr"]),
            nrRsrp = parseNumber(firstNonBlank(raw, "Z5g_rsrp", "nr5g_rsrp", "5g_rx0_rsrp", "5g_rx1_rsrp")),
            nrSinr = parseNumber(firstNonBlank(raw, "Z5g_SINR", "Z5g_snr", "nr5g_sinr")),
            lteBand = cleanBand(firstNonBlank(raw, "wan_active_band", "lte_ca_pcell_band", "lte_band")),
            nrBand = nrBand,
            pci = parseSmartInt(raw["lte_pci"]),
            earfcn = parseSmartInt(firstNonBlank(raw, "wan_active_channel", "lte_ca_pcell_arfcn")),
            cellId = parseSmartLong(raw["cell_id"]),
            caActive = caActive,
            cells = cells,
            modem4gTemperature = parseNumber(raw["pm_sensor_mdm"]),
            modem5gTemperature = parseNumber(raw["pm_modem_5g"]),
            raw = raw
        )
    }

    private fun parseSecondaryCells(value: String?): List<CarrierCell> {
        if (value.isNullOrBlank()) return emptyList()
        return value.trimEnd(';')
            .split(';')
            .mapNotNull { row ->
                val fields = row.split(',')
                if (fields.size < 5) return@mapNotNull null
                val band = fields.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                CarrierCell(
                    role = CellRole.SECONDARY,
                    band = cleanBand(if (band.startsWith("B", true)) band else "B$band"),
                    pci = parseSmartInt(fields.getOrNull(1)),
                    arfcn = parseSmartInt(fields.getOrNull(4)),
                    bandwidthMhz = parseNumber(fields.getOrNull(5))
                )
            }
    }

    private fun firstNonBlank(raw: Map<String, String>, vararg names: String): String? = names
        .asSequence()
        .mapNotNull { raw[it]?.trim() }
        .firstOrNull { it.isNotEmpty() && !it.equals("null", true) && it != "--" }

    private fun isPositiveFlag(value: String?): Boolean {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return normalized in setOf("1", "true", "yes", "on", "active", "activated", "ca_activated", "present")
    }

    private fun cleanBand(value: String?): String? {
        val cleaned = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (cleaned.equals("null", true) || cleaned == "--") return null
        return cleaned.uppercase().replace("BAND", "B").replace("NR", "N")
    }

    private fun parseNumber(value: String?): Double? {
        val match = Regex("-?\\d+(?:\\.\\d+)?").find(value.orEmpty()) ?: return null
        return match.value.toDoubleOrNull()
    }

    private fun parseSmartInt(value: String?): Int? = parseSmartLong(value)?.toInt()

    private fun parseSmartLong(value: String?): Long? {
        val text = value?.trim()?.removePrefix("0x")?.removePrefix("0X")?.takeIf { it.isNotEmpty() } ?: return null
        val radix = if (text.any { it.lowercaseChar() in 'a'..'f' }) 16 else 10
        return text.toLongOrNull(radix)
    }
}
