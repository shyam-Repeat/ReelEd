package com.reeled.quizoverlay.ui.pin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.prefs.TriggerPrefs
import com.reeled.quizoverlay.ui.theme.ReelEdTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PinActivity : ComponentActivity() {

    private val pinPrefs by lazy { PinPrefs(this) }
    private val triggerPrefs by lazy { TriggerPrefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ReelEdTheme {
                var step by remember { mutableStateOf(PinStep.ENTRY) }
                val pinHash by pinPrefs.pinHash.collectAsState(initial = null)
                val lockoutUntil by pinPrefs.lockoutUntil.collectAsState(initial = 0L)
                
                val currentTime = System.currentTimeMillis()
                val isLocked = lockoutUntil > currentTime
                val lockoutTimeRemaining = if (isLocked) lockoutUntil - currentTime else 0L

                if (step == PinStep.ENTRY) {
                    PinGateDialog(
                        storedPinHash = pinHash ?: "",
                        isLocked = isLocked,
                        lockoutTimeRemaining = lockoutTimeRemaining,
                        onPinCorrect = {
                            lifecycleScope.launch {
                                pinPrefs.resetFailedAttempts()
                                step = PinStep.DURATION_SELECTION
                            }
                        },
                        onPinIncorrect = {
                            lifecycleScope.launch {
                                pinPrefs.incrementFailedAttempts()
                                val attempts = pinPrefs.failedAttempts.first()
                                if (attempts >= 3) {
                                    pinPrefs.setLockout(System.currentTimeMillis() + 60_000)
                                }
                            }
                        },
                        onDismiss = { finish() }
                    )
                } else {
                    PauseDurationSelector(
                        onDurationSelected = { minutes ->
                            lifecycleScope.launch {
                                val expiry = System.currentTimeMillis() + (minutes * 60 * 1000)
                                triggerPrefs.setPause(true, expiry)
                                finish()
                            }
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }

    enum class PinStep {
        ENTRY, DURATION_SELECTION
    }
}

@Composable
fun PauseDurationSelector(
    onDurationSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Select Pause Duration",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            val durations = listOf(
                "30 Minutes" to 30L,
                "1 Hour" to 60L,
                "2 Hours" to 120L
            )

            durations.forEach { (label, minutes) ->
                Button(
                    onClick = { onDurationSelected(minutes) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(label)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    }
}
