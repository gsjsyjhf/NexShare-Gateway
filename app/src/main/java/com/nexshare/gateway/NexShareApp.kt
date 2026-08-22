package com.nexshare.gateway

import android.app.Application
import com.nexshare.gateway.data.devices.DeviceTracker
import com.nexshare.gateway.data.network.NetworkMonitor
import com.nexshare.gateway.data.settings.SettingsRepository
import com.nexshare.gateway.data.sharing.LocalHotspotController
import com.nexshare.gateway.data.sharing.SharingState
import com.nexshare.gateway.data.stats.TrafficAccountant

class NexShareApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set
    lateinit var deviceTracker: DeviceTracker
        private set
    lateinit var trafficAccountant: TrafficAccountant
        private set
    lateinit var sharingState: SharingState
        private set
    lateinit var hotspotController: LocalHotspotController
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        networkMonitor = NetworkMonitor(this)
        deviceTracker = DeviceTracker()
        trafficAccountant = TrafficAccountant()
        sharingState = SharingState()
        hotspotController = LocalHotspotController(this)
    }
}
