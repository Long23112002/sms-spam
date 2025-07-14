package com.example.sms_app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sms_app.data.SmsSession
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemoveDuplicatesConfirmDialog(
    duplicateCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.FilterAlt,
                contentDescription = "Remove Duplicates",
                tint = Color(0xFFFF9800)
            )
        },
        title = {
            Text(
                text = "Xóa trùng lặp số điện thoại",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Tìm thấy $duplicateCount số điện thoại bị trùng lặp.",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Chỉ giữ lại 1 khách hàng đầu tiên cho mỗi số điện thoại.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hành động này không thể hoàn tác!",
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Text("Xóa trùng lặp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun RestoreUnsentCustomersDialog(
    sessionHistory: List<SmsSession>,
    onDismiss: () -> Unit,
    onRestoreSession: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    // Lọc chỉ các session có khách hàng chưa gửi
    val sessionsWithUnsent = sessionHistory.filter { it.remainingCustomers.isNotEmpty() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.SettingsBackupRestore,
                contentDescription = "Restore Unsent",
                tint = Color(0xFF4CAF50)
            )
        },
        title = {
            Text(
                text = "Khôi phục khách hàng chưa gửi",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (sessionsWithUnsent.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Không có phiên nào có khách hàng chưa gửi",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tất cả khách hàng trong các phiên trước đã được gửi hoặc xóa.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Chọn phiên để khôi phục khách hàng chưa gửi:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessionsWithUnsent) { session ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                onClick = {
                                    onRestoreSession(session.sessionId)
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = session.sessionName.takeIf { it.isNotEmpty() }
                                            ?: "Phiên ${dateFormat.format(Date(session.startTime))}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Chưa gửi: ${session.remainingCustomers.size} khách hàng",
                                        fontSize = 12.sp,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = "Thời gian: ${dateFormat.format(Date(session.startTime))}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (sessionsWithUnsent.isEmpty()) {
                Button(onClick = onDismiss) {
                    Text("Đóng")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
            }
        },
        dismissButton = null
    )
} 