package com.nexshare.gateway.ui.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(4)
    val value = bytes / 1024.0.pow(exp.toDouble())
    val unit = arrayOf("KB", "MB", "GB", "TB")[exp - 1]
    return String.format(Locale.US, if (value >= 10) "%.0f %s" else "%.1f %s", value, unit)
}
fun formatSpeed(bps: Long) = "${formatBytes(bps)}/s"
fun formatDuration(start: Long?, now: Long = System.currentTimeMillis()): String {
    if (start == null) return "00:00:00"
    val sec = ((now - start) / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)
}
