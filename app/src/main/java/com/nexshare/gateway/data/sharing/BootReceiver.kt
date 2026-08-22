package com.nexshare.gateway.data.sharing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nexshare.gateway.NexShareApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? NexShareApp ?: return
        if (runBlocking { app.settingsRepository.settings.first().autoStart }) SharingService.start(context)
    }
}
