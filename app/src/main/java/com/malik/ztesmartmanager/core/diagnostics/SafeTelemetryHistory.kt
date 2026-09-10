package com.malik.ztesmartmanager.core.diagnostics

import com.malik.ztesmartmanager.core.model.RouterSnapshot

data class SafeTelemetrySample(
    val timestampEpochMs: Long,
    val networkType: String?,
    val lteBand: String?,
    val nrBand: String?,
    val lteRsrp: Double?,
    val lteRsrq: Double?,
    val lteSinr: Double?,
    val nrRsrp: Double?,
    val nrSinr: Double?,
    val pci: Int?,
    val earfcn: Int?,
    val caActive: Boolean,
    val carrierCount: Int,
    val nrVerified: Boolean,
    val caVerified: Boolean
)

object SafeTelemetrySampleFactory {
    fun from(snapshot: RouterSnapshot, timestampEpochMs: Long = System.currentTimeMillis()): SafeTelemetrySample =
        SafeTelemetrySample(
            timestampEpochMs = timestampEpochMs,
            networkType = snapshot.networkType,
            lteBand = snapshot.lteBand,
            nrBand = snapshot.nrBand,
            lteRsrp = snapshot.lteRsrp,
            lteRsrq = snapshot.lteRsrq,
            lteSinr = snapshot.lteSinr,
            nrRsrp = snapshot.nrRsrp,
            nrSinr = snapshot.nrSinr,
            pci = snapshot.pci,
            earfcn = snapshot.earfcn,
            caActive = snapshot.caActive,
            carrierCount = snapshot.cells.size,
            nrVerified = snapshot.raw["_zte_nr_active_verified"].equals("true", ignoreCase = true),
            caVerified = snapshot.raw["_zte_ca_verified"].equals("true", ignoreCase = true)
        )
}

/**
 * Small in-memory ring buffer for diagnostics. It deliberately stores only radio telemetry needed
 * to reproduce truth-first decisions; it never retains the raw router map or authentication/SIM data.
 */
class SafeTelemetryHistory(private val capacity: Int = 30) {
    init {
        require(capacity in 5..120) { "capacity must be between 5 and 120" }
    }

    private val samples = ArrayDeque<SafeTelemetrySample>(capacity)

    fun add(sample: SafeTelemetrySample) {
        if (samples.size == capacity) samples.removeFirst()
        samples.addLast(sample)
    }

    fun add(snapshot: RouterSnapshot, timestampEpochMs: Long = System.currentTimeMillis()) {
        add(SafeTelemetrySampleFactory.from(snapshot, timestampEpochMs))
    }

    fun snapshot(): List<SafeTelemetrySample> = samples.toList()

    fun clear() = samples.clear()
}
