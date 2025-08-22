# Android Hareket Algılama Uygulaması - Kurulum ve Test Rehberi

## 🚀 Hızlı Başlangıç

### Ön Koşullar
1. **Android Studio** (Flamingo 2022.2.1 veya daha yeni)
2. **Android SDK 34**
3. **JDK 8 veya üzeri**
4. **Android cihaz** (API 24+ / Android 7.0+)

### Adım 1: Projeyi Açın
1. Android Studio'yu başlatın
2. **File > Open** menüsünden `android_motion_detector` klasörünü seçin
3. Gradle sync işlemini bekleyin (ilk seferde 5-10 dakika sürebilir)

### Adım 2: Ses Dosyası Ekleyin

**Gerekli**: `app/src/main/res/raw/alert_sound.mp3` dosyasını eklemeniz gerekiyor.

**Önerilen ses dosyaları**:
- Freesound.org'dan ücretsiz alarm sesleri
- Android Studio'nun built-in ses efektleri
- Kısa (1-3 saniye) MP3 veya WAV formatı

```bash
# Ses dosyasını doğru konuma kopyalayın
cp your_alarm_sound.mp3 app/src/main/res/raw/alert_sound.mp3
```

### Adım 3: Çalıştırın
1. Android cihazınızı USB ile bağlayın
2. **Developer Options** ve **USB Debugging**'i etkinleştirin

3. Android Studio'da **Run > Run 'app'** butonuna basın
4. Cihazınızı seçin ve çalıştırın

## 📱 İlk Kullanım

### Uygulama Açılışı

1. **Uygulama açıldığında**, şu izinleri isteyecek:
   - 📷 **Kamera**: Hareket algılama için gerekli
   - 🔔 **Bildirim**: Uyarılar için (Android 13+)
   - 💾 **Depolama**: Ekran görüntüleri için
   - 🔊 **Ses**: Sesli uyarılar için

2. **Tüm izinleri "Allow" olarak ayarlayın**

### Temel Kullanım Testi

1. **Hassasiyet Ayarı**:
   - Alt kısımdaki kaydırıcıyı %70'e ayarlayın
2. **Başlatma**:
   - 🔴 **"Başlat"** butonuna basın
3. **Test Etme**:
   - Kamera önünde elinizi hareket ettirin
   - Sesli uyarı duymalısınız
   - Bildirim gelmelidir
   - "Son hareket" zamanı güncellenmelidir

## 🔍 Test Senaryoları

### 1. Temel Hareket Testi
```
✅ Kameranın önünde el sallama
✅ Nesneleri hareket ettirme
✅ Kişi hareketi (yürüme)
```

### 2. Renk Değişimi Testi
```
✅ Renkli kağıt gösterme
✅ Işık açma/kapama
✅ Farklı renk nesneler
```

### 3. Hassasiyet Testi
```
✅ %10 - Sadece büyük hareketler
✅ %50 - Orta hareketler
✅ %90 - Küçük hareketler
```

### 4. Ekran Görüntüsü Testi

1. **MediaProjection İzni**:
   - 📸 **"Ekran Görüntüsü"** butonuna basın
   - Sistem üzerinden "Share screen" iznini verin

2. **Otomatik Yakalama**:
   - Hareket algılandığında otomatik ekran görüntüsü alınmalı
   - **Pictures/MotionCapture/** klasöründe dosya oluşmalı

### 5. Arka Plan Testi

1. **Ayarlar > Arka Plan Modu**'nu etkinleştirin
2. Ana ekrana geri dönün
3. Hareket algılama devam etmeli
4. Bildirim çubuğunda "Hareket Algılama Aktif" görünmeli

## 🔧 Sorun Giderme

### Yaygın Sorunlar ve Çözümleri

#### 📷 Kamera Açılmıyor
```
Sorun: "Camera binding failed" hatası
Cözüm: 
1. Kamera iznini kontrol edin
2. Başka uygulamaların kamerarı kullanıp kullanmadığını kontrol edin
3. Cihazı yeniden başlatın
```

#### 🔊 Ses Çalmıyor
```
Sorun: Sesli uyarı duyulmuyor
Cözüm:
1. alert_sound.mp3 dosyasının doğru konumda olduğunu kontrol edin
2. Cihaz ses seviyesini kontrol edin
3. Sessiz mod kapalı olduğundan emin olun
```

#### 🔔 Bildirim Gelmiyor
```
Sorun: Push notification görünmüyor
Cözüm:
1. Uygulama bildirim izinlerini kontrol edin
2. Android 13+ için POST_NOTIFICATIONS iznini onaylayın
3. Cihaz bildirimleri aktif olduğundan emin olun
```

#### 📸 Ekran Görüntüsü Alınamıyor
```
Sorun: Screenshot kaydedilmiyor
Cözüm:
1. MediaProjection iznini verin
2. Depolama iznini kontrol edin
3. Pictures/MotionCapture klasörünün oluştuğunu kontrol edin
```

#### 🚀 OpenCV Yüklenmiyor
```
Sorun: "OpenCV load failed" hatası
Cözüm:
1. Gradle sync'i tekrar çalıştırın
2. Build > Clean Project yapın
3. Build > Rebuild Project yapın
```

### Debug Modu

Hata ayıklama için Android Studio Logcat'ı kullanın:

1. **View > Tool Windows > Logcat**
2. **Tag filter**: `MotionDetector`, `MainActivity`, `ScreenCapture`
3. **Log Level**: `Debug` seçin

Önemli log mesajları:
```
[MainActivity] Camera started successfully
[MotionDetector] Motion detected - Motion: true, Color: false
[ScreenCapture] Screenshot saved: /storage/emulated/0/Pictures/MotionCapture/motion_20250822_143052.png
[NotificationHelper] Motion detection notification shown
```

## 📊 Performans Optimizasyonu

### Batarya Tasarrufu
```
✅ Frame skipping (her 3. frame işlenir)
✅ Background thread kullanımı
✅ Gereksiz OpenCV işlemlerinden kaçınma
✅ Memory leak prevention
```

### Bellek Yönetimi
```
✅ Mat nesnelerini release() ile temizleme
✅ ImageProxy'leri close() ile kapatıma
✅ Bitmap'leri recycle() ile temizleme
```

## 📅 İleri Testler

### 1. Stress Test
- 30 dakika sürekli çalıştırma
- Bellek kullanımı izleme
- Batarya tüketimi ölçüme

### 2. Multi-Cihaz Test
- Farklı Android sürümleri
- Farklı ekran boyutları
- Farklı kamera kaliteleri

### 3. Edge Case Test
- Çok düşük ışık
- Çok parlak ışık
- Hızlı hareketler
- Yúvaş hareketler

## 📝 Rapor Hazırlama

Test sonuçlarınızı şu formatta rapor edin:

```
📱 Cihaz: [Model, Android Version]
⚙️ Test Sürümü: [App Version]
🕰️ Test Süresi: [Duration]
✅ Başarılı Testler: [Count]
❌ Başarısız Testler: [Count]
🐛 Bulunan Hatalar: [Description]
💡 Öneriler: [Suggestions]
```

---

ℹ️ **Ek Bilgi**: Test sırasında sorun yaşarsanz, logcat çıktılarını kaydedin ve gerektiğinde paylaşın.