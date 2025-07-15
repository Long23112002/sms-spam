package com.example.sms_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
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
import com.example.sms_app.data.Customer
import com.example.sms_app.data.MessageTemplate

@Composable
fun SmsPreviewDialog(
    customer: Customer,
    template: MessageTemplate,
    onDismiss: () -> Unit,
    onSendSms: (() -> Unit)? = null
) {
    val personalizedMessage = remember(customer, template) {
        template.content
            // Variables without braces
            .replace("xxx", customer.name)
            .replace("XXX", customer.name)
            .replace("yyy", customer.idNumber)
            .replace("YYY", customer.idNumber)
            .replace("ttt", customer.address)
            .replace("TTT", customer.address)
            .replace("zzz", customer.option1)
            .replace("ZZZ", customer.option1)
            .replace("www", customer.option2)
            .replace("WWW", customer.option2)
            .replace("uuu", customer.option3)
            .replace("UUU", customer.option3)
            .replace("vvv", customer.option4)
            .replace("VVV", customer.option4)
            .replace("rrr", customer.option5)
            .replace("RRR", customer.option5)
            // Variables with curly braces {} - keep brackets, replace content
            .replace("{xxx}", "{${customer.name}}")
            .replace("{XXX}", "{${customer.name}}")
            .replace("{yyy}", "{${customer.idNumber}}")
            .replace("{YYY}", "{${customer.idNumber}}")
            .replace("{ttt}", "{${customer.address}}")
            .replace("{TTT}", "{${customer.address}}")
            .replace("{zzz}", "{${customer.option1}}")
            .replace("{ZZZ}", "{${customer.option1}}")
            .replace("{www}", "{${customer.option2}}")
            .replace("{WWW}", "{${customer.option2}}")
            .replace("{uuu}", "{${customer.option3}}")
            .replace("{UUU}", "{${customer.option3}}")
            .replace("{vvv}", "{${customer.option4}}")
            .replace("{VVV}", "{${customer.option4}}")
            .replace("{rrr}", "{${customer.option5}}")
            .replace("{RRR}", "{${customer.option5}}")
            // Variables with square brackets [] - keep brackets, replace content
            .replace("[xxx]", "[${customer.name}]")
            .replace("[XXX]", "[${customer.name}]")
            .replace("[yyy]", "[${customer.idNumber}]")
            .replace("[YYY]", "[${customer.idNumber}]")
            .replace("[ttt]", "[${customer.address}]")
            .replace("[TTT]", "[${customer.address}]")
            .replace("[zzz]", "[${customer.option1}]")
            .replace("[ZZZ]", "[${customer.option1}]")
            .replace("[www]", "[${customer.option2}]")
            .replace("[WWW]", "[${customer.option2}]")
            .replace("[uuu]", "[${customer.option3}]")
            .replace("[UUU]", "[${customer.option3}]")
            .replace("[vvv]", "[${customer.option4}]")
            .replace("[VVV]", "[${customer.option4}]")
            .replace("[rrr]", "[${customer.option5}]")
            .replace("[RRR]", "[${customer.option5}]")
            // Variables with parentheses () - keep brackets, replace content
            .replace("(xxx)", "(${customer.name})")
            .replace("(XXX)", "(${customer.name})")
            .replace("(yyy)", "(${customer.idNumber})")
            .replace("(YYY)", "(${customer.idNumber})")
            .replace("(ttt)", "(${customer.address})")
            .replace("(TTT)", "(${customer.address})")
            .replace("(zzz)", "(${customer.option1})")
            .replace("(ZZZ)", "(${customer.option1})")
            .replace("(www)", "(${customer.option2})")
            .replace("(WWW)", "(${customer.option2})")
            .replace("(uuu)", "(${customer.option3})")
            .replace("(UUU)", "(${customer.option3})")
            .replace("(vvv)", "(${customer.option4})")
            .replace("(VVV)", "(${customer.option4})")
            .replace("(rrr)", "(${customer.option5})")
            .replace("(RRR)", "(${customer.option5})")
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
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Xem trước SMS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color.Gray
                        )
                    }
                }
                
                HorizontalDivider(color = Color(0xFFE0E0E0))
                
                // SMS Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Message",
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Nội dung tin nhắn (Mẫu ${template.id})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = personalizedMessage,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color.Black,
                                    textAlign = TextAlign.Start
                                )
                                
                                HorizontalDivider(
                                    color = Color(0xFFE0E0E0),
                                    thickness = 1.dp
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ký tự: ${personalizedMessage.length}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Nhà mạng: ${customer.carrier}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = "Đóng",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    if (onSendSms != null) {
                        Button(
                            onClick = {
                                onSendSms()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gửi SMS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
} 