package com.minimax.motiondetector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.minimax.motiondetector.databinding.ActivityMotionHistoryBinding

class MotionHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMotionHistoryBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMotionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        setupRecyclerView()
        loadMotionHistory()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun setupRecyclerView() {
        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        // TODO: Set adapter for motion history
    }
    
    private fun loadMotionHistory() {
        // TODO: Load motion history from database
        // Bu özellik opsiyonel olarak Room database ile implement edilebilir
    }
}