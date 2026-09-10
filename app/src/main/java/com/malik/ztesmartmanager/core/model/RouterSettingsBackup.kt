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
    val nrBandLock: String? = null,
    val nrSaBandLock: String? = null,
    val nrNsaBandLock: String? = null,
    val ltePciLock: String? = null,
    val lteEarfcnLock: String? = null,
    val bearerPreference: String? = null
) {
    val hasAnyRestorableRadioSetting: Boolean
        get() = listOf(
            lteBandLock,
            nrBandLock,
            nrSaBandLock,
            nrNsaBandLock,
            ltePciLock,
            lteEarfcnLock,
            bearerPreference
        ).any { it != null }

    /** Exact LTE rollback is safe only when the router exposed a non-empty mask. */
    val canRestoreLteBands: Boolean
        get() = lteBandLock?.let { !isEmptyOrZero(it) } == true

    /**
     * The legacy MC801A command restores one shared NR mask. If SA and NSA are
     * exposed separately they must agree before we treat rollback as safe.
     */
    val canRestoreNrBands: Boolean
        get() {
            nrBandLock?.let { return !isEmptyOrZero(it) }
            val split = listOfNotNull(nrSaBandLock, nrNsaBandLock)
            if (split.isEmpty() || split.any(::isEmptyOrZero)) return false
            return split.map(::normalizeBandList).distinct().size == 1
        }

    val hasCompleteCellLockState: Boolean
        get() = ltePciLock != null && lteEarfcnLock != null

    val cellWasUnlocked: Boolean
        get() = hasCompleteCellLockState && isEmptyOrZero(ltePciLock.orEmpty()) && isEmptyOrZero(lteEarfcnLock.orEmpty())

    val canRestoreNetworkMode: Boolean
        get() = !bearerPreference.isNullOrBlank()

    companion object {
        private fun isEmptyOrZero(value: String): Boolean {
            val normalized = value.trim().lowercase()
            return normalized.isBlank() || normalized == "0" || normalized == "0x0"
        }

        private fun normalizeBandList(value: String): String = value
            .replace("%2C", ",", ignoreCase = true)
            .split(',', '+', ';', ' ')
            .mapNotNull { Regex("\\d+").find(it)?.value?.toIntOrNull() }
            .distinct()
            .sorted()
            .joinToString(",")
    }
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
