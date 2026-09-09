package com.facetracker.app

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceMetricsAnalyzer(
    private val onEventDetected: ((type: String) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    // Configure ML Kit Face Detector with classification enabled for eyes and smile
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
    )

    // State machines for blink detection
    private var isEyesClosed = false
    private var lastBlinkTime = 0L
    private val blinkClosedThreshold = 0.25f
    private val blinkOpenThreshold = 0.70f
    private val blinkDebounceMs = 120L

    // State machines for smile detection
    private var isSmiling = false
    private var lastSmileTime = 0L
    private val smileThreshold = 0.65f
    private val smileResetThreshold = 0.30f
    private val smileDebounceMs = 500L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    processFace(faces[0])
                } else {
                    FaceTrackerState.updateProbabilities(0f, 0f, 0f, false)
                }
            }
            .addOnFailureListener {
                FaceTrackerState.updateProbabilities(0f, 0f, 0f, false)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processFace(face: Face) {
        val leftEye = face.leftEyeOpenProbability ?: -1f
        val rightEye = face.rightEyeOpenProbability ?: -1f
        val smile = face.smilingProbability ?: -1f
        val currentTime = System.currentTimeMillis()

        FaceTrackerState.updateProbabilities(
            if (leftEye >= 0) leftEye else 0f,
            if (rightEye >= 0) rightEye else 0f,
            if (smile >= 0) smile else 0f,
            true
        )

        // 1. Blink Detection Logic
        if (leftEye >= 0 && rightEye >= 0) {
            val avgEyeOpen = (leftEye + rightEye) / 2f
            if (avgEyeOpen < blinkClosedThreshold) {
                isEyesClosed = true
            } else if (avgEyeOpen > blinkOpenThreshold && isEyesClosed) {
                if (currentTime - lastBlinkTime > blinkDebounceMs) {
                    FaceTrackerState.incrementBlink()
                    lastBlinkTime = currentTime
                    onEventDetected?.invoke("BLINK")
                }
                isEyesClosed = false
            }
        }

        // 2. Smile Detection Logic
        if (smile >= 0) {
            if (smile > smileThreshold) {
                if (!isSmiling && (currentTime - lastSmileTime > smileDebounceMs)) {
                    FaceTrackerState.incrementSmile()
                    lastSmileTime = currentTime
                    isSmiling = true
                    onEventDetected?.invoke("SMILE")
                }
            } else if (smile < smileResetThreshold) {
                isSmiling = false
            }
        }
    }
}
