package com.minimax.motiondetector

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private var isDetecting = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupViews()
    }
    
    private fun setupViews() {
        val statusText = findViewById<TextView>(R.id.statusText)
        val sensitivitySeekBar = findViewById<SeekBar>(R.id.sensitivitySeekBar)
        val startStopButton = findViewById<Button>(R.id.startStopButton)
        val screenshotButton = findViewById<Button>(R.id.screenshotButton)
        
        startStopButton.setOnClickListener {
            if (isDetecting) {
                stopDetection()
                startStopButton.text = "Start Detection"
                statusText.text = "Stopped"
            } else {
                startDetection()
                startStopButton.text = "Stop Detection"
                statusText.text = "Detecting..."
            }
            isDetecting = !isDetecting
        }
        
        screenshotButton.setOnClickListener {
            Toast.makeText(this, "Screenshot taken!", Toast.LENGTH_SHORT).show()
        }
        
        sensitivitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Sensitivity changed
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun startDetection() {
        // Motion detection logic will be implemented here
        Toast.makeText(this, "Motion detection started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopDetection() {
        // Stop motion detection
        Toast.makeText(this, "Motion detection stopped", Toast.LENGTH_SHORT).show()
    }
}
