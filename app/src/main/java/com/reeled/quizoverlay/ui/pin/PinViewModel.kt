package com.reeled.quizoverlay.ui.pin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.prefs.TriggerPrefs
import com.reeled.quizoverlay.service.OverlayForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PinUiState(
    val pinHash: String? = null,
    val lockoutUntil: Long = 0L
)

class PinViewModel(application: Application) : AndroidViewModel(application) {
    private val pinPrefs = PinPrefs(application)
    private val triggerPrefs = TriggerPrefs(application)

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            pinPrefs.pinHash.collect { pinHash ->
                _uiState.value = _uiState.value.copy(pinHash = pinHash)
            }
        }
        viewModelScope.launch {
            pinPrefs.lockoutUntil.collect { lockoutUntil ->
                _uiState.value = _uiState.value.copy(lockoutUntil = lockoutUntil)
            }
        }
    }

    fun onPinCorrect() {
        viewModelScope.launch {
            pinPrefs.resetFailedAttempts()
        }
    }

    fun onPinIncorrect() {
        viewModelScope.launch {
            pinPrefs.incrementFailedAttempts()
            val attempts = pinPrefs.failedAttempts.first()
            if (attempts >= 3) {
                pinPrefs.setLockout(System.currentTimeMillis() + 60_000)
            }
        }
    }

    fun applyPause(sourceApp: String?, minutes: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            val expiry = System.currentTimeMillis() + (minutes * 60 * 1000)
            val application = getApplication<Application>()
            if (!sourceApp.isNullOrBlank()) {
                triggerPrefs.setAppPause(sourceApp, expiry)
                application.startService(
                    OverlayForegroundService.startIntent(application).apply {
                        action = OverlayForegroundService.ACTION_SET_APP_PAUSE
                        putExtra(OverlayForegroundService.EXTRA_SOURCE_APP, sourceApp)
                        putExtra(OverlayForegroundService.EXTRA_PAUSE_EXPIRY_MS, expiry)
                    }
                )
            } else {
                triggerPrefs.setPause(true, expiry)
            }
            onComplete()
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PinViewModel(application) as T
                }
            }
    }
}
