package com.nexshare.gateway.data.sharing

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

internal object IoPipes {
    fun readLine(input: InputStream, limit: Int = 8192): String? {
        val buf = StringBuilder()
        while (buf.length < limit) {
            val b = input.read()
            if (b == -1) return if (buf.isEmpty()) null else buf.toString()
            if (b == '\n'.code) break
            if (b != '\r'.code) buf.append(b.toChar())
        }
        return buf.toString()
    }
    fun readHeaders(input: InputStream): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            lines += line
            if (lines.size > 80) break
        }
        return lines
    }
    fun headerValue(headers: List<String>, name: String) =
        headers.firstOrNull { it.startsWith("$name:", true) }?.substringAfter(':')?.trim()

    fun pump(from: InputStream, to: OutputStream, onChunk: (Int) -> Unit, running: () -> Boolean) {
        val buf = ByteArray(16 * 1024)
        while (running()) {
            val n = try { from.read(buf) } catch (_: Exception) { break }
            if (n < 0) break
            if (n == 0) continue
            try { to.write(buf, 0, n); to.flush() } catch (_: Exception) { break }
            onChunk(n)
        }
    }
    fun splice(client: Socket, remote: Socket, onDown: (Int) -> Unit, onUp: (Int) -> Unit, running: () -> Boolean) {
        val up = Thread {
            try { pump(client.getInputStream(), remote.getOutputStream(), onUp, running) } catch (_: Exception) {}
            finally { runCatching { remote.shutdownOutput() } }
        }.also { it.isDaemon = true; it.start() }
        try { pump(remote.getInputStream(), client.getOutputStream(), onDown, running) } catch (_: Exception) {}
        finally { runCatching { client.shutdownOutput() } }
        runCatching { up.join(4000) }
    }
    fun writeAscii(out: OutputStream, text: String) { out.write(text.toByteArray(Charsets.US_ASCII)); out.flush() }
    fun readExact(input: InputStream, n: Int): ByteArray? {
        val buf = ByteArray(n); var off = 0
        while (off < n) { val r = input.read(buf, off, n - off); if (r < 0) return null; off += r }
        return buf
    }
}
