package com.reeled.quizoverlay.ui.childhome

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.prefs.AppPrefs
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.prefs.TriggerPrefs
import com.reeled.quizoverlay.ui.overlay.components.OptionButton
import com.reeled.quizoverlay.util.PermissionChecker
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChildHomeScreen(
    pinPrefs: PinPrefs,
    triggerPrefs: TriggerPrefs,
    appPrefs: AppPrefs,
    onNavigateToDashboard: () -> Unit,
    onNavigateToDevMode: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    val monitoredApps by appPrefs.monitoredApps.collectAsState(initial = emptySet())
    val lastSkipReason by triggerPrefs.lastSkipReason.collectAsState(initial = "None")
    val lastApp by triggerPrefs.lastForegroundApp.collectAsState(initial = "Unknown")
    val sessionStart by triggerPrefs.sessionStartTime.collectAsState(initial = 0L)

    val sessionDuration = if (sessionStart > 0) {
        (System.currentTimeMillis() - sessionStart) / 1000
    } else 0

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
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OptionButton(
                            label = "Dev Mode Logs",
                            enabled = true,
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                            onClick = onNavigateToDevMode
                        )
                    }
                }
            }
        }

        // Pager Indicator
        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
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
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Dev: O=$hasOverlay, U=$hasUsage, B=$isBatteryIgnoring | Skip: $lastSkipReason",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                Text(
                    text = "App: ${lastApp?.substringAfterLast('.')} (${sessionDuration}s) | Monitored: ${monitoredApps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Parent? Tap here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onNavigateToDashboard() }
            )
        }
    }
}
