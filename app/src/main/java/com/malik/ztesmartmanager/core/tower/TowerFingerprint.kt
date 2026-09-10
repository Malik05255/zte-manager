package com.malik.ztesmartmanager.core.tower

import org.json.JSONArray
import org.json.JSONObject

data class TowerFingerprint(
    val createdAtEpochMs: Long,
    val routerAddress: String,
    val profileId: String,
    val pci: Int,
    val earfcn: Int,
    val band: String?,
    val cellId: Long?,
    val enodebId: String?,
    val evidenceScore: Int?,
    val presencePercent: Int?,
    val medianRsrp: Double?
) {
    fun toTarget(): TowerTarget = TowerTarget(
        pci = pci,
        earfcn = earfcn,
        band = band,
        cellId = cellId,
        enodebId = enodebId
    )
}

/**
 * Pure codec so fingerprints can be tested on the JVM. A fingerprint is radio identity only;
 * it contains no password, cookies, auth tokens, IMSI/IMEI or geographic coordinates.
 */
object TowerFingerprintCodec {
    fun encodeHistory(items: List<TowerFingerprint>): String {
        val array = JSONArray()
        items.forEach { array.put(toJson(it)) }
        return array.toString()
    }

    fun decodeHistory(text: String?): List<TowerFingerprint> {
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

    fun toJson(item: TowerFingerprint): JSONObject = JSONObject().apply {
        put("createdAtEpochMs", item.createdAtEpochMs)
        put("routerAddress", item.routerAddress)
        put("profileId", item.profileId)
        put("pci", item.pci)
        put("earfcn", item.earfcn)
        putNullable("band", item.band)
        putNullable("cellId", item.cellId)
        putNullable("enodebId", item.enodebId)
        putNullable("evidenceScore", item.evidenceScore)
        putNullable("presencePercent", item.presencePercent)
        putNullable("medianRsrp", item.medianRsrp)
    }

    fun fromJson(json: JSONObject): TowerFingerprint? = runCatching {
        val pci = json.getInt("pci")
        val earfcn = json.getInt("earfcn")
        if (pci !in 0..503 || earfcn <= 0) return null
        TowerFingerprint(
            createdAtEpochMs = json.getLong("createdAtEpochMs"),
            routerAddress = json.getString("routerAddress"),
            profileId = json.getString("profileId"),
            pci = pci,
            earfcn = earfcn,
            band = json.nullableString("band"),
            cellId = json.nullableLong("cellId"),
            enodebId = json.nullableString("enodebId"),
            evidenceScore = json.nullableInt("evidenceScore"),
            presencePercent = json.nullableInt("presencePercent"),
            medianRsrp = json.nullableDouble("medianRsrp")
        )
    }.getOrNull()

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.nullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private fun JSONObject.nullableInt(key: String): Int? =
        if (!has(key) || isNull(key)) null else optInt(key)

    private fun JSONObject.nullableDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key)
}
