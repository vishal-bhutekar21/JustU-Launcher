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

    private val _topApps = MutableStateFlow<List<Triple<String, String, Long>>>(emptyList())
    val topApps: StateFlow<List<Triple<String, String, Long>>> = _topApps.asStateFlow()

    private val _todayUsageMillis = MutableStateFlow(0L)
    val todayUsageMillis: StateFlow<Long> = _todayUsageMillis.asStateFlow()

    private val _yesterdayUsageMillis = MutableStateFlow(0L)
    val yesterdayUsageMillis: StateFlow<Long> = _yesterdayUsageMillis.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _todayUsageMillis.value = usageRepository.getTodaysUsageTime()
            _yesterdayUsageMillis.value = usageRepository.getYesterdayUsageTime()
            _topApps.value = usageRepository.getTopUsedApps()
        }
    }
}
