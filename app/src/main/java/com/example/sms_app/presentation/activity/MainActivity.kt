package com.example.sms_app.presentation.activity

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sms_app.presentation.PermissionChecker
import com.example.sms_app.presentation.screen.MainScreen
import com.example.sms_app.presentation.theme.SmsAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : androidx.activity.ComponentActivity() {
    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmsAppTheme {
                Surface {
                    var isOk by remember {
                        mutableStateOf(false)
                    }
                    PermissionChecker(
                        permissions = listOf(
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_PHONE_STATE
                        ),
                        intents = listOf(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
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