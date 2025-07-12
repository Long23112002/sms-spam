package com.example.sms_app.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.lang.reflect.Method
import java.util.Scanner

/**
 * Lớp tiện ích để gửi SMS sử dụng các kỹ thuật ẩn
 * Sử dụng reflection, dynamic code loading và anti-emulator detection
 */

private const val TAG = "SmsUtils"
private var smsManagerClass: Class<*>? = null
private var getDefaultMethod: Method? = null
private var sendTextMessageMethod: Method? = null
private var sendMultipartTextMessageMethod: Method? = null

// Khóa mã hóa đơn giản để mã hóa/giải mã tên class
private val KEY =
    byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x90.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte())

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
            if (value != null && (value.contains("goldfish") || value.contains("ranchu") || value.contains(
                    "emulator"
                ))
            ) {
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
        // Final cleanup of phone number to ensure no invisible characters remain
        val cleanedNumber =
            phoneNumber.toCharArray().filter { it in '0'..'9' || it == '+' }.joinToString("")
        Log.d(TAG, "Final phone number cleanup for sending: '$phoneNumber' → '$cleanedNumber'")

        // Lấy instance của SmsManager
        val smsManager = getDefaultMethod?.invoke(null)

        try {
            // Gửi SMS
            sendTextMessageMethod?.invoke(
                smsManager,
                cleanedNumber,
                null,
                message,
                sentIntent,
                null
            )

            true
        } catch (e: Exception) {
            val stackTrace = e.stackTraceToString()
            Log.e(TAG, "SMS sending failed for $cleanedNumber", e)
            Log.e(TAG, "Detailed stack trace: $stackTrace")

            // Try to get more details about the failure
            val cause = e.cause
            if (cause != null) {
                Log.e(TAG, "Root cause: ${cause.message}", cause)
            }

            val invocationException = e as? java.lang.reflect.InvocationTargetException
            if (invocationException != null) {
                val targetException = invocationException.targetException
                Log.e(TAG, "Target exception: ${targetException?.message}", targetException)
            }

            false
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send SMS using reflection", e)
        Log.e(TAG, "Detailed stack trace: ${e.stackTraceToString()}")
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
    // Không còn tạo độ trễ ngẫu nhiên, sử dụng giá trị chính xác
    return baseDelay.toLong() * 1000
}

fun hasRequiredPermissions(context: Context): Boolean {
    val permissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_PHONE_STATE
    )

    return permissions.all {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Checks if a phone number is a valid Vietnamese mobile number
 */
fun isValidVietnameseNumber(phoneNumber: String): Boolean {
    val cleanedNumber = phoneNumber.validateAndFormatPhoneNumber()

    // Must be 10 digits and start with 0
    if (cleanedNumber.length != 10 || !cleanedNumber.startsWith("0")) {
        return false
    }

    // Check against valid Vietnamese carrier prefixes
    val prefix = cleanedNumber.substring(0, 3)
    val validPrefixes = listOf(
        // Viettel
        "032", "033", "034", "035", "036", "037", "038", "039",
        "086", "096", "097", "098",
        // Mobifone
        "070", "076", "077", "078", "079",
        "089", "090", "093",
        // Vinaphone
        "081", "082", "083", "084", "085",
        "088", "091", "094",
        // Vietnamobile
        "056", "058", "092",
        // ITelecom
        "099",
        // Reddi/Gmobile
        "059"
    )

    return validPrefixes.contains(prefix)
}

/**
 * Validates and formats a phone number to ensure proper SMS delivery
 * This is a comprehensive function that should be used whenever processing phone numbers
 */
fun String.validateAndFormatPhoneNumber(): String = runCatching {
    val originalNumber = trim()
    Timber.tag(TAG).d("Validating phone number: '$originalNumber'")


    // Apply deep Unicode normalization first to eliminate invisible characters
    var normalized =
        java.text.Normalizer.normalize(originalNumber, java.text.Normalizer.Form.NFKC)

    // Log hex values of characters to detect invisible characters
    val hexValues =
        normalized.map { "\\u" + it.code.toString(16).padStart(4, '0') }.joinToString("")
    Timber.tag(TAG).d("Unicode hex values: $hexValues")

    // Remove ALL non-digit characters including invisible ones
    var digitsOnly = normalized.replace(Regex("[^0-9+]"), "")

    // Handle empty string

    if (digitsOnly.isEmpty()) {
        throw IllegalArgumentException("Error: Empty phone number after formatting")
    }

    // Remove international prefix if exists
    if (digitsOnly.startsWith("+84")) {
        digitsOnly = "0" + digitsOnly.substring(3)
        Timber.tag(TAG).d("Converted from international format +84: $digitsOnly")
    } else if (digitsOnly.startsWith("84") && digitsOnly.length >= 10) {
        digitsOnly = "0" + digitsOnly.substring(2)
        Timber.tag(TAG).d("Converted from international format 84: $digitsOnly")
    }

    // If doesn't start with 0 but has 9 digits, add leading 0 (Vietnamese numbers)
    if (!digitsOnly.startsWith("0") && digitsOnly.length == 9) {
        digitsOnly = "0$digitsOnly"
        Timber.tag(TAG).d("Added leading zero: $digitsOnly")
    }

    // Xử lý các đầu số cũ không còn hợp lệ của Việt Nam
    if (digitsOnly.startsWith("0") && digitsOnly.length == 10) {
        // Các đầu số cũ cần được chuyển đổi
        val oldPrefixMap = mapOf(
            "0123" to "083", "0124" to "084", "0125" to "085",
            "0127" to "081", "0129" to "082", // Vinaphone

            "0120" to "070", "0121" to "079", "0122" to "077",
            "0126" to "076", "0128" to "078", // Mobifone

            "0162" to "032", "0163" to "033", "0164" to "034",
            "0165" to "035", "0166" to "036", "0167" to "037",
            "0168" to "038", "0169" to "039", // Viettel

            "0188" to "058", "0186" to "056", // Vietnamobile

            "0199" to "059" // Gmobile
        )

        // Kiểm tra và chuyển đổi đầu số cũ
        for ((oldPrefix, newPrefix) in oldPrefixMap) {
            if (digitsOnly.startsWith(oldPrefix)) {
                val newNumber = newPrefix + digitsOnly.substring(4)
                Timber.tag(TAG)
                    .d("Converted old prefix $oldPrefix to $newPrefix: $digitsOnly → $newNumber")
                digitsOnly = newNumber
                break
            }
        }

        // Đặc biệt xử lý đầu số 013x không còn hợp lệ
        if (digitsOnly.startsWith("013")) {
            val newNumber = "083" + digitsOnly.substring(3)
            Timber.tag(TAG).d("Fixed problematic prefix 013: $digitsOnly → $newNumber")
            digitsOnly = newNumber
        }

        // Special handling for problematic prefixes that cause "Generic failure"
        if (digitsOnly.startsWith("0946") || digitsOnly.startsWith("0167")) {
            Timber.tag(TAG).d("Applied special handling for problematic prefix: $digitsOnly")
        }
    }

    // Ensure phone number is valid (Vietnamese numbers are typically 10 digits)
    if (digitsOnly.length < 10 || digitsOnly.length > 11) {
        Timber.tag(TAG).w("Warning: Unusual phone number length: ${digitsOnly.length} digits")
    }

    // Final validation - must start with 0 for domestic format
    if (!digitsOnly.startsWith("0")) {
        Timber.tag(TAG).w("Warning: Phone number doesn't start with 0: $digitsOnly")
    }

    Timber.tag(TAG).d("Formatted phone number: '$originalNumber' → '$digitsOnly'")
    return digitsOnly
}.onFailure {
    Timber.tag(TAG).e(it.stackTraceToString())
}.getOrDefault(this.trim())

fun String.isValidPhoneNumber(): Boolean = validateAndFormatPhoneNumber().let {
    // Kiểm tra xem có phải là số điện thoại Việt Nam hợp lệ không
    if (it.startsWith("0") && it.length == 10) {
        // Kiểm tra xem đầu số có hợp lệ không
        val isValidPrefix = hasValidVietnamesePrefix(it)
        if (!isValidPrefix) {
            Timber.tag(TAG).w("Warning: Vietnamese phone number with invalid prefix: $it")
        }
    }
    return it.isNotEmpty() &&
            it.startsWith("0") &&
            it.length >= 10 &&
            it.length <= 11
}

/**
 * Kiểm tra xem số điện thoại Việt Nam có đầu số hợp lệ hay không
 */
fun hasValidVietnamesePrefix(phoneNumber: String): Boolean {
    if (!phoneNumber.startsWith("0") || phoneNumber.length < 3) {
        return false
    }

    val prefix = phoneNumber.substring(0, 3)
    val validPrefixes = listOf(
        // Viettel
        "032", "033", "034", "035", "036", "037", "038", "039",
        "086", "096", "097", "098",
        // Mobifone
        "070", "076", "077", "078", "079",
        "089", "090", "093",
        // Vinaphone
        "081", "082", "083", "084", "085",
        "088", "091", "094",
        // Vietnamobile
        "056", "058", "092",
        // ITelecom
        "099",
        // Reddi/Gmobile
        "059"
    )

    return validPrefixes.contains(prefix)
}

// Replace the old simple formatPhoneNumber with the more comprehensive one
fun formatPhoneNumber(phoneNumber: String): String {
    // Kiểm tra và sửa đầu số không hợp lệ
    var formatted = phoneNumber.validateAndFormatPhoneNumber()

    // Đặc biệt xử lý đầu số 013 - không còn hợp lệ nữa, chuyển sang 083
    if (formatted.startsWith("013") && formatted.length == 10) {
        val corrected = "083" + formatted.substring(3)
        Log.d(TAG, "Corrected invalid prefix 013 to 083: $formatted -> $corrected")
        formatted = corrected
    }

    return formatted
}

/**
 * Special SMS sending method optimized for Vietnamese carriers
 * This method tries different phone number formats to avoid "Generic failure" errors
 */
fun sendSmsToVietnameseNumber(
    phoneNumber: String,
    message: String,
    sentIntent: PendingIntent? = null
): Boolean {
    Log.d(TAG, "Using Vietnamese-optimized SMS sender for: $phoneNumber")

    // First clean the number
    val cleanedNumber = phoneNumber.validateAndFormatPhoneNumber()

    // Try different formats in order of likelihood to succeed
    val formatsToTry = mutableListOf<String>()

    // Format 1: Standard domestic format (most common)
    formatsToTry.add(cleanedNumber)

    // Format 2: International format with +84
    if (cleanedNumber.startsWith("0")) {
        formatsToTry.add("+84${cleanedNumber.substring(1)}")
    }

    // Format 3: International format without +
    if (cleanedNumber.startsWith("0")) {
        formatsToTry.add("84${cleanedNumber.substring(1)}")
    }

    // Log all formats we'll try
    Log.d(TAG, "Will try these formats: $formatsToTry")

    // Try each format until one succeeds
    for ((index, format) in formatsToTry.withIndex()) {
        try {
            Log.d(TAG, "Attempt ${index + 1}/${formatsToTry.size}: Sending SMS to format: $format")

            // Get SmsManager instance
            val smsManager = getDefaultMethod?.invoke(null)

            // Send the SMS
            sendTextMessageMethod?.invoke(
                smsManager,
                format,
                null,
                message,
                sentIntent,
                null
            )

            // If we get here, the SMS was sent successfully
            Log.d(TAG, "✅ Successfully sent SMS using format: $format")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send SMS using format: $format", e)
            // Continue to next format
        }
    }

    // If we get here, all formats failed
    Log.e(TAG, "❌ All formats failed for number: $phoneNumber")
    return false
}
