package com.reeled.quizoverlay.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.ui.pin.PinActivity

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.ui.pin.PinGateDialog
import com.reeled.quizoverlay.service.OverlayForegroundService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ParentDashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinPrefs = remember { PinPrefs(context) }
    val appPrefs = remember { AppPrefs(context) }

    var showPinDialog by remember { mutableStateOf(false) }
    val pinHash by pinPrefs.pinHash.collectAsState(initial = null)
    val failedAttempts by pinPrefs.failedAttempts.collectAsState(initial = 0)
    val lockoutUntil by pinPrefs.lockoutUntil.collectAsState(initial = 0L)
    
    val currentTime = System.currentTimeMillis()
    val isLocked = lockoutUntil > currentTime
    val lockoutTimeRemaining = if (isLocked) lockoutUntil - currentTime else 0L

    LaunchedEffect(viewModel) {
        viewModel.actions.collect { action ->
            when (action) {
                DashboardAction.ShowPinPrompt -> {
                    showPinDialog = true
                }
                DashboardAction.OpenFeedback -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@reeled.app")
                        putExtra(Intent.EXTRA_SUBJECT, "ReelEd Feedback")
                    }
                    val canOpen = intent.resolveActivity(context.packageManager) != null
                    if (canOpen) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "No email app available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item { TodaySummaryCard(summary = uiState.todaySummary) }
        item { WeekBarCard(weekData = uiState.weekData) }
        item { SubjectBreakdownCard(subjects = uiState.todayBySubject) }
        item { RecentAttemptsCard(attempts = uiState.recentAttempts.take(5)) }
        item {
            ActionButtons(
                onDisable = viewModel::showPinPrompt,
                onFeedback = viewModel::openFeedback,
            )
        }
    }

    if (showPinDialog) {
        PinGateDialog(
            title = "Disable Overlay",
            subtitle = "Enter PIN to stop the service",
            storedPinHash = pinHash ?: "",
            isLocked = isLocked,
            lockoutTimeRemaining = lockoutTimeRemaining,
            onPinCorrect = {
                scope.launch {
                    pinPrefs.resetFailedAttempts()
                    showPinDialog = false
                    // Logic to disable overlay
                    context.stopService(Intent(context, OverlayForegroundService::class.java))
                    Toast.makeText(context, "Overlay Disabled", Toast.LENGTH_SHORT).show()
                }
            },
            onPinIncorrect = {
                scope.launch {
                    pinPrefs.incrementFailedAttempts()
                    val attempts = pinPrefs.failedAttempts.first()
                    if (attempts >= 3) {
                        pinPrefs.setLockout(System.currentTimeMillis() + 60_000)
                    }
                }
            },
            onDismiss = { showPinDialog = false }
        )
    }
}

@Composable
private fun ActionButtons(
    onDisable: () -> Unit,
    onFeedback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onDisable,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("DISABLE OVERLAY ▶")
        }

        OutlinedButton(
            onClick = onFeedback,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("SEND FEEDBACK")
        }
    }
}
