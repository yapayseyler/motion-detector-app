package com.minimax.motiondetector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.minimax.motiondetector.databinding.ActivityMainBinding
import com.minimax.motiondetector.motion.MotionDetector
import com.minimax.motiondetector.motion.MotionDetectorCallback
import com.minimax.motiondetector.services.ScreenCaptureService
import com.minimax.motiondetector.utils.NotificationHelper
import com.minimax.motiondetector.utils.PermissionManager
import com.minimax.motiondetector.utils.PreferencesManager
import com.minimax.motiondetector.utils.SoundManager
import kotlinx.coroutines.launch
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), MotionDetectorCallback {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var motionDetector: MotionDetector
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var soundManager: SoundManager
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var permissionManager: PermissionManager
    
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isDetectionActive = false
    
    companion object {
        private const val TAG = "MainActivity"
        private const val MEDIA_PROJECTION_REQUEST_CODE = 1001
    }
    
    // OpenCV Loader Callback
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.d(TAG, "OpenCV loaded successfully")
                    initializeMotionDetector()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Gerekli izinler verilmedi", Toast.LENGTH_LONG).show()
        }
    }
    
    // Media projection launcher
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = Intent(this, ScreenCaptureService::class.java)
            intent.putExtra("resultCode", result.resultCode)
            intent.putExtra("data", result.data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        initializeComponents()
        checkPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        soundManager.release()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        
        // Sensitivity SeekBar setup
        binding.sensitivitySeekBar.max = 100
        binding.sensitivitySeekBar.progress = 50
        updateSensitivityText(50)
        
        binding.sensitivitySeekBar.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        updateSensitivityText(progress)
                        preferencesManager.setSensitivity(progress)
                        if (::motionDetector.isInitialized) {
                            motionDetector.setSensitivity(progress)
                        }
                    }
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )
        
        // Start/Stop button
        binding.startStopButton.setOnClickListener {
            toggleDetection()
        }
        
        // Screenshot button
        binding.screenshotButton.setOnClickListener {
            requestScreenCapture()
        }
    }
    
    private fun initializeComponents() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        notificationHelper = NotificationHelper(this)
        soundManager = SoundManager(this)
        preferencesManager = PreferencesManager(this)
        permissionManager = PermissionManager(this)
        
        // Load saved sensitivity
        val savedSensitivity = preferencesManager.getSensitivity()
        binding.sensitivitySeekBar.progress = savedSensitivity
        updateSensitivityText(savedSensitivity)
    }
    
    private fun initializeMotionDetector() {
        motionDetector = MotionDetector(this)
        motionDetector.setSensitivity(preferencesManager.getSensitivity())
    }
    
    private fun checkPermissions() {
        val requiredPermissions = permissionManager.getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            startCamera()
        }
    }
    
    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            // Preview use case
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            
            // Image analysis use case for motion detection
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (isDetectionActive && ::motionDetector.isInitialized) {
                            motionDetector.processFrame(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }
            
            // Camera selector
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                Log.d(TAG, "Camera started successfully")
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
                Toast.makeText(this, "Kamera başlatılamadı: ${exc.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun toggleDetection() {
        isDetectionActive = !isDetectionActive
        
        if (isDetectionActive) {
            binding.startStopButton.text = "Durdur"
            binding.statusText.text = "Hareket algılama aktif"
            binding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            
            // Request screen capture permission if needed
            if (preferencesManager.isScreenCaptureEnabled()) {
                requestScreenCapture()
            }
        } else {
            binding.startStopButton.text = "Başlat"
            binding.statusText.text = "Hareket algılama durduruldu"
            binding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            
            // Stop screen capture service
            stopService(Intent(this, ScreenCaptureService::class.java))
        }
    }
    
    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }
    
    private fun updateSensitivityText(sensitivity: Int) {
        binding.sensitivityText.text = "Hassasiyet: $sensitivity%"
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_history -> {
                startActivity(Intent(this, MotionHistoryActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    // MotionDetectorCallback implementation
    override fun onMotionDetected() {
        runOnUiThread {
            binding.lastDetectionText.text = "Son hareket: ${java.text.SimpleDateFormat(
                "HH:mm:ss", java.util.Locale.getDefault()
            ).format(java.util.Date())}"
            
            // Play sound alert
            if (preferencesManager.isSoundEnabled()) {
                soundManager.playAlertSound()
            }
            
            // Show notification
            if (preferencesManager.isNotificationEnabled()) {
                notificationHelper.showMotionDetectedNotification()
            }
            
            // Trigger screen capture if enabled
            if (preferencesManager.isScreenCaptureEnabled()) {
                val intent = Intent(this@MainActivity, ScreenCaptureService::class.java)
                intent.action = "CAPTURE_SCREEN"
                startService(intent)
            }
        }
    }
    
    override fun onError(error: String) {
        runOnUiThread {
            Log.e(TAG, "Motion detection error: $error")
            Toast.makeText(this, "Hata: $error", Toast.LENGTH_LONG).show()
        }
    }
}