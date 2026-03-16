package com.reeled.quizoverlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.core.content.ContextCompat
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.service.OverlayForegroundService
import com.reeled.quizoverlay.ui.navigation.AppNavGraph
import com.reeled.quizoverlay.worker.QuizFetchWorker
import com.reeled.quizoverlay.ui.navigation.Screen
import com.reeled.quizoverlay.ui.theme.ReelEdTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appPrefs = AppPrefs(this)
        
        // Blocking read for start destination to avoid flicker
        // In a larger app, use a Splash Screen or a loading state
        val startDestination = runBlocking {
            getStartDestination(appPrefs)
        }

        QuizFetchWorker.runOnce(this)

        if (startDestination == Screen.ChildHome.route) {
            ContextCompat.startForegroundService(this, OverlayForegroundService.startIntent(this))
        }

        setContent {
            ReelEdTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    private suspend fun getStartDestination(prefs: AppPrefs): String {
        return if (!prefs.onboardingComplete.first()) {
            Screen.Welcome.route
        } else {
            Screen.ChildHome.route
        }
    }
}
