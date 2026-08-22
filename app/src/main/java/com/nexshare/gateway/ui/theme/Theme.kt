package com.nexshare.gateway.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nexshare.gateway.domain.ThemeMode

private val Dark = darkColorScheme(
    primary = Color(0xFF3EE0D4), onPrimary = Color(0xFF003733),
    background = Color(0xFF070B14), onBackground = Color(0xFFE8EEFC),
    surface = Color(0xFF101826), onSurface = Color(0xFFE8EEFC),
    surfaceVariant = Color(0xFF182235), onSurfaceVariant = Color(0xFFB7C0D4),
    outline = Color(0xFF3A4458), error = Color(0xFFFF5D73)
)
private val Light = lightColorScheme(
    primary = Color(0xFF0E8A82), onPrimary = Color.White,
    background = Color(0xFFF4F7FB), onBackground = Color(0xFF0C1220),
    surface = Color.White, onSurface = Color(0xFF0C1220),
    surfaceVariant = Color(0xFFE7EEF7), onSurfaceVariant = Color(0xFF445066),
    outline = Color(0xFFC5CEDC), error = Color(0xFFC5283D)
)

@Composable
fun NexShareTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val w = (view.context as Activity).window
            w.statusBarColor = Color.Transparent.toArgb()
            w.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
