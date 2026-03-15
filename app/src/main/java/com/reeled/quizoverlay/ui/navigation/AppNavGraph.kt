package com.reeled.quizoverlay.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.reeled.quizoverlay.ui.childhome.ChildHomeScreen
import com.reeled.quizoverlay.ui.dashboard.ParentDashboardScreen
import com.reeled.quizoverlay.ui.dashboard.DashboardViewModel
import com.reeled.quizoverlay.ui.onboarding.*

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Consent : Screen("consent")
    object PinSetup : Screen("pin_setup")
    object PermissionOverlay : Screen("permission_overlay")
    object PermissionUsage : Screen("permission_usage")
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
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
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
                onOpenSettings = { /* Open Settings Intent */ }
            )
        }
        composable(Screen.PermissionUsage.route) {
            PermissionUsageScreen(
                onNext = { navController.navigate(Screen.PermissionNotif.route) },
                onBack = { navController.popBackStack() },
                onGrantAccess = { /* Grant Access Intent */ }
            )
        }
        composable(Screen.PermissionNotif.route) {
            PermissionNotifScreen(
                onNext = { navController.navigate(Screen.BatteryOpt.route) },
                onBack = { navController.popBackStack() },
                onAllowNotifications = { /* Allow Notif Intent */ }
            )
        }
        composable(Screen.BatteryOpt.route) {
            BatteryOptScreen(
                onNext = { navController.navigate(Screen.Success.route) },
                onBack = { navController.popBackStack() },
                onDisableOptimization = { /* Disable Opt Intent */ }
            )
        }
        composable(Screen.Success.route) {
            OnboardingSuccessScreen(
                onEnterChildMode = {
                    navController.navigate(Screen.ChildHome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ChildHome.route) {
            ChildHomeScreen(onParentClick = { navController.navigate(Screen.ParentDashboard.route) })
        }
        composable(Screen.ParentDashboard.route) {
            ParentDashboardScreen(viewModel = DashboardViewModel())
        }
    }
}
