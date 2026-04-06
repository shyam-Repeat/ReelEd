package com.reeled.quizoverlay.ui.navigation

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.reeled.quizoverlay.ui.dashboard.DashboardViewModel
import com.reeled.quizoverlay.ui.dashboard.ParentDashboardScreen
import com.reeled.quizoverlay.ui.devmode.DevModeScreen
import com.reeled.quizoverlay.ui.devmode.DevModeViewModel
import com.reeled.quizoverlay.ui.onboarding.AppSelectionScreen
import com.reeled.quizoverlay.ui.onboarding.BatteryOptScreen
import com.reeled.quizoverlay.ui.onboarding.ConsentScreen
import com.reeled.quizoverlay.ui.onboarding.LoadingScreen
import com.reeled.quizoverlay.ui.onboarding.OnboardingSuccessScreen
import com.reeled.quizoverlay.ui.onboarding.OnboardingViewModel
import com.reeled.quizoverlay.ui.onboarding.PermissionNotifScreen
import com.reeled.quizoverlay.ui.onboarding.PermissionOverlayScreen
import com.reeled.quizoverlay.ui.onboarding.PermissionUsageScreen
import com.reeled.quizoverlay.ui.onboarding.PinSetupScreen
import com.reeled.quizoverlay.ui.onboarding.WelcomeScreen
import com.reeled.quizoverlay.util.PermissionChecker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Loading : Screen("loading")
    object Welcome : Screen("welcome")
    object Consent : Screen("consent")
    object PinSetup : Screen("pin_setup")
    object PermissionOverlay : Screen("permission_overlay")
    object PermissionUsage : Screen("permission_usage")
    object AppSelection : Screen("app_selection")
    object PermissionNotif : Screen("permission_notif")
    object BatteryOpt : Screen("battery_opt")
    object Success : Screen("success")
    object ParentDashboard : Screen("parent_dashboard")
    object DevMode : Screen("dev_mode")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val application = context.applicationContext as Application

    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.provideFactory(application)
    )
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(application)
    )
    val devModeViewModel: DevModeViewModel = viewModel(
        factory = DevModeViewModel.provideFactory(application)
    )
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsState(initial = false)
    val monitoredApps by onboardingViewModel.monitoredApps.collectAsState(initial = emptySet())

    fun launchIntent(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    suspend fun nextOnboardingRoute(): String {
        if (!PermissionChecker.hasOverlayPermission(context)) return Screen.PermissionOverlay.route
        if (!PermissionChecker.hasUsageStatsPermission(context)) return Screen.PermissionUsage.route
        if (monitoredApps.isEmpty()) return Screen.AppSelection.route
        val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionChecker.hasNotificationPermission(context)
        } else {
            true
        }
        if (!notificationsEnabled) return Screen.PermissionNotif.route
        val batteryOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionChecker.isIgnoringBatteryOptimizations(context)
        } else {
            true
        }
        if (!batteryOk) return Screen.BatteryOpt.route
        return Screen.Success.route
    }

    suspend fun navigateToNextOnboardingStep() {
        val destination = nextOnboardingRoute()
        if (navController.currentDestination?.route != destination) {
            navController.navigate(destination) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Loading.route) {
            LoadingScreen(onLoadingComplete = {
                scope.launch {
                    val destination = if (onboardingComplete) Screen.ParentDashboard.route else Screen.Welcome.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Loading.route) { inclusive = true }
                    }
                }
            })
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(onNext = { navController.navigate(Screen.Consent.route) })
        }

        composable(Screen.Consent.route) {
            ConsentScreen(
                onAccepted = { nickname ->
                    onboardingViewModel.onConsentAccepted(nickname)
                    navController.navigate(Screen.PinSetup.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PinSetup.route) {
            PinSetupScreen(
                onPinSet = { pin ->
                    onboardingViewModel.onPinSet(pin)
                    scope.launch {
                        navigateToNextOnboardingStep()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PermissionOverlay.route) {
            PermissionOverlayScreen(
                onNext = {
                    scope.launch {
                        if (PermissionChecker.hasOverlayPermission(context)) {
                            navigateToNextOnboardingStep()
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                onOpenSettings = {
                    launchIntent(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            putExtra("package", context.packageName)
                        }
                    )
                }
            )
        }

        composable(Screen.PermissionUsage.route) {
            PermissionUsageScreen(
                onNext = {
                    scope.launch {
                        if (PermissionChecker.hasUsageStatsPermission(context)) {
                            navigateToNextOnboardingStep()
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                onGrantAccess = {
                    launchIntent(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    )
                }
            )
        }

        composable(Screen.AppSelection.route) {
            AppSelectionScreen(
                onNext = {
                    scope.launch {
                        navigateToNextOnboardingStep()
                    }
                },
                onBack = { navController.popBackStack() },
                initialMonitoredApps = monitoredApps,
                onSaveSelection = onboardingViewModel::saveMonitoredApps
            )
        }

        composable(Screen.PermissionNotif.route) {
            PermissionNotifScreen(
                onNext = {
                    scope.launch {
                        navigateToNextOnboardingStep()
                    }
                },
                onBack = { navController.popBackStack() },
                onAllowNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launchIntent(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                            putExtra("permission", Manifest.permission.POST_NOTIFICATIONS)
                        })
                    }
                }
            )
        }

        composable(Screen.BatteryOpt.route) {
            BatteryOptScreen(
                onNext = {
                    scope.launch {
                        if (PermissionChecker.isIgnoringBatteryOptimizations(context)) {
                            navigateToNextOnboardingStep()
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                onDisableOptimization = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val pm = context.getSystemService(PowerManager::class.java)
                        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                            launchIntent(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    }
                }
            )
        }

        composable(Screen.Success.route) {
            OnboardingSuccessScreen(
                onEnterChildMode = {
                    onboardingViewModel.onOnboardingCompleted()
                    navController.navigate(Screen.ParentDashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onGoToDashboard = {
                    onboardingViewModel.onOnboardingCompleted()
                    navController.navigate(Screen.ParentDashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ParentDashboard.route) {
            ParentDashboardScreen(
                dashboardViewModel = dashboardViewModel,
                devModeViewModel = devModeViewModel
            )
        }

        composable(Screen.DevMode.route) {
            DevModeScreen(viewModel = devModeViewModel)
        }
    }
}
