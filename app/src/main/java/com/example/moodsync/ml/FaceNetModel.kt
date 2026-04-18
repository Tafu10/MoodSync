package com.example.moodsync.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceNetModel(context: Context) {
    private val interpreter: Interpreter

    init {
        // Load the model from assets natively
        val assetFileDescriptor = context.assets.openFd("mobilefacenet.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        val options = Interpreter.Options().apply {
            numThreads = 4
        }
        interpreter = Interpreter(modelBuffer, options)
    }

    fun getFaceEmbedding(faceBitmap: Bitmap): FloatArray {
        // 1. Resize to 112x112
        val scaledBitmap = Bitmap.createScaledBitmap(faceBitmap, 112, 112, false)
        
        // 2. Convert Bitmap to ByteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(1 * 112 * 112 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(112 * 112)
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
        
        var pixel = 0
        for (i in 0 until 112) {
            for (j in 0 until 112) {
                val value = intValues[pixel++]
                // MobileFaceNet typically normalizes inputs to [-1, 1] -> (val - 127.5) / 128
                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 128f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 128f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 128f)
            }
        }

        // 3. Run Inference
        // MobileFaceNet output is [1, 192]
        val outputBuffer = Array(1) { FloatArray(192) }
        interpreter.run(byteBuffer, outputBuffer)
        
        return outputBuffer[0]
    }

    // L2 Distance (Euclidean) - Closer to 0 means it's the same face
    fun calculateDistance(emb1: FloatArray, emb2: FloatArray): Float {
        var distance = 0f
        for (i in emb1.indices) {
            val diff = emb1[i] - emb2[i]
            distance += diff * diff
        }
        return sqrt(distance.toDouble()).toFloat()
    }

    fun close() {
        interpreter.close()
    }
}
