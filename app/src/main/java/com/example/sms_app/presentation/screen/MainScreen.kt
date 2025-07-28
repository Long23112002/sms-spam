package com.example.sms_app.presentation.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sms_app.data.MessageTemplate

import com.example.sms_app.presentation.component.MyBottomBar
import com.example.sms_app.presentation.component.MyTopBar
import com.example.sms_app.presentation.component.SearchDialog
import com.example.sms_app.presentation.component.UpdateDialog
import com.example.sms_app.presentation.viewmodel.MainViewModel
import com.example.sms_app.presentation.viewmodel.UpdateViewModel
import com.example.sms_app.ui.SmsPreviewDialog
import com.example.sms_app.utils.IntentUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val providers = listOf("viettel", "mobifone", "vinaphone", "vietnamobile")
    val allCustomers = mainViewModel.customers.observeAsState(listOf()).value
    
    // Update states
    val updateInfo by updateViewModel.updateInfo.collectAsState()
    val isDownloading by updateViewModel.isDownloading.collectAsState()
    val downloadProgress by updateViewModel.downloadProgress.collectAsState()
    val showUpdateDialog by updateViewModel.showUpdateDialog.collectAsState()
    
    // Comment out auto check update để tránh hiển thị dialog liên tục
    // LaunchedEffect(Unit) {
    //     updateViewModel.checkForUpdates()
    // }
    
    var selectedProvider by remember {
        mutableStateOf("all")
    }

    // Khởi tạo selectAll từ repository data dựa trên provider được chọn
    var selectAll by remember(allCustomers, selectedProvider) {
        val filteredCustomers = if (selectedProvider == "all") {
            allCustomers
        } else {
            allCustomers.filter { customer ->
                customer.carrier.lowercase() == selectedProvider.lowercase()
            }
        }

        val shouldSelectAll = filteredCustomers.isNotEmpty() && filteredCustomers.all { it.isSelected }
        android.util.Log.d("MainScreen", "🔄 Initializing selectAll: $shouldSelectAll for provider: $selectedProvider (${filteredCustomers.size} filtered customers)")

        mutableStateOf(shouldSelectAll)
    }
    var customerToDelete by remember {
        mutableStateOf<com.example.sms_app.data.Customer?>(null)
    }
    var showPreviewDialog by remember {
        mutableStateOf(false)
    }
    var selectedCustomerForPreview by remember {
        mutableStateOf<com.example.sms_app.data.Customer?>(null)
    }
    var showSearchDialog by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    
    // Filter customers based on selected provider
    val customers = remember(allCustomers, selectedProvider) {
        if (selectedProvider == "all") {
            allCustomers
        } else {
            allCustomers.filter { customer ->
                customer.carrier.lowercase() == selectedProvider.lowercase()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            mainViewModel.handleExcelFile(it) { msg ->
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MyTopBar(
                selectAll = selectAll,
                onDeleteAll = {
                    mainViewModel.deleteAll()
                    selectAll = false // Reset selectAll khi xóa tất cả
                },
                onUpload = {
                    importLauncher.launch(arrayOf("application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                }
            ) { isChecked ->
                android.util.Log.d("MainScreen", "🔄 SelectAll checkbox changed: $isChecked")
                selectAll = isChecked
                if (isChecked) {
                    android.util.Log.d("MainScreen", "✅ Calling selectAll() for provider: $selectedProvider")
                    mainViewModel.selectAll(selectedProvider)
                } else {
                    android.util.Log.d("MainScreen", "❌ Calling unselectAll() for provider: $selectedProvider")
                    mainViewModel.unselectAll(selectedProvider)
                }
            }
        },
        bottomBar = {
            MyBottomBar(
                providers = providers,
                customers = allCustomers,
                onBottomButton = { },
                onProviderSelected = { provider ->
                    selectedProvider = provider
                },
                onCustomerAdded = {
                    mainViewModel.sync()
                },
                onRemoveDuplicates = {
                    // Xóa trùng lặp số điện thoại
                    val allCustomers = allCustomers
                    val duplicatesMap = allCustomers.groupBy { it.phoneNumber }
                    val duplicateCount = duplicatesMap.values.count { it.size > 1 }
                    
                    if (duplicateCount > 0) {
                        // Xóa trùng lặp - giữ lại khách hàng đầu tiên
                        val uniqueCustomers = allCustomers.groupBy { it.phoneNumber }
                            .map { (_, duplicates) -> duplicates.first() }
                        
                        val duplicatesRemoved = allCustomers.size - uniqueCustomers.size
                        mainViewModel.updateCustomers(uniqueCustomers)
                        
                        android.widget.Toast.makeText(
                            context,
                            "Đã xóa $duplicatesRemoved khách hàng trùng lặp",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Không tìm thấy số điện thoại nào bị trùng lặp",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                // Remove onRestoreUnsentCustomers block and session restore logic
                onUpdateClick = {
                    updateViewModel.checkForUpdates()
                },
                onHomeClick = {
                    IntentUtils.openFacebook(context)
                },
                onSupportClick = {
                    IntentUtils.openZalo(context)
                },
                onSearchClick = {
                    showSearchDialog = true
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val headers = listOf("tên khách hàng", "số điện thoại", "chọn", "xóa")
            val weights = listOf(2.5f, 2.5f, 1f, 1f) // Tăng weight cho cột số điện thoại để hiển thị đầy đủ
            // Định nghĩa màu chung cho tên khách hàng
            val customerNameColor = Color.Blue
            HorizontalDivider()
            Row(
                Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                headers.forEachIndexed { id, item ->
                    val color = when (id) {
                        0 -> customerNameColor    // tên khách hàng
                        1 -> Color.Black          // số điện thoại
                        2 -> Color.Red            // chọn
                        3 -> Color.Red            // xóa
                        else -> Color.Black
                    }
                    Text(
                        text = item.uppercase(),
                        modifier = Modifier
                            .weight(weights[id]),
                        style = TextStyle(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        color = color
                    )
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {

                item {
                    if (customers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Icon với background tròn màu xanh nhạt
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(bottom = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Background tròn màu xanh nhạt
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        drawCircle(
                                            color = androidx.compose.ui.graphics.Color(0xFFE3F2FD),
                                            radius = size.minDimension / 2
                                        )
                                    }

                                    // Icon người dùng
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = androidx.compose.ui.graphics.Color(0xFF2196F3)
                                    )

                                    // Dấu X nhỏ ở góc
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .offset(x = 20.dp, y = (-20).dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Canvas(
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            drawCircle(
                                                color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                                radius = size.minDimension / 2
                                            )
                                        }

                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = androidx.compose.ui.graphics.Color.White
                                        )
                                    }
                                }

                                Text(
                                    text = "Không có dữ liệu",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    color = androidx.compose.ui.graphics.Color.Gray
                                )
                            }
                        }
                    }
                }

                items(customers.size) { index: Int ->
                    val customer = customers[index]
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.clickable {
                            selectedCustomerForPreview = customer
                            showPreviewDialog = true
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            customer.name, modifier = Modifier
                                .weight(weights[0]),
                            style = TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            ),
                            color = customerNameColor
                        )

                        Text(
                            customer.phoneNumber,
                            modifier = Modifier
                                .weight(weights[1]),
                            style = TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp // Font size nhỏ hơn để đảm bảo hiển thị đầy đủ số điện thoại
                            ),
                            maxLines = 1, // Hiển thị trên 1 dòng
                            overflow = TextOverflow.Ellipsis, // Hiển thị ... nếu quá dài
                            softWrap = false // Không wrap text
                        )

                        Checkbox(
                            checked = customer.isSelected, 
                            onCheckedChange = { isChecked ->
                                android.util.Log.d("MainScreen", "🔄 Individual checkbox changed for ${customer.name}: $isChecked")
                                
                                // Nếu đang trong chế độ selectAll và user uncheck 1 checkbox individual
                                if (selectAll && !isChecked) {
                                    android.util.Log.d("MainScreen", "⚠️ User unchecked individual while selectAll=true, turning off selectAll")
                                    selectAll = false
                                }
                                
                                // Cập nhật customer trong danh sách và lưu
                                val updatedCustomers = allCustomers.map { 
                                    if (it.id == customer.id) it.copy(isSelected = isChecked) else it
                                }
                                mainViewModel.updateCustomers(updatedCustomers)
                                
                                // Cập nhật selectAll dựa trên trạng thái mới của updatedCustomers
                                val allSelected = updatedCustomers.all { it.isSelected }
                                val newSelectAll = updatedCustomers.isNotEmpty() && allSelected
                                
                                if (newSelectAll != selectAll) {
                                    android.util.Log.d("MainScreen", "🎯 Auto-updating selectAll: $selectAll -> $newSelectAll")
                                    selectAll = newSelectAll
                                }
                                
                                android.util.Log.d("MainScreen", "💾 Updated customer ${customer.name} isSelected to: $isChecked")
                            }, 
                            modifier = Modifier.weight(weights[2]),
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Red,
                                checkmarkColor = Color.White
                            )
                        )

                        IconButton(
                            onClick = {
                                customerToDelete = customer
                            }, modifier = Modifier
                                .weight(weights[3])
                        ) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Update Dialog
    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            currentVersion = updateViewModel.getCurrentVersion(),
            onUpdateClick = {
                updateViewModel.startUpdate()
            },
            onDismiss = {
                updateViewModel.dismissUpdateDialog()
            },
            isDownloading = isDownloading,
            downloadProgress = downloadProgress
        )
    }
    
    // SMS Preview Dialog
    if (showPreviewDialog && selectedCustomerForPreview != null) {
        val defaultTemplate = mainViewModel.getMessageTemplates().find { 
            it.id == mainViewModel.getDefaultTemplate() 
        } ?: MessageTemplate(1, "Mẫu tin nhắn trống", "Template 1")
        
        SmsPreviewDialog(
            customer = selectedCustomerForPreview!!,
            template = defaultTemplate,
            onDismiss = { 
                showPreviewDialog = false 
                selectedCustomerForPreview = null
            }
        )
    }
    
    // Confirmation dialog for delete individual customer
    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = {
                Text("Xác nhận xóa khách hàng")
            },
            text = {
                Text("Bạn có chắc chắn muốn xóa khách hàng \"${customer.name}\" không? Hành động này không thể hoàn tác.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainViewModel.delete(customer)
                        customerToDelete = null
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { customerToDelete = null }
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    // Search Dialog
    if (showSearchDialog) {
        SearchDialog(
            onDismiss = { showSearchDialog = false },
            onSearch = { query ->
                mainViewModel.searchCustomers(query)
            }
        )
    }


}
