package com.minimax.motiondetector.services

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.minimax.motiondetector.utils.NotificationHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenCaptureService : Service() {
    
    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val VIRTUAL_DISPLAY_NAME = "MotionDetectorCapture"
        private const val FOREGROUND_SERVICE_ID = 2001
    }
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var notificationHelper: NotificationHelper
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    
    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        getScreenMetrics()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = notificationHelper.createForegroundNotification()
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "CAPTURE_SCREEN" -> {
                captureScreen()
            }
            else -> {
                val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
                val data = intent?.getParcelableExtra<Intent>("data")
                
                if (resultCode != -1 && data != null) {
                    setupMediaProjection(resultCode, data)
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun getScreenMetrics() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = windowManager.defaultDisplay
            display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        }
        
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        screenDensity = displayMetrics.densityDpi
        
        Log.d(TAG, "Screen metrics: ${screenWidth}x$screenHeight, density: $screenDensity")
    }
    
    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        try {
            val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped")
                    stopVirtualDisplay()
                }
            }, Handler(Looper.getMainLooper()))
            
            setupVirtualDisplay()
            Log.d(TAG, "MediaProjection setup complete")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up MediaProjection", e)
            stopSelf()
        }
    }
    
    private fun setupVirtualDisplay() {
        try {
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            
            Log.d(TAG, "VirtualDisplay created")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up VirtualDisplay", e)
        }
    }
    
    private fun captureScreen() {
        if (imageReader == null || virtualDisplay == null) {
            Log.w(TAG, "Screen capture not ready")
            return
        }
        
        try {
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                saveImageToFile(image)
                image.close()
            } else {
                Log.w(TAG, "No image available for capture")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen", e)
        }
    }
    
    private fun saveImageToFile(image: Image) {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Dosya adı oluştur
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "motion_$timeStamp.png"
            
            // Dosya kaydetme dizini
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val motionCaptureDir = File(picturesDir, "MotionCapture")
            if (!motionCaptureDir.exists()) {
                motionCaptureDir.mkdirs()
            }
            
            val imageFile = File(motionCaptureDir, fileName)
            
            // Dosyayı kaydet
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Log.d(TAG, "Screenshot saved: ${imageFile.absolutePath}")
            
            // Bildirim göster
            notificationHelper.showScreenshotNotification(imageFile.name)
            
            bitmap.recycle()
            
        } catch (e: IOException) {
            Log.e(TAG, "Error saving screenshot", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving screenshot", e)
        }
    }
    
    private fun stopVirtualDisplay() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVirtualDisplay()
        mediaProjection?.stop()
        mediaProjection = null
        Log.d(TAG, "ScreenCaptureService destroyed")
    }
}