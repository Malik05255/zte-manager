package com.malik.ztesmartmanager.core.storage

import android.content.Context
import com.malik.ztesmartmanager.core.tower.PersistedTowerGuardState
import com.malik.ztesmartmanager.core.tower.PersistedTowerGuardStateCodec
import com.malik.ztesmartmanager.core.tower.TowerTarget

/** Local-only persisted Tower Guard intent keyed by the exact router address. */
class TowerGuardStateStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun save(state: PersistedTowerGuardState) {
        preferences.edit()
            .putString(key(state.routerAddress), PersistedTowerGuardStateCodec.encode(state))
            .apply()
    }

    fun saveVerifiedTarget(
        routerAddress: String,
        profileId: String,
        target: TowerTarget,
        guardRequested: Boolean
    ) {
        save(
            PersistedTowerGuardState(
                routerAddress = routerAddress,
                profileId = profileId,
                target = target,
                guardRequested = guardRequested,
                savedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    fun load(routerAddress: String): PersistedTowerGuardState? =
        PersistedTowerGuardStateCodec.decode(preferences.getString(key(routerAddress), null))

    fun setGuardRequested(routerAddress: String, requested: Boolean) {
        val existing = load(routerAddress) ?: return
        save(existing.copy(guardRequested = requested, savedAtEpochMs = System.currentTimeMillis()))
    }

    fun clear(routerAddress: String) {
        preferences.edit().remove(key(routerAddress)).apply()
    }

    private fun key(routerAddress: String): String = "router_$routerAddress"

    companion object {
        private const val PREFS_NAME = "zte_verified_tower_guard_state_v1"
    }
}
