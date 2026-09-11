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

    suspend fun getJson(path: String, params: Map<String, String>): JSONObject =
        JSONObject(get(path, params))

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
            // This is always a local-router connection. Fail a dead/wrong local address faster while
            // keeping a longer read window for slower firmware responses after TCP is established.
            connectTimeout = 3_500
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

        val code = connection.responseCode
        captureCookies(connection)
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

        if (code !in 200..299) {
            throw ZteTransportException("HTTP $code", code, text)
        }
        return text
    }

    private fun captureCookies(connection: HttpURLConnection) {
        connection.headerFields.entries
            .asSequence()
            .filter { (name, _) -> name?.equals("Set-Cookie", ignoreCase = true) == true }
            .flatMap { (_, values) -> values.orEmpty().asSequence() }
            .forEach { rawCookie ->
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
}

class ZteTransportException(
    message: String,
    val statusCode: Int,
    val responseBody: String
) : Exception(message)
