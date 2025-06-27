package com.example.sms_app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.example.sms_app.R
import com.example.sms_app.data.Customer
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.data.AppSettings
import com.example.sms_app.data.SessionBackup
import com.example.sms_app.data.SmsSession
import kotlinx.coroutines.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.random.Random
import com.example.sms_app.MainActivity
import com.example.sms_app.data.SmsTemplate
import com.example.sms_app.utils.SmsUtils
import android.os.Handler
import android.os.Looper
import android.app.Activity

class SmsService : Service() {
    private var serviceJob: Job? = null
    private lateinit var smsRepository: SmsRepository
    private lateinit var sessionBackup: SessionBackup
    private var selectedTemplateId: Int = 1
    private var shouldStop = false
    private lateinit var smsResultReceiver: BroadcastReceiver
    private lateinit var smsDeliveryReceiver: BroadcastReceiver
    private val pendingSmsResults = mutableMapOf<String, kotlinx.coroutines.CancellableContinuation<Boolean>>()
    private val multipartMessageTracker = mutableMapOf<String, MutableSet<String>>() // Track parts of multipart messages
    private val pendingSmsDeliveries = mutableMapOf<String, Customer>()
    private var isRunning = false
    private var totalSent = 0
    private var totalToSend = 0
    private var currentTemplateId = 0
    private var intervalSeconds = 0
    private var maxRetryAttempts = 0
    private var retryDelaySeconds = 0
    private var isSendingMessages = false
    private var currentProgress = 0
    private var totalMessageCount = 0
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "SmsService"
    
    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "SmsServiceChannel"
        const val NOTIFICATION_CHANNEL_ID = "SmsServiceChannel"
        
        const val ACTION_PROGRESS_UPDATE = "com.example.sms_app.ACTION_PROGRESS_UPDATE"
        const val ACTION_SMS_COMPLETED = "com.example.sms_app.ACTION_SMS_COMPLETED"
        const val ACTION_CUSTOMER_DELETED = "com.example.sms_app.ACTION_CUSTOMER_DELETED"
        
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_CUSTOMER_ID = "customer_id"
        const val EXTRA_TEMPLATE_ID = "template_id"
        const val EXTRA_INTERVAL_SECONDS = "interval_seconds"
        const val EXTRA_MAX_RETRY = "max_retry"
        const val EXTRA_RETRY_DELAY = "retry_delay"
        
        // SMS Delivery constants
        const val SMS_SENT_ACTION = "com.example.sms_app.SMS_SENT"
        const val SMS_DELIVERED_ACTION = "com.example.sms_app.SMS_DELIVERED"
        const val SMS_TIMEOUT_MS = 30000L // 30 seconds timeout
        
        // Danh sách emoji ngẫu nhiên để thêm vào tin nhắn
        private val RANDOM_EMOJIS = listOf(
            "👍", "👋", "😊", "🙂", "👌", "✅", "🎉", "✨", "💯", "⭐",
            "🌟", "💫", "🌈", "🔆", "📲", "✔️", "💪", "🤝", "👏", "🙌"
        )
        
        // Danh sách ký tự đặc biệt để thêm vào tin nhắn
        private val RANDOM_CHARS = listOf(
            " ", "  ", " · ", ".", "..", "…", " ", " ", " ", "  ",
            " ", " ", "", " ", " ", " ", " ", " ", " ", " "
        )
        
        // Danh sách từ ngẫu nhiên để thêm vào tin nhắn
        private val RANDOM_WORDS = listOf(
            "Xin", "Cảm ơn", "Chúc", "Thân", "Trân trọng", "Mến", "Hi", "Vui", "Tốt", "Thân mến"
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SmsService onCreate")
        isSendingMessages = false
        currentProgress = 0
        totalMessageCount = 0
        
        smsRepository = SmsRepository(applicationContext)
        sessionBackup = SessionBackup(this)
        
        // Tạo notification channel cho Android 8.0 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "SMS Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for SMS sending service notifications"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        // Initialize SmsUtils for standard SMS functionality
        SmsUtils.initialize()
        
        // Skip dynamic code loading for now as it's causing issues
        Log.d(TAG, "Using standard SMS APIs instead of dynamic loading")
        
        try {
            setupSmsResultReceiver()
            setupSmsDeliveryReceiver()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup SMS result receiver", e)
        }
        
        // Kiểm tra các cài đặt thiết bị
        val deviceStatus = checkDeviceSettings()
        Log.d(TAG, "Device status: $deviceStatus")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 SmsService onStartCommand called, intent: ${intent?.action}")
        
        try {
            if (intent != null) {
                try {
                    // Kiểm tra xem có phải là action dừng dịch vụ không
                    if (intent.action == "STOP_SMS_SERVICE") {
                        Log.d(TAG, "Received stop action from notification")
                        isRunning = false
                        sendCompletionBroadcast("Dịch vụ gửi SMS đã bị dừng bởi người dùng")
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    
                    currentTemplateId = intent.getIntExtra(EXTRA_TEMPLATE_ID, 1)
                    intervalSeconds = intent.getIntExtra(EXTRA_INTERVAL_SECONDS, 25)
                    maxRetryAttempts = intent.getIntExtra(EXTRA_MAX_RETRY, 3)
                    retryDelaySeconds = intent.getIntExtra(EXTRA_RETRY_DELAY, 10)
                    
                    Log.d(TAG, "📋 Loaded settings: templateId=$currentTemplateId, interval=$intervalSeconds, maxRetry=$maxRetryAttempts, retryDelay=$retryDelaySeconds")
                    
                    // Khởi tạo notification với mức độ ưu tiên cao hơn
                    try {
                        val notification = createNotification("Đang chuẩn bị gửi SMS...")
                        Log.d(TAG, "📲 Starting foreground service with notification")
                        startForeground(NOTIFICATION_ID, notification)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error creating notification", e)
                    }
                    
                    // Thêm một delay nhỏ trước khi bắt đầu để đảm bảo foreground service đã được thiết lập
                    serviceScope.launch {
                        try {
                            delay(500) // Đợi 500ms
                            Log.d(TAG, "⏱️ Starting SMS sending after delay")
                            startSendingSms()
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error in delayed start", e)
                            sendCompletionBroadcast("Lỗi khởi động dịch vụ: ${e.message}")
                            stopSelf()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing intent", e)
                    sendCompletionBroadcast("Lỗi xử lý intent: ${e.message}")
                    stopSelf()
                    return START_NOT_STICKY
                }
            } else {
                Log.w(TAG, "⚠️ onStartCommand called with null intent")
                stopSelf()
                return START_NOT_STICKY
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Critical error in onStartCommand", e)
            try {
                sendCompletionBroadcast("Lỗi nghiêm trọng: ${e.message}")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to send completion broadcast", e2)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Sử dụng START_REDELIVER_INTENT thay vì START_NOT_STICKY để hệ thống khởi động lại service nếu bị kill
        return START_REDELIVER_INTENT
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called, ensuring completion broadcast is sent")
        
        // Đảm bảo gửi thông báo hoàn thành khi service bị dừng
        try {
            // Luôn gửi thông báo hoàn thành khi service bị dừng
            val message = if (totalSent > 0) {
                "🏁 Đã gửi $totalSent/${totalToSend} tin nhắn (dịch vụ đã kết thúc)"
            } else {
                "⚠️ Dịch vụ gửi SMS đã dừng"
            }
            sendCompletionBroadcast(message)
            
            // Cập nhật session backup
            sessionBackup.updateSessionTime()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending completion broadcast in onDestroy", e)
        }
        
        // Hủy các tác vụ đang chạy
        serviceJob?.cancel()
        smsResultReceiver?.let { unregisterReceiver(it) }
        smsDeliveryReceiver?.let { unregisterReceiver(it) }
        pendingSmsResults.values.forEach { it.cancel() }
        pendingSmsResults.clear()
        pendingSmsDeliveries.clear()
        multipartMessageTracker.clear()
        isRunning = false
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startSendingSms() {
        try {
            if (isRunning) return
            isRunning = true

            // Kiểm tra cài đặt thiết bị trước khi bắt đầu
            val deviceCheck = checkDeviceSettings()
            Log.i(TAG, "🔍 Device check result: $deviceCheck")

            if (deviceCheck.contains("❌")) {
                Log.e(TAG, "❌ Device check failed, stopping service")
                sendCompletionBroadcast("Lỗi thiết bị: $deviceCheck")
                stopSelf()
                return
            }

            val customers = smsRepository.getCustomers().filter { it.isSelected }
            
            // Debug logs to check what's being loaded
            val allTemplates = smsRepository.getTemplates()
            val messageTemplates = smsRepository.getMessageTemplates()
            Log.d(TAG, "📋 Loaded ${allTemplates.size} templates: ${allTemplates.map { it.id }}")
            Log.d(TAG, "📋 Loaded ${messageTemplates.size} message templates: ${messageTemplates.map { it.id }}")
            Log.d(TAG, "🔢 Current template ID: $currentTemplateId")
            
            // Check both template sources
            var template = smsRepository.getTemplates().find { it.id == currentTemplateId.toString() }
            
            // If template not found, try to get from message templates and convert
            if (template == null) {
                val messageTemplate = smsRepository.getMessageTemplates().find { it.id == currentTemplateId }
                if (messageTemplate != null) {
                    template = SmsTemplate(
                        id = messageTemplate.id.toString(),
                        name = messageTemplate.description,
                        content = messageTemplate.content
                    )
                    Log.d(TAG, "🔄 Using converted message template: ${template.name}")
                }
            }
            
            if (customers.isEmpty()) {
                Log.e(TAG, "❌ No customers selected for SMS")
                sendCompletionBroadcast("Không có khách hàng được chọn")
                stopSelf()
                return
            }
            
            if (template == null) {
                Log.e(TAG, "❌ Template ID $currentTemplateId not found")
                sendCompletionBroadcast("Không tìm thấy template ID: $currentTemplateId")
                stopSelf()
                return
            }
            
            totalToSend = customers.size
            totalSent = 0

            Log.i(TAG, "🚀 Starting SMS sending: $totalToSend messages to send")
            Log.d(TAG, "📋 Template: ${template.name} (ID: ${template.id})")
            Log.d(TAG, "🔤 Template content: ${template.content}")
            Log.d(TAG, "⚙️ Settings: interval=${intervalSeconds}s, maxRetry=$maxRetryAttempts, retryDelay=${retryDelaySeconds}s")

            // Lưu trữ phiên làm việc hiện tại
            val session = SmsSession(
                sessionId = sessionBackup.generateSessionId(),
                templateId = currentTemplateId,
                totalCustomers = customers.size,
                sentCount = 0,
                remainingCustomers = customers,
                startTime = System.currentTimeMillis(),
                lastUpdateTime = System.currentTimeMillis()
            )
            sessionBackup.saveActiveSession(session)
            
            // Cập nhật notification để tăng mức độ ưu tiên
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification("Bắt đầu gửi ${customers.size} tin nhắn..."))
            
            serviceScope.launch {
                try {
                    // Thêm timeout cho toàn bộ quá trình gửi SMS (10 phút)
                    withTimeout(10 * 60 * 1000L) {
                        sendSmsToCustomers(customers, template.content)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "⏰ SMS sending timeout after 10 minutes")
                    sendCompletionBroadcast("⏰ Timeout: Quá trình gửi SMS bị dừng sau 10 phút")
                    stopSelf()
                } catch (e: Exception) {
                    Log.e(TAG, "💥 Critical error in SMS sending", e)
                    sendCompletionBroadcast("💥 Lỗi nghiêm trọng: ${e.message}")
                    stopSelf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startSendingSms", e)
            sendCompletionBroadcast("Lỗi bắt đầu gửi SMS: ${e.message}")
            stopSelf()
        }
    }
    
    private suspend fun sendSmsToCustomers(customers: List<Customer>, templateContent: String) {
        try {
            // Đảm bảo service vẫn chạy trong foreground
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification("Đang gửi tin nhắn..."))
            
            // Đếm số lần thử lại tổng cộng
            var totalRetries = 0
            val maxTotalRetries = maxRetryAttempts * 3 // Giới hạn số lần thử lại tổng cộng
            
            for (customer in customers) {
                if (!isRunning) {
                    Log.d(TAG, "Service stopped, breaking SMS sending loop")
                    break
                }
                
                try {
                    // Cập nhật notification để giữ service trong foreground
                    notificationManager.notify(
                        NOTIFICATION_ID, 
                        createNotification("Đang gửi tin nhắn ${totalSent + 1}/${totalToSend}")
                    )
                    
                    val message = formatMessage(templateContent, customer)
                    Log.d(TAG, "🚀 Attempting to send SMS to ${customer.name} (${customer.phoneNumber})")
                    Log.d(TAG, "📝 Message content: ${message.take(50)}${if (message.length > 50) "..." else ""}")

                    // Sử dụng phương thức gửi SMS với delivery report để có thể theo dõi trạng thái
                    val selectedSim = smsRepository.getSelectedSim()
                    var success = false
                    var retryCount = 0
                    
                    while (!success && retryCount < maxRetryAttempts && totalRetries < maxTotalRetries && isRunning) {
                        try {
                            if (retryCount > 0) {
                                Log.d(TAG, "Retry attempt $retryCount for ${customer.phoneNumber}")
                                delay(retryDelaySeconds * 1000L)
                            }
                            
                            success = sendSmsWithDeliveryReport(customer.phoneNumber, message, selectedSim, customer)
                            
                            if (!success) {
                                retryCount++
                                totalRetries++
                                Log.d(TAG, "SMS sending failed, retry $retryCount/$maxRetryAttempts")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Exception during SMS sending to ${customer.phoneNumber}: ${e.message}", e)
                            retryCount++
                            totalRetries++
                            
                            // Đợi trước khi thử lại
                            delay(retryDelaySeconds * 1000L)
                        }
                    }
                    
                    if (success) {
                        totalSent++
                        // Không xóa khách hàng ngay, mà chờ xác nhận từ delivery report
                        // Chỉ đánh dấu là đã gửi thành công
                        
                        // Gửi broadcast cập nhật tiến độ
                        sendProgressBroadcast(
                            totalSent,
                            totalToSend,
                            "✅ Đã gửi SMS cho ${customer.name} (${customer.phoneNumber}), đang chờ xác nhận"
                        )

                        // Tăng số lượng SMS đã gửi trong ngày
                        smsRepository.incrementSmsCount(selectedSim)

                        Log.d(TAG, "✅ SMS sent successfully to ${customer.name} (${customer.phoneNumber})")

                        // Đợi một khoảng thời gian ngẫu nhiên trước khi gửi tin nhắn tiếp theo
                        val randomDelay = SmsUtils.getRandomDelay(intervalSeconds)
                        Log.d(TAG, "⏳ Waiting ${randomDelay}ms before next SMS...")
                        
                        // Chia nhỏ thời gian chờ để kiểm tra trạng thái service thường xuyên hơn
                        val checkInterval = 1000L // 1 giây
                        var remainingDelay = randomDelay
                        
                        while (remainingDelay > 0 && isRunning) {
                            val delayStep = minOf(checkInterval, remainingDelay)
                            delay(delayStep)
                            remainingDelay -= delayStep
                            
                            // Cập nhật notification để giữ service trong foreground - thường xuyên hơn
                            if (remainingDelay % 1000 == 0L) { // Cập nhật mỗi 1 giây
                                val remainingSecs = remainingDelay / 1000
                                val nextCustomerIndex = totalSent
                                
                                // Tính tổng thời gian còn lại cho tất cả tin nhắn
                                val remainingCustomers = totalToSend - totalSent
                                val totalRemainingSeconds = remainingSecs + (remainingCustomers * intervalSeconds)
                                val totalRemainingMinutes = totalRemainingSeconds / 60
                                val totalRemainingSecsDisplay = totalRemainingSeconds % 60
                                
                                // Hiển thị thông tin chi tiết hơn trong notification
                                val notificationMessage = if (nextCustomerIndex < totalToSend) {
                                    val nextCustomer = customers[nextCustomerIndex]
                                    "Đã gửi $totalSent/$totalToSend tin nhắn. Còn lại: ${remainingSecs}s trước khi gửi cho ${nextCustomer.name}"
                                } else {
                                    "Đã gửi $totalSent/$totalToSend tin nhắn. Còn lại: ${remainingSecs}s..."
                                }
                                
                                notificationManager.notify(
                                    NOTIFICATION_ID, 
                                    createNotification(notificationMessage)
                                )
                                
                                // Cập nhật UI thông qua broadcast với tổng thời gian còn lại
                                val progressMessage = if (nextCustomerIndex < totalToSend) {
                                    val nextCustomer = customers[nextCustomerIndex]
                                    if (remainingCustomers > 0) {
                                        "Còn lại: ${remainingSecs}s trước khi gửi cho ${nextCustomer.name} (Tổng: ${totalRemainingMinutes}m:${totalRemainingSecsDisplay}s)"
                                    } else {
                                        "Còn lại: ${remainingSecs}s trước khi gửi cho ${nextCustomer.name}"
                                    }
                                } else {
                                    "Còn lại: ${remainingSecs}s..."
                                }
                                
                                sendProgressBroadcast(
                                    totalSent,
                                    totalToSend,
                                    progressMessage
                                )
                            }
                        }
                    } else {
                        Log.e(TAG, "❌ Failed to send SMS to ${customer.name} (${customer.phoneNumber}) after $retryCount retries")
                        
                        // Đánh dấu session thất bại để có thể khôi phục sau này
                        sessionBackup.markSessionFailed(customer.id, "Không thể gửi SMS sau $retryCount lần thử")
                        
                        sendProgressBroadcast(
                            totalSent,
                            totalToSend,
                            "❌ Lỗi gửi ${customer.name} (${customer.phoneNumber}) sau $retryCount lần thử"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "💥 Error processing customer ${customer.name} (${customer.phoneNumber})", e)
                    
                    // Đánh dấu session thất bại để có thể khôi phục sau này
                    sessionBackup.markSessionFailed(customer.id, "Lỗi xử lý: ${e.message}")
                    
                    sendProgressBroadcast(
                        totalSent,
                        totalToSend,
                        "💥 Lỗi xử lý ${customer.name}: ${e.message}"
                    )
                    
                    // Đợi một khoảng thời gian trước khi xử lý khách hàng tiếp theo
                    delay(5000)
                }
                
                // Cập nhật thời gian hoạt động của session
                sessionBackup.updateSessionTime()
            }

            // Hoàn thành gửi SMS
            Log.i(TAG, "🏁 SMS sending completed: $totalSent/$totalToSend messages sent")
            sendCompletionBroadcast("🏁 Đã hoàn thành gửi $totalSent/${totalToSend} tin nhắn")
            sessionBackup.completeSession()
            
            // Không dừng service ngay lập tức để cho phép MainActivity nhận được thông báo hoàn thành
            // và tự quyết định khi nào tắt UI loading
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Dừng service sau khi đã gửi thông báo hoàn thành")
                stopSelf()
            }, 1000) // Đợi 1 giây để đảm bảo thông báo được gửi đi
        } catch (e: Exception) {
            Log.e(TAG, "💥 Critical error in sendSmsToCustomers", e)
            sendCompletionBroadcast("💥 Lỗi nghiêm trọng: ${e.message}")
            sessionBackup.updateSessionTime() // Cập nhật thời gian để có thể khôi phục
            stopSelf()
        }
    }
    
    private suspend fun sendSmsWithDeliveryReport(
        phoneNumber: String,
        message: String,
        selectedSim: Int,
        customer: Customer
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val requestId = "SMS_${System.currentTimeMillis()}_${phoneNumber.hashCode()}"
            
            // Store continuation for callback
            pendingSmsResults[requestId] = continuation
            
            // Store customer for delivery report
            pendingSmsDeliveries[requestId] = customer
            
            // Create PendingIntent for sent status
            val sentIntent = Intent(SMS_SENT_ACTION).apply {
                putExtra("REQUEST_ID", requestId)
                putExtra("CUSTOMER_ID", customer.id)
            }
            
            // Create PendingIntent for delivery status
            val deliveredIntent = Intent(SMS_DELIVERED_ACTION).apply {
                putExtra("REQUEST_ID", requestId)
                putExtra("CUSTOMER_ID", customer.id)
            }
            
            // Use compatible PendingIntent flags based on Android version
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val sentPendingIntent = PendingIntent.getBroadcast(
                this@SmsService, 
                requestId.hashCode(), 
                sentIntent, 
                pendingIntentFlags
            )
            
            val deliveredPendingIntent = PendingIntent.getBroadcast(
                this@SmsService,
                (requestId + "_DELIVERED").hashCode(),
                deliveredIntent,
                pendingIntentFlags
            )
            
            // Get appropriate SmsManager instance
            val smsManager = try {
                if (selectedSim != -1 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    SmsManager.getSmsManagerForSubscriptionId(selectedSim)
                } else {
                    SmsManager.getDefault()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting SmsManager for SIM $selectedSim, using default", e)
                SmsManager.getDefault()
            }
            
            // Check if message is longer than single SMS limit
            if (message.length > 160) {
                Log.d(TAG, "📝 Message longer than 160 chars, using divideMessage")
                val parts = smsManager.divideMessage(message)
                val sentIntents = ArrayList<PendingIntent>()
                
                // Create separate pending intents for each part
                for (i in parts.indices) {
                    val partRequestId = "${requestId}_PART_$i"
                    val partIntent = Intent(SMS_SENT_ACTION).apply {
                        putExtra("REQUEST_ID", requestId) // Use same request ID for all parts
                        putExtra("PART_ID", partRequestId) // Also include the part ID for tracking
                        putExtra("TOTAL_PARTS", parts.size)
                        putExtra("PART_INDEX", i)
                    }
                    val partPendingIntent = PendingIntent.getBroadcast(
                        this@SmsService,
                        partRequestId.hashCode(),
                        partIntent,
                        pendingIntentFlags
                    )
                    sentIntents.add(partPendingIntent)
                }
                
                // Initialize tracking for this multipart message
                multipartMessageTracker[requestId] = mutableSetOf()
                
                try {
                    // Send multi-part message
                    // Tạo danh sách deliveredIntents
                    val deliveredIntents = ArrayList<PendingIntent>()
                    for (i in parts.indices) {
                        val deliveredPartRequestId = "${requestId}_DELIVERED_PART_$i"
                        val deliveredPartIntent = Intent(SMS_DELIVERED_ACTION).apply {
                            putExtra("REQUEST_ID", requestId)
                            putExtra("CUSTOMER_ID", customer.id)
                            putExtra("PART_ID", deliveredPartRequestId)
                            putExtra("TOTAL_PARTS", parts.size)
                            putExtra("PART_INDEX", i)
                        }
                        val deliveredPartPendingIntent = PendingIntent.getBroadcast(
                            this@SmsService,
                            deliveredPartRequestId.hashCode(),
                            deliveredPartIntent,
                            pendingIntentFlags
                        )
                        deliveredIntents.add(deliveredPartPendingIntent)
                    }
                    
                    smsManager.sendMultipartTextMessage(
                        phoneNumber,
                        null,
                        parts,
                        sentIntents,
                        deliveredIntents
                    )
                    Log.d(TAG, "📤 Sending multi-part SMS (${parts.size} parts) to $phoneNumber (requestId: $requestId)")
                } catch (e: Exception) {
                    // If sendMultipartTextMessage fails, try normal send as fallback
                    Log.e(TAG, "❌ Error with sendMultipartTextMessage, falling back to sendTextMessage", e)
                    smsManager.sendTextMessage(
                        phoneNumber,
                        null,
                        message.take(160), // Take only first 160 chars in fallback mode
                        sentPendingIntent,
                        deliveredPendingIntent
                    )
                }
            } else {
                // Send normal SMS
                try {
                    smsManager.sendTextMessage(
                        phoneNumber,
                        null,
                        message,
                        sentPendingIntent,
                        deliveredPendingIntent
                    )
                    Log.d(TAG, "📤 Sending SMS to $phoneNumber (requestId: $requestId)")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Exception in sendTextMessage: ${e.message}", e)
                    throw e // Re-throw to be caught by outer catch block
                }
            }
            
            // Set up timeout
            continuation.invokeOnCancellation {
                pendingSmsResults.remove(requestId)
                Log.w(TAG, "⏰ SMS timeout or cancelled (requestId: $requestId)")
            }
            
            // Timeout after 30 seconds
            CoroutineScope(Dispatchers.IO).launch {
                delay(SMS_TIMEOUT_MS)
                val timeoutContinuation = pendingSmsResults.remove(requestId)
                if (timeoutContinuation != null && timeoutContinuation == continuation) {
                    Log.e(TAG, "⏰ SMS timeout after ${SMS_TIMEOUT_MS}ms (requestId: $requestId)")
                    continuation.resume(false)
                }
            }
            
        } catch (e: Exception) {
            pendingSmsResults.remove(continuation.hashCode().toString())
            Log.e(TAG, "❌ Exception sending SMS: ${e.message}", e)
            continuation.resume(false)
        }
    }
    
    private fun formatMessage(template: String, customer: Customer): String {
        try {
            Log.d(TAG, "⚙️ Formatting template: ${template.take(50)}${if (template.length > 50) "..." else ""}")
            Log.d(TAG, "📋 Customer data: ${customer.name}, ${customer.phoneNumber}, ${customer.idNumber}")
            
            // Format theo các cột cấu hình: xxx, yyy, sdt, ttt, zzz, www, uuu, vvv, rrr
            var message = template
            
            // Cột 1: xxx - Tên khách hàng
            message = message.replace("{xxx}", customer.name)
                .replace("{XXX}", customer.name)
                .replace(" xxx", " " + customer.name) // Thêm space trước để tránh thay thế từ trong từ khác
                .replace(" XXX", " " + customer.name)
                .replace("xxx ", customer.name + " ")
                .replace("XXX ", customer.name + " ")
                .replace("\nxxx", "\n" + customer.name)
                .replace("\nXXX", "\n" + customer.name)
                // Trường hợp đặc biệt ở đầu hoặc cuối text
                .replace("xxx\n", customer.name + "\n")
                .replace("XXX\n", customer.name + "\n")
                
            // Kiểm tra trường hợp "xxx" ở đầu hoặc cuối chuỗi
            if (message.startsWith("xxx")) {
                message = customer.name + message.substring(3)
            } 
            if (message.startsWith("XXX")) {
                message = customer.name + message.substring(3)
            }
            
            if (message.endsWith("xxx")) {
                message = message.substring(0, message.length - 3) + customer.name
            }
            if (message.endsWith("XXX")) {
                message = message.substring(0, message.length - 3) + customer.name
            }
            
            // Xử lý các trường hợp đặc biệt
            val specialCases = arrayOf(
                "Hello xxx", "Hello XXX", "Xin chào xxx", "Xin chào XXX",
                "Hi xxx", "Hi XXX", "Chào xxx", "Chào XXX"
            )
            
            for (case in specialCases) {
                if (message.contains(case)) {
                    val prefix = case.substring(0, case.length - 3)
                    message = message.replace(case, "$prefix${customer.name}")
                }
            }
            
            // Cột 2: yyy - IdNumber (CMND)
            message = message.replace("{yyy}", customer.idNumber)
                .replace("{YYY}", customer.idNumber)
                .replace(" yyy", " " + customer.idNumber)
                .replace(" YYY", " " + customer.idNumber)
                .replace("yyy ", customer.idNumber + " ")
                .replace("YYY ", customer.idNumber + " ")
                .replace("\nyyy", "\n" + customer.idNumber)
                .replace("\nYYY", "\n" + customer.idNumber)
                // Trường hợp đặc biệt ở đầu hoặc cuối text
                .replace("yyy\n", customer.idNumber + "\n")
                .replace("YYY\n", customer.idNumber + "\n")
            
            // Kiểm tra trường hợp "yyy" ở đầu hoặc cuối chuỗi
            if (message.startsWith("yyy")) {
                message = customer.idNumber + message.substring(3)
            }
            if (message.startsWith("YYY")) {
                message = customer.idNumber + message.substring(3)
            }
            
            if (message.endsWith("yyy")) {
                message = message.substring(0, message.length - 3) + customer.idNumber
            }
            if (message.endsWith("YYY")) {
                message = message.substring(0, message.length - 3) + customer.idNumber
            }
            
            // Cột 3: sdt - Số điện thoại
            message = message.replace("{sdt}", customer.phoneNumber)
                .replace("{SDT}", customer.phoneNumber)
                .replace("{SĐT}", customer.phoneNumber)
                .replace(" sdt", " " + customer.phoneNumber)
                .replace(" SDT", " " + customer.phoneNumber)
                .replace(" SĐT", " " + customer.phoneNumber)
                .replace("sdt ", customer.phoneNumber + " ")
                .replace("SDT ", customer.phoneNumber + " ")
                .replace("SĐT ", customer.phoneNumber + " ")
            
            // Cột 4: ttt - Địa chỉ
            message = message.replace("{ttt}", customer.address)
                .replace("{TTT}", customer.address)
                .replace(" ttt", " " + customer.address)
                .replace(" TTT", " " + customer.address)
                .replace("ttt ", customer.address + " ")
                .replace("TTT ", customer.address + " ")
            
            // Cột 5-9: zzz, www, uuu, vvv, rrr - Các trường tùy chọn 1-5
            message = message.replace("{zzz}", customer.option1 ?: "")
                .replace("{ZZZ}", customer.option1 ?: "")
                .replace(" zzz", " " + (customer.option1 ?: ""))
                .replace(" ZZZ", " " + (customer.option1 ?: ""))
                .replace("zzz ", (customer.option1 ?: "") + " ")
                .replace("ZZZ ", (customer.option1 ?: "") + " ")
                
            message = message.replace("{www}", customer.option2 ?: "")
                .replace("{WWW}", customer.option2 ?: "")
                .replace(" www", " " + (customer.option2 ?: ""))
                .replace(" WWW", " " + (customer.option2 ?: ""))
                .replace("www ", (customer.option2 ?: "") + " ")
                .replace("WWW ", (customer.option2 ?: "") + " ")
                
            message = message.replace("{uuu}", customer.option3 ?: "")
                .replace("{UUU}", customer.option3 ?: "")
                .replace(" uuu", " " + (customer.option3 ?: ""))
                .replace(" UUU", " " + (customer.option3 ?: ""))
                .replace("uuu ", (customer.option3 ?: "") + " ")
                .replace("UUU ", (customer.option3 ?: "") + " ")
                
            message = message.replace("{vvv}", customer.option4 ?: "")
                .replace("{VVV}", customer.option4 ?: "")
                .replace(" vvv", " " + (customer.option4 ?: ""))
                .replace(" VVV", " " + (customer.option4 ?: ""))
                .replace("vvv ", (customer.option4 ?: "") + " ")
                .replace("VVV ", (customer.option4 ?: "") + " ")
                
            message = message.replace("{rrr}", customer.option5 ?: "")
                .replace("{RRR}", customer.option5 ?: "")
                .replace(" rrr", " " + (customer.option5 ?: ""))
                .replace(" RRR", " " + (customer.option5 ?: ""))
                .replace("rrr ", (customer.option5 ?: "") + " ")
                .replace("RRR ", (customer.option5 ?: "") + " ")
            
            // Đồng thời vẫn giữ cách thay thế biến cũ để tương thích ngược
            message = message.replace("{ten}", customer.name)
                .replace("{TEN}", customer.name)
                .replace("{Tên}", customer.name)
                .replace("{tên}", customer.name)
                
            message = message.replace("{cmnd}", customer.idNumber)
                .replace("{CMND}", customer.idNumber)
                .replace("{Cmnd}", customer.idNumber)
                
            message = message.replace("{diachi}", customer.address)
                .replace("{DIACHI}", customer.address)
                .replace("{Diachi}", customer.address)
                .replace("{địa chỉ}", customer.address)
                .replace("{Địa chỉ}", customer.address)
                .replace("{ĐỊA CHỈ}", customer.address)
                
            message = message.replace("{option1}", customer.option1 ?: "")
                .replace("{OPTION1}", customer.option1 ?: "")
                
            message = message.replace("{option2}", customer.option2 ?: "")
                .replace("{OPTION2}", customer.option2 ?: "")
                
            message = message.replace("{option3}", customer.option3 ?: "")
                .replace("{OPTION3}", customer.option3 ?: "")
                
            message = message.replace("{option4}", customer.option4 ?: "")
                .replace("{OPTION4}", customer.option4 ?: "")
                
            message = message.replace("{option5}", customer.option5 ?: "")
                .replace("{OPTION5}", customer.option5 ?: "")
            
            // Thêm thời gian hiện tại nếu cần
            val currentDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            
            message = message.replace("{ngay}", currentDate)
                .replace("{NGAY}", currentDate)
                .replace("{Ngày}", currentDate)
                .replace("{ngày}", currentDate)
                .replace("{gio}", currentTime)
                .replace("{GIO}", currentTime)
                .replace("{Giờ}", currentTime)
                .replace("{giờ}", currentTime)
            
            // Thêm random emoji vào cuối tin nhắn nếu tin nhắn có đánh dấu {emoji}
            if (message.contains("{emoji}")) {
                val emojis = listOf("👋", "👍", "✅", "🎉", "👌", "😊", "🙂", "💯", "⭐", "✨")
                val randomEmoji = emojis.random()
                message = message.replace("{emoji}", randomEmoji)
            }
            
            Log.d(TAG, "✅ Formatted result: ${message.take(50)}${if (message.length > 50) "..." else ""}")
            return message
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error formatting message: ${e.message}", e)
            return template // Return original template if formatting fails
        }
    }
    
    private fun createNotification(message: String): android.app.Notification {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            pendingIntentFlags
        )
        
        // Thêm action dừng dịch vụ
        val stopIntent = Intent(this, SmsService::class.java).apply {
            action = "STOP_SMS_SERVICE"
        }
        
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            pendingIntentFlags
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dịch vụ gửi SMS")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_sms_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Mức độ ưu tiên cao nhất
            .setCategory(NotificationCompat.CATEGORY_SERVICE) // Đánh dấu là service notification
            .setOngoing(true) // Không cho phép người dùng xóa notification
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Hiển thị trên màn hình khóa
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dừng gửi", stopPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Hiển thị ngay lập tức
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SMS Service Channel"
            val descriptionText = "Channel for SMS Service"
            val importance = NotificationManager.IMPORTANCE_HIGH // Tăng mức độ ưu tiên
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // Hiển thị trên màn hình khóa
                setShowBadge(true) // Hiển thị badge trên icon
                enableLights(true) // Bật đèn thông báo
                lightColor = android.graphics.Color.BLUE
                enableVibration(true) // Bật rung
                vibrationPattern = longArrayOf(0, 1000, 500, 1000) // Mẫu rung
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun sendProgressBroadcast(progress: Int, total: Int, message: String) {
        val intent = Intent(ACTION_PROGRESS_UPDATE).apply {
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_TOTAL, total)
            putExtra(EXTRA_MESSAGE, message)
        }
        sendBroadcast(intent)
        
        // Cập nhật notification với thông tin chi tiết hơn
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Sử dụng message đã được định dạng từ bên ngoài
        notificationManager.notify(NOTIFICATION_ID, createNotification(message))
    }
    
    private fun sendCompletionBroadcast(message: String) {
        try {
            Log.d(TAG, "📣 Sending completion broadcast: $message")
            
            // Tạo intent với thông tin
            val intent = Intent(ACTION_SMS_COMPLETED).apply {
                putExtra(EXTRA_MESSAGE, message)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            
            // Gửi broadcast trên main thread để đảm bảo độ tin cậy
            Handler(Looper.getMainLooper()).post {
                try {
                    sendBroadcast(intent)
                    Log.d(TAG, "📣 Sent completion broadcast on main thread")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error sending completion broadcast on main thread", e)
                }
            }
            
            // Thêm cơ chế thử lại sau một khoảng thời gian
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    applicationContext.sendBroadcast(intent)
                    Log.d(TAG, "📣 Sent completion broadcast again after delay")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error sending delayed completion broadcast", e)
                }
            }, 500) // Thử lại sau 500ms
            
            // Thử lại lần thứ 3 với context khác
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val newIntent = Intent(ACTION_SMS_COMPLETED).apply {
                        putExtra(EXTRA_MESSAGE, message)
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    }
                    applicationContext.sendBroadcast(newIntent)
                    Log.d(TAG, "📣 Sent completion broadcast third attempt")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error sending third completion broadcast", e)
                }
            }, 1000) // Thử lại sau 1 giây
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in sendCompletionBroadcast", e)
        }
    }
    
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun setupSmsResultReceiver() {
        smsResultReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val requestId = intent.getStringExtra("REQUEST_ID") ?: return
                val partId = intent.getStringExtra("PART_ID")
                val totalParts = intent.getIntExtra("TOTAL_PARTS", 0)
                val partIndex = intent.getIntExtra("PART_INDEX", -1)
                val continuation = pendingSmsResults[requestId]
                
                when (intent.action) {
                    SMS_SENT_ACTION -> {
                        // Kiểm tra kết quả thực tế của việc gửi SMS
                        val resultCode = resultCode
                        val success = when (resultCode) {
                            Activity.RESULT_OK -> {
                                Log.d(TAG, "✅ SMS sent successfully (resultCode: RESULT_OK)")
                                true
                            }
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                                Log.e(TAG, "❌ SMS sending failed: Generic failure")
                                false
                            }
                            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                                Log.e(TAG, "❌ SMS sending failed: No service")
                                false
                            }
                            SmsManager.RESULT_ERROR_NULL_PDU -> {
                                Log.e(TAG, "❌ SMS sending failed: Null PDU")
                                false
                            }
                            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                                Log.e(TAG, "❌ SMS sending failed: Radio off")
                                false
                            }
                            else -> {
                                Log.w(TAG, "⚠️ SMS sending result unknown (resultCode: $resultCode), assuming success")
                                true // Giả sử thành công nếu không rõ kết quả
                            }
                        }
                        
                        // Log thông tin
                        if (partId != null) {
                            Log.d(TAG, "📤 SMS part ${partIndex + 1}/$totalParts sent (requestId: $requestId, success: $success)")
                        } else {
                            Log.d(TAG, "📤 SMS sent (requestId: $requestId, success: $success)")
                        }
                        
                        // Xử lý tin nhắn nhiều phần
                        if (partId != null && continuation != null) {
                            // Theo dõi phần đã hoàn thành
                            val tracker = multipartMessageTracker.getOrPut(requestId) { mutableSetOf() }
                            tracker.add(partId)
                            
                            // Nếu tất cả các phần đã hoàn thành, tiếp tục coroutine
                            if (tracker.size == totalParts) {
                                pendingSmsResults.remove(requestId)?.resume(success)
                                multipartMessageTracker.remove(requestId)
                                Log.d(TAG, "✅ All $totalParts parts of multipart SMS sent (requestId: $requestId, success: $success)")
                            }
                            return
                        }
                        
                        // SMS thông thường - xóa và tiếp tục
                        pendingSmsResults.remove(requestId)?.resume(success)
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(SMS_SENT_ACTION)
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(smsResultReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(smsResultReceiver, filter)
            }
            Log.d(TAG, "✅ SMS result receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register SMS result receiver", e)
        }
    }
    
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun setupSmsDeliveryReceiver() {
        smsDeliveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val requestId = intent.getStringExtra("REQUEST_ID") ?: return
                val customerId = intent.getStringExtra("CUSTOMER_ID") ?: return
                
                when (intent.action) {
                    SMS_DELIVERED_ACTION -> {
                        val resultCode = resultCode
                        when (resultCode) {
                            Activity.RESULT_OK -> {
                                Log.d(TAG, "✅ SMS delivered successfully (requestId: $requestId)")
                                
                                // Lấy thông tin khách hàng từ map
                                val customer = pendingSmsDeliveries.remove(requestId)
                                if (customer != null) {
                                    // Xóa khách hàng sau khi SMS đã được gửi đến người nhận
                                    deleteCustomerAfterSuccessfulSend(customer)
                                    
                                    // Gửi broadcast cập nhật tiến độ
                                    sendProgressBroadcast(
                                        totalSent,
                                        totalToSend,
                                        "✅ Đã xác nhận gửi đến ${customer.name} (${customer.phoneNumber})"
                                    )
                                } else {
                                    Log.w(TAG, "⚠️ Customer not found for requestId: $requestId")
                                }
                            }
                            Activity.RESULT_CANCELED -> {
                                Log.e(TAG, "❌ SMS delivery failed: Canceled (requestId: $requestId)")
                                
                                // Đánh dấu session thất bại
                                sessionBackup.markSessionFailed(customerId, "SMS không đến được người nhận")
                                
                                // Gửi broadcast cập nhật tiến độ
                                sendProgressBroadcast(
                                    totalSent,
                                    totalToSend,
                                    "❌ SMS không đến được người nhận (requestId: $requestId)"
                                )
                            }
                            else -> {
                                Log.w(TAG, "⚠️ SMS delivery result unknown (resultCode: $resultCode, requestId: $requestId)")
                                
                                // Nếu không rõ kết quả, vẫn xóa khách hàng để tránh trường hợp treo
                                val customer = pendingSmsDeliveries.remove(requestId)
                                if (customer != null) {
                                    // Đánh dấu khách hàng đã được xử lý nhưng không xóa
                                    sessionBackup.markCustomerProcessed(customer.id)
                                    
                                    // Gửi broadcast cập nhật tiến độ
                                    sendProgressBroadcast(
                                        totalSent,
                                        totalToSend,
                                        "⚠️ Không xác định được kết quả gửi đến ${customer.name}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(SMS_DELIVERED_ACTION)
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(smsDeliveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(smsDeliveryReceiver, filter)
            }
            Log.d(TAG, "✅ SMS delivery receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register SMS delivery receiver", e)
        }
    }
    
    private fun broadcastCustomerDeleted(customerId: String) {
        val intent = Intent(ACTION_CUSTOMER_DELETED).apply {
            putExtra(EXTRA_CUSTOMER_ID, customerId)
        }
        sendBroadcast(intent)
    }

    /**
     * Kiểm tra các cài đặt thiết bị có thể ảnh hưởng đến việc gửi SMS
     */
    private fun checkDeviceSettings(): String {
        val issues = mutableListOf<String>()

        try {
            // Kiểm tra quyền SMS
            if (!SmsUtils.hasRequiredPermissions(this)) {
                issues.add("❌ Thiếu quyền SMS")
            }

            // Kiểm tra trạng thái mạng
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val networkState = telephonyManager.networkType
            if (networkState == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                issues.add("⚠️ Không xác định được loại mạng")
            }

            // Kiểm tra SIM card
            val simState = telephonyManager.simState
            when (simState) {
                TelephonyManager.SIM_STATE_ABSENT -> issues.add("❌ Không có SIM card")
                TelephonyManager.SIM_STATE_NETWORK_LOCKED -> issues.add("❌ SIM bị khóa mạng")
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> issues.add("❌ SIM yêu cầu PIN")
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> issues.add("❌ SIM yêu cầu PUK")
                TelephonyManager.SIM_STATE_UNKNOWN -> issues.add("⚠️ Trạng thái SIM không xác định")
                TelephonyManager.SIM_STATE_READY -> Log.d(TAG, "✅ SIM card sẵn sàng")
            }

            // Kiểm tra airplane mode
            val airplaneMode = android.provider.Settings.Global.getInt(
                contentResolver,
                android.provider.Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0
            if (airplaneMode) {
                issues.add("❌ Chế độ máy bay đang bật")
            }

            // Kiểm tra SmsManager
            try {
                val smsManager = SmsManager.getDefault()
                Log.d(TAG, "✅ SmsManager khả dụng")
            } catch (e: Exception) {
                issues.add("❌ Không thể truy cập SmsManager: ${e.message}")
            }

        } catch (e: Exception) {
            issues.add("❌ Lỗi kiểm tra thiết bị: ${e.message}")
            Log.e(TAG, "Error checking device settings", e)
        }

        return if (issues.isEmpty()) {
            "✅ Thiết bị sẵn sàng gửi SMS"
        } else {
            "Phát hiện vấn đề:\n${issues.joinToString("\n")}"
        }
    }

    /**
     * Xóa khách hàng sau khi gửi SMS thành công và gửi broadcast thông báo
     */
    private fun deleteCustomerAfterSuccessfulSend(customer: Customer) {
        Log.d(TAG, "🗑️ Bắt đầu xóa khách hàng ${customer.name} (ID: ${customer.id})")
        
        // Đánh dấu khách hàng đã được xử lý trong session backup trước khi xóa
        sessionBackup.markCustomerProcessed(customer.id)
        
        // Lấy danh sách khách hàng hiện tại
        val currentCustomers = smsRepository.getCustomers()
        
        // Kiểm tra xem khách hàng có tồn tại trong danh sách không
        val customerExists = currentCustomers.any { it.id == customer.id }
        if (!customerExists) {
            Log.w(TAG, "⚠️ Khách hàng ${customer.id} không tồn tại trong danh sách")
            return
        }
        
        // Lọc khách hàng ra khỏi danh sách
        val updatedCustomers = currentCustomers.filter { it.id != customer.id }
        
        // Lưu danh sách mới
        smsRepository.saveCustomers(updatedCustomers)
        Log.d(TAG, "💾 Đã lưu danh sách khách hàng mới (đã xóa ${customer.id})")
        
        // Kiểm tra lại xem khách hàng đã thực sự bị xóa chưa
        val checkCustomers = smsRepository.getCustomers()
        val stillExists = checkCustomers.any { it.id == customer.id }
        
        if (stillExists) {
            Log.e(TAG, "❌ Khách hàng ${customer.id} vẫn còn tồn tại sau khi xóa!")
        } else {
            Log.d(TAG, "✅ Đã xóa thành công khách hàng ${customer.id}")
            
            // Gửi broadcast thông báo đã xóa khách hàng
            val intent = Intent(ACTION_CUSTOMER_DELETED).apply {
                putExtra(EXTRA_CUSTOMER_ID, customer.id)
            }
            sendBroadcast(intent)
            Log.d(TAG, "📢 Đã gửi broadcast xóa khách hàng với ID: ${customer.id}")
            
            // Đảm bảo broadcast được gửi bằng cách gửi thêm một lần nữa sau một khoảng thời gian ngắn
            Handler(Looper.getMainLooper()).postDelayed({
                val retryIntent = Intent(ACTION_CUSTOMER_DELETED).apply {
                    putExtra(EXTRA_CUSTOMER_ID, customer.id)
                }
                sendBroadcast(retryIntent)
                Log.d(TAG, "📢 Gửi lại broadcast xóa khách hàng với ID: ${customer.id}")
            }, 500)
        }
    }
} 