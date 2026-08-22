package com.nexshare.gateway.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.nexshare.gateway.domain.AppLanguage
import java.util.Locale

data class L10n(
    val isArabic: Boolean,
    val appName: String, val vpnActive: String, val vpnOff: String,
    val sharingOff: String, val sharingOn: String, val starting: String,
    val startSharing: String, val stopSharing: String,
    val connectedDevices: String, val download: String, val upload: String,
    val session: String, val internet: String, val online: String, val offline: String,
    val home: String, val devices: String, val stats: String, val settings: String, val howTo: String,
    val noDevices: String, val disconnect: String, val blocked: String, val allow: String,
    val autoStart: String, val autoStartHint: String, val vpnStatus: String,
    val networkMode: String, val timeout: String, val maxDevices: String,
    val darkMode: String, val language: String, val about: String, val aboutBody: String,
    val themeSystem: String, val themeDark: String, val themeLight: String,
    val langSystem: String, val langAr: String, val langEn: String,
    val modeWifi: String, val modeUsb: String, val modeLocal: String,
    val proxyHttp: String, val proxySocks: String, val copied: String, val gateway: String,
    val warningNoVpn: String, val warningNoLink: String, val warningNeedProxy: String,
    val privacyBody: String, val error: String, val ssid: String, val password: String,
    val seconds: String, val openHotspotSettings: String, val consumed: String, val liveChart: String
)

fun l10n(language: AppLanguage): L10n {
    val ar = when (language) {
        AppLanguage.ARABIC -> true
        AppLanguage.ENGLISH -> false
        AppLanguage.SYSTEM -> Locale.getDefault().language.startsWith("ar")
    }
    return if (ar) ar() else en()
}
val LocalL10n = staticCompositionLocalOf { l10n(AppLanguage.SYSTEM) }

private fun ar() = L10n(true, "NexShare", "VPN فعّال", "VPN غير متصل", "المشاركة متوقفة", "المشاركة نشطة", "جارٍ التشغيل…",
    "بدء المشاركة", "إيقاف المشاركة", "الأجهزة المتصلة", "تنزيل", "رفع", "مدة الجلسة", "الإنترنت", "متصل", "بدون إنترنت",
    "الرئيسية", "الأجهزة", "الإحصاءات", "الإعدادات", "طريقة الاتصال",
    "لا توجد أجهزة بعد. اضبط البروكسي على الجهاز الثاني.", "قطع", "محظور", "سماح",
    "تشغيل تلقائي", "ابدأ البوابة بعد إعادة التشغيل", "حالة VPN",
    "وضع الشبكة", "مهلة الاتصال", "الحد الأقصى للأجهزة",
    "المظهر", "اللغة", "حول التطبيق",
    "NexShare بوابة محلية تشارك إنترنت هذا الهاتف — بما فيه VPN النشط — عبر HTTP وSOCKS5. لا يُرسل الترافيك إلى أي خادم تابع لنا.",
    "حسب النظام", "داكن", "فاتح", "حسب النظام", "العربية", "English",
    "Hotspot النظام", "USB Tethering", "Hotspot محلي",
    "بروكسي HTTP", "بروكسي SOCKS5", "تم النسخ", "البوابة",
    "VPN غير نشط. المشاركة ستعمل لكن الجهاز الثاني لن يخرج من نفق VPN.",
    "لم يُكتشف Hotspot أو USB بعد.",
    "الجهاز الثاني يحتاج ضبط بروكسي. بدون ذلك سيظهر متصلًا بلا إنترنت.",
    "كل التوجيه محلي على جهازك.", "تعذّر التشغيل", "اسم الشبكة", "كلمة المرور", "ثانية",
    "فتح إعدادات Hotspot", "البيانات المستهلكة", "السرعة اللحظية")

private fun en() = L10n(false, "NexShare", "VPN active", "VPN off", "Sharing off", "Sharing active", "Starting…",
    "Start sharing", "Stop sharing", "Connected devices", "Download", "Upload", "Session", "Internet", "Online", "Offline",
    "Home", "Devices", "Stats", "Settings", "How to connect",
    "No clients yet. Set the proxy on the second device.", "Disconnect", "Blocked", "Allow",
    "Auto start", "Launch the gateway after reboot", "VPN status",
    "Network mode", "Connection timeout", "Maximum devices",
    "Appearance", "Language", "About",
    "NexShare is a local gateway that shares this phone’s internet — including the active VPN — through HTTP and SOCKS5. Traffic never leaves for our servers.",
    "System", "Dark", "Light", "System", "العربية", "English",
    "System hotspot", "USB tethering", "Local hotspot",
    "HTTP proxy", "SOCKS5 proxy", "Copied", "Gateway",
    "VPN is off. Sharing will work, but clients will not exit through a VPN tunnel.",
    "No hotspot or USB link detected yet.",
    "The client must set a proxy. Otherwise it will look connected with no internet.",
    "All forwarding stays on this device.", "Could not start", "Network name", "Password", "sec",
    "Open hotspot settings", "Data used", "Live speed")
