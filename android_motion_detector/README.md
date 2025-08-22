# Android Hareket Algılama Uygulaması

## 🎯 Genel Bakış

Bu uygulama, OpenCV kullanarak gerçek zamanlı hareket algılama yapabilen kapsamlı bir Android uygulamasıdır. Kullanıcı tarafından ayarlanabilir hassasiyetle hareket algılar, sesli uyarı verir, bildirim gönderir ve ekran görüntüsü alır.

## ✨ Ana Özellikler

### 🔍 Hareket Algılama
- **Frame Differencing**: Ardışık video karelerindeki farkları analiz eder
- **Background Subtraction**: Arka plan değişikliklerini tespit eder
- **HSV Renk Uzayı**: Renk bazlı değişiklikleri algılar
- **Ayarlanabilir Hassasiyet**: 1-100 arası hassasiyet kontrolü

### 🔔 Uyarı Sistemleri
- **Sesli Uyarı**: MediaPlayer/SoundPool ile özelleştirilebilir alarm sesleri
- **Push Bildirim**: Android 13+ uyumlu bildirim sistemi
- **Titreşim**: Haptik geri bildirim
- **Görsel Uyarı**: Ekranda anlık durum gösterimi

### 📸 Ekran Görüntüsü
- **MediaProjection API**: Android 14+ uyumlu ekran yakalama
- **Otomatik Kaydetme**: `/Pictures/MotionCapture/` dizinine kayıt
- **Dosya Adlandırma**: `motion_YYYYMMDD_HHMMSS.png` formatı
- **Scoped Storage**: Android 11+ depolama uyumluluğu

### 🏃‍♂️ Arka Plan İşlemi
- **Foreground Service**: Sürekli hareket izleme
- **Battery Optimization**: Optimize edilmiş güç tüketimi
- **Lifecycle Aware**: Uygulama yaşam döngüsü uyumluluğu

## 🛠️ Teknik Detaylar

### Kullanılan Teknolojiler
- **Kotlin**: Ana geliştirme dili
- **OpenCV 4.10.0**: Bilgisayarlı görü işlemleri
- **CameraX 1.3.1**: Modern kamera API
- **Material Design 3**: UI/UX tasarımı
- **AndroidX**: Jetpack kütüphaneleri

### Minimum Gereksinimler
- **Android API 24+** (Android 7.0)
- **ARM64/ARMv7** processor
- **Kamera** donanımı
- **2GB RAM** (önerilen)

### İzinler
```xml
<!-- Gerekli İzinler -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

## 🚀 Kurulum ve Çalıştırma

### Ön Koşullar
1. **Android Studio** (Arctic Fox ve üzeri)
2. **Android SDK 34**
3. **Gradle 8.2+**
4. **JDK 8+**

### Adım Adım Kurulum

1. **Projeyi İndirin**
   ```bash
   # Proje dosyalarını yerel dizine kopyalayın
   ```

2. **Android Studio'da Açın**
   - File > Open > Proje klasörünü seçin
   - Gradle sync'i bekleyin

3. **Ses Dosyası Ekleyin**
   ```
   app/src/main/res/raw/alert_sound.mp3
   ```
   - Ses dosyasını bu konuma ekleyin
   - Freesound.org'dan ücretsiz alarm sesleri indirebilirsiniz

4. **Çalıştırın**
   - Android cihaz veya emülatör bağlayın
   - Run > Run 'app'

## 📱 Kullanım Kılavuzu

### İlk Kurulum
1. Uygulamayı başlatın
2. Gerekli izinleri verin:
   - Kamera erişimi
   - Bildirim izni
   - Depolama erişimi
3. Hassasiyet seviyesini ayarlayın

### Temel Kullanım
1. **Başlat** butonuna basın
2. Kamera önizlemesi başlayacak
3. Hareket algılandığında:
   - Sesli uyarı çalacak
   - Bildirim gelecek
   - Ekran görüntüsü alınacak (etkinse)

### Gelişmiş Ayarlar
- **Menü > Ayarlar**'dan erişilebilir:
  - Sesli uyarı açma/kapama
  - Bildirim kontrolü
  - Ekran görüntüsü ayarları
  - Arka plan modu

## 🔧 Geliştirici Notları

### Önemli Sınıflar

```kotlin
// Ana hareket algılama motoru
MotionDetector.kt

// Kamera ve OpenCV entegrasyonu  
MainActivity.kt

// Arka plan servisi
MotionDetectionService.kt

// Ekran görüntüsü alma
ScreenCaptureService.kt
```

### Performans Optimizasyonları
- Frame skipping (her 3. frame işlenir)
- Background thread processing
- Memory leak prevention
- Battery optimization

### Hata Yönetimi
- OpenCV yükleme kontrolü
- Kamera erişim hataları
- İzin reddi durumları
- Bellek yetersizliği

## 🐛 Sorun Giderme

### Yaygın Sorunlar

**1. OpenCV Yüklenmiyor**
```
Çözüm: OpenCVLoader.initDebug() kontrolü yapın
```

**2. Kamera Başlamıyor**
```
Çözüm: CAMERA izni kontrol edin
```

**3. Ekran Görüntüsü Alınamıyor**
```
Çözüm: MediaProjection izni verin
```

**4. Bildirim Gelmiyor**
```
Çözüm: POST_NOTIFICATIONS izni kontrol edin (Android 13+)
```

### Debug Modu
```kotlin
// Log seviyesini artırın
Log.d(TAG, "Debug message")
```

## 📋 Yapılacaklar (TODO)

- [ ] **Room Database**: Hareket logları kaydetme
- [ ] **Cloud Backup**: Görüntüleri bulut depolamaya yükleme
- [ ] **AI Enhancement**: Makine öğrenmesi ile gelişmiş algılama
- [ ] **Multi-Camera**: Birden fazla kamera desteği
- [ ] **Real-time Streaming**: Canlı video akışı
- [ ] **Web Interface**: Web tabanlı uzaktan kontrol

## 📄 Lisans

Bu proje MiniMax Agent tarafından geliştirilmiştir.

## 🆘 Destek

Sorularınız için:
- GitHub Issues
- E-posta: [destek@minimax.com]

---

**Not**: Bu uygulama eğitim amaçlı geliştirilmiştir. Ticari kullanım için uygun lisansları kontrol edin.
