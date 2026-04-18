package com.example.moodsync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.moodsync.ui.screens.MainScreen
import com.example.moodsync.ui.screens.PermissionScreen
import com.example.moodsync.ui.screens.PrivateGalleryScreen
import com.example.moodsync.viewmodel.MoodViewModel

object Routes {
    const val PERMISSION_SCREEN = "permission_screen"
    const val MAIN_SCREEN = "main_screen"
    const val GALLERY_SCREEN = "gallery_screen"
}

@Composable
fun MoodSyncNavigation() {
    val navController = rememberNavController()
    // Hoist the ViewModel here so it can be shared between MainScreen and GalleryScreen
    val moodViewModel: MoodViewModel = viewModel()

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
            MainScreen(
                viewModel = moodViewModel,
                onNavigateToGallery = {
                    navController.navigate(Routes.GALLERY_SCREEN)
                }
            )
        }

        composable(Routes.GALLERY_SCREEN) {
            PrivateGalleryScreen(
                onNavigateBack = {
                    moodViewModel.lockGallery()
                    navController.popBackStack()
                },
                onClearFace = {
                    moodViewModel.clearRegisteredFace()
                }
            )
        }
    }
}
