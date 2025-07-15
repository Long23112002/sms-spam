package com.example.sms_app.presentation.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.presentation.PermissionChecker
import com.example.sms_app.presentation.screen.MainScreen
import com.example.sms_app.presentation.theme.SmsAppTheme
import com.example.sms_app.presentation.viewmodel.SendMessageViewModel
import com.example.sms_app.service.SmsService
import com.example.sms_app.utils.AutoSmsDisabler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : androidx.activity.ComponentActivity() {
    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Đảm bảo ứng dụng luôn ở chế độ an toàn - không tự động gửi SMS
        AutoSmsDisabler.initializeSafeMode(this)

        setContent {
            SmsAppTheme {
                Surface {
                    var isOk by remember {
                        mutableStateOf(false)
                    }
                    
                    // Get SendMessageViewModel để kiểm tra trạng thái gửi SMS
                    val sendMessageViewModel: SendMessageViewModel = hiltViewModel()
                    val isSending = sendMessageViewModel.isSending.observeAsState(false)
                    val millisUntilFinished = sendMessageViewModel.millisUntilFinished.observeAsState(0L)
                    
                    // Xử lý khi user nhấn back button
                    BackHandler {
                        if (isSending.value == true && millisUntilFinished.value > 0) {
                            // Nếu đang gửi SMS, hiển thị cảnh báo và dừng service
                            android.util.Log.d("MainActivity", "⚠️ User attempted to exit while sending SMS - stopping service")
                            
                            // Hiển thị toast cảnh báo
                            Toast.makeText(
                                this@MainActivity,
                                "⚠️ Đã dừng quá trình gửi SMS do thoát ứng dụng!",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Dừng SmsService
                            sendMessageViewModel.stop()
                            
                            // Dừng service bằng intent
                            val stopIntent = Intent(this@MainActivity, SmsService::class.java).apply {
                                action = "STOP_SMS_SERVICE"
                            }
                            stopService(stopIntent)
                            
                            // Thoát app
                            finish()
                        } else {
                            // Không đang gửi SMS, thoát bình thường
                            android.util.Log.d("MainActivity", "✅ Normal app exit - not sending SMS")
                            finish()
                        }
                    }
                    
                    PermissionChecker(
                        permissions = listOf(
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_PHONE_STATE
                        ),
                        intents = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) listOf(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                        ) else listOf()
                    ) {
                        isOk = true
                    }
                    if (isOk) {
                        MainScreen()
                    }
                }
            }
        }
    }
}