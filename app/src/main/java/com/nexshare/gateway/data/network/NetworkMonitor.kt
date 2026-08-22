package com.nexshare.gateway.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.nexshare.gateway.domain.NetworkSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkMonitor(context: Context) {
    private val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val snapshot: Flow<NetworkSnapshot> = callbackFlow {
        fun emitNow() { trySend(readSnapshot()) }
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(n: Network) = emitNow()
            override fun onLost(n: Network) = emitNow()
            override fun onCapabilitiesChanged(n: Network, c: NetworkCapabilities) = emitNow()
        }
        runCatching { cm.registerDefaultNetworkCallback(cb) }
        runCatching { cm.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), cb) }
        emitNow()
        val t = Thread {
            try { while (!isClosedForSend) { emitNow(); Thread.sleep(2000) } } catch (_: InterruptedException) {}
        }.also { it.isDaemon = true; it.start() }
        awaitClose { runCatching { cm.unregisterNetworkCallback(cb) }; t.interrupt() }
    }.distinctUntilChanged()

    fun readSnapshot(): NetworkSnapshot {
        val active = cm.activeNetwork
        val caps = active?.let { cm.getNetworkCapabilities(it) }
        val all = cm.allNetworks.mapNotNull { cm.getNetworkCapabilities(it) }
        val vpn = all.any { it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } || caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val cellular = all.any { it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } || caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val wifi = all.any { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } || caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val ifaces = inspect()
        val label = when {
            vpn && cellular -> "VPN · Mobile data"
            vpn && wifi -> "VPN · Wi-Fi"
            vpn -> "VPN"
            cellular -> "Mobile data"
            wifi -> "Wi-Fi"
            else -> "Offline"
        }
        return NetworkSnapshot(
            hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            vpnActive = vpn, cellularActive = cellular, wifiActive = wifi,
            transportLabel = label, gatewayIpv4 = ifaces.first, hotspotLikely = ifaces.second, usbLikely = ifaces.third
        )
    }

    private fun inspect(): Triple<String?, Boolean, Boolean> {
        var gw: String? = null
        var hotspot = false
        var usb = false
        val preferred = mutableListOf<String>()
        runCatching {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().forEach { nif ->
                if (!nif.isUp || nif.isLoopback) return@forEach
                val name = nif.name.lowercase()
                val addrs = nif.inetAddresses.toList().filterIsInstance<Inet4Address>().filter { !it.isLoopbackAddress }.mapNotNull { it.hostAddress }
                val isUsb = name.contains("rndis") || name.contains("usb") || name.contains("ncm")
                val isAp = name.contains("ap") || name.contains("swlan") || name.contains("softap") || name == "wlan1" || name.contains("tether")
                if (isUsb && addrs.isNotEmpty()) usb = true
                if (isAp && addrs.isNotEmpty()) hotspot = true
                addrs.forEach { ip ->
                    if (isAp || isUsb) preferred += ip
                    else if (ip.startsWith("192.168.43.") || ip.startsWith("192.168.49.") || ip.startsWith("192.168.137.")) {
                        hotspot = true; preferred += ip
                    } else if (gw == null && (ip.startsWith("192.168.") || ip.startsWith("10."))) gw = ip
                }
            }
        }
        return Triple(preferred.firstOrNull() ?: gw, hotspot, usb)
    }
}
