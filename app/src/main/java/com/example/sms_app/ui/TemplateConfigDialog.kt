package com.example.sms_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.data.TemplateManager

@Composable
fun TemplateConfigDialog(
    currentTemplates: List<MessageTemplate>,
    currentDefaultTemplate: Int = 1,
    onDismiss: () -> Unit,
    onSave: (List<MessageTemplate>, Int) -> Unit
) {
    var selectedTemplateId by remember { mutableStateOf(1) }
    var templateContent by remember { mutableStateOf("") }
    var defaultTemplateId by remember { mutableStateOf(currentDefaultTemplate) }
    var templates by remember { 
        mutableStateOf(
            (1..9).map { id ->
                currentTemplates.find { it.id == id } 
                    ?: TemplateManager.getDefaultTemplates().find { it.id == id }
                    ?: MessageTemplate(id, "", "Template $id")
            }
        ) 
    }
    
    // Update content when selected template changes
    LaunchedEffect(selectedTemplateId) {
        templateContent = templates.find { it.id == selectedTemplateId }?.content ?: ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nhận tin nhắn mẫu",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Status line with character count
                val characterCount = templateContent.length
                val pageCount = if (characterCount <= 160) 1 else (characterCount / 160) + 1
                Text(
                    text = "Kí tự: $characterCount | Trang: $pageCount",
                    fontSize = 12.sp,
                    color = Color.Red
                )
                
                // Text input
                OutlinedTextField(
                    value = templateContent,
                    onValueChange = { templateContent = it },
                    label = { Text("Nhập tin nhắn mẫu") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Blue,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                // Template selection radio buttons (horizontal scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..9).forEach { templateId ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedTemplateId == templateId,
                                onClick = { selectedTemplateId = templateId },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color.Red
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "$templateId",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
                
                // Default template selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎯 Chọn mẫu mặc định để gửi:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (1..9).forEach { templateId ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = defaultTemplateId == templateId,
                                        onClick = { defaultTemplateId = templateId },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFFFF5722)
                                        ),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "$templateId",
                                        fontSize = 9.sp,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "Mẫu mặc định hiện tại: Mẫu $defaultTemplateId",
                            fontSize = 11.sp,
                            color = Color(0xFFBF360C),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            // Clear current template
                            templateContent = ""
                            templates = templates.map { template ->
                                if (template.id == selectedTemplateId) {
                                    template.copy(content = "")
                                } else template
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text(
                            text = "XÓA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    
                    Button(
                        onClick = {
                            // Save current template and close
                            val updatedTemplates = templates.map { template ->
                                if (template.id == selectedTemplateId) {
                                    template.copy(content = templateContent)
                                } else template
                            }
                            onSave(updatedTemplates, defaultTemplateId)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Blue
                        )
                    ) {
                        Text(
                            text = "LƯU",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    
                    Button(
                        onClick = {
                            onSave(templates, defaultTemplateId)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(
                            text = "ĐẶT MẶC ĐỊNH",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

 