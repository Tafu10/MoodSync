package com.example.moodsync.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodsync.camera.CameraPreview
import com.example.moodsync.ml.FaceAnalyzer
import com.example.moodsync.viewmodel.MoodViewModel

@Composable
fun MainScreen(
    viewModel: MoodViewModel = viewModel(),
    onNavigateToGallery: () -> Unit = {}
) {
    val faceBoundingBox by viewModel.faceBoundingBox.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val isVerifying by viewModel.isVerifying.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()
    val verificationMessage by viewModel.verificationMessage.collectAsState()
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val authLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = com.spotify.sdk.android.auth.AuthorizationClient.getResponse(result.resultCode, result.data)
        if (response.type == com.spotify.sdk.android.auth.AuthorizationResponse.Type.TOKEN) {
            viewModel.spotifyAccessToken = response.accessToken
            android.util.Log.d("SpotifyAuth", "Got token: ${response.accessToken}")
        }
    }

    val faceAnalyzer = remember {
        FaceAnalyzer { rect, bitmap ->
            viewModel.updateFaceBoundingBox(rect, bitmap)
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            viewModel.connectToSpotify(context)
            onNavigateToGallery()
        }
    }

    LaunchedEffect(verificationMessage) {
        verificationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Live camera feed
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            analyzer = faceAnalyzer
        )

        // Emotion Badge removed for privacy and performance on Lock Screen

        // Simple Face Detection indicator overlay
        val (text, bgColor) = if (faceBoundingBox != null) {
            "Face Detected!" to Color(0x994CAF50) // Semi-transparent Green
        } else {
            "No face found" to Color(0x99F44336) // Semi-transparent Red
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .background(bgColor, shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Web API Login Button
        if (viewModel.spotifyAccessToken == null && isUnlocked) {
            Button(
                onClick = {
                    val intent = com.spotify.sdk.android.auth.AuthorizationClient.createLoginActivityIntent(
                        activity,
                        viewModel.getSpotifyAuthRequest()
                    )
                    authLauncher.launch(intent)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(top = 32.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
            ) {
                Text("Link Spotify API")
            }
        }

        // Custom Face ID Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            if (!isRegistered) {
                Button(
                    onClick = { viewModel.registerFace() }
                ) {
                    Text("Register My Face")
                }
            } else {
                Button(
                    onClick = { viewModel.verifyFaceMatch() },
                    enabled = !isVerifying
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(end = 8.dp))
                        Text("Scanning...")
                    } else {
                        Text("🔒 Verify")
                    }
                }
            }
        }
    }
}
