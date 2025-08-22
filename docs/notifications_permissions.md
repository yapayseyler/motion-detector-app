# Android Bildirim Sistemi, Ses Çalma ve İzin Yönetimi Rehberi

## Özet

Bu rehber, Android 13+ ve Android 11+ sürümlerinde bildirim sistemi (NotificationCompat), ses çalma (MediaPlayer/SoundPool) ve izin yönetimi (Runtime Permissions) konularında güncel implementasyon detaylarını kapsamaktadır. Özellikle POST_NOTIFICATIONS izni (Android 13+) ve MANAGE_EXTERNAL_STORAGE izni (Android 11+) ile diğer gerekli izinlerin detaylı kullanımı açıklanmıştır.

## 1. Android Bildirim Sistemi (NotificationCompat)

### 1.1 Temel Bildirim Oluşturma

NotificationCompat, Android'in farklı sürümlerinde tutarlı bildirim deneyimi sağlamak için AndroidX kütüphanesinin bir parçasıdır[3]. Android 9 (API 28) ile uyumluluk sağlar.

#### Temel Implementasyon:

```kotlin
// Bildirim kanalı oluşturma (Android 8.0+ için zorunlu)
private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Ana Kanal"
        val descriptionText = "Uygulama bildirimleri için ana kanal"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

// Temel bildirim oluşturma
private fun createBasicNotification() {
    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle("Bildirim Başlığı")
        .setContentText("Bildirim içeriği")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(this)) {
        // notificationId benzersiz bir tam sayı
        notify(notificationId, builder.build())
    }
}

companion object {
    private const val CHANNEL_ID = "main_channel"
    private const val notificationId = 1
}
```

### 1.2 Bildirim Kanalları Yönetimi

Android 8.0 (API 26) ve üzeri sürümler için bildirim kanalları zorunludur[3]. Kanallar, kullanıcının bildirim davranışlarını kategorize etmesini sağlar.

```kotlin
class NotificationChannelManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Yüksek öncelikli kanal (sesli bildirimler)
            val highPriorityChannel = NotificationChannel(
                HIGH_PRIORITY_CHANNEL,
                "Acil Bildirimler",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Acil durumlar ve önemli bildirimler"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            
            // Normal öncelikli kanal
            val normalChannel = NotificationChannel(
                NORMAL_CHANNEL,
                "Normal Bildirimler", 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Genel uygulama bildirimleri"
            }
            
            // Düşük öncelikli kanal (sessiz)
            val lowPriorityChannel = NotificationChannel(
                LOW_PRIORITY_CHANNEL,
                "Arka Plan Bildirimleri",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arka plan güncellemeleri"
                setSound(null, null)
                enableVibration(false)
            }
            
            notificationManager.createNotificationChannels(
                listOf(highPriorityChannel, normalChannel, lowPriorityChannel)
            )
        }
    }
    
    companion object {
        const val HIGH_PRIORITY_CHANNEL = "high_priority_channel"
        const val NORMAL_CHANNEL = "normal_channel"
        const val LOW_PRIORITY_CHANNEL = "low_priority_channel"
    }
}
```

### 1.3 Büyük Metin ve Resim Bildirimleri

```kotlin
// Büyük metin bildirimi
private fun createBigTextNotification() {
    val bigText = "Bu çok uzun bir bildirim metnidir. Genişletilebilir bildirimler " +
            "kullanıcıya daha fazla bilgi sunmak için kullanılır. Kullanıcı bildirimi " +
            "genişlettiğinde tüm metin görülebilir."
    
    val builder = NotificationCompat.Builder(this, NORMAL_CHANNEL)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle("Büyük Metin Bildirimi")
        .setContentText("Kısa özet...")
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(bigText)
                .setBigContentTitle("Genişletilmiş Başlık")
                .setSummaryText("Özet metni")
        )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    
    NotificationManagerCompat.from(this).notify(2, builder.build())
}

// Büyük resim bildirimi
private fun createBigPictureNotification() {
    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.big_image)
    
    val builder = NotificationCompat.Builder(this, NORMAL_CHANNEL)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle("Resim Bildirimi")
        .setContentText("Bir resim içeren bildirim")
        .setLargeIcon(bitmap)
        .setStyle(
            NotificationCompat.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(null) // Genişletildiğinde büyük ikonu gizle
        )
    
    NotificationManagerCompat.from(this).notify(3, builder.build())
}
```

### 1.4 İnteraktif Bildirimler

```kotlin
// Aksiyon butonları ile bildirim
private fun createActionNotification() {
    // PendingIntent'ler oluştur
    val replyIntent = Intent(this, ReplyReceiver::class.java)
    val replyPendingIntent = PendingIntent.getBroadcast(
        this, 0, replyIntent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    val dismissIntent = Intent(this, DismissReceiver::class.java)
    val dismissPendingIntent = PendingIntent.getBroadcast(
        this, 1, dismissIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    val builder = NotificationCompat.Builder(this, NORMAL_CHANNEL)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle("İnteraktif Bildirim")
        .setContentText("Bu bildirimde aksiyon butonları var")
        .addAction(R.drawable.ic_reply, "Yanıtla", replyPendingIntent)
        .addAction(R.drawable.ic_dismiss, "Reddet", dismissPendingIntent)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    
    NotificationManagerCompat.from(this).notify(4, builder.build())
}

// Doğrudan yanıt özelliği (Direct Reply)
private fun createDirectReplyNotification() {
    val remoteInput = RemoteInput.Builder("key_text_reply")
        .setLabel("Yanıtınızı yazın...")
        .build()
    
    val replyIntent = Intent(this, DirectReplyReceiver::class.java)
    val replyPendingIntent = PendingIntent.getBroadcast(
        this, 0, replyIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
    
    val action = NotificationCompat.Action.Builder(
        R.drawable.ic_reply,
        "Yanıtla",
        replyPendingIntent
    )
        .addRemoteInput(remoteInput)
        .build()
    
    val builder = NotificationCompat.Builder(this, NORMAL_CHANNEL)
        .setSmallIcon(R.drawable.notification_icon)
        .setContentTitle("Mesaj")
        .setContentText("Yeni mesajınız var")
        .addAction(action)
    
    NotificationManagerCompat.from(this).notify(5, builder.build())
}

// DirectReplyReceiver sınıfı
class DirectReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val results = RemoteInput.getResultsFromIntent(intent)
        val replyText = results?.getCharSequence("key_text_reply")
        
        if (replyText != null) {
            // Yanıtı işle
            processReply(replyText.toString())
            
            // Bildirimi güncelle
            updateNotificationAfterReply(context)
        }
    }
    
    private fun processReply(reply: String) {
        // Yanıt işleme logıını buraya ekle
        Log.d("DirectReply", "Yanıt alındı: $reply")
    }
    
    private fun updateNotificationAfterReply(context: Context) {
        val updatedBuilder = NotificationCompat.Builder(context, NORMAL_CHANNEL)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("Yanıt Gönderildi")
            .setContentText("Yanıtınız başarıyla gönderildi")
        
        NotificationManagerCompat.from(context).notify(5, updatedBuilder.build())
    }
}
```

## 2. Android 13+ POST_NOTIFICATIONS İzni

### 2.1 İzin Beyanı ve Implementasyonu

Android 13 (API 33) ve üzeri, bildirim göndermek için POST_NOTIFICATIONS çalışma zamanı izni gerektirir[1].

#### AndroidManifest.xml'de İzin Beyanı:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <application>
        <!-- Uygulama bileşenleri -->
    </application>
</manifest>
```

#### Kotlin/Java'da İzin İsteği:

```kotlin
class NotificationPermissionManager(private val activity: AppCompatActivity) {
    
    private val requestPermissionLauncher = 
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // İzin verildi - bildirim gönderebilirsiniz
                showNotificationPermissionGranted()
                enableNotificationFeatures()
            } else {
                // İzin reddedildi - kullanıcıyı bilgilendirin
                showNotificationPermissionDenied()
                handleNotificationPermissionDenied()
            }
        }
    
    fun checkAndRequestNotificationPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        // İzin zaten var
                        enableNotificationFeatures()
                    }
                    
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) -> {
                        // Kullanıcıya açıklama göster
                        showPermissionRationale()
                    }
                    
                    else -> {
                        // Doğrudan izin iste
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            else -> {
                // Android 13 öncesi - izin gerekmez
                enableNotificationFeatures()
            }
        }
    }
    
    private fun showPermissionRationale() {
        AlertDialog.Builder(activity)
            .setTitle("Bildirim İzni Gerekli")
            .setMessage("Uygulamamız size önemli güncellemeler ve hatırlatmalar " +
                       "göndermek için bildirim iznine ihtiyaç duyar. " +
                       "Bu izni vermek isterseniz lütfen 'İzin Ver' butonuna basın.")
            .setPositiveButton("İzin Ver") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Şimdi Değil") { dialog, _ ->
                dialog.dismiss()
                handleNotificationPermissionDenied()
            }
            .show()
    }
    
    private fun showNotificationPermissionGranted() {
        Toast.makeText(
            activity,
            "Bildirim izni verildi. Artık bildirimler alabilirsiniz!",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun showNotificationPermissionDenied() {
        Toast.makeText(
            activity,
            "Bildirim izni reddedildi. Önemli güncellemeleri kaçırabilirsiniz.",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun enableNotificationFeatures() {
        // Bildirim özelliklerini etkinleştir
        val notificationChannelManager = NotificationChannelManager(activity)
        notificationChannelManager.createChannels()
        
        // UI'da bildirim ile ilgili özellikleri göster
        updateUIForNotificationPermission(true)
    }
    
    private fun handleNotificationPermissionDenied() {
        // Bildirim özelliklerini devre dışı bırak
        updateUIForNotificationPermission(false)
        
        // Kullanıcıya alternatif bilgilendirme yöntemleri sun
        showAlternativeNotificationMethods()
    }
    
    private fun updateUIForNotificationPermission(hasPermission: Boolean) {
        // UI elementlerinin görünürlüğünü güncelle
        // Örn: bildirim ayarları butonunu göster/gizle
    }
    
    private fun showAlternativeNotificationMethods() {
        // Alternatif yöntemler (email, SMS vb.) göster
    }
    
    // İzin durumunu kontrol et
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 öncesi için her zaman true
            true
        }
    }
    
    // Bildirim gönderilmesi güvenli mi kontrol et
    fun canSendNotifications(): Boolean {
        val notificationManager = NotificationManagerCompat.from(activity)
        return notificationManager.areNotificationsEnabled() && hasNotificationPermission()
    }
}
```

### 2.2 En İyi Uygulamalar

```kotlin
class NotificationBestPractices {
    
    // Bağlam içinde izin isteme
    fun requestPermissionInContext(activity: Activity, feature: String) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle("$feature için Bildirim İzni")
            .setMessage("$feature özelliği için size bildirimler göndermemiz gerekiyor. " +
                       "Bu sayede önemli güncellemelerden haberdar olabilirsiniz.")
            .setPositiveButton("İzin Ver") { _, _ ->
                // İzin isteme kodu
            }
            .setNegativeButton("Hayır") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        dialog.show()
    }
    
    // Bildirim gönderiminde güvenlik kontrolü
    fun sendNotificationSafely(context: Context, notification: Notification, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        
        if (notificationManager.areNotificationsEnabled()) {
            try {
                notificationManager.notify(notificationId, notification)
            } catch (e: SecurityException) {
                Log.e("Notification", "Bildirim gönderme hatası: ${e.message}")
                // Hata durumunu kullanıcıya bildirme veya loglama
            }
        } else {
            Log.w("Notification", "Bildirimler devre dışı - bildirim gönderilmedi")
            // Kullanıcıyı bildirim ayarlarına yönlendirme önerebilirsiniz
            showNotificationSettingsDialog(context)
        }
    }
    
    private fun showNotificationSettingsDialog(context: Context) {
        if (context is Activity) {
            AlertDialog.Builder(context)
                .setTitle("Bildirimler Kapalı")
                .setMessage("Bu özelliği kullanmak için bildirim ayarlarından izin vermeniz gerekiyor.")
                .setPositiveButton("Ayarlara Git") { _, _ ->
                    openNotificationSettings(context)
                }
                .setNegativeButton("Tamam", null)
                .show()
        }
    }
    
    private fun openNotificationSettings(activity: Activity) {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}
```

## 3. Android 11+ MANAGE_EXTERNAL_STORAGE İzni

### 3.1 İzin Beyanı ve Implementasyonu

Android 11 (API 30) ile tanıtılan MANAGE_EXTERNAL_STORAGE izni, uygulamaların tüm dosyalara erişimini sağlar[2]. Bu izin çok güçlü olduğu için dikkatli kullanılmalıdır.

#### AndroidManifest.xml'de İzin Beyanı:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Android 11+ için tüm dosyalara erişim -->
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
        tools:ignore="ScopedStorage" />
    
    <!-- Eski Android sürümleri için -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />
        
    <application
        android:requestLegacyExternalStorage="true"
        tools:targetApi="q">
        <!-- Uygulama bileşenleri -->
    </application>
</manifest>
```

#### Kotlin/Java'da İzin İsteği:

```kotlin
class ExternalStoragePermissionManager(private val activity: AppCompatActivity) {
    
    private val manageStorageLauncher = 
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // İzin verildi
                    onStoragePermissionGranted()
                } else {
                    // İzin reddedildi
                    onStoragePermissionDenied()
                }
            }
        }
    
    private val legacyStorageLauncher = 
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
            
            if (readGranted && (writeGranted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
                onStoragePermissionGranted()
            } else {
                onStoragePermissionDenied()
            }
        }
    
    fun checkAndRequestStoragePermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ - MANAGE_EXTERNAL_STORAGE
                if (Environment.isExternalStorageManager()) {
                    onStoragePermissionGranted()
                } else {
                    requestManageStoragePermission()
                }
            }
            
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-10 - Legacy permissions
                val readPermission = ContextCompat.checkSelfPermission(
                    activity, 
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                val writePermission = ContextCompat.checkSelfPermission(
                    activity, 
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                
                if (readPermission == PackageManager.PERMISSION_GRANTED &&
                    (writePermission == PackageManager.PERMISSION_GRANTED || 
                     Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
                    onStoragePermissionGranted()
                } else {
                    requestLegacyStoragePermissions()
                }
            }
            
            else -> {
                // Android 5.x ve altı - İzin gerekmiyor
                onStoragePermissionGranted()
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestManageStoragePermission() {
        AlertDialog.Builder(activity)
            .setTitle("Dosya Erişim İzni")
            .setMessage("Bu uygulama tüm dosyalara erişim için özel izin gerektiriyor. " +
                       "Sistem ayarlarında bu izni etkinleştirmeniz gerekmektedir.")
            .setPositiveButton("Ayarlara Git") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    // Genel ayarlar sayfasına yönlendir
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                }
            }
            .setNegativeButton("İptal") { _, _ ->
                onStoragePermissionDenied()
            }
            .show()
    }
    
    private fun requestLegacyStoragePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(
                activity, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            legacyStorageLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            onStoragePermissionGranted()
        }
    }
    
    private fun onStoragePermissionGranted() {
        Toast.makeText(activity, "Depolama erişim izni verildi", Toast.LENGTH_SHORT).show()
        // Dosya işlemlerini etkinleştir
        enableFileOperations()
    }
    
    private fun onStoragePermissionDenied() {
        Toast.makeText(
            activity,
            "Depolama erişim izni olmadan bazı özellikler çalışmayabilir",
            Toast.LENGTH_LONG
        ).show()
        // Sınırlı mod etkinleştir
        enableLimitedMode()
    }
    
    private fun enableFileOperations() {
        // Tam dosya erişim özellikleri
    }
    
    private fun enableLimitedMode() {
        // Sınırlı dosya erişim özellikleri
    }
    
    // İzin durumu kontrolü
    fun hasStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> true
        }
    }
}
```

### 3.2 Scoped Storage ile Çalışma

Android 11+ için scoped storage kullanımı[7]:

```kotlin
class ScopedStorageManager(private val context: Context) {
    
    // Medya dosyası kaydetme (izin gerektirmez)
    fun saveImageToGallery(bitmap: Bitmap, displayName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        return uri?.let { imageUri ->
            try {
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                imageUri
            } catch (e: IOException) {
                // Hata durumunda URI'yi sil
                resolver.delete(imageUri, null, null)
                null
            }
        }
    }
    
    // Uygulama-özel dizinde dosya kaydetme (izin gerektirmez)
    fun saveFileToAppDirectory(fileName: String, content: String): File? {
        return try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            file.writeText(content)
            file
        } catch (e: IOException) {
            null
        }
    }
    
    // Storage Access Framework ile dosya seçme
    fun createFilePickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
    }
    
    // Storage Access Framework ile dosya kaydetme
    fun createFileSaveIntent(fileName: String, mimeType: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
    }
}
```

## 4. Ses Çalma Sistemleri

### 4.1 MediaPlayer vs SoundPool Karşılaştırması

| Özellik | MediaPlayer | SoundPool |
|---------|-------------|-----------|
| Kullanım Alanı | Uzun ses dosyaları (müzik, podcast) | Kısa ses efektleri (game sounds, UI sounds) |
| Bellek Kullanımı | Streaming (az bellek) | RAM'e yükleme (fazla bellek) |
| Gecikme | Yüksek gecikme | Düşük gecikme |
| Eşzamanlı Çalma | Tek ses | Çoklu ses |
| Codec Desteği | Geniş codec desteği | Sınırlı codec desteği |
| Lifecycle Yönetimi | Karmaşık | Basit |

### 4.2 MediaPlayer Implementasyonu

Jetpack Media3 kullanımı önerilmektedir[5], ancak MediaPlayer için best practices[9]:

```kotlin
class MediaPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // Audio Focus yönetimi için listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Odak kazanıldı - normal ses seviyesinde devam et
                mediaPlayer?.setVolume(1.0f, 1.0f)
                if (!mediaPlayer?.isPlaying!!) {
                    mediaPlayer?.start()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Kalıcı odak kaybı - oynatmayı durdur
                stopPlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Geçici odak kaybı - duraklat
                mediaPlayer?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Geçici odak kaybı - sesi kıs
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
        }
    }
    
    fun playAudio(assetFileName: String) {
        // Önce audio focus iste
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
                
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
                
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            try {
                // Mevcut MediaPlayer'ı temizle
                releaseMediaPlayer()
                
                // Yeni MediaPlayer oluştur
                mediaPlayer = MediaPlayer().apply {
                    // Asset dosyasını yükle
                    val assetFileDescriptor = context.assets.openFd(assetFileName)
                    setDataSource(
                        assetFileDescriptor.fileDescriptor,
                        assetFileDescriptor.startOffset,
                        assetFileDescriptor.length
                    )
                    assetFileDescriptor.close()
                    
                    // Listener'ları ayarla
                    setOnPreparedListener { player ->
                        player.start()
                    }
                    
                    setOnCompletionListener {
                        // Oynatma tamamlandığında audio focus'u bırak
                        abandonAudioFocus()
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        Log.e("MediaPlayer", "Error: what=$what extra=$extra")
                        releaseMediaPlayer()
                        abandonAudioFocus()
                        true
                    }
                    
                    // Asenkron hazırla
                    prepareAsync()
                }
            } catch (e: IOException) {
                Log.e("MediaPlayer", "Ses dosyası yüklenirken hata: ${e.message}")
                abandonAudioFocus()
            }
        }
    }
    
    fun pausePlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
    }
    
    fun resumePlayback() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
            }
        }
    }
    
    fun stopPlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
        }
        releaseMediaPlayer()
        abandonAudioFocus()
    }
    
    private fun releaseMediaPlayer() {
        mediaPlayer?.let { player ->
            try {
                player.release()
            } catch (e: Exception) {
                Log.e("MediaPlayer", "Release hatası: ${e.message}")
            }
        }
        mediaPlayer = null
    }
    
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // AudioFocusRequest ile bırak (modern API)
            // Bu örnekte basitlik için deprecated API kullanılıyor
        }
        
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(audioFocusChangeListener)
    }
    
    fun onDestroy() {
        stopPlayback()
    }
}
```

### 4.3 SoundPool Implementasyonu

```kotlin
class SoundPoolManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<String, Int>()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var currentStreamId = 0
    
    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
                
            SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            SoundPool(10, AudioManager.STREAM_MUSIC, 0)
        }
        
        soundPool?.setOnLoadCompleteListener { soundPool, sampleId, status ->
            if (status == 0) {
                Log.d("SoundPool", "Ses yüklendi: sampleId=$sampleId")
            } else {
                Log.e("SoundPool", "Ses yüklenirken hata: status=$status")
            }
        }
    }
    
    fun loadSound(soundName: String, assetPath: String): Boolean {
        return try {
            val assetFileDescriptor = context.assets.openFd(assetPath)
            val soundId = soundPool?.load(assetFileDescriptor, 1) ?: -1
            
            if (soundId != -1) {
                soundMap[soundName] = soundId
                assetFileDescriptor.close()
                true
            } else {
                assetFileDescriptor.close()
                false
            }
        } catch (e: IOException) {
            Log.e("SoundPool", "Ses yüklenirken hata: ${e.message}")
            false
        }
    }
    
    fun playSound(soundName: String, volume: Float = 1.0f, loop: Boolean = false): Int {
        val soundId = soundMap[soundName] ?: return -1
        
        return soundPool?.let { pool ->
            val streamId = pool.play(
                soundId,
                volume, volume,
                1, // priority
                if (loop) -1 else 0, // loop: -1 = infinite, 0 = no loop
                1.0f // rate
            ) ?: -1
            
            if (streamId != -1) {
                currentStreamId = streamId
            }
            streamId
        } ?: -1
    }
    
    fun stopSound(streamId: Int) {
        soundPool?.stop(streamId)
    }
    
    fun stopAllSounds() {
        soundMap.values.forEach { soundId ->
            soundPool?.stop(soundId)
        }
    }
    
    fun pauseSound(streamId: Int) {
        soundPool?.pause(streamId)
    }
    
    fun resumeSound(streamId: Int) {
        soundPool?.resume(streamId)
    }
    
    fun setVolume(streamId: Int, volume: Float) {
        soundPool?.setVolume(streamId, volume, volume)
    }
    
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}

// Kullanım örneği
class GameActivity : AppCompatActivity() {
    private lateinit var soundPoolManager: SoundPoolManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        soundPoolManager = SoundPoolManager(this)
        
        // Sesleri yükle
        soundPoolManager.loadSound("button_click", "sounds/button_click.ogg")
        soundPoolManager.loadSound("game_over", "sounds/game_over.wav")
        soundPoolManager.loadSound("background_music", "sounds/bg_music.mp3")
    }
    
    private fun playButtonClickSound() {
        soundPoolManager.playSound("button_click", 0.8f)
    }
    
    private fun playGameOverSound() {
        soundPoolManager.playSound("game_over", 1.0f)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        soundPoolManager.release()
    }
}
```

## 5. Audio Focus Yönetimi

### 5.1 Audio Focus Best Practices

Audio Focus, birden fazla uygulamanın aynı anda ses çalmasını engellemek için kritik bir mekanizmadır[6].

```kotlin
class AudioFocusManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    
    // Audio Focus değişiklik listener'ı
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Ses odağı kazanıldı
                hasAudioFocus = true
                onAudioFocusGained()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Kalıcı ses odağı kaybı
                hasAudioFocus = false
                onAudioFocusLost(permanent = true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Geçici ses odağı kaybı
                hasAudioFocus = false
                onAudioFocusLost(permanent = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Geçici ses odağı kaybı - ducking mümkün
                onAudioFocusDuck()
            }
        }
    }
    
    fun requestAudioFocus(
        usage: Int = AudioAttributes.USAGE_MEDIA,
        contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC,
        focusGain: Int = AudioManager.AUDIOFOCUS_GAIN
    ): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestAudioFocusModern(usage, contentType, focusGain)
        } else {
            requestAudioFocusLegacy(focusGain)
        }
        
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocusModern(usage: Int, contentType: Int, focusGain: Int): Int {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(contentType)
            .build()
            
        audioFocusRequest = AudioFocusRequest.Builder(focusGain)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .setAcceptsDelayedFocusGain(true) // Gecikmeli odak kazanmayı kabul et
            .setWillPauseWhenDucked(false) // Otomatik ducking kullan
            .build()
            
        return audioManager.requestAudioFocus(audioFocusRequest!!)
    }
    
    @Suppress("DEPRECATION")
    private fun requestAudioFocusLegacy(focusGain: Int): Int {
        return audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            focusGain
        )
    }
    
    fun abandonAudioFocus() {
        if (hasAudioFocus) {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
            
            hasAudioFocus = false
            audioFocusRequest = null
        }
    }
    
    // Callback fonksiyonları - override edilmeli
    open fun onAudioFocusGained() {
        // Ses odağı kazanıldığında yapılacak işlemler
        // Örn: Oynatmayı başlat/devam ettir, sesi normal seviyeye çıkar
    }
    
    open fun onAudioFocusLost(permanent: Boolean) {
        // Ses odağı kaybedildiğinde yapılacak işlemler
        if (permanent) {
            // Kalıcı kayıp - oynatmayı durdur
        } else {
            // Geçici kayıp - duraklat
        }
    }
    
    open fun onAudioFocusDuck() {
        // Ducking durumunda yapılacak işlemler
        // Otomatik ducking kullanılmıyorsa sesi manuel olarak kıs
    }
    
    fun hasAudioFocus(): Boolean = hasAudioFocus
}

// MediaPlayer ile AudioFocus entegrasyonu
class AudioFocusAwareMediaPlayer(context: Context) : AudioFocusManager(context) {
    private var mediaPlayer: MediaPlayer? = null
    private var wasPlayingBeforeFocusLoss = false
    private var volumeBeforeDuck = 1.0f
    
    override fun onAudioFocusGained() {
        mediaPlayer?.let { player ->
            // Sesi normal seviyeye çıkar
            player.setVolume(1.0f, 1.0f)
            
            // Eğer focus kaybından önce çalıyorsa devam ettir
            if (wasPlayingBeforeFocusLoss) {
                player.start()
                wasPlayingBeforeFocusLoss = false
            }
        }
    }
    
    override fun onAudioFocusLost(permanent: Boolean) {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                wasPlayingBeforeFocusLoss = true
                if (permanent) {
                    // Kalıcı kayıp - durdur
                    player.stop()
                    wasPlayingBeforeFocusLoss = false
                } else {
                    // Geçici kayıp - duraklat
                    player.pause()
                }
            }
        }
    }
    
    override fun onAudioFocusDuck() {
        mediaPlayer?.let { player ->
            // Sesi %30'a düşür (ducking)
            volumeBeforeDuck = 1.0f
            player.setVolume(0.3f, 0.3f)
        }
    }
}
```

## 6. Diğer Gerekli İzinler

### 6.1 RECORD_AUDIO İzni

Mikrofon erişimi için gerekli olan RECORD_AUDIO izni[8]:

```kotlin
class AudioRecordPermissionManager(private val activity: AppCompatActivity) {
    
    private val requestPermissionLauncher = 
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onRecordAudioPermissionGranted()
            } else {
                onRecordAudioPermissionDenied()
            }
        }
    
    fun checkAndRequestRecordAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                onRecordAudioPermissionGranted()
            }
            
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) -> {
                showPermissionRationale()
            }
            
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun showPermissionRationale() {
        AlertDialog.Builder(activity)
            .setTitle("Mikrofon İzni Gerekli")
            .setMessage("Bu özellik için mikrofonunuza erişim izni gerekiyor. " +
                       "Ses kaydetmek ve sesli komutları kullanmak için bu izni verin.")
            .setPositiveButton("İzin Ver") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
                onRecordAudioPermissionDenied()
            }
            .show()
    }
    
    private fun onRecordAudioPermissionGranted() {
        // Mikrofon özelliklerini etkinleştir
        Toast.makeText(activity, "Mikrofon izni verildi", Toast.LENGTH_SHORT).show()
        enableMicrophoneFeatures()
    }
    
    private fun onRecordAudioPermissionDenied() {
        // Mikrofon özelliklerini devre dışı bırak
        Toast.makeText(
            activity,
            "Mikrofon izni olmadan ses kayıt özellikleri çalışmayacak",
            Toast.LENGTH_LONG
        ).show()
        disableMicrophoneFeatures()
    }
    
    private fun enableMicrophoneFeatures() {
        // Ses kayıt butonlarını aktif et
        // AudioRecord veya MediaRecorder başlat
    }
    
    private fun disableMicrophoneFeatures() {
        // Ses kayıt butonlarını devre dışı bırak
        // Alternatif özellikler öner
    }
}
```

### 6.2 VIBRATE İzni

Titreşim özelliği için gerekli olan izin:

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.VIBRATE" />
```

```kotlin
class VibrationManager(private val context: Context) {
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    fun vibrateShort() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }
    
    fun vibrateLong() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }
    
    fun vibratePattern() {
        val pattern = longArrayOf(0, 250, 250, 250) // Başla, titreş, dur, titreş
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
```

### 6.3 WAKE_LOCK İzni

Cihazın uyku moduna girmesini engellemek için:

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

```kotlin
class WakeLockManager(private val context: Context) {
    private var wakeLock: PowerManager.WakeLock? = null
    
    fun acquireWakeLock(tag: String = "MyApp:WakeLock") {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            tag
        ).apply {
            acquire(10*60*1000L /*10 minutes*/)
        }
    }
    
    fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        wakeLock = null
    }
}
```

## 7. Test ve Hata Yönetimi

### 7.1 İzin Durumları için Test Senaryoları

```kotlin
class PermissionTestHelper {
    
    // ADB komutları ile test durumları oluşturma
    fun printTestCommands(packageName: String) {
        println("=== Android 13+ POST_NOTIFICATIONS Test Komutları ===")
        println("Yeni yükleme simülasyonu:")
        println("adb shell pm revoke $packageName android.permission.POST_NOTIFICATIONS")
        println("adb shell pm clear-permission-flags $packageName android.permission.POST_NOTIFICATIONS user-set")
        println()
        
        println("Upgrade simülasyonu (izin verilmiş):")
        println("adb shell pm grant $packageName android.permission.POST_NOTIFICATIONS")
        println("adb shell pm set-permission-flags $packageName android.permission.POST_NOTIFICATIONS user-set")
        println()
        
        println("=== MANAGE_EXTERNAL_STORAGE Test Komutları ===")
        println("İzin verme:")
        println("adb shell appops set --uid $packageName MANAGE_EXTERNAL_STORAGE allow")
        println("İzin iptal etme:")
        println("adb shell appops set --uid $packageName MANAGE_EXTERNAL_STORAGE deny")
    }
    
    fun logCurrentPermissionStates(context: Context) {
        Log.d("PermissionTest", "=== Mevcut İzin Durumları ===")
        
        // POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPostNotifications = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            Log.d("PermissionTest", "POST_NOTIFICATIONS: $hasPostNotifications")
        }
        
        // MANAGE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasManageStorage = Environment.isExternalStorageManager()
            Log.d("PermissionTest", "MANAGE_EXTERNAL_STORAGE: $hasManageStorage")
        }
        
        // RECORD_AUDIO
        val hasRecordAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        Log.d("PermissionTest", "RECORD_AUDIO: $hasRecordAudio")
        
        // Bildirim durumu
        val notificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        Log.d("PermissionTest", "Notifications Enabled: $notificationEnabled")
    }
}
```

### 7.2 Hata Yönetimi Best Practices

```kotlin
class ErrorHandlingManager {
    
    fun handleMediaPlayerError(what: Int, extra: Int): String {
        return when (what) {
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> "Bilinmeyen hata oluştu"
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "Medya sunucusu yanıt vermiyor"
            else -> "Medya hatası: what=$what, extra=$extra"
        }
    }
    
    fun handleNotificationError(e: Exception) {
        when (e) {
            is SecurityException -> {
                Log.e("Notification", "İzin hatası: ${e.message}")
                // Kullanıcıyı ayarlara yönlendir
            }
            is IllegalArgumentException -> {
                Log.e("Notification", "Geçersiz parametre: ${e.message}")
                // Hatalı bildirim parametrelerini düzelt
            }
            else -> {
                Log.e("Notification", "Bildirim hatası: ${e.message}")
            }
        }
    }
    
    fun handleAudioFocusError(result: Int): String {
        return when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> "Audio focus isteği başarısız"
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> "Audio focus gecikmeli olarak verilecek"
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> "Audio focus başarıyla verildi"
            else -> "Bilinmeyen audio focus durumu: $result"
        }
    }
}
```

## 8. Sonuç ve Öneriler

### 8.1 Temel Prensipler

1. **İzinleri Bağlam İçinde İsteyin**: Kullanıcının neden bu izne ihtiyacınız olduğunu açıklayın[1,4]
2. **Graceful Degradation Uygulayın**: İzin verilmezse uygulamanın kırılmaması için alternatif yollar sunun[4]
3. **Audio Focus'u Her Zaman Yönetin**: Birden fazla ses uygulaması arasında uyumlu davranış için kritiktir[6]
4. **Modern API'leri Tercih Edin**: Media3 ve AudioFocusRequest gibi yeni API'leri kullanın[5,6]
5. **Test Senaryolarını Kapsamlı Tutun**: Farklı Android sürümleri ve izin durumları için test yapın

### 8.2 Sürüm Bazında Önemli Noktalar

- **Android 13+ (API 33)**: POST_NOTIFICATIONS iznini mutlaka handle edin[1]
- **Android 11+ (API 30)**: MANAGE_EXTERNAL_STORAGE kullanımını Google Play politikalarına uygun tutun[2]
- **Android 8.0+ (API 26)**: NotificationChannel kullanımı zorunludur[3]
- **Audio Focus**: Android 8.0+ için AudioFocusRequest, öncesi için legacy API kullanın[6]

### 8.3 Performans Optimizasyonları

- MediaPlayer için tek instance kullanıp reset() ile yeniden kullanın[9]
- SoundPool için ses sayısını makul sınırlarda tutun
- Audio focus'u gereksiz yere tutmayın, kullanım bittiğinde abandon edin[6]
- Bildirim kanallarını uygulama başlangıcında bir kez oluşturun[3]

## Kaynaklar

[1] [Android 13+ POST_NOTIFICATIONS İzni](https://developer.android.com/develop/ui/views/notifications/notification-permission) - Yüksek Güvenilirlik - Resmi Android dokümantasyonu
[2] [Android 11+ MANAGE_EXTERNAL_STORAGE İzni](https://developer.android.com/training/data-storage/manage-all-files) - Yüksek Güvenilirlik - Resmi Android dokümantasyonu  
[3] [NotificationCompat Kılavuzu](https://developer.android.com/develop/ui/views/notifications/build-notification) - Yüksek Güvenilirlik - Resmi Android dokümantasyonu
[4] [Runtime Permissions Kılavuzu](https://developer.android.com/training/permissions/requesting) - Yüksek Güvenilirlik - Resmi Android dokümantasyonu
[5] [MediaPlayer API](https://developer.android.com/media/platform/mediaplayer) - Yüksek Güvenilirlik - Resmi Android dokümantasyonu
[6] [Audio Focus Yönetimi](https://developer.android.com/media/optimize/audio-focus) - Yüksek Güvenilirlik - Resmi Android dokümantasyonu
[7] [Android 11 Depolama Güncellemeleri](https://developer.android.com/about/versions/11/privacy/storage) - Yüksek Güvenilirlik - Resmi Android dokümantasyonu
[8] [RECORD_AUDIO İzni](https://developer.android.com/training/permissions/explaining-access) - Yüksek Güvenilirlik - Resmi Android dokümantasyonu
[9] [MediaPlayer Best Practices](https://medium.com/androiddevelopers/deep-dive-mediaplayer-best-practices-feb4d15a66f5) - Yüksek Güvenilirlik - Android Developers Medium
