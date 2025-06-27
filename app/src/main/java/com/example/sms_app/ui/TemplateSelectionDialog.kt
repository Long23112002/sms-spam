package com.example.sms_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
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
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.utils.SimInfo
import com.example.sms_app.utils.SimManager

@Composable
fun TemplateSelectionDialog(
    customers: List<Customer>,
    templates: List<MessageTemplate>,
    smsRepository: SmsRepository,
    defaultTemplateId: Int = 1,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedTemplateId by remember { mutableStateOf(defaultTemplateId) }
    var selectedSim by remember { mutableStateOf(smsRepository.getSelectedSim()) }
    val selectedCustomers = customers.filter { it.isSelected }
    val availableSims = remember { SimManager.getAvailableSims(context) }
    
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
                modifier = Modifier
                    .padding(16.dp)
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
                        text = "Chọn mẫu tin nhắn",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng"
                        )
                    }
                }
                
                // Info
                Text(
                    text = "Sẽ gửi cho ${selectedCustomers.size} khách hàng đã chọn",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Text(
                    text = "⚠️ Giới hạn: 40 tin nhắn/ngày/SIM",
                    fontSize = 12.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                
                // SIM Selection
                if (availableSims.size > 1) {
                    Text(
                        text = "Chọn SIM để gửi:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Column {
                        availableSims.forEach { sim ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSim == sim.subscriptionId,
                                    onClick = { 
                                        selectedSim = sim.subscriptionId
                                        smsRepository.setSelectedSim(sim.subscriptionId)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color.Blue)
                                )
                                
                                Column(
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = sim.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${sim.carrierName} • ${smsRepository.getSmsCountToday(sim.subscriptionId)}/40 tin đã gửi",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                } else if (availableSims.isNotEmpty()) {
                    // Single SIM info
                    val sim = availableSims.first()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📱",
                                fontSize = 16.sp
                            )
                            Column(
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = sim.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${sim.carrierName} • ${smsRepository.getSmsCountToday(sim.subscriptionId)}/40 tin đã gửi",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                // Template selection
                Text(
                    text = "Chọn mẫu tin nhắn:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(templates.filter { it.content.isNotEmpty() }) { index, template ->
                        TemplateSelectionItem(
                            template = template,
                            isSelected = selectedTemplateId == template.id,
                            onClick = { selectedTemplateId = template.id }
                        )
                    }
                }
                
//                // Preview with first customer (using default template)
//                if (selectedCustomers.isNotEmpty()) {
//                    val previewTemplate = templates.find { it.id == defaultTemplateId }
//                    if (previewTemplate != null && previewTemplate.content.isNotEmpty()) {
//                        Card(
//                            modifier = Modifier.fillMaxWidth(),
//                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
//                        ) {
//                            Column(
//                                modifier = Modifier.padding(12.dp)
//                            ) {
//                                Text(
//                                    text = "Tin nhắn sẽ được gửi (${selectedCustomers.first().name}) - Mẫu mặc định $defaultTemplateId:",
//                                    fontWeight = FontWeight.Bold,
//                                    fontSize = 12.sp,
//                                    color = Color(0xFF2E7D32)
//                                )
//                                Spacer(modifier = Modifier.height(4.dp))
//                                Text(
//                                    text = selectedCustomers.first().getPersonalizedMessage(templates, defaultTemplateId),
//                                    fontSize = 12.sp,
//                                    color = Color(0xFF2E7D32)
//                                )
//                            }
//                        }
//                    }
//                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy")
                    }
                    
                    Button(
                        onClick = { 
                            android.util.Log.d("TemplateSelectionDialog", "🔍 Selected template ID: $selectedTemplateId")
                            onConfirm(selectedTemplateId) 
                        },
                        modifier = Modifier.weight(1f),
                        enabled = templates.any { it.id == selectedTemplateId && it.content.isNotEmpty() }
                    ) {
                        Text("Bắt đầu gửi")
                    }
                }
            }
        }
    }
}

@Composable
fun TemplateSelectionItem(
    template: MessageTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Color.Blue)
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = "Mẫu ${template.id}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Blue else Color.Black
                )
                
                Text(
                    text = template.content.take(50) + if (template.content.length > 50) "..." else "",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
            }
        }
    }
} 