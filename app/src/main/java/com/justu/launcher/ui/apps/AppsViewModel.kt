package com.justu.launcher.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justu.launcher.data.model.AppInfo
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

    val homeSettings: StateFlow<com.justu.launcher.data.model.HomeSettings> = settingsRepository.homeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.justu.launcher.data.model.HomeSettings())

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(_allApps, _searchQuery) { apps, query ->
        if (query.isEmpty()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val allApps = appRepository.getInstalledApps()
            settingsRepository.homeSettings.collect { settings ->
                _allApps.value = allApps.filter { !settings.hiddenApps.contains(it.packageName) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

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
}
