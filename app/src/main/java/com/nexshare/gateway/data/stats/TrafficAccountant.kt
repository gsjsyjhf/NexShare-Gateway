package com.nexshare.gateway.data.stats

import com.nexshare.gateway.domain.TrafficSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong

class TrafficAccountant {
    private val down = AtomicLong(0)
    private val up = AtomicLong(0)
    private var sessionStart: Long? = null
    private var lastTs = System.currentTimeMillis()
    private var lastDown = 0L
    private var lastUp = 0L
    private val histDown = ArrayDeque<Float>()
    private val histUp = ArrayDeque<Float>()
    private val _snapshot = MutableStateFlow(TrafficSnapshot())
    val snapshot: StateFlow<TrafficSnapshot> = _snapshot.asStateFlow()

    @Synchronized fun startSession() {
        down.set(0); up.set(0); sessionStart = System.currentTimeMillis(); lastTs = sessionStart!!; lastDown = 0; lastUp = 0
        histDown.clear(); histUp.clear(); _snapshot.value = TrafficSnapshot(sessionStartedAt = sessionStart)
    }
    @Synchronized fun stopSession() { sessionStart = null; _snapshot.value = TrafficSnapshot() }
    fun add(d: Long, u: Long) { if (d != 0L) down.addAndGet(d); if (u != 0L) up.addAndGet(u) }
    @Synchronized fun tick() {
        val now = System.currentTimeMillis()
        val dt = (now - lastTs).coerceAtLeast(1L) / 1000f
        val dNow = down.get(); val uNow = up.get()
        val dBps = ((dNow - lastDown) / dt).toLong().coerceAtLeast(0)
        val uBps = ((uNow - lastUp) / dt).toLong().coerceAtLeast(0)
        lastDown = dNow; lastUp = uNow; lastTs = now
        histDown.addLast(dBps.toFloat()); histUp.addLast(uBps.toFloat())
        while (histDown.size > 36) histDown.removeFirst()
        while (histUp.size > 36) histUp.removeFirst()
        _snapshot.value = TrafficSnapshot(dNow, uNow, dBps, uBps, sessionStart, histDown.toList(), histUp.toList())
    }
}
