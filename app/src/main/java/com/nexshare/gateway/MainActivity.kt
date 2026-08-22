package com.nexshare.gateway

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexshare.gateway.presentation.MainViewModel
import com.nexshare.gateway.ui.NexShareRoot
import com.nexshare.gateway.ui.i18n.LocalL10n
import com.nexshare.gateway.ui.i18n.l10n
import com.nexshare.gateway.ui.theme.NexShareTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val perm = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val need = buildList {
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            if (Build.VERSION.SDK_INT in 26..32) add(Manifest.permission.ACCESS_FINE_LOCATION)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) perm.launch(need.toTypedArray())

        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            val strings = l10n(state.settings.language)
            CompositionLocalProvider(LocalL10n provides strings) {
                NexShareTheme(state.settings.themeMode) {
                    Box(Modifier.fillMaxSize()) { NexShareRoot(state, vm) }
                }
            }
        }
    }
}
