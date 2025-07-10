package com.example.sms_app.presentation.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.sms_app.presentation.screen.MainScreen
import com.example.sms_app.presentation.theme.SmsAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity: androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmsAppTheme {
                Surface {
                    MainScreen()
                }
            }
        }
    }
}