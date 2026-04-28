package com.reeled.quizoverlay.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.prefs.AppDetectionMode
import com.reeled.quizoverlay.ui.devmode.DevModeUiState
import com.reeled.quizoverlay.ui.devmode.DevModeViewModel
import kotlinx.coroutines.delay

sealed class DashboardTab(val titleRes: Int, val icon: ImageVector) {
    object Dashboard : DashboardTab(R.string.dashboard_tab_dashboard, Icons.Default.Home)
    object Controls : DashboardTab(R.string.dashboard_tab_controls, Icons.Default.Build)
    object Settings : DashboardTab(R.string.dashboard_tab_settings, Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    devModeViewModel: DevModeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by dashboardViewModel.uiState.collectAsState()
    val devUiState by devModeViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf<DashboardTab>(DashboardTab.Dashboard) }

    LaunchedEffect(selectedTab) {
        if (selectedTab != DashboardTab.Dashboard) return@LaunchedEffect
        while (true) {
            dashboardViewModel.refreshDashboard()
            delay(5_000)
        }
    }

    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel.actions.collect { action ->
            when (action) {
                DashboardAction.ShowPinPrompt -> Unit
                DashboardAction.OpenFeedback -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@reeled.app")
                        putExtra(Intent.EXTRA_SUBJECT, "ReelEd Feedback")
                    }
                    val canOpen = intent.resolveActivity(context.packageManager) != null
                    if (canOpen) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, context.getString(R.string.dashboard_no_email_app), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            stringResource(R.string.dashboard_parent_title),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.dashboard_welcome_back),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { dashboardViewModel.openFeedback() },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.dashboard_feedback), fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(DashboardTab.Dashboard, DashboardTab.Controls, DashboardTab.Settings)
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.titleRes)) },
                        label = { Text(stringResource(tab.titleRes)) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                DashboardTab.Dashboard -> DashboardContent(uiState, modifier)
                DashboardTab.Controls -> ControlsContent(
                    isOverlayEnabled = uiState.isOverlayEnabled,
                    dailyCap = uiState.dailyCap,
                    quizTimerMinutes = uiState.quizTimerMinutes,
                    forceQuizEnabled = uiState.forceQuizEnabled,
                    appDetectionMode = uiState.appDetectionMode,
                    onOverlayToggle = dashboardViewModel::setOverlayEnabled,
                    onDailyCapChange = dashboardViewModel::setDailyCap,
                    onQuizTimerMinutesChange = dashboardViewModel::setQuizTimerMinutes,
                    onForceQuizToggle = dashboardViewModel::setForceQuizEnabled,
                    onAppDetectionModeChange = dashboardViewModel::setAppDetectionMode,
                    onFeedback = dashboardViewModel::openFeedback
                )
                DashboardTab.Settings -> SettingsContent(
                    uiState = devUiState,
                    viewModel = devModeViewModel
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item { TodaySummaryCard(summary = uiState.todaySummary) }
        item { WeekBarCard(weekData = uiState.weekData) }
        item { SubjectBreakdownCard(subjects = uiState.todayBySubject) }
        item { RecentAttemptsCard(attempts = uiState.recentAttempts.take(5)) }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlsContent(
    isOverlayEnabled: Boolean,
    dailyCap: Int,
    quizTimerMinutes: Int,
    forceQuizEnabled: Boolean,
    appDetectionMode: AppDetectionMode,
    onOverlayToggle: (Boolean) -> Unit,
    onDailyCapChange: (Int) -> Unit,
    onQuizTimerMinutesChange: (Int) -> Unit,
    onForceQuizToggle: (Boolean) -> Unit,
    onAppDetectionModeChange: (AppDetectionMode) -> Unit,
    onFeedback: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Text(
                stringResource(R.string.controls_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.controls_detection_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.controls_detection_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = appDetectionMode == AppDetectionMode.USAGE_STATS,
                        onClick = { onAppDetectionModeChange(AppDetectionMode.USAGE_STATS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(stringResource(R.string.controls_detection_usage_stats))
                    }
                    SegmentedButton(
                        selected = appDetectionMode == AppDetectionMode.MEDIA_SESSION,
                        onClick = { onAppDetectionModeChange(AppDetectionMode.MEDIA_SESSION) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(stringResource(R.string.controls_detection_media_session))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (appDetectionMode == AppDetectionMode.MEDIA_SESSION) {
                        stringResource(R.string.controls_detection_media_session_note)
                    } else {
                        stringResource(R.string.controls_detection_usage_stats_note)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.controls_overlay_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.controls_overlay_enable),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.controls_overlay_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isOverlayEnabled,
                        onCheckedChange = onOverlayToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = com.reeled.quizoverlay.ui.theme.Primary
                        )
                    )
                }
                if (!isOverlayEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.controls_overlay_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.controls_daily_cap_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.controls_daily_cap_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.controls_daily_cap_value, dailyCap),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = dailyCap.toFloat(),
                    onValueChange = { onDailyCapChange(it.toInt()) },
                    valueRange = 15f..20f,
                    steps = 4
                )
            }
        }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.controls_quiz_timer_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.controls_quiz_timer_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.controls_quiz_timer_value, quizTimerMinutes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = quizTimerMinutes.toFloat(),
                    onValueChange = { onQuizTimerMinutesChange(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }
        }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.controls_force_quiz_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.controls_force_quiz_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = forceQuizEnabled,
                        onCheckedChange = onForceQuizToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = com.reeled.quizoverlay.ui.theme.Primary
                        )
                    )
                }
            }
        }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.controls_support_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.controls_support_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onFeedback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, com.reeled.quizoverlay.ui.theme.Primary.copy(alpha = 0.5f))
                ) {
                    Text(stringResource(R.string.controls_send_feedback), color = com.reeled.quizoverlay.ui.theme.Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: DevModeUiState,
    viewModel: DevModeViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.devmode_test_mode),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.devmode_test_mode_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isTestModeEnabled,
                            onCheckedChange = { viewModel.toggleTestMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = com.reeled.quizoverlay.ui.theme.Primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.devmode_logs_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Button(
                onClick = viewModel::refreshLogs,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.devmode_refresh_logs))
            }
        }

        if (uiState.logs.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.devmode_no_logs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(uiState.logs) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = viewModel.formatTime(log.timestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = log.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = log.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
