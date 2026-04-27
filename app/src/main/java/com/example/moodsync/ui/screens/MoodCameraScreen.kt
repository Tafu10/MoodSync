package com.example.moodsync.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moodsync.camera.CameraPreview
import com.example.moodsync.ml.FaceAnalyzer
import com.example.moodsync.viewmodel.CalibrationDialogState
import com.example.moodsync.viewmodel.MoodViewModel

@Composable
fun MoodCameraScreen(
    viewModel: MoodViewModel,
    onNavigateBack: () -> Unit,
    onClearFace: () -> Unit
) {
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val activeProfile by viewModel.activeCollectionSubject.collectAsState()
    val calibrationDialogState by viewModel.calibrationDialogState.collectAsState()
    val context = LocalContext.current
    
    var imageCaptureUseCase by remember { mutableStateOf<androidx.camera.core.ImageCapture?>(null) }

    val faceAnalyzer = remember {
        FaceAnalyzer { face, bitmap ->
            viewModel.updateFaceBoundingBox(face, bitmap)
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

    // Dialogs for Collection Profile Identity
    if (calibrationDialogState != null) {
        when (val state = calibrationDialogState) {
            is CalibrationDialogState.Recognized -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissCalibrationDialog() },
                    title = { Text("Profile Recognized") },
                    text = { Text("Are you " + state.name + "?") },
                    confirmButton = {
                        Button(onClick = { viewModel.confirmCollectionSubject(state.name) }) { Text("Yes") }
                    },
                    dismissButton = {
                        Button(onClick = { viewModel.dismissCalibrationDialog() }) { Text("No") }
                    }
                )
            }
            is CalibrationDialogState.NotRecognized -> {
                var newName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { viewModel.dismissCalibrationDialog() },
                    title = { Text("Face Not Recognized") },
                    text = {
                        Column {
                            Text("We don't recognize this face. Enter a name to create a new Collection Profile:")
                            OutlinedTextField(
                                value = newName, 
                                onValueChange = { newName = it },
                                modifier = Modifier.padding(top = 8.dp),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newName.isNotBlank()) {
                                viewModel.createNewCollectionProfile(newName, state.embedding)
                            }
                        }) { Text("Create Profile") }
                    },
                    dismissButton = {
                        Button(onClick = { viewModel.dismissCalibrationDialog() }) { Text("Cancel") }
                    }
                )
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            analyzer = faceAnalyzer,
            onImageCaptureReady = { capture ->
                imageCaptureUseCase = capture
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )

        // Top Status Bar (Emotion & Active Profile)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(Color(0xCC000000), shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Vibe: " + (currentEmotion ?: "Scanning..."),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Subject: " + (activeProfile ?: "Unknown"),
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Spotify Connection Status + Reconnect
        val spotifyStatus by viewModel.currentTrack.collectAsState()
        val isSpotifyFailed = spotifyStatus?.startsWith("Failed") == true || spotifyStatus == "Not Connected"
        
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // Status dot
                val (statusColor, statusText) = when {
                    spotifyStatus == "Ready to Play" -> Color(0xFF4CAF50) to "Spotify ✓"
                    spotifyStatus == "Connecting to Spotify..." -> Color(0xFFFFC107) to "Connecting..."
                    isSpotifyFailed -> Color(0xFFF44336) to "Spotify ✗"
                    spotifyStatus != null && !spotifyStatus!!.startsWith("Failed") && spotifyStatus != "Not Connected" -> Color(0xFF1DB954) to "♫ Playing"
                    else -> Color.Gray to "Spotify"
                }
                
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(Color(0xCC000000), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                
                // Show reconnect button if failed
                if (isSpotifyFailed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.connectToSpotify(context) },
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Reconnect", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isCalibrated by viewModel.isCalibrated.collectAsState()
            
            if (!isCalibrated || activeProfile == null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .background(Color(0xCC000000), shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Identify Subject & Calibrate Face", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.calibrateEmotionBaseline() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("🎯 Scan Face")
                        }
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.calibrateEmotionBaseline() },
                    modifier = Modifier.padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("🔄 Rescan Face")
                }
            }

            Row {
                Button(onClick = onNavigateBack) {
                    Text("🔒 Lock")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val current = currentEmotion ?: "Neutral"
                        imageCaptureUseCase?.let { capture ->
                            Toast.makeText(context, "Capturing...", Toast.LENGTH_SHORT).show()
                            viewModel.captureMoodPhoto(context, capture, current) { resultMsg ->
                                Toast.makeText(context, resultMsg, Toast.LENGTH_LONG).show()
                            }
                        } ?: run {
                            Toast.makeText(context, "Camera not ready", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    enabled = activeProfile != null
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
                    Text("Erase Owner ID")
                }
            }
        }
    }
}
