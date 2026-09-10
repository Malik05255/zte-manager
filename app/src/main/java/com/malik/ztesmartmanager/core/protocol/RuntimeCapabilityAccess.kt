package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.RuntimeCapabilityEvidence
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState

enum class RuntimeAction {
    LTE_BAND_WRITE,
    NR_BAND_WRITE,
    CELL_LOCK_WRITE,
    NETWORK_MODE_WRITE,
    NEIGHBOR_SCAN,
    ANTENNA_WRITE,
    CA_READ
}

data class RuntimeActionDecision(
    val allowed: Boolean,
    val readOnly: Boolean,
    val state: RuntimeCapabilityState,
    val reason: String,
    val evidenceFields: Set<String>
)

object RuntimeCapabilityAccess {
    fun decide(report: RuntimeCapabilityReport, action: RuntimeAction): RuntimeActionDecision {
        val evidence = evidence(report, action)
        val isReadAction = action == RuntimeAction.NEIGHBOR_SCAN || action == RuntimeAction.CA_READ
        val allowed = if (isReadAction) evidence.hasRuntimeEvidence else evidence.canAttemptWrite
        return RuntimeActionDecision(
            allowed = allowed,
            readOnly = isReadAction && allowed,
            state = evidence.state,
            reason = evidence.reason,
            evidenceFields = evidence.evidenceFields
        )
    }

    fun writeReadyCount(report: RuntimeCapabilityReport): Int = listOf(
        RuntimeAction.LTE_BAND_WRITE,
        RuntimeAction.NR_BAND_WRITE,
        RuntimeAction.CELL_LOCK_WRITE,
        RuntimeAction.NETWORK_MODE_WRITE
    ).count { decide(report, it).allowed }

    fun writeActionCount(): Int = 4

    private fun evidence(report: RuntimeCapabilityReport, action: RuntimeAction): RuntimeCapabilityEvidence = when (action) {
        RuntimeAction.LTE_BAND_WRITE -> report.lteBandControl
        RuntimeAction.NR_BAND_WRITE -> report.nrBandControl
        RuntimeAction.CELL_LOCK_WRITE -> report.cellLock
        RuntimeAction.NETWORK_MODE_WRITE -> report.networkMode
        RuntimeAction.NEIGHBOR_SCAN -> report.neighborScan
        RuntimeAction.ANTENNA_WRITE -> report.antennaControl
        RuntimeAction.CA_READ -> report.carrierAggregationTelemetry
    }
}
