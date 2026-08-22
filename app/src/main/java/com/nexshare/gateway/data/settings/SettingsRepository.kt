package com.nexshare.gateway.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexshare.gateway.domain.AppLanguage
import com.nexshare.gateway.domain.ShareMode
import com.nexshare.gateway.domain.ThemeMode
import com.nexshare.gateway.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("nexshare_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val AUTO = booleanPreferencesKey("auto_start")
        val HTTP = intPreferencesKey("http_port")
        val SOCKS = intPreferencesKey("socks_port")
        val TIMEOUT = intPreferencesKey("timeout_seconds")
        val MAX = intPreferencesKey("max_devices")
        val MODE = stringPreferencesKey("share_mode")
        val THEME = stringPreferencesKey("theme_mode")
        val LANG = stringPreferencesKey("language")
        val DNS = stringPreferencesKey("custom_dns")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            autoStart = p[K.AUTO] ?: false,
            httpPort = p[K.HTTP] ?: 8080,
            socksPort = p[K.SOCKS] ?: 1080,
            timeoutSeconds = p[K.TIMEOUT] ?: 30,
            maxDevices = p[K.MAX] ?: 8,
            shareMode = runCatching { ShareMode.valueOf(p[K.MODE] ?: "WIFI_HOTSPOT") }.getOrDefault(ShareMode.WIFI_HOTSPOT),
            themeMode = runCatching { ThemeMode.valueOf(p[K.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            language = runCatching { AppLanguage.valueOf(p[K.LANG] ?: "SYSTEM") }.getOrDefault(AppLanguage.SYSTEM),
            customDns = p[K.DNS] ?: "1.1.1.1"
        )
    }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.dataStore.edit { prefs ->
            val cur = UserSettings(
                autoStart = prefs[K.AUTO] ?: false,
                httpPort = prefs[K.HTTP] ?: 8080,
                socksPort = prefs[K.SOCKS] ?: 1080,
                timeoutSeconds = prefs[K.TIMEOUT] ?: 30,
                maxDevices = prefs[K.MAX] ?: 8,
                shareMode = runCatching { ShareMode.valueOf(prefs[K.MODE] ?: "WIFI_HOTSPOT") }.getOrDefault(ShareMode.WIFI_HOTSPOT),
                themeMode = runCatching { ThemeMode.valueOf(prefs[K.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
                language = runCatching { AppLanguage.valueOf(prefs[K.LANG] ?: "SYSTEM") }.getOrDefault(AppLanguage.SYSTEM),
                customDns = prefs[K.DNS] ?: "1.1.1.1"
            )
            val n = transform(cur)
            prefs[K.AUTO] = n.autoStart
            prefs[K.HTTP] = n.httpPort
            prefs[K.SOCKS] = n.socksPort
            prefs[K.TIMEOUT] = n.timeoutSeconds
            prefs[K.MAX] = n.maxDevices
            prefs[K.MODE] = n.shareMode.name
            prefs[K.THEME] = n.themeMode.name
            prefs[K.LANG] = n.language.name
            prefs[K.DNS] = n.customDns
        }
    }
}
