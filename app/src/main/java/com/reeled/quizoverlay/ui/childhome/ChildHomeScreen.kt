package com.reeled.quizoverlay.ui.childhome

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.prefs.TriggerPrefs
import com.reeled.quizoverlay.util.PermissionChecker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.reeled.quizoverlay.ui.overlay.components.OptionButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

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
    val pagerState = rememberPagerState(pageCount = { 2 })
    val monitoredApps by appPrefs.monitoredApps.collectAsState(initial = emptySet())
    val lastSkipReason by triggerPrefs.lastSkipReason.collectAsState(initial = "None")
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
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
                }

                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Settings & Options",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        OptionButton(
                            label = "Configure Learning",
                            enabled = true,
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { /* Action for option */ }
                        )
                    }
                }

                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Parent Tools",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        OptionButton(
                            label = "Open Dashboard",
                            enabled = true,
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            onClick = onNavigateToDashboard,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OptionButton(
                            label = "Dev Mode Logs",
                            enabled = true,
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = onNavigateToDevMode,
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                }
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }

        // Parent Link & Dev Info
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
                modifier = Modifier.clickable { onNavigateToDashboard() }
            )
        }
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
