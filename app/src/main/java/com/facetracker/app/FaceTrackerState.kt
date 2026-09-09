package com.facetracker.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Shared state holding live detection statistics, blink/smile counts,
 * and current face probabilities.
 */
object FaceTrackerState {

    private val _blinkCount = MutableLiveData(0)
    val blinkCount: LiveData<Int> = _blinkCount

    private val _smileCount = MutableLiveData(0)
    val smileCount: LiveData<Int> = _smileCount

    private val _leftEyeOpenProb = MutableLiveData(0f)
    val leftEyeOpenProb: LiveData<Float> = _leftEyeOpenProb

    private val _rightEyeOpenProb = MutableLiveData(0f)
    val rightEyeOpenProb: LiveData<Float> = _rightEyeOpenProb

    private val _smileProb = MutableLiveData(0f)
    val smileProb: LiveData<Float> = _smileProb

    private val _isFaceDetected = MutableLiveData(false)
    val isFaceDetected: LiveData<Boolean> = _isFaceDetected

    private val _isServiceRunning = MutableLiveData(false)
    val isServiceRunning: LiveData<Boolean> = _isServiceRunning

    fun incrementBlink() {
        _blinkCount.postValue((_blinkCount.value ?: 0) + 1)
    }

    fun incrementSmile() {
        _smileCount.postValue((_smileCount.value ?: 0) + 1)
    }

    fun updateProbabilities(
        leftEye: Float,
        rightEye: Float,
        smile: Float,
        detected: Boolean
    ) {
        _leftEyeOpenProb.postValue(leftEye)
        _rightEyeOpenProb.postValue(rightEye)
        _smileProb.postValue(smile)
        _isFaceDetected.postValue(detected)
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.postValue(running)
    }

    fun resetCounters() {
        _blinkCount.postValue(0)
        _smileCount.postValue(0)
    }
}
