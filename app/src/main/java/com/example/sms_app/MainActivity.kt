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
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sms_app.data.Customer
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.data.SmsTemplate
import com.example.sms_app.data.SessionBackup
import com.example.sms_app.service.SmsService
import com.example.sms_app.ui.AddCustomerDialog
import com.example.sms_app.ui.CustomerDetailDialog
import com.example.sms_app.ui.TemplateConfigDialog
import com.example.sms_app.ui.TemplateSelectionDialog
import com.example.sms_app.ui.SettingsDialog
import com.example.sms_app.ui.theme.SmsappTheme
import com.example.sms_app.utils.ExcelImporter
import com.example.sms_app.utils.SmsUtils
import com.example.sms_app.utils.SecurityUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import android.app.Application
import java.io.File
import timber.log.Timber
import android.os.Environment
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// SmsApplication đã được chuyển sang file riêng

class MainActivity : ComponentActivity() {
    private lateinit var smsRepository: SmsRepository
    private lateinit var sessionBackup: SessionBackup
    
    data class SmsProgress(val progress: Int, val total: Int, val message: String)
    
    private val progressFlow = MutableSharedFlow<SmsProgress>()
    private val completionFlow = MutableSharedFlow<String>()
    private val customerDeletionFlow = MutableSharedFlow<String>()
    
    private val smsProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MainActivity", "📡 Received broadcast: ${intent?.action}")
            when (intent?.action) {
                SmsService.ACTION_PROGRESS_UPDATE -> {
                    val progress = intent.getIntExtra(SmsService.EXTRA_PROGRESS, 0)
                    val total = intent.getIntExtra(SmsService.EXTRA_TOTAL, 0)
                    val message = intent.getStringExtra(SmsService.EXTRA_MESSAGE) ?: ""

                    Log.d("MainActivity", "📊 Progress update: $progress/$total - $message")
                    CoroutineScope(Dispatchers.Main).launch {
                        progressFlow.emit(SmsProgress(progress, total, message))
                    }
                }
                SmsService.ACTION_SMS_COMPLETED -> {
                    val message = intent.getStringExtra(SmsService.EXTRA_MESSAGE) ?: "Hoàn thành gửi SMS"
                    Log.d("MainActivity", "🏁 SMS completed: $message")
                    CoroutineScope(Dispatchers.Main).launch {
                        completionFlow.emit(message)
                    }
                }
                SmsService.ACTION_CUSTOMER_DELETED -> {
                    val customerId = intent.getStringExtra(SmsService.EXTRA_CUSTOMER_ID)
                    Log.d("MainActivity", "🗑️ Customer deleted: $customerId")
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
            Toast.makeText(this, "✅ Đã cấp quyền SMS thành công!", Toast.LENGTH_SHORT).show()
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            showPermissionDeniedDialog(deniedPermissions.toList())
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

        // Yêu cầu quyền SMS ngay khi khởi động ứng dụng
        checkAndRequestPermissions()
        
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
                SmsappTheme {
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
        Log.d("MainActivity", "📡 Broadcast receiver registered")
    }
    
    override fun onPause() {
        super.onPause()
        // Unregister broadcast receiver
        try {
            unregisterReceiver(smsProgressReceiver)
            Log.d("MainActivity", "📡 Broadcast receiver unregistered")
        } catch (e: Exception) {
            Log.w("MainActivity", "📡 Receiver might not be registered: ${e.message}")
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
                // Hiển thị dialog giải thích trước khi yêu cầu quyền
                val permissionNames = permissionsToRequest.map { permission ->
                    when (permission) {
                        Manifest.permission.SEND_SMS -> "Gửi SMS"
                        Manifest.permission.READ_SMS -> "Đọc SMS"
                        Manifest.permission.RECEIVE_SMS -> "Nhận SMS"
                        Manifest.permission.READ_PHONE_STATE -> "Đọc trạng thái điện thoại"
                        else -> permission
                    }
                }
                
                val message = "Ứng dụng cần các quyền sau để hoạt động:\n\n" +
                        "• ${permissionNames.joinToString("\n• ")}\n\n" +
                        "Vui lòng cấp quyền để ứng dụng hoạt động đúng chức năng."
                
                android.app.AlertDialog.Builder(this)
                    .setTitle("Yêu cầu quyền")
                    .setMessage(message)
                    .setPositiveButton("Cấp quyền") { _, _ ->
                        Log.d("MainActivity", "Requesting permissions: ${permissionsToRequest.joinToString()}")
                        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                    .setNegativeButton("Để sau") { _, _ ->
                        Toast.makeText(
                            this,
                            "⚠️ Ứng dụng cần quyền SMS để hoạt động đúng chức năng",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .setCancelable(false)
                    .show()
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
        // Kiểm tra quyền SMS trước khi bắt đầu
        if (!SmsUtils.hasRequiredPermissions(this)) {
            Toast.makeText(this, "⚠️ Ứng dụng chưa được cấp đủ quyền SMS. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show()
            // Mở cài đặt ứng dụng thay vì yêu cầu lại quyền
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Không thể mở cài đặt", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Kiểm tra xem thiết bị có phải là máy ảo không
        // Comment lại để chạy thử trên máy ảo
        /*if (SmsUtils.isEmulator(this)) {
            Toast.makeText(this, "Không thể gửi SMS trên máy ảo", Toast.LENGTH_LONG).show()
            return
        }*/

        // Load settings ngay khi bắt đầu gửi SMS
        val currentSettings = smsRepository.getAppSettings()
        android.util.Log.d("MainActivity", "Starting SMS service with settings: interval=${currentSettings.intervalBetweenSmsSeconds}s")
        
        // Kiểm tra template có tồn tại và có nội dung không
        val template = smsRepository.getMessageTemplates().find { it.id == templateId }
        if (template == null) {
            Toast.makeText(this, "⚠️ Không tìm thấy template ID: $templateId", Toast.LENGTH_LONG).show()
            return
        }
        
        if (template.content.isBlank()) {
            Toast.makeText(this, "⚠️ Template ${template.description} không có nội dung. Vui lòng cập nhật template trước khi gửi.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Kiểm tra đã chọn khách hàng chưa
        val selectedCustomers = smsRepository.getCustomers().filter { it.isSelected }
        if (selectedCustomers.isEmpty()) {
            Toast.makeText(this, "⚠️ Vui lòng chọn ít nhất một khách hàng trước khi gửi SMS", Toast.LENGTH_LONG).show()
            return
        }
        
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

    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        val permissionNames = deniedPermissions.map { permission ->
            when (permission) {
                Manifest.permission.SEND_SMS -> "Gửi SMS"
                Manifest.permission.READ_SMS -> "Đọc SMS"
                Manifest.permission.RECEIVE_SMS -> "Nhận SMS"
                Manifest.permission.READ_PHONE_STATE -> "Đọc trạng thái điện thoại"
                else -> permission
            }
        }

        val message = "Ứng dụng cần các quyền sau để hoạt động:\n\n" +
                "❌ ${permissionNames.joinToString("\n❌ ")}\n\n" +
                "Vui lòng vào Cài đặt > Ứng dụng > SMS App > Quyền để cấp quyền."

        android.app.AlertDialog.Builder(this)
            .setTitle("Quyền bị từ chối")
            .setMessage(message)
            .setPositiveButton("Mở cài đặt") { _, _ ->
                // Mở cài đặt ứng dụng
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Không thể mở cài đặt", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Để sau") { _, _ ->
                Toast.makeText(
                    this,
                    "⚠️ Ứng dụng cần quyền SMS để hoạt động đúng chức năng",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
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
    var searchQuery by remember { mutableStateOf("") }
    var templates by remember { mutableStateOf(smsRepository.getTemplates()) }
    var countdownTime by remember { mutableStateOf(0) }
    
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
            
            // Cập nhật UI với thông tin mới nhất
            if (progress.message.contains("Còn lại:")) {
                try {
                    // Kiểm tra xem thông báo có chứa thông tin về tổng thời gian không
                    if (progress.message.contains("Tổng:")) {
                        // Trích xuất thời gian đếm ngược cho tin nhắn hiện tại
                        val timePattern = "Còn lại: (\\d+)s".toRegex()
                        val matchResult = timePattern.find(progress.message)
                        val extractedTime = matchResult?.groupValues?.get(1)?.toIntOrNull()
                        
                        // Trích xuất tổng thời gian còn lại
                        val totalTimePattern = "Tổng: (\\d+)m:(\\d+)s".toRegex()
                        val totalMatchResult = totalTimePattern.find(progress.message)
                        val totalMinutes = totalMatchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        val totalSeconds = totalMatchResult?.groupValues?.get(2)?.toIntOrNull() ?: 0
                        
                        // Tính tổng thời gian còn lại tính bằng giây
                        val totalRemainingTime = totalMinutes * 60 + totalSeconds
                        
                        if (extractedTime != null) {
                            Log.d("MainActivity", "⏱️ Countdown for current message: ${extractedTime}s")
                            Log.d("MainActivity", "⏱️ Total remaining time: ${totalMinutes}m:${totalSeconds}s (${totalRemainingTime}s)")
                            
                            // Cập nhật biến state để hiển thị trong UI
                            countdownTime = extractedTime
                        }
                    } else {
                        // Xử lý theo cách cũ nếu không có thông tin tổng thời gian
                        val timePattern = "Còn lại: (\\d+)s".toRegex()
                        val matchResult = timePattern.find(progress.message)
                        val extractedTime = matchResult?.groupValues?.get(1)?.toIntOrNull()
                        
                        if (extractedTime != null) {
                            Log.d("MainActivity", "⏱️ Countdown between messages: ${extractedTime}s")
                            countdownTime = extractedTime
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Lỗi khi trích xuất thời gian đếm ngược: ${e.message}")
                }
            }
            
            // Chỉ đánh dấu hoàn thành khi đã gửi hết tất cả tin nhắn
            if (progress.progress >= progress.total && progress.total > 0) {
                Log.d("MainActivity", "🏁 SMS sending complete based on progress: $progress")
                // Không set isSending = false ở đây, để đợi thông báo hoàn thành từ service
            }
        }
    }
    
    // Listen to SMS completion
    LaunchedEffect(Unit) {
        completionFlow.collect { message ->
            Log.d("MainActivity", "🏁 SMS sending complete: $message")
            // Đảm bảo UI loading được tắt khi nhận thông báo hoàn thành
            isSending = false
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    // Check service state periodically to detect disconnected service
    LaunchedEffect(key1 = isSending) {
        if (isSending) {
            while (true) {
                delay(5000) // Check every 5 seconds
                if (!isServiceRunning(context, SmsService::class.java) && isSending) {
                    // Service is not running but UI still shows sending state
                    // Không tự động dừng UI khi service dừng, để service tự gửi thông báo hoàn thành
                    Log.d("MainActivity", "🔍 Service stopped but waiting for completion message")
                    
                    // Chỉ hiển thị thông báo log, không hiển thị Toast
                    // Không làm gì thêm, đợi thông báo hoàn thành từ service
                }
            }
        }
    }

    // Fallback timeout để tránh UI bị treo vô hạn
    LaunchedEffect(isSending) {
        if (isSending) {
            // Thêm timeout ngắn hơn (5 phút) để kiểm tra trạng thái
            delay(5 * 60 * 1000L) // 5 phút timeout
            
            // Nếu vẫn đang gửi, kiểm tra service có còn chạy không
            if (isSending) {
                if (!isServiceRunning(context, SmsService::class.java)) {
                    // Service đã dừng nhưng UI vẫn hiển thị đang gửi
                    Log.w("MainActivity", "⏰ Service stopped but UI still showing sending state after 5 minutes")
                    isSending = false
                    Toast.makeText(
                        context, 
                        "⚠️ Dịch vụ gửi SMS đã dừng nhưng không nhận được thông báo hoàn thành", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Service vẫn đang chạy, đợi thêm 5 phút nữa
                    delay(5 * 60 * 1000L) // Thêm 5 phút nữa
                    
                    // Nếu sau 10 phút vẫn đang gửi, buộc dừng
                    if (isSending) {
                        Log.w("MainActivity", "⏰ UI timeout fallback triggered after 10 minutes")
                        isSending = false
                        Toast.makeText(
                            context, 
                            "⏰ Timeout: Dừng gửi SMS sau 10 phút", 
                            Toast.LENGTH_LONG
                        ).show()
                        onAction(SmsAppAction.StopService)
                    }
                }
            }
        }
    }
    
    // Listen to customer deletion
    LaunchedEffect(Unit) {
        customerDeletionFlow.collect { customerId ->
            Log.d("MainActivity", "📣 Nhận thông báo xóa khách hàng ID: $customerId")
            try {
                // Lấy danh sách khách hàng hiện tại
                val currentCustomers = customers
                
                // Kiểm tra xem khách hàng có tồn tại không
                val customerExists = currentCustomers.any { it.id == customerId }
                if (customerExists) {
                    Log.d("MainActivity", "🗑️ Xóa khách hàng ID: $customerId khỏi UI")
                    // Cập nhật danh sách khách hàng
                    customers = currentCustomers.filter { it.id != customerId }
                    // Lưu danh sách mới
                    smsRepository.saveCustomers(customers)
                } else {
                    Log.d("MainActivity", "⚠️ Khách hàng ID: $customerId không tồn tại trong UI")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Lỗi khi xóa khách hàng từ UI: ${e.message ?: "Lỗi không xác định"}", e)
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
                    listOf("Tất cả", "Viettel", "Mobifone", "Vinaphone").forEach { carrier ->
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
                        progress = if (totalToSend > 0) {
                            sendingProgress.toFloat() / totalToSend.toFloat()
                        } else {
                            0f
                        },
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
                        
                        // Hiển thị thời gian còn lại
                        val remainingText = if (countdownTime > 0) {
                            // Nếu có đếm ngược giữa các tin nhắn, hiển thị thời gian đó
                            "Còn lại: ${countdownTime}s"
                        } else {
                            // Nếu không có đếm ngược, hiển thị thời gian dự kiến hoàn thành
                            val currentSettings = smsRepository.getAppSettings()
                            val interval = currentSettings?.intervalBetweenSmsSeconds ?: 30
                            val remainingCustomers = totalToSend - sendingProgress
                            val totalRemainingSeconds = remainingCustomers * interval
                            val remainingMinutes = totalRemainingSeconds / 60
                            val remainingSeconds = totalRemainingSeconds % 60
                            
                            if (remainingCustomers > 0)
                                "Còn lại: ${remainingMinutes}m:${remainingSeconds.toString().padStart(2, '0')}s"
                            else
                                "Đã hoàn thành!"
                        }
                        
                        Text(
                            text = remainingText,
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Thêm dòng hiển thị thời gian hoàn thành dự kiến
                    if (sendingProgress > 0 && totalToSend > sendingProgress) {
                        // Tính tổng thời gian còn lại cho tất cả tin nhắn
                        val currentSettings = smsRepository.getAppSettings()
                        val interval = currentSettings?.intervalBetweenSmsSeconds ?: 30
                        val remainingCustomers = totalToSend - sendingProgress
                        val totalRemainingSeconds = remainingCustomers * interval
                        val totalRemainingMinutes = totalRemainingSeconds / 60
                        val totalRemainingSecondsDisplay = totalRemainingSeconds % 60
                        
                        // Hiển thị tổng thời gian còn lại
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Tổng thời gian còn lại: ${totalRemainingMinutes}m:${totalRemainingSecondsDisplay.toString().padStart(2, '0')}s",
                                fontSize = 12.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Tính thời gian hoàn thành theo giờ địa phương
                        val calendar = java.util.Calendar.getInstance()
                        calendar.add(java.util.Calendar.SECOND, totalRemainingSeconds.toInt())
                        val completionTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(calendar.time)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Dự kiến hoàn thành lúc: $completionTime",
                                fontSize = 12.sp,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Hiển thị thông tin về thời gian giữa các lần gửi
                    val currentSettings = smsRepository.getAppSettings()
                    val interval = currentSettings?.intervalBetweenSmsSeconds ?: 25
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⏱️ Thời gian chờ giữa các tin nhắn: ${interval}s",
                            fontSize = 12.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
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
                Log.d("MainActivity", "🔄 Template selected: $templateId")
                pendingTemplateId = templateId
                Log.d("MainActivity", "✅ pendingTemplateId set to: $pendingTemplateId")
                showTemplateSelectionDialog = false
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
                        
                        // Lưu lại templateId trước khi xóa pendingTemplateId
                        val templateIdToSend = pendingTemplateId
                        Log.d("MainActivity", "📋 Template ID to send: $templateIdToSend")
                        
                        // Tạo backup session ngay khi người dùng xác nhận gửi
                        if (templateIdToSend != null) {
                            val selectedCustomers = customers.filter { it.isSelected }
                            Log.d("MainActivity", "💾 Tạo backup session với ${selectedCustomers.size} khách hàng")
                            
                            // Đảm bảo lưu tất cả khách hàng vào session backup trước khi bắt đầu gửi
                            sessionBackup.saveActiveSession(templateIdToSend, selectedCustomers)
                            
                            // Thêm một nhật ký để xác nhận đã lưu
                            Log.d("MainActivity", "✅ Đã lưu backup session thành công với ${selectedCustomers.size} khách hàng")
                            
                            // Hiển thị thông báo đã lưu backup
                            Toast.makeText(
                                context,
                                "Đã lưu ${selectedCustomers.size} khách hàng vào backup",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        
                        // Bắt đầu gửi SMS ngay lập tức, không cần đếm ngược nữa
                        Toast.makeText(
                            context,
                            "Bắt đầu gửi $selectedCount tin nhắn ($simName: $smsCount/40 tin hôm nay)",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Kiểm tra null trước khi sử dụng - dùng templateIdToSend đã lưu trước đó
                        if (templateIdToSend != null) {
                            Log.d("MainActivity", "🚀 Starting service with template ID: $templateIdToSend")
                            onAction(SmsAppAction.StartService(templateIdToSend))
                        } else {
                            Log.e("MainActivity", "Lỗi: templateIdToSend là null")
                            Toast.makeText(
                                context,
                                "Lỗi: Không thể xác định template",
                                Toast.LENGTH_SHORT
                            ).show()
                            isSending = false
                        }
                        
                        // Chỉ xóa pendingTemplateId sau khi đã lưu giá trị
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
                    text = "Quản lý phiên gửi SMS",
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
                                            
                                            // Tạo danh sách mới bao gồm khách hàng hiện tại và khách hàng từ backup
                                            val updatedCustomers = currentCustomers.toMutableList()
                                            
                                            // Thêm khách hàng từ backup nếu chưa tồn tại trong danh sách hiện tại
                                            remainingCustomers.forEach { backupCustomer ->
                                                val exists = currentCustomers.any { it.id == backupCustomer.id }
                                                if (!exists) {
                                                    // Thêm khách hàng mới vào danh sách
                                                    updatedCustomers.add(backupCustomer)
                                                    Log.d("MainActivity", "Thêm khách hàng mới từ backup: ${backupCustomer.name}")
                                                }
                                            }
                                            
                                            // Đánh dấu tất cả khách hàng từ backup là selected
                                            val finalCustomers = updatedCustomers.map { customer ->
                                                val shouldSelect = remainingCustomers.any { it.id == customer.id }
                                                customer.copy(isSelected = if (shouldSelect) true else customer.isSelected)
                                            }
                                            
                                            // Cập nhật danh sách khách hàng
                                            customers = finalCustomers
                                            smsRepository.saveCustomers(customers)
                                            
                                            showBackupDialog = false
                                            Toast.makeText(
                                                context,
                                                "Đã khôi phục ${remainingCustomers.size} khách hàng từ backup",
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
                            text = "Không có phiên làm việc nào đang chạy",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    if (sessionHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 Lịch sử phiên gửi SMS:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF424242)
                            )
                            
                            Text(
                                text = "(${sessionHistory.size} phiên)",
                                fontSize = 10.sp,
                                color = Color(0xFF757575)
                            )
                        }
                        
                        // Sử dụng LazyColumn với chiều cao linh hoạt hơn
                        LazyColumn(
                            modifier = Modifier
                                .height(250.dp)
                                .padding(top = 8.dp)
                        ) {
                            items(sessionHistory) { session ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            // Hiển thị dialog xác nhận khôi phục từ phiên lịch sử
                                            val remainingCount = session.remainingCustomers.size
                                            val statusText = when (session.status) {
                                                "Hoàn thành" -> "hoàn thành"
                                                "Thất bại" -> "thất bại"
                                                else -> "đang xử lý"
                                            }
                                            
                                            if (remainingCount > 0 || session.status == "Thất bại") {
                                                val message = if (session.status == "Thất bại") {
                                                    "Khôi phục ${session.remainingCustomers.size + 1} khách hàng từ phiên $statusText?"
                                                } else {
                                                    "Khôi phục ${session.remainingCustomers.size} khách hàng từ phiên $statusText?"
                                                }
                                                
                                                // Hiển thị dialog xác nhận
                                                val confirmDialog = android.app.AlertDialog.Builder(context)
                                                    .setTitle("Khôi phục phiên")
                                                    .setMessage(message)
                                                    .setPositiveButton("Khôi phục") { _, _ ->
                                                        // Khôi phục danh sách khách hàng từ phiên lịch sử
                                                        val customersToRestore = sessionBackup.restoreCustomersFromSession(session.sessionId)
                                                        if (customersToRestore.isNotEmpty()) {
                                                            val currentCustomers = smsRepository.getCustomers()
                                                            
                                                            // Tạo danh sách mới bao gồm khách hàng hiện tại và khách hàng từ backup
                                                            val updatedCustomers = currentCustomers.toMutableList()
                                                            
                                                            // Thêm khách hàng từ backup nếu chưa tồn tại trong danh sách hiện tại
                                                            var newCustomersCount = 0
                                                            customersToRestore.forEach { backupCustomer ->
                                                                val exists = currentCustomers.any { it.id == backupCustomer.id }
                                                                if (!exists) {
                                                                    // Thêm khách hàng mới vào danh sách
                                                                    updatedCustomers.add(backupCustomer)
                                                                    newCustomersCount++
                                                                    Log.d("MainActivity", "Thêm khách hàng mới từ history: ${backupCustomer.name}")
                                                                }
                                                            }
                                                            
                                                            // Đánh dấu tất cả khách hàng từ backup là selected
                                                            val finalCustomers = updatedCustomers.map { customer ->
                                                                val shouldSelect = customersToRestore.any { it.id == customer.id }
                                                                customer.copy(isSelected = if (shouldSelect) true else customer.isSelected)
                                                            }
                                                            
                                                            // Cập nhật danh sách khách hàng
                                                            customers = finalCustomers
                                                            smsRepository.saveCustomers(customers)
                                                            
                                                            val message = if (newCustomersCount > 0) {
                                                                "Đã khôi phục ${customersToRestore.size} khách hàng (${newCustomersCount} khách hàng mới)"
                                                            } else {
                                                                "Đã khôi phục ${customersToRestore.size} khách hàng từ phiên lịch sử"
                                                            }
                                                            
                                                            Toast.makeText(
                                                                context,
                                                                message,
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            
                                                            // Đóng dialog backup sau khi khôi phục thành công
                                                            showBackupDialog = false
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Không có khách hàng nào để khôi phục",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                    .setNegativeButton("Hủy", null)
                                                    .create()
                                                
                                                confirmDialog.show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Phiên này không có khách hàng nào để khôi phục",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (session.status) {
                                            "Hoàn thành" -> Color(0xFFE8F5E9) // Xanh nhạt
                                            "Thất bại" -> Color(0xFFFFEBEE) // Đỏ nhạt
                                            else -> Color(0xFFF5F5F5) // Xám nhạt
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Text(
                                            text = sessionBackup.getSessionSummary(session),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF616161)
                                        )
                                        
                                        if (session.status == "Thất bại" && session.failedReason.isNotEmpty()) {
                                            Text(
                                                text = "Lý do: ${session.failedReason}",
                                                fontSize = 9.sp,
                                                color = Color.Red
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Template: ${session.templateId}",
                                                fontSize = 9.sp,
                                                color = Color(0xFF757575)
                                            )
                                            
                                            val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                                            val startDate = dateFormat.format(Date(session.startTime))
                                            Text(
                                                text = startDate,
                                                fontSize = 9.sp,
                                                color = Color(0xFF757575)
                                            )
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (session.remainingCustomers.isNotEmpty() || session.status == "Thất bại") {
                                                val count = if (session.status == "Thất bại" && session.failedCustomerId.isNotEmpty()) 
                                                    session.remainingCustomers.size + 1 
                                                else 
                                                    session.remainingCustomers.size
                                                    
                                                Text(
                                                    text = "👆 Nhấn để khôi phục $count khách hàng",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1976D2),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                            
                                            // Nút xóa phiên làm việc
                                            IconButton(
                                                onClick = {
                                                    // Hiển thị dialog xác nhận xóa phiên
                                                    val confirmDialog = android.app.AlertDialog.Builder(context)
                                                        .setTitle("Xóa phiên")
                                                        .setMessage("Bạn có chắc muốn xóa phiên này khỏi lịch sử?")
                                                        .setPositiveButton("Xóa") { _, _ ->
                                                            // Xóa phiên làm việc khỏi lịch sử
                                                            val success = sessionBackup.deleteSessionFromHistory(session.sessionId)
                                                            if (success) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Đã xóa phiên khỏi lịch sử",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                
                                                                // Cập nhật lại UI
                                                                showBackupDialog = false
                                                            } else {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Không thể xóa phiên",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                        .setNegativeButton("Hủy", null)
                                                        .create()
                                                    
                                                    confirmDialog.show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Xóa phiên",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chưa có lịch sử phiên gửi SMS nào",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBackupDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Đóng", color = Color.White)
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

// Helper function to check if a service is running
fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}