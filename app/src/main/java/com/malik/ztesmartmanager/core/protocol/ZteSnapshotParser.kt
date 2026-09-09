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

        val cells = buildList {
            val primaryBand = cleanBand(raw["lte_ca_pcell_band"] ?: raw["wan_active_band"])
            if (primaryBand != null) {
                add(
                    CarrierCell(
                        role = CellRole.PRIMARY,
                        band = primaryBand,
                        pci = parseSmartInt(raw["lte_pci"]),
                        arfcn = parseSmartInt(raw["lte_ca_pcell_arfcn"] ?: raw["wan_active_channel"]),
                        bandwidthMhz = parseNumber(raw["lte_ca_pcell_bandwidth"])
                    )
                )
            }

            parseSecondaryCells(raw["lte_multi_ca_scell_info"]).forEach(::add)

            val nrBand = cleanBand(raw["nr5g_action_band"])
            if (nrBand != null) {
                add(
                    CarrierCell(
                        role = CellRole.NR,
                        band = nrBand,
                        pci = parseSmartInt(raw["nr5g_pci"]),
                        arfcn = parseSmartInt(raw["nr5g_action_channel"]),
                        bandwidthMhz = null
                    )
                )
            }
        }

        val mcc = raw["rmcc"].orEmpty().trim()
        val mnc = raw["rmnc"].orEmpty().trim()

        return RouterSnapshot(
            model = raw["device_name"].takeUnless { it.isNullOrBlank() },
            firmware = raw["wa_inner_version"].takeUnless { it.isNullOrBlank() },
            hardwareVersion = raw["hardware_version"].takeUnless { it.isNullOrBlank() },
            networkType = raw["network_type"].takeUnless { it.isNullOrBlank() },
            operatorCode = (mcc + mnc).takeUnless { it.isBlank() },
            lteRsrp = parseNumber(raw["lte_rsrp"]),
            lteRsrq = parseNumber(raw["lte_rsrq"]),
            lteRssi = parseNumber(raw["lte_rssi"]),
            lteSinr = parseNumber(raw["lte_snr"]),
            nrRsrp = parseNumber(raw["Z5g_rsrp"]),
            nrSinr = parseNumber(raw["Z5g_SINR"]),
            lteBand = cleanBand(raw["wan_active_band"] ?: raw["lte_ca_pcell_band"]),
            nrBand = cleanBand(raw["nr5g_action_band"]),
            pci = parseSmartInt(raw["lte_pci"]),
            earfcn = parseSmartInt(raw["wan_active_channel"]),
            cellId = parseSmartLong(raw["cell_id"]),
            caActive = raw["wan_lte_ca"].orEmpty().contains("activated", ignoreCase = true) ||
                cells.count { it.role == CellRole.SECONDARY } > 0,
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
                CarrierCell(
                    role = CellRole.SECONDARY,
                    band = fields.getOrNull(3)?.let { cleanBand("B$it") },
                    pci = parseSmartInt(fields.getOrNull(1)),
                    arfcn = parseSmartInt(fields.getOrNull(4)),
                    bandwidthMhz = parseNumber(fields.getOrNull(5))
                )
            }
    }

    private fun cleanBand(value: String?): String? {
        val cleaned = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
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
