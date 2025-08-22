package com.minimax.motiondetector.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.minimax.motiondetector.R

class SoundManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SoundManager"
        private const val MAX_STREAMS = 1
    }
    
    private var soundPool: SoundPool? = null
    private var alertSoundId: Int = 0
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(audioAttributes)
                .build()
            
            // Varsayılan alarm sesi yükle (ses dosyası yoksa 0 olacak)
            try {
                alertSoundId = soundPool?.load(context, R.raw.alert_sound, 1) ?: 0
            } catch (e: Exception) {
                Log.w(TAG, "Custom alert sound not found, will use system sound")
                alertSoundId = 0
            }
            
            Log.d(TAG, "SoundPool initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SoundPool", e)
        }
    }
    
    fun playAlertSound() {
        try {
            soundPool?.let { pool ->
                if (alertSoundId != 0) {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    val volume = currentVolume.toFloat() / maxVolume.toFloat()
                    
                    pool.play(alertSoundId, volume, volume, 1, 0, 1.0f)
                    Log.d(TAG, "Alert sound played")
                } else {
                    // SoundPool başarısız olursa MediaPlayer kullan
                    playAlertSoundWithMediaPlayer()
                }
            } ?: run {
                playAlertSoundWithMediaPlayer()
            }
            
            // Titreşim ekle
            vibrateDevice()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alert sound", e)
            // Fallback olarak sistem sesi çal
            playSystemAlertSound()
        }
    }
    
    private fun playAlertSoundWithMediaPlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, R.raw.alert_sound)
            mediaPlayer?.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer?.start()
            Log.d(TAG, "Alert sound played with MediaPlayer")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound with MediaPlayer", e)
            playSystemAlertSound()
        }
    }
    
    private fun playSystemAlertSound() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
            Log.d(TAG, "System alert sound played")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing system alert sound", e)
        }
    }
    
    private fun vibrateDevice() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                    vib.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(500)
                }
                Log.d(TAG, "Device vibrated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating device", e)
        }
    }
    
    fun setVolume(volume: Float) {
        // Ses seviyesi ayarlama (0.0f - 1.0f arası)
        val clampedVolume = volume.coerceIn(0.0f, 1.0f)
        Log.d(TAG, "Volume set to: $clampedVolume")
    }
    
    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            mediaPlayer?.release()
            mediaPlayer = null
            Log.d(TAG, "SoundManager resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing SoundManager resources", e)
        }
    }
}