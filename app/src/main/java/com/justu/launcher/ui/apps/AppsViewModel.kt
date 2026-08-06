package com.justu.launcher.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justu.launcher.data.model.AppInfo
import com.justu.launcher.data.model.HomeSettings
import com.justu.launcher.data.repository.AppRepository
import com.justu.launcher.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val homeSettings: StateFlow<HomeSettings> = settingsRepository.homeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeSettings())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // All apps, sorted alphabetically, filtered by hidden list and search query
    val apps: StateFlow<List<AppInfo>> = combine(settingsRepository.homeSettings, _searchQuery) { settings, query ->
        appRepository.getInstalledApps()
            .filter { !settings.hiddenApps.contains(it.packageName) }
            .filter { it.label.contains(query, ignoreCase = true) }
            .sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.homeSettings.first()
            val currentFavs = currentSettings.favoriteApps.toMutableList()
            if (currentFavs.contains(packageName)) {
                currentFavs.remove(packageName)
            } else {
                currentFavs.add(packageName)
            }
            settingsRepository.updateFavoriteApps(currentFavs)
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.homeSettings.first()
            val currentHidden = currentSettings.hiddenApps.toMutableSet()
            currentHidden.add(packageName)
            settingsRepository.updateHiddenApps(currentHidden)
        }
    }

    fun toggleExempt(packageName: String) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.homeSettings.first()
            val current = currentSettings.exemptApps.toMutableSet()
            if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
            settingsRepository.updateExemptApps(current)
        }
    }
}
