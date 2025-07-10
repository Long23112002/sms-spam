package com.example.sms_app.presentation.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.sms_app.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SwitchSetting(val text: String, val default: Boolean) {
    Vibrate("Rung khi gửi", false),
    Audio("Âm báo khi gửi", true),
    FilterHead("Lọc số nhà mạng ảo", false),
    Random("Gửi ngẫu nhiên", false),
    Limit("Gới hạn số khách hàng", false),
    Update("Cập nhật", true),
}

enum class NumSetting(val text: String, val default: Int) {
    Delay("Thời gian chờ", 25),
    Limit("Giới hạn thất bại liên tiếp", 10),
}

enum class TextSetting(val text: String, val default: String) {
    Activation(
        "Mã kích hoạt",
        "Hết hạn ngày ${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale("vi")).format(Date())}"
    ),
    Permission("Quản lý quyền", "Quyền, Dữ liệu, Thông báo"),
    Info("Thông tin phiên bản", "v${BuildConfig.VERSION_NAME}-${BuildConfig.APP_SIGNATURE}"),
    Others("Ứng dụng khác của team", "Xem thêm Ứng dụng khác của team"),
    History("Lịch sử tin nhắn trong ngày", "Không có dữ liệu ${SimpleDateFormat("dd-MM-yyyy", Locale("vi")).format(Date())}"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDialog(onDismissRequest: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Card(Modifier.fillMaxSize()) {
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
                        var check by remember {
                            mutableStateOf(false)
                        }
                        ListItem(
                            headlineContent = {
                                Text(item.text)
                            },
                            supportingContent = {
                                Text(if (check) "Bật" else "Tắt")
                            },
                            trailingContent = {
                                Switch(check, onCheckedChange = { check = it })
                            }
                        )
                        HorizontalDivider()
                    }

                    items(NumSetting.entries.size) { index: Int ->
                        val item = NumSetting.entries[index]

                        ListItem(
                            headlineContent = {
                                Text(item.text)
                            },
                            supportingContent = {
                                Text(item.default.toString())
                            },
                            trailingContent = {
                                IconButton(onClick = {}) {
                                    Icon(Icons.Default.QuestionMark, null)
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
}