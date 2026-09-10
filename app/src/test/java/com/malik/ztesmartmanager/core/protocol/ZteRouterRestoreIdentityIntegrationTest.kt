package com.malik.ztesmartmanager.core.protocol

import com.malik.ztesmartmanager.core.model.RouterSettingsBackup
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class ZteRouterRestoreIdentityIntegrationTest {
    @Test
    fun firmwareMismatchStopsRestoreBeforeAnyPostWrite() = runBlocking {
        val getCount = AtomicInteger(0)
        val postCount = AtomicInteger(0)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/goform/") { exchange ->
            handleRouterRequest(exchange, getCount, postCount)
        }
        server.start()

        try {
            val address = "127.0.0.1:${server.address.port}"
            val client = ZteRouterClient(address)
            val backup = RouterSettingsBackup(
                createdAtEpochMs = 1L,
                routerAddress = address,
                profileId = "zte-generic",
                modelFamily = "ZTE Generic",
                model = "GENERIC-ZTE",
                firmware = "FW_OLD",
                hardwareVersion = "HW1",
                lteBandLock = "3"
            )

            val report = client.restoreSettings(backup)

            assertFalse(report.success)
            assertTrue(getCount.get() >= 1)
            assertEquals("identity mismatch must not emit a mutating POST", 0, postCount.get())
            assertTrue(report.steps.single().result.message.contains("Firmware"))
        } finally {
            server.stop(0)
        }
    }

    private fun handleRouterRequest(
        exchange: HttpExchange,
        getCount: AtomicInteger,
        postCount: AtomicInteger
    ) {
        val body = when (exchange.requestMethod.uppercase()) {
            "GET" -> {
                getCount.incrementAndGet()
                """{"model_name":"GENERIC-ZTE","hardware_version":"HW1","wa_inner_version":"FW_CURRENT","loginfo":"ok"}"""
            }
            "POST" -> {
                postCount.incrementAndGet()
                """{"result":"0"}"""
            }
            else -> "{}"
        }.toByteArray(Charsets.UTF_8)

        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }
}
