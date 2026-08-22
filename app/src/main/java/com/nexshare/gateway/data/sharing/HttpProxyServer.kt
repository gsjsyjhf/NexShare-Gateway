package com.nexshare.gateway.data.sharing

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class HttpProxyServer(
    private val bind: InetAddress,
    private val port: Int,
    private val timeoutMs: Int,
    private val shouldAccept: (String) -> Boolean,
    private val onOpen: (String) -> Unit,
    private val onClose: (String) -> Unit,
    private val onBytes: (String, Long, Long) -> Unit,
    private val localIpv4: () -> String?
) {
    private val running = AtomicBoolean(false)
    private var server: ServerSocket? = null
    private val pool = Executors.newCachedThreadPool { r -> Thread(r, "nex-http").apply { isDaemon = true } }
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
            if (!shouldAccept(ip)) {
                runCatching { IoPipes.writeAscii(client.getOutputStream(), "HTTP/1.1 403 Forbidden\r\nConnection: close\r\nContent-Length: 0\r\n\r\n") }
                return
            }
            onOpen(ip); opened = true
            val input = client.getInputStream(); val output = client.getOutputStream()
            val first = IoPipes.readLine(input) ?: return
            val headers = IoPipes.readHeaders(input)
            if (isLocal(first, headers)) { serveLocal(first, output); return }
            val parts = first.split(" ", limit = 3)
            if (parts.size < 2) return
            if (parts[0].equals("CONNECT", true)) handleConnect(client, ip, parts[1])
            else handleForward(client, ip, parts[0], parts[1], parts.getOrNull(2) ?: "HTTP/1.1", headers, input)
        } catch (_: Exception) {
        } finally { if (opened) onClose(ip); runCatching { client.close() } }
    }

    private fun isLocal(line: String, headers: List<String>): Boolean {
        val target = line.split(" ").getOrNull(1) ?: return false
        val host = IoPipes.headerValue(headers, "Host")?.substringBefore(':')
        val local = localIpv4()
        return target.startsWith("/") && (host == null || host == local || host == "127.0.0.1")
    }

    private fun serveLocal(line: String, output: java.io.OutputStream) {
        val path = line.split(" ").getOrNull(1)?.substringBefore("?") ?: "/"
        val ip = localIpv4() ?: "127.0.0.1"
        if (path.contains("pac")) {
            val pac = "function FindProxyForURL(url, host){if(isPlainHostName(host))return \"DIRECT\";return \"PROXY $ip:$port\";}"
            val b = pac.toByteArray()
            IoPipes.writeAscii(output, "HTTP/1.1 200 OK\r\nContent-Type: application/x-ns-proxy-autoconfig\r\nContent-Length: ${b.size}\r\nConnection: close\r\n\r\n")
            output.write(b); output.flush()
        } else {
            val html = """<!doctype html><html lang=ar dir=rtl><meta charset=utf-8><title>NexShare</title>
                <body style="font-family:sans-serif;background:#070b14;color:#e8eefc;padding:24px">
                <h1>NexShare</h1><p>HTTP Proxy: <code>$ip:$port</code></p>
                <p>PAC: <code>http://$ip:$port/proxy.pac</code></p></body></html>""".trimIndent().toByteArray(Charsets.UTF_8)
            IoPipes.writeAscii(output, "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${html.size}\r\nConnection: close\r\n\r\n")
            output.write(html); output.flush()
        }
    }

    private fun handleConnect(client: Socket, ip: String, target: String) {
        val host = target.substringBeforeLast(':').removePrefix("[").removeSuffix("]")
        val destPort = target.substringAfterLast(':', "443").toIntOrNull() ?: 443
        val remote = Socket()
        try {
            remote.tcpNoDelay = true; remote.soTimeout = timeoutMs
            remote.connect(InetSocketAddress(host, destPort), timeoutMs)
            IoPipes.writeAscii(client.getOutputStream(), "HTTP/1.1 200 Connection Established\r\nProxy-Agent: NexShare\r\n\r\n")
            client.soTimeout = 0; remote.soTimeout = 0
            IoPipes.splice(client, remote, { n -> onBytes(ip, n.toLong(), 0) }, { n -> onBytes(ip, 0, n.toLong()) }) { running.get() }
        } catch (_: Exception) {
            runCatching { IoPipes.writeAscii(client.getOutputStream(), "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\nConnection: close\r\n\r\n") }
        } finally { runCatching { remote.close() } }
    }

    private fun handleForward(client: Socket, ip: String, method: String, target: String, version: String, headers: List<String>, input: java.io.InputStream) {
        val uriHost: String; val uriPort: Int; val path: String
        if (target.startsWith("http://", true) || target.startsWith("https://", true)) {
            val rest = target.substringAfter("://")
            val auth = rest.substringBefore('/')
            path = "/" + rest.substringAfter('/', "")
            uriHost = auth.substringBefore(':').removePrefix("[").removeSuffix("]")
            uriPort = auth.substringAfter(':', "80").toIntOrNull() ?: 80
        } else {
            val h = IoPipes.headerValue(headers, "Host") ?: return
            uriHost = h.substringBefore(':'); uriPort = h.substringAfter(':', "80").toIntOrNull() ?: 80; path = target
        }
        val cl = IoPipes.headerValue(headers, "Content-Length")?.toIntOrNull() ?: 0
        val body = if (cl in 1 until 8 * 1024 * 1024) IoPipes.readExact(input, cl) ?: ByteArray(0) else ByteArray(0)
        val remote = Socket()
        try {
            remote.tcpNoDelay = true; remote.soTimeout = timeoutMs
            remote.connect(InetSocketAddress(uriHost, uriPort), timeoutMs)
            val filtered = headers.filterNot { it.startsWith("Proxy-", true) }.map { if (it.startsWith("Connection:", true)) "Connection: close" else it }.toMutableList()
            if (filtered.none { it.startsWith("Connection:", true) }) filtered += "Connection: close"
            val req = buildString {
                append("$method $path $version\r\n"); filtered.forEach { append(it).append("\r\n") }; append("\r\n")
            }
            IoPipes.writeAscii(remote.getOutputStream(), req)
            if (body.isNotEmpty()) remote.getOutputStream().write(body)
            onBytes(ip, 0, (req.length + body.size).toLong())
            client.soTimeout = 0; remote.soTimeout = 0
            IoPipes.pump(remote.getInputStream(), client.getOutputStream(), { n -> onBytes(ip, n.toLong(), 0) }) { running.get() }
        } catch (_: Exception) {
            runCatching { IoPipes.writeAscii(client.getOutputStream(), "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 11\r\nConnection: close\r\n\r\nBad Gateway") }
        } finally { runCatching { remote.close() } }
    }
}
