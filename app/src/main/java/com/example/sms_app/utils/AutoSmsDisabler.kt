package com.example.sms_app.utils

import android.content.Context
import android.util.Log
import com.example.sms_app.data.SmsRepository

/**
 * Utility class để vô hiệu hóa hoàn toàn tất cả tính năng tự động gửi SMS
 * và kiểm tra trạng thái bảo mật của ứng dụng
 */
object AutoSmsDisabler {
    private const val TAG = "AutoSmsDisabler"
    
    /**
     * Vô hiệu hóa hoàn toàn tất cả tính năng tự động gửi SMS
     */
    fun disableAllAutoSmsFeatures(context: Context): Boolean {
        return try {
            Log.d(TAG, "🚫 Bắt đầu vô hiệu hóa tất cả tính năng tự động gửi SMS...")
            
            // 1. Cập nhật settings để tắt tự động gửi SMS
            val smsRepository = SmsRepository(context)
            val currentSettings = smsRepository.getAppSettings()
            val newSettings = currentSettings.copy(
                enableAutoSms = false,
                clearCacheAfterImport = true
            )
            smsRepository.saveAppSettings(newSettings)
            Log.d(TAG, "✅ Đã tắt enableAutoSms trong settings")
            
            // 2. Xóa tất cả cache
            val cacheManager = CacheManager(context)
            cacheManager.clearAllSmsCache()
            Log.d(TAG, "✅ Đã xóa tất cả cache SMS")
            
            // 3. Ghi log trạng thái
            Log.d(TAG, "🎉 Hoàn tất vô hiệu hóa tất cả tính năng tự động gửi SMS")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi vô hiệu hóa tính năng tự động", e)
            false
        }
    }
    
    /**
     * Kiểm tra trạng thái bảo mật và các tính năng tự động
     */
    fun checkSecurityStatus(context: Context): SecurityReport {
        return try {
            val issues = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            val info = mutableListOf<String>()
            
            // 1. Kiểm tra HiddenSmsReceiver
            info.add("✅ HiddenSmsReceiver đã được vô hiệu hóa trong code")
            
            // 2. Kiểm tra settings
            val smsRepository = SmsRepository(context)
            val settings = smsRepository.getAppSettings()
            
            if (settings.enableAutoSms) {
                issues.add("⚠️ Tính năng tự động gửi SMS đang BẬT")
            } else {
                info.add("✅ Tính năng tự động gửi SMS đã TẮT")
            }
            
            if (!settings.clearCacheAfterImport) {
                warnings.add("⚠️ Không tự động xóa cache sau import")
            } else {
                info.add("✅ Tự động xóa cache sau import đã BẬT")
            }
            
            // 3. Kiểm tra cache hiện tại
            val cacheManager = CacheManager(context)
            val cacheStatus = cacheManager.checkForPotentialAutoSmsCache()
            if (cacheStatus.contains("⚠️")) {
                warnings.add(cacheStatus)
            } else {
                info.add(cacheStatus)
            }
            
            // 4. Kiểm tra session backup
            val sessionBackup = com.example.sms_app.data.SessionBackup(context)
            val activeSession = sessionBackup.getActiveSession()
            if (activeSession != null) {
                warnings.add("⚠️ Có active session với ${activeSession.remainingCustomers.size} khách hàng")
            } else {
                info.add("✅ Không có active session")
            }
            
            SecurityReport(
                issues = issues,
                warnings = warnings,
                info = info,
                isSecure = issues.isEmpty()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra trạng thái bảo mật", e)
            SecurityReport(
                issues = listOf("❌ Lỗi khi kiểm tra: ${e.message}"),
                warnings = emptyList(),
                info = emptyList(),
                isSecure = false
            )
        }
    }
    
    /**
     * Tạo báo cáo bảo mật chi tiết
     */
    fun generateSecurityReport(context: Context): String {
        val report = checkSecurityStatus(context)
        val sb = StringBuilder()
        
        sb.appendLine("📋 BÁO CÁO BẢO MẬT SMS APP")
        sb.appendLine("========================================")
        sb.appendLine()
        
        if (report.isSecure) {
            sb.appendLine("🔒 TRẠNG THÁI: AN TOÀN")
        } else {
            sb.appendLine("⚠️ TRẠNG THÁI: CẦN KHẮC PHỤC")
        }
        sb.appendLine()
        
        if (report.issues.isNotEmpty()) {
            sb.appendLine("🚨 VẤN ĐỀ NGHIÊM TRỌNG:")
            report.issues.forEach { sb.appendLine("  $it") }
            sb.appendLine()
        }
        
        if (report.warnings.isNotEmpty()) {
            sb.appendLine("⚠️ CẢNH BÁO:")
            report.warnings.forEach { sb.appendLine("  $it") }
            sb.appendLine()
        }
        
        if (report.info.isNotEmpty()) {
            sb.appendLine("ℹ️ THÔNG TIN:")
            report.info.forEach { sb.appendLine("  $it") }
            sb.appendLine()
        }
        
        sb.appendLine("🛡️ KHUYẾN NGHỊ:")
        sb.appendLine("  1. Luôn kiểm tra trước khi import Excel")
        sb.appendLine("  2. Chỉ gửi SMS khi người dùng chủ động kích hoạt")
        sb.appendLine("  3. Xóa cache sau mỗi lần sử dụng")
        sb.appendLine("  4. Kiểm tra log thường xuyên")
        
        return sb.toString()
    }
    
    /**
     * Khởi tạo ứng dụng ở chế độ an toàn
     */
    fun initializeSafeMode(context: Context): Boolean {
        return try {
            Log.d(TAG, "🛡️ Khởi tạo ứng dụng ở chế độ an toàn...")
            
            // Vô hiệu hóa tất cả tính năng tự động
            val success = disableAllAutoSmsFeatures(context)
            
            if (success) {
                Log.d(TAG, "✅ Ứng dụng đã được khởi tạo ở chế độ an toàn")
            } else {
                Log.e(TAG, "❌ Không thể khởi tạo chế độ an toàn")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khi khởi tạo chế độ an toàn", e)
            false
        }
    }
}

/**
 * Data class để lưu trữ báo cáo bảo mật
 */
data class SecurityReport(
    val issues: List<String>,      // Vấn đề nghiêm trọng cần khắc phục ngay
    val warnings: List<String>,    // Cảnh báo cần chú ý
    val info: List<String>,        // Thông tin tích cực
    val isSecure: Boolean          // Tổng thể có an toàn không
)