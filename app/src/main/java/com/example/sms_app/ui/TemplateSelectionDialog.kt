package com.example.sms_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SimCard
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
import kotlinx.coroutines.delay

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

    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val selectedSim = smsRepository.getSelectedSim()
        smsRepository.getSmsCountToday(selectedSim)
        

        availableSims.forEach { sim ->
            smsRepository.getSmsCountToday(sim.subscriptionId)
        }
        

        refreshTrigger++

        while (true) {
            delay(3000)

            val currentSim = smsRepository.getSelectedSim()
            smsRepository.getSmsCountToday(currentSim)

            availableSims.forEach { sim ->
                smsRepository.getSmsCountToday(sim.subscriptionId)
            }

            refreshTrigger++
            android.util.Log.d("TemplateSelectionDialog", "🔄 Auto-refreshed SMS counts, trigger: $refreshTrigger")
        }
    }
    
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
                
                // Add a prominent refresh button
                Button(
                    onClick = {
                        // Force refresh all SMS counts
                        val currentSim = smsRepository.getSelectedSim()
                        val count = smsRepository.forceRefreshSmsCount(currentSim)
                        
                        // Also refresh counts for all available SIMs
                        availableSims.forEach { sim ->
                            smsRepository.forceRefreshSmsCount(sim.subscriptionId)
                        }
                        
                        // Update trigger to force recomposition
                        refreshTrigger++
                        
                        // Show confirmation toast
                        android.widget.Toast.makeText(
                            context,
                            "Đã cập nhật số lượng SMS: SIM ${currentSim} - $count/40",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh counts",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CẬP NHẬT SỐ LƯỢNG SMS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // SIM Selection
                if (availableSims.size > 1) {
                    Text(
                        text = "Chọn SIM để gửi:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Column {
                        availableSims.forEach { sim ->
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
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedSim = sim.subscriptionId
                                        smsRepository.setSelectedSim(sim.subscriptionId)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedSim == sim.subscriptionId) 
                                        Color(0xFFE3F2FD) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (selectedSim == sim.subscriptionId) 2.dp else 1.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = selectedSim == sim.subscriptionId,
                                            onClick = { 
                                                selectedSim = sim.subscriptionId
                                                smsRepository.setSelectedSim(sim.subscriptionId)
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = carrierColor)
                                        )
                                        
                                        Column(
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "SIM ${sim.simSlotIndex + 1}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = carrierColor
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = sim.carrierName,
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        // Remove SMS count display
                                        /*
                                        // Force refresh SMS count for accurate display
                                        val smsCount = remember(sim.subscriptionId, refreshTrigger) { 
                                            smsRepository.forceRefreshSmsCount(sim.subscriptionId) 
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Đã gửi: ",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "$smsCount/40",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    smsCount > 35 -> Color.Red
                                                    smsCount > 25 -> Color(0xFFFF9800) // Orange
                                                    else -> Color(0xFF4CAF50) // Green
                                                }
                                            )
                                            Text(
                                                text = " tin hôm nay",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            
                                            // Add refresh button
                                            IconButton(
                                                onClick = {
                                                    // Force refresh SMS count
                                                    val count = smsRepository.forceRefreshSmsCount(sim.subscriptionId)
                                                    // Update trigger to force recomposition
                                                    refreshTrigger++
                                                    
                                                    // Show toast with updated count
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Đã cập nhật: $count/40 tin",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Refresh count",
                                                    tint = Color(0xFF2196F3),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        */
                                        
                                        if (sim.phoneNumber?.isNotEmpty() == true) {
                                            Text(
                                                text = "SĐT: ${sim.phoneNumber}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    }
                                    
                                                                // Reset button removed
                                }
                            }
                        }
                    }
                } else if (availableSims.isNotEmpty()) {
                    // Single SIM info
                    val sim = availableSims.first()
                    val carrierColor = when(sim.carrierName) {
                        "Viettel" -> Color(0xFF4CAF50)
                        "Mobifone" -> Color(0xFF2196F3)
                        "Vinaphone" -> Color(0xFFF44336)
                        "Vietnamobile" -> Color(0xFF607D8B)
                        else -> Color(0xFF9C27B0)
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SimCard,
                                    contentDescription = null,
                                    tint = carrierColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Column(
                                    modifier = Modifier.padding(start = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SIM ${sim.simSlotIndex + 1}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = carrierColor
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = sim.carrierName,
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (sim.phoneNumber?.isNotEmpty() == true) {
                                        Text(
                                            text = "SĐT: ${sim.phoneNumber}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                        }
                    }
                }

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