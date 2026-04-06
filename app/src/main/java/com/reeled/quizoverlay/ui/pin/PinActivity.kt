package com.reeled.quizoverlay.ui.pin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.ui.theme.ReelEdTheme

class PinActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SOURCE_APP = "extra_source_app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ReelEdTheme {
                val pinViewModel: PinViewModel = viewModel(
                    factory = PinViewModel.provideFactory(application)
                )
                val uiState by pinViewModel.uiState.collectAsState()
                var step by remember { mutableStateOf(PinStep.ENTRY) }
                
                val currentTime = System.currentTimeMillis()
                val isLocked = uiState.lockoutUntil > currentTime
                val lockoutTimeRemaining = if (isLocked) uiState.lockoutUntil - currentTime else 0L

                if (step == PinStep.ENTRY) {
                    PinGateDialog(
                        title = stringResource(R.string.pin_gate_title),
                        subtitle = stringResource(R.string.pin_gate_subtitle),
                        storedPinHash = uiState.pinHash ?: "",
                        isLocked = isLocked,
                        lockoutTimeRemaining = lockoutTimeRemaining,
                        onPinCorrect = {
                            pinViewModel.onPinCorrect()
                            step = PinStep.DURATION_SELECTION
                        },
                        onPinIncorrect = pinViewModel::onPinIncorrect,
                        onDismiss = { finish() }
                    )
                } else {
                    PauseDurationSelector(
                        onDurationSelected = { minutes ->
                            pinViewModel.applyPause(
                                sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP),
                                minutes = minutes
                            ) {
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
                text = stringResource(R.string.pause_duration_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            val durations = listOf(
                stringResource(R.string.pause_duration_30m) to 30L,
                stringResource(R.string.pause_duration_1h) to 60L,
                stringResource(R.string.pause_duration_2h) to 120L
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
                Text(stringResource(R.string.common_cancel))
            }
        }
    }
}
