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

@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit
) {
    var intervalSeconds by remember { mutableStateOf(currentSettings.intervalBetweenSmsSeconds.toString()) }
    var maxRetry by remember { mutableStateOf(currentSettings.maxRetryAttempts.toString()) }
    var retryDelay by remember { mutableStateOf(currentSettings.retryDelaySeconds.toString()) }
    var randomizeInterval by remember { mutableStateOf(currentSettings.randomizeInterval) }
    var randomizeContent by remember { mutableStateOf(currentSettings.randomizeContent) }
    var addRandomEmoji by remember { mutableStateOf(currentSettings.addRandomEmoji) }
    var useRandomSpacing by remember { mutableStateOf(currentSettings.useRandomSpacing) }
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
                        
                        // Thêm tùy chọn ngẫu nhiên hóa thời gian
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = randomizeInterval,
                                onCheckedChange = { randomizeInterval = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4CAF50)
                                )
                            )
                            
                            Text(
                                text = "Ngẫu nhiên hóa thời gian gửi",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        // Khoảng thời gian ngẫu nhiên
                        if (randomizeInterval) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = minIntervalSeconds,
                                    onValueChange = { 
                                        if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() >= 1)) {
                                            minIntervalSeconds = it
                                        }
                                    },
                                    label = { Text("Tối thiểu (giây)", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = maxIntervalSeconds,
                                    onValueChange = { 
                                        if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() >= 1)) {
                                            maxIntervalSeconds = it
                                        }
                                    },
                                    label = { Text("Tối đa (giây)", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            
                            Text(
                                text = "Thời gian gửi sẽ ngẫu nhiên trong khoảng này",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }
                    
                    Divider()
                    
                    // Anti-Detection Settings
                    SettingSection(
                        title = "🛡️ Cài đặt chống phát hiện tin nhắn tự động",
                        icon = Icons.Default.Shield,
                        iconColor = Color(0xFF9C27B0)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = randomizeContent,
                                onCheckedChange = { randomizeContent = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF9C27B0)
                                )
                            )
                            
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = "Ngẫu nhiên hóa nội dung tin nhắn",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Thêm ký tự ngẫu nhiên để tránh phát hiện",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = useRandomSpacing,
                                onCheckedChange = { useRandomSpacing = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF9C27B0)
                                )
                            )
                            
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = "Sử dụng khoảng cách ngẫu nhiên",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Thêm khoảng trắng ngẫu nhiên vào tin nhắn",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = addRandomEmoji,
                                onCheckedChange = { addRandomEmoji = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF9C27B0)
                                )
                            )
                            
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = "Thêm emoji ngẫu nhiên",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Thêm emoji vào cuối tin nhắn (có thể tăng phí)",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Các tùy chọn này giúp tránh bị nhà mạng phát hiện tin nhắn tự động",
                                    fontSize = 12.sp,
                                    color = Color(0xFFAD1457),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
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
                        val availableSims = remember { com.example.sms_app.utils.SimManager.getAvailableSims(context) }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Quản lý số lượt gửi SMS",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0)
                                )
                                
                                Text(
                                    text = "Mỗi SIM có giới hạn 40 tin nhắn mỗi ngày. Khi thay SIM, bạn cần reset số lượt để đếm lại từ đầu.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2)
                                )
                                
                                // Hiển thị danh sách các SIM và số lượt đã gửi
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
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${sim.displayName} (${sim.carrierName})",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Đã gửi: $smsCount/40 tin hôm nay",
                                                    fontSize = 12.sp,
                                                    color = if (smsCount > 30) Color.Red else Color.Gray
                                                )
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    smsRepo.resetSmsCount(sim.subscriptionId)
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Đã reset số lượt gửi cho ${sim.displayName}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF2196F3)
                                                ),
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Reset",
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                    
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = Color(0xFFBBDEFB)
                                    )
                                }
                                
                                // Default SIM (always show)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SIM Mặc định",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Đã gửi: ${smsRepo.getSmsCountToday(-1)}/40 tin hôm nay",
                                            fontSize = 12.sp,
                                            color = if (smsRepo.getSmsCountToday(-1) > 30) Color.Red else Color.Gray
                                        )
                                    }
                                    
                                    Button(
                                        onClick = {
                                            smsRepo.resetSmsCount(-1)
                                            android.widget.Toast.makeText(
                                                context,
                                                "Đã reset số lượt gửi cho SIM Mặc định",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2196F3)
                                        ),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Reset",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        // Gọi hàm reset số lượt gửi SMS
                                        smsRepo.resetAllSimCounts()
                                        android.widget.Toast.makeText(context, "Đã reset số lượt gửi SMS cho tất cả SIM", android.widget.Toast.LENGTH_SHORT).show()
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF9C27B0),
                                modifier = Modifier.size(32.dp)
                            )
                            
                            Text(
                                text = "👨‍💻 Thông tin nhà phát triển",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7B1FA2)
                            )
                            
                            Text(
                                text = "Bình An =))",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A148C)
                            )
                            
                            Text(
                                text = "SMS App v1.0",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            
                            Text(
                                text = "📱 Ứng dụng gửi SMS hàng loạt\n⚡ Tự động hóa tin nhắn khách hàng",
                                fontSize = 11.sp,
                                color = Color(0xFF6A1B9A),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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