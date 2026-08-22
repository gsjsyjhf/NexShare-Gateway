package com.nexshare.gateway.domain

enum class ShareMode { WIFI_HOTSPOT, USB_TETHER, LOCAL_HOTSPOT }
enum class ThemeMode { SYSTEM, DARK, LIGHT }
enum class AppLanguage { SYSTEM, ARABIC, ENGLISH }
enum class SharingStatus { OFF, STARTING, ACTIVE, ERROR }

data class NetworkSnapshot(
    val hasInternet: Boolean = false,
    val vpnActive: Boolean = false,
    val cellularActive: Boolean = false,
    val wifiActive: Boolean = false,
    val transportLabel: String = "Offline",
    val gatewayIpv4: String? = null,
    val hotspotLikely: Boolean = false,
    val usbLikely: Boolean = false
)

data class TrafficSnapshot(
    val downloadBytes: Long = 0L,
    val uploadBytes: Long = 0L,
    val downloadBps: Long = 0L,
    val uploadBps: Long = 0L,
    val sessionStartedAt: Long? = null,
    val historyDown: List<Float> = emptyList(),
    val historyUp: List<Float> = emptyList()
)

data class ConnectedDevice(
    val id: String,
    val ip: String,
    val name: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val downloadBytes: Long,
    val uploadBytes: Long,
    val downloadBps: Long,
    val uploadBps: Long,
    val activeConnections: Int,
    val blocked: Boolean
)

data class ShareRuntime(
    val status: SharingStatus = SharingStatus.OFF,
    val errorMessage: String? = null,
    val bindAddress: String? = null,
    val httpPort: Int = 8080,
    val socksPort: Int = 1080,
    val localSsid: String? = null,
    val localPassword: String? = null,
    val mode: ShareMode = ShareMode.WIFI_HOTSPOT
)

data class UserSettings(
    val autoStart: Boolean = false,
    val httpPort: Int = 8080,
    val socksPort: Int = 1080,
    val timeoutSeconds: Int = 30,
    val maxDevices: Int = 8,
    val shareMode: ShareMode = ShareMode.WIFI_HOTSPOT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val customDns: String = "1.1.1.1"
)

data class AppUiState(
    val settings: UserSettings = UserSettings(),
    val network: NetworkSnapshot = NetworkSnapshot(),
    val share: ShareRuntime = ShareRuntime(),
    val traffic: TrafficSnapshot = TrafficSnapshot(),
    val devices: List<ConnectedDevice> = emptyList()
)
