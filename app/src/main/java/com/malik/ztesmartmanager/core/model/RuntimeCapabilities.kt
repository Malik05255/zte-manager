package com.malik.ztesmartmanager.core.model

enum class RuntimeCapabilityState {
    SAFE_TO_ATTEMPT,
    READ_ONLY,
    PROFILE_ONLY,
    UNAVAILABLE
}

data class RuntimeCapabilityEvidence(
    val state: RuntimeCapabilityState,
    val reason: String,
    val evidenceFields: Set<String> = emptySet()
) {
    val canAttemptWrite: Boolean get() = state == RuntimeCapabilityState.SAFE_TO_ATTEMPT
    val hasRuntimeEvidence: Boolean
        get() = state == RuntimeCapabilityState.SAFE_TO_ATTEMPT || state == RuntimeCapabilityState.READ_ONLY
}

data class RuntimeCapabilityReport(
    val lteBandControl: RuntimeCapabilityEvidence,
    val nrBandControl: RuntimeCapabilityEvidence,
    val cellLock: RuntimeCapabilityEvidence,
    val networkMode: RuntimeCapabilityEvidence,
    val neighborScan: RuntimeCapabilityEvidence,
    val antennaControl: RuntimeCapabilityEvidence,
    val carrierAggregationTelemetry: RuntimeCapabilityEvidence,
    val probedAtEpochMs: Long
) {
    val safeWriteCount: Int
        get() = listOf(lteBandControl, nrBandControl, cellLock, networkMode, antennaControl)
            .count { it.canAttemptWrite }
}
