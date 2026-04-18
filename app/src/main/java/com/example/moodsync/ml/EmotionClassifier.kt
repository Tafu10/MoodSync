package com.example.moodsync.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class EmotionClassifier(context: Context) {
    private var interpreter: Interpreter? = null
    private var isMockMode = false

    private val emotions = arrayOf("Angry", "Disgust", "Fear", "Happy", "Sad", "Surprise", "Neutral")

    init {
        try {
            val assetFileDescriptor = context.assets.openFd("emotion.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply { numThreads = 2 }
            interpreter = Interpreter(modelBuffer, options)
            Log.d("EmotionClassifier", "emotion.tflite loaded successfully!")
        } catch (e: Exception) {
            Log.e("EmotionClassifier", "emotion.tflite not found! Falling back to MOCK mode.", e)
            isMockMode = true
        }
    }

    fun analyzeEmotion(bitmap: Bitmap): String {
        if (isMockMode || interpreter == null) {
            // MOCK MODE: Cycle through an emotion every 5 seconds to prevent rapid flickering
            val timeMs = System.currentTimeMillis()
            val index = ((timeMs / 5000) % 7).toInt()
            return emotions[index]
        }

        // REAL TFLITE INFERENCE:
        // 1. Resize to 48x48
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
        
        // 2. Convert to Grayscale ByteBuffer [1, 48, 48, 1]
        val inputBuffer = ByteBuffer.allocateDirect(1 * 48 * 48 * 1 * 4) // 4 bytes per float
        inputBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(48 * 48)
        resizedBitmap.getPixels(intValues, 0, 48, 0, 0, 48, 48)
        
        for (pixelValue in intValues) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)
            
            // Standard grayscale conversion
            val gray = (r * 0.299f + g * 0.587f + b * 0.114f)
            
            // Normalize to [0, 1]
            inputBuffer.putFloat(gray / 255.0f)
        }

        // 3. Output buffer [1, 7]
        val outputBuffer = Array(1) { FloatArray(7) }

        // 4. Run inference
        interpreter?.run(inputBuffer, outputBuffer)

        // 5. Find highest probability
        val probabilities = outputBuffer[0]
        var maxIdx = 0
        var maxProb = probabilities[0]
        for (i in 1 until 7) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIdx = i
            }
        }
        
        return emotions[maxIdx]
    }

    fun close() {
        interpreter?.close()
    }
}
