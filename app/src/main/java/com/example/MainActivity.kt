package com.example

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.MainAppShell
import com.example.ui.theme.GVJVaultTheme

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.example.network.NetworkClient
import com.example.ui.ScreenState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup global Coil ImageLoader with browser User-Agent and SVG support for StashDB studio logos
        val imageLoader = ImageLoader.Builder(applicationContext)
            .okHttpClient(NetworkClient.okHttpClient)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
        Coil.setImageLoader(imageLoader)

        // Pure Transparent Edge-To-Edge for both Status Bar and Navigation Bar
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        // Disable system-enforced scrim / black background behind Gesture Navigation Bar on Android 10 (Q) and newer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val safeSettings = settings ?: com.example.data.local.entity.SettingsEntity()

            // Initialize startup screen directly in ViewModel without triggering a secondary transition
            LaunchedEffect(Unit) {
                val startScreen = intent?.getStringExtra("start_screen")
                val startSection = intent?.getStringExtra("start_section")
                if (startScreen != null) {
                    viewModel.initStartScreen(startScreen, startSection)
                }
            }

            // Sync window background with active theme to eliminate dark flash on light theme cold launch
            LaunchedEffect(safeSettings.currentTheme) {
                val isLight = safeSettings.currentTheme.equals("Light", ignoreCase = true)
                window.decorView.setBackgroundColor(if (isLight) 0xFFF7F7F8.toInt() else 0xFF1E1E22.toInt())
            }

            GVJVaultTheme(
                paletteName = safeSettings.currentTheme,
                accentColorHex = safeSettings.accentColorHex,
                actressNameColorHex = safeSettings.actressNameColorHex,
                betaTestPrivacy = safeSettings.betaTestPrivacy
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppShell(viewModel = viewModel)
                }
            }
        }
    }
}
