package com.reeled.quizoverlay.ui.childhome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.ui.pin.PinGateDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.Color
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.prefs.TriggerPrefs
import com.reeled.quizoverlay.util.PermissionChecker
import androidx.compose.ui.platform.LocalContext

@Composable
fun ChildHomeScreen(
    pinPrefs: PinPrefs,
    triggerPrefs: TriggerPrefs,
    appPrefs: AppPrefs,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }
    val pinHash by pinPrefs.pinHash.collectAsState(initial = null)
    val failedAttempts by pinPrefs.failedAttempts.collectAsState(initial = 0)
    val lockoutUntil by pinPrefs.lockoutUntil.collectAsState(initial = 0L)
    
    val monitoredApps by appPrefs.monitoredApps.collectAsState(initial = emptySet())
    val lastSkipReason by triggerPrefs.lastSkipReason.collectAsState(initial = "None")

    val scope = rememberCoroutineScope()
    val currentTime = System.currentTimeMillis()
    val isLocked = lockoutUntil > currentTime
    val lockoutTimeRemaining = if (isLocked) lockoutUntil - currentTime else 0L

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_brain),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Learning mode is ON!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Quizzes will pop up while you scroll. Keep learning to earn your scroll time!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Parent Link
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dev Info (Visible in MVP for debugging)
            val hasOverlay = PermissionChecker.hasOverlayPermission(context)
            val hasUsage = PermissionChecker.hasUsageStatsPermission(context)
            val isBatteryIgnoring = PermissionChecker.isIgnoringBatteryOptimizations(context)
            
            Text(
                text = "Dev: O=$hasOverlay, U=$hasUsage, B=$isBatteryIgnoring | Apps: ${monitoredApps.size} | Skip: $lastSkipReason",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Parent? Tap here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { showPinDialog = true }
            )
        }
    }

    if (showPinDialog) {
        PinGateDialog(
            storedPinHash = pinHash ?: "",
            isLocked = isLocked,
            lockoutTimeRemaining = lockoutTimeRemaining,
            onPinCorrect = {
                scope.launch {
                    pinPrefs.resetFailedAttempts()
                    showPinDialog = false
                    onNavigateToDashboard()
                }
            },
            onPinIncorrect = {
                scope.launch {
                    pinPrefs.incrementFailedAttempts()
                    val attempts = pinPrefs.failedAttempts.first()
                    if (attempts >= 3) {
                        pinPrefs.setLockout(System.currentTimeMillis() + 60_000)
                    }
                }
            },
            onDismiss = { showPinDialog = false }
        )
    }
}
