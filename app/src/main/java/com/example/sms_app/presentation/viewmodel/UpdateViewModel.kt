package com.example.sms_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sms_app.utils.AppUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {
    
    private val _updateInfo = MutableStateFlow<AppUpdateManager.UpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateManager.UpdateInfo?> = _updateInfo.asStateFlow()
    
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()
    
    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()
    
    init {
        // Đảm bảo dialog không hiển thị khi khởi tạo
        _showUpdateDialog.value = false
        _updateInfo.value = null
    }
    
    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val update = appUpdateManager.checkForUpdates()
                if (update != null) {
                    _updateInfo.value = update
                    _showUpdateDialog.value = true
                    Timber.d("Update available: ${update.versionName}")
                } else {
                    Timber.d("No update available")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking for updates")
            }
        }
    }
    
    fun startUpdate() {
        val update = _updateInfo.value ?: return

        // Kiểm tra permission trước khi download
        if (!appUpdateManager.canInstallApk()) {
            Timber.w("⚠️ App installation permission not granted")
            // Có thể hiển thị dialog yêu cầu permission hoặc mở settings
            appUpdateManager.openInstallPermissionSettings()
            return
        }

        viewModelScope.launch {
            try {
                _isDownloading.value = true
                _downloadProgress.value = 0f

                Timber.d("🚀 Starting update download...")

                val success = appUpdateManager.downloadAndInstallUpdate(update) { progress ->
                    _downloadProgress.value = progress
                    Timber.d("📥 Download progress: ${(progress * 100).toInt()}%")
                }

                if (success) {
                    Timber.d("✅ Update download successful")
                    // The app will close and update process will begin
                } else {
                    Timber.e("❌ Update download failed")
                    _isDownloading.value = false
                    _downloadProgress.value = 0f
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error during update")
                _isDownloading.value = false
                _downloadProgress.value = 0f
            }
        }
    }
    
    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }
    
    fun getCurrentVersion(): String {
        return appUpdateManager.getCurrentVersionName()
    }

    suspend fun testApiConnection() {
        appUpdateManager.testApiConnection()
    }
}