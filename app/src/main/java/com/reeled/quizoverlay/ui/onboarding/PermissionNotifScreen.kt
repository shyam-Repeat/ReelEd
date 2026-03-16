package com.reeled.quizoverlay.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
fun PermissionNotifScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAllowNotifications: () -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(PermissionChecker.hasNotificationPermission(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = PermissionChecker.hasNotificationPermission(context)
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
        // Progress Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(9) { index ->
                    val isActive = index == 6
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 8.dp else 6.dp)
                            .width(if (isActive) 32.dp else 6.dp)
                            .background(
                                color = if (isActive) Primary else Primary.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Step 7 of 9",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Illustration
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Circles
                Box(modifier = Modifier.fillMaxSize().background(Primary.copy(alpha = 0.05f), CircleShape))
                
                Surface(
                    modifier = Modifier.size(192.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = Primary.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Primary, modifier = Modifier.size(48.dp))
                                }
                            }
                            Surface(
                                modifier = Modifier.size(24.dp).align(Alignment.TopEnd),
                                shape = CircleShape,
                                color = Color(0xFFFFD700),
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.width(100.dp).height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width(140.dp).height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(5.dp)))
                    }
                }

                // Floating elements
                Surface(
                    modifier = Modifier.size(40.dp).align(Alignment.TopEnd).rotate(12f),
                    shape = RoundedCornerShape(12.dp),
                    color = Primary,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Celebration, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    modifier = Modifier.size(48.dp).align(Alignment.BottomStart).rotate(-12f),
                    shape = CircleShape,
                    color = Color(0xFF818CF8),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text("Stay Active", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Required to keep the learning service running reliably.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFD7F5E1),
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Permission Granted", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            } else {
                Button(
                    onClick = onAllowNotifications,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Allow Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bottom Nav
        Surface(
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Back", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onNext,
                    enabled = isGranted,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Next", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
