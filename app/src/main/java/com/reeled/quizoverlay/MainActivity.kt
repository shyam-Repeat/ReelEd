package com.reeled.quizoverlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.reeled.quizoverlay.data.repository.QuizRepository
import com.reeled.quizoverlay.ui.navigation.AppNavGraph
import com.reeled.quizoverlay.ui.navigation.Screen
import com.reeled.quizoverlay.worker.QuizFetchWorker
import com.reeled.quizoverlay.ui.theme.ReelEdTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository by lazy { QuizRepository(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val startDestination = Screen.Loading.route

        QuizFetchWorker.runOnce(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                repository.logEvent("app_opened", "{\"source\":\"main_activity\"}")
            } catch (_: Exception) {
                // Keep app startup resilient.
            }
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

}
