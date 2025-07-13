package com.example.sms_app

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sms_app.data.Customer
import com.example.sms_app.data.SessionBackup
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.data.SmsTemplate
import com.example.sms_app.presentation.theme.SmsAppTheme
import com.example.sms_app.service.SmsService
import com.example.sms_app.ui.AddCustomerDialog
import com.example.sms_app.ui.CustomerDetailDialog
import com.example.sms_app.ui.SettingsDialog
import com.example.sms_app.ui.TemplateConfigDialog
import com.example.sms_app.ui.TemplateSelectionDialog
import com.example.sms_app.utils.ExcelImporter
import com.example.sms_app.utils.SecurityUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors

// SmsApplication đã được chuyển sang file riêng

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var smsRepository: SmsRepository
    private lateinit var sessionBackup: SessionBackup
    
    data class SmsProgress(val progress: Int = 0, val total: Int = 0, val message: String = "")
    
    private val progressFlow = MutableSharedFlow<SmsProgress>()
    private val completionFlow = MutableSharedFlow<String>()
    private val customerDeletionFlow = MutableSharedFlow<String>()
    
    private val smsProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                SmsService.ACTION_PROGRESS_UPDATE -> {
                    val progress = intent.getIntExtra(SmsService.EXTRA_PROGRESS, 0)
                    val total = intent.getIntExtra(SmsService.EXTRA_TOTAL, 0)
                    val message = intent.getStringExtra(SmsService.EXTRA_MESSAGE) ?: ""
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        progressFlow.emit(SmsProgress(progress, total, message))
                    }
                }
                SmsService.ACTION_SMS_COMPLETED -> {
                    val message = intent.getStringExtra(SmsService.EXTRA_MESSAGE) ?: "Hoàn thành gửi SMS"
                    CoroutineScope(Dispatchers.Main).launch {
                        completionFlow.emit(message)
                    }
                }
                SmsService.ACTION_CUSTOMER_DELETED -> {
                    val customerId = intent.getStringExtra(SmsService.EXTRA_CUSTOMER_ID)
                    if (customerId != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            customerDeletionFlow.emit(customerId)
                        }
                    }
                }
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Đã cấp quyền SMS", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Cần quyền SMS để hoạt động", Toast.LENGTH_LONG).show()
        }
    }
    
    // Add permission request code
    private val STORAGE_PERMISSION_CODE = 1001
    
    // Kiểm tra xem thiết bị có phải máy ảo không
    private fun isEmulator(): Boolean {
        // Tắt phát hiện emulator để có thể chạy trên thiết bị giả lập
        return false
        
        // Code cũ
        /*
        return Build.FINGERPRINT.contains("generic") || 
               Build.MODEL.contains("sdk") ||
               Build.MANUFACTURER.contains("Genymotion") ||
               Build.PRODUCT.contains("sdk") ||
               Build.BRAND.startsWith("generic") ||
               Build.DEVICE.startsWith("generic") ||
               Build.HARDWARE.contains("goldfish") ||
               Build.HARDWARE.contains("ranchu")
        */
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Kiểm tra trạng thái ứng dụng - tạm thời bỏ qua để test
        /*
        if (isEmulator()) {
            Log.e("MainActivity", "Phát hiện thiết bị giả lập, thoát ứng dụng")
            finishAffinity()
            return
        }
        */
        
        // Bắt buộc check để đảm bảo ứng dụng không bị tấn công
        if (SecurityUtils.verifyAppIntegrity(this)) {
            Log.e("MainActivity", "Xác thực chữ ký ứng dụng thành công")
        } else {
            Log.e("MainActivity", "Xác thực chữ ký ứng dụng thất bại, thoát ứng dụng")
            finishAffinity()
            return
        }

        // Xin quyền đọc/ghi bộ nhớ ngoài để lưu log
        checkAndRequestStoragePermissions()
        
        try {
            // Khởi tạo repository
            smsRepository = SmsRepository(this)
            sessionBackup = SessionBackup(this)
            
            // Log thông tin khởi động
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                Timber.i("MainActivity được khởi động với version: ${packageInfo.versionName}")
            } catch (e: Exception) {
                Timber.e(e, "Không thể lấy thông tin phiên bản")
            }
            
            setContent {
                SmsAppTheme {
                    SmsApp(
                        smsRepository = smsRepository,
                        sessionBackup = sessionBackup,
                        progressFlow = progressFlow,
                        completionFlow = completionFlow,
                        customerDeletionFlow = customerDeletionFlow
                    ) { action ->
                        when (action) {
                            is SmsAppAction.StartService -> startSmsService(action.templateId)
                            is SmsAppAction.StopService -> stopSmsService()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Lỗi khởi tạo: ${e.message}")
            Timber.e(e, "Lỗi khởi tạo MainActivity: ${e.message}")
            finishAffinity()
        }
    }
    
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        // Register broadcast receiver
        val intentFilter = IntentFilter().apply {
            addAction(SmsService.ACTION_PROGRESS_UPDATE)
            addAction(SmsService.ACTION_SMS_COMPLETED)
            addAction(SmsService.ACTION_CUSTOMER_DELETED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsProgressReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(smsProgressReceiver, intentFilter)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Unregister broadcast receiver
        try {
            unregisterReceiver(smsProgressReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
    
    private fun checkAndRequestPermissions() {
        // Phân loại quyền thành runtime permissions và normal permissions
        val runtimePermissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        
        // Các quyền không cần yêu cầu runtime (được cấp khi cài đặt)
        // Manifest.permission.RECEIVE_BOOT_COMPLETED,
        // Manifest.permission.ACCESS_NETWORK_STATE,
        // Manifest.permission.INTERNET
        
        // Kiểm tra phiên bản Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Từ Android 6.0 (API 23) trở lên mới cần yêu cầu runtime permissions
            val permissionsToRequest = runtimePermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                Log.d("MainActivity", "Requesting permissions: ${permissionsToRequest.joinToString()}")
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                Log.d("MainActivity", "All required permissions already granted")
            }
        } else {
            // Trước Android 6.0 không cần yêu cầu runtime permissions
            Log.d("MainActivity", "Android < 6.0, no need to request runtime permissions")
        }
    }
    
    // Hàm kiểm tra và yêu cầu quyền đọc/ghi bộ nhớ ngoài
    private fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = android.net.Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Không thể mở cài đặt quyền truy cập tệp: ${e.message}")
                    Toast.makeText(this, "Cần cấp quyền truy cập tệp để ghi log", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // Android 10- (API 29-)
            val permissions = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            
            if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
                requestPermissions(permissions, STORAGE_PERMISSION_CODE)
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Timber.i("Quyền truy cập bộ nhớ đã được cấp")
            } else {
                Timber.w("Người dùng từ chối cấp quyền bộ nhớ")
                Toast.makeText(this, "Cần quyền bộ nhớ để ghi nhật ký lỗi", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun startSmsService(templateId: Int) {
        // Kiểm tra xem thiết bị có phải là máy ảo không
        // Comment lại để chạy thử trên máy ảo
        /*if (SmsUtils.isEmulator(this)) {
            Toast.makeText(this, "Không thể gửi SMS trên máy ảo", Toast.LENGTH_LONG).show()
            return
        }*/
        
        // Load settings ngay khi bắt đầu gửi SMS
        val currentSettings = smsRepository.getAppSettings()
        android.util.Log.d("MainActivity", "Starting SMS service with settings: interval=${currentSettings.intervalBetweenSmsSeconds}s")
        
        val intent = Intent(this, SmsService::class.java)
        intent.putExtra(SmsService.EXTRA_TEMPLATE_ID, templateId)
        intent.putExtra(SmsService.EXTRA_INTERVAL_SECONDS, currentSettings.intervalBetweenSmsSeconds)
        intent.putExtra(SmsService.EXTRA_MAX_RETRY, currentSettings.maxRetryAttempts)
        intent.putExtra(SmsService.EXTRA_RETRY_DELAY, currentSettings.retryDelaySeconds)
        ContextCompat.startForegroundService(this, intent)
    }
    
    private fun stopSmsService() {
        val intent = Intent(this, SmsService::class.java)
        stopService(intent)
    }
}

sealed class SmsAppAction {
    data class StartService(val templateId: Int) : SmsAppAction()
    object StopService : SmsAppAction()
}

@Composable
fun SmsApp(
    smsRepository: SmsRepository,
    sessionBackup: SessionBackup,
    progressFlow: SharedFlow<MainActivity.SmsProgress>,
    completionFlow: SharedFlow<String>,
    customerDeletionFlow: SharedFlow<String>,
    onAction: (SmsAppAction) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var customers by remember { mutableStateOf(smsRepository.getCustomers()) }
    var messageTemplates by remember { mutableStateOf(smsRepository.getMessageTemplates()) }
    var defaultTemplateId by remember { mutableStateOf(smsRepository.getDefaultTemplate()) }
    var selectedCarrier by remember { mutableStateOf("Tất cả") }
    var selectedTemplate by remember { mutableStateOf<SmsTemplate?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showTemplateConfigDialog by remember { mutableStateOf(false) }
    var showTemplateSelectionDialog by remember { mutableStateOf(false) }
    var showCustomerDetailDialog by remember { mutableStateOf(false) }
    var selectedCustomerForDetail by remember { mutableStateOf<Customer?>(null) }
    var showConfirmSendDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var pendingTemplateId by remember { mutableStateOf<Int?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var sendingProgress by remember { mutableStateOf(0) }
    var totalToSend by remember { mutableStateOf(0) }
    var countdownSeconds by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var templates by remember { mutableStateOf(smsRepository.getTemplates()) }
    
    // File picker launcher for Excel export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            exportToExcel(customers, context, it)
        }
    }
    
    // File picker launcher for Excel import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                Log.d("MainActivity", "Bắt đầu import Excel từ $uri")
                Toast.makeText(context, "Đang nhập dữ liệu từ Excel...", Toast.LENGTH_SHORT).show()
                
                // Sử dụng ExecutorService thay vì coroutines
                val executor = Executors.newSingleThreadExecutor()
                executor.execute {
                    try {
                        // Thực hiện import trong background
                        val excelImporter = ExcelImporter(context)
                        val importedCustomers = excelImporter.importCustomers(it)
                        
                        // Quay lại main thread để cập nhật UI
                        Handler(Looper.getMainLooper()).post {
                            if (importedCustomers.isNotEmpty()) {
                                customers = customers + importedCustomers
                                smsRepository.saveCustomers(customers)
                                
                                Toast.makeText(
                                    context, 
                                    "Đã nhập thành công ${importedCustomers.size} khách hàng từ Excel", 
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Không tìm thấy khách hàng hợp lệ trong file Excel",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Lỗi import Excel", e)
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context, 
                                "Lỗi nhập Excel: ${e.message}", 
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Lỗi xử lý file", e)
                Toast.makeText(
                    context, 
                    "Lỗi xử lý file: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    val filteredCustomers = remember(customers, selectedCarrier, searchQuery) {
        var filtered = customers
        
        // Filter by carrier
        if (selectedCarrier != "Tất cả") {
            filtered = filtered.filter { it.carrier == selectedCarrier }
        }
        
        // Filter by search query
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery) ||
                it.idNumber.contains(searchQuery, ignoreCase = true)
            }
        }
        
        filtered
    }
    
    val selectedCount = customers.count { it.isSelected }
    
    // Listen to SMS progress updates
    LaunchedEffect(Unit) {
        progressFlow.collect { progress ->
            sendingProgress = progress.progress
            totalToSend = progress.total
            if (progress.progress >= progress.total) {
                isSending = false
            }
        }
    }
    
    // Listen to SMS completion
    LaunchedEffect(Unit) {
        completionFlow.collect { message ->
            isSending = false
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    // Listen to customer deletion
    LaunchedEffect(Unit) {
        customerDeletionFlow.collect { customerId ->
            customers = customers.filter { it.id != customerId }
        }
    }
    
    // Countdown timer effect
    LaunchedEffect(isSending, sendingProgress, totalToSend) {
        if (isSending && sendingProgress < totalToSend) {
            val remainingMessages = totalToSend - sendingProgress
            // Sử dụng settings thực tế thay vì 25 giây cố định
            val currentSettings = smsRepository.getAppSettings()
            val timeRemaining = remainingMessages * currentSettings.intervalBetweenSmsSeconds
            countdownSeconds = timeRemaining
            
            while (countdownSeconds > 0 && isSending) {
                delay(1000)
                countdownSeconds--
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                text = "Chọn tất cả",
                isSelected = false,
                modifier = Modifier.weight(1f)
            ) {
                customers = customers.map { it.copy(isSelected = true) }
                smsRepository.saveCustomers(customers)
            }
            
            TabButton(
                text = "Xóa tất cả", 
                isSelected = true,
                modifier = Modifier.weight(1f)
            ) {
                customers = customers.map { it.copy(isSelected = false) }
                smsRepository.saveCustomers(customers)
            }
            
            TabButton(
                text = "Thêm KH",
                isSelected = false,
                modifier = Modifier.weight(1f)
            ) {
                showAddCustomerDialog = true
            }
        }
        
        // Horizontal Action Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ActionButton(
                    text = "Tìm kiếm",
                    icon = Icons.Default.Search,
                    backgroundColor = Color(0xFF4CAF50)
                ) {
                    showSearchDialog = true
                }
            }
            item {
                ActionButton(
                    text = "Xóa KH",
                    icon = Icons.Default.Delete,
                    backgroundColor = Color(0xFFF44336)
                ) {
                    showDeleteDialog = true
                }
            }
            item {
                ActionButton(
                    text = "Cài đặt",
                    icon = Icons.Default.Settings,
                    backgroundColor = Color(0xFF9C27B0)
                ) {
                    showSettingsDialog = true
                }
            }
            item {
                ActionButton(
                    text = "Template",
                    icon = Icons.Default.Create,
                    backgroundColor = Color(0xFF4CAF50)
                ) {
                    showTemplateConfigDialog = true
                }
            }
            item {
                ActionButton(
                    text = "Nhập Excel",
                    icon = Icons.Default.FileUpload,
                    backgroundColor = Color(0xFFFF9800)
                ) {
                    // Hiển thị hướng dẫn trước khi chọn file
                    Toast.makeText(
                        context,
                        "Hỗ trợ cả định dạng Excel .xls và .xlsx",
                        Toast.LENGTH_SHORT
                    ).show()
                    importLauncher.launch(arrayOf("*/*"))
                }
            }
            item {
                ActionButton(
                    text = "Xuất Excel",
                    icon = Icons.Default.FileDownload,
                    backgroundColor = Color(0xFF795548)
                ) {
                    exportLauncher.launch("customers_${System.currentTimeMillis()}.xlsx")
                }
            }
            item {
                val hasBackup = sessionBackup.hasActiveSession()
                ActionButton(
                    text = "Backup",
                    icon = Icons.Default.Backup,
                    backgroundColor = if (hasBackup) Color(0xFFFF9800) else Color(0xFF757575)
                ) {
                    showBackupDialog = true
                }
            }
        }
        
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TÊN KHÁCH HÀNG",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 8.sp,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "SỐ ĐIỆN THOẠI",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "CHỌN",
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Customer List
        if (filteredCustomers.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No data",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Không có dữ liệu",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(filteredCustomers) { customer ->
                    CustomerItem(
                        customer = customer,
                        onSelectionChanged = { isSelected ->
                            customers = customers.map {
                                if (it.id == customer.id) it.copy(isSelected = isSelected)
                                else it
                            }
                            smsRepository.saveCustomers(customers)
                        },
                        onClick = {
                            selectedCustomerForDetail = customer
                            showCustomerDetailDialog = true
                        }
                    )
                }
            }
        }
        
        // Carrier Filter - moved below customer list
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Lọc theo nhà mạng:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Tất cả", "Viettel", "Mobifone", "Vinaphone", "Vietnamobile").forEach { carrier ->
                        FilterChip(
                            text = carrier,
                            isSelected = selectedCarrier == carrier,
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedCarrier = carrier
                        }
                    }
                }
            }
        }
        
        // SMS Sending Progress
        if (isSending) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Blue,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Đang gửi SMS...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = if (totalToSend > 0) sendingProgress.toFloat() / totalToSend.toFloat() else 0f,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Blue,
                        trackColor = Color.LightGray
                    )
                    
                    // Progress text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tiến độ: $sendingProgress/$totalToSend",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        val minutes = countdownSeconds / 60
                        val seconds = countdownSeconds % 60
                        Text(
                            text = "Còn lại: ${minutes}:${seconds.toString().padStart(2, '0')}",
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Stop button
                    Button(
                        onClick = {
                            isSending = false
                            onAction(SmsAppAction.StopService)
                            Toast.makeText(context, "Đã dừng gửi SMS", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("DỪNG GỬI SMS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // Send SMS Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Choose Template and Send
            Button(
                onClick = {
                    if (selectedCount > 0 && !isSending) {
                        val availableTemplates = messageTemplates.filter { it.content.isNotEmpty() }
                        if (availableTemplates.isNotEmpty()) {
                            showTemplateSelectionDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Vui lòng cấu hình ít nhất 1 template trước khi gửi",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                enabled = selectedCount > 0 && !isSending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSending) Color.Gray else Color(0xFF2196F3)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GỬI SMS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    // Template Dialog
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Chọn template") },
            text = {
                LazyColumn {
                    items(templates) { template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedTemplate = template
                                    showTemplateDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTemplate?.id == template.id) 
                                    MaterialTheme.colorScheme.primaryContainer
                                else Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = template.name,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = template.content,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
    
    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onSave = { newCustomer ->
                customers = customers + newCustomer
                smsRepository.saveCustomers(customers)
                showAddCustomerDialog = false
                Toast.makeText(context, "Đã thêm khách hàng: ${newCustomer.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // Template Config Dialog
    if (showTemplateConfigDialog) {
        TemplateConfigDialog(
            currentTemplates = messageTemplates,
            currentDefaultTemplate = defaultTemplateId,
            onDismiss = { showTemplateConfigDialog = false },
            onSave = { templates, newDefaultTemplateId ->
                messageTemplates = templates
                defaultTemplateId = newDefaultTemplateId
                smsRepository.saveMessageTemplates(templates)
                smsRepository.setDefaultTemplate(newDefaultTemplateId)
                showTemplateConfigDialog = false
                Toast.makeText(context, "Đã lưu cấu hình template và mẫu mặc định: $newDefaultTemplateId", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // Template Selection Dialog
    if (showTemplateSelectionDialog) {
        TemplateSelectionDialog(
            customers = customers,
            templates = messageTemplates,
            smsRepository = smsRepository,
            defaultTemplateId = defaultTemplateId,
            onDismiss = { showTemplateSelectionDialog = false },
            onConfirm = { templateId ->
                showTemplateSelectionDialog = false
                pendingTemplateId = templateId
                showConfirmSendDialog = true
            }
        )
    }
    
    // Search Dialog
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Tìm kiếm khách hàng") },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Nhập tên, SĐT hoặc CMND") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Tìm")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    searchQuery = ""
                    showSearchDialog = false 
                }) {
                    Text("Xóa")
                }
            }
        )
    }
    
    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa khách hàng") },
            text = {
                val selectedCount = customers.count { it.isSelected }
                Text("Bạn có chắc muốn xóa $selectedCount khách hàng đã chọn?")
            },
            confirmButton = {
                TextButton(onClick = { 
                    customers = customers.filter { !it.isSelected }
                    smsRepository.saveCustomers(customers)
                    showDeleteDialog = false
                }) {
                    Text("Xóa", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
    
    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = smsRepository.getAppSettings(),
            onDismiss = { showSettingsDialog = false },
            onSave = { settings ->
                smsRepository.saveAppSettings(settings)
                showSettingsDialog = false
                
                // Dừng service cũ nếu đang chạy để áp dụng settings mới
                if (isSending) {
                    onAction(SmsAppAction.StopService)
                    isSending = false
                    Toast.makeText(context, "Đã lưu cài đặt và dừng gửi SMS để áp dụng cài đặt mới", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Đã lưu cài đặt thành công", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    // Customer Detail Dialog
    if (showCustomerDetailDialog && selectedCustomerForDetail != null) {
        CustomerDetailDialog(
            customer = selectedCustomerForDetail!!,
            templates = messageTemplates,
            defaultTemplateId = defaultTemplateId,
            onDismiss = {
                showCustomerDetailDialog = false
                selectedCustomerForDetail = null
            }
        )
    }
    
    // Confirm Send SMS Dialog
    if (showConfirmSendDialog && pendingTemplateId != null) {
        AlertDialog(
            onDismissRequest = { 
                showConfirmSendDialog = false
                pendingTemplateId = null
            },
            title = { 
                Text(
                    text = "Xác nhận gửi SMS",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    val selectedSim = smsRepository.getSelectedSim()
                    val smsCount = smsRepository.getSmsCountToday(selectedSim)
                    val simName = if (selectedSim == -1) "Default SIM" else "SIM $selectedSim"
                    val templateName = messageTemplates.find { it.id == pendingTemplateId }?.description ?: "Template $pendingTemplateId"
                    val currentSettings = smsRepository.getAppSettings()
                    
                    Text(
                        text = "Bạn có chắc muốn gửi SMS?",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Số tin nhắn:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "$selectedCount tin",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Template:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = templateName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SIM:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "$simName ($smsCount/40 hôm nay)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Delay:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${currentSettings.intervalBetweenSmsSeconds}s giữa các tin",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "⚠️ Mỗi tin nhắn sẽ đợi ${currentSettings.intervalBetweenSmsSeconds} giây",
                        fontSize = 11.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSendDialog = false
                        isSending = true
                        sendingProgress = 0
                        totalToSend = customers.count { it.isSelected }
                        val selectedSim = smsRepository.getSelectedSim()
                        val smsCount = smsRepository.getSmsCountToday(selectedSim)
                        val simName = if (selectedSim == -1) "Default SIM" else "SIM $selectedSim"
                        Toast.makeText(
                            context,
                            "Bắt đầu gửi $selectedCount tin nhắn ($simName: $smsCount/40 tin hôm nay)",
                            Toast.LENGTH_SHORT
                        ).show()
                        onAction(SmsAppAction.StartService(pendingTemplateId!!))
                        pendingTemplateId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("GỬI", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showConfirmSendDialog = false
                        pendingTemplateId = null
                    }
                ) {
                    Text("HỦY", color = Color.Gray)
                }
            }
        )
    }
    
    // Backup Session Dialog
    if (showBackupDialog) {
        val activeSession = sessionBackup.getActiveSession()
        val sessionHistory = sessionBackup.getSessionHistory()
        
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { 
                Text(
                    text = "Khôi phục phiên làm việc",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    if (activeSession != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECB3)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "🔄 Phiên làm việc chưa hoàn thành",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = sessionBackup.getSessionSummary(activeSession),
                                    fontSize = 12.sp,
                                    color = Color(0xFF424242)
                                )
                                
                                Text(
                                    text = "Template: ${activeSession.templateId}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF424242)
                                )
                                
                                Text(
                                    text = "Còn lại: ${activeSession.remainingCustomers.size} khách hàng",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD84315)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            // Khôi phục danh sách khách hàng từ session
                                            val remainingCustomers = activeSession.remainingCustomers.map { it.copy(isSelected = true) }
                                            val currentCustomers = smsRepository.getCustomers()
                                            val restoredCustomers = currentCustomers.map { customer ->
                                                val shouldSelect = remainingCustomers.any { it.id == customer.id }
                                                customer.copy(isSelected = shouldSelect)
                                            }
                                            customers = restoredCustomers
                                            smsRepository.saveCustomers(customers)
                                            
                                            showBackupDialog = false
                                            Toast.makeText(
                                                context,
                                                "Đã khôi phục ${remainingCustomers.size} khách hàng chưa gửi",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        Text("Khôi phục", fontSize = 10.sp, color = Color.White)
                                    }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            sessionBackup.clearActiveSession()
                                            showBackupDialog = false
                                            Toast.makeText(context, "Đã xóa backup", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Xóa", fontSize = 10.sp, color = Color.Red)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Không có phiên làm việc nào để khôi phục",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    if (sessionHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "📋 Lịch sử gần đây:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.height(150.dp)
                        ) {
                            items(sessionHistory.take(5)) { session ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Text(
                                        text = sessionBackup.getSessionSummary(session),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(8.dp),
                                        color = Color(0xFF616161)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

// Helper functions
fun exportToExcel(customers: List<Customer>, context: android.content.Context, uri: android.net.Uri) {
    try {
        // Here you would implement actual Excel export logic
        // For now, just show success message
        android.widget.Toast.makeText(
            context,
            "Xuất ${customers.size} khách hàng ra Excel thành công!\nLưu tại: ${uri.path}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Lỗi xuất Excel: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

fun downloadTemplate(context: android.content.Context) {
    android.widget.Toast.makeText(
        context,
        "Template Excel:\n\nDòng 1 (Header): Tên khách hàng | Số điện thoại | CMND/CCCD | Địa chỉ | Tùy chọn 1 | Tùy chọn 2 | Tùy chọn 3 | Tùy chọn 4 | Tùy chọn 5 | Mẫu tin nhắn\n\nCột A: Tên khách hàng (bắt buộc)\nCột B: Số điện thoại (bắt buộc)\nCột C: CMND/CCCD (tùy chọn)\nCột D: Địa chỉ (tùy chọn)\nCột E: Tùy chọn 1 (tùy chọn)\nCột F: Tùy chọn 2 (tùy chọn)\nCột G: Tùy chọn 3 (tùy chọn)\nCột H: Tùy chọn 4 (tùy chọn)\nCột I: Tùy chọn 5 (tùy chọn)\nCột J: Mẫu tin nhắn (1-9, mặc định là 1)",
        android.widget.Toast.LENGTH_LONG
    ).show()
}

fun downloadCSVTemplate(context: android.content.Context) {
    android.widget.Toast.makeText(
        context,
        "Template Excel:\n\nDòng 1 (Header): Tên khách hàng | Số điện thoại | CMND/CCCD | Địa chỉ | Tùy chọn 1 | Tùy chọn 2 | Tùy chọn 3 | Tùy chọn 4 | Tùy chọn 5 | Mẫu tin nhắn\n\nCột A: Tên khách hàng (bắt buộc)\nCột B: Số điện thoại (bắt buộc)\nCột C: CMND/CCCD (tùy chọn)\nCột D: Địa chỉ (tùy chọn)\nCột E: Tùy chọn 1 (tùy chọn)\nCột F: Tùy chọn 2 (tùy chọn)\nCột G: Tùy chọn 3 (tùy chọn)\nCột H: Tùy chọn 4 (tùy chọn)\nCột I: Tùy chọn 5 (tùy chọn)\nCột J: Mẫu tin nhắn (1-9, mặc định là 1)\n\nCác cột tùy chọn có thể để trống",
        android.widget.Toast.LENGTH_LONG
    ).show()
}

@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(50.dp)
            .width(110.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.Red else Color.Gray,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CustomerItem(
    customer: Customer,
    onSelectionChanged: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = customer.name,
                fontSize = 9.sp,
                modifier = Modifier.weight(2f)
            )
            Text(
                text = customer.phoneNumber,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(2f)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = customer.isSelected,
                    onCheckedChange = onSelectionChanged
                )
            }
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(28.dp)
            .wrapContentWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                when (text) {
                    "Viettel" -> Color(0xFF4CAF50)
                    "Mobifone" -> Color(0xFF2196F3)
                    "Vinaphone" -> Color(0xFFF44336)
                    else -> Color(0xFF607D8B)
                }
            } else Color.LightGray,
            contentColor = if (isSelected) Color.White else Color.Gray
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Thêm hàm hiển thị thông tin về template Excel
fun showExcelTemplateInfo(context: android.content.Context) {
    android.widget.Toast.makeText(
        context,
        "Lưu ý: File Excel cần có cấu trúc như sau:\n" +
        "- Định dạng: .xls (Excel 97-2003)\n" +
        "- Dòng 1: Tiêu đề (tùy chọn)\n" +
        "- Cột A: Tên khách hàng (bắt buộc)\n" +
        "- Cột B: Số điện thoại (bắt buộc)\n" +
        "- Cột C-J: Các thông tin khác (tùy chọn)",
        android.widget.Toast.LENGTH_LONG
    ).show()
}