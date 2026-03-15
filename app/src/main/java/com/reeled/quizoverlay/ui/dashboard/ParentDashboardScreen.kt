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

@Composable
fun ParentDashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.actions.collect { action ->
            when (action) {
                DashboardAction.ShowPinPrompt -> {
                    context.startActivity(Intent(context, PinActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
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
