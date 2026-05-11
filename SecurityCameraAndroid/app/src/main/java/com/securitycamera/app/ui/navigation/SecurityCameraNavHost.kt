package com.securitycamera.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.securitycamera.app.data.PrefsRepository
import com.securitycamera.app.ui.screens.EnrollScreen
import com.securitycamera.app.ui.screens.HomeScreen
import com.securitycamera.app.ui.screens.MonitorScreen
import com.securitycamera.app.ui.screens.SettingsScreen
import com.securitycamera.app.ui.screens.UsersScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val USERS = "users"
    const val ENROLL = "enroll/{subjectId}"
    const val MONITOR = "monitor"

    fun enroll(subjectId: String) = "enroll/${android.net.Uri.encode(subjectId)}"
}

@Composable
fun SecurityCameraNavHost(
    navController: NavHostController,
    prefsRepository: PrefsRepository,
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onUsers = { navController.navigate(Routes.USERS) },
                onMonitor = { navController.navigate(Routes.MONITOR) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                prefsRepository = prefsRepository,
                onBack = { navController.popBackStack() },
                onOpenUserEnrollment = { navController.navigate(Routes.USERS) },
            )
        }
        composable(Routes.USERS) { entry ->
            UsersScreen(
                prefsRepository = prefsRepository,
                onBack = { navController.popBackStack() },
                onEnroll = { id -> navController.navigate(Routes.enroll(id)) },
                savedStateHandle = entry.savedStateHandle,
            )
        }
        composable(
            route = Routes.ENROLL,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType }),
        ) {
            val arg = it.arguments?.getString("subjectId").orEmpty()
            val subjectId = android.net.Uri.decode(arg)
            EnrollScreen(
                subjectId = subjectId,
                prefsRepository = prefsRepository,
                onBack = { navController.popBackStack() },
                onRegistered = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        RegistrationNavKeys.SUCCESS_SUBJECT_ID,
                        subjectId,
                    )
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.MONITOR) {
            MonitorScreen(
                prefsRepository = prefsRepository,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
