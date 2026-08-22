package com.nexshare.gateway.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexshare.gateway.NexShareApp
import com.nexshare.gateway.data.sharing.SharingService
import com.nexshare.gateway.domain.AppLanguage
import com.nexshare.gateway.domain.AppUiState
import com.nexshare.gateway.domain.ShareMode
import com.nexshare.gateway.domain.SharingStatus
import com.nexshare.gateway.domain.ThemeMode
import com.nexshare.gateway.domain.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NexShareApp
    val state = combine(
        app.settingsRepository.settings,
        app.networkMonitor.snapshot,
        app.sharingState.runtime,
        app.trafficAccountant.snapshot,
        app.deviceTracker.devices
    ) { settings, network, share, traffic, devices ->
        val s = if (share.status == SharingStatus.ACTIVE && share.bindAddress == null && network.gatewayIpv4 != null) {
            app.sharingState.updateBind(network.gatewayIpv4)
            share.copy(bindAddress = network.gatewayIpv4)
        } else share
        AppUiState(settings, network, s, traffic, devices)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppUiState())

    fun toggleSharing() {
        val st = state.value.share.status
        if (st == SharingStatus.ACTIVE || st == SharingStatus.STARTING) SharingService.stop(getApplication())
        else SharingService.start(getApplication())
    }
    fun disconnectDevice(ip: String) = app.deviceTracker.block(ip, true)
    fun allowDevice(ip: String) = app.deviceTracker.block(ip, false)
    private fun update(t: (UserSettings) -> UserSettings) { viewModelScope.launch { app.settingsRepository.update(t) } }
    fun setTheme(m: ThemeMode) = update { it.copy(themeMode = m) }
    fun setLanguage(l: AppLanguage) = update { it.copy(language = l) }
    fun setMode(m: ShareMode) = update { it.copy(shareMode = m) }
    fun setAutoStart(v: Boolean) = update { it.copy(autoStart = v) }
    fun setTimeout(s: Int) = update { it.copy(timeoutSeconds = s) }
    fun setMaxDevices(n: Int) = update { it.copy(maxDevices = n) }
}
