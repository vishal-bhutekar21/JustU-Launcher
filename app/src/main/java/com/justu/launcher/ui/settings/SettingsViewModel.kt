package com.justu.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justu.launcher.data.model.AppInfo
import com.justu.launcher.data.model.HomeSettings
import com.justu.launcher.data.model.ThemeMode
import com.justu.launcher.data.model.ThemeSettings
import com.justu.launcher.data.repository.AppRepository
import com.justu.launcher.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    val homeSettings: StateFlow<HomeSettings> = settingsRepository.homeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeSettings())

    val themeSettings: StateFlow<ThemeSettings> = settingsRepository.themeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeSettings())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = appRepository.getInstalledApps()
        }
    }

    fun updateHomeElementVisibility(
        showClock: Boolean? = null,
        showDate: Boolean? = null,
        showGreeting: Boolean? = null,
        showBattery: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsRepository.updateHomeElementVisibility(showClock, showDate, showBattery, showGreeting)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun updateFontFamily(fontFamily: String) {
        viewModelScope.launch {
            settingsRepository.updateFontFamily(fontFamily)
        }
    }

    fun updateMaxFavoriteApps(max: Int) {
        viewModelScope.launch {
            settingsRepository.updateMaxFavoriteApps(max)
        }
    }

    fun updateFavoritesAlignment(alignment: String) {
        viewModelScope.launch {
            settingsRepository.updateFavoritesAlignment(alignment)
        }
    }

    fun toggleBlockApp(packageName: String) {
        viewModelScope.launch {
            val currentBlocked = homeSettings.value.blockedApps.toMutableSet()
            if (currentBlocked.contains(packageName)) {
                currentBlocked.remove(packageName)
            } else {
                currentBlocked.add(packageName)
            }
            settingsRepository.updateBlockedApps(currentBlocked)
        }
    }

    fun toggleBlockYoutubeShorts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleBlockYoutubeShorts(enabled)
        }
    }
}
