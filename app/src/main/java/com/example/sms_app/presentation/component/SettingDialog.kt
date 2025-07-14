package com.example.sms_app.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.BuildConfig
import com.example.sms_app.data.AppSettings
import com.example.sms_app.presentation.viewmodel.SettingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SwitchSetting(val text: String, val default: Boolean) {
    Limit("Giới hạn số khách hàng", false),
    Update("Cập nhật", true);

    fun getValue(appSettings: AppSettings?): Boolean = when (this) {
        Limit -> appSettings?.isLimitCustomer ?: default
        Update -> appSettings?.enableUpdate ?: default
    }
}

enum class NumSetting(val text: String, val default: Int) {
    Delay("Thời gian chờ", 25),
    Limit("Giới hạn thất bại liên tiếp", 10);

    fun getValue(appSettings: AppSettings?): String = when (this) {
        Delay -> {
            appSettings?.intervalBetweenSmsSeconds?.toString()
                ?: default.toString()
        }

        Limit -> {
            appSettings?.maxRetryAttempts?.toString() ?: default.toString()
        }
    }
}

enum class TextSetting(val text: String, val default: String) {
    Activation(
        "Mã kích hoạt",
        "Trịnh Thị Bình An"
    ),
    Permission("Quản lý quyền", "Quyền, Dữ liệu, Thông báo"),
    Info("Thông tin phiên bản", "v${BuildConfig.VERSION_NAME}-${BuildConfig.APP_SIGNATURE}"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDialog(
    settingViewModel: SettingViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit
) {
    val appSettings = settingViewModel.appSettings.observeAsState().value
    val context = LocalContext.current
    var setting by remember {
        mutableStateOf("")
    }
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth(1f)
                .wrapContentHeight()
                .heightIn(max = 700.dp)
                .padding(8.dp)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Cài đặt ứng dụng")
                        },
                        actions = {
                            IconButton(onClick = {
                                onDismissRequest()
                            }) {
                                Icon(Icons.Default.Close, null)
                            }
                        })
                },
                bottomBar = {
                    BottomAppBar {
                        TextButton(onClick = {}, Modifier.fillMaxWidth()) {
                            Text(
                                "Xóa lịch sử nhắn tin".uppercase(),
                                Modifier.fillMaxWidth(),
                                style = TextStyle(textAlign = TextAlign.Center)
                            )
                        }
                    }
                }

            ) { paddingValues ->
                LazyColumn(Modifier.padding(paddingValues)) {
                    item {
                        HorizontalDivider()
                    }
                    items(SwitchSetting.entries.size) { index: Int ->
                        val item = SwitchSetting.entries[index]
                        var check by remember(appSettings) {
                            mutableStateOf(item.getValue(appSettings))
                        }
                        var showLimitDialog by remember {
                            mutableStateOf(false)
                        }
                        var limitValue by remember(appSettings) {
                            mutableStateOf(appSettings?.customerLimit?.toString() ?: "20")
                        }

                        Column {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = item.text,
                                        modifier = if (item == SwitchSetting.Limit && check) {
                                            Modifier.clickable {
                                                showLimitDialog = true
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = if (item == SwitchSetting.Limit && check) {
                                            "Bật - Giới hạn: ${appSettings?.customerLimit ?: 20} khách hàng"
                                        } else if (check) {
                                            "Bật"
                                        } else {
                                            "Tắt"
                                        },
                                        modifier = if (item == SwitchSetting.Limit && check) {
                                            Modifier.clickable {
                                                showLimitDialog = true
                                            }
                                        } else {
                                            Modifier
                                        }
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        check, onCheckedChange = {
                                            check = it
                                            settingViewModel.saveBool(item, it)
                                            if (item == SwitchSetting.Limit && it) {
                                                showLimitDialog = true
                                            }
                                        }
                                    )
                                }
                            )

                            // Modal dialog cho nhập giới hạn số khách hàng
                            if (item == SwitchSetting.Limit && showLimitDialog) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showLimitDialog = false
                                        // Nếu user đóng dialog mà chưa nhập, tắt switch
                                        check = false
                                        settingViewModel.saveBool(item, false)
                                    },
                                    title = {
                                        Text("Giới hạn số khách hàng")
                                    },
                                    text = {
                                        Column {
                                            Text("Nhập số lượng khách hàng tối đa:")
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = limitValue,
                                                onValueChange = { limitValue = it },
                                                label = { Text("Số lượng") },
                                                placeholder = { Text("Ví dụ: 20") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                val number = limitValue.toIntOrNull()
                                                if (number != null && number > 0) {
                                                    settingViewModel.saveCustomerLimit(number)
                                                    showLimitDialog = false
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Đã lưu giới hạn: $number khách hàng",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Vui lòng nhập số hợp lệ (> 0)",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        ) {
                                            Text("Lưu")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showLimitDialog = false
                                                // Tắt switch nếu user hủy
                                                check = false
                                                settingViewModel.saveBool(item, false)
                                            }
                                        ) {
                                            Text("Hủy")
                                        }
                                    }
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    items(NumSetting.entries.size) { index: Int ->
                        val item = NumSetting.entries[index]
                        ListItem(
                            headlineContent = {
                                Text(item.text)
                            },
                            supportingContent = {
                                Text(item.getValue(appSettings))
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    setting = item.name
                                }) {
                                    Icon(Icons.Default.Edit, null)
                                }
                            }
                        )
                        HorizontalDivider()
                    }

                    items(TextSetting.entries.size) { index: Int ->
                        val item = TextSetting.entries[index]

                        ListItem(
                            headlineContent = {
                                Text(item.text)
                            },
                            supportingContent = {
                                Text(item.default)
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }


    when {
        NumSetting.entries.any { setting == it.name } -> {
            val item = NumSetting.valueOf(setting)
            EditValueDialog(
                onDismissRequest = {
                    setting = ""
                },
                title = item.text,
                itemValue = item.getValue(appSettings),
                keyboardType = KeyboardType.Number
            ) {
                when (item) {
                    NumSetting.Delay -> settingViewModel.saveDelay(it)
                    NumSetting.Limit -> settingViewModel.saveRetry(it)
                }

                setting = ""
            }
        }

        else -> {}
    }
}

@Composable
fun EditValueDialog(
    onDismissRequest: () -> Unit,
    title: String,
    itemValue: String,
    keyboardType: KeyboardType,
    confirm: (String) -> Unit,
) {
    var value by remember {
        mutableStateOf(itemValue)
    }
    AlertDialog(
        onDismissRequest = {
            onDismissRequest()
        },
        icon = {
            Icon(Icons.Default.Edit, null)
        },
        title = {
            Text(title)
        },
        text = {
            TextField(
                value,
                onValueChange = {
                    value = it
                },
                label = {
                    Text("Sửa")
                },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                confirm(value)
            }) {
                Text("lưu".uppercase())
            }
        }
    )
}
