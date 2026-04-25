package com.example.moodsync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.moodsync.viewmodel.MoodViewModel
import org.json.JSONObject
import java.io.File

@Composable
fun PhotoViewerScreen(
    photoPath: String,
    viewModel: MoodViewModel,
    onNavigateBack: () -> Unit
) {
    val photoFile = File(photoPath)
    val metadataFile = File(photoFile.parent, "${photoFile.nameWithoutExtension}.json")
    
    var mood by remember { mutableStateOf("Unknown") }
    var songUri by remember { mutableStateOf<String?>(null) }
    var songName by remember { mutableStateOf<String?>("No Song Data") }
    val context = LocalContext.current
    
    LaunchedEffect(photoPath) {
        if (metadataFile.exists()) {
            try {
                val jsonText = metadataFile.readText()
                val json = JSONObject(jsonText)
                mood = json.getString("mood")
                songUri = json.optString("songUri")
                songName = json.optString("songName", "Unknown Track")
                
                // Trigger Spotify to play the contextual track!
                songUri?.let { uri ->
                    viewModel.resetTrackState()
                    viewModel.playSpotifyUri(context, uri)
                }
            } catch (e: Exception) {
                // Parse error
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.pauseSpotify()
        }
    }

    val liveTrack by viewModel.currentTrack.collectAsState()
    val displayTrack = if (liveTrack != null && liveTrack != "Not Connected" && !liveTrack!!.startsWith("Failed") && liveTrack != "Ready to Play" && liveTrack != "Connecting to Spotify...") liveTrack else songName

    // Determine the color overlay based on the detected mood
    val overlayColor = when (mood) {
        "Happy" -> Color(0x66FFC107) // Warm Golden
        "Sad" -> Color(0x662196F3) // Cool Blue
        "Surprise" -> Color(0x66E91E63) // Vibrant Pink/Purple
        "Angry" -> Color(0x66F44336) // Red
        "Fear" -> Color(0x669C27B0) // Deep Purple
        "Disgust" -> Color(0x664CAF50) // Sickly Green
        else -> Color.Transparent
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        AsyncImage(
            model = photoFile,
            contentDescription = "Fullscreen Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        
        // Graphical Overlay
        Box(modifier = Modifier.fillMaxSize().background(overlayColor))
        
        // Top Banner for Song Info
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(Color(0xCC000000), shape = MaterialTheme.shapes.large)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Playing linked track:", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                Text(displayTrack ?: "Unknown", color = Color(0xFF1DB954), style = MaterialTheme.typography.titleMedium)
            }
        }
        
        // Bottom Banner for Mood Info
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xCC000000), shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Mood: $mood",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateBack) {
                Text("Back to Collections")
            }
        }
    }
}
