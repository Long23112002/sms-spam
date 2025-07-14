package com.example.sms_app.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming

/**
 * API service cho việc kiểm tra và tải xuống cập nhật app
 */
interface UpdateApiService {
    
    /**
     * Lấy thông tin version mới nhất từ server
     * GET http://42.96.16.211:6868/version
     */
    @GET("version")
    suspend fun getLatestVersion(): Response<VersionResponse>
    
    /**
     * Tải xuống file APK mới nhất
     * GET http://42.96.16.211:6868/download
     */
    @Streaming
    @GET("download")
    suspend fun downloadApk(): Response<ResponseBody>
}

/**
 * Response model cho API version
 */
data class VersionResponse(
    val version: String = "1.0.0",
    val versionCode: Int = 1,
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false,
    val downloadUrl: String = "",
    val fileSize: Long = 0
)
