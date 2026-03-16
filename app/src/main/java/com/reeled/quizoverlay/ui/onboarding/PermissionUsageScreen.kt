package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.reeled.quizoverlay.ui.theme.Primary
import com.reeled.quizoverlay.util.PermissionChecker

@Composable
fun PermissionUsageScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    onGrantAccess: () -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(PermissionChecker.hasUsageStatsPermission(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = PermissionChecker.hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header & Progress
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                repeat(9) { index ->
                    val isActive = index == 4
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .weight(1f)
                            .background(
                                color = if (isActive) Primary else Primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Usage Access",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Box(modifier = Modifier.size(48.dp))
            }
        }

        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Primary.copy(alpha = 0.05f), Primary.copy(alpha = 0.2f))
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Central Device Mockup
                Surface(
                    modifier = Modifier.size(width = 128.dp, height = 224.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp,
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(width = 48.dp, height = 16.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).align(Alignment.CenterHorizontally))
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(Primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp)))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(96.dp).background(Primary.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = Primary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)))
                            Box(modifier = Modifier.fillMaxWidth(0.8f).height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)))
                        }
                    }
                }

                // Floating Detection Labels
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-16).dp, y = 40.dp),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Text("Instagram detected", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).offset(x = 16.dp, y = (-48).dp),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.School, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text("YouTube detected", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text("Detect Active Apps", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Allows ReelEd to know when to show a quiz by detecting your active learning apps.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD7F5E1),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Permission Granted", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onGrantAccess,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Grant Access", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your data is processed locally and never leaves your device.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Bottom Nav
        Surface(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = onNext,
                    enabled = isGranted,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Next")
                }
            }
        }
    }
}
