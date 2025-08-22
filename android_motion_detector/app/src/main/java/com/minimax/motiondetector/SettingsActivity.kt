package com.minimax.motiondetector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.minimax.motiondetector.databinding.ActivitySettingsBinding
import com.minimax.motiondetector.utils.PreferencesManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        
        private lateinit var preferencesManager: PreferencesManager
        
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferencesManager = PreferencesManager(requireContext())
        }
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            
            setupPreferences()
        }
        
        private fun setupPreferences() {
            // Ses ayarı
            findPreference<SwitchPreferenceCompat>("sound_enabled")?.apply {
                isChecked = preferencesManager.isSoundEnabled()
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.setSoundEnabled(newValue as Boolean)
                    true
                }
            }
            
            // Bildirim ayarı
            findPreference<SwitchPreferenceCompat>("notification_enabled")?.apply {
                isChecked = preferencesManager.isNotificationEnabled()
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.setNotificationEnabled(newValue as Boolean)
                    true
                }
            }
            
            // Ekran görüntüsü ayarı
            findPreference<SwitchPreferenceCompat>("screen_capture_enabled")?.apply {
                isChecked = preferencesManager.isScreenCaptureEnabled()
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.setScreenCaptureEnabled(newValue as Boolean)
                    true
                }
            }
            
            // Titreşim ayarı
            findPreference<SwitchPreferenceCompat>("vibration_enabled")?.apply {
                isChecked = preferencesManager.isVibrationEnabled()
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.setVibrationEnabled(newValue as Boolean)
                    true
                }
            }
            
            // Arka plan modu ayarı
            findPreference<SwitchPreferenceCompat>("background_mode")?.apply {
                isChecked = preferencesManager.isBackgroundModeEnabled()
                setOnPreferenceChangeListener { _, newValue ->
                    preferencesManager.setBackgroundMode(newValue as Boolean)
                    // Arka plan servisini başlat/durdur
                    // TODO: Implement background service control
                    true
                }
            }
        }
    }
}