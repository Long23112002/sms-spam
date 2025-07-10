package com.example.sms_app.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign


enum class CustomerField(val icon: ImageVector, val placeholder: String, val keyboardType: KeyboardType = KeyboardType.Text) {
    Name(Icons.Default.Person, "Cột A - Tên khách (xxx)"),
    Id(Icons.Default.DocumentScanner, "Cột B - Số CCCD (yyy)", KeyboardType.Number),
    PhoneNumber(Icons.Default.Call, "Cột C - Số Điện thoại", KeyboardType.Phone),
    Address(Icons.Default.Home, "Cột D - Địa chỉ (ttt)"),
    Option1(Icons.AutoMirrored.Filled.Notes, "Cột E - Tùy chọn 1 (zzz)"),
    Option2(Icons.AutoMirrored.Filled.Notes, "Cột F - Tùy chọn 2 (www)"),
    Option3(Icons.AutoMirrored.Filled.Notes, "Cột G - Tùy chọn 3 (uuu)"),
    Option4(Icons.AutoMirrored.Filled.Notes, "Cột H - Tùy chọn 4 (vvv)"),
    Option5(Icons.AutoMirrored.Filled.Notes, "Cột I - Tùy chọn 5 (rrr)"),
    Pattern(Icons.AutoMirrored.Filled.Message, "Cột J - Mẫu tin"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(onDismissRequest: () -> Unit) {
    val values = remember {
        mutableStateListOf<String>().apply {
            addAll(CustomerField.entries.map { _ -> "" })
        }
    }
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Card {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Thêm khách")
                        },
                        actions = {
                            IconButton(onClick = {
                                onDismissRequest()
                            }) {
                                Icon(Icons.Default.Close, null)
                            }
                        })
                },
                bottomBar = {
                    BottomAppBar {
                        TextButton(onClick = {}, Modifier.fillMaxWidth()) {
                            Text(
                                "Lưu".uppercase(),
                                Modifier.fillMaxWidth(),
                                style = TextStyle(textAlign = TextAlign.Center)
                            )
                        }
                    }
                }

            ) { paddingValues ->
                LazyColumn(Modifier.padding(paddingValues)) {
                    items(CustomerField.entries.size) { index ->
                        val item = CustomerField.entries[index]
                        TextField(
                            values[index],
                            onValueChange = {
                                values[index] = it
                            },
                            placeholder = {
                                Text(item.placeholder)
                            },
                            trailingIcon = {
                                Icon(item.icon, null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = item.keyboardType)
                        )
                    }
                }
            }
        }
    }
}
