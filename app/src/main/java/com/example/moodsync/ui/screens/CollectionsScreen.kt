package com.example.moodsync.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moodsync.viewmodel.MoodViewModel
import java.io.File

@Composable
fun CollectionsScreen(
    viewModel: MoodViewModel,
    onPhotoSelected: (File) -> Unit
) {
    val activeProfile by viewModel.activeCollectionSubject.collectAsState()
    val refreshTrigger by viewModel.photoRefreshTrigger.collectAsState()
    val context = LocalContext.current
    
    var photos by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(activeProfile, refreshTrigger) {
        val profile = activeProfile
        if (profile != null) {
            val profileDir = File(context.filesDir, "MoodSync/$profile")
            if (profileDir.exists()) {
                val allPhotos = profileDir.listFiles()?.flatMap { moodDir ->
                    moodDir.listFiles { file -> file.extension == "jpg" }?.toList() ?: emptyList()
                } ?: emptyList()
                photos = allPhotos.sortedByDescending { it.lastModified() }
            } else {
                photos = emptyList()
            }
        } else {
            photos = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 32.dp)
    ) {
        Text(
            text = (activeProfile ?: "No Profile") + "'s Collections",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (activeProfile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Scan Face on the Camera tab to view collections.")
            }
        } else if (photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No photos captured yet. Swipe left and take some!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { photoFile ->
                    val bitmap = remember(photoFile) { BitmapFactory.decodeFile(photoFile.absolutePath) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Mood Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onPhotoSelected(photoFile) }
                        )
                    }
                }
            }
        }
    }
}
