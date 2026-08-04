package com.justu.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justu.launcher.data.model.AppInfo
import com.justu.launcher.data.model.HomeSettings
import com.justu.launcher.data.repository.AppRepository
import com.justu.launcher.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val homeSettings: StateFlow<HomeSettings> = settingsRepository.homeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeSettings())

    private val _favoriteApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val favoriteApps: StateFlow<List<AppInfo>> = _favoriteApps.asStateFlow()

    private val _currentTime = MutableStateFlow(getCurrentTimeString())
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow(getCurrentDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _batteryLevel = MutableStateFlow("")
    val batteryLevel: StateFlow<String> = _batteryLevel.asStateFlow()

    init {
        loadFavoriteApps()
        startClock()
        startBatteryPolling()
    }

    private fun loadFavoriteApps() {
        viewModelScope.launch {
            settingsRepository.homeSettings.collect { settings ->
                val allApps = appRepository.getInstalledApps()
                var favs = settings.favoriteApps.mapNotNull { pkg ->
                    allApps.find { it.packageName == pkg }
                }
                if (favs.isEmpty() && allApps.isNotEmpty()) {
                    favs = allApps.take(settings.maxFavoriteApps)
                } else {
                    favs = favs.take(settings.maxFavoriteApps)
                }
                _favoriteApps.value = favs
            }
        }
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = getCurrentTimeString()
                _currentDate.value = getCurrentDateString()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun startBatteryPolling() {
        viewModelScope.launch {
            while (true) {
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }
                val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level != -1 && scale != -1) {
                    val batteryPct = level * 100 / scale
                    _batteryLevel.value = "$batteryPct%"
                }
                kotlinx.coroutines.delay(60000)
            }
        }
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }

    fun agreeToTerms() {
        viewModelScope.launch {
            settingsRepository.agreeToTerms()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.completeOnboarding()
        }
    }

    fun updateOnboardingPage(page: Int) {
        viewModelScope.launch {
            settingsRepository.updateOnboardingPage(page)
        }
    }

    fun toggleFocusMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleFocusMode(enabled)
        }
    }

    fun removeFavoriteApp(packageName: String) {
        viewModelScope.launch {
            settingsRepository.removeFavoriteApp(packageName)
        }
    }

    fun replaceFavoriteApp(oldPackage: String, newPackage: String) {
        viewModelScope.launch {
            settingsRepository.replaceFavoriteApp(oldPackage, newPackage)
        }
    }

    fun markTooltipSeen() {
        viewModelScope.launch {
            settingsRepository.markTooltipSeen()
        }
    }
}
