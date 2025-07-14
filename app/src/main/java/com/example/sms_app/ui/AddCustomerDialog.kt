package com.example.sms_app.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sms_app.data.Customer
import com.example.sms_app.utils.isValidPhoneNumber
import com.example.sms_app.utils.validateAndFormatPhoneNumber
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
    var phoneNumberError by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current

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
                        onValueChange = { 
                            phoneNumber = it
                            phoneNumberError = null
                        },
                        icon = Icons.Default.Phone,
                        iconColor = Color.Green,
                        isError = phoneNumberError != null,
                        errorMessage = phoneNumberError
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
                        label = "Cột G - Tùy chọn 3 (uuu)",
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
                            // Format and validate phone number
                            val formattedPhoneNumber = phoneNumber.validateAndFormatPhoneNumber()
                            
                            if (formattedPhoneNumber.isValidPhoneNumber()) {
                                val customer = Customer(
                                    id = UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    idNumber = idNumber.trim(),
                                    phoneNumber = formattedPhoneNumber,
                                    address = address.trim(),
                                    option1 = option1.trim(),
                                    option2 = option2.trim(),
                                    option3 = option3.trim(),
                                    option4 = option4.trim(),
                                    option5 = option5.trim(),
                                    templateNumber = templateNumber.toIntOrNull() ?: 1
                                )
                                onSave(customer)
                                
                                // Log the successful formatting
                                Log.d("AddCustomerDialog", "Phone number formatted: $phoneNumber → $formattedPhoneNumber")
                            } else {
                                // Show error for invalid phone number
                                phoneNumberError = "Số điện thoại không hợp lệ"
                                Toast.makeText(
                                    context,
                                    "Số điện thoại không hợp lệ! Vui lòng nhập số điện thoại đúng định dạng VN (0xxx)",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            // Show error for missing fields
                            if (name.isEmpty()) {
                                Toast.makeText(context, "Vui lòng nhập tên khách hàng", Toast.LENGTH_SHORT).show()
                            }
                            if (phoneNumber.isEmpty()) {
                                phoneNumberError = "Vui lòng nhập số điện thoại"
                            }
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
                        text = "THÊM KHÁCH HÀNG",
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
private fun CustomerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    iconColor: Color,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
            },
            singleLine = true,
            isError = isError
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
} 