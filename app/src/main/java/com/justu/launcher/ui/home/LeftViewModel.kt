package com.justu.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justu.launcher.data.model.DailyIntention
import com.justu.launcher.data.repository.IntentionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class LeftViewModel @Inject constructor(
    private val intentionsRepository: IntentionsRepository
) : ViewModel() {

    private val _intentionText = MutableStateFlow("")
    val intentionText: StateFlow<String> = _intentionText.asStateFlow()

    private val _history = MutableStateFlow<List<DailyIntention>>(emptyList())
    val history: StateFlow<List<DailyIntention>> = _history.asStateFlow()

    init {
        // Load today's intention on start
        viewModelScope.launch {
            intentionsRepository.getTodayIntention().collect { text ->
                _intentionText.value = text
            }
        }
        // Load full history
        loadHistory()
        // Auto-save with 500ms debounce
        viewModelScope.launch {
            _intentionText
                .debounce(500L)
                .drop(1) // skip initial empty value
                .collect { text ->
                    intentionsRepository.saveIntention(text)
                    loadHistory()
                }
        }
    }

    fun updateIntention(text: String) {
        _intentionText.value = text
    }

    private fun loadHistory() {
        viewModelScope.launch {
            intentionsRepository.getAllIntentions().collect { list ->
                _history.value = list
            }
        }
    }
}
