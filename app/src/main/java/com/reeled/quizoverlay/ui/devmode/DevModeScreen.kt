package com.reeled.quizoverlay.ui.devmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment

@Composable
fun DevModeScreen(
    viewModel: DevModeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text(
                text = "Dev Mode & Test Tools",
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Test Mode",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Shows quiz every 30s, ignores trigger logic",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isTestModeEnabled,
                            onCheckedChange = { viewModel.toggleTestMode() }
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = viewModel::refreshLogs,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Refresh Logs")
            }
        }

        item {
            Text(
                text = "Entries: ${uiState.logs.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.logs.isEmpty()) {
            item {
                Text(
                    text = "No logs yet. Use the app for a minute, then tap Refresh Logs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(uiState.logs) { log ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = viewModel.formatTime(log.timestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = log.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
