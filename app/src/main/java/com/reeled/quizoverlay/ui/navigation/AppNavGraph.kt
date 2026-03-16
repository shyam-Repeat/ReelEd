package com.reeled.quizoverlay.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.reeled.quizoverlay.ui.childhome.ChildHomeScreen
import com.reeled.quizoverlay.ui.dashboard.ParentDashboardScreen
import com.reeled.quizoverlay.ui.dashboard.DashboardViewModel
import com.reeled.quizoverlay.ui.onboarding.*
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
    object ChildHome : Screen("child_home")
    object ParentDashboard : Screen("parent_dashboard")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appPrefs = androidx.compose.runtime.remember { com.reeled.quizoverlay.prefs.AppPrefs(context) }
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Loading.route) {
            LoadingScreen(onLoadingComplete = {
                navController.navigate(Screen.Welcome.route) {
                    popUpTo(Screen.Loading.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Welcome.route) {
            WelcomeScreen(onNext = { navController.navigate(Screen.Consent.route) })
        }
        composable(Screen.Consent.route) {
            ConsentScreen(
                onAccepted = { navController.navigate(Screen.PinSetup.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PinSetup.route) {
            PinSetupScreen(
                onPinSet = { pin -> 
                    // Save PIN logic would go here
                    navController.navigate(Screen.PermissionOverlay.route) 
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PermissionOverlay.route) {
            PermissionOverlayScreen(
                onNext = { navController.navigate(Screen.PermissionUsage.route) },
                onBack = { navController.popBackStack() },
                onOpenSettings = { 
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.data = android.net.Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                }
            )
        }
        composable(Screen.PermissionUsage.route) {
            PermissionUsageScreen(
                onNext = { navController.navigate(Screen.AppSelection.route) },
                onBack = { navController.popBackStack() },
                onGrantAccess = { 
                    val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }
        composable(Screen.AppSelection.route) {
            val monitoredApps by appPrefs.monitoredApps.collectAsState(initial = emptySet())
            AppSelectionScreen(
                onNext = { navController.navigate(Screen.PermissionNotif.route) },
                onBack = { navController.popBackStack() },
                initialMonitoredApps = monitoredApps,
                onSaveSelection = { apps -> 
                    scope.launch {
                        appPrefs.setMonitoredApps(apps)
                    }
                }
            )
        }
        composable(Screen.PermissionNotif.route) {
            PermissionNotifScreen(
                onNext = { navController.navigate(Screen.BatteryOpt.route) },
                onBack = { navController.popBackStack() },
                onAllowNotifications = { 
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    context.startActivity(intent)
                }
            )
        }
        composable(Screen.BatteryOpt.route) {
            BatteryOptScreen(
                onNext = { navController.navigate(Screen.Success.route) },
                onBack = { navController.popBackStack() },
                onDisableOptimization = { 
                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = android.net.Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                }
            )
        }
        composable(Screen.Success.route) {
            OnboardingSuccessScreen(
                onEnterChildMode = {
                    scope.launch { appPrefs.setOnboardingComplete(true) }
                    navController.navigate(Screen.ChildHome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ChildHome.route) {
            val pinPrefs = androidx.compose.runtime.remember { com.reeled.quizoverlay.prefs.PinPrefs(context) }
            ChildHomeScreen(
                pinPrefs = pinPrefs,
                onNavigateToDashboard = { navController.navigate(Screen.ParentDashboard.route) }
            )
        }
        composable(Screen.ParentDashboard.route) {
            ParentDashboardScreen(viewModel = DashboardViewModel())
        }
    }
}
