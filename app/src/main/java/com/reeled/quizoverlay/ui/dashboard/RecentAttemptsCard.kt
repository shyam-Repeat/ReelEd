package com.reeled.quizoverlay.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RecentAttemptsCard(
    attempts: List<AttemptPreview>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Recent Attempts (last 5)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            attempts.forEach { attempt ->
                AttemptRow(attempt)
            }
        }
    }
}

@Composable
private fun AttemptRow(attempt: AttemptPreview) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = when (attempt.status) {
                AttemptStatus.CORRECT -> "✓"
                AttemptStatus.WRONG -> "✗"
                AttemptStatus.DISMISSED -> "—"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (attempt.status == AttemptStatus.DISMISSED) "(dismissed)" else attempt.questionText,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (attempt.status != AttemptStatus.DISMISSED) {
            Text(
                text = "${attempt.responseSeconds}s",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = attempt.subject,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
