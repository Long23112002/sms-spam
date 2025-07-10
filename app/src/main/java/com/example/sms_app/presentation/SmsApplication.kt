package com.example.sms_app.presentation

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File

@HiltAndroidApp
class SmsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Cấu hình Timber để log trong cả debug và release build
        Timber.plant(object : Timber.DebugTree() {
            override fun isLoggable(tag: String?, priority: Int): Boolean {
                // Cho phép log kể cả khi là release build
                return true
            }
            
            // Thêm thông tin chi tiết hơn vào log
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // Ghi log vào file
                try {
                    val logDir = File(applicationContext.getExternalFilesDir(null), "logs")
                    if (!logDir.exists()) {
                        logDir.mkdirs()
                    }
                    
                    val logFile = File(logDir, "app_log.txt")
                    val priorityChar = when (priority) {
                        Log.VERBOSE -> 'V'
                        Log.DEBUG -> 'D'
                        Log.INFO -> 'I'
                        Log.WARN -> 'W'
                        Log.ERROR -> 'E'
                        Log.ASSERT -> 'A'
                        else -> '?'
                    }
                    
                    // Thêm thông tin stacktrace nếu có
                    val stackTrace = if (t != null) {
                        "\n${Log.getStackTraceString(t)}"
                    } else {
                        ""
                    }
                    
                    // Ghi log ra file
                    logFile.appendText("$priorityChar/$tag: $message$stackTrace\n")
                } catch (e: Exception) {
                    // Nếu không ghi được vào file, vẫn ghi vào logcat
                    Log.e("Timber", "Không thể ghi log vào file: ${e.message}")
                }
                
                // Vẫn ghi vào logcat như bình thường
                super.log(priority, tag, message, t)
            }
        })
        
        // Dynamic code loading disabled due to compatibility issues on many devices
        Timber.i("Standard SMS APIs will be used instead of dynamic code loading")
        
        Timber.i("Ứng dụng đã khởi động")
    }
    
    // DEX preparation disabled since we're using standard SMS APIs
    /*
    private fun checkAndPrepareSmsSenderDex() {
        try {
            // Kiểm tra xem file smssender.dex có tồn tại trong assets không
            val assetsList = assets.list("") ?: emptyArray()
            if (!assetsList.contains("smssender.dex")) {
                Timber.e("smssender.dex không tồn tại trong assets")
                return
            }
            
            // Tạo thư mục app_dex
            val dexDir = getDir("dex", MODE_PRIVATE)
            val optimizedDir = getDir("optimized_dex", MODE_PRIVATE)
            
            // Đảm bảo thư mục tồn tại và có thể ghi
            if (!dexDir.exists() && !dexDir.mkdirs()) {
                Timber.e("Không thể tạo thư mục app_dex")
                return
            }
            if (!optimizedDir.exists() && !optimizedDir.mkdirs()) {
                Timber.e("Không thể tạo thư mục optimized_dex")
                return
            }
            
            // Tiến hành sao chép file DEX từ assets
            val dexFile = File(dexDir, "smssender.dex")
            
            // Nếu file đã tồn tại, xóa để chuẩn bị sao chép lại
            if (dexFile.exists()) {
                dexFile.delete()
            }
            
            // Tiến hành sao chép
            assets.open("smssender.dex").use { input ->
                FileOutputStream(dexFile).use { output ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            
            // Kiểm tra file đã được sao chép thành công
            if (dexFile.exists() && dexFile.length() > 0) {
                Timber.d("Đã sao chép thành công smssender.dex vào ${dexFile.absolutePath}, kích thước: ${dexFile.length()} bytes")
                
                // Tính toán MD5 để kiểm tra tính toàn vẹn
                val md5 = calculateMD5(dexFile)
                Timber.d("MD5 của file DEX: $md5")
            } else {
                Timber.e("Sao chép smssender.dex thất bại")
            }
        } catch (e: Exception) {
            Timber.e(e, "Lỗi khi chuẩn bị file smssender.dex")
        }
    }
    
    private fun calculateMD5(file: File): String {
        try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    md.update(buffer, 0, read)
                }
            }
            
            val md5Bytes = md.digest()
            return md5Bytes.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            Timber.e(e, "Lỗi khi tính toán MD5")
            return "unknown"
        }
    }
    */
} 