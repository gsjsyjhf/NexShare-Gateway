package com.nexshare.gateway.data.sharing

import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class Socks5ProxyServer(
    private val bind: InetAddress,
    private val port: Int,
    private val timeoutMs: Int,
    private val shouldAccept: (String) -> Boolean,
    private val onOpen: (String) -> Unit,
    private val onClose: (String) -> Unit,
    private val onBytes: (String, Long, Long) -> Unit
) {
    private val running = AtomicBoolean(false)
    private var server: ServerSocket? = null
    private val pool = Executors.newCachedThreadPool { r -> Thread(r, "nex-socks").apply { isDaemon = true } }
    val actualPort: Int get() = server?.localPort ?: port

    fun start() {
        if (running.getAndSet(true)) return
        val ss = ServerSocket(); ss.reuseAddress = true
        ss.bind(InetSocketAddress(bind, port), 128); server = ss
        pool.execute {
            while (running.get()) {
                try { val c = ss.accept(); pool.execute { handle(c) } }
                catch (_: SocketException) { if (!running.get()) break }
                catch (_: Exception) { if (!running.get()) break }
            }
        }
    }
    fun stop() { running.set(false); runCatching { server?.close() }; server = null; pool.shutdownNow(); runCatching { pool.awaitTermination(1, TimeUnit.SECONDS) } }

    private fun handle(client: Socket) {
        val ip = client.inetAddress?.hostAddress ?: "unknown"
        var opened = false
        try {
            client.soTimeout = timeoutMs; client.tcpNoDelay = true
            if (!shouldAccept(ip)) { client.close(); return }
            onOpen(ip); opened = true
            val input = client.getInputStream(); val output = client.getOutputStream()
            val greet = IoPipes.readExact(input, 2) ?: return
            if (greet[0].toInt() != 5) return
            IoPipes.readExact(input, greet[1].toInt() and 0xFF) ?: return
            output.write(byteArrayOf(5, 0)); output.flush()
            val hdr = IoPipes.readExact(input, 4) ?: return
            if (hdr[0].toInt() != 5) return
            val dest = readDest(hdr[3].toInt() and 0xFF, input) ?: return
            val pb = IoPipes.readExact(input, 2) ?: return
            val destPort = ((pb[0].toInt() and 0xFF) shl 8) or (pb[1].toInt() and 0xFF)
            if ((hdr[1].toInt() and 0xFF) != 1) { reply(output, 7); return }
            val remote = Socket()
            try {
                remote.tcpNoDelay = true
                remote.connect(InetSocketAddress(dest, destPort), timeoutMs)
                reply(output, 0)
                client.soTimeout = 0; remote.soTimeout = 0
                IoPipes.splice(client, remote, { n -> onBytes(ip, n.toLong(), 0) }, { n -> onBytes(ip, 0, n.toLong()) }) { running.get() }
            } catch (_: Exception) { reply(output, 5) }
            finally { runCatching { remote.close() } }
        } catch (_: Exception) {
        } finally { if (opened) onClose(ip); runCatching { client.close() } }
    }
    private fun readDest(atyp: Int, input: InputStream): String? = when (atyp) {
        1 -> IoPipes.readExact(input, 4)?.let { InetAddress.getByAddress(it).hostAddress }
        3 -> { val n = input.read(); if (n <= 0) null else IoPipes.readExact(input, n)?.let { String(it, Charsets.US_ASCII) } }
        4 -> IoPipes.readExact(input, 16)?.let { InetAddress.getByAddress(it).hostAddress }
        else -> null
    }
    private fun reply(out: java.io.OutputStream, status: Int) {
        val buf = ByteBuffer.allocate(10)
        buf.put(5); buf.put(status.toByte()); buf.put(0); buf.put(1); buf.put(ByteArray(4)); buf.putShort(0)
        out.write(buf.array()); out.flush()
    }
}
