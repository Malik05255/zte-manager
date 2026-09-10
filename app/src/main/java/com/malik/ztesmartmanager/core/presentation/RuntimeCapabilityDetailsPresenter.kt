package com.malik.ztesmartmanager.core.presentation

import com.malik.ztesmartmanager.core.model.RuntimeCapabilityEvidence
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityReport
import com.malik.ztesmartmanager.core.model.RuntimeCapabilityState

data class RuntimeCapabilityDetail(
    val key: String,
    val title: String,
    val accessKind: String,
    val state: RuntimeCapabilityState,
    val stateLabel: String,
    val reason: String,
    val evidenceFields: List<String>
)

object RuntimeCapabilityDetailsPresenter {
    fun from(report: RuntimeCapabilityReport): List<RuntimeCapabilityDetail> = listOf(
        detail("lte", "ترددات 4G LTE", true, report.lteBandControl),
        detail("nr", "ترددات 5G NR", true, report.nrBandControl),
        detail("cell", "تثبيت خلية LTE", true, report.cellLock),
        detail("mode", "وضع الشبكة", true, report.networkMode),
        detail("scan", "مسح الخلايا", false, report.neighborScan),
        detail("antenna", "التحكم بالهوائي", true, report.antennaControl),
        detail("ca", "Carrier Aggregation", false, report.carrierAggregationTelemetry)
    )

    private fun detail(
        key: String,
        title: String,
        writeFeature: Boolean,
        evidence: RuntimeCapabilityEvidence
    ): RuntimeCapabilityDetail {
        val stateLabel = when (evidence.state) {
            RuntimeCapabilityState.SAFE_TO_ATTEMPT -> if (writeFeature) "جاهز للمحاولة" else "متاح للقراءة"
            RuntimeCapabilityState.READ_ONLY -> "قراءة فقط"
            RuntimeCapabilityState.PROFILE_ONLY -> "Profile فقط"
            RuntimeCapabilityState.UNAVAILABLE -> "غير متاح"
        }
        return RuntimeCapabilityDetail(
            key = key,
            title = title,
            accessKind = if (writeFeature) "كتابة مشروطة" else "قراءة",
            state = evidence.state,
            stateLabel = stateLabel,
            reason = evidence.reason,
            evidenceFields = evidence.evidenceFields
                .asSequence()
                .filterNot(::looksSensitive)
                .sorted()
                .toList()
        )
    }

    private fun looksSensitive(field: String): Boolean {
        val normalized = field.lowercase()
        return SENSITIVE_MARKERS.any { it in normalized }
    }

    private val SENSITIVE_MARKERS = setOf(
        "password", "passwd", "token", "auth", "secret",
        "imsi", "imei", "iccid", "puk", "pin_code", "sim_pin"
    )
}
