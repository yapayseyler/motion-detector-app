package com.minimax.motiondetector.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.minimax.motiondetector.MainActivity
import com.minimax.motiondetector.R

class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID = "motion_detection_channel"
        private const val CHANNEL_NAME = "Hareket Algılama"
        private const val CHANNEL_DESCRIPTION = "Hareket algılandığında bildirimler"
        private const val NOTIFICATION_ID = 1001
        private const val FOREGROUND_NOTIFICATION_ID = 1002
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
    
    fun showMotionDetectedNotification() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⚠️ Hareket Algılandı!")
                .setContentText("${getCurrentTime()}'da hareket tespit edildi")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setLights(0xFF0000, 1000, 1000)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()
            
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(NOTIFICATION_ID, notification)
                Log.d(TAG, "Motion detection notification shown")
            } else {
                Log.w(TAG, "Notifications are disabled")
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for notifications", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing motion detection notification", e)
        }
    }
    
    fun createForegroundNotification(): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hareket Algılama Aktif")
            .setContentText("Arka planda hareket izleniyor...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    fun showScreenshotNotification(filePath: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_camera)
                .setContentTitle("📷 Ekran Görüntüsü Alındı")
                .setContentText("Görüntü kaydedildi: $filePath")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
            Log.d(TAG, "Screenshot notification shown")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing screenshot notification", e)
        }
    }
    
    fun cancelAllNotifications() {
        try {
            notificationManager.cancelAll()
            Log.d(TAG, "All notifications cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notifications", e)
        }
    }
    
    private fun getCurrentTime(): String {
        return java.text.SimpleDateFormat(
            "HH:mm:ss", 
            java.util.Locale.getDefault()
        ).format(java.util.Date())
    }
    
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
}