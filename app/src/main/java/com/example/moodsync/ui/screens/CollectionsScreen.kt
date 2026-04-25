package com.example.moodsync.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    
    var groupedPhotos by remember { mutableStateOf<Map<String, List<File>>>(emptyMap()) }
    var photoToDelete by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(activeProfile, refreshTrigger) {
        val profile = activeProfile
        if (profile != null) {
            val profileDir = File(context.filesDir, "MoodSync/$profile")
            if (profileDir.exists()) {
                val newMap = mutableMapOf<String, List<File>>()
                profileDir.listFiles()?.forEach { moodDir ->
                    if (moodDir.isDirectory) {
                        val photos = moodDir.listFiles { file -> file.extension == "jpg" }?.toList() ?: emptyList()
                        if (photos.isNotEmpty()) {
                            newMap[moodDir.name] = photos.sortedByDescending { it.lastModified() }
                        }
                    }
                }
                groupedPhotos = newMap
            } else {
                groupedPhotos = emptyMap()
            }
        } else {
            groupedPhotos = emptyMap()
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
        } else if (groupedPhotos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No photos captured yet. Swipe left and take some!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                groupedPhotos.forEach { (mood, photos) ->
                    item {
                        Text(text = mood, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(photos) { photoFile ->
                                val overlayColor = when (mood) {
                                    "Happy" -> Color(0x66FFC107)
                                    "Sad" -> Color(0x662196F3)
                                    "Surprise" -> Color(0x66E91E63)
                                    "Angry" -> Color(0x66F44336)
                                    "Fear" -> Color(0x669C27B0)
                                    "Disgust" -> Color(0x664CAF50)
                                    else -> Color.Transparent
                                }
                                Box(modifier = Modifier
                                    .size(120.dp)
                                    .clickable { onPhotoSelected(photoFile) }
                                ) {
                                    AsyncImage(
                                        model = photoFile,
                                        contentDescription = "Mood Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(modifier = Modifier.fillMaxSize().background(overlayColor))
                                    
                                    // Delete "X" Button
                                    IconButton(
                                        onClick = { photoToDelete = photoFile },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .background(Color(0x80000000), shape = androidx.compose.foundation.shape.CircleShape)
                                    ) {
                                        Text(
                                            text = "X",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Delete Confirmation Dialog
        if (photoToDelete != null) {
            AlertDialog(
                onDismissRequest = { photoToDelete = null },
                title = { Text("Delete Photo?") },
                text = { Text("Are you sure you want to permanently delete this photo and its linked song?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            photoToDelete?.let { file ->
                                viewModel.deletePhoto(file.absolutePath)
                            }
                            photoToDelete = null
                        }
                    ) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { photoToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
