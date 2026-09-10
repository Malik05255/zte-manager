package com.malik.ztesmartmanager.core.tower

import org.json.JSONObject

data class PersistedTowerGuardState(
    val routerAddress: String,
    val profileId: String,
    val target: TowerTarget,
    val guardRequested: Boolean,
    val savedAtEpochMs: Long
)

enum class TowerGuardResumeKind {
    NONE,
    RESUME_VERIFIED,
    TARGET_VERIFIED_GUARD_OFF,
    CONFIGURED_ONLY,
    CONFIG_UNKNOWN,
    STALE
}

data class TowerGuardResumeDecision(
    val kind: TowerGuardResumeKind,
    val target: TowerTarget?,
    val enableGuard: Boolean,
    val discardPersistedState: Boolean,
    val message: String
)

/** Pure fail-closed verifier used before restoring any Tower Guard UI/runtime state. */
object TowerGuardResumeVerifier {
    fun decide(
        saved: PersistedTowerGuardState?,
        currentRouterAddress: String,
        currentProfileId: String,
        configuredPci: String?,
        configuredEarfcn: String?,
        liveMatch: TowerMatch?
    ): TowerGuardResumeDecision {
        if (saved == null) return TowerGuardResumeDecision(
            TowerGuardResumeKind.NONE, null, false, false, "لا توجد حالة Tower Guard محفوظة"
        )

        if (saved.routerAddress != currentRouterAddress || saved.profileId != currentProfileId) {
            return TowerGuardResumeDecision(
                TowerGuardResumeKind.STALE,
                null,
                false,
                true,
                "حالة Tower Guard المحفوظة تخص راوتر/Profile مختلف؛ تم رفضها"
            )
        }

        val pci = strictConfiguredLockValue(configuredPci)
        val earfcn = strictConfiguredLockValue(configuredEarfcn)
        if (pci == null || earfcn == null) {
            return TowerGuardResumeDecision(
                TowerGuardResumeKind.CONFIG_UNKNOWN,
                null,
                false,
                false,
                "تعذر إثبات Cell Lock الحالي من read-back؛ لن يستأنف حارس البرج"
            )
        }

        if (pci != saved.target.pci || earfcn != saved.target.earfcn) {
            return TowerGuardResumeDecision(
                TowerGuardResumeKind.STALE,
                null,
                false,
                true,
                "Cell Lock في الراوتر لا يطابق الهدف المحفوظ؛ تم اعتبار الحالة المحلية قديمة"
            )
        }

        return when (liveMatch) {
            TowerMatch.MATCHED -> {
                if (saved.guardRequested) {
                    TowerGuardResumeDecision(
                        TowerGuardResumeKind.RESUME_VERIFIED,
                        saved.target,
                        true,
                        false,
                        "تم إثبات القفل والخلية الحية؛ استؤنف حارس البرج داخل التطبيق"
                    )
                } else {
                    TowerGuardResumeDecision(
                        TowerGuardResumeKind.TARGET_VERIFIED_GUARD_OFF,
                        saved.target,
                        false,
                        false,
                        "تم إثبات الهدف المحفوظ والخلية الحية؛ حارس البرج يبقى متوقفًا حسب اختيارك"
                    )
                }
            }
            TowerMatch.RADIO_MATCH_ID_CHANGED -> TowerGuardResumeDecision(
                TowerGuardResumeKind.CONFIGURED_ONLY,
                saved.target,
                false,
                false,
                "قفل PCI/EARFCN مطابق، لكن Cell ID تغيّر؛ لن يستأنف الحارس تلقائيًا"
            )
            TowerMatch.DRIFTED -> TowerGuardResumeDecision(
                TowerGuardResumeKind.CONFIGURED_ONLY,
                saved.target,
                false,
                false,
                "قفل الراوتر مطابق للهدف المحفوظ لكن الخلية الحية مختلفة؛ الحارس متوقف"
            )
            TowerMatch.UNKNOWN, null -> TowerGuardResumeDecision(
                TowerGuardResumeKind.CONFIGURED_ONLY,
                saved.target,
                false,
                false,
                "قفل الراوتر مطابق، لكن الخلية الحية غير كافية للتحقق؛ الحارس متوقف"
            )
        }
    }

    /**
     * null/blank are unknown because JSONObject.optString cannot distinguish an absent field from
     * some firmware returning an empty token. Explicit numeric zero is positive unlocked evidence.
     */
    private fun strictConfiguredLockValue(value: String?): Int? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (text == "0" || text.equals("0x0", true)) return 0
        if (!text.all { it.isDigit() }) return null
        return text.toIntOrNull()?.takeIf { it >= 0 }
    }
}

/** Pure JSON codec. Persists radio identity only; no credentials/SIM identifiers. */
object PersistedTowerGuardStateCodec {
    fun encode(state: PersistedTowerGuardState): String = JSONObject().apply {
        put("routerAddress", state.routerAddress)
        put("profileId", state.profileId)
        put("guardRequested", state.guardRequested)
        put("savedAtEpochMs", state.savedAtEpochMs)
        put("target", JSONObject().apply {
            put("pci", state.target.pci)
            put("earfcn", state.target.earfcn)
            putNullable("band", state.target.band)
            putNullable("cellId", state.target.cellId)
            putNullable("enodebId", state.target.enodebId)
        })
    }.toString()

    fun decode(text: String?): PersistedTowerGuardState? {
        if (text.isNullOrBlank()) return null
        return runCatching {
            val root = JSONObject(text)
            val targetJson = root.getJSONObject("target")
            val pci = targetJson.getInt("pci")
            val earfcn = targetJson.getInt("earfcn")
            if (pci !in 0..503 || earfcn <= 0) return null
            PersistedTowerGuardState(
                routerAddress = root.getString("routerAddress"),
                profileId = root.getString("profileId"),
                target = TowerTarget(
                    pci = pci,
                    earfcn = earfcn,
                    band = targetJson.nullableString("band"),
                    cellId = targetJson.nullableLong("cellId"),
                    enodebId = targetJson.nullableString("enodebId")
                ),
                guardRequested = root.optBoolean("guardRequested", false),
                savedAtEpochMs = root.getLong("savedAtEpochMs")
            )
        }.getOrNull()
    }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.nullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)
}
