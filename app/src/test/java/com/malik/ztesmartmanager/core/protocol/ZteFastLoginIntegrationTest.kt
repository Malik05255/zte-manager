package com.malik.ztesmartmanager.core.protocol

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class ZteFastLoginIntegrationTest {
    @Test
    fun verifiedBootstrapUsesExactlyThreeRouterRoundTrips() = runBlocking {
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

            val bootstrap = client.loginAndReadSnapshot("test-value")

            assertEquals("zte-mc801a", bootstrap.profile.id)
            assertEquals(-90.0, bootstrap.snapshot.lteRsrp ?: Double.NaN, 0.001)
            assertEquals("login bootstrap must use two GETs total", 2, getCount.get())
            assertEquals("login bootstrap must use one POST total", 1, postCount.get())
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
                val count = getCount.incrementAndGet()
                if (count == 1) {
                    """{"LD":"ABC123","wa_inner_version":"FW1","cr_version":"CR1","RD":"RD1"}"""
                } else {
                    """{"model_name":"MC801A","hardware_version":"HW1","wa_inner_version":"FW1","loginfo":"ok","network_type":"LTE","lte_rsrp":"-90","lte_rsrq":"-10","lte_snr":"12","lte_pci":"00A","wan_active_channel":"1300","wan_active_band":"3"}"""
                }
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
