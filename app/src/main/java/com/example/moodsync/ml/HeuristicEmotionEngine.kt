package com.example.moodsync.ml

import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark

data class FaceBaseline(
    val neutralEyebrowRaise: Float,
    val neutralEyebrowDistanceRatio: Float,
    val neutralLipCurveRatio: Float
)

class HeuristicEmotionEngine {

    var baseline: FaceBaseline? = null

    fun calibrate(face: Face) {
        val faceHeight = face.boundingBox.height().toFloat()
        val faceWidth = face.boundingBox.width().toFloat()
        if (faceHeight <= 0 || faceWidth <= 0) return

        var eyebrowRaise = 0f
        var eyebrowDistanceRatio = 0f
        var lipCurveRatio = 0f

        val leftEyebrow = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points
        val rightEyebrow = face.getContour(FaceContour.RIGHT_EYEBROW_TOP)?.points
        val leftEye = face.getContour(FaceContour.LEFT_EYE)?.points

        if (leftEyebrow != null && rightEyebrow != null && leftEye != null && leftEyebrow.isNotEmpty() && rightEyebrow.isNotEmpty() && leftEye.isNotEmpty()) {
            val eyebrowY = leftEyebrow.map { it.y }.average().toFloat()
            val eyeY = leftEye.map { it.y }.average().toFloat()
            eyebrowRaise = (eyeY - eyebrowY) / faceHeight

            var minEyebrowDistance = Float.MAX_VALUE
            for (p1 in leftEyebrow) {
                for (p2 in rightEyebrow) {
                    val dist = Math.abs(p1.x - p2.x)
                    if (dist < minEyebrowDistance) {
                        minEyebrowDistance = dist
                    }
                }
            }
            eyebrowDistanceRatio = minEyebrowDistance / faceWidth
        }

        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
        if (mouthLeft != null && mouthRight != null && mouthBottom != null) {
            val avgCornerY = (mouthLeft.y + mouthRight.y) / 2f
            val lipCurve = mouthBottom.y - avgCornerY
            lipCurveRatio = lipCurve / faceHeight
        }

        baseline = FaceBaseline(eyebrowRaise, eyebrowDistanceRatio, lipCurveRatio)
    }

    fun analyzeEmotion(face: Face): String {
        // 1. Happy (Smiling uses native ML Kit probability)
        val smileProb = face.smilingProbability ?: 0f
        if (smileProb > 0.5f) {
            return "Happy"
        }

        val faceHeight = face.boundingBox.height().toFloat()
        if (faceHeight <= 0) return "Neutral"

        // 2. Sad (Based on Mouth Corner Landmarks - Check this FIRST before Angry)
        // We use exact landmarks instead of contours for maximum accuracy on the corners!
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position

        if (mouthLeft != null && mouthRight != null && mouthBottom != null) {
            val avgCornerY = (mouthLeft.y + mouthRight.y) / 2f
            
            // The distance between the bottom of the lip and the corners of the mouth.
            // Normally, corners are much higher up (so lipCurve is a large positive number).
            // When sad (upside down smile), corners pull down, making this distance much smaller.
            val lipCurve = mouthBottom.y - avgCornerY
            
            val lipCurveRatio = lipCurve / faceHeight
            
            val isSad = if (baseline != null) {
                // If the lip curve flattens out or drops by 1.2% compared to neutral
                val curveDrop = baseline!!.neutralLipCurveRatio - lipCurveRatio
                curveDrop > 0.012f && smileProb < 0.3f
            } else {
                lipCurve < (faceHeight * 0.065f) && smileProb < 0.3f
            }

            if (isSad) {
                return "Sad"
            }
        }

        // 3. Angry & Surprise (Based on Eyebrow Height and Distance)
        val leftEyebrow = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points
        val rightEyebrow = face.getContour(FaceContour.RIGHT_EYEBROW_TOP)?.points
        val leftEye = face.getContour(FaceContour.LEFT_EYE)?.points
        
        if (leftEyebrow != null && rightEyebrow != null && leftEye != null && leftEyebrow.isNotEmpty() && rightEyebrow.isNotEmpty() && leftEye.isNotEmpty()) {
            val eyebrowY = leftEyebrow.map { it.y }.average().toFloat()
            val eyeY = leftEye.map { it.y }.average().toFloat()
            
            // Distance from eye to eyebrow relative to face height
            val eyebrowRaise = (eyeY - eyebrowY) / faceHeight
            
            // Calculate distance between the two eyebrows (frown intensity)
            val faceWidth = face.boundingBox.width().toFloat()
            var minEyebrowDistance = Float.MAX_VALUE
            for (p1 in leftEyebrow) {
                for (p2 in rightEyebrow) {
                    val dist = Math.abs(p1.x - p2.x)
                    if (dist < minEyebrowDistance) {
                        minEyebrowDistance = dist
                    }
                }
            }
            val eyebrowDistanceRatio = minEyebrowDistance / faceWidth

            // Delta checking if baseline exists, otherwise absolute thresholds
            val isAngry = if (baseline != null) {
                // If eyebrow distance drops horizontally OR drops vertically
                // (Glasses frames can sometimes freeze horizontal tracking, so we check both!)
                val distanceDrop = baseline!!.neutralEyebrowDistanceRatio - eyebrowDistanceRatio
                val heightDrop = baseline!!.neutralEyebrowRaise - eyebrowRaise
                
                // Absolute middle ground for anger thresholds (tiny bit more sensitive)
                (distanceDrop > 0.0046f || heightDrop > 0.0105f) && smileProb < 0.45f
            } else {
                eyebrowDistanceRatio < 0.25f && smileProb < 0.45f
            }

            if (isAngry) {
                return "Angry"
            }

            // Surprise check (Eyebrows high + Mouth open)
            val upperLip = face.getContour(FaceContour.UPPER_LIP_TOP)?.points
            val lowerLip = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points
            if (upperLip != null && lowerLip != null && upperLip.isNotEmpty() && lowerLip.isNotEmpty()) {
                val upperLipY = upperLip.map { it.y }.average().toFloat()
                val lowerLipY = lowerLip.map { it.y }.average().toFloat()
                val mouthOpenness = (lowerLipY - upperLipY) / faceHeight
                if (mouthOpenness > 0.08f && eyebrowRaise > 0.115f) {
                    return "Surprise"
                }
            }
        }

        return "Neutral"
    }
}
