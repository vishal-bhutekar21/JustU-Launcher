package com.justu.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justu.launcher.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: UsageRepository
) : ViewModel() {

    private val _topApps = MutableStateFlow<List<Pair<String, Long>>>(emptyList())
    val topApps: StateFlow<List<Pair<String, Long>>> = _topApps.asStateFlow()

    init {
        loadTopApps()
    }

    private fun loadTopApps() {
        viewModelScope.launch {
            _topApps.value = usageRepository.getTopUsedApps()
        }
    }
}
