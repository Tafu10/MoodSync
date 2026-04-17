package com.example.moodsync.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.moodsync.camera.CameraPreview

@Composable
fun MainScreen() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Live camera feed
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onFrameCaptured = { imageProxy ->
                // ML Kit processing will go here in Step 4
            }
        )
    }
}
