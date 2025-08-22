package com.minimax.motiondetector.services

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.minimax.motiondetector.motion.MotionDetector
import com.minimax.motiondetector.motion.MotionDetectorCallback
import com.minimax.motiondetector.utils.NotificationHelper
import com.minimax.motiondetector.utils.PreferencesManager
import com.minimax.motiondetector.utils.SoundManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MotionDetectionService : LifecycleService(), MotionDetectorCallback {
    
    companion object {
        private const val TAG = "MotionDetectionService"
        private const val FOREGROUND_SERVICE_ID = 1003
    }
    
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var motionDetector: MotionDetector
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var soundManager: SoundManager
    private lateinit var preferencesManager: PreferencesManager
    
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    override fun onCreate() {
        super.onCreate()
        initializeComponents()
        startForegroundService()
        startCamera()
    }
    
    private fun initializeComponents() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        motionDetector = MotionDetector(this)
        notificationHelper = NotificationHelper(this)
        soundManager = SoundManager(this)
        preferencesManager = PreferencesManager(this)
        
        motionDetector.setSensitivity(preferencesManager.getSensitivity())
    }
    
    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = notificationHelper.createForegroundNotification()
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Image analysis use case for motion detection
                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            motionDetector.processFrame(imageProxy)
                        }
                    }
                
                // Camera selector
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, imageAnalyzer
                )
                
                Log.d(TAG, "Background camera started successfully")
                
            } catch (exc: Exception) {
                Log.e(TAG, "Background camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            "STOP_DETECTION" -> {
                stopSelf()
            }
            "UPDATE_SENSITIVITY" -> {
                val sensitivity = intent.getIntExtra("sensitivity", 50)
                motionDetector.setSensitivity(sensitivity)
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        motionDetector.release()
        soundManager.release()
        Log.d(TAG, "MotionDetectionService destroyed")
    }
    
    // MotionDetectorCallback implementation
    override fun onMotionDetected() {
        Log.d(TAG, "Motion detected in background service")
        
        // Ses çalma
        if (preferencesManager.isSoundEnabled()) {
            soundManager.playAlertSound()
        }
        
        // Bildirim gösterme
        if (preferencesManager.isNotificationEnabled()) {
            notificationHelper.showMotionDetectedNotification()
        }
        
        // Ekran görüntüsü alma
        if (preferencesManager.isScreenCaptureEnabled()) {
            val intent = Intent(this, ScreenCaptureService::class.java)
            intent.action = "CAPTURE_SCREEN"
            startService(intent)
        }
    }
    
    override fun onError(error: String) {
        Log.e(TAG, "Motion detection error in background: $error")
    }
}