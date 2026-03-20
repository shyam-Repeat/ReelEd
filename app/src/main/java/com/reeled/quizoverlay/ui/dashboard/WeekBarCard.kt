package com.reeled.quizoverlay.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.ui.theme.Primary

@Composable
fun WeekBarCard(
    weekData: List<DaySummary>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Weekly Activity",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weekData.forEach { day ->
                    BarItem(day, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BarItem(day: DaySummary, modifier: Modifier = Modifier) {
    val barHeight = (120.dp * day.progress).coerceAtLeast(4.dp)
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (day.hasSession) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(barHeight)
                    .background(
                        color = if (day.isToday) Primary else Primary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = day.dayLabel.take(1),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (day.isToday) Primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
