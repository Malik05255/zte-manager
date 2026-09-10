package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.profile.RadioIdEncoding
import org.json.JSONObject

object ZteSnapshotParser {
    fun parse(
        json: JSONObject,
        radioIdEncoding: RadioIdEncoding = RadioIdEncoding.SAFE_AUTO
    ): RouterSnapshot {
        val raw = mutableMapOf<String, String>()
        json.keys().forEach { key -> raw[key] = json.optString(key, "") }

        val rawNetworkType = firstNonBlank(raw, "network_type", "current_network", "nRat")
        val radioState = classifyRadioState(rawNetworkType)

        val lteBand = normalizeLteBand(firstNonBlank(raw, "lte_ca_pcell_band", "wan_active_band", "lte_band"))
        val ltePci = ZteRadioIdParser.parseInt(raw["lte_pci"], 503, radioIdEncoding)
        val lteArfcn = parsePositiveInt(firstNonBlank(raw, "lte_ca_pcell_arfcn", "lte_ca_pcell_freq", "wan_active_channel"))
        val lteRsrp = firstSignal(raw, -160.0, -35.0, "lte_rsrp")
        val lteRsrq = firstSignal(raw, -40.0, 0.0, "lte_rsrq")
        val lteRssi = firstSignal(raw, -160.0, -20.0, "lte_rssi", "rssi")
        val lteSinr = firstSignal(raw, -30.0, 60.0, "lte_snr")

        val lteStateExplicit = radioState == RadioState.LTE_ONLY ||
            radioState == RadioState.NSA_ACTIVE || radioState == RadioState.NSA_STANDBY
        val lteLiveCarrierEvidence = ltePci != null && lteArfcn != null && lteRsrp != null
        val lteVerifiedActive = lteStateExplicit && lteLiveCarrierEvidence

        val primaryCell = if (lteLiveCarrierEvidence || lteBand != null) {
            CarrierCell(
                role = CellRole.PRIMARY,
                band = lteBand,
                pci = ltePci,
                arfcn = lteArfcn,
                bandwidthMhz = parseNumber(firstNonBlank(raw, "lte_ca_pcell_bandwidth", "bandwidth"))
            )
        } else null

        val parsedSecondaryCells = buildList {
            addAll(
                parseSecondaryCells(
                    firstNonBlank(raw, "lte_multi_ca_scell_info", "lte_ca_scell_info"),
                    radioIdEncoding
                )
            )
            if (isEmpty()) {
                val singleBand = normalizeLteBand(raw["lte_ca_scell_band"])
                if (singleBand != null) {
                    add(
                        CarrierCell(
                            role = CellRole.SECONDARY,
                            band = singleBand,
                            pci = ZteRadioIdParser.parseInt(raw["lte_ca_scell_pci"], 503, radioIdEncoding),
                            arfcn = parsePositiveInt(firstNonBlank(raw, "lte_ca_scell_arfcn", "lte_ca_scell_freq")),
                            bandwidthMhz = parseNumber(raw["lte_ca_scell_bandwidth"])
                        )
                    )
                }
            }
        }.distinctBy { Triple(it.band, it.pci, it.arfcn) }

        val nonDuplicateSecondaryCells = parsedSecondaryCells.filterNot { secondary ->
            primaryCell?.let { primary -> sameLteCarrier(primary, secondary) } == true
        }

        // A secondary-cell string can remain cached after CA is released. For an ACTIVE claim we
        // therefore require BOTH an activation state and at least one structurally complete SCell.
        val verifiedSecondaryCells = nonDuplicateSecondaryCells.filter { cell ->
            cell.band != null && cell.pci != null && cell.arfcn != null
        }
        val secondaryCarrierEvidence = verifiedSecondaryCells.isNotEmpty()

        val rawCaState = firstNonBlank(raw, "wan_lte_ca", "Lte_ca_status")
        val explicitCaActive = isCaActivated(rawCaState)
        val explicitCaInactive = isCaDeactivated(rawCaState)
        val scellActivationFlag = parseBooleanFlag(raw["lte_ca_scell_ca_activated"])
        val activationSaysActive = explicitCaActive || scellActivationFlag == true
        val activationSaysInactive = explicitCaInactive || scellActivationFlag == false
        val caStateConflict = activationSaysActive && activationSaysInactive
        val caActive = !caStateConflict && activationSaysActive && secondaryCarrierEvidence
        val caVerified = !caStateConflict && (caActive || activationSaysInactive)
        val secondaryCells = if (caActive) verifiedSecondaryCells else emptyList()

        val nsaBand = normalizeNrBand(firstNonBlank(raw, "nr_ca_pcell_band", "nr5g_action_nsa_band", "nr5g_action_band", "ZCELLINFO_band"))
        val saBand = normalizeNrBand(firstNonBlank(raw, "nr_ca_pcell_band", "nr5g_action_band", "ZCELLINFO_band", "nr5g_action_nsa_band"))
        val rawNrBand = when (radioState) {
            RadioState.NSA_ACTIVE, RadioState.NSA_STANDBY -> nsaBand
            RadioState.SA_ACTIVE -> saBand
            else -> nsaBand ?: saBand
        }
        val rawNrArfcn = parsePositiveInt(firstNonBlank(raw, "nr_ca_pcell_freq", "nr5g_action_channel", "Z5g_dlEarfcn"))
        val rawNrPci = ZteRadioIdParser.parseInt(
            firstNonBlank(raw, "nr5g_pci", "Z_PCI"),
            1007,
            radioIdEncoding
        )
        val rawNrCellId = ZteRadioIdParser.parseLong(
            firstNonBlank(raw, "nr5g_cell_id", "Z5g_CELL_ID"),
            encoding = radioIdEncoding
        )?.takeIf { it > 0 }
        val rawNrRsrp = firstSignal(raw, -170.0, -35.0, "Z5g_rsrp", "nr5g_rsrp", "5g_rx0_rsrp", "5g_rx1_rsrp")
        val rawNrSinr = firstNrSinr(raw, "Z5g_SINR", "Z5g_snr", "nr5g_sinr", "nr5g_snr")

        val explicitNrActiveState = radioState == RadioState.NSA_ACTIVE ||
            radioState == RadioState.SA_ACTIVE || radioState == RadioState.FIVE_G_ACTIVE
        val nrStructuralEvidence = rawNrBand != null && rawNrArfcn != null && rawNrPci != null
        val nrSignalEvidence = rawNrRsrp != null
        val nrLiveCarrierEvidence = nrStructuralEvidence && nrSignalEvidence

        // VERIFIED-ONLY NR policy:
        // Never infer 5G from stale NR fields. Both an explicit live RAT state and a complete current
        // NR carrier signature (band + ARFCN + PCI + RSRP) are required before the UI may say 5G.
        val nrVerifiedActive = explicitNrActiveState && nrLiveCarrierEvidence
        val nrActive = nrVerifiedActive

        val nrBand = rawNrBand.takeIf { nrVerifiedActive }
        val nrArfcn = rawNrArfcn.takeIf { nrVerifiedActive }
        val nrPci = rawNrPci.takeIf { nrVerifiedActive }
        val nrRsrp = rawNrRsrp.takeIf { nrVerifiedActive }
        val nrSinr = rawNrSinr.takeIf { nrVerifiedActive }

        val nrCells = buildList {
            if (nrVerifiedActive) {
                add(
                    CarrierCell(
                        role = CellRole.NR,
                        band = nrBand,
                        pci = nrPci,
                        arfcn = nrArfcn,
                        bandwidthMhz = parseNumber(raw["nr_ca_pcell_bandwidth"])
                    )
                )
                addAll(parseNrSecondaryCells(raw["nr_multi_ca_scell_info"], radioIdEncoding))
            }
        }.distinctBy { Triple(it.band, it.pci, it.arfcn) }

        val verifiedNetworkType = when {
            nrVerifiedActive && radioState == RadioState.NSA_ACTIVE && lteVerifiedActive -> "5G NSA / 4G"
            nrVerifiedActive && radioState == RadioState.SA_ACTIVE -> "5G SA"
            nrVerifiedActive -> "5G"
            lteVerifiedActive -> if (caVerified && caActive) "4G+" else "4G"
            else -> "غير مؤكد"
        }

        val radioMode = when {
            nrVerifiedActive && radioState == RadioState.NSA_ACTIVE -> "NSA_ACTIVE_VERIFIED"
            nrVerifiedActive && radioState == RadioState.SA_ACTIVE -> "SA_ACTIVE_VERIFIED"
            nrVerifiedActive -> "5G_ACTIVE_VERIFIED"
            lteVerifiedActive -> "LTE_ACTIVE_VERIFIED"
            explicitNrActiveState -> "NR_STATE_UNVERIFIED"
            radioState == RadioState.NSA_STANDBY -> "NSA_STANDBY"
            else -> "UNKNOWN"
        }

        raw["_zte_interpreted_network_type"] = verifiedNetworkType
        raw["_zte_verified_network_type"] = verifiedNetworkType
        raw["_zte_radio_mode"] = radioMode
        raw["_zte_radio_id_encoding"] = radioIdEncoding.name
        raw["_zte_lte_pci_decoded"] = ltePci?.toString().orEmpty()
        raw["_zte_nr_pci_decoded"] = rawNrPci?.toString().orEmpty()
        raw["_zte_lte_active_verified"] = lteVerifiedActive.toString()
        raw["_zte_nr_active"] = nrActive.toString()
        raw["_zte_nr_active_verified"] = nrVerifiedActive.toString()
        raw["_zte_nr_explicit_state"] = explicitNrActiveState.toString()
        raw["_zte_nr_structural_evidence"] = nrStructuralEvidence.toString()
        raw["_zte_nr_signal_evidence"] = nrSignalEvidence.toString()
        raw["_zte_nr_cell_id_evidence"] = (rawNrCellId != null).toString()
        raw["_zte_ca_active"] = caActive.toString()
        raw["_zte_ca_verified"] = caVerified.toString()
        raw["_zte_ca_state_conflict"] = caStateConflict.toString()
        raw["_zte_ca_secondary_evidence"] = secondaryCarrierEvidence.toString()
        raw["_zte_ca_state_raw"] = rawCaState.orEmpty()
        raw["_zte_raw_network_type"] = rawNetworkType.orEmpty()
        raw["_zte_secondary_cells_raw_count"] = parsedSecondaryCells.size.toString()
        raw["_zte_secondary_cells_verified_count"] = verifiedSecondaryCells.size.toString()
        raw["_zte_secondary_cells_count"] = secondaryCells.size.toString()

        val mcc = firstNonBlank(raw, "rmcc", "mdm_mcc").orEmpty().trim()
        val mnc = firstNonBlank(raw, "rmnc", "mdm_mnc").orEmpty().trim()
        val lteCellId = ZteRadioIdParser.parseLong(raw["cell_id"], encoding = radioIdEncoding)
            ?.takeIf { it > 0 && lteVerifiedActive }

        return RouterSnapshot(
            model = firstNonBlank(raw, "device_name", "model_name", "product_name"),
            firmware = firstNonBlank(raw, "wa_inner_version", "web_version", "cr_version"),
            hardwareVersion = firstNonBlank(raw, "hardware_version"),
            networkType = verifiedNetworkType,
            operatorCode = (mcc + mnc).takeUnless { it.isBlank() },
            lteRsrp = lteRsrp.takeIf { lteVerifiedActive },
            lteRsrq = lteRsrq.takeIf { lteVerifiedActive },
            lteRssi = lteRssi.takeIf { lteVerifiedActive },
            lteSinr = lteSinr.takeIf { lteVerifiedActive },
            nrRsrp = nrRsrp,
            nrSinr = nrSinr,
            lteBand = lteBand.takeIf { lteVerifiedActive },
            nrBand = nrBand,
            pci = ltePci.takeIf { lteVerifiedActive },
            earfcn = lteArfcn.takeIf { lteVerifiedActive },
            cellId = lteCellId,
            caActive = caVerified && caActive,
            cells = buildList {
                if (lteVerifiedActive) primaryCell?.let(::add)
                if (caVerified && caActive) addAll(secondaryCells)
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

        // Deliberately exact: strings that merely contain "5G" can be configuration labels rather
        // than the current RAT. New firmware aliases must be added explicitly after evidence.
        return when (type) {
            "ENDC", "EN-DC" -> RadioState.NSA_ACTIVE
            "LTE-NSA" -> RadioState.NSA_STANDBY
            "SA", "5G-SA", "NR-SA", "NR5G-SA", "NR5G" -> RadioState.SA_ACTIVE
            "5G", "NR", "5G-ACTIVE", "NR-ACTIVE", "NR5G-ACTIVE" -> RadioState.FIVE_G_ACTIVE
            "LTE", "4G", "LTE-A", "LTE+" -> RadioState.LTE_ONLY
            else -> RadioState.UNKNOWN
        }
    }

    private fun parseSecondaryCells(
        value: String?,
        radioIdEncoding: RadioIdEncoding
    ): List<CarrierCell> {
        if (value.isNullOrBlank()) return emptyList()

        return value.trimEnd(';')
            .split(';')
            .mapNotNull { row ->
                val fields = row.split(',').map { it.trim() }
                if (fields.size < 6) return@mapNotNull null

                val band = normalizeLteBand(fields.getOrNull(3)) ?: return@mapNotNull null
                CarrierCell(
                    role = CellRole.SECONDARY,
                    band = band,
                    pci = ZteRadioIdParser.parseInt(fields.getOrNull(1), 503, radioIdEncoding),
                    arfcn = parsePositiveInt(fields.getOrNull(4)),
                    bandwidthMhz = parseNumber(fields.getOrNull(5))
                )
            }
    }

    private fun parseNrSecondaryCells(
        value: String?,
        radioIdEncoding: RadioIdEncoding
    ): List<CarrierCell> {
        if (value.isNullOrBlank()) return emptyList()

        return value.trimEnd(';')
            .split(';')
            .mapNotNull { row ->
                val fields = row.split(',').map { it.trim() }
                if (fields.size < 6) return@mapNotNull null

                val band = normalizeNrBand(fields.getOrNull(3)) ?: return@mapNotNull null
                CarrierCell(
                    role = CellRole.NR,
                    band = band,
                    pci = ZteRadioIdParser.parseInt(fields.getOrNull(1), 1007, radioIdEncoding),
                    arfcn = parsePositiveInt(fields.getOrNull(4)),
                    bandwidthMhz = parseNumber(fields.getOrNull(5))
                )
            }
    }

    private fun sameLteCarrier(primary: CarrierCell, secondary: CarrierCell): Boolean {
        if (primary.band == null || secondary.band == null || primary.band != secondary.band) return false

        val primaryArfcn = primary.arfcn
        val secondaryArfcn = secondary.arfcn
        if (primaryArfcn != null && secondaryArfcn != null) return primaryArfcn == secondaryArfcn

        val primaryPci = primary.pci
        val secondaryPci = secondary.pci
        return primaryPci != null && secondaryPci != null && primaryPci == secondaryPci
    }

    private fun firstNonBlank(raw: Map<String, String>, vararg names: String): String? = names
        .asSequence()
        .mapNotNull { raw[it]?.trim() }
        .firstOrNull { value ->
            value.isNotEmpty() &&
                !value.equals("null", true) &&
                value != "--" &&
                value != "-" &&
                !value.equals("N/A", true)
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

    private fun firstNrSinr(raw: Map<String, String>, vararg names: String): Double? {
        names.forEach { name ->
            val value = parseNumber(raw[name]) ?: return@forEach
            if (value == -20.0 || value == -3276.8) return@forEach
            if (value in -19.9..60.0) return value
        }
        return null
    }

    private fun parseBooleanFlag(value: String?): Boolean? {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return when (normalized) {
            "1", "true", "yes", "on", "active", "activated", "ca_activated" -> true
            "0", "false", "no", "off", "inactive", "deactivated", "ca_deactivated" -> false
            else -> null
        }
    }

    private fun isCaActivated(value: String?): Boolean {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return normalized in setOf("ca_activated", "activated", "active", "1")
    }

    private fun isCaDeactivated(value: String?): Boolean {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return normalized in setOf("ca_deactivated", "deactivated", "inactive", "0")
    }

    private fun normalizeLteBand(value: String?): String? {
        val cleaned = cleanValue(value) ?: return null
        val number = Regex("\\d+").find(cleaned)?.value?.toIntOrNull() ?: return null
        if (number <= 0) return null
        return "B$number"
    }

    private fun normalizeNrBand(value: String?): String? {
        val cleaned = cleanValue(value) ?: return null
        if (cleaned == "-1" || cleaned == "0") return null
        val number = Regex("(?:^|[^0-9])(\\d{1,3})(?:$|[^0-9])")
            .find(cleaned)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: return null
        if (number <= 0 || number > 999) return null
        return "N$number"
    }

    private fun cleanValue(value: String?): String? {
        val cleaned = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (
            cleaned.equals("null", true) || cleaned == "--" || cleaned == "-" ||
            cleaned.equals("N/A", true) || cleaned.equals("undefined", true)
        ) return null
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

    private fun parsePositiveInt(value: String?): Int? = parseSmartInt(value)?.takeIf { it > 0 }
}
