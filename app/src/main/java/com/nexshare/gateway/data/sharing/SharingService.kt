package com.nexshare.gateway.data.sharing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.nexshare.gateway.MainActivity
import com.nexshare.gateway.NexShareApp
import com.nexshare.gateway.R
import com.nexshare.gateway.domain.ShareMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetAddress

class SharingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var http: HttpProxyServer? = null
    private var socks: Socks5ProxyServer? = null
    private var tickJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW)
        )
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nexshare:share").apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopInternal(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY
        }
        startInternal(); return START_STICKY
    }

    private fun startInternal() {
        val n = notif("Starting gateway…")
        if (Build.VERSION.SDK_INT >= 34) startForeground(42, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(42, n)
        runCatching { wakeLock?.acquire(6 * 60 * 60 * 1000L) }
        val app = application as NexShareApp
        scope.launch {
            val s = app.settingsRepository.settings.first()
            app.sharingState.setStarting(s.shareMode, s.httpPort, s.socksPort)
            try {
                if (s.shareMode == ShareMode.LOCAL_HOTSPOT) {
                    app.hotspotController.start().onSuccess { app.sharingState.setLocalAp(it.ssid, it.password) }
                        .onFailure { app.sharingState.setError(it.message ?: "Local hotspot failed") }
                    delay(400)
                }
                val snap = app.networkMonitor.readSnapshot()
                val timeout = s.timeoutSeconds.coerceIn(5, 180) * 1000
                val bind = InetAddress.getByName("0.0.0.0")
                val httpS = HttpProxyServer(bind, s.httpPort, timeout,
                    { ip -> app.deviceTracker.shouldAccept(ip, s.maxDevices) },
                    { ip -> app.deviceTracker.onOpen(ip) },
                    { ip -> app.deviceTracker.onClose(ip) },
                    { ip, d, u -> app.deviceTracker.addBytes(ip, d, u); app.trafficAccountant.add(d, u) },
                    { app.networkMonitor.readSnapshot().gatewayIpv4 }
                )
                val socksS = Socks5ProxyServer(bind, s.socksPort, timeout,
                    { ip -> app.deviceTracker.shouldAccept(ip, s.maxDevices) },
                    { ip -> app.deviceTracker.onOpen(ip) },
                    { ip -> app.deviceTracker.onClose(ip) },
                    { ip, d, u -> app.deviceTracker.addBytes(ip, d, u); app.trafficAccountant.add(d, u) }
                )
                httpS.start(); socksS.start(); http = httpS; socks = socksS
                app.trafficAccountant.startSession()
                app.sharingState.setActive(snap.gatewayIpv4, httpS.actualPort, socksS.actualPort, s.shareMode)
                getSystemService(NotificationManager::class.java).notify(42, notif("HTTP ${snap.gatewayIpv4 ?: "0.0.0.0"}:${httpS.actualPort}"))
                tickJob?.cancel()
                tickJob = launch { while (isActive) { app.trafficAccountant.tick(); app.deviceTracker.tick(); delay(1000) } }
            } catch (t: Throwable) {
                app.sharingState.setError(t.message ?: "Failed to start")
                stopInternal(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            }
        }
    }

    private fun stopInternal() {
        tickJob?.cancel(); tickJob = null
        runCatching { http?.stop() }; runCatching { socks?.stop() }; http = null; socks = null
        val app = application as NexShareApp
        app.hotspotController.stop(); app.trafficAccountant.stopSession(); app.deviceTracker.clear(); app.sharingState.setOff()
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
    }

    override fun onDestroy() { stopInternal(); scope.cancel(); super.onDestroy() }

    private fun notif(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, SharingService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_share)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text).setOngoing(true).setContentIntent(open)
            .addAction(0, "Stop", stop).build()
    }

    companion object {
        const val ACTION_STOP = "com.nexshare.gateway.STOP"
        const val CHANNEL = "nexshare_share"
        fun start(ctx: Context) {
            val i = Intent(ctx, SharingService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
        fun stop(ctx: Context) { ctx.startService(Intent(ctx, SharingService::class.java).setAction(ACTION_STOP)) }
    }
}
