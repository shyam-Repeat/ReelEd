package com.reeled.quizoverlay.ui.navigation

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.reeled.quizoverlay.prefs.PinPrefs
import com.reeled.quizoverlay.ui.childhome.ChildHomeScreen
import com.reeled.quizoverlay.ui.dashboard.DashboardViewModel
import com.reeled.quizoverlay.ui.dashboard.ParentDashboardScreen
import com.reeled.quizoverlay.ui.onboarding.BatteryOptScreen
import com.reeled.quizoverlay.ui.onboarding.ConsentScreen
import com.reeled.quizoverlay.ui.onboarding.OnboardingSuccessScreen
import com.reeled.quizoverlay.ui.onboarding.OnboardingViewModel
import com.reeled.quizoverlay.ui.onboarding.PermissionNotifScreen
import com.reeled.quizoverlay.ui.onboarding.PermissionOverlayScreen
import com.reeled.quizoverlay.ui.onboarding.PermissionUsageScreen
import com.reeled.quizoverlay.ui.onboarding.PinSetupScreen
import com.reeled.quizoverlay.ui.onboarding.WelcomeScreen

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
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.provideFactory(application)
    )
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(application)
    )

    fun launchIntent(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

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
                onAccepted = {
                    onboardingViewModel.onConsentAccepted()
                    navController.navigate(Screen.PinSetup.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.PinSetup.route) {
            PinSetupScreen(
                onPinSet = { pin ->
                    onboardingViewModel.onPinSet(pin)
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
                    launchIntent(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ))
                }
            )
        }
        composable(Screen.PermissionUsage.route) {
            PermissionUsageScreen(
                onNext = { navController.navigate(Screen.PermissionNotif.route) },
                onBack = { navController.popBackStack() },
                onGrantAccess = {
                    launchIntent(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
        }
        composable(Screen.PermissionNotif.route) {
            PermissionNotifScreen(
                onNext = { navController.navigate(Screen.BatteryOpt.route) },
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
                onNext = { navController.navigate(Screen.Success.route) },
                onBack = { navController.popBackStack() },
                onDisableOptimization = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val pm = context.getSystemService(PowerManager::class.java)
                        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                            launchIntent(Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            ))
                        }
                    }
                }
            )
        }
        composable(Screen.Success.route) {
            OnboardingSuccessScreen(
                onEnterChildMode = {
                    onboardingViewModel.onOnboardingCompleted()
                    navController.navigate(Screen.ChildHome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ChildHome.route) {
            val pinPrefs = androidx.compose.runtime.remember { PinPrefs(context) }
            ChildHomeScreen(
                pinPrefs = pinPrefs,
                onNavigateToDashboard = { navController.navigate(Screen.ParentDashboard.route) }
            )
        }
        composable(Screen.ParentDashboard.route) {
            ParentDashboardScreen(viewModel = dashboardViewModel)
        }
    }
}
