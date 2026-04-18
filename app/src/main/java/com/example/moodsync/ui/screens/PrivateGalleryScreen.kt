package com.example.moodsync.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.graphics.asImageBitmap
import com.spotify.sdk.android.auth.AuthorizationClient
import com.example.moodsync.camera.CameraPreview
import com.example.moodsync.ml.FaceAnalyzer
import com.example.moodsync.viewmodel.MoodViewModel

@Composable
fun PrivateGalleryScreen(
    viewModel: MoodViewModel,
    onNavigateBack: () -> Unit,
    onClearFace: () -> Unit
) {
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val context = LocalContext.current
    
    // Extract real Activity context from Compose ContextWrapper
    var activityContext: android.app.Activity? = null
    var currentContext = context
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) {
            activityContext = currentContext
            break
        }
        currentContext = currentContext.baseContext
    }

    val faceAnalyzer = remember {
        FaceAnalyzer { rect, bitmap ->
            viewModel.updateFaceBoundingBox(rect, bitmap)
        }
    }

    val authLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = com.spotify.sdk.android.auth.AuthorizationClient.getResponse(result.resultCode, result.data)
        when (response.type) {
            com.spotify.sdk.android.auth.AuthorizationResponse.Type.TOKEN -> {
                // Success! The Spotify backend has granted permission.
                // We can now connect App Remote safely.
                viewModel.connectToSpotify(activityContext ?: context)
            }
            com.spotify.sdk.android.auth.AuthorizationResponse.Type.ERROR -> {
                Toast.makeText(context, "Auth Error: ${response.error}", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(context, "Auth Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (activityContext != null) {
            // Launch the explicit Auth Flow first
            val request = viewModel.getSpotifyAuthRequest()
            val intent = AuthorizationClient.createLoginActivityIntent(activityContext, request)
            authLauncher.launch(intent)
        } else {
            // Fallback just in case
            viewModel.connectToSpotify(context)
        }
    }

    // Determine the color overlay based on the current emotion
    val overlayColor = when (currentEmotion) {
        "Happy" -> Color(0x66FFC107) // Warm Golden
        "Sad" -> Color(0x662196F3) // Cool Blue
        "Surprise" -> Color(0x66E91E63) // Vibrant Pink/Purple
        "Angry" -> Color(0x66F44336) // Red
        "Fear" -> Color(0x669C27B0) // Deep Purple
        "Disgust" -> Color(0x664CAF50) // Sickly Green
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Live camera feed acting as the background!
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            analyzer = faceAnalyzer
        )

        // The dynamic live color filter overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )

        // Top Status Bar (Emotion & Spotify Track Info)
        currentEmotion?.let { emotion ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .background(Color(0xCC000000), shape = RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vibe: $emotion",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val trackImageBitmap = viewModel.currentTrackImage.collectAsState().value
                    if (trackImageBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = trackImageBitmap.asImageBitmap(),
                            contentDescription = "Album Cover",
                            modifier = Modifier
                                .height(64.dp)
                                .width(64.dp)
                                .padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = "🎵 ${currentTrack ?: "No track playing"}",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Bottom Controls
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Music Controls Row
            Row(
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val isFrozen by viewModel.isEmotionFrozen.collectAsState()
                Button(
                    onClick = { viewModel.toggleEmotionFreeze() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isFrozen) Color(0xFF2196F3) else Color.DarkGray)
                ) {
                    Text(if (isFrozen) "▶️ Resume" else "❄️ Freeze")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { viewModel.skipNextTrack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)) // Spotify Green
                ) {
                    Text("⏭️ Next")
                }
            }

            // System Controls Row
            Row {
                Button(onClick = onNavigateBack) {
                    Text("🔒 Lock")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val current = currentEmotion ?: "Neutral"
                        Toast.makeText(context, "Saved to $current Memories!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("📸 Capture", color = Color.Black)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        onClearFace()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Erase ID")
                }
            }
        }
    }
}
