package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import org.json.JSONObject

object ZteSnapshotParser {
    fun parse(json: JSONObject): RouterSnapshot {
        val raw = mutableMapOf<String, String>()
        json.keys().forEach { key -> raw[key] = json.optString(key, "") }

        val rawNetworkType = firstNonBlank(raw, "network_type", "current_network", "nRat")
        val radioState = classifyRadioState(rawNetworkType)

        val lteBand = normalizeLteBand(firstNonBlank(raw, "wan_active_band", "lte_ca_pcell_band", "lte_band"))
        val ltePci = parseZtePci(raw["lte_pci"], 503)
        val lteArfcn = parseSmartInt(firstNonBlank(raw, "lte_ca_pcell_arfcn", "lte_ca_pcell_freq", "wan_active_channel"))
        val lteRsrp = firstSignal(raw, -160.0, -35.0, "lte_rsrp")
        val lteRsrq = firstSignal(raw, -40.0, 0.0, "lte_rsrq")
        val lteRssi = firstSignal(raw, -160.0, -20.0, "lte_rssi", "rssi")
        val lteSinr = firstSignal(raw, -30.0, 60.0, "lte_snr")

        val primaryCell = if (lteBand != null || ltePci != null || lteArfcn != null) {
            CarrierCell(
                role = CellRole.PRIMARY,
                band = lteBand,
                pci = ltePci,
                arfcn = lteArfcn,
                bandwidthMhz = parseNumber(firstNonBlank(raw, "lte_ca_pcell_bandwidth", "bandwidth"))
            )
        } else null

        val secondaryCells = buildList {
            addAll(parseSecondaryCells(firstNonBlank(raw, "lte_multi_ca_scell_info", "lte_ca_scell_info")))

            if (isEmpty()) {
                val singleBand = normalizeLteBand(firstNonBlank(raw, "lte_ca_scell_band"))
                val singleActive = isPositiveFlag(raw["lte_ca_scell_ca_activated"]) ||
                    isPositiveFlag(raw["lte_ca_scell_present"]) ||
                    isCaActivated(raw["wan_lte_ca"]) ||
                    isCaActivated(raw["Lte_ca_status"])

                if (singleBand != null && singleActive) {
                    add(
                        CarrierCell(
                            role = CellRole.SECONDARY,
                            band = singleBand,
                            pci = parseFlexiblePci(raw["lte_ca_scell_pci"], 503),
                            arfcn = parseSmartInt(firstNonBlank(raw, "lte_ca_scell_arfcn", "lte_ca_scell_freq")),
                            bandwidthMhz = parseNumber(raw["lte_ca_scell_bandwidth"])
                        )
                    )
                }
            }
        }.distinctBy { Triple(it.band, it.pci, it.arfcn) }

        val rawNrBand = normalizeNrBand(
            firstNonBlank(
                raw,
                "nr5g_action_band",
                "nr5g_action_nsa_band",
                "ZCELLINFO_band",
                "nr_ca_pcell_band"
            )
        )
        val rawNrArfcn = parseSmartInt(firstNonBlank(raw, "nr5g_action_channel", "Z5g_dlEarfcn", "nr_ca_pcell_freq"))
        val rawNrPci = parseZtePci(firstNonBlank(raw, "nr5g_pci", "Z_PCI"), 1007)
        val rawNrCellId = parseZteHexLong(firstNonBlank(raw, "nr5g_cell_id", "Z5g_CELL_ID"))
        val rawNrRsrp = firstSignal(raw, -170.0, -35.0, "Z5g_rsrp", "nr5g_rsrp", "5g_rx0_rsrp", "5g_rx1_rsrp")
        val rawNrSinr = firstSignal(raw, -30.0, 60.0, "Z5g_SINR", "Z5g_snr", "nr5g_sinr", "nr5g_snr")

        // Some MC801A firmware leaves network_type at LTE/LTE-NSA while it still reports
        // live NR measurements. ZManager itself shows the 5G block whenever NR-RSRP is live.
        // Require multiple independent NR indicators so stale values do not create false 5G.
        val liveNrSignalEvidence = rawNrRsrp != null &&
            (rawNrBand != null || rawNrArfcn != null || rawNrPci != null || rawNrCellId != null)
        val structuralNrEvidence = rawNrBand != null &&
            ((rawNrArfcn != null && rawNrPci != null) || rawNrCellId != null)
        val strongNrEvidence = liveNrSignalEvidence || structuralNrEvidence

        val nrActive = when (radioState) {
            RadioState.NSA_ACTIVE, RadioState.SA_ACTIVE, RadioState.FIVE_G_ACTIVE -> true
            RadioState.NSA_STANDBY, RadioState.LTE_ONLY, RadioState.UNKNOWN -> strongNrEvidence
        }

        val nrBand = rawNrBand.takeIf { nrActive }
        val nrArfcn = rawNrArfcn.takeIf { nrActive }
        val nrPci = rawNrPci.takeIf { nrActive }
        val nrRsrp = rawNrRsrp.takeIf { nrActive }
        val nrSinr = rawNrSinr.takeIf { nrActive }

        val nrCells = buildList {
            if (nrActive && (nrBand != null || nrPci != null || nrArfcn != null || nrRsrp != null)) {
                add(
                    CarrierCell(
                        role = CellRole.NR,
                        band = nrBand,
                        pci = nrPci,
                        arfcn = nrArfcn,
                        bandwidthMhz = parseNumber(raw["nr_ca_pcell_bandwidth"])
                    )
                )
            }
            if (nrActive) addAll(parseNrSecondaryCells(raw["nr_multi_ca_scell_info"]))
        }.distinctBy { Triple(it.band, it.pci, it.arfcn) }

        val caActive = secondaryCells.isNotEmpty() ||
            isCaActivated(raw["wan_lte_ca"]) ||
            isCaActivated(raw["Lte_ca_status"]) ||
            isPositiveFlag(raw["lte_ca_scell_ca_activated"])

        val ltePresent = primaryCell != null || lteRsrp != null ||
            radioState == RadioState.LTE_ONLY || radioState == RadioState.NSA_ACTIVE || radioState == RadioState.NSA_STANDBY

        val interpretedNetworkType = when (radioState) {
            RadioState.NSA_ACTIVE -> "5G NSA • ${rawNetworkType ?: "ENDC"}"
            RadioState.NSA_STANDBY -> if (nrActive) "5G NSA + 4G • LTE-NSA" else "LTE-NSA"
            RadioState.SA_ACTIVE -> "5G SA"
            RadioState.FIVE_G_ACTIVE -> rawNetworkType ?: "5G"
            RadioState.LTE_ONLY -> if (nrActive) "5G NSA + 4G • LTE telemetry" else rawNetworkType ?: "LTE"
            RadioState.UNKNOWN -> when {
                nrActive && ltePresent -> "5G + 4G"
                nrActive -> "5G"
                else -> rawNetworkType
            }
        }

        raw["_zte_interpreted_network_type"] = interpretedNetworkType.orEmpty()
        raw["_zte_nr_active"] = nrActive.toString()
        raw["_zte_nr_live_signal_evidence"] = liveNrSignalEvidence.toString()
        raw["_zte_nr_structural_evidence"] = structuralNrEvidence.toString()
        raw["_zte_ca_active"] = caActive.toString()
        raw["_zte_raw_network_type"] = rawNetworkType.orEmpty()

        val mcc = firstNonBlank(raw, "rmcc", "mdm_mcc").orEmpty().trim()
        val mnc = firstNonBlank(raw, "rmnc", "mdm_mnc").orEmpty().trim()

        return RouterSnapshot(
            model = firstNonBlank(raw, "device_name", "model_name", "product_name"),
            firmware = firstNonBlank(raw, "wa_inner_version", "web_version", "cr_version"),
            hardwareVersion = firstNonBlank(raw, "hardware_version"),
            networkType = interpretedNetworkType,
            operatorCode = (mcc + mnc).takeUnless { it.isBlank() },
            lteRsrp = lteRsrp,
            lteRsrq = lteRsrq,
            lteRssi = lteRssi,
            lteSinr = lteSinr,
            nrRsrp = nrRsrp,
            nrSinr = nrSinr,
            lteBand = lteBand,
            nrBand = nrBand,
            pci = ltePci,
            earfcn = lteArfcn,
            cellId = parseZteHexLong(raw["cell_id"]),
            caActive = caActive,
            cells = buildList {
                primaryCell?.let(::add)
                addAll(secondaryCells)
                addAll(nrCells)
            },
            modem4gTemperature = parseNumber(raw["pm_sensor_mdm"]),
            modem5gTemperature = parseNumber(firstNonBlank(raw, "pm_modem_5g", "pm_sensor_5g")),
            raw = raw
        )
    }

    private enum class RadioState {
        LTE_ONLY,
        NSA_ACTIVE,
        NSA_STANDBY,
        SA_ACTIVE,
        FIVE_G_ACTIVE,
        UNKNOWN
    }

    private fun classifyRadioState(value: String?): RadioState {
        val type = value?.trim()?.uppercase()?.replace('_', '-')?.replace(" ", "").orEmpty()
        if (type.isBlank()) return RadioState.UNKNOWN

        return when {
            type == "ENDC" || type == "EN-DC" || type.contains("EN-DC") || type.contains("ENDC") -> RadioState.NSA_ACTIVE
            type == "LTE-NSA" || (type.contains("LTE") && type.contains("NSA")) -> RadioState.NSA_STANDBY
            type == "SA" || type == "5G-SA" || type == "NR-SA" || type == "NR5G-SA" -> RadioState.SA_ACTIVE
            type.contains("5G") || type.startsWith("NR") -> RadioState.FIVE_G_ACTIVE
            type.contains("LTE") || type == "4G" -> RadioState.LTE_ONLY
            else -> RadioState.UNKNOWN
        }
    }

    private fun parseSecondaryCells(value: String?): List<CarrierCell> {
        if (value.isNullOrBlank()) return emptyList()

        return value.trimEnd(';')
            .split(';')
            .mapNotNull { row ->
                val fields = row.split(',').map { it.trim() }
                if (fields.size < 5) return@mapNotNull null

                val band = normalizeLteBand(fields.getOrNull(3)) ?: return@mapNotNull null
                CarrierCell(
                    role = CellRole.SECONDARY,
                    band = band,
                    pci = parseFlexiblePci(fields.getOrNull(1), 503),
                    arfcn = parseSmartInt(fields.getOrNull(4)),
                    bandwidthMhz = parseNumber(fields.getOrNull(5))
                )
            }
    }

    private fun parseNrSecondaryCells(value: String?): List<CarrierCell> {
        if (value.isNullOrBlank()) return emptyList()

        // Observed legacy format example:
        // 0,XX,1,n75,292330,30MHz,0,-73.3,-10.5,17.5;
        return value.trimEnd(';')
            .split(';')
            .mapNotNull { row ->
                val fields = row.split(',').map { it.trim() }
                if (fields.size < 6) return@mapNotNull null

                val band = normalizeNrBand(fields.getOrNull(3)) ?: return@mapNotNull null
                CarrierCell(
                    role = CellRole.NR,
                    band = band,
                    pci = parseFlexiblePci(fields.getOrNull(1), 1007),
                    arfcn = parseSmartInt(fields.getOrNull(4)),
                    bandwidthMhz = parseNumber(fields.getOrNull(5))
                )
            }
    }

    private fun firstNonBlank(raw: Map<String, String>, vararg names: String): String? = names
        .asSequence()
        .mapNotNull { raw[it]?.trim() }
        .firstOrNull { value ->
            value.isNotEmpty() &&
                !value.equals("null", true) &&
                value != "--" &&
                value != "N/A"
        }

    private fun firstSignal(
        raw: Map<String, String>,
        min: Double,
        max: Double,
        vararg names: String
    ): Double? {
        names.forEach { name ->
            val value = parseNumber(raw[name])
            if (value != null && value in min..max) return value
        }
        return null
    }

    private fun isPositiveFlag(value: String?): Boolean {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return normalized in setOf("1", "true", "yes", "on", "active", "activated", "ca_activated", "present")
    }

    private fun isCaActivated(value: String?): Boolean {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return normalized == "ca_activated" || normalized == "activated" || normalized == "active" || normalized == "1"
    }

    private fun normalizeLteBand(value: String?): String? {
        val cleaned = cleanValue(value) ?: return null
        val number = Regex("\\d+").find(cleaned)?.value ?: return null
        return "B$number"
    }

    private fun normalizeNrBand(value: String?): String? {
        val cleaned = cleanValue(value) ?: return null
        val number = Regex("\\d+").find(cleaned)?.value ?: return null
        return "N$number"
    }

    private fun cleanValue(value: String?): String? {
        val cleaned = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (cleaned.equals("null", true) || cleaned == "--" || cleaned.equals("N/A", true)) return null
        return cleaned
    }

    private fun parseNumber(value: String?): Double? {
        val match = Regex("-?\\d+(?:\\.\\d+)?").find(value.orEmpty()) ?: return null
        return match.value.toDoubleOrNull()
    }

    private fun parseSmartInt(value: String?): Int? {
        val text = cleanValue(value)?.removePrefix("0x")?.removePrefix("0X") ?: return null
        val radix = if (text.any { it.lowercaseChar() in 'a'..'f' }) 16 else 10
        return text.toIntOrNull(radix)
    }

    // Legacy ZTE goform exposes lte_pci/nr5g_pci as hexadecimal strings.
    // Prefer hex, but keep a decimal fallback for firmware families that return decimal.
    private fun parseZtePci(value: String?, max: Int): Int? {
        val text = cleanValue(value)?.removePrefix("0x")?.removePrefix("0X") ?: return null
        val hex = text.toIntOrNull(16)
        if (hex != null && hex in 0..max) return hex

        val decimal = text.toIntOrNull(10)
        return decimal?.takeIf { it in 0..max }
    }

    private fun parseFlexiblePci(value: String?, max: Int): Int? {
        val text = cleanValue(value)?.removePrefix("0x")?.removePrefix("0X") ?: return null
        val decimal = text.toIntOrNull(10)
        if (decimal != null && decimal in 0..max) return decimal

        val hex = text.toIntOrNull(16)
        return hex?.takeIf { it in 0..max }
    }

    // cell_id / NR cell IDs in legacy goform are also hexadecimal, even when the string happens to contain digits only.
    private fun parseZteHexLong(value: String?): Long? {
        val text = cleanValue(value)?.removePrefix("0x")?.removePrefix("0X") ?: return null
        return text.toLongOrNull(16) ?: text.toLongOrNull(10)
    }
}
