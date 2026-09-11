package com.malik.ztesmartmanager.core.smart

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.roundToInt

data class NetworkPerformance(
    val latencyMs: Double?,
    val jitterMs: Double?,
    val packetLossPercent: Double?,
    val downloadMbps: Double?,
    val uploadMbps: Double? = null,
    val measuredAtEpochMs: Long? = null
)

/**
 * Lightweight internet probe. It never uploads user content: upload testing sends only generated
 * in-memory bytes. Smart optimization keeps upload disabled unless the caller explicitly asks for it.
 */
class NetworkPerformanceProbe(
    private val latencyUrl: String = "https://connectivitycheck.gstatic.com/generate_204",
    private val downloadUrl: String = "https://speed.cloudflare.com/__down?bytes=750000",
    private val uploadUrl: String = "https://speed.cloudflare.com/__up"
) {
    suspend fun measure(
        includeDownload: Boolean = true,
        includeUpload: Boolean = false
    ): NetworkPerformance = withContext(Dispatchers.IO) {
        val latencySamples = mutableListOf<Double>()
        var failures = 0

        repeat(4) {
            val elapsed = runCatching { timedRequest(latencyUrl, readBody = false) }.getOrNull()
            if (elapsed == null) failures++ else latencySamples += elapsed
        }

        val latency = latencySamples.takeIf { it.isNotEmpty() }?.average()
        val jitter = if (latencySamples.size >= 2) {
            latencySamples.zipWithNext().map { (a, b) -> abs(a - b) }.average()
        } else null
        val loss = failures * 25.0
        val download = if (includeDownload) runCatching { downloadMbps() }.getOrNull() else null
        val upload = if (includeUpload) runCatching { uploadMbps() }.getOrNull() else null

        NetworkPerformance(
            latencyMs = latency?.let(::round1),
            jitterMs = jitter?.let(::round1),
            packetLossPercent = loss,
            downloadMbps = download?.let(::round1),
            uploadMbps = upload?.let(::round1),
            measuredAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun timedRequest(url: String, readBody: Boolean): Double {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 4_000
            requestMethod = "GET"
            useCaches = false
            instanceFollowRedirects = true
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("User-Agent", "ZTE-Smart-Manager/0.1")
        }
        val start = System.nanoTime()
        try {
            connection.responseCode
            if (readBody) {
                BufferedInputStream(connection.inputStream).use { input ->
                    val buffer = ByteArray(8 * 1024)
                    while (input.read(buffer) >= 0) Unit
                }
            }
            return (System.nanoTime() - start) / 1_000_000.0
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadMbps(): Double {
        val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            requestMethod = "GET"
            useCaches = false
            instanceFollowRedirects = true
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("User-Agent", "ZTE-Smart-Manager/0.1")
        }
        val start = System.nanoTime()
        var bytes = 0L
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("Download probe HTTP $code")
            BufferedInputStream(connection.inputStream).use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    bytes += read
                }
            }
        } finally {
            connection.disconnect()
        }
        val seconds = (System.nanoTime() - start) / 1_000_000_000.0
        if (seconds <= 0.0 || bytes <= 0L) error("Invalid download sample")
        return (bytes * 8.0 / 1_000_000.0) / seconds
    }

    private fun uploadMbps(): Double {
        val payload = ByteArray(UPLOAD_BYTES) { index -> ((index * 31 + 17) and 0xFF).toByte() }
        val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            requestMethod = "POST"
            doOutput = true
            useCaches = false
            instanceFollowRedirects = true
            setFixedLengthStreamingMode(payload.size)
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Content-Type", "application/octet-stream")
            setRequestProperty("User-Agent", "ZTE-Smart-Manager/0.1")
        }
        val start = System.nanoTime()
        try {
            connection.outputStream.buffered().use { output -> output.write(payload) }
            val code = connection.responseCode
            if (code !in 200..299) error("Upload probe HTTP $code")
            runCatching { connection.inputStream.close() }
        } finally {
            connection.disconnect()
        }
        val seconds = (System.nanoTime() - start) / 1_000_000_000.0
        if (seconds <= 0.0) error("Invalid upload sample")
        return (payload.size * 8.0 / 1_000_000.0) / seconds
    }

    private fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0

    private companion object {
        const val UPLOAD_BYTES = 500_000
    }
}
