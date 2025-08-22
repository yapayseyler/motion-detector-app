# Android Hareket Algılama - APK Hazırlama Rehberi

## 🚀 Hızlı APK Oluşturma (5 Dakika)

### Adım 1: Android Command Line Tools
```bash
# 1. Android SDK Command Line Tools indir
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip

# 2. Çıkar ve kur
unzip commandlinetools-linux-*_latest.zip
export ANDROID_HOME=$PWD/cmdline-tools
export PATH=$ANDROID_HOME/bin:$PATH

# 3. Platform tools yükle
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

### Adım 2: Java Kurulumu
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install openjdk-11-jdk

# macOS
brew install openjdk@11

# Windows - Oracle JDK veya OpenJDK indir
```

### Adım 3: APK Build
```bash
cd android_motion_detector

# Debug APK oluştur
./gradlew assembleDebug

# APK dosyası konumu:
# app/build/outputs/apk/debug/app-debug.apk
```

### Adım 4: Telefona Yükle
```bash
# USB Debugging etkin Android cihaz bağla
adb install app/build/outputs/apk/debug/app-debug.apk

# Veya APK dosyasını telefona kopyala ve manuel yükle
```

## 📱 Manuel Yükleme (Kolay)

### 1. APK Dosyasını Al
- Build işlemi sonrası `app-debug.apk` dosyası
- Google Drive, WeTransfer ile paylaş

### 2. Telefonda Ayarlar
```
Ayarlar > Güvenlik > Bilinmeyen Kaynaklardan Yükleme
(veya)
Ayarlar > Uygulamalar > Özel Erişim > Bilinmeyen Kaynaklardan Yükleme
```

### 3. APK'yı Aç
- Dosya yöneticisi ile APK'ya dokun
- "Yükle" butonuna bas
- İzinleri onayla

## 🅰️ Alternatif Çözümler

### GitHub Actions (Otomatik)
1. Proje kodlarını GitHub'a yükle
2. Actions workflow ekle
3. Her commit'te otomatik APK oluşsun
4. Releases bölümünden indir

### Online Build Platform
- **Appetize.io**: Tarayıcıda Android emülatör
- **Replit**: Online Android geliştirme
- **CodeSandbox**: Cloud geliştirme ortamı

### Android Studio (En Kolay)
1. Android Studio'yu aç
2. Projeyi içe aktar
3. Build > Generate Signed Bundle / APK
4. APK seç > Create

## ⚡ Hata Giderme

### Java Versiyonu
```bash
# Java 8+ gerekli
java -version
# java version "11.0.x" görmeli
```

### Android SDK
```bash
# SDK yüklenmemişse
echo $ANDROID_HOME
# Boş değilse tamam
```

### Gradle Build Hatası
```bash
# Cache temizle
./gradlew clean

# Tekrar build et
./gradlew assembleDebug --stacktrace
```

### APK Yükleme Hatası
```
1. Bilinmeyen kaynaklar izni ver
2. Cihaz yöneticisi ayarlarını kontrol et
3. Eski sürümü varsa kaldır
```

## 🗏️ APK Boyutu

**Beklenen boyut**: ~45-60 MB
- OpenCV library: ~30 MB
- CameraX: ~8 MB  
- Uygulama kodu: ~5 MB
- Assets: ~2 MB

## 🔒 Güvenlik

**Debug APK**: Geliştirme amaçlı
- Google Play'e yüklenemez
- Güvenlik imzası yok
- Sadece test için kullanın

**Release APK**: Üretim için
```bash
# Release APK oluştur
./gradlew assembleRelease

# İmzalama gerekir (keystore)
keytool -genkey -v -keystore my-release-key.keystore
```

---

ℹ️ **Not**: Talimatları izleyerek 5-10 dakikada APK hazır olur. Android Studio varsa daha da kolay!