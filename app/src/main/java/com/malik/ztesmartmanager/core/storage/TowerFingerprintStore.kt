package com.malik.ztesmartmanager.core.storage

import android.content.Context
import com.malik.ztesmartmanager.core.tower.TowerFingerprint
import com.malik.ztesmartmanager.core.tower.TowerFingerprintCodec

/**
 * Local-only history of verified radio-cell fingerprints. No credentials, SIM identifiers or
 * geographic coordinates are stored here.
 */
class TowerFingerprintStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun save(item: TowerFingerprint) {
        val history = loadAll().toMutableList()
        history.add(0, item)
        val trimmed = history
            .distinctBy { Triple(it.routerAddress, it.pci, it.earfcn) }
            .take(MAX_FINGERPRINTS)
        preferences.edit().putString(KEY_HISTORY, TowerFingerprintCodec.encodeHistory(trimmed)).apply()
    }

    fun latest(routerAddress: String, profileId: String? = null): TowerFingerprint? =
        loadAll().firstOrNull {
            it.routerAddress == routerAddress && (profileId == null || it.profileId == profileId)
        }

    fun loadAll(): List<TowerFingerprint> =
        TowerFingerprintCodec.decodeHistory(preferences.getString(KEY_HISTORY, null))

    companion object {
        private const val PREFS_NAME = "zte_verified_tower_fingerprints"
        private const val KEY_HISTORY = "history_v1"
        private const val MAX_FINGERPRINTS = 12
    }
}
