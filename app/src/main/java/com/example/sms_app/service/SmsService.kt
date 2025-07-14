package com.example.sms_app.service

import android.Manifest
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
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.example.sms_app.R
import com.example.sms_app.data.Customer
import com.example.sms_app.data.SmsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.TimeoutCancellationException
import com.example.sms_app.presentation.activity.MainActivity
import com.example.sms_app.data.SmsTemplate
import android.os.Handler
import android.os.Looper
import android.app.Activity
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresPermission
import com.example.sms_app.utils.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SmsService : Service() {
    private var serviceJob: Job? = null
    private lateinit var smsRepository: SmsRepository
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

    companion object {
        private const val TAG = "SmsService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "SmsServiceChannel"
        const val NOTIFICATION_CHANNEL_ID = "SmsServiceChannel"

        const val ACTION_PROGRESS_UPDATE = "com.example.sms_app.ACTION_PROGRESS_UPDATE"
        const val ACTION_SMS_COMPLETED = "com.example.sms_app.ACTION_SMS_COMPLETED"
        const val ACTION_CUSTOMER_DELETED = "com.example.sms_app.ACTION_CUSTOMER_DELETED"
        const val ACTION_SMS_COUNT_UPDATED = "com.example.sms_app.ACTION_SMS_COUNT_UPDATED"

        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_CUSTOMER_ID = "customer_id"
        const val EXTRA_TEMPLATE_ID = "template_id"
        const val EXTRA_INTERVAL_SECONDS = "interval_seconds"
        const val EXTRA_MAX_RETRY = "max_retry"
        const val EXTRA_RETRY_DELAY = "retry_delay"
        const val EXTRA_SIM_ID = "sim_id"
        const val EXTRA_SMS_COUNT = "sms_count"

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

    data class SmsAttempt(
        val requestId: String,
        val phoneNumber: String,
        val message: String,
        val customer: Customer,
        val smsManager: SmsManager,
        val managerIndex: Int,
        val phoneFormat: String,
        val formatIndex: Int,
        val attemptNumber: Int,
        val maxAttempts: Int,
        val continuation: CancellableContinuation<Boolean>
    )

    // Global attempt tracking
    private val activeAttempts = mutableMapOf<String, SmsAttempt>()
    private val attemptQueue = mutableMapOf<String, MutableList<SmsAttempt>>()
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SmsService onCreate")
        isSendingMessages = false
        currentProgress = 0
        totalMessageCount = 0

        smsRepository = SmsRepository(applicationContext)

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
        initialize()

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

    @SuppressLint("MissingPermission")
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
                        val notification = createHiddenNotification()
                        Log.d(TAG, "📲 Starting foreground service with hidden notification")
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

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun startSendingSms() {
        try {
            if (isRunning) {
                Log.w(TAG, "⚠️ SMS service is already running, ignoring new start request")
                return
            }

            // Reset all counters and states
            totalSent = 0
            totalToSend = 0
            Log.d(TAG, "🔄 Reset SMS counters: totalSent=$totalSent, totalToSend=$totalToSend")

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

            Log.d(TAG, "🚀 Starting SMS sending process")

            val allCustomers = smsRepository.getCustomers()
            var customers = allCustomers.filter { it.isSelected }
            
            // Áp dụng giới hạn khách hàng từ settings
            val settings = smsRepository.getAppSettings()
            if (settings.isLimitCustomer) {
                Log.d(TAG, "🚫 Service applying customer limit: ${customers.size} → ${settings.customerLimit}")
                customers = customers.take(settings.customerLimit)
                Log.d(TAG, "✂️ Final customer list: ${customers.map { it.name }}")
            } else {
                Log.d(TAG, "🔍 Customer limit disabled in service - processing all ${customers.size} selected customers")
            }

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
                sendCompletionBroadcast("❌ Không có khách hàng nào được chọn để gửi SMS")
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

            Log.d(TAG, "🎯 Will send SMS to ${customers.size} customers")

            // Cập nhật notification để tăng mức độ ưu tiên
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            // notificationManager.notify(NOTIFICATION_ID, createNotification("Bắt đầu gửi ${customers.size} tin nhắn..."))

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
//            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // notificationManager.notify(NOTIFICATION_ID, createNotification("Đang chuẩn bị gửi tin nhắn..."))

            // Gửi broadcast ban đầu để UI biết tổng số người cần gửi
            sendProgressBroadcast(0, totalToSend, "Bắt đầu gửi tin nhắn...")

            // Đếm số lần thử lại tổng cộng
            var totalRetries = 0
            val maxTotalRetries = maxRetryAttempts * 3 // Giới hạn số lần thử lại tổng cộng

            // Thêm initial delay trước tin nhắn đầu tiên
            Log.d(TAG, "⏳ Đợi ${intervalSeconds}s trước khi gửi tin nhắn đầu tiên...")
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "🕐 Service start time: ${startTime}")
            
            // Cập nhật notification trong lúc chờ
            // notificationManager.notify(
            //     NOTIFICATION_ID,
            //     createNotification("Chuẩn bị gửi tin nhắn...")
            // )
            
            // Thực hiện initial delay với countdown
            val initialDelay = intervalSeconds * 1000L
            var remainingInitialDelay = initialDelay
            
            while (remainingInitialDelay > 0 && isRunning) {
                val delayStep = minOf(1000L, remainingInitialDelay)
                delay(delayStep)
                remainingInitialDelay -= delayStep
                
                // Cập nhật notification mỗi giây
                if (remainingInitialDelay % 1000 == 0L) {
                    val remainingSecs = remainingInitialDelay / 1000
                    // notificationManager.notify(
                    //     NOTIFICATION_ID,
                    //     createNotification("Đợi ${remainingSecs}s trước khi gửi tin nhắn đầu tiên...")
                    // )
                    Log.d(TAG, "⏳ Còn lại ${remainingSecs}s trước khi gửi tin nhắn đầu tiên")
                }
            }
            
            if (!isRunning) {
                Log.d(TAG, "❌ Service stopped during initial delay")
                return
            }
            
            Log.d(TAG, "🚀 Bắt đầu gửi SMS sau initial delay ${intervalSeconds}s")
            
            // Cập nhật notification
            // notificationManager.notify(
            //     NOTIFICATION_ID,
            //     createNotification("Bắt đầu gửi tin nhắn...")
            // )

            for ((customerIndex, customer) in customers.withIndex()) {
                if (!isRunning) {
                    Log.d(TAG, "Service stopped, breaking SMS sending loop")
                    break
                }
                
                Log.i(TAG, "📋 Processing customer ${customerIndex + 1}/${totalToSend}: ${customer.name} (${customer.phoneNumber})")
                
                val currentTime = System.currentTimeMillis()
                Log.d(TAG, "🕐 Current time: ${currentTime}")

                try {
//                    // Cập nhật notification để giữ service trong foreground
//                    notificationManager.notify(
//                        NOTIFICATION_ID,
//                        createNotification("Đang gửi tin nhắn ${totalSent + 1}/${totalToSend}")
//                    )

                    val message = formatMessage(templateContent, customer)
                    Log.d(TAG, "🚀 Attempting to send SMS to ${customer.name} (${customer.phoneNumber})")
                    Log.d(TAG, "📝 Message content: ${message.take(50)}${if (message.length > 50) "..." else ""}")

                    // Sử dụng phương thức gửi SMS với delivery report để có thể theo dõi trạng thái
                    // Lấy SIM cho khách hàng này (dual SIM hoặc single SIM)
                    val selectedSim = smsRepository.getSimForCustomer(customerIndex)

                    // Debug dual SIM
                    if (smsRepository.isDualSimEnabled()) {
                        val (sim1, sim2) = smsRepository.getDualSimIds()
                        Log.d(TAG, "🔄 Dual SIM: Customer $customerIndex → SIM $selectedSim (SIM1: $sim1, SIM2: $sim2)")
                    } else {
                        Log.d(TAG, "📱 Single SIM: Customer $customerIndex → SIM $selectedSim")
                    }

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
                        Log.d(TAG, "✅ Customer ${customer.name} processed successfully")

                        // Xóa khách hàng khỏi danh sách UI sau khi gửi thành công
                        deleteCustomerAfterSuccessfulSend(customer)

                        // Ghi lại thông tin để gửi broadcast sau khi delay xong
                        val successMessage = "✅ Đã gửi SMS cho ${customer.name} (${customer.phoneNumber})"
                        Log.d(TAG, "✅ SMS sent successfully to ${customer.name}")
                        Log.d(TAG, "📋 Success recorded, will broadcast AFTER delay period (${intervalSeconds}s)")

                        // Tăng số lượng SMS đã gửi trong ngày
                        val updatedCount = smsRepository.incrementSmsCount(selectedSim)

                        // Force read the count again to ensure it's updated
                        val forceUpdatedCount = smsRepository.forceRefreshSmsCount(selectedSim)
                        Log.d(TAG, "📊 Updated SMS count for SIM $selectedSim: $forceUpdatedCount/40, increment success: $updatedCount")

                        // Send broadcast to update UI with new SMS count
                        sendSmsCountUpdateBroadcast(selectedSim, forceUpdatedCount)

                        Log.d(TAG, "✅ SMS sent successfully to ${customer.name} (${customer.phoneNumber})")

                        // Kiểm tra xem đây có phải là khách hàng cuối cùng không
                        val isLastCustomer = customerIndex >= customers.size - 1

                        // Nếu đây là khách hàng cuối cùng, gửi completion broadcast ngay lập tức
                        if (isLastCustomer) {
                            val failedCount = totalToSend - totalSent
                            val completionMessage = if (failedCount == 0) {
                                "🏁 Đã hoàn thành gửi $totalSent/${totalToSend} tin nhắn"
                            } else {
                                "🏁 Đã hoàn thành: $totalSent thành công, $failedCount thất bại (tổng $totalToSend tin nhắn)"
                            }
                            sendCompletionBroadcast(completionMessage)
                            Log.d(TAG, "🏁 Sent immediate completion broadcast for last customer")
                        }

                        // Chỉ đợi thêm nếu không phải là khách hàng cuối cùng
                        if (!isLastCustomer) {
                            // Dual SIM: chỉ giảm thời gian chờ một nửa khi có ít nhất 2 khách hàng
                            val effectiveInterval = if (smsRepository.isDualSimEnabled() && customers.size >= 2) {
                                maxOf(intervalSeconds / 2, 1) // Giảm một nửa, tối thiểu 1 giây
                            } else {
                                intervalSeconds
                            }

                            val randomDelay = getRandomDelay(effectiveInterval)
                            Log.d(TAG, "⏳ Waiting ${randomDelay}ms (${randomDelay/1000}s) before next SMS...")
                            Log.d(TAG, "⏳ Interval setting: ${intervalSeconds}s (effective: ${effectiveInterval}s, dual SIM: ${smsRepository.isDualSimEnabled()}, customers: ${customers.size})")

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
                                    val nextCustomerIndex = customerIndex + 1

                                    // Tính tổng thời gian còn lại cho tất cả tin nhắn
                                    val remainingCustomers = totalToSend - totalSent
                                    // remainingCustomers bao gồm cả customer hiện tại đang delay
                                    val totalRemainingSeconds = remainingSecs + ((remainingCustomers - 1) * intervalSeconds)
                                    val totalRemainingMinutes = totalRemainingSeconds / 60
                                    val totalRemainingSecsDisplay = totalRemainingSeconds % 60

                                    // Hiển thị thông tin chi tiết hơn trong notification
                                    val notificationMessage = if (nextCustomerIndex < customers.size) {
                                        val nextCustomer = customers[nextCustomerIndex]
                                        "Đã gửi $totalSent/$totalToSend tin nhắn. Còn lại: ${remainingSecs}s trước khi gửi cho ${nextCustomer.name}"
                                    } else {
                                        "Đã gửi $totalSent/$totalToSend tin nhắn. Còn lại: ${remainingSecs}s..."
                                    }
//
//                                    notificationManager.notify(
//                                        NOTIFICATION_ID,
//                                        createNotification(notificationMessage)
//                                    )

                                    // Cập nhật UI thông qua broadcast với tổng thời gian còn lại
                                    val progressMessage = if (nextCustomerIndex < customers.size) {
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
                            
                            // Sau khi delay xong, gửi broadcast thành công
                            Log.d(TAG, "⏰ Delay period completed, sending success broadcast")
                            sendProgressBroadcast(
                                totalSent,
                                totalToSend,
                                successMessage
                            )
                        } else {
                            Log.d(TAG, "🏁 Đã gửi tin nhắn cho khách hàng cuối cùng, không cần đợi thêm")
                            // Gửi broadcast ngay cho khách hàng cuối cùng
                            sendProgressBroadcast(
                                totalSent,
                                totalToSend,
                                successMessage
                            )
                        }
                    } else {
                        Log.e(TAG, "❌ Failed to send SMS to ${customer.name} (${customer.phoneNumber}) after $retryCount retries")
                        
                        // KHÔNG DỪNG SERVICE - Chỉ log lỗi và tiếp tục với khách hàng tiếp theo
                        Log.w(TAG, "⚠️ SMS failed for ${customer.name}, continuing with next customer")
                        
                        Log.w(TAG, "❌ Failed to send SMS to ${customer.name} after $retryCount attempts")

                        val failureMessage = "❌ Lỗi gửi ${customer.name} (${customer.phoneNumber}) sau $retryCount lần thử - Tiếp tục với người tiếp theo"
                        
                        // KHÔNG xóa customer khi gửi thất bại
                        // Customer sẽ được giữ lại trong danh sách
                        Log.d(TAG, "📋 Customer ${customer.name} kept in list due to failed sending")
                        
                        // Vẫn cần delay trước khi xử lý customer tiếp theo
                        val isLastCustomer = customerIndex >= customers.size - 1
                        
                        if (!isLastCustomer) {
                            // Dual SIM: chỉ giảm thời gian chờ một nửa khi có ít nhất 2 khách hàng
                            val effectiveInterval = if (smsRepository.isDualSimEnabled() && customers.size >= 2) {
                                maxOf(intervalSeconds / 2, 1)
                            } else {
                                intervalSeconds
                            }
                            val randomDelay = getRandomDelay(effectiveInterval)
                            Log.d(TAG, "⏳ Waiting ${randomDelay}ms before next customer (after failure, dual SIM: ${smsRepository.isDualSimEnabled()})...")
                            delay(randomDelay)
                            
                            // Gửi broadcast sau khi delay xong
                            Log.d(TAG, "⏰ Failure delay completed, sending failure broadcast")
                            sendProgressBroadcast(
                                totalSent,
                                totalToSend,
                                failureMessage
                            )
                        } else {
                            // Gửi broadcast ngay cho khách hàng cuối cùng khi failed
                            sendProgressBroadcast(
                                totalSent,
                                totalToSend,
                                failureMessage
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "💥 Error processing customer ${customer.name} (${customer.phoneNumber})", e)

                    // KHÔNG DỪNG SERVICE - Chỉ log lỗi và tiếp tục với khách hàng tiếp theo
                    Log.w(TAG, "⚠️ Exception for ${customer.name}, continuing with next customer")
                    
                    Log.e(TAG, "💥 Processing error for ${customer.name}: ${e.message}")

                    val exceptionMessage = "💥 Lỗi xử lý ${customer.name}: ${e.message} - Tiếp tục với người tiếp theo"

                    // KHÔNG xóa customer khi có exception
                    // Customer sẽ được giữ lại trong danh sách
                    Log.d(TAG, "📋 Customer ${customer.name} kept in list due to processing exception")
                    
                    // Vẫn cần delay trước khi xử lý customer tiếp theo
                    val isLastCustomer = customerIndex >= customers.size - 1
                    
                    if (!isLastCustomer) {
                        // Dual SIM: chỉ giảm thời gian chờ một nửa khi có ít nhất 2 khách hàng
                        val effectiveInterval = if (smsRepository.isDualSimEnabled() && customers.size >= 2) {
                            maxOf(intervalSeconds / 2, 1)
                        } else {
                            intervalSeconds
                        }
                        val randomDelay = getRandomDelay(effectiveInterval)
                        Log.d(TAG, "⏳ Waiting ${randomDelay}ms before next customer (after exception, dual SIM: ${smsRepository.isDualSimEnabled()})...")
                        delay(randomDelay)
                        
                        // Gửi broadcast sau khi delay xong
                        Log.d(TAG, "⏰ Exception delay completed, sending exception broadcast")
                        sendProgressBroadcast(
                            totalSent,
                            totalToSend,
                            exceptionMessage
                        )
                    } else {
                        // Gửi broadcast ngay cho khách hàng cuối cùng khi exception
                        sendProgressBroadcast(
                            totalSent,
                            totalToSend,
                            exceptionMessage
                        )
                    }
                }

                // Continue processing next customer
                Log.d(TAG, "🔄 Finished processing customer ${customer.name}")
            }

            // Hoàn thành gửi SMS
            val failedCount = totalToSend - totalSent
            Log.i(TAG, "🏁 SMS sending completed: $totalSent/$totalToSend messages sent, $failedCount failed")
            
            val completionMessage = if (failedCount == 0) {
                "🏁 Đã hoàn thành gửi $totalSent/${totalToSend} tin nhắn"
            } else {
                "🏁 Đã hoàn thành: $totalSent thành công, $failedCount thất bại (tổng $totalToSend tin nhắn)"
            }
            
            sendCompletionBroadcast(completionMessage)

            Log.d(TAG, "🏁 SMS sending process completed")

            // Dừng service sau khi gửi thông báo hoàn thành
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "💥 Critical error in sendSmsToCustomers", e)
            sendCompletionBroadcast("💥 Lỗi nghiêm trọng: ${e.message}")
            Log.e(TAG, "Service stopping due to critical error")
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
            
            // Apply deep cleaning for problematic numbers before sending
            val extraCleanedPhone = phoneNumber.trim()
                .replace(Regex("[^0-9+]"), "") // Remove non-digits
                .let { num ->
                    // Special formatting for Vietnamese carrier numbers
                    when {
                        // Viettel numbers (038x) - có thể có vấn đề với một số đầu số
                        num.startsWith("038") -> {
                            Log.d(TAG, "🔧 Detected Viettel 038 number, applying special handling")
                            "+84" + num.substring(1)
                        }
                        // Mobifone numbers có thể gặp vấn đề
                        num.startsWith("070") || num.startsWith("076") || num.startsWith("077") || 
                        num.startsWith("078") || num.startsWith("079") -> {
                            Log.d(TAG, "🔧 Detected Mobifone number, applying special handling")
                            "+84" + num.substring(1)
                        }
                        // Vinaphone numbers
                        num.startsWith("081") || num.startsWith("082") || num.startsWith("083") || 
                        num.startsWith("084") || num.startsWith("085") -> {
                            Log.d(TAG, "🔧 Detected Vinaphone number, applying special handling")
                            "+84" + num.substring(1)
                        }
                        // Problematic prefixes - force international format
                        num.startsWith("0946") || num.startsWith("0167") -> {
                            Log.d(TAG, "🔧 Detected problematic prefix, forcing international format")
                            "+84" + num.substring(1)
                        }
                        else -> num
                    }
                }
            
            Log.d(TAG, "📱 Original phone: $phoneNumber → Cleaned: $extraCleanedPhone")
            
            // Get SIM manager info for debugging
            val subscription = if (selectedSim >= 0) {
                try {
                    val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    subscriptionManager.getActiveSubscriptionInfo(selectedSim)
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot get subscription info: ${e.message}")
                    null
                }
            } else null
            
            // Try multiple SIM managers based on selection
            val smsManagers = mutableListOf<SmsManager>()
            
            if (selectedSim >= 0 && subscription != null) {
                try {
                    val simSpecificManager = SmsManager.getSmsManagerForSubscriptionId(subscription.subscriptionId)
                    smsManagers.add(simSpecificManager)
                    Log.d(TAG, "📱 Added SIM-specific manager for subscription: ${subscription.subscriptionId}")
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot create SIM-specific manager: ${e.message}")
                }
            }
            
            // Always add default manager as fallback
            smsManagers.add(SmsManager.getDefault())
            
            // Try different phone number formats
            val phoneFormats = listOf(
                extraCleanedPhone,
                phoneNumber.trim(), // Original format
                if (extraCleanedPhone.startsWith("+84")) extraCleanedPhone.replace("+84", "0") else "+84" + extraCleanedPhone.substring(1)
            ).distinct()
            
            Log.d(TAG, "📱 Will try ${smsManagers.size} managers with ${phoneFormats.size} formats = ${smsManagers.size * phoneFormats.size} total attempts")
            
            // Create all attempts
            val attempts = mutableListOf<SmsAttempt>()
            var attemptNumber = 1
            val maxAttempts = smsManagers.size * phoneFormats.size
            
            for ((managerIndex, smsManager) in smsManagers.withIndex()) {
                for ((formatIndex, phoneFormat) in phoneFormats.withIndex()) {
                    attempts.add(
                        SmsAttempt(
                            requestId = requestId,
                            phoneNumber = extraCleanedPhone,
                            message = message,
                            customer = customer,
                            smsManager = smsManager,
                            managerIndex = managerIndex + 1,
                            phoneFormat = phoneFormat,
                            formatIndex = formatIndex,
                            attemptNumber = attemptNumber,
                            maxAttempts = maxAttempts,
                            continuation = continuation
                        )
                    )
                    attemptNumber++
                }
            }
            
            // Store attempts in queue
            attemptQueue[requestId] = attempts
            
            // Store continuation for later use
            pendingSmsResults[requestId] = continuation
            Log.d(TAG, "💾 Stored continuation for requestId: $requestId")
            
            // Start with first attempt
            executeNextAttempt(requestId)
            
            // Set up timeout mechanism
            CoroutineScope(Dispatchers.IO).launch {
                delay(30000) // 30 second timeout
                if (pendingSmsResults.containsKey(requestId)) {
                    attemptQueue.remove(requestId)
                    activeAttempts.remove(requestId)
                    val timeoutContinuation = pendingSmsResults.remove(requestId)
                    if (timeoutContinuation?.isActive == true) {
                        Log.e(TAG, "⏰ SMS sending timeout after 30 seconds for requestId: $requestId")
                        timeoutContinuation.resume(false)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Critical SMS sending error", e)
            continuation.resume(false)
        }
    }
    
    private fun executeNextAttempt(requestId: String) {
        val queue = attemptQueue[requestId]
        if (queue.isNullOrEmpty()) {
            Log.e(TAG, "❌ No more attempts available for $requestId")
            return
        }
        
        val attempt = queue.removeAt(0)
        activeAttempts[requestId] = attempt
        
        Log.d(TAG, "🔧 Executing attempt ${attempt.attemptNumber}/${attempt.maxAttempts}: Manager #${attempt.managerIndex} with phone: ${attempt.phoneFormat}")
        
        try {
            val attemptRequestId = "${requestId}_ATTEMPT_${attempt.attemptNumber}"
            
            val sentPendingIntent = PendingIntent.getBroadcast(
                this,
                attemptRequestId.hashCode(),
                Intent(SMS_SENT_ACTION).apply {
                    putExtra("REQUEST_ID", requestId)
                    putExtra("ATTEMPT_REQUEST_ID", attemptRequestId)
                    putExtra("PHONE_NUMBER", attempt.phoneFormat)
                    putExtra("CUSTOMER_NAME", attempt.customer.name)
                    putExtra("MANAGER_INDEX", attempt.managerIndex)
                    putExtra("ATTEMPT_COUNT", attempt.attemptNumber)
                    putExtra("MAX_ATTEMPTS", attempt.maxAttempts)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val deliveredPendingIntent = PendingIntent.getBroadcast(
                this,
                (attemptRequestId + "_DELIVERED").hashCode(),
                Intent(SMS_DELIVERED_ACTION).apply {
                    putExtra("REQUEST_ID", requestId)
                    putExtra("PHONE_NUMBER", attempt.phoneFormat)
                    putExtra("CUSTOMER_NAME", attempt.customer.name)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (attempt.message.length > 160) {
                Log.d(TAG, "📝 Message longer than 160 chars, using divideMessage")
                val parts = attempt.smsManager.divideMessage(attempt.message)
                val sentIntents = ArrayList<PendingIntent>()
                val deliveredIntents = ArrayList<PendingIntent>()
                
                for (i in parts.indices) {
                    sentIntents.add(sentPendingIntent)
                    deliveredIntents.add(deliveredPendingIntent)
                }
                
                attempt.smsManager.sendMultipartTextMessage(
                    attempt.phoneFormat,
                    null,
                    parts,
                    sentIntents,
                    deliveredIntents
                )
                Log.d(TAG, "📤 Sending multi-part SMS (${parts.size} parts)")
            } else {
                attempt.smsManager.sendTextMessage(
                    attempt.phoneFormat,
                    null,
                    attempt.message,
                    sentPendingIntent,
                    deliveredPendingIntent
                )
                Log.d(TAG, "📤 Sending single SMS")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during attempt ${attempt.attemptNumber}: ${e.message}")
            
            // Try next attempt after a short delay
            CoroutineScope(Dispatchers.IO).launch {
                delay(500)
                executeNextAttempt(requestId)
            }
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

        val notificationIntent = Intent(this, com.example.sms_app.presentation.activity.MainActivity::class.java).apply {
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

    private fun createHiddenNotification(): android.app.Notification {
        createNotificationChannel()

        val notificationIntent = Intent(this, com.example.sms_app.presentation.activity.MainActivity::class.java).apply {
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.color.transparent)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setSilent(true)
            .setShowWhen(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SMS Service Channel"
            val descriptionText = "Channel for SMS Service"
            val importance = NotificationManager.IMPORTANCE_HIGH // Tăng mức độ ưu tiên
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE // Hiển thị trên màn hình khóa
                setShowBadge(true) // Hiển thị badge trên icon
                enableLights(true) // Bật đèn thông báo
                lightColor = android.graphics.Color.BLUE
                enableVibration(true) // Bật rung
                vibrationPattern = longArrayOf(0, 1000, 500, 1000) // Mẫu rung
            }

//            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
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
        // notificationManager.notify(NOTIFICATION_ID, createNotification(message))
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
                        val requestId = intent.getStringExtra("REQUEST_ID") ?: return
                        val attemptRequestId = intent.getStringExtra("ATTEMPT_REQUEST_ID") ?: requestId
                        val phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: "Unknown"
                        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: "Unknown"
                        val managerIndex = intent.getIntExtra("MANAGER_INDEX", 0)
                        val attemptCount = intent.getIntExtra("ATTEMPT_COUNT", 0)
                        val maxAttempts = intent.getIntExtra("MAX_ATTEMPTS", 0)
                        
                        when (resultCode) {
                            Activity.RESULT_OK -> {
                                Log.d(TAG, "✅ SMS sent successfully! (Attempt $attemptCount/$maxAttempts, Manager #$managerIndex)")
                                
                                // Success! Clean up and resume continuation
                                val attempt = activeAttempts.remove(requestId)
                                attemptQueue.remove(requestId)
                                val continuation = pendingSmsResults.remove(requestId)
                                
                                if (continuation != null) {
                                    Log.d(TAG, "📤 SMS sent (requestId: $requestId, success: true)")
                                    continuation.resume(true)
                                } else {
                                    Log.w(TAG, "⚠️ No continuation found for successful SMS: $requestId")
                                }
                            }
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                                Log.e(TAG, "❌ SMS sending failed: Generic failure (Attempt $attemptCount/$maxAttempts, Manager #$managerIndex)")
                                
                                // Get additional system information for debugging
                                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                                val networkOperator = try {
                                    telephonyManager.networkOperatorName ?: "Unknown"
                                } catch (e: Exception) {
                                    "Error getting operator: ${e.message}"
                                }
                                
                                val simState = try {
                                    when (telephonyManager.simState) {
                                        TelephonyManager.SIM_STATE_ABSENT -> "ABSENT"
                                        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "NETWORK_LOCKED"
                                        TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN_REQUIRED"
                                        TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK_REQUIRED"
                                        TelephonyManager.SIM_STATE_READY -> "READY"
                                        TelephonyManager.SIM_STATE_UNKNOWN -> "UNKNOWN"
                                        else -> "OTHER"
                                    }
                                } catch (e: Exception) {
                                    "Error getting SIM state: ${e.message}"
                                }
                                
                                val phoneValidation = phoneNumber.validateAndFormatPhoneNumber()
                                
                                Log.e(TAG, """
🔍 DETAILED ERROR INFO (Attempt $attemptCount/$maxAttempts):
📱 Phone: $requestId
👤 Customer: $customerName
☎️ Phone Number: $phoneNumber
✓ Phone Format: ${if (phoneValidation == phoneNumber) "Valid" else "Modified"}
📞 Formatted: $phoneValidation
💳 Using SIM: ${intent.getIntExtra("SIM_ID", -1)}
📶 Network: $networkOperator
🔋 Battery saver: ${(getSystemService(Context.POWER_SERVICE) as android.os.PowerManager).isPowerSaveMode}
💾 Available storage: ${android.os.StatFs(filesDir.path).availableBytes / 1024 / 1024} MB
📡 SIM State: $simState
🔧 Manager Index: $managerIndex
""")
                                
                                // Check if there are more attempts to try
                                val queue = attemptQueue[requestId]
                                if (queue != null && queue.isNotEmpty()) {
                                    // Try next attempt after a short delay
                                    Log.d(TAG, "🔄 ${queue.size} attempts remaining, trying next...")
                                    
                                    CoroutineScope(Dispatchers.IO).launch {
                                        delay(1000) // Wait 1 second before next attempt
                                        
                                                                                 executeNextAttempt(requestId)
                                    }
                                } else {
                                    // No more attempts, fail
                                    Log.e(TAG, "💥 All attempts exhausted. SMS sending failed.")
                                    
                                    val attempt = activeAttempts.remove(requestId)
                                    attemptQueue.remove(requestId)
                                    val continuation = pendingSmsResults.remove(requestId)
                                    
                                    if (continuation != null) {
                                        Log.d(TAG, "📤 SMS sent (requestId: $requestId, success: false)")
                                        continuation.resume(false)
                                    } else {
                                        Log.w(TAG, "⚠️ No continuation found for failed SMS: $requestId")
                                    }
                                }
                            }
                            SmsManager.RESULT_ERROR_NO_SERVICE,
                            SmsManager.RESULT_ERROR_NULL_PDU,
                            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                                val errorType = when (resultCode) {
                                    SmsManager.RESULT_ERROR_NO_SERVICE -> "No service"
                                    SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
                                    SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio off"
                                    else -> "Unknown error $resultCode"
                                }
                                
                                Log.e(TAG, "❌ SMS sending failed: $errorType (Attempt $attemptCount/$maxAttempts, Manager #$managerIndex)")
                                
                                // For these errors, we should fail immediately as they indicate system issues
                                val attempt = activeAttempts.remove(requestId)
                                attemptQueue.remove(requestId)
                                val continuation = pendingSmsResults.remove(requestId)
                                
                                if (continuation != null) {
                                    Log.d(TAG, "📤 SMS sent (requestId: $requestId, success: false) - System error")
                                    continuation.resume(false)
                                } else {
                                    Log.w(TAG, "⚠️ No continuation found for failed SMS (system error): $requestId")
                                }
                            }
                            else -> {
                                Log.e(TAG, "❌ SMS sending failed: Unknown error code $resultCode (Attempt $attemptCount/$maxAttempts, Manager #$managerIndex)")
                                
                                // Check if there are more attempts to try
                                val queue = attemptQueue[requestId]
                                if (queue != null && queue.isNotEmpty()) {
                                    Log.d(TAG, "🔄 Unknown error, trying next attempt...")
                                    
                                    CoroutineScope(Dispatchers.IO).launch {
                                        delay(1000)
                                        executeNextAttempt(requestId)
                                    }
                                } else {
                                    val attempt = activeAttempts.remove(requestId)
                                    attemptQueue.remove(requestId)
                                    val continuation = pendingSmsResults.remove(requestId)
                                    
                                    if (continuation != null) {
                                        continuation.resume(false)
                                    } else {
                                        Log.w(TAG, "⚠️ No continuation found for failed SMS (unknown error): $requestId")
                                    }
                                }
                            }
                        }
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
                                    Log.d(TAG, "✅ SMS delivery confirmed for ${customer.name}")

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

                                Log.w(TAG, "SMS delivery failed for customer ID: $customerId")

                                // Gửi broadcast cập nhật tiến độ
                                sendProgressBroadcast(
                                    totalSent,
                                    totalToSend,
                                    "❌ SMS không đến được người nhận (requestId: $requestId)"
                                )
                            }
                            else -> {
                                Log.w(TAG, "⚠️ SMS delivery result unknown (resultCode: $resultCode, requestId: $requestId)")

                                // Khách hàng đã được xóa sau khi gửi SMS thành công
                                val customer = pendingSmsDeliveries.remove(requestId)
                                if (customer != null) {
                                    Log.d(TAG, "SMS delivery status unknown for ${customer.name}")

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


    /**
     * Kiểm tra các cài đặt thiết bị có thể ảnh hưởng đến việc gửi SMS
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun checkDeviceSettings(): String {
        val issues = mutableListOf<String>()

        try {
            // Kiểm tra quyền SMS
            if (!hasRequiredPermissions(this)) {
                issues.add("❌ Thiếu quyền SMS")
            }

            // Kiểm tra trạng thái mạng
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
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
            val airplaneMode = Settings.Global.getInt(
                contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
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
            Log.e(TAG, "Error checking device settings ${e.printStackTrace()}")
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

            // Gửi broadcast thông báo đã xóa khách hàng với nhiều cơ chế đảm bảo
            val intent = Intent(ACTION_CUSTOMER_DELETED).apply {
                putExtra(EXTRA_CUSTOMER_ID, customer.id)
                putExtra(EXTRA_MESSAGE, "Đã xóa khách hàng ${customer.name} sau khi gửi SMS thành công")
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            
            // Gửi broadcast ngay lập tức
            try {
                sendBroadcast(intent)
                Log.d(TAG, "📢 Đã gửi broadcast xóa khách hàng với ID: ${customer.id}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi gửi broadcast xóa khách hàng", e)
            }
            
            // Gửi lại với applicationContext
            try {
                applicationContext.sendBroadcast(intent)
                Log.d(TAG, "📢 Đã gửi broadcast xóa khách hàng qua applicationContext")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi gửi broadcast qua applicationContext", e)
            }

            // Đảm bảo broadcast được gửi bằng cách gửi thêm một lần nữa sau một khoảng thời gian ngắn
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val retryIntent = Intent(ACTION_CUSTOMER_DELETED).apply {
                        putExtra(EXTRA_CUSTOMER_ID, customer.id)
                        putExtra(EXTRA_MESSAGE, "Đã xóa khách hàng ${customer.name} sau khi gửi SMS thành công")
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    }
                    sendBroadcast(retryIntent)
                    applicationContext.sendBroadcast(retryIntent)
                    Log.d(TAG, "📢 Gửi lại broadcast xóa khách hàng với ID: ${customer.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Lỗi gửi lại broadcast xóa khách hàng", e)
                }
            }, 500)
        }
    }

    // Método para enviar broadcast de atualização de contagem de SMS
    private fun sendSmsCountUpdateBroadcast(simId: Int, smsCount: Int) {
        val intent = Intent(ACTION_SMS_COUNT_UPDATED).apply {
            putExtra(EXTRA_SIM_ID, simId)
            putExtra(EXTRA_SMS_COUNT, smsCount)
        }
        Log.d(TAG, "📢 Broadcasting SMS count update: SIM $simId, count $smsCount")
        sendBroadcast(intent)
    }

    private fun isPowerSaveMode(): Boolean {
        return try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                powerManager.isPowerSaveMode
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking power save mode", e)
            false
        }
    }

    private fun getAvailableStorage(): Long {
        return try {
            val path = applicationContext.filesDir.absolutePath
            val stat = android.os.StatFs(path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            (availableBlocks * blockSize) / (1024 * 1024) // Return in MB
        } catch (e: Exception) {
            Log.e(TAG, "Error checking available storage", e)
            -1
        }
    }

    // Add this helper method for logging exception details
    private fun logSmsException(e: Exception, phoneNumber: String, operation: String) {
        val sb = StringBuilder()
        sb.append(e.printStackTrace())
        sb.append("\n📛 SMS EXCEPTION DETAILS:")
        sb.append("\n🔧 Operation: $operation")
        sb.append("\n📱 Phone Number: $phoneNumber")
        sb.append("\n💥 Exception: ${e.javaClass.name}")
        sb.append("\n💬 Message: ${e.message}")

        // Check phone number format
        val isValidFormat = phoneNumber.isValidPhoneNumber()
        val formattedNumber = phoneNumber.validateAndFormatPhoneNumber()
        sb.append("\n☎️ Phone Format Valid: $isValidFormat")
        sb.append("\n📞 Formatted Number: $formattedNumber")

        // Include SIM info
        val selectedSim = smsRepository.getSelectedSim()
        sb.append("\n💳 SIM: $selectedSim")

        // Add stack trace
        val stackTrace = e.stackTraceToString()
        // Limit stack trace to first 15 lines to avoid excessive logging
        val limitedStackTrace = stackTrace.lines().take(15).joinToString("\n")
        sb.append("\n📚 Stack Trace (first 15 lines):\n$limitedStackTrace")

        Log.e(TAG, sb.toString())
    }
} 