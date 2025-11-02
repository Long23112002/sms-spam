package com.example.sms_app.presentation.component

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Send

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.data.SmsProgress
import com.example.sms_app.data.Customer
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.presentation.viewmodel.SendMessageViewModel
import com.example.sms_app.utils.SimInfo
import com.example.sms_app.utils.SimConfig


enum class BottomButton(val icon: ImageVector) {
    Add(Icons.Default.PersonAdd),
    Send(Icons.AutoMirrored.Filled.Send),
    Message(Icons.AutoMirrored.Filled.Message),

    Setting(Icons.Default.Settings),
    Search(Icons.Default.Search),
    MoreVert(Icons.Default.MoreVert),
    None(Icons.Default.MoreVert),

    SendMessage(Icons.Default.MoreVert),
    WrongPattern(Icons.Default.MoreVert),
    ;
}

@Composable
fun MyBottomBar(
    providers: List<String>,
    customers: List<Customer> = emptyList(),
    sendMessageViewModel: SendMessageViewModel = hiltViewModel(),
    onBottomButton: ((BottomButton) -> Unit),
    onProviderSelected: ((String) -> Unit) = {},
    onCustomerAdded: (() -> Unit) = {},
    onRemoveDuplicates: (() -> Unit) = {},
    onRestoreUnsentCustomers: (() -> Unit) = {},
    onUpdateClick: (() -> Unit) = {},
    onHomeClick: (() -> Unit) = {},
    onSupportClick: (() -> Unit) = {},
    onSearchClick: (() -> Unit) = {},
    onExportClick: (() -> Unit) = {}
) {
    var button by remember {
        mutableStateOf(BottomButton.None)
    }
    var messageTemplate by remember {
        mutableStateOf(MessageTemplate())
    }
    var simConfig by remember {
        mutableStateOf(SimConfig())
    }
    val progress = sendMessageViewModel.progress.observeAsState(SmsProgress()).value
    val completion = sendMessageViewModel.completion.observeAsState().value
    val isSending = sendMessageViewModel.isSending.observeAsState().value
    val context = LocalContext.current

    LaunchedEffect(completion) {
        completion?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    BottomAppBar(Modifier.height(210.dp)) {
        Column {
            if (isSending == true) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    progress.let {
                        Text("${it.progress}/${it.total}")
                        TextButton(onClick = {
                            button = BottomButton.SendMessage
                        }) {
                            Text("Open dialog")
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(
                    listOf(BottomButton.Setting),
                    listOf(BottomButton.Search, BottomButton.MoreVert),
                ).forEach {
                    Row {
                        it.forEach { x ->
                            IconButton(onClick = {
                                button = x
                                onBottomButton(x)
                            }) {
                                Icon(x.icon, contentDescription = null)
                            }
                            if (button == BottomButton.MoreVert && x == BottomButton.MoreVert) {
                                MoreView(
                                    button = button,
                                    onDismissRequest = {
                                        button = BottomButton.None
                                    },
                                    onRemoveDuplicates = onRemoveDuplicates,
                                    onRestoreUnsentCustomers = onRestoreUnsentCustomers,
                                    onUpdateClick = onUpdateClick,
                                    onHomeClick = onHomeClick,
                                    onSupportClick = onSupportClick,
                                    onExportClick = onExportClick
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var selectedProvider by remember {
                    mutableStateOf("all")
                }
                
                // Calculate customer counts for each carrier
                val totalCustomers = customers.size
                val carrierCounts = providers.associateWith { provider ->
                    customers.count { customer ->
                        customer.carrier.lowercase() == provider.lowercase()
                    }
                }
                
                Text("Lọc theo nhà mạng")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedProvider == "all",
                            onClick = { 
                                selectedProvider = "all"
                                onProviderSelected("all")
                            }
                        )
                        Text("Tất cả ($totalCustomers)")
                    }

                    providers.forEach { provider ->
                        val count = carrierCounts[provider] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedProvider == provider,
                                onClick = {
                                    selectedProvider = provider
                                    onProviderSelected(provider)
                                }
                            )
                            Text("${provider.capitalize(Locale.current)} ($count)")
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(BottomButton.Add, BottomButton.Send, BottomButton.Message).forEach { buttonType ->
                    val backgroundColor = when (buttonType) {
                        BottomButton.Add -> Color(0xFF4CAF50) // Xanh lá
                        BottomButton.Send -> Color(0xFF2196F3) // Xanh dương
                        BottomButton.Message -> Color(0xFFF44336) // Đỏ
                        else -> Color.Gray
                    }

                    Button(
                        onClick = {
                            button = buttonType
                            onBottomButton(buttonType)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = backgroundColor
                        ),
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            buttonType.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    when (button) {
        BottomButton.Add -> {
            AddCustomerDialog(
                onDismissRequest = {
                    button = BottomButton.None
                },
                onCustomerAdded = {
                    onCustomerAdded()
                }
            )
        }

        BottomButton.Send -> {
            SelectSimDialog(
                customers = customers,
                onDismissRequest = {
                    button = BottomButton.None
                },
                onSend = { t, s ->
                    messageTemplate = t
                    simConfig = s
                    button = BottomButton.SendMessage
                }
            )
        }

        BottomButton.Message -> {
            PatternDialog {
                button = BottomButton.None
            }
        }

        BottomButton.Setting -> {
            SettingDialog {
                button = BottomButton.None
            }
        }



        BottomButton.Search -> {
            button = BottomButton.None
            onSearchClick()
        }
        BottomButton.MoreVert -> {}
        BottomButton.None -> {}
        BottomButton.SendMessage -> {
            // Debug: Hiển thị tất cả khách hàng được pass vào MyBottomBar
            android.util.Log.d("MyBottomBar", "🔍 ALL customers passed to MyBottomBar: ${customers.size}")
            customers.forEachIndexed { index, customer ->
                android.util.Log.d("MyBottomBar", "   $index. ${customer.name} (${customer.phoneNumber}) - isSelected: ${customer.isSelected}")
            }
            
            // Kiểm tra số khách hàng được chọn
            val selectedCustomers = customers.filter { it.isSelected }
            android.util.Log.d("MyBottomBar", "🔍 Selected customers before SendMessageDialog: ${selectedCustomers.size}")
            selectedCustomers.forEach { customer ->
                android.util.Log.d("MyBottomBar", "✅ Selected: ${customer.name} (${customer.phoneNumber})")
            }
            
            if (selectedCustomers.isEmpty()) {
                android.util.Log.w("MyBottomBar", "❌ No customers selected")
                // Toast.makeText(context, "❌ Vui lòng chọn ít nhất một khách hàng", Toast.LENGTH_SHORT).show()
                button = BottomButton.None
            } else if (messageTemplate.content.isBlank()) {
                android.util.Log.w("MyBottomBar", "❌ Message template is empty")
                Toast.makeText(context, "❌ Vui lòng cấu hình mẫu tin nhắn trước khi gửi!", Toast.LENGTH_LONG).show()
                button = BottomButton.None
            } else {
                android.util.Log.d("MyBottomBar", "🚀 Opening SendMessageDialog with ${selectedCustomers.size} selected customers")
                android.util.Log.d("MyBottomBar", "📝 Message template: ${messageTemplate.content.take(50)}...")
                SendMessageDialog(
                    messageTemplate,
                    simConfig,
                    sendMessageViewModel
                ) {
                    button = BottomButton.None
                }
            }
        }

        BottomButton.WrongPattern -> {
            WrongPatternDialog(
                onDismissRequest = {
                    button = BottomButton.None
                },
                confirm = {},
                continueMessage = {}
            )
        }
    }
}
