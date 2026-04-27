package com.example.moodsync.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodsync.ml.FaceNetModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.google.mlkit.vision.face.Face
import com.example.moodsync.ml.HeuristicEmotionEngine
import java.io.File
import androidx.core.content.ContextCompat

sealed class CalibrationDialogState {
    data class Recognized(val name: String) : CalibrationDialogState()
    data class NotRecognized(val embedding: FloatArray) : CalibrationDialogState()
}

class MoodViewModel(application: Application) : AndroidViewModel(application) {
    // Shared Preferences for Persistence
    private val prefs = application.getSharedPreferences("MoodSyncPrefs", Context.MODE_PRIVATE)

    // ML Models & Integrations
    private val faceNetModel = FaceNetModel(application)
    private val heuristicEngine = HeuristicEmotionEngine()
    private val spotifyHelper = com.example.moodsync.spotify.SpotifyHelper()

    // Current Bounding Box and Cropped Face
    private val _faceBoundingBox = MutableStateFlow<Rect?>(null)
    val faceBoundingBox: StateFlow<Rect?> = _faceBoundingBox.asStateFlow()

    private var currentCroppedFace: Bitmap? = null

    // Emotion State
    private val _currentEmotion = MutableStateFlow<String?>(null)
    val currentEmotion: StateFlow<String?> = _currentEmotion.asStateFlow()

    // Spotify Track State
    private val _currentTrack = MutableStateFlow<String?>("Not Connected")
    val currentTrack: StateFlow<String?> = _currentTrack.asStateFlow()

    private val _currentTrackImage = MutableStateFlow<android.graphics.Bitmap?>(null)
    val currentTrackImage: StateFlow<android.graphics.Bitmap?> = _currentTrackImage.asStateFlow()



    private val _currentTrackUri = MutableStateFlow<String?>("spotify:track:7ouMYWcgJqo60a2vKkM7tT") // default fallback
    val currentTrackUri: StateFlow<String?> = _currentTrackUri.asStateFlow()

    // Face Recognition State
    private var registeredEmbedding: FloatArray? = null
    
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    // Multi-User Collections State
    private val collectionProfiles = mutableMapOf<String, FloatArray>()
    
    private val _activeCollectionSubject = MutableStateFlow<String?>(null)
    val activeCollectionSubject: StateFlow<String?> = _activeCollectionSubject.asStateFlow()

    private val _photoRefreshTrigger = MutableStateFlow(0)
    val photoRefreshTrigger: StateFlow<Int> = _photoRefreshTrigger.asStateFlow()

    private val _calibrationDialogState = MutableStateFlow<CalibrationDialogState?>(null)
    val calibrationDialogState: StateFlow<CalibrationDialogState?> = _calibrationDialogState.asStateFlow()

    init {
        // Load the saved App Owner face signature
        val savedFace = prefs.getString("registered_face", null)
        if (savedFace != null) {
            registeredEmbedding = savedFace.split(",").map { it.toFloat() }.toFloatArray()
            _isRegistered.value = true
        }

        // Load Collection Profiles
        val savedProfiles = prefs.getStringSet("collection_profiles", null)
        savedProfiles?.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val name = parts[0]
                val embedding = parts[1].split(",").map { it.toFloat() }.toFloatArray()
                collectionProfiles[name] = embedding
            }
        }
    }

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated: StateFlow<Boolean> = _isCalibrated.asStateFlow()
    
    private var shouldCalibrateOnNextFrame = false

    fun calibrateEmotionBaseline() {
        shouldCalibrateOnNextFrame = true
    }

    fun confirmCollectionSubject(name: String) {
        _activeCollectionSubject.value = name
        _calibrationDialogState.value = null
    }

    fun createNewCollectionProfile(name: String, embedding: FloatArray) {
        collectionProfiles[name] = embedding
        _activeCollectionSubject.value = name
        _calibrationDialogState.value = null

        // Save to SharedPreferences
        val serializedProfiles = collectionProfiles.map { it.key + ":" + it.value.joinToString(",") }.toSet()
        prefs.edit().putStringSet("collection_profiles", serializedProfiles).apply()
    }

    fun dismissCalibrationDialog() {
        _calibrationDialogState.value = null
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _verificationMessage = MutableStateFlow<String?>(null)
    val verificationMessage: StateFlow<String?> = _verificationMessage.asStateFlow()

    fun getSpotifyAuthRequest(): AuthorizationRequest {
        return spotifyHelper.getAuthRequest()
    }

    fun connectToSpotify(context: Context) {
        // Only connect if we haven't already (allow retries if it failed previously)
        if (_currentTrack.value != "Connecting to Spotify...") {
            _currentTrack.value = "Connecting to Spotify..."
            
            // Timeout failsafe for Android 15 background service blocking
            viewModelScope.launch {
                delay(8000)
                if (_currentTrack.value == "Connecting to Spotify...") {
                    _currentTrack.value = "Failed: Spotify App blocked connection (Open Spotify manually to wake it up)"
                }
            }

            spotifyHelper.connect(
                context = context,
                onConnected = {
                    // We no longer auto-play music on the camera screen!
                    // Update _currentTrack so the 8-second timeout doesn't incorrectly mark it as failed.
                    if (_currentTrack.value == "Connecting to Spotify...") {
                        _currentTrack.value = "Ready to Play"
                    }
                },
                onTrackChanged = { trackName, trackImage ->
                    _currentTrack.value = trackName
                    _currentTrackImage.value = trackImage
                    // Currently Spotify App Remote doesn't easily expose full URI on every track change without heavy callback setup,
                    // so we'll just save a placeholder if we can't get it.
                },
                onFailure = { error ->
                    _currentTrack.value = "Failed: $error"
                }
            )
        }
    }

    fun playSpotifyUri(context: Context, uri: String) {
        spotifyHelper.playUri(context, uri)
    }

    fun playMoodPlaylist(context: Context, mood: String) {
        spotifyHelper.playPlaylistForEmotion(context, mood)
    }

    fun pauseSpotify() {
        spotifyHelper.pause()
    }

    fun resetTrackState() {
        _currentTrack.value = "Ready to Play"
        _currentTrackImage.value = null
    }

    private val _isEmotionFrozen = MutableStateFlow(false)
    val isEmotionFrozen: StateFlow<Boolean> = _isEmotionFrozen.asStateFlow()

    fun toggleEmotionFreeze() {
        _isEmotionFrozen.value = !_isEmotionFrozen.value
    }

    fun skipNextTrack() {
        spotifyHelper.skipNext()
    }

    fun updateFaceBoundingBox(face: Face?, faceBitmap: Bitmap?) {
        _faceBoundingBox.value = face?.boundingBox
        currentCroppedFace = faceBitmap
        
        if (face != null) {
            if (shouldCalibrateOnNextFrame) {
                heuristicEngine.calibrate(face)
                shouldCalibrateOnNextFrame = false
                _isCalibrated.value = true

                // Also run FaceNet verification for Collections
                currentCroppedFace?.let { bitmap ->
                    viewModelScope.launch {
                        try {
                            val liveEmbedding = faceNetModel.getFaceEmbedding(bitmap)
                            var bestMatchName: String? = null
                            var bestMatchDistance = Float.MAX_VALUE

                            // Find the closest registered profile
                            for ((name, targetEmbedding) in collectionProfiles) {
                                val distance = faceNetModel.calculateDistance(liveEmbedding, targetEmbedding)
                                if (distance < 1.0f && distance < bestMatchDistance) {
                                    bestMatchDistance = distance
                                    bestMatchName = name
                                }
                            }

                            if (bestMatchName != null) {
                                _calibrationDialogState.value = CalibrationDialogState.Recognized(bestMatchName)
                            } else {
                                _calibrationDialogState.value = CalibrationDialogState.NotRecognized(liveEmbedding)
                            }
                        } catch (e: Exception) {
                            // Ignored or logged
                        }
                    }
                }
            }

            // Continuously scan for emotion if a face is detected and NOT frozen
            if (!_isEmotionFrozen.value) {
                val emotion = heuristicEngine.analyzeEmotion(face)
                _currentEmotion.value = emotion
                
                // If unlocked (in Gallery), trigger Spotify
                if (_isUnlocked.value) {
                    // spotifyHelper.playPlaylistForEmotion(emotion) // Temporarily Disabled
                }
            }
        } else if (!_isEmotionFrozen.value) {
            _currentEmotion.value = null
        }
    }

    fun registerFace() {
        val bitmap = currentCroppedFace ?: return
        viewModelScope.launch {
            try {
                val embedding = faceNetModel.getFaceEmbedding(bitmap)
                registeredEmbedding = embedding
                
                // Save to local storage so it survives app restarts
                prefs.edit().putString("registered_face", embedding.joinToString(",")).apply()
                
                _isRegistered.value = true
                _verificationMessage.value = "Face registered successfully!"
            } catch (e: Exception) {
                _verificationMessage.value = "Failed to register face."
            }
        }
    }

    fun verifyFaceMatch() {
        if (_isVerifying.value) return
        val bitmap = currentCroppedFace
        val targetEmbedding = registeredEmbedding

        if (bitmap == null) {
            _verificationMessage.value = "No face detected in frame."
            return
        }
        if (targetEmbedding == null) {
            _verificationMessage.value = "Please register a face first."
            return
        }

        viewModelScope.launch {
            _isVerifying.value = true
            try {
                val liveEmbedding = faceNetModel.getFaceEmbedding(bitmap)
                val distance = faceNetModel.calculateDistance(liveEmbedding, targetEmbedding)
                
                // Typical threshold for MobileFaceNet is around 1.0
                if (distance < 1.0f) {
                    _isUnlocked.value = true
                    _verificationMessage.value = "Access Granted"
                } else {
                    _verificationMessage.value = "Access Denied. Distance: $distance"
                }
            } catch (e: Exception) {
                _verificationMessage.value = "Error during verification."
            }
            _isVerifying.value = false
        }
    }

    fun lockGallery() {
        _isUnlocked.value = false
        _verificationMessage.value = null
    }
    
    fun clearMessage() {
        _verificationMessage.value = null
    }

    fun captureMoodPhoto(context: Context, imageCapture: androidx.camera.core.ImageCapture, mood: String, onComplete: (String) -> Unit) {
        val activeProfile = _activeCollectionSubject.value ?: "Unknown"
        val profileDir = File(context.filesDir, "MoodSync/$activeProfile/$mood")
        if (!profileDir.exists()) profileDir.mkdirs()

        val photoFile = File(profileDir, "IMG_${System.currentTimeMillis()}.jpg")
        val outputOptions = androidx.camera.core.ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : androidx.camera.core.ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: androidx.camera.core.ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        val metadataFile = File(photoFile.parent, "${photoFile.nameWithoutExtension}.json")
                        // Spotify Web API restricts access for Developer Apps,
                        // so we pull from the internal massive TrackDatabase of 100 global hits!
                        val dynamicTrack = com.example.moodsync.spotify.TrackDatabase.getRandomTrack(mood)
                        val trackName = dynamicTrack.first
                        val trackUri = dynamicTrack.second
                        
                        metadataFile.writeText("{\"mood\": \"$mood\", \"songUri\": \"$trackUri\", \"songName\": \"$trackName\"}")
                        _photoRefreshTrigger.value += 1
                        onComplete("Saved to $activeProfile's $mood collection!")
                    }
                }

                override fun onError(exc: androidx.camera.core.ImageCaptureException) {
                    onComplete("Failed to save photo: ${exc.message}")
                }
            }
        )
    }

    fun deletePhoto(photoPath: String) {
        val photoFile = File(photoPath)
        val metadataFile = File(photoFile.parent, "${photoFile.nameWithoutExtension}.json")
        
        if (photoFile.exists()) photoFile.delete()
        if (metadataFile.exists()) metadataFile.delete()
        
        // Trigger a refresh so the UI updates
        _photoRefreshTrigger.value += 1
    }

    fun clearRegisteredFace() {
        registeredEmbedding = null
        prefs.edit().remove("registered_face").apply()
        _isRegistered.value = false
        _isUnlocked.value = false
        _verificationMessage.value = "Registered face cleared."
    }

    override fun onCleared() {
        super.onCleared()
        faceNetModel.close()
        spotifyHelper.disconnect()
    }
}
