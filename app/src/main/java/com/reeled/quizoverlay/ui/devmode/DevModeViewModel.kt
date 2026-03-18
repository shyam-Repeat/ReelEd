package com.reeled.quizoverlay.ui.devmode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reeled.quizoverlay.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DevLogItem(
    val timestamp: Long,
    val title: String,
    val details: String,
)

data class DevModeUiState(
    val logs: List<DevLogItem> = emptyList(),
    val isTestModeEnabled: Boolean = false,
)

class DevModeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuizRepository(application)
    private val appPrefs = com.reeled.quizoverlay.prefs.AppPrefs(application)

    private val _uiState = MutableStateFlow(DevModeUiState())
    val uiState: StateFlow<DevModeUiState> = _uiState.asStateFlow()

    init {
        refreshLogs()
        viewModelScope.launch {
            appPrefs.isTestMode.collect { enabled ->
                _uiState.value = _uiState.value.copy(isTestModeEnabled = enabled)
            }
        }
    }

    fun toggleTestMode() {
        viewModelScope.launch {
            appPrefs.setTestMode(!_uiState.value.isTestModeEnabled)
        }
    }

    fun refreshLogs() {
        viewModelScope.launch {
            val eventLogs = repository.getRecentEventLogs(limit = 200).map {
                DevLogItem(
                    timestamp = it.createdAt,
                    title = "EVENT • ${it.eventType}",
                    details = it.payloadJson,
                )
            }

            val attempts = repository.getRecentAttemptDetails(limit = 200).map {
                val status = when {
                    it.dismissed -> "dismissed"
                    it.isCorrect -> "correct"
                    else -> "wrong"
                }
                DevLogItem(
                    timestamp = it.shownAt,
                    title = "ATTEMPT • ${it.subject} • $status",
                    details = "${it.questionText} (${(it.responseTimeMs / 1000f)}s)",
                )
            }

            _uiState.value = _uiState.value.copy(
                logs = (eventLogs + attempts).sortedByDescending { it.timestamp }
            )
        }
    }

    fun formatTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DevModeViewModel(application) as T
                }
            }
    }
}
