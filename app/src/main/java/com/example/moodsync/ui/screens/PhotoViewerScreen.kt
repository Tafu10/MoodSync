package com.example.moodsync.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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
    val metadataFile = File(photoFile.parent, "\${photoFile.nameWithoutExtension}.json")
    
    var mood by remember { mutableStateOf("Unknown") }
    var songUri by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(photoPath) {
        if (metadataFile.exists()) {
            try {
                val jsonText = metadataFile.readText()
                val json = JSONObject(jsonText)
                mood = json.getString("mood")
                songUri = json.getString("songUri")
                
                // Trigger Spotify to play the contextual track!
                songUri?.let { uri ->
                    viewModel.playSpotifyUri(uri)
                }
            } catch (e: Exception) {
                // Parse error
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val bitmap = remember(photoFile) { BitmapFactory.decodeFile(photoFile.absolutePath) }
        
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Fullscreen Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mood: \$mood",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateBack) {
                Text("Back to Collections")
            }
        }
    }
}
