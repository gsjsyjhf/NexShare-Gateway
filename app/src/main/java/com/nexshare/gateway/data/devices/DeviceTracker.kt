package com.nexshare.gateway.data.devices

import com.nexshare.gateway.domain.ConnectedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class DeviceTracker {
    private data class Live(
        val ip: String, var name: String, val firstSeen: Long, var lastSeen: Long,
        var down: Long = 0, var up: Long = 0, var lastDown: Long = 0, var lastUp: Long = 0,
        var downBps: Long = 0, var upBps: Long = 0, var connections: Int = 0, var blocked: Boolean = false
    )
    private val live = ConcurrentHashMap<String, Live>()
    private val _devices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val devices: StateFlow<List<ConnectedDevice>> = _devices.asStateFlow()

    fun shouldAccept(ip: String, max: Int): Boolean {
        val e = live[ip]
        if (e?.blocked == true) return false
        if (e != null) return true
        return live.values.count { !it.blocked } < max
    }
    fun onOpen(ip: String) {
        val now = System.currentTimeMillis()
        live.compute(ip) { _, c ->
            val n = c ?: Live(ip, "Device · ${ip.substringAfterLast('.')}", now, now)
            n.lastSeen = now; n.connections += 1; n
        }; publish()
    }
    fun onClose(ip: String) {
        live.computeIfPresent(ip) { _, c -> c.connections = (c.connections - 1).coerceAtLeast(0); c.lastSeen = System.currentTimeMillis(); c }
        publish()
    }
    fun addBytes(ip: String, down: Long, up: Long) {
        live.computeIfPresent(ip) { _, c -> c.down += down; c.up += up; c.lastSeen = System.currentTimeMillis(); c }
    }
    fun block(ip: String, blocked: Boolean) { live.computeIfPresent(ip) { _, c -> c.blocked = blocked; c }; publish() }
    fun clear() { live.clear(); publish() }
    @Synchronized fun tick() {
        live.values.forEach { d -> d.downBps = (d.down - d.lastDown).coerceAtLeast(0); d.upBps = (d.up - d.lastUp).coerceAtLeast(0); d.lastDown = d.down; d.lastUp = d.up }
        publish()
    }
    private fun publish() {
        _devices.value = live.values.map {
            ConnectedDevice(it.ip, it.ip, it.name, it.firstSeen, it.lastSeen, it.down, it.up, it.downBps, it.upBps, it.connections, it.blocked)
        }.sortedByDescending { it.lastSeenAt }
    }
}
