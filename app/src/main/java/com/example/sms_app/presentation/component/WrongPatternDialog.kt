package com.example.sms_app.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun WrongPatternDialog(
    onDismissRequest: () -> Unit,
    confirm: () -> Unit,
    continueMessage: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismissRequest() },
        icon = {
            Icon(Icons.Default.Error, null)
        },
        title = {
            Text("Cảnh báo nội dung")
        },
        text = {
            Text("Tin nhắn mẫu không chứa các kí tự thay thế trong nội dung nếu tiếp tục gửi tin nhắn nhà mạng có thể đưa sim của bạn vào danh sách theo dõi chặn nhắn tin. Bạn vẫn muốn tiếp tục gửi tin nhắn?")
        },
        confirmButton = {
            TextButton(onClick = { confirm() }) {
                Text("SỬA MẪU TIN")
            }
        },
        dismissButton = {
            TextButton(onClick = { continueMessage() }) {
                Text("TIẾP TỤC")
            }
        }
    )
}