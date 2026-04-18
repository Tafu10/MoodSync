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

class MoodViewModel(application: Application) : AndroidViewModel(application) {
    // Shared Preferences for Persistence
    private val prefs = application.getSharedPreferences("MoodSyncPrefs", Context.MODE_PRIVATE)

    // ML Model
    private val faceNetModel = FaceNetModel(application)

    // Current Bounding Box and Cropped Face
    private val _faceBoundingBox = MutableStateFlow<Rect?>(null)
    val faceBoundingBox: StateFlow<Rect?> = _faceBoundingBox.asStateFlow()

    private var currentCroppedFace: Bitmap? = null

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

    fun updateFaceBoundingBox(rect: Rect?, faceBitmap: Bitmap?) {
        _faceBoundingBox.value = rect
        currentCroppedFace = faceBitmap
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
    }
}
