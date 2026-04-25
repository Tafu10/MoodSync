package com.example.moodsync.ml

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

import com.google.mlkit.vision.face.Face

class FaceAnalyzer(
    private val onFaceDetected: (Face?, Bitmap?) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            // Get the full frame bitmap
            val frameBitmap = imageProxy.toBitmap()
            // We need to rotate the bitmap to match the imageProxy rotation
            val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(frameBitmap, 0, 0, frameBitmap.width, frameBitmap.height, matrix, true)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces.first()
                        val rect = face.boundingBox

                        // Bounds check before cropping
                        val left = Math.max(0, rect.left)
                        val top = Math.max(0, rect.top)
                        val width = Math.min(rotatedBitmap.width - left, rect.width())
                        val height = Math.min(rotatedBitmap.height - top, rect.height())

                        var croppedFace: Bitmap? = null
                        if (width > 0 && height > 0) {
                            croppedFace = Bitmap.createBitmap(rotatedBitmap, left, top, width, height)
                        }

                        onFaceDetected(face, croppedFace)
                    } else {
                        onFaceDetected(null, null)
                    }
                }
                .addOnFailureListener {
                    onFaceDetected(null, null)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
