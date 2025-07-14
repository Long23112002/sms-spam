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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.system.exitProcess

enum class MoreVertFunctions(val icon: ImageVector, val text: String) {
    Home(Icons.Filled.Home, "Trang chủ"),
    Support(Icons.Filled.Call, "Hỗ trợ"),
    Filter(Icons.Filled.FilterAlt, "Xóa lặp"),
    Update(Icons.Filled.CloudDownload, "Cập nhật"),
    Out(Icons.AutoMirrored.Filled.ExitToApp, "Thoát"),
}

@Composable
fun MoreView(
    button: BottomButton,
    onDismissRequest: () -> Unit,
    onRemoveDuplicates: () -> Unit = {},
    onRestoreUnsentCustomers: () -> Unit = {},
    onUpdateClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onSupportClick: () -> Unit = {}
) {
    DropdownMenu(
        expanded = button == BottomButton.MoreVert,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .widthIn(min = 200.dp)
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
                        MoreVertFunctions.Home -> {
                            onHomeClick()
                            onDismissRequest()
                        }
                        MoreVertFunctions.Support -> {
                            onSupportClick()
                            onDismissRequest()
                        }
                        MoreVertFunctions.Filter -> {
                            onRemoveDuplicates()
                            onDismissRequest()
                        }
//                        MoreVertFunctions.Random -> {}
//                        MoreVertFunctions.Previous -> {
//                            onRestoreUnsentCustomers()
//                            onDismissRequest()
//                        }
                        MoreVertFunctions.Update -> {
                            onUpdateClick()
                            onDismissRequest()
                        }
//                        MoreVertFunctions.Info -> {}
//                        MoreVertFunctions.Out -> {
//                            exitProcess(0)
//                        }
//                        MoreVertFunctions.Previous -> TODO()
                        MoreVertFunctions.Out -> TODO()
                    }
                }
            )
            HorizontalDivider()
        }
    }
}