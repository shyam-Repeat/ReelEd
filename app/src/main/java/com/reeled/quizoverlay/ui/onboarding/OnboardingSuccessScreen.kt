package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.ui.theme.Primary

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun OnboardingSuccessScreen(
    onEnterChildMode: () -> Unit,
    onGoToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Indicator (Final)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 16.dp)) {
            repeat(7) { Box(modifier = Modifier.size(6.dp).background(Primary.copy(alpha = 0.2f), CircleShape)) }
            Box(modifier = Modifier.height(8.dp).width(32.dp).background(Primary, RoundedCornerShape(4.dp)))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Success Header
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(100.dp).background(Primary.copy(alpha = 0.1f), CircleShape))
            Icon(
                imageVector = Icons.Outlined.Verified,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(72.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Ready to Learn!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Your safe environment is ready. The following apps will be available for use:",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Apps Grid
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppStatusCard("Instagram", Color(0xFFE4405F), modifier = Modifier.weight(1f))
            AppStatusCard("YouTube", Color(0xFFFF0000), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Success Visual Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Primary, Color(0xFFA855F7), Color(0xFFFDBA74))
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(120.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("🎉 You're All Set!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Child mode is now fully configured with your selected restrictions and time limits.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onEnterChildMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Outlined.RocketLaunch, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Enter Child Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onGoToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Primary)
            ) {
                Text("Go to Parent Dashboard", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            }
        }
    }
}

@Composable
fun AppStatusCard(name: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.1f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(color, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Active", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
