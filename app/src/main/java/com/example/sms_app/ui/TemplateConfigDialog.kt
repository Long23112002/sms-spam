package com.example.sms_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                Text(
                    text = "Kí tự: $characterCount",
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
                
                // Template selection with default buttons
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(9) { index ->
                        val templateId = index + 1
                        val template = templates.find { it.id == templateId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplateId = templateId },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTemplateId == templateId) 
                                    Color(0xFFE3F2FD) 
                                else if (defaultTemplateId == templateId) 
                                    Color(0xFFFFF3E0) 
                                else Color(0xFFF5F5F5)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedTemplateId == templateId,
                                            onClick = { selectedTemplateId = templateId },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color.Red
                                            ),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Mẫu $templateId",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                        if (defaultTemplateId == templateId) {
                                            Text(
                                                text = " (Mặc định)",
                                                fontSize = 10.sp,
                                                color = Color(0xFFFF5722),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (template?.content?.isNotEmpty() == true) {
                                        Text(
                                            text = template.content.take(30) + if (template.content.length > 30) "..." else "",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(start = 24.dp)
                                        )
                                    }
                                }
                                Button(
                                    onClick = { 
                                        defaultTemplateId = templateId
                                        // Auto save default template
                                        onSave(templates, templateId)
                                    },
                                    modifier = Modifier.size(width = 80.dp, height = 24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (defaultTemplateId == templateId) 
                                            Color(0xFF4CAF50) 
                                        else Color(0xFFFF5722)
                                    ),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(
                                        text = if (defaultTemplateId == templateId) "Đã đặt" else "Đặt MD",
                                        fontSize = 8.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
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

                }
            }
        }
    }
}

 