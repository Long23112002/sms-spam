package com.example.sms_app.presentation.component

import android.annotation.SuppressLint
import android.os.CountDownTimer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.data.SmsProgress
import com.example.sms_app.data.AppSettings
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.presentation.viewmodel.PatternViewModel
import com.example.sms_app.presentation.viewmodel.SendMessageViewModel
import com.example.sms_app.presentation.viewmodel.SettingViewModel
import com.example.sms_app.utils.SimInfo
import com.example.sms_app.utils.SimConfig
import com.example.sms_app.utils.SimManager

@SuppressLint("MissingPermission")
@Composable
fun SendMessageDialog(
    messageTemplate: MessageTemplate,
    simConfig: SimConfig,
    sendMessageViewModel: SendMessageViewModel,
    onDismissRequest: () -> Unit
) {
    val time = remember {
        mutableStateListOf<Long>().apply {
            for (i in 0..3) {
                add(0L)
            }
        }
    }
    val millisUntilFinished = sendMessageViewModel.millisUntilFinished.observeAsState(0L).value
    val isSending = sendMessageViewModel.isSending.observeAsState(false).value
    val progress = sendMessageViewModel.progress.observeAsState(SmsProgress()).value
    val completion = sendMessageViewModel.completion.observeAsState().value
    val context = LocalContext.current

    // Debug logging
    LaunchedEffect(millisUntilFinished) {
        android.util.Log.d(
            "SendMessageDialog",
            "🔄 Countdown update: ${millisUntilFinished}ms (${millisUntilFinished / 1000}s)"
        )
    }

    // Giữ màn hình sáng khi đang countdown/gửi SMS
    LaunchedEffect(isSending, millisUntilFinished) {
        if (isSending && millisUntilFinished > 0) {
            // Giữ màn hình sáng
            android.util.Log.d("SendMessageDialog", "💡 Keeping screen on during countdown/sending")
            if (context is android.app.Activity) {
                context.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } else {
            // Tắt keep screen on khi hoàn thành
            android.util.Log.d("SendMessageDialog", "💡 Removing keep screen on flag")
            if (context is android.app.Activity) {
                context.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val formattedTime = try {
        millisUntilFinished.formatDuration()
    } catch (e: Exception) {
        listOf(0L, 0L, 0L, 0L)
    }

    time.apply {
        clear()
        addAll(formattedTime)
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("SendMessageDialog", "🚀 Dialog launched")

        // Debug SIM config
        android.util.Log.d("SendMessageDialog", "📱 SIM Config: isDualSim=${simConfig.isDualSim}")
        android.util.Log.d(
            "SendMessageDialog",
            "📱 Primary SIM: ${simConfig.primarySim.displayName} (slot ${simConfig.primarySim.simSlotIndex}, id ${simConfig.primarySim.subscriptionId})"
        )
        if (simConfig.isDualSim) {
            simConfig.allSims.forEachIndexed { index, sim ->
                android.util.Log.d(
                    "SendMessageDialog",
                    "📱 SIM $index: ${sim.displayName} (slot ${sim.simSlotIndex}, id ${sim.subscriptionId})"
                )
            }
        }

        // Validate mẫu tin nhắn trước khi gửi
        if (messageTemplate.content.isBlank()) {
            android.util.Log.e("SendMessageDialog", "❌ Message template is empty")
            android.widget.Toast.makeText(
                context,
                "❌ Vui lòng cấu hình mẫu tin nhắn trước khi gửi!",
                android.widget.Toast.LENGTH_LONG
            ).show()
            // Đóng dialog nếu không có mẫu tin nhắn
            onDismissRequest()
            return@LaunchedEffect
        }

        android.util.Log.d(
            "SendMessageDialog",
            "✅ Message template validated, starting sendMessage"
        )
        sendMessageViewModel.sendMessage(messageTemplate, simConfig)
    }

    // Auto-close modal khi hoàn thành
    LaunchedEffect(completion) {
        completion?.let {
            android.util.Log.d("SendMessageDialog", "✅ SMS completion received: $it")
            // Tắt keep screen on khi hoàn thành
            if (context is android.app.Activity) {
                context.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            // Đợi 2 giây rồi tự động đóng dialog
            kotlinx.coroutines.delay(2000)
            onDismissRequest()
        }
    }
    AlertDialog(
        onDismissRequest = {
            // Ngăn đóng dialog bằng cách touch outside khi đang gửi SMS
            if (isSending && millisUntilFinished > 0 && completion == null) {
                android.util.Log.d(
                    "SendMessageDialog",
                    "⚠️ Prevented dialog dismissal during SMS sending"
                )
                // Hiển thị cảnh báo
                android.widget.Toast.makeText(
                    context,
                    "⚠️ Không thể đóng khi đang gửi SMS! Hãy dùng nút 'Dừng gửi'",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                // Tắt keep screen on khi đóng dialog
                if (context is android.app.Activity) {
                    context.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    android.util.Log.d(
                        "SendMessageDialog",
                        "💡 Cleared keep screen on flag on dismiss"
                    )
                }
                onDismissRequest()
            }
        },
        icon = {
            if (completion != null) {
                // SMS đã hoàn thành - hiển thị tick xanh
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Green
                )
            } else {
                // Đang gửi SMS - hiển thị progress indicator
                CircularProgressIndicator()
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val titleText = if (completion != null) {
                    "✅ Hoàn thành gửi SMS"
                } else if (isSending && millisUntilFinished > 0) {
                    "Đang gửi tin nhắn..."
                } else {
                    " Đang chuẩn bị gửi SMS..."
                }

                Text(titleText)

                // Hiển thị progress tracking (1/10 người)
                if (progress.total > 0) {
                    Text(
                        text = "Đã gửi: ${progress.progress}/${progress.total} người",
                        fontSize = 12.sp,
                        color = if (completion != null) Color.Green else Color.Blue
                    )
                }

                if (millisUntilFinished == 0L && !isSending && completion == null && progress.total > 0) {
                    Text(
                        text = "⏳ Đang chờ xác nhận hoàn thành...",
                        fontSize = 12.sp,
                        color = Color.Blue
                    )
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // SIM info với màu xanh dương
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SimCard,
                        contentDescription = null,
                        tint = Color(0xFF2196F3) // Màu xanh dương
                    )
                    Spacer(Modifier.width(4.dp))
                    if (simConfig.isDualSim) {
                        Text(
                            "DUAL",
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        // Hiển thị SIM dựa trên carrier name
                        val simText = when {
                            simConfig.primarySim.carrierName.contains(
                                "Viettel",
                                ignoreCase = true
                            ) -> "SIM ${simConfig.primarySim.simSlotIndex + 1} - VIETTEL"

                            simConfig.primarySim.carrierName.contains(
                                "Vinaphone",
                                ignoreCase = true
                            ) -> "SIM ${simConfig.primarySim.simSlotIndex + 1} - VINAPHONE"

                            simConfig.primarySim.carrierName.contains(
                                "Mobifone",
                                ignoreCase = true
                            ) -> "SIM ${simConfig.primarySim.simSlotIndex + 1} - MOBIFONE"

                            else -> "SIM ${simConfig.primarySim.simSlotIndex + 1}"
                        }
                        Text(
                            simText,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Row chứa NORMAL và Mẫu tin
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // NORMAL với màu xanh lá
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50), // Màu xanh lá
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "NORMAL",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Mẫu tin với màu hồng
                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Icon(
//                            Icons.AutoMirrored.Filled.Message,
//                            contentDescription = null,
//                            tint = Color(0xFFE91E63), // Màu hồng
//                            modifier = Modifier.size(16.dp)
//                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Mẫu tin ${messageTemplate.id}",
                            color = Color(0xFFE91E63),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                // Hiển thị progress message từ SmsService
                if (progress.message.isNotEmpty()) {
                    Text(
                        text = progress.message,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Thời gian chi tiết:",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val labels = listOf("", "Giờ", "Phút", "Giây") // Bỏ days
                        time.forEachIndexed { id, item ->
                            if (id > 0) { // Bỏ qua index 0 (days)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .background(
                                                Color.Black,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "%02d".format(item),
                                            color = Color.White,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Text(
                                        labels[id],
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                if (id < time.size - 1) {
                                    Text(":", modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Tắt keep screen on khi đóng dialog
                if (context is android.app.Activity) {
                    context.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDismissRequest()
            }) {
                Text("Đóng")
            }
        },
        dismissButton = {
            // Chỉ hiển thị nút dừng khi đang gửi
            if (isSending && completion == null) {
                TextButton(onClick = {
                    android.util.Log.d("SendMessageDialog", "⛔ User clicked stop button")

                    // Hiển thị cảnh báo
                    android.widget.Toast.makeText(
                        context,
                        "⚠️ Đã dừng quá trình gửi SMS!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    // Tắt keep screen on
                    if (context is android.app.Activity) {
                        context.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }

                    sendMessageViewModel.stop()
                    onDismissRequest()
                }) {
                    Text("🛑 Dừng gửi", color = Color.Red)
                }
            }
        }
    )
}

fun Long.formatDuration(): List<Long> {
    val seconds = maxOf(0L, this / 1000) // Đảm bảo không âm
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return listOf(
        0L, // không hiển thị days
        hours,
        minutes,
        secs
    )
}