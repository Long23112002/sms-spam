package com.example.sms_app.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.sms_app.api.UpdateApiService
import com.example.sms_app.api.VersionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val BASE_URL = "http://42.96.16.211:6868/"
        private const val CURRENT_VERSION = "1.0.0"
        private const val CURRENT_VERSION_CODE = 1
    }

    private val apiService: UpdateApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateApiService::class.java)
    }
    
    data class UpdateInfo(
        val versionCode: Int = 0,
        val versionName: String = "",
        val downloadUrl: String = "",
        val releaseNotes: String = "",
        val forceUpdate: Boolean = false,
        val minSupportedVersion: Int = 0,
        val fileSize: Long = 0
    )
    
    suspend fun checkForUpdates(): UpdateInfo? {
        return try {
            Timber.d("🔍 Checking for updates from API...")
            Toast.makeText(context, "Đang kiểm tra cập nhật...", Toast.LENGTH_SHORT).show()

            // Call API để lấy version mới nhất
            val response = apiService.getLatestVersion()

            if (response.isSuccessful) {
                val versionResponse = response.body()
                Timber.d("📊 API response: $versionResponse")

                if (versionResponse != null) {
                    val currentVersionCode = CURRENT_VERSION_CODE
                    val currentVersionName = CURRENT_VERSION

                    Timber.d("📱 Current version: $currentVersionName ($currentVersionCode)")
                    Timber.d("🆕 Latest version: ${versionResponse.version} (${versionResponse.versionCode})")

                    if (versionResponse.versionCode > currentVersionCode) {
                        Timber.d("✅ Update available!")
                        Toast.makeText(context, "🚀 Có cập nhật mới v${versionResponse.version}!", Toast.LENGTH_SHORT).show()

                        // Convert VersionResponse to UpdateInfo
                        return UpdateInfo(
                            versionCode = versionResponse.versionCode,
                            versionName = versionResponse.version,
                            downloadUrl = BASE_URL + "download",
                            releaseNotes = versionResponse.releaseNotes.ifEmpty {
                                "🚀 Cập nhật mới v${versionResponse.version}\n\n✅ Cải thiện hiệu suất\n✅ Sửa lỗi và tối ưu hóa"
                            },
                            forceUpdate = versionResponse.forceUpdate,
                            minSupportedVersion = currentVersionCode,
                            fileSize = versionResponse.fileSize
                        )
                    } else {
                        Timber.d("✅ App is up to date")
                        Toast.makeText(context, "✅ Đã sử dụng phiên bản mới nhất", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Timber.w("❌ API response body is null")
                    Toast.makeText(context, "❌ Không nhận được dữ liệu từ server", Toast.LENGTH_SHORT).show()
                }
            } else if (response.code() == 404) {
                // 404 có nghĩa là không có version info - tức là không có update
                Timber.d("✅ No update available (404 response)")
                Toast.makeText(context, "✅ Đã sử dụng phiên bản mới nhất", Toast.LENGTH_SHORT).show()
            } else {
                Timber.w("❌ API call failed: ${response.code()} - ${response.message()}")
                Toast.makeText(context, "❌ Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "❌ Error checking for updates")
            // App vẫn chạy bình thường khi API lỗi - chỉ log và hiển thị toast ngắn
            when {
                e.message?.contains("CLEARTEXT communication", ignoreCase = true) == true -> {
                    Toast.makeText(context, "🔒 Lỗi bảo mật mạng - vui lòng thử lại", Toast.LENGTH_SHORT).show()
                }
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    Toast.makeText(context, "⏰ Timeout khi kiểm tra cập nhật", Toast.LENGTH_SHORT).show()
                }
                e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("connection", ignoreCase = true) == true ||
                e.message?.contains("UnknownServiceException", ignoreCase = true) == true -> {
                    Toast.makeText(context, "📶 Không thể kết nối server cập nhật", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(context, "⚠️ Không thể kiểm tra cập nhật", Toast.LENGTH_SHORT).show()
                }
            }
            // Trả về null để app tiếp tục hoạt động bình thường
            null
        }
    }
    
    suspend fun downloadAndInstallUpdate(
        updateInfo: UpdateInfo,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        return try {
            val apkFile = File(context.getExternalFilesDir(null), "update.apk")

            // Xóa file cũ nếu có
            if (apkFile.exists()) {
                apkFile.delete()
            }

            Toast.makeText(context, "Bắt đầu tải xuống cập nhật...", Toast.LENGTH_SHORT).show()
            Timber.d("🚀 Starting download from: ${updateInfo.downloadUrl}")

            // Download APK từ API
            val response = apiService.downloadApk()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    withContext(Dispatchers.IO) {
                        val inputStream: InputStream = body.byteStream()
                        val outputStream = FileOutputStream(apkFile)

                        val totalBytes = body.contentLength()
                        var downloadedBytes = 0L
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            // Update progress
                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                onProgress(progress)
                                Timber.d("📥 Download progress: ${(progress * 100).toInt()}%")
                            }
                        }

                        outputStream.close()
                        inputStream.close()
                    }

                    Toast.makeText(context, "✅ Tải xuống hoàn thành! Đang cài đặt...", Toast.LENGTH_SHORT).show()
                    Timber.d("✅ Download completed: ${apkFile.absolutePath}")

                    // Install APK
                    installApk(apkFile)
                    true
                } else {
                    Timber.e("❌ Download response body is null")
                    Toast.makeText(context, "❌ Lỗi tải xuống: Không nhận được dữ liệu", Toast.LENGTH_LONG).show()
                    false
                }
            } else {
                Timber.e("❌ Download failed: ${response.code()} - ${response.message()}")
                Toast.makeText(context, "❌ Lỗi tải xuống: ${response.code()}", Toast.LENGTH_LONG).show()
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error downloading update")
            when {
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    Toast.makeText(context, "⏰ Timeout khi tải xuống", Toast.LENGTH_LONG).show()
                }
                e.message?.contains("network", ignoreCase = true) == true -> {
                    Toast.makeText(context, "📶 Lỗi kết nối khi tải xuống", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(context, "❌ Lỗi tải xuống: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            false
        }
    }
    
    private fun installApk(apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                },
                "application/vnd.android.package-archive"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(intent)
    }
    
    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Could not get version code")
            0
        }
    }
    
    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Could not get version name")
            "Unknown"
        }
    }

    /**
     * Kiểm tra xem app có thể cài đặt APK không
     */
    fun canInstallApk(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Mở settings để cho phép cài đặt APK
     */
    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Test API connection
     */
    suspend fun testApiConnection(): Boolean {
        return try {
            Timber.d("🧪 Testing API connection...")
            Toast.makeText(context, "🧪 Testing API...", Toast.LENGTH_SHORT).show()

            // Test API version endpoint
            val response = apiService.getLatestVersion()

            if (response.isSuccessful) {
                val versionResponse = response.body()
                Timber.d("🔗 API connected successfully")
                Timber.d("📊 Version data: $versionResponse")

                Toast.makeText(context, "✅ API OK! Version: ${versionResponse?.version ?: "Unknown"}", Toast.LENGTH_LONG).show()
                true
            } else {
                Timber.w("❌ API test failed: ${response.code()} - ${response.message()}")
                Toast.makeText(context, "❌ API Error: ${response.code()}", Toast.LENGTH_LONG).show()
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ API test failed")
            Toast.makeText(context, "❌ API test error: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }
}