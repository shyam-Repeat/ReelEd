package com.reeled.quizoverlay.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.util.PinHasher
import com.reeled.quizoverlay.data.repository.QuizRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val appPrefs = AppPrefs(application)
    private val pinPrefs = PinPrefs(application)
    private val repository = QuizRepository(application)

    fun onConsentAccepted() {
        viewModelScope.launch {
            appPrefs.setConsentGiven(true)
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
            repository.registerTester(name)
            appPrefs.setOnboardingComplete(true)
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
