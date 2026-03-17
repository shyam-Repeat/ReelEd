package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.reeled.quizoverlay.ui.theme.Primary
import com.reeled.quizoverlay.util.PermissionChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    onDisableOptimization: () -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(PermissionChecker.isIgnoringBatteryOptimizations(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = PermissionChecker.isIgnoringBatteryOptimizations(context)
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
        // Top App Bar
        CenterAlignedTopAppBar(
            title = { Text("Battery Optimization", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", modifier = Modifier.size(20.dp))
                }
            }
        )

        // Progress
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Setup Progress", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("Step 8 of 9", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 8f / 9f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Illustration
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Blobs
                Box(modifier = Modifier.size(320.dp).background(Primary.copy(alpha = 0.05f), CircleShape))
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(24.dp, RoundedCornerShape(40.dp))
                        .background(
                            brush = Brush.sweepGradient(listOf(Color(0xFF3B82F6), Primary, Color(0xFF10B981))),
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
                        }
                    }
                    
                    // Floating bubbles
                    Surface(
                        modifier = Modifier.size(40.dp).align(Alignment.TopEnd).offset(x = (-20).dp, y = 40.dp),
                        shape = CircleShape,
                        color = Color(0xFF10B981),
                        border = BorderStroke(2.dp, Color.White)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Surface(
                        modifier = Modifier.size(32.dp).align(Alignment.BottomStart).offset(x = 40.dp, y = (-40).dp),
                        shape = CircleShape,
                        color = Color(0xFF93C5FD),
                        border = BorderStroke(2.dp, Color.White)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Ignore Battery Limits", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Ensures the app isn't killed by the system for seamless background operation and real-time updates.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Action Card
            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFD7F5E1),
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Optimization Disabled", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Surface(color = Primary.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Primary)
                                }
                            }
                            Column {
                                Text("Keep App Active", fontWeight = FontWeight.Bold)
                                Text("Allow background processing anytime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onDisableOptimization,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Disable Optimization", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "This setting helps maintain a stable connection for critical features like sync and notifications.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Footer
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
                    Text("Next Step")
                }
            }
        }
    }
}
