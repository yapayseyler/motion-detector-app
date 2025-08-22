package com.minimax.motiondetector.motion

interface MotionDetectorCallback {
    fun onMotionDetected()
    fun onError(error: String)
}