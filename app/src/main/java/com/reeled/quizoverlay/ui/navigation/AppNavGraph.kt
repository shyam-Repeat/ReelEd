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
            ConsentScreen(onAccepted = { navController.navigate(Screen.PinSetup.route) })
        }
        composable(Screen.PinSetup.route) {
            // Placeholder for PinSetupScreen which needs prefs
            Text("Pin Setup Screen") 
        }
        composable(Screen.PermissionOverlay.route) {
            PermissionOverlayScreen(onNext = { navController.navigate(Screen.PermissionUsage.route) })
        }
        composable(Screen.PermissionUsage.route) {
            PermissionUsageScreen(onNext = { navController.navigate(Screen.PermissionNotif.route) })
        }
        composable(Screen.PermissionNotif.route) {
            PermissionNotifScreen(onNext = { navController.navigate(Screen.BatteryOpt.route) })
        }
        composable(Screen.BatteryOpt.route) {
            BatteryOptScreen(onNext = { 
                // Mark onboarding complete and go home
                navController.navigate(Screen.ChildHome.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            })
        }
        composable(Screen.ChildHome.route) {
            ChildHomeScreen(onParentClick = { navController.navigate(Screen.ParentDashboard.route) })
        }
        composable(Screen.ParentDashboard.route) {
            ParentDashboardScreen(viewModel = DashboardViewModel())
        }
    }
}

// Temporary for compilation
@androidx.compose.runtime.Composable
fun Text(text: String) {
    androidx.compose.material3.Text(text = text)
}
