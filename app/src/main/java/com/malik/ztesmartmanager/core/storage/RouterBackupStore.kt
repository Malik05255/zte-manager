package com.malik.ztesmartmanager.core.storage

import android.content.Context
import com.malik.ztesmartmanager.core.model.RouterSettingsBackup
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only safety history. It stores router configuration values, never the
 * administration password, LD/RD/AD tokens, cookies, IMSI, IMEI or WAN data.
 */
class RouterBackupStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun save(backup: RouterSettingsBackup) {
        val history = loadAll().toMutableList()
        history.add(0, backup)
        val trimmed = history
            .distinctBy { Triple(it.createdAtEpochMs, it.routerAddress, it.profileId) }
            .take(MAX_BACKUPS)
        preferences.edit().putString(KEY_HISTORY, RouterSettingsBackupCodec.encodeHistory(trimmed)).apply()
    }

    fun latest(routerAddress: String? = null): RouterSettingsBackup? =
        loadAll().firstOrNull { routerAddress == null || it.routerAddress == routerAddress }

    fun loadAll(): List<RouterSettingsBackup> =
        RouterSettingsBackupCodec.decodeHistory(preferences.getString(KEY_HISTORY, null))

    companion object {
        private const val PREFS_NAME = "zte_router_safety_backups"
        private const val KEY_HISTORY = "history_v1"
        private const val MAX_BACKUPS = 8
    }
}

/** Pure codec kept separate so it can be unit-tested on the JVM. */
object RouterSettingsBackupCodec {
    fun encodeHistory(backups: List<RouterSettingsBackup>): String {
        val array = JSONArray()
        backups.forEach { array.put(toJson(it)) }
        return array.toString()
    }

    fun decodeHistory(text: String?): List<RouterSettingsBackup> {
        if (text.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(text)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    fromJson(json)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun toJson(backup: RouterSettingsBackup): JSONObject = JSONObject().apply {
        put("createdAtEpochMs", backup.createdAtEpochMs)
        put("routerAddress", backup.routerAddress)
        put("profileId", backup.profileId)
        put("modelFamily", backup.modelFamily)
        putNullable("model", backup.model)
        putNullable("firmware", backup.firmware)
        putNullable("hardwareVersion", backup.hardwareVersion)
        putNullable("lteBandLock", backup.lteBandLock)
        putNullable("nrSaBandLock", backup.nrSaBandLock)
        putNullable("nrNsaBandLock", backup.nrNsaBandLock)
        putNullable("ltePciLock", backup.ltePciLock)
        putNullable("lteEarfcnLock", backup.lteEarfcnLock)
        putNullable("bearerPreference", backup.bearerPreference)
    }

    fun fromJson(json: JSONObject): RouterSettingsBackup? = runCatching {
        RouterSettingsBackup(
            createdAtEpochMs = json.getLong("createdAtEpochMs"),
            routerAddress = json.getString("routerAddress"),
            profileId = json.getString("profileId"),
            modelFamily = json.getString("modelFamily"),
            model = json.nullableString("model"),
            firmware = json.nullableString("firmware"),
            hardwareVersion = json.nullableString("hardwareVersion"),
            lteBandLock = json.nullableString("lteBandLock"),
            nrSaBandLock = json.nullableString("nrSaBandLock"),
            nrNsaBandLock = json.nullableString("nrNsaBandLock"),
            ltePciLock = json.nullableString("ltePciLock"),
            lteEarfcnLock = json.nullableString("lteEarfcnLock"),
            bearerPreference = json.nullableString("bearerPreference")
        )
    }.getOrNull()

    private fun JSONObject.putNullable(key: String, value: String?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.nullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return getString(key)
    }
}
