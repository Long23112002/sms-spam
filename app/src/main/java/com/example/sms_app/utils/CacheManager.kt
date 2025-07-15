package com.example.sms_app.utils

import android.content.Context
import android.util.Log
import com.example.sms_app.data.SessionBackup
import com.example.sms_app.data.SmsRepository

/**
 * Utility class để quản lý cache và ngăn chặn việc tự động gửi SMS
 */
class CacheManager(private val context: Context) {
    private val TAG = "CacheManager"
    
    /**
     * Xóa tất cả cache liên quan đến SMS để ngăn chặn việc tự động gửi
     */
    fun clearAllSmsCache() {
        try {
            Log.d(TAG, "🧹 Bắt đầu xóa tất cả cache SMS...")
            
            // 1. Xóa session backup
            val sessionBackup = SessionBackup(context)
            sessionBackup.clearActiveSession()
            sessionBackup.clearAllSessionHistory()
            Log.d(TAG, "✅ Đã xóa session backup")
            
            // 2. Xóa countdown data
            val smsRepository = SmsRepository(context)
            smsRepository.clearCountdownData()
            Log.d(TAG, "✅ Đã xóa countdown data")
            
            // 3. Reset SMS count (tùy chọn)
            smsRepository.resetAllSimCounts()
            Log.d(TAG, "✅ Đã reset SMS count")
            
            Log.d(TAG, "🎉 Hoàn tất xóa tất cả cache SMS")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi xóa cache SMS", e)
        }
    }
    
    /**
     * Xóa cache sau khi import Excel (chỉ xóa session, không reset SMS count)
     */
    fun clearCacheAfterImport() {
        try {
            Log.d(TAG, "🧹 Xóa cache sau khi import Excel...")
            
            // Chỉ xóa session backup để ngăn việc tự động gửi SMS
            val sessionBackup = SessionBackup(context)
            sessionBackup.clearActiveSession()
            Log.d(TAG, "✅ Đã xóa active session sau import")
            
            // Xóa countdown data nếu có
            val smsRepository = SmsRepository(context)
            smsRepository.clearCountdownData()
            Log.d(TAG, "✅ Đã xóa countdown data sau import")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi xóa cache sau import", e)
        }
    }
    
    /**
     * Kiểm tra xem có cache nào có thể gây ra việc tự động gửi SMS không
     */
    fun checkForPotentialAutoSmsCache(): String {
        try {
            val issues = mutableListOf<String>()
            
            // Kiểm tra active session
            val sessionBackup = SessionBackup(context)
            val activeSession = sessionBackup.getActiveSession()
            if (activeSession != null) {
                issues.add("⚠️ Có active session với ${activeSession.remainingCustomers.size} khách hàng chưa gửi")
            }
            
            // Kiểm tra countdown data
            val smsRepository = SmsRepository(context)
            val countdownStartTime = smsRepository.getCountdownStartTime()
            if (countdownStartTime > 0) {
                issues.add("⚠️ Có countdown data đang hoạt động")
            }
            
            // Kiểm tra app settings
            val settings = smsRepository.getAppSettings()
            if (settings.enableAutoSms) {
                issues.add("⚠️ Tính năng tự động gửi SMS đang BẬT")
            }
            
            return if (issues.isEmpty()) {
                "✅ Không phát hiện cache có thể gây tự động gửi SMS"
            } else {
                issues.joinToString("\n")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra cache", e)
            return "❌ Lỗi khi kiểm tra cache: ${e.message}"
        }
    }
    
    /**
     * Vô hiệu hóa tất cả tính năng tự động gửi SMS
     */
    fun disableAllAutoSmsFeatures() {
        try {
            Log.d(TAG, "🚫 Vô hiệu hóa tất cả tính năng tự động gửi SMS...")
            
            val smsRepository = SmsRepository(context)
            val currentSettings = smsRepository.getAppSettings()
            
            // Tắt tính năng tự động gửi SMS
            val newSettings = currentSettings.copy(
                enableAutoSms = false,
                clearCacheAfterImport = true
            )
            
            smsRepository.saveAppSettings(newSettings)
            Log.d(TAG, "✅ Đã tắt tính năng tự động gửi SMS trong settings")
            
            // Xóa tất cả cache
            clearAllSmsCache()
            
            Log.d(TAG, "🎉 Hoàn tất vô hiệu hóa tất cả tính năng tự động")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi vô hiệu hóa tính năng tự động", e)
        }
    }
}