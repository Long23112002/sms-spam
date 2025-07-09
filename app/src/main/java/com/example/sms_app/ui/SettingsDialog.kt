package com.example.sms_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sms_app.data.AppSettings
import com.example.sms_app.ui.components.AboutScreen

@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit
) {
    var intervalSeconds by remember { mutableStateOf(currentSettings.intervalBetweenSmsSeconds.toString()) }
    var maxRetry by remember { mutableStateOf(currentSettings.maxRetryAttempts.toString()) }
    var retryDelay by remember { mutableStateOf(currentSettings.retryDelaySeconds.toString()) }
    var minIntervalSeconds by remember { mutableStateOf(currentSettings.minIntervalSeconds.toString()) }
    var maxIntervalSeconds by remember { mutableStateOf(currentSettings.maxIntervalSeconds.toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2196F3))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "⚙️ Cài đặt hệ thống",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color.White
                        )
                    }
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SMS Timing Settings
                    SettingSection(
                        title = "⏱️ Cài đặt thời gian gửi SMS",
                        icon = Icons.Default.AccessTime,
                        iconColor = Color(0xFF4CAF50)
                    ) {
                        SettingItem(
                            label = "Thời gian giữa các tin nhắn (giây)",
                            value = intervalSeconds,
                            onValueChange = { 
                                if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() >= 1)) {
                                    intervalSeconds = it
                                }
                            },
                            icon = Icons.Default.Schedule,
                            helper = "Khuyến nghị: 25-30 giây để tránh spam"
                        )
                    }
                    
                    Divider()
                    
                    // Retry Settings
                    SettingSection(
                        title = "🔄 Cài đặt thử lại khi thất bại",
                        icon = Icons.Default.Refresh,
                        iconColor = Color(0xFFFF9800)
                    ) {
                        SettingItem(
                            label = "Số lần thử lại tối đa",
                            value = maxRetry,
                            onValueChange = { 
                                if (it.isEmpty() || (it.toIntOrNull() in 0..10)) {
                                    maxRetry = it
                                }
                            },
                            icon = Icons.Default.Refresh,
                            helper = "Số lần thử lại khi gửi SMS thất bại"
                        )
                        
                        SettingItem(
                            label = "Thời gian chờ giữa các lần thử (giây)",
                            value = retryDelay,
                            onValueChange = { 
                                if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() >= 1)) {
                                    retryDelay = it
                                }
                            },
                            icon = Icons.Default.Timer,
                            helper = "Thời gian chờ trước khi thử lại"
                        )
                    }
                    
                    Divider()
                    
                    // SIM Settings
                    SettingSection(
                        title = "📱 Cài đặt SIM",
                        icon = Icons.Default.SimCard,
                        iconColor = Color(0xFF2196F3)
                    ) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val smsRepo = remember { com.example.sms_app.data.SmsRepository(context) }
                        
                        // Force refresh the SMS counts when dialog opens
                        val refreshTrigger = remember { mutableStateOf(0) }
                        val availableSims = remember(refreshTrigger.value) { 
                            // Force refresh SMS counts for all SIMs
                            val sims = com.example.sms_app.utils.SimManager.getAvailableSims(context)
                            sims.forEach { sim ->
                                // This will refresh the count
                                smsRepo.getSmsCountToday(sim.subscriptionId)
                            }
                            sims
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Quản lý số lượt gửi SMS",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1565C0)
                                    )

                                    IconButton(
                                        onClick = {
                                            refreshTrigger.value = refreshTrigger.value + 1
                                            android.widget.Toast.makeText(
                                                context,
                                                "Đã cập nhật số lượt gửi SMS",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Làm mới",
                                            tint = Color(0xFF1976D2)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "Mỗi SIM có giới hạn 40 tin nhắn mỗi ngày. Khi thay SIM, bạn cần reset số lượt để đếm lại từ đầu.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2)
                                )
                                
                                // Hiển thị thông tin từng SIM
                                if (availableSims.size > 1) {
                                    Text(
                                        text = "SIM đã cài đặt trên thiết bị:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1976D2),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    
                                    // Hiển thị thông tin từng SIM
                                    availableSims.forEach { sim ->
                                        val smsCount = smsRepo.getSmsCountToday(sim.subscriptionId)
                                        val carrierColor = when(sim.carrierName) {
                                            "Viettel" -> Color(0xFF4CAF50)
                                            "Mobifone" -> Color(0xFF2196F3)
                                            "Vinaphone" -> Color(0xFFF44336)
                                            "Vietnamobile" -> Color(0xFF607D8B)
                                            else -> Color(0xFF9C27B0)
                                        }
                                        
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.White
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "SIM ${sim.simSlotIndex + 1}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Card(
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = carrierColor
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 2.dp),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = sim.carrierName,
                                                                fontSize = 9.sp,
                                                                color = Color.White,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "Đã gửi: ",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                        Text(
                                                            text = "$smsCount/40",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when {
                                                                smsCount > 35 -> Color.Red
                                                                smsCount > 25 -> Color(0xFFFF9800) // Orange
                                                                else -> Color(0xFF4CAF50) // Green
                                                            }
                                                        )
                                                        Text(
                                                            text = " tin",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                    
                                                    if (sim.phoneNumber?.isNotEmpty() == true) {
                                                        Text(
                                                            text = "SĐT: ${sim.phoneNumber}",
                                                            fontSize = 9.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        val success = smsRepo.resetSmsCount(sim.subscriptionId)
                                                        // Update the refresh trigger to force UI update
                                                        refreshTrigger.value++
                                                        
                                                        if (success) {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Đã reset số lượt gửi cho SIM ${sim.simSlotIndex + 1} (${sim.carrierName})",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Không thể reset số lượt gửi cho SIM ${sim.simSlotIndex + 1}",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = carrierColor
                                                    ),
                                                    modifier = Modifier
                                                        .padding(start = 4.dp)
                                                        .height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "Reset",
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Default SIM
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE1F5FE)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "SIM Mặc định",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = Color(0xFF607D8B)
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 2.dp),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Default",
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            
                                            val defaultSmsCount = smsRepo.getSmsCountToday(-1)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Đã gửi: ",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "$defaultSmsCount/40",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        defaultSmsCount > 35 -> Color.Red
                                                        defaultSmsCount > 25 -> Color(0xFFFF9800) // Orange
                                                        else -> Color(0xFF4CAF50) // Green
                                                    }
                                                )
                                                Text(
                                                    text = " tin",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        
                                        Button(
                                            onClick = {
                                                val success = smsRepo.resetSmsCount(-1)
                                                // Update the refresh trigger to force UI update
                                                refreshTrigger.value++
                                                
                                                if (success) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Đã reset số lượt gửi cho SIM mặc định",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Không thể reset số lượt gửi cho SIM mặc định",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF607D8B)
                                            ),
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Reset",
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Reset All SIMs button
                                Button(
                                    onClick = {
                                        // Gọi hàm reset số lượt gửi SMS
                                        val success = smsRepo.resetAllSimCounts()
                                        // Update the refresh trigger to force UI update
                                        refreshTrigger.value++
                                        
                                        if (success) {
                                            android.widget.Toast.makeText(
                                                context, 
                                                "Đã reset số lượt gửi SMS cho tất cả SIM", 
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            android.widget.Toast.makeText(
                                                context, 
                                                "Không thể reset số lượt gửi SMS cho tất cả SIM", 
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2196F3)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "RESET TẤT CẢ SIM",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Developer Info
                    AboutScreen(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                }
                
                // Save Button
                Button(
                    onClick = {
                        val settings = AppSettings(
                            intervalBetweenSmsSeconds = intervalSeconds.toIntOrNull() ?: 25,
                            maxRetryAttempts = maxRetry.toIntOrNull() ?: 3,
                            retryDelaySeconds = retryDelay.toIntOrNull() ?: 5,
                            randomizeInterval = false,
                            randomizeContent = false,
                            addRandomEmoji = false,
                            useRandomSpacing = false,
                            minIntervalSeconds = minIntervalSeconds.toIntOrNull() ?: 20,
                            maxIntervalSeconds = maxIntervalSeconds.toIntOrNull() ?: 35
                        )
                        onSave(settings)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LƯU CÀI ĐẶT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }
        
        content()
    }
}

@Composable
private fun SettingItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    helper: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555)
            )
        }
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color.Gray
            )
        )
        
        if (helper != null) {
            Text(
                text = helper,
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
} 