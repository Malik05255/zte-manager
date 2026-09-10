package com.malik.ztesmartmanager.core.diagnostics

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.presentation.RuntimeCapabilityDetailsPresenter
import org.json.JSONArray
import org.json.JSONObject

object SupportBundleBuilder {
    fun build(
        appVersion: String,
        modelFamily: String,
        snapshot: RouterSnapshot?,
        runtime: RuntimeCapabilityReport?,
        history: List<SafeTelemetrySample>,
        stability: ConnectionStabilityReport? = null,
        generatedAtEpochMs: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
            .put("schema_version", 2)
            .put("generated_at_epoch_ms", generatedAtEpochMs)
            .put("app_version", appVersion)
            .put("model_family", modelFamily)
            .put("privacy", "raw credentials and SIM identifiers excluded")

        snapshot?.let {
            root.put("live", liveObject(it))
            val traffic = TrafficTelemetryParser.parse(it.raw)
            if (traffic.hasAnyEvidence) root.put("traffic", trafficObject(traffic))
        }
        runtime?.let { root.put("runtime_capabilities", capabilityArray(it)) }
        stability?.let { root.put("connection_stability", stabilityObject(it)) }

        val samples = JSONArray()
        history.takeLast(MAX_HISTORY_SAMPLES).forEach { samples.put(sampleObject(it)) }
        root.put("telemetry_history", samples)

        return root.toString(2)
    }

    private fun liveObject(snapshot: RouterSnapshot): JSONObject = JSONObject().apply {
        putOptional("model", snapshot.model)
        putOptional("firmware", snapshot.firmware)
        putOptional("hardware_version", snapshot.hardwareVersion)
        putOptional("network_type", snapshot.networkType)
        putOptional("lte_band", snapshot.lteBand)
        putOptional("nr_band", snapshot.nrBand)
        putOptional("lte_rsrp", snapshot.lteRsrp)
        putOptional("lte_rsrq", snapshot.lteRsrq)
        putOptional("lte_sinr", snapshot.lteSinr)
        putOptional("nr_rsrp", snapshot.nrRsrp)
        putOptional("nr_sinr", snapshot.nrSinr)
        putOptional("pci", snapshot.pci)
        putOptional("earfcn", snapshot.earfcn)
        put("ca_active", snapshot.caActive)
        put("carrier_count", snapshot.cells.size)
        put("nr_verified", snapshot.raw["_zte_nr_active_verified"].equals("true", ignoreCase = true))
        put("ca_verified", snapshot.raw["_zte_ca_verified"].equals("true", ignoreCase = true))

        val carriers = JSONArray()
        snapshot.cells.forEach { cell ->
            carriers.put(JSONObject().apply {
                put("role", cell.role.name)
                putOptional("band", cell.band)
                putOptional("pci", cell.pci)
                putOptional("arfcn", cell.arfcn)
                putOptional("bandwidth_mhz", cell.bandwidthMhz)
            })
        }
        put("carriers", carriers)
    }

    private fun trafficObject(traffic: TrafficTelemetry): JSONObject = JSONObject().apply {
        putOptional("rx_bytes_per_second", traffic.rxBytesPerSecond)
        putOptional("tx_bytes_per_second", traffic.txBytesPerSecond)
        putOptional("session_rx_bytes", traffic.sessionRxBytes)
        putOptional("session_tx_bytes", traffic.sessionTxBytes)
        putOptional("session_seconds", traffic.sessionSeconds)
        putOptional("monthly_rx_bytes", traffic.monthlyRxBytes)
        putOptional("monthly_tx_bytes", traffic.monthlyTxBytes)
        putOptional("monthly_seconds", traffic.monthlySeconds)
        putOptional("month_marker", traffic.monthMarker)
    }

    private fun capabilityArray(report: RuntimeCapabilityReport): JSONArray = JSONArray().apply {
        RuntimeCapabilityDetailsPresenter.from(report).forEach { detail ->
            put(JSONObject().apply {
                put("key", detail.key)
                put("title", detail.title)
                put("access", detail.accessKind)
                put("state", detail.state.name)
                put("state_label", detail.stateLabel)
                put("reason", detail.reason)
                put("evidence_fields", JSONArray(detail.evidenceFields))
            })
        }
    }

    private fun stabilityObject(report: ConnectionStabilityReport): JSONObject = JSONObject().apply {
        put("sample_count", report.sampleCount)
        put("level", report.level.name)
        putOptional("score", report.score)
        putOptional("cell_stability_percent", report.cellStabilityPercent)
        putOptional("mode_stability_percent", report.modeStabilityPercent)
        putOptional("signal_stability_percent", report.signalStabilityPercent)
        putOptional("nr_active_percent", report.nrActivePercent)
        putOptional("ca_active_percent", report.caActivePercent)
        put("cell_switches", report.cellSwitches)
        put("mode_switches", report.modeSwitches)
        put("summary", report.summary)
    }

    private fun sampleObject(sample: SafeTelemetrySample): JSONObject = JSONObject().apply {
        put("timestamp_epoch_ms", sample.timestampEpochMs)
        putOptional("network_type", sample.networkType)
        putOptional("lte_band", sample.lteBand)
        putOptional("nr_band", sample.nrBand)
        putOptional("lte_rsrp", sample.lteRsrp)
        putOptional("lte_rsrq", sample.lteRsrq)
        putOptional("lte_sinr", sample.lteSinr)
        putOptional("nr_rsrp", sample.nrRsrp)
        putOptional("nr_sinr", sample.nrSinr)
        putOptional("pci", sample.pci)
        putOptional("earfcn", sample.earfcn)
        put("ca_active", sample.caActive)
        put("carrier_count", sample.carrierCount)
        put("nr_verified", sample.nrVerified)
        put("ca_verified", sample.caVerified)
    }

    private fun JSONObject.putOptional(key: String, value: Any?) {
        if (value != null) put(key, value)
    }

    private const val MAX_HISTORY_SAMPLES = 30
}
