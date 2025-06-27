package com.example.sms_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sms_app.data.Customer
import java.util.UUID

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }
    var option4 by remember { mutableStateOf("") }
    var option5 by remember { mutableStateOf("") }
    var templateNumber by remember { mutableStateOf("1") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF4FC3F7))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Thêm khách hàng",
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
                
                // Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CustomerField(
                        label = "Cột A - Tên khách hàng (xxx)",
                        value = name,
                        onValueChange = { name = it },
                        icon = Icons.Default.Person,
                        iconColor = Color.Blue
                    )
                    
                    CustomerField(
                        label = "Cột B - Số CMND/CCCD (yyy)",
                        value = idNumber,
                        onValueChange = { idNumber = it },
                        icon = Icons.Default.AccountBox,
                        iconColor = Color.Red
                    )
                    
                    CustomerField(
                        label = "Cột C - Số điện thoại",
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        icon = Icons.Default.Phone,
                        iconColor = Color.Green
                    )
                    
                    CustomerField(
                        label = "Cột D - Địa chỉ (ttt)",
                        value = address,
                        onValueChange = { address = it },
                        icon = Icons.Default.Home,
                        iconColor = Color.Blue
                    )
                    
                    CustomerField(
                        label = "Cột E - Tùy chọn 1 (zzz)",
                        value = option1,
                        onValueChange = { option1 = it },
                        icon = Icons.Default.Settings,
                        iconColor = Color.Gray
                    )
                    
                    CustomerField(
                        label = "Cột F - Tùy chọn 2 (www)",
                        value = option2,
                        onValueChange = { option2 = it },
                        icon = Icons.Default.Settings,
                        iconColor = Color.Gray
                    )
                    
                    CustomerField(
                        label = "Cột G - Tùy chọn 3 (uuuu)",
                        value = option3,
                        onValueChange = { option3 = it },
                        icon = Icons.Default.Settings,
                        iconColor = Color.Gray
                    )
                    
                    CustomerField(
                        label = "Cột H - Tùy chọn 4 (vvv)",
                        value = option4,
                        onValueChange = { option4 = it },
                        icon = Icons.Default.Settings,
                        iconColor = Color.Gray
                    )
                    
                    CustomerField(
                        label = "Cột I - Tùy chọn 5 (rrr)",
                        value = option5,
                        onValueChange = { option5 = it },
                        icon = Icons.Default.Settings,
                        iconColor = Color.Gray
                    )
                    
                    CustomerField(
                        label = "Cột J - Số mẫu tin nhắn (1-9)",
                        value = templateNumber,
                        onValueChange = { if (it.isEmpty() || (it.toIntOrNull() in 1..9)) templateNumber = it },
                        icon = Icons.Default.Edit,
                        iconColor = Color.Red
                    )
                    
                    // Preview (sẽ cập nhật sau khi có template)
                }
                
                // Save Button
                Button(
                    onClick = {
                        if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                            val customer = Customer(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                idNumber = idNumber,
                                phoneNumber = phoneNumber,
                                address = address,
                                option1 = option1,
                                option2 = option2,
                                option3 = option3,
                                option4 = option4,
                                option5 = option5,
                                templateNumber = templateNumber.toIntOrNull() ?: 1
                            )
                            onSave(customer)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    enabled = name.isNotEmpty() && phoneNumber.isNotEmpty()
                ) {
                    Text(
                        text = "LƯU THÔNG TIN",
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
fun CustomerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    iconColor: Color,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { 
                if (isHighlighted) 
                    it.background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                else it
            }
            .padding(if (isHighlighted) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { 
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = if (isHighlighted) Color.Red else Color.Gray
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = if (label.contains("Mẫu tin nhắn")) 3 else 1,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isHighlighted) Color.Red else Color.Blue,
                unfocusedBorderColor = Color.Gray
            )
        )
    }
} 