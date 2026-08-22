package com.nexshare.gateway.data.sharing

import com.nexshare.gateway.domain.ShareMode
import com.nexshare.gateway.domain.ShareRuntime
import com.nexshare.gateway.domain.SharingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SharingState {
    private val _runtime = MutableStateFlow(ShareRuntime())
    val runtime: StateFlow<ShareRuntime> = _runtime.asStateFlow()
    fun setStarting(mode: ShareMode, http: Int, socks: Int) {
        _runtime.value = ShareRuntime(SharingStatus.STARTING, httpPort = http, socksPort = socks, mode = mode)
    }
    fun setActive(bind: String?, http: Int, socks: Int, mode: ShareMode) {
        _runtime.update { it.copy(status = SharingStatus.ACTIVE, bindAddress = bind, httpPort = http, socksPort = socks, mode = mode, errorMessage = null) }
    }
    fun setLocalAp(ssid: String?, password: String?) { _runtime.update { it.copy(localSsid = ssid, localPassword = password) } }
    fun setError(msg: String) { _runtime.update { it.copy(status = SharingStatus.ERROR, errorMessage = msg) } }
    fun setOff() { _runtime.value = ShareRuntime() }
    fun updateBind(ip: String?) { _runtime.update { it.copy(bindAddress = ip ?: it.bindAddress) } }
}
