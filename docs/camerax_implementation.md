# Android CameraX API Implementation Guide

## İçindekiler
1. [CameraX Genel Bakış](#camerax-genel-bakış)
2. [Kurulum ve Yapılandırma](#kurulum-ve-yapılandırma)
3. [Use Case'ler](#use-caseler)
   - [Preview (Önizleme)](#preview-önizleme)
   - [ImageCapture (Fotoğraf Çekme)](#imagecapture-fotoğraf-çekme)
   - [ImageAnalysis (Görüntü Analizi)](#imageanalysis-görüntü-analizi)
   - [VideoCapture (Video Çekme)](#videocapture-video-çekme)
4. [Lifecycle Yönetimi](#lifecycle-yönetimi)
5. [OpenCV Entegrasyonu](#opencv-entegrasyonu)
6. [CameraX 1.4.0 Yeni Özellikler](#camerax-140-yeni-özellikler)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

## CameraX Genel Bakış

CameraX, Android Jetpack kütüphanesinin bir parçası olup kamera uygulaması geliştirmeyi kolaylaştırmak amacıyla oluşturulmuştur[1]. Android 5.0 (API seviye 21) ile geriye dönük uyumluluk sunar ve mevcut Android cihazlarının %98'inden fazlasında çalışır[1].

### Temel Faydaları
- **Geniş Cihaz Uyumluluğu**: Android 5.0 ve üzeri cihazları destekler
- **Kullanım Kolaylığı**: Cihaza özgü nüanslar yerine temel kullanım senaryolarına odaklanma
- **Cihazlar Arası Tutarlılık**: En boy oranı, yönelim, önizleme boyutu gibi temel davranışlarda tutarlılık
- **Kamera Eklentileri**: Bokeh, HDR, gece modu gibi gelişmiş özelliklere erişim[1]

### Mimari Yapı

CameraX, use case (kullanım durumu) tabanlı bir API sunar. Başlıca use case'ler:
- **Preview**: Kamera önizlemesi gösterme
- **ImageCapture**: Fotoğraf çekme ve kaydetme
- **ImageAnalysis**: Real-time görüntü analizi
- **VideoCapture**: Video kaydetme[2]

## Kurulum ve Yapılandırma

### Gradle Bağımlılıkları

```kotlin
val camerax_version = "1.4.0"

dependencies {
    // CameraX core library
    implementation "androidx.camera:camera-core:${camerax_version}"
    
    // CameraX Camera2 extensions
    implementation "androidx.camera:camera-camera2:${camerax_version}"
    
    // CameraX Lifecycle Library
    implementation "androidx.camera:camera-lifecycle:${camerax_version}"
    
    // CameraX Video Library
    implementation "androidx.camera:camera-video:${camerax_version}"
    
    // CameraX View class
    implementation "androidx.camera:camera-view:${camerax_version}"
    
    // CameraX Extensions library (optional)
    implementation "androidx.camera:camera-extensions:${camerax_version}"
}
```

### İzinler

AndroidManifest.xml dosyasına gerekli izinleri ekleyin:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

### Java 8 Uyumluluğu

```kotlin
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

### ViewBinding Etkinleştirme

```kotlin
android {
    buildFeatures {
        viewBinding true
    }
}
```

## Use Case'ler

### Preview (Önizleme)

Preview use case, kamera görünümünü kullanıcıya göstermek için kullanılır[3].

#### Temel Kurulum

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageAnalyzer: ImageAnalysis? = null
    
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // İzinleri kontrol et
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // CameraProvider'ı al
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview'ı oluştur
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Arka kamerayı seç
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Mevcut bağlantıları kaldır
                cameraProvider.unbindAll()

                // Use case'leri lifecycle'a bağla
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
}
```

#### Gelişmiş Preview Yapılandırması

```kotlin
private fun createAdvancedPreview(): Preview {
    return Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .setTargetRotation(Surface.ROTATION_0)
        .build()
        .also { preview ->
            preview.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
        }
}
```

### ImageCapture (Fotoğraf Çekme)

ImageCapture use case, fotoğraf çekme ve depolamaya kaydetme işlemlerini yönetir[3].

#### Temel Implementasyon

```kotlin
private fun setupImageCapture() {
    imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
        .build()
}

private fun takePhoto() {
    val imageCapture = imageCapture ?: return

    // Benzersiz dosya adı oluştur
    val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
        .format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }

    // Output seçeneklerini oluştur
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
        .build()

    // Fotoğrafı çek
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults){
                val msg = "Photo capture succeeded: ${output.savedUri}"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
            }
        }
    )
}
```

#### Bellek İçi Yakalama

```kotlin
private fun captureToMemory() {
    val imageCapture = imageCapture ?: return
    
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                // ImageProxy'yi işle
                processImageProxy(image)
                
                // İşlem tamamlandıktan sonra kapat
                image.close()
            }
            
            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}
```

### ImageAnalysis (Görüntü Analizi)

ImageAnalysis use case, kamera karelerinden gerçek zamanlı görüntü analizi yapmak için kullanılır[5].

#### Analyzer Sınıfı Oluşturma

```kotlin
private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // buffer'ı başa al
        val data = ByteArray(remaining())
        get(data)   // buffer'ı byte array'e kopyala
        return data // byte array'i döndür
    }

    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()

        listener(luma)

        image.close()
    }
}
```

#### ImageAnalysis Kurulumu

```kotlin
private fun setupImageAnalysis() {
    imageAnalyzer = ImageAnalysis.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setImageQueueDepth(1)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .build()
        .also {
            it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                Log.d(TAG, "Average luminosity: $luma")
            })
        }
}
```

#### Geri Basınç Stratejileri

```kotlin
// En son frame'i tut - varsayılan
imageAnalysis.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

// Blocking mod - yüksek performans için
imageAnalysis.setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
```

### VideoCapture (Video Çekme)

VideoCapture use case, video ve ses kaydı yapmak için kullanılır[3].

#### Video Capture Kurulumu

```kotlin
private fun setupVideoCapture() {
    val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
        .build()
    videoCapture = VideoCapture.withOutput(recorder)
}

private fun captureVideo() {
    val videoCapture = this.videoCapture ?: return

    viewBinding.videoCaptureButton.isEnabled = false

    val curRecording = recording
    if (curRecording != null) {
        // Mevcut kaydı durdur
        curRecording.stop()
        recording = null
        return
    }

    // Yeni kayıt oluştur
    val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
        .format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
        }
    }

    val mediaStoreOutputOptions = MediaStoreOutputOptions
        .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        .setContentValues(contentValues)
        .build()
    
    recording = videoCapture.output
        .prepareRecording(this, mediaStoreOutputOptions)
        .apply {
            if (PermissionChecker.checkSelfPermission(this@MainActivity,
                    Manifest.permission.RECORD_AUDIO) ==
                PermissionChecker.PERMISSION_GRANTED)
            {
                withAudioEnabled()
            }
        }
        .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            when(recordEvent) {
                is VideoRecordEvent.Start -> {
                    viewBinding.videoCaptureButton.apply {
                        text = getString(R.string.stop_capture)
                        isEnabled = true
                    }
                }
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val msg = "Video capture succeeded: " +
                            "${recordEvent.outputResults.outputUri}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                            .show()
                        Log.d(TAG, msg)
                    } else {
                        recording?.close()
                        recording = null
                        Log.e(TAG, "Video capture ends with error: " +
                            "${recordEvent.error}")
                    }
                    viewBinding.videoCaptureButton.apply {
                        text = getString(R.string.start_capture)
                        isEnabled = true
                    }
                }
            }
        }
}
```

## Lifecycle Yönetimi

CameraX, Android yaşam döngüsüyle entegre olarak kamera kaynaklarını otomatik olarak yönetir[7].

### Temel Lifecycle Bağlama

```kotlin
private fun bindCameraUseCases() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalyzer = ImageAnalysis.Builder().build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Mevcut bağlantıları kaldır
            cameraProvider.unbindAll()

            // Use case'leri lifecycle'a bağla
            val camera = cameraProvider.bindToLifecycle(
                this, // LifecycleOwner
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )

            // Kamera kontrollerine erişim
            val cameraControl = camera.cameraControl
            val cameraInfo = camera.cameraInfo

        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }, ContextCompat.getMainExecutor(this))
}
```

### Özel Lifecycle Owner

```kotlin
class CustomLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    fun startCamera() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun stopCamera() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
```

### Resource Management

```kotlin
override fun onDestroy() {
    super.onDestroy()
    cameraExecutor.shutdown()
}

private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(
        baseContext, it) == PackageManager.PERMISSION_GRANTED
}

override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_CODE_PERMISSIONS) {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this,
                "Permissions not granted by the user.",
                Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

companion object {
    private const val TAG = "CameraXApp"
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private const val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS =
        mutableListOf (
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
}
```

## OpenCV Entegrasyonu

CameraX ve OpenCV'nin entegrasyonu, gerçek zamanlı görüntü işleme uygulamaları için güçlü bir kombinasyon sağlar[4].

### OpenCV Kurulumu

#### build.gradle (Module: app)

```kotlin
android {
    packagingOptions {
        pickFirst "**/libc++_shared.so"
        pickFirst "**/libopenmp.so"
    }
}

dependencies {
    implementation project(':opencv')
}
```

#### OpenCV Yükleme

```kotlin
class MainActivity : AppCompatActivity() {
    
    private val openCVLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.d(TAG, "OpenCV loaded successfully")
                    // OpenCV hazır, kamera başlatılabilir
                    startCamera()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, openCVLoaderCallback)
        } else {
            openCVLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
}
```

### ImageProxy'den OpenCV Mat'e Dönüşüm

```kotlin
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

// Extension function: ImageProxy'yi OpenCV Mat'e dönüştür
fun ImageProxy.yuvToRgba(): Mat {
    val yBuffer = planes[0].buffer // Y
    val vuBuffer = planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
    yuvMat.put(0, 0, nv21)

    val rgbMat = Mat()
    Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGBA_NV21)

    return rgbMat
}

// YUV420_888'den RGBA'ya gelişmiş dönüşüm
fun ImageProxy.toOpenCvRgba(): Mat {
    val planes = planes
    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // Y kanalını kopyala
    yBuffer.get(nv21, 0, ySize)
    
    val uvPixelStride = uPlane.pixelStride
    if (uvPixelStride == 1) {
        // U ve V ayrık
        uBuffer.get(nv21, ySize, uSize)
        vBuffer.get(nv21, ySize + uSize, vSize)
    } else {
        // UV interleaved
        val uvBuffer = if (uBuffer.hasArray()) uBuffer.array() else {
            val uvArray = ByteArray(uSize)
            uBuffer.get(uvArray)
            uvArray
        }
        
        // UV'yi NV21 formatına dönüştür
        for (i in 0 until uSize step uvPixelStride) {
            nv21[ySize + i / uvPixelStride] = uvBuffer[i]
            nv21[ySize + i / uvPixelStride + vSize] = vBuffer.get(i)
        }
    }

    val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
    yuvMat.put(0, 0, nv21)

    val rgbMat = Mat()
    Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21)

    return rgbMat
}
```

### OpenCV Analyzer Implementation

```kotlin
class OpenCvAnalyzer(private val listener: OpenCvListener) : ImageAnalysis.Analyzer {
    
    interface OpenCvListener {
        fun onImageProcessed(processedMat: Mat)
        fun onError(error: String)
    }

    override fun analyze(image: ImageProxy) {
        try {
            // ImageProxy'yi OpenCV Mat'e dönüştür
            val rgbMat = image.toOpenCvRgba()
            
            // OpenCV işlemlerini uygula
            val processedMat = processFrame(rgbMat)
            
            // Ana thread'de UI güncelle
            Handler(Looper.getMainLooper()).post {
                listener.onImageProcessed(processedMat)
            }
            
            // Kaynakları temizle
            rgbMat.release()
            
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                listener.onError("OpenCV işleme hatası: ${e.message}")
            }
        } finally {
            image.close()
        }
    }
    
    private fun processFrame(inputMat: Mat): Mat {
        val grayMat = Mat()
        val edgesMat = Mat()
        
        // Gri tonlamaya dönüştür
        Imgproc.cvtColor(inputMat, grayMat, Imgproc.COLOR_RGB2GRAY)
        
        // Kenar tespiti uygula
        Imgproc.Canny(grayMat, edgesMat, 100.0, 200.0)
        
        // Gri tonlamalı sonucu RGB'ye dönüştür
        val resultMat = Mat()
        Imgproc.cvtColor(edgesMat, resultMat, Imgproc.COLOR_GRAY2RGB)
        
        // Geçici Mat'leri temizle
        grayMat.release()
        edgesMat.release()
        
        return resultMat
    }
}
```

### OpenCV ile ImageAnalysis Kullanımı

```kotlin
class MainActivity : AppCompatActivity(), OpenCvAnalyzer.OpenCvListener {
    
    private fun setupOpenCvAnalysis() {
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, OpenCvAnalyzer(this))
            }
    }
    
    override fun onImageProcessed(processedMat: Mat) {
        // İşlenmiş görüntüyü ImageView'da göster
        val bitmap = Bitmap.createBitmap(
            processedMat.cols(), 
            processedMat.rows(), 
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(processedMat, bitmap)
        
        viewBinding.processedImageView.setImageBitmap(bitmap)
        
        // Mat'i temizle
        processedMat.release()
    }
    
    override fun onError(error: String) {
        Log.e(TAG, "OpenCV Error: $error")
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }
}
```

### Yön Düzeltme (Rotation Correction)

```kotlin
fun Mat.correctOrientation(rotationDegrees: Int): Mat {
    val rotatedMat = Mat()
    when (rotationDegrees) {
        90 -> Core.transpose(this, rotatedMat).also { 
            Core.flip(rotatedMat, rotatedMat, 1) 
        }
        180 -> Core.flip(this, rotatedMat, -1)
        270 -> Core.transpose(this, rotatedMat).also { 
            Core.flip(rotatedMat, rotatedMat, 0) 
        }
        else -> this.copyTo(rotatedMat)
    }
    return rotatedMat
}

// Kullanım
private fun processFrameWithRotation(inputMat: Mat, imageProxy: ImageProxy): Mat {
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val correctedMat = inputMat.correctOrientation(rotationDegrees)
    
    // İşleme devam et...
    val processedMat = processFrame(correctedMat)
    
    correctedMat.release()
    return processedMat
}
```

## CameraX 1.4.0 Yeni Özellikler

CameraX 1.4.0, Android kamera uygulamalarını geliştirmek için bir dizi yeni özellik sunar[4].

### HDR Genişletmeleri

#### HDR Önizleme

```kotlin
private fun setupHdrPreview(): Preview {
    val openGLPipelineSupportedDynamicRange = setOf(
        DynamicRange.SDR, 
        DynamicRange.HLG_10_BIT
    )
    
    val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)
    val isHlg10Supported = cameraInfo
        .querySupportedDynamicRanges(openGLPipelineSupportedDynamicRange)
        .contains(DynamicRange.HLG_10_BIT)
    
    return Preview.Builder().apply {
        if (isHlg10Supported) {
            setDynamicRange(DynamicRange.HLG_10_BIT)
        }
    }.build()
}
```

#### Ultra HDR

```kotlin
private fun setupUltraHdr(): ImageCapture {
    val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)
    val isUltraHdrSupported = ImageCapture
        .getImageCaptureCapabilities(cameraInfo)
        .supportedOutputFormats
        .contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
    
    return ImageCapture.Builder().apply {
        if (isUltraHdrSupported) {
            setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        }
    }.build()
}
```

### Önizleme Stabilizasyonu

```kotlin
private fun setupPreviewStabilization(): Preview {
    val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)
    val isPreviewStabilizationSupported = Preview
        .getPreviewCapabilities(cameraInfo)
        .isStabilizationSupported
    
    return Preview.Builder().apply {
        if (isPreviewStabilizationSupported) {
            setPreviewStabilizationEnabled(true)
        }
    }.build()
}
```

### Kamera Efektleri (Effects)

#### Overlay Effect

```kotlin
// Gradle'da ek bağımlılık
implementation "androidx.camera:camera-effects:1.4.0"
implementation "androidx.camera:camera-mlkit-vision:1.4.0"

// Overlay efekti kullanımı
private fun setupOverlayEffect() {
    val overlayEffect = OverlayEffect(
        CameraEffect.PREVIEW,
        Executors.newSingleThreadExecutor(),
        Handler(Looper.getMainLooper())
    ) {
        // Overlay tamamlandığında callback
    }
    
    overlayEffect.setOnDrawListener { frame ->
        val canvas = frame.overlayCanvas
        canvas.setMatrix(frame.sensorToBufferTransform)
        
        // Canvas üzerine çizim
        val paint = Paint().apply {
            color = Color.RED
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        
        canvas.drawRect(100f, 100f, 300f, 300f, paint)
    }
    
    cameraController.setEffects(setOf(overlayEffect))
}
```

#### Media3 Effects

```kotlin
// Gradle'da ek bağımlılık
implementation "androidx.camera.media3:media3-effect:1.0.0-alpha01"
implementation "androidx.media3:media3-effect:1.5.0"

private fun setupMedia3Effects() {
    val media3Effect = Media3Effect(
        this, // Context
        CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
        ContextCompat.getMainExecutor(this)
    ) {
        // Effect tamamlandığında callback
    }
    
    // Gri tonlama filtresi
    media3Effect.setEffects(listOf(
        RgbFilter.createGrayscaleFilter()
    ))
    
    cameraController.setEffects(setOf(media3Effect))
}
```

### Ekran Flaşı (Screen Flash)

```kotlin
private fun setupScreenFlash() {
    // PreviewView ile
    viewBinding.previewView.setScreenFlashWindow(window)
    
    imageCapture?.screenFlash = viewBinding.previewView.screenFlash
    imageCapture?.setFlashMode(ImageCapture.FLASH_MODE_SCREEN)
    
    // Veya CameraController ile
    cameraController.setImageCaptureFlashMode(ImageCapture.FLASH_MODE_SCREEN)
}
```

### Kotlin Suspend Functions

```kotlin
// Kotlin coroutines ile kullanım
private suspend fun initializeCameraWithCoroutines() {
    try {
        // ProcessCameraProvider'ı await ile al
        val cameraProvider = ProcessCameraProvider.awaitInstance(this)
        
        // Use case'leri oluştur
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        
        // Lifecycle'a bağla
        val camera = cameraProvider.bindToLifecycle(
            this,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
        
        // Suspend function ile fotoğraf çek
        val imageProxy = imageCapture.takePicture()
        processImageProxy(imageProxy)
        
    } catch (exception: Exception) {
        Log.e(TAG, "Camera initialization failed", exception)
    }
}

// Activity'de kullanım
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            initializeCameraWithCoroutines()
        }
    }
}
```

## Best Practices

### 1. Use Case Kombinasyonları

```kotlin
// ✅ Önerilen: Tüm use case'leri tek çağrıda bağla
private fun bindAllUseCases() {
    val cameraProvider = ProcessCameraProvider.getInstance(this).get()
    
    val preview = Preview.Builder().build()
    val imageCapture = ImageCapture.Builder().build()
    val imageAnalyzer = ImageAnalysis.Builder().build()
    
    try {
        cameraProvider.unbindAll()
        
        // Tek çağrıda tüm use case'leri bağla
        cameraProvider.bindToLifecycle(
            this,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture,
            imageAnalyzer
        )
    } catch (exc: Exception) {
        Log.e(TAG, "Use case binding failed", exc)
    }
}

// ❌ Önerilmeyen: Use case'leri ayrı ayrı bağlama
private fun bindUseCasesSeparately() {
    // Bu yaklaşım performans sorunlarına yol açabilir
    cameraProvider.bindToLifecycle(this, cameraSelector, preview)
    cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
    cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
}
```

### 2. Performans Optimizasyonu

```kotlin
// ImageAnalysis için performans ayarları
private fun createOptimizedImageAnalysis(): ImageAnalysis {
    return ImageAnalysis.Builder()
        .setTargetResolution(Size(640, 480)) // Düşük çözünürlük = yüksek performans
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setImageQueueDepth(1) // Minimum bellek kullanımı
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .build()
}

// Executor yönetimi
class CameraActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Tek thread executor kullan
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
```

### 3. Error Handling

```kotlin
private fun bindToLifecycleWithErrorHandling() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            
            // Use case'leri oluştur
            val preview = createPreview()
            val imageCapture = createImageCapture()
            val imageAnalyzer = createImageAnalysis()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // Mevcut bağlantıları kaldır
            cameraProvider.unbindAll()
            
            // Use case'leri bağla
            val camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            )
            
            setupCameraControls(camera)
            
        } catch (exc: Exception) {
            when (exc) {
                is IllegalArgumentException -> {
                    Log.e(TAG, "Use case combination not supported", exc)
                    handleUnsupportedUseCaseCombination()
                }
                is IllegalStateException -> {
                    Log.e(TAG, "Camera provider not available", exc)
                    handleCameraProviderError()
                }
                else -> {
                    Log.e(TAG, "Unknown camera error", exc)
                    handleGenericCameraError(exc)
                }
            }
        }
    }, ContextCompat.getMainExecutor(this))
}

private fun handleUnsupportedUseCaseCombination() {
    // Use case kombinasyonunu basitleştir
    Toast.makeText(this, 
        "Kamera yapılandırması desteklenmiyor. Basit mod kullanılıyor.", 
        Toast.LENGTH_LONG).show()
    
    // Sadece Preview + ImageCapture kullan
    bindSimplifiedUseCases()
}
```

### 4. Kamera Yetenekleri Kontrolü

```kotlin
private fun checkCameraCapabilities() {
    val cameraProvider = ProcessCameraProvider.getInstance(this).get()
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)
    
    // Flash desteği kontrol et
    val hasFlash = cameraInfo.hasFlashUnit()
    Log.d(TAG, "Flash available: $hasFlash")
    
    // Zoom desteği kontrol et
    val zoomState = cameraInfo.zoomState.value
    val maxZoomRatio = zoomState?.maxZoomRatio ?: 1f
    val minZoomRatio = zoomState?.minZoomRatio ?: 1f
    Log.d(TAG, "Zoom range: $minZoomRatio - $maxZoomRatio")
    
    // HDR desteği kontrol et
    val supportedDynamicRanges = cameraInfo.querySupportedDynamicRanges(
        setOf(DynamicRange.SDR, DynamicRange.HLG_10_BIT)
    )
    Log.d(TAG, "Supported dynamic ranges: $supportedDynamicRanges")
    
    // Preview stabilization desteği
    val previewCapabilities = Preview.getPreviewCapabilities(cameraInfo)
    val isStabilizationSupported = previewCapabilities.isStabilizationSupported
    Log.d(TAG, "Preview stabilization supported: $isStabilizationSupported")
}
```

### 5. Memory Management

```kotlin
class MemoryEfficientAnalyzer : ImageAnalysis.Analyzer {
    private var lastProcessTime = 0L
    private val processingInterval = 100L // 100ms aralıklarla işle
    
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Çok sık işleme yapma
        if (currentTime - lastProcessTime < processingInterval) {
            image.close()
            return
        }
        
        try {
            // Görüntü işleme
            processImage(image)
            lastProcessTime = currentTime
            
        } catch (e: Exception) {
            Log.e(TAG, "Image processing error", e)
        } finally {
            // Her zaman image'ı kapat
            image.close()
        }
    }
    
    private fun processImage(image: ImageProxy) {
        // CPU-intensive işlemler için background thread kullan
        // Ana thread'i bloke etme
    }
}
```

## Troubleshooting

### Sık Karşılaşılan Sorunlar

#### 1. Use Case Kombinasyon Hataları

```kotlin
// Problem: Desteklenmeyen use case kombinasyonu
// Çözüm: Use case sayısını azalt veya çözünürlüğü düşür

private fun handleUseCaseCombinationError() {
    try {
        // İlk deneme: Tüm use case'ler
        cameraProvider.bindToLifecycle(
            this, cameraSelector, preview, imageCapture, imageAnalyzer, videoCapture
        )
    } catch (exc: IllegalArgumentException) {
        Log.w(TAG, "Full combination not supported, trying simplified")
        
        try {
            // İkinci deneme: Video olmadan
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            )
        } catch (exc2: IllegalArgumentException) {
            Log.w(TAG, "Simplified combination not supported, using basic")
            
            // Son deneme: Sadece temel özellikler
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
        }
    }
}
```

#### 2. ImageProxy Kapama Sorunları

```kotlin
// ❌ Yanlış: ImageProxy kapatılmıyor
class BadAnalyzer : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        // İşlemler...
        // image.close() çağrılmıyor - MEMORY LEAK!
    }
}

// ✅ Doğru: Her durumda ImageProxy kapatılıyor
class GoodAnalyzer : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            // İşlemler...
            processImage(image)
        } catch (e: Exception) {
            Log.e(TAG, "Processing error", e)
        } finally {
            image.close() // Her durumda kapat
        }
    }
}
```

#### 3. Thread Yönetimi

```kotlin
// ❌ Yanlış: Ana thread'de uzun işlem
class BadAnalyzer : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        // Ana thread'de ağır işlem yapma!
        val result = heavyImageProcessing(image)
        
        // UI güncellemesi de ana thread'de
        updateUI(result)
        
        image.close()
    }
}

// ✅ Doğru: Background thread'de işlem, main thread'de UI
class GoodAnalyzer : ImageAnalysis.Analyzer {
    private val processingExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun analyze(image: ImageProxy) {
        // Hızlı kopyalama
        val imageData = extractImageData(image)
        val rotation = image.imageInfo.rotationDegrees
        
        image.close() // Hemen kapat
        
        // Ağır işlemi background thread'de yap
        processingExecutor.execute {
            try {
                val result = heavyImageProcessing(imageData, rotation)
                
                // UI güncellemesini main thread'de yap
                mainHandler.post {
                    updateUI(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Processing error", e)
            }
        }
    }
}
```

### Debug İpuçları

#### 1. Logging

```kotlin
class DebugAnalyzer : ImageAnalysis.Analyzer {
    private var frameCount = 0
    private var lastLogTime = System.currentTimeMillis()
    
    override fun analyze(image: ImageProxy) {
        frameCount++
        val currentTime = System.currentTimeMillis()
        
        // Her saniye FPS logla
        if (currentTime - lastLogTime >= 1000) {
            Log.d(TAG, "FPS: $frameCount")
            frameCount = 0
            lastLogTime = currentTime
        }
        
        // Görüntü bilgilerini logla
        Log.v(TAG, """
            Image info:
            - Size: ${image.width}x${image.height}
            - Format: ${image.format}
            - Rotation: ${image.imageInfo.rotationDegrees}
            - Timestamp: ${image.imageInfo.timestamp}
        """.trimIndent())
        
        image.close()
    }
}
```

#### 2. Kamera Durum İzleme

```kotlin
private fun observeCameraState(camera: Camera) {
    // Kamera durumunu izle
    camera.cameraInfo.cameraState.observe(this) { cameraState ->
        when (cameraState.type) {
            CameraState.Type.PENDING_OPEN -> {
                Log.d(TAG, "Camera opening...")
            }
            CameraState.Type.OPENING -> {
                Log.d(TAG, "Camera opening...")
            }
            CameraState.Type.OPEN -> {
                Log.d(TAG, "Camera opened successfully")
            }
            CameraState.Type.CLOSING -> {
                Log.d(TAG, "Camera closing...")
            }
            CameraState.Type.CLOSED -> {
                Log.d(TAG, "Camera closed")
            }
        }
        
        // Hata durumunu kontrol et
        cameraState.error?.let { error ->
            Log.e(TAG, "Camera error: ${error.code}")
            handleCameraError(error)
        }
    }
}

private fun handleCameraError(error: CameraState.StateError) {
    when (error.code) {
        CameraState.ERROR_STREAM_CONFIG -> {
            Log.e(TAG, "Stream configuration error")
            // Use case yapılandırmasını basitleştir
        }
        CameraState.ERROR_CAMERA_IN_USE -> {
            Log.e(TAG, "Camera in use by another app")
            // Kullanıcıyı bilgilendir
        }
        CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
            Log.e(TAG, "Maximum cameras in use")
            // Diğer kamera kullanımlarını kapat
        }
        CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
            Log.e(TAG, "Recoverable camera error")
            // Yeniden başlatmayı dene
        }
        CameraState.ERROR_CAMERA_DISABLED -> {
            Log.e(TAG, "Camera disabled")
            // Kullanıcıyı ayarlara yönlendir
        }
        CameraState.ERROR_CAMERA_FATAL_ERROR -> {
            Log.e(TAG, "Fatal camera error")
            // Uygulamayı yeniden başlat
        }
    }
}
```

### Test İpuçları

```kotlin
// Unit test için mock analyzer
class MockAnalyzer : ImageAnalysis.Analyzer {
    var processedImageCount = 0
    var lastImageTimestamp = 0L
    
    override fun analyze(image: ImageProxy) {
        processedImageCount++
        lastImageTimestamp = image.imageInfo.timestamp
        image.close()
    }
    
    fun reset() {
        processedImageCount = 0
        lastImageTimestamp = 0L
    }
}

// Test kullanımı
@Test
fun testImageAnalysis() {
    val mockAnalyzer = MockAnalyzer()
    
    // Test senaryosu...
    
    // Sonuçları doğrula
    assertEquals(expectedFrameCount, mockAnalyzer.processedImageCount)
    assertTrue(mockAnalyzer.lastImageTimestamp > 0)
}
```

---

Bu kapsamlı guide, Android CameraX API'nın tüm önemli yönlerini Kotlin kod örnekleriyle birlikte ele almaktadır. Real-time görüntü işleme, OpenCV entegrasyonu ve en güncel özellikler dahil olmak üzere pratik implementasyon detayları sunulmaktadır.

## Kaynaklar

[1] [CameraX Overview - Official Documentation](https://developer.android.com/media/camera/camerax)
[2] [CameraX Architecture Documentation](https://developer.android.com/media/camera/camerax/architecture)
[3] [Getting Started with CameraX Codelab](https://developer.android.com/codelabs/camerax-getting-started)
[4] [What's new in CameraX 1.4.0](https://android-developers.googleblog.com/2024/12/whats-new-in-camerax-140-and-jetpack-compose-support.html)
[5] [Image Analysis with CameraX](https://developer.android.com/media/camera/camerax/analyze)
[6] [CameraX OpenCV Integration Sample](https://github.com/mcanyucel/camerax-opencv)
[7] [Core Principles Behind CameraX](https://medium.com/androiddevelopers/core-principles-behind-camerax-jetpack-library-8e8380f7604c)
[8] [Android CameraX: Preview, Analyze, Capture](https://proandroiddev.com/android-camerax-preview-analyze-capture-1b3f403a9395)
