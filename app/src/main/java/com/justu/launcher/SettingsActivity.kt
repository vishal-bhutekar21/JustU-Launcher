package com.justu.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.justu.launcher.data.model.ThemeSettings
import com.justu.launcher.data.repository.SettingsRepository
import com.justu.launcher.ui.settings.SettingsScreen
import com.justu.launcher.ui.theme.JustUTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeSettings by settingsRepository.themeSettings.collectAsState(initial = ThemeSettings())
            JustUTheme(themeMode = themeSettings.themeMode, fontFamily = themeSettings.fontFamily) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
