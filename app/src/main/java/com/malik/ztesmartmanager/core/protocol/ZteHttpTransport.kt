package com.malik.ztesmartmanager.core.protocol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ZteHttpTransport(routerAddress: String) {
    private val baseUrl = normalizeBaseUrl(routerAddress)
    private val cookies = linkedMapOf<String, String>()
    private val sparseFieldCache = linkedMapOf<String, CachedSparseField>()
    private val sparseFieldLastAttemptMs = linkedMapOf<String, Long>()

    /**
     * ZTE firmware is inconsistent with a few large/list-valued fields when they are requested through
     * `multi_data=1`: some builds return the field empty even though the same `cmd` works when requested
     * alone. Keep the workaround at transport level so every snapshot reader benefits without opening a
     * second login/session or fabricating data.
     *
     * Only read-only fields with observed sparse multi-data behaviour are retried. A successful single
     * field response is merged verbatim into the original JSON object. Retries are rate-limited because
     * snapshots can refresh several times per second in placement mode; this prevents client-list probing
     * from hammering the router when an empty string legitimately means "no connected devices".
     */
    suspend fun getJson(path: String, params: Map<String, String>): JSONObject {
        val primary = JSONObject(get(path, params))
        if (params["multi_data"] != "1") return primary

        val requested = params["cmd"].orEmpty()
            .split(',')
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        val now = System.currentTimeMillis()

        SPARSE_MULTI_DATA_FIELDS
            .asSequence()
            .filter { it in requested }
            .forEach { field ->
                val directValue = primary.opt(field)
                if (hasUsableValue(primary, field)) {
                    if (directValue != null) sparseFieldCache[field] = CachedSparseField(directValue, now)
                    return@forEach
                }

                sparseFieldCache[field]
                    ?.takeIf { now - it.savedAtEpochMs <= SPARSE_FIELD_CACHE_TTL_MS }
                    ?.let { primary.put(field, it.value) }

                val lastAttempt = sparseFieldLastAttemptMs[field] ?: 0L
                if (now - lastAttempt < SPARSE_FIELD_RETRY_MS) return@forEach
                sparseFieldLastAttemptMs[field] = now

                val singleParams = params.toMutableMap().apply {
                    this["cmd"] = field
                    remove("multi_data")
                }
                val single = runCatching { JSONObject(get(path, singleParams)) }.getOrNull()
                if (single != null && single.has(field) && !single.isNull(field)) {
                    val value = single.opt(field)
                    if (value != null) {
                        primary.put(field, value)
                        sparseFieldCache[field] = CachedSparseField(value, now)
                    }
                }
            }

        return primary
    }

    suspend fun get(path: String, params: Map<String, String>): String = withContext(Dispatchers.IO) {
        val query = encodeForm(params)
        val url = URL("$baseUrl$path${if (query.isBlank()) "" else "?$query"}")
        execute(url, "GET", null)
    }

    suspend fun postForm(path: String, params: Map<String, String>): String = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl$path")
        execute(url, "POST", encodeForm(params))
    }

    private fun execute(url: URL, method: String, body: String?): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 7_000
            useCaches = false
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("Referer", "$baseUrl/")
            setRequestProperty("X-Requested-With", "XMLHttpRequest")
            setRequestProperty("Cookie", cookieHeader())

            if (method == "POST") {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
        }

        if (body != null) {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
        }

        captureCookies(connection)
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

        if (code !in 200..299) {
            throw ZteTransportException("HTTP $code", code, text)
        }
        return text
    }

    private fun captureCookies(connection: HttpURLConnection) {
        connection.headerFields["Set-Cookie"].orEmpty().forEach { rawCookie ->
            val firstPart = rawCookie.substringBefore(';')
            val separator = firstPart.indexOf('=')
            if (separator > 0) {
                cookies[firstPart.substring(0, separator).trim()] = firstPart.substring(separator + 1).trim()
            }
        }
    }

    private fun cookieHeader(): String {
        if (cookies.isEmpty()) return "stok="
        return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun encodeForm(values: Map<String, String>): String = values.entries.joinToString("&") {
        "${encode(it.key)}=${encode(it.value)}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun normalizeBaseUrl(address: String): String {
        val cleaned = address.trim().removeSuffix("/")
        return when {
            cleaned.startsWith("http://", ignoreCase = true) -> cleaned
            cleaned.startsWith("https://", ignoreCase = true) -> cleaned
            else -> "http://$cleaned"
        }
    }

    private fun hasUsableValue(json: JSONObject, field: String): Boolean {
        if (!json.has(field) || json.isNull(field)) return false
        val value = json.opt(field) ?: return false
        if (value !is String) return true
        val normalized = value.trim()
        return normalized.isNotEmpty() && !normalized.equals("null", true)
    }

    private data class CachedSparseField(val value: Any, val savedAtEpochMs: Long)

    companion object {
        /** Read-only client lists observed to require an individual GET on some MC-series firmware. */
        private val SPARSE_MULTI_DATA_FIELDS = setOf("station_list", "lan_station_list")
        private const val SPARSE_FIELD_RETRY_MS = 10_000L
        private const val SPARSE_FIELD_CACHE_TTL_MS = 15_000L
    }
}

class ZteTransportException(
    message: String,
    val statusCode: Int,
    val responseBody: String
) : Exception(message)
