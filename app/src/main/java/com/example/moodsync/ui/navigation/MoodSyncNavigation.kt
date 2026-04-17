package com.example.moodsync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.moodsync.ui.screens.MainScreen
import com.example.moodsync.ui.screens.PermissionScreen

object Routes {
    const val PERMISSION_SCREEN = "permission_screen"
    const val MAIN_SCREEN = "main_screen"
}

@Composable
fun MoodSyncNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.PERMISSION_SCREEN) {
        composable(Routes.PERMISSION_SCREEN) {
            PermissionScreen(
                onPermissionGranted = {
                    navController.navigate(Routes.MAIN_SCREEN) {
                        popUpTo(Routes.PERMISSION_SCREEN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.MAIN_SCREEN) {
            MainScreen()
        }
    }
}
