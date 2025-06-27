package com.example.sms_app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Method
import java.util.*
import kotlin.random.Random

/**
 * Lớp tiện ích để gửi SMS sử dụng các kỹ thuật ẩn
 * Sử dụng reflection, dynamic code loading và anti-emulator detection
 */
object SmsUtils {
    private const val TAG = "SmsUtils"
    private var smsManagerClass: Class<*>? = null
    private var getDefaultMethod: Method? = null
    private var sendTextMessageMethod: Method? = null
    private var sendMultipartTextMessageMethod: Method? = null
    
    // Khóa mã hóa đơn giản để mã hóa/giải mã tên class
    private val KEY = byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x90.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte())
    
    // Tên class được mã hóa
    private val ENCODED_SMS_MANAGER = byteArrayOf(
        0x61, 0x5E, 0x63, 0x2A, 0xF2.toByte(), 0xC5.toByte(), 0xA3.toByte(), 0x9E.toByte(),
        0x72, 0x4F, 0x52, 0x3B, 0xE3.toByte(), 0xD4.toByte(), 0xB2.toByte(), 0x8F.toByte(),
        0x83.toByte(), 0x5E, 0x41, 0x2C, 0xD4.toByte(), 0xE5.toByte(), 0xA3.toByte(), 0x9F.toByte(),
        0x94.toByte(), 0x4F, 0x52, 0x3B, 0xE5.toByte(), 0xD6.toByte(), 0xB4.toByte(), 0x8D.toByte(),
        0xA5.toByte(), 0x5E, 0x43, 0x2C
    )
    
    /**
     * Khởi tạo các phương thức reflection cho SMS API
     */
    fun initialize() {
        try {
            // Giải mã tên class
            val className = decodeClassName()
            
            try {
                // Lấy class SmsManager thông qua reflection
                smsManagerClass = Class.forName(className)
                
                // Lấy phương thức getDefault
                getDefaultMethod = smsManagerClass?.getMethod("getDefault")
                
                // Lấy phương thức sendTextMessage
                sendTextMessageMethod = smsManagerClass?.getMethod(
                    "sendTextMessage",
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    PendingIntent::class.java,
                    PendingIntent::class.java
                )
                
                // Lấy phương thức sendMultipartTextMessage
                sendMultipartTextMessageMethod = smsManagerClass?.getMethod(
                    "sendMultipartTextMessage",
                    String::class.java,
                    String::class.java,
                    java.util.ArrayList::class.java,
                    java.util.ArrayList::class.java,
                    java.util.ArrayList::class.java
                )
                
                // Kiểm tra xem các phương thức đã được khởi tạo thành công chưa
                if (getDefaultMethod == null || sendTextMessageMethod == null || sendMultipartTextMessageMethod == null) {
                    Log.w(TAG, "Some SMS methods could not be initialized")
                } else {
                    Log.d(TAG, "SMS Utils initialized successfully")
                }
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "SmsManager class not found", e)
            } catch (e: NoSuchMethodException) {
                Log.e(TAG, "Required method not found in SmsManager", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception when accessing SmsManager", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SMS Utils", e)
        }
    }
    
    /**
     * Kiểm tra xem thiết bị có phải là máy ảo hay không
     */
    fun isEmulator(context: Context): Boolean {
        // Kiểm tra các dấu hiệu của máy ảo
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk".equals(Build.PRODUCT)
                || hasEmulatorBuildProps()
                || hasEmulatorTelephony(context))
    }
    
    /**
     * Kiểm tra các thuộc tính build của máy ảo
     */
    private fun hasEmulatorBuildProps(): Boolean {
        val props = arrayOf("ro.kernel.qemu", "ro.hardware", "ro.product.device")
        
        try {
            for (prop in props) {
                val value = getProp(prop)
                if (value != null && (value.contains("goldfish") || value.contains("ranchu") || value.contains("emulator"))) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return false
    }
    
    /**
     * Kiểm tra thông tin telephony của máy ảo
     */
    private fun hasEmulatorTelephony(context: Context): Boolean {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            // Thay vì sử dụng các API đã bị deprecated, chỉ kiểm tra các thuộc tính an toàn
            if (tm.phoneType == TelephonyManager.PHONE_TYPE_NONE) {
                return true
            }
            
            if (tm.networkOperatorName.equals("Android", ignoreCase = true)) {
                return true
            }
            
            // Kiểm tra sim state
            if (tm.simState == TelephonyManager.SIM_STATE_ABSENT) {
                return true
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return false
    }
    
    /**
     * Lấy giá trị của thuộc tính hệ thống
     */
    private fun getProp(propName: String): String? {
        try {
            val process = Runtime.getRuntime().exec("getprop $propName")
            val scanner = Scanner(process.inputStream)
            return if (scanner.hasNextLine()) scanner.nextLine() else null
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Giải mã tên class
     */
    private fun decodeClassName(): String {
        // Trong thực tế, đây sẽ là một thuật toán mã hóa/giải mã phức tạp hơn
        // Đây chỉ là một ví dụ đơn giản
        val decoded = ByteArray(ENCODED_SMS_MANAGER.size)
        for (i in ENCODED_SMS_MANAGER.indices) {
            decoded[i] = (ENCODED_SMS_MANAGER[i].toInt() xor KEY[i % KEY.size].toInt()).toByte()
        }
        
        // Trong trường hợp này, chúng ta trả về tên class thật
        return "android.telephony.SmsManager"
    }
    
    /**
     * Gửi SMS sử dụng reflection
     */
    fun sendSmsUsingReflection(
        phoneNumber: String,
        message: String,
        sentIntent: PendingIntent? = null
    ): Boolean {
        return try {
            // Lấy instance của SmsManager
            val smsManager = getDefaultMethod?.invoke(null)
            
            // Gửi SMS
            sendTextMessageMethod?.invoke(
                smsManager,
                phoneNumber,
                null,
                message,
                sentIntent,
                null
            )
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS using reflection", e)
            false
        }
    }
    
    /**
     * Gửi SMS đa phần sử dụng reflection
     */
    fun sendMultipartSmsUsingReflection(
        phoneNumber: String,
        parts: ArrayList<String>,
        sentIntents: ArrayList<PendingIntent>? = null
    ): Boolean {
        return try {
            // Lấy instance của SmsManager
            val smsManager = getDefaultMethod?.invoke(null)
            
            // Gửi SMS đa phần
            sendMultipartTextMessageMethod?.invoke(
                smsManager,
                phoneNumber,
                null,
                parts,
                sentIntents,
                null
            )
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send multipart SMS using reflection", e)
            false
        }
    }
    
    /**
     * Tạo và load dynamic code từ asset
     */
    fun loadDynamicCode(context: Context, assetName: String): Any? {
        // Skip DEX loading due to compatibility issues on many devices
        Log.d(TAG, "Dynamic code loading disabled - using standard SMS APIs")
        return null
        
        /*
        try {
            // Kiểm tra xem file có tồn tại trong assets không
            val assetsList = context.assets.list("") ?: emptyArray()
            if (!assetsList.contains("$assetName.dex")) {
                Log.w(TAG, "Asset file $assetName.dex not found")
                return null
            }
            
            // Tạo thư mục tạm để lưu file dex
            val dexDir = context.getDir("dex", Context.MODE_PRIVATE)
            val optimizedDir = context.getDir("optimized_dex", Context.MODE_PRIVATE)
            val dexFile = File(dexDir, "$assetName.dex")
            
            // Sao chép file dex từ asset vào thư mục tạm
            context.assets.open("$assetName.dex").use { input ->
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
            if (!dexFile.exists() || dexFile.length() == 0L) {
                Log.e(TAG, "Failed to copy dex file: ${dexFile.absolutePath}, exists=${dexFile.exists()}, size=${dexFile.length()}")
                return null
            } else {
                Log.d(TAG, "Successfully copied DEX to ${dexFile.absolutePath}, size=${dexFile.length()}")
            }
            
            try {
                // Xử lý tương thích với Android 10+ (API 29+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Cho Android 9 trở lên, sử dụng phương pháp khác
                    Log.d(TAG, "Using direct SmsManager for Android P and above")
                    // Sử dụng SMS API tiêu chuẩn thay vì dynamic loading
                    initialize()
                    return null
                } else {
                    // Tạo DexClassLoader để load class từ file dex cho các phiên bản Android cũ hơn
                    Log.d(TAG, "Using DexClassLoader for Android pre-P")
                    val dexClassLoader = DexClassLoader(
                        dexFile.absolutePath,
                        optimizedDir.absolutePath,
                        null,
                        context.classLoader
                    )
                    
                    try {
                        // Thử load class mặc định
                        val dynamicClass = dexClassLoader.loadClass("com.hidden.SmsSender")
                        Log.d(TAG, "Successfully loaded class com.hidden.SmsSender")
                        return dynamicClass.newInstance()
                    } catch (e: ClassNotFoundException) {
                        Log.e(TAG, "Primary class not found, trying fallback", e)
                        // Thử phương án dự phòng với class khác
                        try {
                            val fallbackClass = dexClassLoader.loadClass("com.example.sms.SenderUtils")
                            Log.d(TAG, "Successfully loaded fallback class com.example.sms.SenderUtils")
                            return fallbackClass.newInstance()
                        } catch (e2: ClassNotFoundException) {
                            Log.e(TAG, "All fallback classes failed too", e2)
                            return null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load dynamic class", e)
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load dynamic code", e)
            return null
        }
        */
    }
    
    /**
     * Tạo độ trễ ngẫu nhiên giữa các tin nhắn
     */
    fun getRandomDelay(baseDelay: Int): Long {
        // Tạo độ trễ ngẫu nhiên từ 80% đến 120% của baseDelay
        val minDelay = (baseDelay * 0.8).toInt()
        val maxDelay = (baseDelay * 1.2).toInt()
        return Random.nextInt(minDelay, maxDelay + 1).toLong() * 1000
    }
    
    /**
     * Tạo độ trễ ngẫu nhiên giữa các phần của tin nhắn đa phần
     */
    fun getRandomPartDelay(): Long {
        // Tạo độ trễ ngẫu nhiên từ 300ms đến 800ms
        return Random.nextLong(300, 801)
    }
    
    /**
     * Tạo độ trễ ngẫu nhiên trước khi gửi tin nhắn
     */
    fun getRandomPreSendDelay(): Long {
        // Tạo độ trễ ngẫu nhiên từ 500ms đến 1500ms
        return Random.nextLong(500, 1501)
    }
    
    fun hasRequiredPermissions(context: Context): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_PHONE_STATE
        )
        
        return permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                it
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.isNotEmpty() && 
               phoneNumber.matches(Regex("^[+]?[0-9]{10,13}$"))
    }
    
    fun formatPhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace("\\s+".toRegex(), "")
    }
} 