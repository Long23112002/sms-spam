package com.example.sms_app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sms_app.data.Customer
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.utils.SmsUtils
import android.util.Log

@Composable
fun CustomerDetailDialog(
    customer: Customer,
    templates: List<MessageTemplate>,
    defaultTemplateId: Int = 1,
    onDismiss: () -> Unit
) {
    val actualTemplateId = defaultTemplateId
    val selectedTemplate = templates.find { it.id == actualTemplateId }

    val isPhoneNumberValid = SmsUtils.isValidPhoneNumber(customer.phoneNumber)
    val formattedPhoneNumber = SmsUtils.validateAndFormatPhoneNumber(customer.phoneNumber)

    if (!isPhoneNumberValid || formattedPhoneNumber != customer.phoneNumber) {
        Log.w("CustomerDetailDialog", "Phone number issue detected: " +
              "Original=${customer.phoneNumber}, Formatted=$formattedPhoneNumber, Valid=$isPhoneNumberValid")
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = customer.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = customer.phoneNumber,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            
                            // Add phone number validation icon
                            Icon(
                                imageVector = if (isPhoneNumberValid) 
                                    Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = if (isPhoneNumberValid) 
                                    "Số điện thoại hợp lệ" else "Số điện thoại không hợp lệ",
                                tint = if (isPhoneNumberValid) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        // Show warning if phone number is not in expected format
                        if (!isPhoneNumberValid) {
                            Text(
                                text = "⚠️ Số điện thoại không đúng định dạng VN",
                                fontSize = 12.sp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color.Gray
                        )
                    }
                }
                
                // Message Preview
                if (selectedTemplate != null && selectedTemplate.content.isNotEmpty()) {
                    Text(
                        text = "💬 Tin nhắn sẽ được gửi:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            val personalizedMessage = customer.getPersonalizedMessage(templates, actualTemplateId)
                            Text(
                                text = personalizedMessage,
                                fontSize = 16.sp,
                                color = Color(0xFF1B5E20),
                                lineHeight = 22.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Mẫu: $actualTemplateId (mặc định)",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${personalizedMessage.length} ký tự",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Text(
                            text = "⚠️ Chưa có mẫu tin nhắn được thiết lập cho khách hàng này",
                            fontSize = 14.sp,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                // Show phone number information for troubleshooting
                if (!isPhoneNumberValid) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEEE))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Thông tin lỗi số điện thoại:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                            Text(
                                text = "Định dạng hiện tại: ${customer.phoneNumber}",
                                fontSize = 12.sp,
                                color = Color(0xFF424242)
                            )
                            Text(
                                text = "Định dạng đề xuất: $formattedPhoneNumber",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "SMS có thể không gửi được do số điện thoại không đúng định dạng.",
                                fontSize = 12.sp,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                }
                
                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text(
                        text = "ĐÓNG",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

 