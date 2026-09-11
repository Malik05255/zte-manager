package com.malik.ztesmartmanager.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address

/** Reads the active Wi-Fi default IPv4 gateway without probing or changing the network. */
object AndroidRouterGatewayDetector {
    fun detect(context: Context): String? {
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
            ?: return null
        val network = manager.activeNetwork ?: return null
        val capabilities = manager.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

        val properties = manager.getLinkProperties(network) ?: return null
        return properties.routes
            .asSequence()
            .filter { it.isDefaultRoute }
            .mapNotNull { it.gateway }
            .filterIsInstance<Inet4Address>()
            .mapNotNull { it.hostAddress }
            .firstOrNull()
    }
}
