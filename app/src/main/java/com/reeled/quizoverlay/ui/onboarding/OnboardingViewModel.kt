package com.reeled.quizoverlay.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reeled.quizoverlay.data.repository.QuizRepository
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.service.OverlayServiceCoordinator
import com.reeled.quizoverlay.util.PinHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val appPrefs = AppPrefs(application)
    private val pinPrefs = PinPrefs(application)
    private val repository = QuizRepository(application)

    fun onConsentAccepted(nickname: String) {
        viewModelScope.launch {
            appPrefs.setNickname(nickname)
            appPrefs.setConsentGiven(true)
            // Initial registration, permissions might be false here
            try {
                repository.registerTester(nickname)
            } catch (_: Exception) {}
        }
    }

    fun onPinSet(pin: String) {
        viewModelScope.launch {
            val pinHash = PinHasher.hash(pin)
            pinPrefs.savePinHash(pinHash)
            appPrefs.setPinSet(true)
        }
    }

    fun onOnboardingCompleted() {
        viewModelScope.launch {
            val name = appPrefs.nickname.first()
            try {
                repository.registerTester(name)
            } catch (_: Exception) {
                // Continue onboarding even if tester sync temporarily fails.
            }
            appPrefs.setOnboardingComplete(true)
            OverlayServiceCoordinator.startAfterOnboarding(getApplication())
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(application) as T
                }
            }
    }
}
