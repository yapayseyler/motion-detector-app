package com.minimax.motiondetector.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "motion_detector_prefs"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_SCREEN_CAPTURE_ENABLED = "screen_capture_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_BACKGROUND_MODE = "background_mode"
        
        private const val DEFAULT_SENSITIVITY = 50
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun setSensitivity(sensitivity: Int) {
        prefs.edit().putInt(KEY_SENSITIVITY, sensitivity).apply()
    }
    
    fun getSensitivity(defaultValue: Int = DEFAULT_SENSITIVITY): Int {
        return prefs.getInt(KEY_SENSITIVITY, defaultValue)
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
    
    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }
    
    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }
    
    fun isNotificationEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }
    
    fun setScreenCaptureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREEN_CAPTURE_ENABLED, enabled).apply()
    }
    
    fun isScreenCaptureEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCREEN_CAPTURE_ENABLED, true)
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }
    
    fun isVibrationEnabled(): Boolean {
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
    }
    
    fun setBackgroundMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_MODE, enabled).apply()
    }
    
    fun isBackgroundModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_BACKGROUND_MODE, false)
    }
}