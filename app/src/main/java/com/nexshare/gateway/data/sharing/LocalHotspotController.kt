package com.nexshare.gateway.data.sharing

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LocalHotspotInfo(val ssid: String?, val password: String?)

class LocalHotspotController(context: Context) {
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    @SuppressLint("MissingPermission")
    suspend fun start(): Result<LocalHotspotInfo> = suspendCancellableCoroutine { cont ->
        if (reservation != null) {
            val r = reservation!!
            if (cont.isActive) cont.resume(Result.success(infoOf(r)))
            return@suspendCancellableCoroutine
        }
        val cb = object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                reservation = res
                if (cont.isActive) cont.resume(Result.success(infoOf(res)))
            }
            override fun onFailed(reason: Int) {
                if (cont.isActive) cont.resume(Result.failure(IllegalStateException("Local hotspot failed: $reason")))
            }
            override fun onStopped() { reservation = null }
        }
        try { wifi.startLocalOnlyHotspot(cb, Handler(Looper.getMainLooper())) }
        catch (t: Throwable) { if (cont.isActive) cont.resume(Result.failure(t)) }
    }

    fun stop() { runCatching { reservation?.close() }; reservation = null }

    private fun infoOf(res: WifiManager.LocalOnlyHotspotReservation): LocalHotspotInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            LocalHotspotInfo(res.softApConfiguration.ssid, res.softApConfiguration.passphrase)
        } else {
            @Suppress("DEPRECATION")
            LocalHotspotInfo(res.wifiConfiguration?.SSID, res.wifiConfiguration?.preSharedKey)
        }
    }
}
