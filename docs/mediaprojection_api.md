# Android MediaProjection API ile Ekran Görüntüsü Alma - Kapsamlı Geliştirici Rehberi

## Özet

Bu rehber, Android MediaProjection API'sini kullanarak ekran görüntüsü alma tekniklerini, izin yönetimini ve dosya kaydetme yöntemlerini detaylı olarak ele almaktadır. Android 14+ için güncel implementasyon yöntemleri, VirtualDisplay kullanımı ve bitmap işleme teknikleri açıklanmıştır. MediaProjection API, Android 5 (API 21) ile tanıtılan ve cihaz ekranının veya tek bir uygulamanın içeriğini medya akışı olarak yakalama yeteneği sunan güçlü bir mekanizmadır[1].

## Giriş

MediaProjection API, Android geliştiricilerine cihaz ekranının veya belirli uygulama pencerelerinin içeriğini yakalama imkanı sunar. Bu API, ekran kaydı, canlı yayın, uzaktan destek ve ekran paylaşımı gibi senaryolarda kritik öneme sahiptir. Android 14 ile birlikte getirilen yeni özellikler ve güvenlik güncellemeleri, bu API'nin kullanımında önemli değişiklikler içermektedir[2,3].

## 1. MediaProjection API'ye Genel Bakış

### 1.1 API'nin Temelleri

MediaProjection API, üç temel bileşen üzerinde çalışır[1]:

1. **Gerçek Ekran**: Cihaz ekranı veya uygulama penceresi
2. **VirtualDisplay**: Yakalanan görüntüyü işleyen sanal ekran
3. **Surface**: Görüntü verilerini tüketen yüzey (MediaRecorder, SurfaceTexture, ImageReader)

### 1.2 API Seviyesi Gereksinimleri

- **Minimum API Seviyesi**: Android 5.0 (API 21)
- **Tam Özellik Desteği**: Android 7.0+ (API 24) - PixelCopy desteği
- **Android 14 Yenilikleri**: API 34 - Uygulama ekranı paylaşımı

### 1.3 Desteklenen Surface Türleri

MediaProjection API aşağıdaki Surface türlerini destekler[1]:
- **MediaRecorder**: Video kayıt işlemleri için
- **SurfaceTexture**: OpenGL ES tabanlı işlemler için  
- **ImageReader**: Bitmap oluşturma ve görüntü işleme için

## 2. İzin Yönetimi ve Güvenlik

### 2.1 Android 14+ Güvenlik Değişiklikleri

Android 14 (API 34) ile birlikte MediaProjection kullanımında kritik güvenlik değişiklikleri yapılmıştır[2]:

#### 2.1.1 Foreground Service İzinleri

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />

<service
    android:name=".ScreenCaptureService"
    android:foregroundServiceType="mediaProjection"
    android:exported="false" />
```

#### 2.1.2 Yakalama Oturumu Başına Kullanıcı Onayı

Android 14+ hedefleyen uygulamalar için her MediaProjection yakalama oturumunda kullanıcı onayı zorunludur[2]:

```kotlin
// Her createVirtualDisplay çağrısı için yeni izin gereklidir
// Aynı MediaProjection örneği sadece bir kez kullanılabilir
class ScreenCaptureManager {
    
    fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK) {
            val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
            
            // MediaProjection sadece bir kez kullanılabilir (Android 14+)
            startScreenCapture(mediaProjection)
        }
    }
}
```

#### 2.1.3 SecurityException Durumları

Android 14+ cihazlarda aşağıdaki durumlar SecurityException'a neden olur[2]:

1. **Intent Önbellekleme**: `createScreenCaptureIntent()` sonucunun cache'lenmesi
2. **Çoklu Kullanım**: Aynı MediaProjection örneği üzerinde birden fazla `createVirtualDisplay()` çağrısı
3. **İzin Eksikliği**: Foreground service izinlerinin eksik olması

### 2.2 Callback Kaydı Zorunluluğu

Android 14+ cihazlarda MediaProjection.Callback kaydı zorunludur[2]:

```kotlin
class ScreenCaptureService : Service() {
    
    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            // Kaynakları serbest bırak
            virtualDisplay?.release()
            imageReader?.close()
            
            // UI güncelle
            stopForeground(true)
        }
        
        override fun onCapturedContentResize(width: Int, height: Int) {
            // VirtualDisplay boyutunu güncelle
            virtualDisplay?.resize(width, height, displayMetrics.densityDpi)
        }
        
        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            // Görünürlük değişikliklerini işle
            if (!isVisible) {
                // Kaynakları koru, yakalamayı duraklat
                pauseCapture()
            } else {
                // Yakalamaya devam et
                resumeCapture()
            }
        }
    }
}
```

## 3. Ekran Görüntüsü Alma Teknikleri

### 3.1 ImageReader Yaklaşımı

ImageReader, MediaProjection ile ekran yakalama için en yaygın kullanılan yöntemdir[4]:

```kotlin
class ScreenCaptureWithImageReader {
    
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    
    fun setupScreenCapture(mediaProjection: MediaProjection, width: Int, height: Int) {
        // ImageReader oluştur
        imageReader = ImageReader.newInstance(
            width, height, 
            PixelFormat.RGBA_8888, // Android 14+ için önerilen format
            2 // Buffer sayısı
        ).apply {
            setOnImageAvailableListener(imageAvailableListener, backgroundHandler)
        }
        
        // VirtualDisplay oluştur
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width, height,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            backgroundHandler
        )
    }
    
    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            image?.let { processImage(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
        } finally {
            image?.close()
        }
    }
    
    private fun processImage(image: Image) {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        
        bitmap.copyPixelsFromBuffer(buffer)
        
        // Bitmap'i işle veya kaydet
        saveBitmapToFile(bitmap)
        bitmap.recycle()
    }
}
```

### 3.2 OpenGL ES Yaklaşımı

Özel işlemler veya performans optimizasyonu gerektiğinde OpenGL ES kullanılabilir[4]:

```kotlin
class ScreenCaptureWithOpenGL {
    
    private var eglCore: EglCore? = null
    private var offscreenSurface: OffscreenSurface? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var textureRenderer: TextureRenderer? = null
    
    fun setupOpenGLCapture(mediaProjection: MediaProjection, width: Int, height: Int) {
        // EGL Context oluştur
        eglCore = EglCore(null, EglCore.FLAG_TRY_GLES3)
        offscreenSurface = OffscreenSurface(eglCore!!, width, height)
        offscreenSurface!!.makeCurrent()
        
        // Texture renderer kurulumu
        textureRenderer = TextureRenderer()
        val textureId = textureRenderer!!.createTextureObject()
        
        // SurfaceTexture oluştur
        surfaceTexture = SurfaceTexture(textureId, false).apply {
            setDefaultBufferSize(width, height)
            setOnFrameAvailableListener(frameAvailableListener)
        }
        
        // VirtualDisplay oluştur
        val producerSurface = Surface(surfaceTexture)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "OpenGL_ScreenCapture",
            width, height,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            producerSurface,
            null,
            backgroundHandler
        )
    }
    
    private val frameAvailableListener = SurfaceTexture.OnFrameAvailableListener { texture ->
        offscreenSurface?.makeCurrent()
        texture.updateTexImage()
        
        val transformMatrix = FloatArray(16)
        texture.getTransformMatrix(transformMatrix)
        
        textureRenderer?.drawFrame(transformMatrix)
        offscreenSurface?.swapBuffers()
        
        // Piksel verilerini oku
        readPixelsAndCreateBitmap()
    }
    
    private fun readPixelsAndCreateBitmap() {
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
        buffer.order(ByteOrder.nativeOrder())
        
        GLES20.glReadPixels(
            0, 0, width, height,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
            buffer
        )
        
        buffer.rewind()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        
        // Y koordinatını ters çevir (OpenGL koordinat sistemi)
        val flippedBitmap = flipBitmapVertically(bitmap)
        saveBitmapToFile(flippedBitmap)
        
        bitmap.recycle()
        flippedBitmap.recycle()
    }
}
```

## 4. VirtualDisplay Kullanımı

### 4.1 VirtualDisplay Oluşturma ve Yönetimi

VirtualDisplay, MediaProjection'ın kalbidir ve dikkatli yönetim gerektirir[1]:

```kotlin
class VirtualDisplayManager {
    
    fun createVirtualDisplay(
        mediaProjection: MediaProjection,
        surface: Surface,
        width: Int,
        height: Int
    ): VirtualDisplay? {
        
        return try {
            mediaProjection.createVirtualDisplay(
                "ScreenCapture_VirtualDisplay",
                width,
                height,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface,
                virtualDisplayCallback,
                backgroundHandler
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "VirtualDisplay oluşturulamadı: ${e.message}")
            null
        }
    }
    
    private val virtualDisplayCallback = object : VirtualDisplay.Callback() {
        override fun onPaused() {
            Log.d(TAG, "VirtualDisplay duraklatıldı")
        }
        
        override fun onResumed() {
            Log.d(TAG, "VirtualDisplay devam ettirildi")
        }
        
        override fun onStopped() {
            Log.d(TAG, "VirtualDisplay durduruldu")
            // Temizlik işlemleri
        }
    }
    
    fun resizeVirtualDisplay(virtualDisplay: VirtualDisplay, newWidth: Int, newHeight: Int) {
        // Android 12L+ için boyut değişikliklerini handle et
        virtualDisplay.resize(newWidth, newHeight, resources.displayMetrics.densityDpi)
    }
}
```

### 4.2 Boyutlandırma ve Ölçekleme

Android 12L (API 32) ile gelen ölçekleme özelliklerini kullanarak optimum performans elde edilebilir[1]:

```kotlin
class ScreenScalingManager {
    
    fun calculateOptimalDimensions(
        screenWidth: Int,
        screenHeight: Int,
        maxWidth: Int = 1920,
        maxHeight: Int = 1080
    ): Pair<Int, Int> {
        
        val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
        
        return when {
            screenWidth > maxWidth || screenHeight > maxHeight -> {
                if (aspectRatio > 1.0f) {
                    // Landscape
                    Pair(maxWidth, (maxWidth / aspectRatio).toInt())
                } else {
                    // Portrait
                    Pair((maxHeight * aspectRatio).toInt(), maxHeight)
                }
            }
            else -> Pair(screenWidth, screenHeight)
        }
    }
    
    fun createScaledVirtualDisplay(
        mediaProjection: MediaProjection,
        surface: Surface
    ): VirtualDisplay? {
        
        val displayMetrics = resources.displayMetrics
        val (scaledWidth, scaledHeight) = calculateOptimalDimensions(
            displayMetrics.widthPixels,
            displayMetrics.heightPixels
        )
        
        return mediaProjection.createVirtualDisplay(
            "ScaledCapture",
            scaledWidth,
            scaledHeight,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )
    }
}
```

## 5. Bitmap İşleme Teknikleri

### 5.1 Bellek Verimli Bitmap İşleme

Ekran yakalama sırasında bellek yönetimi kritik öneme sahiptir:

```kotlin
class BitmapProcessor {
    
    private val bitmapPool = LruCache<String, Bitmap>(10)
    
    fun processImageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        
        // Bitmap boyutlarını hesapla
        val bitmapWidth = image.width + rowPadding / pixelStride
        val bitmapHeight = image.height
        
        // Mevcut bitmap'i yeniden kullan
        val cacheKey = "${bitmapWidth}x${bitmapHeight}"
        var bitmap = bitmapPool.get(cacheKey)
        
        if (bitmap == null || bitmap.isRecycled) {
            bitmap = Bitmap.createBitmap(
                bitmapWidth,
                bitmapHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmapPool.put(cacheKey, bitmap)
        }
        
        try {
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Row padding'i kırp
            return if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap işleme hatası", e)
            return null
        }
    }
    
    fun compressBitmapForStorage(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
    
    fun scaleBitmapToFitScreen(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        
        if (ratio >= 1.0f) return bitmap
        
        val scaledWidth = (bitmap.width * ratio).toInt()
        val scaledHeight = (bitmap.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }
}
```

### 5.2 Async Bitmap İşleme

UI engellemelerini önlemek için asenkron işleme:

```kotlin
class AsyncBitmapProcessor {
    
    private val executorService = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    fun processImageAsync(
        image: Image,
        onComplete: (Bitmap?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        executorService.execute {
            try {
                val bitmap = processImageToBitmap(image)
                mainHandler.post { onComplete(bitmap) }
            } catch (e: Exception) {
                mainHandler.post { onError(e) }
            }
        }
    }
    
    fun shutdown() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
    }
}
```

## 6. Dosya Kaydetme Yöntemleri

### 6.1 Android 14+ Scoped Storage Uyumluluğu

Android 14 ile birlikte dosya kaydetme işlemleri için MediaStore API kullanımı önerilir:

```kotlin
class ScreenshotSaver {
    
    fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        displayName: String = "screenshot_${System.currentTimeMillis()}.jpg"
    ): Uri? {
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        return uri?.let { imageUri ->
            try {
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                
                // Pending durumunu kaldır
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
                
                imageUri
            } catch (e: Exception) {
                Log.e(TAG, "Dosya kaydetme hatası", e)
                resolver.delete(imageUri, null, null)
                null
            }
        }
    }
    
    fun saveToAppPrivateStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): File? {
        
        return try {
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Private storage kaydetme hatası", e)
            null
        }
    }
    
    fun saveWithSAF(
        activity: Activity,
        bitmap: Bitmap,
        requestCode: Int
    ) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/jpeg"
            putExtra(Intent.EXTRA_TITLE, "screenshot_${System.currentTimeMillis()}.jpg")
        }
        activity.startActivityForResult(intent, requestCode)
    }
    
    fun handleSAFResult(
        context: Context,
        data: Intent?,
        bitmap: Bitmap
    ): Boolean {
        
        return data?.data?.let { uri ->
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "SAF kaydetme hatası", e)
                false
            }
        } ?: false
    }
}
```

### 6.2 Batch Kaydetme İşlemleri

Çoklu ekran görüntüsü kaydetme için optimize edilmiş yaklaşım:

```kotlin
class BatchScreenshotSaver {
    
    private val saveQueue = LinkedBlockingQueue<SaveTask>()
    private val saveExecutor = Executors.newSingleThreadExecutor()
    
    data class SaveTask(
        val bitmap: Bitmap,
        val fileName: String,
        val callback: (Uri?) -> Unit
    )
    
    init {
        saveExecutor.execute { processSaveQueue() }
    }
    
    fun queueSave(bitmap: Bitmap, fileName: String, callback: (Uri?) -> Unit) {
        saveQueue.offer(SaveTask(bitmap, fileName, callback))
    }
    
    private fun processSaveQueue() {
        while (!Thread.currentThread().isInterrupted) {
            try {
                val task = saveQueue.take()
                val uri = saveBitmapToMediaStore(task.bitmap, task.fileName)
                
                Handler(Looper.getMainLooper()).post {
                    task.callback(uri)
                    task.bitmap.recycle()
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                Log.e(TAG, "Batch save hatası", e)
            }
        }
    }
    
    fun shutdown() {
        saveExecutor.shutdown()
    }
}
```

## 7. En İyi Uygulamalar ve Performans Optimizasyonu

### 7.1 Bellek Yönetimi

```kotlin
class MemoryOptimizedScreenCapture {
    
    // Bitmap pool kullanarak bellek ayırma/bırakma döngüsünü azalt
    private val bitmapPool = Pools.SynchronizedPool<Bitmap>(5)
    
    // WeakReference ile memory leak'leri önle
    private var contextRef: WeakReference<Context>? = null
    
    fun optimizeForMemory() {
        // JVM heap boyutunu izle
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        
        if (usedMemory > maxMemory * 0.8) {
            // Bellek temizliği yap
            triggerGarbageCollection()
            clearBitmapCache()
        }
    }
    
    private fun triggerGarbageCollection() {
        System.gc()
        // Kısa bir bekleme sonrası tekrar kontrol et
        Handler().postDelayed({
            System.runFinalization()
        }, 100)
    }
    
    private fun clearBitmapCache() {
        // LruCache'leri temizle
        bitmapPool.release()
    }
}
```

### 7.2 Thread Yönetimi

```kotlin
class ThreadOptimizedCapture {
    
    private val captureThread = HandlerThread("ScreenCapture").apply { start() }
    private val captureHandler = Handler(captureThread.looper)
    
    private val processingThread = HandlerThread("ImageProcessing").apply { start() }
    private val processingHandler = Handler(processingThread.looper)
    
    fun setupOptimizedCapture() {
        // ImageReader'ı capture thread'inde çalıştır
        imageReader?.setOnImageAvailableListener({ reader ->
            // Görüntü işlemeyi ayrı thread'e taşı
            processingHandler.post {
                processImageSafely(reader)
            }
        }, captureHandler)
    }
    
    private fun processImageSafely(reader: ImageReader) {
        try {
            reader.acquireLatestImage()?.use { image ->
                val bitmap = convertImageToBitmap(image)
                bitmap?.let { saveBitmapAsync(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Güvenli görüntü işleme hatası", e)
        }
    }
    
    fun cleanup() {
        captureThread.quitSafely()
        processingThread.quitSafely()
    }
}
```

### 7.3 Performans İzleme

```kotlin
class PerformanceMonitor {
    
    private var frameCount = 0
    private var lastFrameTime = System.currentTimeMillis()
    
    fun trackCapturePerformance() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastFrameTime >= 1000) {
            val fps = frameCount.toFloat() / ((currentTime - lastFrameTime) / 1000f)
            Log.d(TAG, "Capture FPS: $fps")
            
            frameCount = 0
            lastFrameTime = currentTime
            
            // Düşük FPS durumunda optimize et
            if (fps < 15) {
                optimizeCaptureSettings()
            }
        }
    }
    
    private fun optimizeCaptureSettings() {
        // Çözünürlüğü düşür
        // Buffer sayısını azalt
        // İşleme kalitesini düşür
        Log.d(TAG, "Performans optimizasyonu uygulandı")
    }
}
```

## 8. Android 14+ Güncellemeleri ve Yeni Özellikler

### 8.1 Uygulama Ekranı Paylaşımı

Android 14 QPR2 ile gelen uygulama ekranı paylaşımı özelliği[3]:

```kotlin
class AppScreenSharingManager {
    
    fun enableAppScreenSharing(mediaProjection: MediaProjection) {
        // MediaProjection callback'leri kaydet
        mediaProjection.registerCallback(object : MediaProjection.Callback() {
            
            override fun onCapturedContentResize(width: Int, height: Int) {
                // Yakalanan içeriğin boyutu değiştiğinde
                Log.d(TAG, "Yakalanan içerik boyutu: ${width}x${height}")
                
                // VirtualDisplay'i yeniden boyutlandır
                virtualDisplay?.resize(width, height, displayMetrics.densityDpi)
                
                // Surface boyutunu güncelle
                updateSurfaceSize(width, height)
            }
            
            override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                // Yakalanan içeriğin görünürlüğü değiştiğinde
                if (isVisible) {
                    Log.d(TAG, "Yakalanan uygulama görünür")
                    resumeCapture()
                } else {
                    Log.d(TAG, "Yakalanan uygulama gizli")
                    pauseCapture()
                }
            }
        }, backgroundHandler)
    }
    
    fun createConfigForAppSharing(): MediaProjectionConfig {
        // Uygulama ekranı paylaşımı için varsayılan config
        return MediaProjectionConfig.createConfigForUserChoice()
    }
    
    fun optOutOfAppScreenSharing(): Intent {
        // Uygulama ekranı paylaşımından vazgeçme
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        
        val config = MediaProjectionConfig.createConfigForDefaultDisplay()
        return mediaProjectionManager.createScreenCaptureIntent(config)
    }
}
```

### 8.2 Android 15 QPR1 Özellikleri

Android 15 QPR1+ cihazlarda durum çubuğu çipi ve otomatik durdurma[1]:

```kotlin
class AndroidVersionCompatibility {
    
    fun handleAndroid15Features() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Android 15+ özel işlemler
            setupStatusBarChipHandling()
            setupAutoStopHandling()
        }
    }
    
    private fun setupStatusBarChipHandling() {
        // Durum çubuğu çipi ile kullanıcı etkileşimini handle et
        // Kullanıcı çipe dokunduğunda yakalamayı durdur
    }
    
    private fun setupAutoStopHandling() {
        // Ekran kilitlendiğinde otomatik durdurma
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        
        // Screen lock listener implementasyonu
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    // Ekran yakalamayı durdur
                    stopScreenCapture()
                }
            }
        }, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }
}
```

## 9. Sorun Giderme ve Yaygın Hatalar

### 9.1 Android 14 SecurityException Çözümleri

```kotlin
class TroubleshootingManager {
    
    fun handleSecurityException(exception: SecurityException) {
        val message = exception.message ?: ""
        
        when {
            message.contains("FOREGROUND_SERVICE_MEDIA_PROJECTION") -> {
                // Foreground service izni eksik
                Log.e(TAG, "Foreground service izni gerekli")
                requestForegroundServicePermission()
            }
            
            message.contains("MediaProjection") -> {
                // MediaProjection kullanım hatası
                Log.e(TAG, "MediaProjection yeniden kullanım hatası")
                createNewMediaProjection()
            }
            
            else -> {
                Log.e(TAG, "Bilinmeyen güvenlik hatası: $message")
            }
        }
    }
    
    private fun requestForegroundServicePermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${packageName}")
        }
        startActivity(intent)
    }
    
    private fun createNewMediaProjection() {
        // Yeni MediaProjection instance'ı oluştur
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }
}
```

### 9.2 Yaygın ImageReader Sorunları

```kotlin
class ImageReaderTroubleshooting {
    
    fun handleImageReaderIssues() {
        try {
            imageReader = ImageReader.newInstance(width, height, format, maxImages)
        } catch (e: IllegalArgumentException) {
            when {
                e.message?.contains("width") == true -> {
                    Log.e(TAG, "Geçersiz genişlik değeri")
                    // Geçerli boyutları yeniden hesapla
                    recalculateDimensions()
                }
                
                e.message?.contains("format") == true -> {
                    Log.e(TAG, "Desteklenmeyen görüntü formatı")
                    // Alternatif format kullan
                    useAlternativeFormat()
                }
                
                e.message?.contains("maxImages") == true -> {
                    Log.e(TAG, "Çok fazla buffer sayısı")
                    // Buffer sayısını azalt
                    reduceBufferCount()
                }
            }
        }
    }
    
    private fun useAlternativeFormat() {
        // RGBA_8888 yerine RGB_565 dene
        val fallbackFormat = when (originalFormat) {
            PixelFormat.RGBA_8888 -> PixelFormat.RGB_565
            ImageFormat.JPEG -> ImageFormat.YUV_420_888
            else -> PixelFormat.RGBA_8888
        }
        
        createImageReaderWithFormat(fallbackFormat)
    }
    
    fun handleAcquireImageFailures() {
        try {
            val image = imageReader?.acquireLatestImage()
            image?.use { processImage(it) }
        } catch (e: UnsupportedOperationException) {
            Log.w(TAG, "acquireLatestImage desteklenmiyor, acquireNextImage dene")
            tryAcquireNextImage()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "ImageReader kapalı, yeniden oluştur")
            recreateImageReader()
        }
    }
}
```

### 9.3 Bellek Yönetimi Sorunları

```kotlin
class MemoryIssueResolver {
    
    fun handleOutOfMemoryError() {
        try {
            // Bitmap işleme
            processBitmap()
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Bellek yetersiz, optimizasyon uygulanıyor")
            
            // 1. Mevcut bitmap'leri temizle
            clearBitmapCache()
            
            // 2. Garbage collection tetikle
            System.gc()
            
            // 3. Daha düşük çözünürlük kullan
            reduceResolution()
            
            // 4. Yeniden dene
            retryWithOptimizedSettings()
        }
    }
    
    private fun clearBitmapCache() {
        // Tüm cached bitmap'leri recycle et
        bitmapCache.evictAll()
        
        // Pool'daki bitmap'leri temizle
        bitmapPool.clear()
    }
    
    private fun reduceResolution() {
        // Çözünürlüğü %75'e düşür
        currentWidth = (currentWidth * 0.75).toInt()
        currentHeight = (currentHeight * 0.75).toInt()
        
        Log.d(TAG, "Çözünürlük düşürüldü: ${currentWidth}x${currentHeight}")
    }
    
    private fun retryWithOptimizedSettings() {
        Handler().postDelayed({
            try {
                setupScreenCaptureWithOptimizedSettings()
            } catch (e: Exception) {
                Log.e(TAG, "Optimize edilmiş ayarlarla yeniden deneme başarısız", e)
            }
        }, 1000)
    }
}
```

## 10. Kod Örnekleri - Tam İmplementasyon

### 10.1 Komple ScreenCaptureService

```kotlin
class ScreenCaptureService : Service() {
    
    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START_CAPTURE = "ACTION_START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "ACTION_STOP_CAPTURE"
    }
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    private val binder = ScreenCaptureBinder()
    
    inner class ScreenCaptureBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }
    
    override fun onCreate() {
        super.onCreate()
        startBackgroundThread()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CAPTURE -> {
                val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>("data")
                
                if (resultCode == Activity.RESULT_OK && data != null) {
                    startScreenCapture(resultCode, data)
                }
            }
            ACTION_STOP_CAPTURE -> {
                stopScreenCapture()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    private fun startScreenCapture(resultCode: Int, data: Intent) {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "MediaProjection durduruldu")
                stopScreenCapture()
            }
            
            override fun onCapturedContentResize(width: Int, height: Int) {
                Log.d(TAG, "İçerik boyutu değişti: ${width}x${height}")
                virtualDisplay?.resize(width, height, resources.displayMetrics.densityDpi)
            }
            
            override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                Log.d(TAG, "İçerik görünürlüğü: $isVisible")
            }
        }, backgroundHandler)
        
        setupVirtualDisplay()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    private fun setupVirtualDisplay() {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        
        imageReader = ImageReader.newInstance(
            width, height,
            PixelFormat.RGBA_8888,
            2
        ).apply {
            setOnImageAvailableListener(imageAvailableListener, backgroundHandler)
        }
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            backgroundHandler
        )
    }
    
    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            image?.let { processImage(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Görüntü işleme hatası", e)
        } finally {
            image?.close()
        }
    }
    
    private fun processImage(image: Image) {
        val bitmap = convertImageToBitmap(image)
        bitmap?.let { 
            saveBitmapToFile(it)
            it.recycle()
        }
    }
    
    private fun convertImageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Row padding'i kırp
            if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap dönüştürme hatası", e)
            null
        }
    }
    
    private fun saveBitmapToFile(bitmap: Bitmap) {
        val fileName = "screenshot_${System.currentTimeMillis()}.jpg"
        val screenshotSaver = ScreenshotSaver()
        screenshotSaver.saveBitmapToMediaStore(this, bitmap, fileName)
    }
    
    private fun stopScreenCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        stopForeground(true)
        stopSelf()
    }
    
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("ScreenCapture").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }
    
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Background thread durdurma hatası", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SCREEN_CAPTURE_CHANNEL",
                "Ekran Yakalama",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ekran yakalama servisi bildirimleri"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) 
                as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP_CAPTURE
        }
        
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, "SCREEN_CAPTURE_CHANNEL")
            .setContentTitle("Ekran Yakalama Aktif")
            .setContentText("Ekran görüntüsü alınıyor...")
            .setSmallIcon(R.drawable.ic_screen_capture)
            .addAction(R.drawable.ic_stop, "Durdur", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
        stopBackgroundThread()
    }
}
```

### 10.2 MainActivity İmplementasyonu

```kotlin
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1000
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }
    
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var screenCaptureService: ScreenCaptureService? = null
    private var isServiceBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScreenCaptureService.ScreenCaptureBinder
            screenCaptureService = binder.getService()
            isServiceBound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            screenCaptureService = null
            isServiceBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
            as MediaProjectionManager
        
        setupUI()
        checkPermissions()
    }
    
    private fun setupUI() {
        findViewById<Button>(R.id.btnStartCapture).setOnClickListener {
            startScreenCapture()
        }
        
        findViewById<Button>(R.id.btnStopCapture).setOnClickListener {
            stopScreenCapture()
        }
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        // Android 14+ için foreground service izni
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION)
            }
        }
        
        // Bildirim izni (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    private fun startScreenCapture() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }
    
    private fun stopScreenCapture() {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP_CAPTURE
        }
        startService(intent)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val intent = Intent(this, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_START_CAPTURE
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }
                
                // Foreground service olarak başlat
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                
                // Service'e bağlan
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                
            } else {
                Toast.makeText(this, "Ekran yakalama izni reddedildi", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            
            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "Gerekli izinler verilmedi: ${deniedPermissions.joinToString()}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}
```

## Sonuç

Android MediaProjection API, güçlü ekran yakalama yetenekleri sunmasına rağmen, özellikle Android 14+ cihazlarda dikkatli implementasyon gerektirir. Bu rehberde ele alınan konular:

- **İzin Yönetimi**: Android 14+ için yeni güvenlik gereksinimleri ve foreground service izinleri
- **VirtualDisplay Kullanımı**: Verimli ekran yakalama için optimize edilmiş yaklaşımlar
- **Bitmap İşleme**: Bellek verimli görüntü işleme teknikleri
- **Dosya Kaydetme**: Scoped Storage uyumlu kaydetme yöntemleri
- **Performans Optimizasyonu**: Thread yönetimi ve bellek optimizasyonu

Geliştiricilerin bu teknikleri uygulamasıyla birlikte, kullanıcı deneyimini etkilemeden güvenilir ekran yakalama uygulamaları geliştirebilirler. Android platformunun sürekli evrilmesi nedeniyle, yeni API güncellemelerini takip etmek ve uygulamaları buna göre güncellemek kritik öneme sahiptir.

## Kaynaklar

[1] [Media projection - Android Developers](https://developer.android.com/media/grow/media-projection)
[2] [Android 14 Davranış Değişiklikleri](https://developer.android.com/about/versions/14/behavior-changes-14)
[3] [Android 14'te Gelişmiş Ekran Paylaşım Özellikleri](https://android-developers.googleblog.com/2024/03/enhanced-screen-sharing-capabilities-in-android-14.html)
[4] [MediaProjection ile Ekran Görüntüsü Alma](https://stackoverflow.com/questions/26545970/take-a-screenshot-using-mediaprojection)
[5] [Android 14'te MediaProjection Sorunları](https://stackoverflow.com/questions/77307867/screen-capture-mediaprojection-on-android-14)
[6] [Android'de Programatik Ekran Yakalama Rehberi](https://medium.com/bolt-labs/how-to-programmatically-capture-screen-on-android-a-comprehensive-guide-f500c95e455a)
[7] [Kotlin ile Ekran Görüntüsü Alma Örneği](https://gist.github.com/nosix/fcf1923f0c7cc42aa8c2ee70ff457b9c)
