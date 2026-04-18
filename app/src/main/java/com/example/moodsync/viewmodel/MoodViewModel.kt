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
import com.example.moodsync.ml.EmotionClassifier

class MoodViewModel(application: Application) : AndroidViewModel(application) {
    // Shared Preferences for Persistence
    private val prefs = application.getSharedPreferences("MoodSyncPrefs", Context.MODE_PRIVATE)

    // ML Models & Integrations
    private val faceNetModel = FaceNetModel(application)
    private val emotionClassifier = EmotionClassifier(application)
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

    // Face Recognition State
    private var registeredEmbedding: FloatArray? = null
    
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    init {
        // Load the saved face signature if it exists
        val savedFace = prefs.getString("registered_face", null)
        if (savedFace != null) {
            registeredEmbedding = savedFace.split(",").map { it.toFloat() }.toFloatArray()
            _isRegistered.value = true
        }
    }

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

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
                    // Start playing based on current emotion if available
                    currentEmotion.value?.let { spotifyHelper.playPlaylistForEmotion(it) }
                },
                onTrackChanged = { trackName, trackImage ->
                    _currentTrack.value = trackName
                    _currentTrackImage.value = trackImage
                },
                onFailure = { error ->
                    _currentTrack.value = "Failed: $error"
                }
            )
        }
    }

    private val _isEmotionFrozen = MutableStateFlow(false)
    val isEmotionFrozen: StateFlow<Boolean> = _isEmotionFrozen.asStateFlow()

    fun toggleEmotionFreeze() {
        _isEmotionFrozen.value = !_isEmotionFrozen.value
    }

    fun skipNextTrack() {
        spotifyHelper.skipNext()
    }

    fun updateFaceBoundingBox(rect: Rect?, faceBitmap: Bitmap?) {
        _faceBoundingBox.value = rect
        currentCroppedFace = faceBitmap
        
        // Continuously scan for emotion if a face is detected and NOT frozen
        faceBitmap?.let {
            if (!_isEmotionFrozen.value) {
                val emotion = emotionClassifier.analyzeEmotion(it)
                _currentEmotion.value = emotion
                
                // If unlocked (in Gallery), trigger Spotify
                if (_isUnlocked.value) {
                    spotifyHelper.playPlaylistForEmotion(emotion)
                }
            }
        } ?: run {
            if (!_isEmotionFrozen.value) {
                _currentEmotion.value = null
            }
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
                    _verificationMessage.value = "Access Denied. Distance: \$distance"
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
        emotionClassifier.close()
        spotifyHelper.disconnect()
    }
}
