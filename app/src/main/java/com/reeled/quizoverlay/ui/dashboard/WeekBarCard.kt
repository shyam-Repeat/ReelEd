package com.reeled.quizoverlay.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WeekBarCard(
    weekData: List<DaySummary>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "This Week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            weekData.forEach { day ->
                WeekRow(day)
            }
        }
    }
}

@Composable
private fun WeekRow(day: DaySummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = day.dayLabel,
            modifier = Modifier.weight(0.15f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
        )

        Box(
            modifier = Modifier
                .weight(0.45f)
                .height(12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(999.dp),
                ),
        ) {
            if (day.hasSession) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(day.progress.coerceIn(0f, 1f))
                        .height(12.dp)
                        .background(
                            color = if (day.isToday) MaterialTheme.colorScheme.primary else Color(0xFF58A6FF),
                            shape = RoundedCornerShape(999.dp),
                        ),
                )
            }
        }

        Text(
            text = when {
                day.hasSession -> "${day.correct}/${day.shown} correct"
                else -> "No session"
            } + if (day.isToday) " ← today" else "",
            modifier = Modifier.weight(0.40f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
