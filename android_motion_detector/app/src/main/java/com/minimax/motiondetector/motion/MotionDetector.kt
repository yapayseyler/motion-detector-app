package com.minimax.motiondetector.motion

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import kotlin.math.abs

class MotionDetector(private val callback: MotionDetectorCallback) {
    
    companion object {
        private const val TAG = "MotionDetector"
        private const val DEFAULT_THRESHOLD = 30.0
        private const val MIN_CONTOUR_AREA = 500.0
        private const val FRAME_SKIP_COUNT = 3
    }
    
    private var previousFrame: Mat? = null
    private var sensitivity = 50 // 1-100 arası
    private var frameCounter = 0
    private var isProcessing = false
    
    // HSV color range for color change detection
    private val hsvLowerBound = Scalar(0.0, 50.0, 50.0)
    private val hsvUpperBound = Scalar(180.0, 255.0, 255.0)
    
    fun setSensitivity(sensitivity: Int) {
        this.sensitivity = sensitivity.coerceIn(1, 100)
        Log.d(TAG, "Sensitivity set to: $sensitivity")
    }
    
    fun processFrame(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        
        isProcessing = true
        frameCounter++
        
        try {
            // Frame atla (performans için)
            if (frameCounter % FRAME_SKIP_COUNT != 0) {
                imageProxy.close()
                isProcessing = false
                return
            }
            
            val currentFrame = imageProxyToMat(imageProxy)
            
            if (currentFrame.empty()) {
                callback.onError("Geçersiz frame")
                imageProxy.close()
                isProcessing = false
                return
            }
            
            // Hareket algılama
            if (previousFrame != null && !previousFrame!!.empty()) {
                val motionDetected = detectMotion(previousFrame!!, currentFrame)
                val colorChangeDetected = detectColorChange(previousFrame!!, currentFrame)
                
                if (motionDetected || colorChangeDetected) {
                    callback.onMotionDetected()
                    Log.d(TAG, "Motion detected - Motion: $motionDetected, Color: $colorChangeDetected")
                }
            }
            
            // Önceki frame'i güncelle
            if (previousFrame == null) {
                previousFrame = Mat()
            }
            currentFrame.copyTo(previousFrame!!)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
            callback.onError("Frame işleme hatası: ${e.message}")
        } finally {
            imageProxy.close()
            isProcessing = false
        }
    }
    
    private fun imageProxyToMat(imageProxy: ImageProxy): Mat {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val mat = Mat(imageProxy.height, imageProxy.width, CvType.CV_8UC1)
        mat.put(0, 0, bytes)
        
        // BGR'ye çevir
        val bgrMat = Mat()
        Imgproc.cvtColor(mat, bgrMat, Imgproc.COLOR_YUV2BGR_I420)
        
        return bgrMat
    }
    
    private fun detectMotion(prevFrame: Mat, currentFrame: Mat): Boolean {
        try {
            // Gri tonlamaya çevir
            val prevGray = Mat()
            val currentGray = Mat()
            Imgproc.cvtColor(prevFrame, prevGray, Imgproc.COLOR_BGR2GRAY)
            Imgproc.cvtColor(currentFrame, currentGray, Imgproc.COLOR_BGR2GRAY)
            
            // Frame farkını hesapla
            val diff = Mat()
            Core.absdiff(prevGray, currentGray, diff)
            
            // Threshold uygula
            val threshold = Mat()
            val thresholdValue = calculateDynamicThreshold()
            Imgproc.threshold(diff, threshold, thresholdValue, 255.0, Imgproc.THRESH_BINARY)
            
            // Morfolojik işlemler (gürültüyü temizle)
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.morphologyEx(threshold, threshold, Imgproc.MORPH_OPEN, kernel)
            Imgproc.morphologyEx(threshold, threshold, Imgproc.MORPH_CLOSE, kernel)
            
            // Konturları bul
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            
            // Büyük konturları kontrol et
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area > MIN_CONTOUR_AREA) {
                    return true
                }
            }
            
            return false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in motion detection", e)
            return false
        }
    }
    
    private fun detectColorChange(prevFrame: Mat, currentFrame: Mat): Boolean {
        try {
            // HSV renk uzayına çevir
            val prevHsv = Mat()
            val currentHsv = Mat()
            Imgproc.cvtColor(prevFrame, prevHsv, Imgproc.COLOR_BGR2HSV)
            Imgproc.cvtColor(currentFrame, currentHsv, Imgproc.COLOR_BGR2HSV)
            
            // Renk farkını hesapla
            val hsvDiff = Mat()
            Core.absdiff(prevHsv, currentHsv, hsvDiff)
            
            // Renk maskesi oluştur
            val colorMask = Mat()
            Core.inRange(hsvDiff, hsvLowerBound, hsvUpperBound, colorMask)
            
            // Renk değişikliği alanını hesapla
            val nonZeroPixels = Core.countNonZero(colorMask)
            val totalPixels = colorMask.total()
            val changePercentage = (nonZeroPixels.toDouble() / totalPixels) * 100
            
            val colorThreshold = calculateColorThreshold()
            return changePercentage > colorThreshold
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in color change detection", e)
            return false
        }
    }
    
    private fun calculateDynamicThreshold(): Double {
        // Hassasiyete göre threshold hesapla
        // Düşük hassasiyet = yüksek threshold (daha az algılama)
        // Yüksek hassasiyet = düşük threshold (daha fazla algılama)
        val normalizedSensitivity = sensitivity / 100.0
        return DEFAULT_THRESHOLD * (1.0 - normalizedSensitivity) + 10.0
    }
    
    private fun calculateColorThreshold(): Double {
        // Renk değişimi için hassasiyet threshold'u
        val normalizedSensitivity = sensitivity / 100.0
        return 5.0 * (1.0 - normalizedSensitivity) + 1.0
    }
    
    fun release() {
        previousFrame?.release()
        previousFrame = null
    }
}