package com.malik.ztesmartmanager.core.model

/**
 * Exact values read from the router immediately before a mutating operation.
 *
 * A null value means the firmware did not expose that field, while an empty
 * string is a real value returned by the firmware and must not be collapsed
 * into null. Passwords and authentication tokens are intentionally excluded.
 */
data class RouterSettingsBackup(
    val createdAtEpochMs: Long,
    val routerAddress: String,
    val profileId: String,
    val modelFamily: String,
    val model: String? = null,
    val firmware: String? = null,
    val hardwareVersion: String? = null,
    val lteBandLock: String? = null,
    val nrSaBandLock: String? = null,
    val nrNsaBandLock: String? = null,
    val ltePciLock: String? = null,
    val lteEarfcnLock: String? = null,
    val bearerPreference: String? = null
) {
    val hasAnyRestorableRadioSetting: Boolean
        get() = listOf(
            lteBandLock,
            nrSaBandLock,
            nrNsaBandLock,
            ltePciLock,
            lteEarfcnLock,
            bearerPreference
        ).any { it != null }
}

data class RouterRestoreStep(
    val setting: String,
    val result: OperationResult
)

data class RouterRestoreReport(
    val steps: List<RouterRestoreStep>
) {
    val success: Boolean get() = steps.isNotEmpty() && steps.all { it.result.success }
    val verified: Boolean get() = success && steps.all { it.result.verified }

    val message: String
        get() = when {
            steps.isEmpty() -> "لا توجد إعدادات قابلة للاستعادة في النسخة المحفوظة"
            verified -> "تمت استعادة الإعدادات المحفوظة والتحقق من جميع القيم"
            success -> "تم إرسال الاستعادة، لكن بعض القيم لم يعطِ الراوتر read-back موثوقًا لها"
            else -> {
                val failed = steps.filterNot { it.result.success }.joinToString("، ") { it.setting }
                "تعذر استعادة بعض الإعدادات: $failed"
            }
        }
}
