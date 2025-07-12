package com.example.sms_app.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.system.exitProcess

enum class MoreVertFunctions(val icon: ImageVector, val text: String) {
    Home(Icons.Filled.Home, "Trang chủ"),
    Support(Icons.Filled.Call, "Hỗ trợ"),
    Filter(Icons.Filled.FilterAlt, "Xóa lặp"),
    Random(Icons.Filled.ChangeCircle, "Ngẫu nhiên"),
    Previous(Icons.Filled.SettingsBackupRestore, "Phiên trước"),
    Update(Icons.Filled.CloudDownload, "Cập nhật"),
    Info(Icons.Filled.Info, "Thông tin"),
    Out(Icons.AutoMirrored.Filled.ExitToApp, "Thoát"),
}

@Composable
fun MoreView(button: BottomButton, onDismissRequest: () -> Unit) {
    DropdownMenu(
        button == BottomButton.MoreVert,
        onDismissRequest = onDismissRequest,
    ) {
        MoreVertFunctions.entries.forEach {
            DropdownMenuItem(
                leadingIcon = {
                    Icon(it.icon, null)
                },
                text = {
                    Text(it.text)
                },
                onClick = {
                    when (it) {
                        MoreVertFunctions.Home -> {}
                        MoreVertFunctions.Support -> {}
                        MoreVertFunctions.Filter -> {}
                        MoreVertFunctions.Random -> {}
                        MoreVertFunctions.Previous -> {}
                        MoreVertFunctions.Update -> {}
                        MoreVertFunctions.Info -> {}
                        MoreVertFunctions.Out -> {
                            exitProcess(0)
                        }
                    }
                }
            )
            HorizontalDivider()
        }
    }
}