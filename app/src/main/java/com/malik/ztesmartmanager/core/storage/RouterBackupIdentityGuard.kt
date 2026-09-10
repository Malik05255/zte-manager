package com.malik.ztesmartmanager.core.storage

import com.malik.ztesmartmanager.core.model.RouterSettingsBackup

data class RouterBackupIdentityDecision(
    val allowed: Boolean,
    val reason: String,
    val matchedEvidence: Set<String> = emptySet()
)

/**
 * Fail-closed compatibility gate for persistent safety backups.
 *
 * A profile match alone is not enough: two physical routers can share the same profile and default
 * management address. Before restore, every identity value captured in the backup must be readable
 * again and match the current router. At least one device identity field beyond address/profile is
 * required. Firmware changes are intentionally treated as incompatible because goform semantics can
 * differ across firmware builds.
 */
object RouterBackupIdentityGuard {
    fun verify(
        backup: RouterSettingsBackup,
        currentRouterAddress: String,
        currentProfileId: String,
        currentModel: String?,
        currentHardwareVersion: String?,
        currentFirmware: String?
    ): RouterBackupIdentityDecision {
        if (normalizeAddress(backup.routerAddress) != normalizeAddress(currentRouterAddress)) {
            return blocked("عنوان الراوتر الحالي لا يطابق عنوان النسخة المحفوظة")
        }
        if (!same(backup.profileId, currentProfileId)) {
            return blocked("الـProfile الحالي لا يطابق Profile النسخة المحفوظة")
        }

        val matched = linkedSetOf("router_address", "profile_id")
        val identityChecks = listOf(
            IdentityCheck("model", "Model", backup.model, currentModel),
            IdentityCheck("hardware", "Hardware", backup.hardwareVersion, currentHardwareVersion),
            IdentityCheck("firmware", "Firmware", backup.firmware, currentFirmware)
        )

        val captured = identityChecks.filter { !it.expected.isNullOrBlank() }
        if (captured.isEmpty()) {
            return blocked("النسخة القديمة لا تحتوي هوية جهاز كافية؛ Profile والعنوان وحدهما لا يثبتان أنها تخص هذا الراوتر")
        }

        captured.forEach { check ->
            if (check.actual.isNullOrBlank()) {
                return blocked("النسخة تحتوي ${check.label} لكن الراوتر الحالي لم يعرضه؛ لا يمكن التحقق من الهوية")
            }
            if (!same(check.expected.orEmpty(), check.actual.orEmpty())) {
                return blocked("${check.label} الحالي لا يطابق القيمة المحفوظة؛ أُوقفت الاستعادة قبل أي كتابة")
            }
            matched += check.key
        }

        return RouterBackupIdentityDecision(
            allowed = true,
            reason = "تم التحقق من هوية النسخة والراوتر قبل الاستعادة",
            matchedEvidence = matched
        )
    }

    private data class IdentityCheck(
        val key: String,
        val label: String,
        val expected: String?,
        val actual: String?
    )

    private fun blocked(reason: String) = RouterBackupIdentityDecision(false, reason)

    private fun same(a: String, b: String): Boolean = normalizeIdentity(a) == normalizeIdentity(b)

    private fun normalizeIdentity(value: String): String = value
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()

    private fun normalizeAddress(value: String): String = value
        .trim()
        .lowercase()
        .removePrefix("http://")
        .removePrefix("https://")
        .trimEnd('/')
}
