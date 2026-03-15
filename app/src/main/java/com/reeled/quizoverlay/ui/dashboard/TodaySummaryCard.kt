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
import androidx.compose.ui.unit.dp

@Composable
fun TodaySummaryCard(
    summary: TodaySummary,
    modifier: Modifier = Modifier,
) {
    val answeredPct = if (summary.shown > 0) (summary.answered * 100) / summary.shown else 0
    val correctPct = if (summary.answered > 0) (summary.correct * 100) / summary.answered else 0

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SummaryRow(label = "Quizzes shown", value = summary.shown.toString())
            SummaryRow(label = "Answered", value = "${summary.answered} ($answeredPct%)")
            SummaryRow(label = "Correct", value = "${summary.correct} ($correctPct%)")
            SummaryRow(label = "Dismissed", value = summary.dismissed.toString())
            SummaryRow(label = "Avg response time", value = "${summary.avgResponseSeconds}s")
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
