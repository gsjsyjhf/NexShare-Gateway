package com.nexshare.gateway.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexshare.gateway.domain.AppLanguage
import com.nexshare.gateway.domain.AppUiState
import com.nexshare.gateway.domain.ConnectedDevice
import com.nexshare.gateway.domain.ShareMode
import com.nexshare.gateway.domain.SharingStatus
import com.nexshare.gateway.domain.ThemeMode
import com.nexshare.gateway.presentation.MainViewModel
import com.nexshare.gateway.ui.i18n.LocalL10n
import com.nexshare.gateway.ui.util.formatBytes
import com.nexshare.gateway.ui.util.formatDuration
import com.nexshare.gateway.ui.util.formatSpeed

@Composable
fun NexShareRoot(state: AppUiState, vm: MainViewModel) {
    val t = LocalL10n.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                listOf(
                    Triple(t.home, Icons.Outlined.Home, 0),
                    Triple(t.devices, Icons.Outlined.Person, 1),
                    Triple(t.stats, Icons.Outlined.Share, 2),
                    Triple(t.howTo, Icons.Outlined.Info, 3),
                    Triple(t.settings, Icons.Outlined.Settings, 4)
                ).forEach { (label, icon, i) ->
                    NavigationBarItem(tab == i, { tab = i }, { Icon(icon, label) }, label = { Text(label) })
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { cur ->
                when (cur) {
                    0 -> HomeScreen(state, vm::toggleSharing) { tab = 3 }
                    1 -> DevicesScreen(state.devices, vm::disconnectDevice, vm::allowDevice)
                    2 -> StatsScreen(state)
                    3 -> GuideScreen(state)
                    else -> SettingsScreen(state, vm)
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface.copy(0.94f), MaterialTheme.colorScheme.surfaceVariant.copy(0.7f))))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(0.35f), shape)
            .padding(18.dp)
    ) { content() }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.55f)).padding(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HomeScreen(state: AppUiState, onToggle: () -> Unit, onGuide: () -> Unit) {
    val t = LocalL10n.current
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val active = state.share.status == SharingStatus.ACTIVE
    val starting = state.share.status == SharingStatus.STARTING
    val btn by animateColorAsState(if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, label = "b")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(t.appName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(110.dp).background((if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline).copy(0.25f), CircleShape))
            Box(Modifier.size(28.dp).background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
        }
        Text(if (state.network.vpnActive) t.vpnActive else t.vpnOff, color = if (state.network.vpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(when (state.share.status) {
            SharingStatus.ACTIVE -> t.sharingOn
            SharingStatus.STARTING -> t.starting
            SharingStatus.ERROR -> state.share.errorMessage ?: t.error
            else -> t.sharingOff
        }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onToggle, enabled = !starting, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(btn, Color.White)) {
            Icon(Icons.Outlined.Share, null, Modifier.padding(end = 8.dp).size(20.dp))
            Text(if (active) t.stopSharing else if (starting) t.starting else t.startSharing)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(t.connectedDevices, state.devices.count { !it.blocked }.toString(), Modifier.weight(1f))
            Tile(t.download, formatSpeed(state.traffic.downloadBps), Modifier.weight(1f))
            Tile(t.upload, formatSpeed(state.traffic.uploadBps), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(t.consumed, formatBytes(state.traffic.downloadBytes + state.traffic.uploadBytes), Modifier.weight(1f))
            Tile(t.session, formatDuration(state.traffic.sessionStartedAt), Modifier.weight(1f))
            Tile(t.internet, if (state.network.hasInternet) t.online else t.offline, Modifier.weight(1f))
        }
        if (!state.network.vpnActive) { Spacer(Modifier.height(10.dp)); Card { Text(t.warningNoVpn) } }
        Spacer(Modifier.height(12.dp))
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Star, null, tint = MaterialTheme.colorScheme.primary)
                    Text("  ${t.gateway}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(state.network.transportLabel, style = MaterialTheme.typography.labelSmall)
                }
                val host = state.share.bindAddress ?: state.network.gatewayIpv4 ?: "—"
                CopyLine(t.proxyHttp, "$host:${state.share.httpPort}") {
                    clip.setText(AnnotatedString("$host:${state.share.httpPort}"))
                    Toast.makeText(ctx, t.copied, Toast.LENGTH_SHORT).show()
                }
                CopyLine(t.proxySocks, "$host:${state.share.socksPort}") {
                    clip.setText(AnnotatedString("$host:${state.share.socksPort}"))
                    Toast.makeText(ctx, t.copied, Toast.LENGTH_SHORT).show()
                }
                if (state.share.localSsid != null) {
                    CopyLine(t.ssid, state.share.localSsid.orEmpty()) { clip.setText(AnnotatedString(state.share.localSsid.orEmpty())) }
                    CopyLine(t.password, state.share.localPassword.orEmpty()) { clip.setText(AnnotatedString(state.share.localPassword.orEmpty())) }
                }
                Text(t.warningNeedProxy, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    TextButton(onGuide) { Text(t.howTo) }
                    Spacer(Modifier.weight(1f))
                    TextButton({ runCatching { ctx.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) {
                        Text(t.openHotspotSettings)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CopyLine(label: String, value: String, onCopy: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Medium)
        }
        IconButton(onCopy) { Icon(Icons.Outlined.Share, label) }
    }
}

@Composable
private fun DevicesScreen(devices: List<ConnectedDevice>, onDisc: (String) -> Unit, onAllow: (String) -> Unit) {
    val t = LocalL10n.current
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(t.connectedDevices, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        if (devices.isEmpty()) Card { Text(t.noDevices) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(devices, key = { it.id }) { d ->
                Card {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(d.name, fontWeight = FontWeight.SemiBold)
                        Text(d.ip, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${formatSpeed(d.downloadBps)} ↓  ${formatSpeed(d.uploadBps)} ↑  ${formatDuration(d.firstSeenAt)}")
                        if (d.blocked) OutlinedButton({ onAllow(d.ip) }, Modifier.fillMaxWidth()) { Text(t.allow) }
                        else OutlinedButton({ onDisc(d.ip) }, Modifier.fillMaxWidth()) { Text(t.disconnect) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(state: AppUiState) {
    val t = LocalL10n.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(t.stats, style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(t.download, formatSpeed(state.traffic.downloadBps), Modifier.weight(1f))
            Tile(t.upload, formatSpeed(state.traffic.uploadBps), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(t.download, formatBytes(state.traffic.downloadBytes), Modifier.weight(1f))
            Tile(t.upload, formatBytes(state.traffic.uploadBytes), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(t.connectedDevices, state.devices.count { !it.blocked }.toString(), Modifier.weight(1f))
            Tile(t.session, formatDuration(state.traffic.sessionStartedAt), Modifier.weight(1f))
        }
        Card {
            Text(t.vpnStatus, fontWeight = FontWeight.SemiBold)
            Text(if (state.network.vpnActive) t.vpnActive else t.vpnOff)
            Text(state.network.transportLabel)
        }
    }
}

@Composable
private fun GuideScreen(state: AppUiState) {
    val t = LocalL10n.current
    val host = state.share.bindAddress ?: state.network.gatewayIpv4 ?: "192.168.43.1"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(t.howTo, style = MaterialTheme.typography.headlineMedium)
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. ${t.vpnActive}")
                Text("2. Hotspot / USB")
                Text("3. ${t.startSharing}")
                Text("4. Proxy = $host:${state.settings.httpPort}")
            }
        }
        Card {
            Text("${t.proxyHttp}: $host:${state.settings.httpPort}", fontWeight = FontWeight.SemiBold)
            Text("${t.proxySocks}: $host:${state.settings.socksPort}")
            Text("PAC: http://$host:${state.settings.httpPort}/proxy.pac")
            Text("Windows: Settings → Proxy → Manual")
            Text("Android: Wi-Fi → Advanced → Proxy → Manual")
        }
        Card { Text(t.warningNeedProxy); Text(t.privacyBody, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SettingsScreen(state: AppUiState, vm: MainViewModel) {
    val t = LocalL10n.current
    val s = state.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(t.settings, style = MaterialTheme.typography.headlineMedium)
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t.networkMode, fontWeight = FontWeight.SemiBold)
                FilterChip(s.shareMode == ShareMode.WIFI_HOTSPOT, { vm.setMode(ShareMode.WIFI_HOTSPOT) }, { Text(t.modeWifi) })
                FilterChip(s.shareMode == ShareMode.USB_TETHER, { vm.setMode(ShareMode.USB_TETHER) }, { Text(t.modeUsb) })
                FilterChip(s.shareMode == ShareMode.LOCAL_HOTSPOT, { vm.setMode(ShareMode.LOCAL_HOTSPOT) }, { Text(t.modeLocal) })
            }
        }
        Card {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(t.autoStart, fontWeight = FontWeight.SemiBold); Text(t.autoStartHint, style = MaterialTheme.typography.bodyMedium) }
                Switch(s.autoStart, vm::setAutoStart)
            }
            Text("${t.timeout}: ${s.timeoutSeconds} ${t.seconds}")
            Slider(s.timeoutSeconds.toFloat(), { vm.setTimeout(it.toInt()) }, valueRange = 10f..90f)
            Text("${t.maxDevices}: ${s.maxDevices}")
            Slider(s.maxDevices.toFloat(), { vm.setMaxDevices(it.toInt()) }, valueRange = 1f..16f, steps = 14)
        }
        Card {
            Text(t.darkMode, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(s.themeMode == ThemeMode.SYSTEM, { vm.setTheme(ThemeMode.SYSTEM) }, { Text(t.themeSystem) })
                FilterChip(s.themeMode == ThemeMode.DARK, { vm.setTheme(ThemeMode.DARK) }, { Text(t.themeDark) })
                FilterChip(s.themeMode == ThemeMode.LIGHT, { vm.setTheme(ThemeMode.LIGHT) }, { Text(t.themeLight) })
            }
        }
        Card {
            Text(t.language, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(s.language == AppLanguage.SYSTEM, { vm.setLanguage(AppLanguage.SYSTEM) }, { Text(t.langSystem) })
                FilterChip(s.language == AppLanguage.ARABIC, { vm.setLanguage(AppLanguage.ARABIC) }, { Text(t.langAr) })
                FilterChip(s.language == AppLanguage.ENGLISH, { vm.setLanguage(AppLanguage.ENGLISH) }, { Text(t.langEn) })
            }
        }
        Card {
            Text("NexShare 1.0.1", fontWeight = FontWeight.SemiBold)
            Text(t.aboutBody)
            Text(t.privacyBody, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
    }
}
