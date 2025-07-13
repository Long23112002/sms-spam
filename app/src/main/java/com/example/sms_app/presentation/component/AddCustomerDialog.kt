package com.example.sms_app.presentation.component

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.presentation.viewmodel.AddCustomerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


enum class CustomerField(
    val icon: ImageVector,
    val placeholder: String,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val default: String = ""
) {
    Name(Icons.Default.Person, "Cột A - Tên khách (xxx)", default = "Hxt"),
    Id(
        Icons.Default.DocumentScanner,
        "Cột B - Số CCCD (yyy)",
        KeyboardType.Number,
        default = "123456789"
    ),
    PhoneNumber(
        Icons.Default.Call,
        "Cột C - Số Điện thoại",
        KeyboardType.Phone,
        default = "0386228382"
    ),
    Address(Icons.Default.Home, "Cột D - Địa chỉ (ttt)", default = "Hà Nội"),
    Option1(Icons.AutoMirrored.Filled.Notes, "Cột E - Tùy chọn 1 (zzz)", default = ""),
    Option2(Icons.AutoMirrored.Filled.Notes, "Cột F - Tùy chọn 2 (www)", default = ""),
    Option3(Icons.AutoMirrored.Filled.Notes, "Cột G - Tùy chọn 3 (uuu)", default = ""),
    Option4(Icons.AutoMirrored.Filled.Notes, "Cột H - Tùy chọn 4 (vvv)", default = ""),
    Option5(Icons.AutoMirrored.Filled.Notes, "Cột I - Tùy chọn 5 (rrr)", default = ""),
    Pattern(
        Icons.AutoMirrored.Filled.Message,
        "Cột J - Mẫu tin",
        KeyboardType.Number,
        default = ""
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    addCustomerViewModel: AddCustomerViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit,
    onCustomerAdded: () -> Unit = {}
) {
    val values = remember {
        mutableStateListOf<String>().apply {
            addAll(CustomerField.entries.map { _ -> "" })
        }
    }
    val context = LocalContext.current
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Card {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Thêm khách", modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        CustomerField.entries.forEach {
                                            values[it.ordinal] = it.default
                                        }
                                    }
                                )
                            })
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
                        TextButton(onClick = {
                            addCustomerViewModel.verify(values) {
                                onDismissRequest()
                                onCustomerAdded()
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(context, "Đã thêm khách hàng: ${values[CustomerField.Name.ordinal]}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }, Modifier.fillMaxWidth()) {
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
