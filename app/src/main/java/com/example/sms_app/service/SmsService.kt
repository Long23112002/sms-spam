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
import kotlinx.coroutines.coroutineScope
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
    private val pendingSmsDeliveries = mutableMapOf<String, Customer>()
    private var isRunning = false
    private var totalSent = 0
    private var totalToSend = 0
    private var currentTemplateId = 0
    private var intervalSeconds = 0
    private var isSendingMessages = false
    private var currentProgress = 0
    private var totalMessageCount = 0
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "SmsService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "SmsServiceChannel"
        const val NOTIFICATION_CHANNEL_ID = "SmsServiceChannel"
        
        // Error message constants
        private const val ERROR_NO_CUSTOMERS = "Bạn chưa chọn khách hàng nào để gửi."
        private const val ERROR_NO_SIM = "Thiết bị chưa lắp SIM, vui lòng kiểm tra lại."


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
        const val EXTRA_SIM_ID = "sim_id"
        const val EXTRA_SMS_COUNT = "sms_count"

        // SMS Delivery constants
        const val SMS_SENT_ACTION = "com.example.sms_app.SMS_SENT"
        const val SMS_DELIVERED_ACTION = "com.example.sms_app.SMS_DELIVERED"
    }


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

        initialize()

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

                    Log.d(TAG, "📋 Loaded settings: templateId=$currentTemplateId, interval=$intervalSeconds")

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

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called, ensuring completion broadcast is sent")

        serviceJob?.cancel()
        smsResultReceiver?.let { unregisterReceiver(it) }
        smsDeliveryReceiver?.let { unregisterReceiver(it) }
        pendingSmsResults.values.forEach { it.cancel() }
        pendingSmsResults.clear()
        pendingSmsDeliveries.clear()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    private fun checkSimAndCustomers(): Pair<Boolean, String> {
        try {
            // Kiểm tra SIM
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            
            // Kiểm tra xem có SIM nào được lắp không
            val simState = telephonyManager.simState
            if (simState == TelephonyManager.SIM_STATE_ABSENT) {
                return Pair(false, "Thiết bị chưa lắp SIM, vui lòng kiểm tra lại")
            }

            // Kiểm tra xem có SIM nào hoạt động không
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
            if (activeSubscriptions.isNullOrEmpty()) {
                return Pair(false, "Không tìm thấy SIM nào đang hoạt động")
            }

            // Kiểm tra trạng thái SIM
            if (simState != TelephonyManager.SIM_STATE_READY) {
                return Pair(false, "SIM chưa sẵn sàng, trạng thái: $simState")
            }

            // Kiểm tra danh sách khách hàng
            val customers = smsRepository.getCustomers()
            val selectedCustomers = customers.filter { it.isSelected }
            
            if (selectedCustomers.isEmpty()) {
                return Pair(false, "Bạn chưa chọn khách hàng nào để gửi")
            }

            return Pair(true, "OK")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi kiểm tra SIM và danh sách khách hàng: ${e.message}")
            return Pair(false, "Lỗi kiểm tra: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun startSendingSms() {
        try {
            if (isRunning) {
                Log.w(TAG, "⚠️ SMS service is already running, ignoring new start request")
                return
            }

            // Kiểm tra SIM và danh sách khách hàng trước khi bắt đầu
            val (isValid, message) = checkSimAndCustomers()
            if (!isValid) {
                Log.e(TAG, "❌ Không thể bắt đầu gửi SMS: $message")
                sendCompletionBroadcast(message)
                stopSelf()
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
                customers = customers.take(settings.customerLimit)
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


            serviceScope.launch {
                try {
                    // Tính timeout động: 30 giây/khách hàng + 5 phút buffer, tối thiểu 10 phút
                    val estimatedTimePerCustomer = 30 * 1000L // 30 giây
                    val bufferTime = 5 * 60 * 1000L // 5 phút buffer
                    val minTimeout = 10 * 60 * 1000L // Tối thiểu 10 phút
                    val dynamicTimeout = maxOf(
                        minTimeout,
                        (customers.size * estimatedTimePerCustomer) + bufferTime
                    )

                    Log.d(TAG, "⏰ Timeout được tính: ${dynamicTimeout / 1000 / 60} phút cho ${customers.size} khách hàng")

                    withTimeout(dynamicTimeout) {
                        sendSmsToCustomers(customers, template.content)
                    }
                } catch (e: TimeoutCancellationException) {
                    val timeoutMinutes = (e.message?.let {
                        // Extract timeout from exception if possible
                        Regex("(\\d+)").find(it)?.value?.toLongOrNull()?.div(1000)?.div(60)
                    } ?: 0)
                    Log.e(TAG, "⏰ SMS sending timeout after $timeoutMinutes minutes for ${customers.size} customers")
                    sendCompletionBroadcast("⏰ Timeout: Quá trình gửi SMS bị dừng sau $timeoutMinutes phút (${customers.size} khách hàng)")
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
            // Khởi tạo biến đếm tin nhắn
            var sentMessages = 0
            val totalMessages = customers.size

            // Gửi broadcast ban đầu để UI biết tổng số người cần gửi
            sendProgressBroadcast(0, totalToSend, "Bắt đầu gửi tin nhắn...")




            val initialDelay = intervalSeconds * 1000L
            var remainingInitialDelay = initialDelay
            
            while (remainingInitialDelay > 0 && isRunning) {
                val delayStep = minOf(1000L, remainingInitialDelay)
                delay(delayStep)
                remainingInitialDelay -= delayStep
                
                // Cập nhật notification mỗi giây
                if (remainingInitialDelay % 1000 == 0L) {
                    val remainingSecs = remainingInitialDelay / 1000
                }
            }
            
            if (!isRunning) {
                return
            }



            var currentIndex = 0
            val shouldSendParallel = smsRepository.shouldSendParallelToDualSim(customers.size)
            Log.d(TAG, "🚀 Starting SMS sending: ${customers.size} customers, shouldSendParallel: $shouldSendParallel")

            while (currentIndex < customers.size && isRunning) {
                val customer = customers[currentIndex]

                val currentTime = System.currentTimeMillis()
                try {
//                    // Cập nhật notification để giữ service trong foreground
//                    notificationManager.notify(
//                        NOTIFICATION_ID,
//                        createNotification("Đang gửi tin nhắn ${totalSent + 1}/${totalToSend}")
//                    )

                    val message = formatMessage(templateContent, customer)

                    // Kiểm tra xem có nên gửi song song 2 khách hàng vào 2 SIM không
                    if (shouldSendParallel) {
                        val (sim1, sim2) = smsRepository.getDualSimIds()
                    }

                    // Sử dụng phương thức gửi SMS với delivery report để có thể theo dõi trạng thái
                    // Lấy SIM cho khách hàng này (dual SIM hoặc single SIM)
                    val selectedSim = smsRepository.getSimForCustomer(currentIndex)

                    // Debug dual SIM
                    if (smsRepository.isDualSimEnabled()) {
                        val (sim1, sim2) = smsRepository.getDualSimIds()
                    }

                    var success = false

                    try {
                        if (shouldSendParallel && currentIndex < customers.size - 1) {
                            val nextCustomer = customers[currentIndex + 1]
                            val nextMessage = formatMessage(templateContent, nextCustomer)
                            val (sim1, sim2) = smsRepository.getDualSimIds()

                            val results = coroutineScope {
                                val job1 = async { sendSmsWithDeliveryReport(customer.phoneNumber, message, sim1, customer) }
                                val job2 = async { sendSmsWithDeliveryReport(nextCustomer.phoneNumber, nextMessage, sim2, nextCustomer) }
                                job1.await() to job2.await()
                            }
                            val (success1, success2) = results
                            success = success1 && success2

                            if (success1) {
                                deleteCustomerAfterSuccessfulSend(customer)
                                val forceUpdatedCount1 = smsRepository.forceRefreshSmsCount(sim1)
                                sendSmsCountUpdateBroadcast(sim1, forceUpdatedCount1)
                            }
                            if (success2) {
                                deleteCustomerAfterSuccessfulSend(nextCustomer)
                                val forceUpdatedCount2 = smsRepository.forceRefreshSmsCount(sim2)
                                sendSmsCountUpdateBroadcast(sim2, forceUpdatedCount2)
                            }

                            if (success) {
                                currentIndex += 2
                            }

                        } else {
                            val sentCount = sendSmsToAllPhoneNumbers(customer, message, selectedSim)
                            sentMessages += sentCount
                            success = sentCount > 0

                            // Cập nhật progress theo số tin nhắn đã gửi
                            sendProgressBroadcast(sentMessages, totalMessages, "Đã gửi $sentMessages/$totalMessages tin nhắn")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Lỗi khi gửi SMS cho ${customer.name}: ${e.message}")
                        success = false
                    }

                    // Xử lý khi gửi thất bại - DỪNG TOÀN BỘ
                    if (!success) {
                        Log.e(TAG, "❌ Không thể gửi SMS cho ${customer.name} - DỪNG TOÀN BỘ")

                        // Gọi logic nút "Dừng gửi" - dừng toàn bộ quá trình
                        val errorMessage = "❌ Dừng gửi SMS do không thể gửi tới ${customer.name}"
                        handleSendFailure(customer, errorMessage)
                        return@sendSmsToCustomers // Thoát khỏi toàn bộ quá trình gửi
                    }

                    if (success) {
                        // Khai báo successMessage ở đầu để tránh lỗi reassignment
                        val successMessage = if (shouldSendParallel && currentIndex < customers.size - 1) {
                            val nextCustomer = customers[currentIndex + 1]
                            "🔄 Đã gửi song song: ${customer.name} → SIM1, ${nextCustomer.name} → SIM2"
                        } else {
                            "✅ Đã gửi SMS cho ${customer.name} (${customer.phoneNumber})"
                        }
                        
                        if (shouldSendParallel && currentIndex < customers.size - 1) {
                            // Đã xử lý song song 2 khách hàng
                            totalSent += 2
                        } else {
                            // Xử lý thông thường 1 khách hàng
                            totalSent++
                            // Xóa khách hàng khỏi danh sách UI sau khi gửi thành công
                            deleteCustomerAfterSuccessfulSend(customer)


                            // Tăng số lượng SMS đã gửi trong ngày (chỉ cho single SIM mode)
                            val updatedCount = smsRepository.incrementSmsCount(selectedSim)
                            val forceUpdatedCount = smsRepository.forceRefreshSmsCount(selectedSim)

                            // Send broadcast to update UI with new SMS count
                            sendSmsCountUpdateBroadcast(selectedSim, forceUpdatedCount)
                        }

                        // Kiểm tra xem đây có phải là khách hàng cuối cùng không
                        val isLastCustomer = if (shouldSendParallel && currentIndex < customers.size - 1) {
                            currentIndex + 1 >= customers.size - 1
                        } else {
                            currentIndex >= customers.size - 1
                        }

                        // Nếu đây là khách hàng cuối cùng, gửi completion broadcast ngay lập tức
                        if (isLastCustomer) {
                            val failedCount = totalToSend - totalSent
                            if (failedCount == 0) {
                                val completionMessage = "🏁 Đã hoàn thành gửi $totalSent/$totalToSend tin nhắn"
                                sendCompletionBroadcast(completionMessage)
                            }
                            Log.d(TAG, "🏁 Sent immediate completion broadcast for last customer")
                        }

                        // Chỉ đợi thêm nếu không phải là khách hàng cuối cùng
                        if (!isLastCustomer) {
                            // Dual SIM parallel mode: không delay để gửi thực sự song song
                            val effectiveInterval = if (shouldSendParallel) {
                                0 // Không delay cho parallel mode để gửi thực sự song song
                            } else if (smsRepository.isDualSimEnabled() && customers.size >= 2) {
                                maxOf(intervalSeconds / 2, 1) // Giảm một nửa, tối thiểu 1 giây
                            } else {
                                intervalSeconds
                            }

                            val randomDelay = getRandomDelay(effectiveInterval)

                            // Chỉ delay nếu không phải parallel mode
                            if (effectiveInterval > 0) {
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
                                    val nextCustomerIndex = currentIndex + 1

                                    // Tính tổng thời gian còn lại cho tất cả tin nhắn
                                    val remainingCustomers = totalToSend - totalSent
                                    // remainingCustomers bao gồm cả customer hiện tại đang delay
                                    val totalRemainingSeconds = remainingSecs + ((remainingCustomers - 1) * intervalSeconds)
                                    val totalRemainingMinutes = totalRemainingSeconds / 60
                                    val totalRemainingSecsDisplay = totalRemainingSeconds % 60

                                    // Cập nhật UI thông qua broadcast với tổng thời gian còn lại
                                    val progressMessage = if (shouldSendParallel && nextCustomerIndex < customers.size - 1) {
                                        val nextCustomer = customers[nextCustomerIndex]
                                        val nextNextCustomer = customers[nextCustomerIndex + 1]
                                        if (remainingCustomers > 0) {
                                            "🔄 Còn lại: ${remainingSecs}s trước khi gửi song song cho ${nextCustomer.name} và ${nextNextCustomer.name} (Tổng: ${totalRemainingMinutes}m:${totalRemainingSecsDisplay}s)"
                                        } else {
                                            "🔄 Còn lại: ${remainingSecs}s trước khi gửi song song cho ${nextCustomer.name} và ${nextNextCustomer.name}"
                                        }
                                    } else if (nextCustomerIndex < customers.size) {
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
                                // Parallel mode: không delay, gửi broadcast ngay
                                Log.d(TAG, "🚀 Parallel mode: No delay, sending success broadcast immediately")
                                sendProgressBroadcast(
                                    totalSent,
                                    totalToSend,
                                    successMessage
                                )
                            }
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

                        val failureMessage = if (shouldSendParallel && currentIndex < customers.size - 1) {
                            val nextCustomer = customers[currentIndex + 1]
                            "❌ Lỗi gửi song song cho ${customer.name} và ${nextCustomer.name} - Tiếp tục với người tiếp theo"
                        } else {
                            "❌ Lỗi gửi ${customer.name} (${customer.phoneNumber}) - Tiếp tục với người tiếp theo"
                        }
                        
                        // KHÔNG xóa customer khi gửi thất bại
                        // Customer sẽ được giữ lại trong danh sách
                        Log.d(TAG, "📋 Customer ${customer.name} kept in list due to failed sending")
                        
                        // Vẫn cần delay trước khi xử lý customer tiếp theo
                        val isLastCustomer = currentIndex >= customers.size - 1
                        
                        if (!isLastCustomer) {
                            // Dual SIM parallel mode: không delay để gửi thực sự song song
                            val effectiveInterval = if (shouldSendParallel) {
                                0 // Không delay cho parallel mode
                            } else if (smsRepository.isDualSimEnabled() && customers.size >= 2) {
                                maxOf(intervalSeconds / 2, 1) // Giảm một nửa, tối thiểu 1 giây
                            } else {
                                intervalSeconds
                            }

                            if (effectiveInterval > 0) {
                                val randomDelay = getRandomDelay(effectiveInterval)
                                Log.d(TAG, "⏳ Waiting ${randomDelay}ms before next customer (after failure, dual SIM: ${smsRepository.isDualSimEnabled()})...")
                                delay(randomDelay)
                            } else {
                                Log.d(TAG, "🚀 Parallel mode: No delay after failure")
                            }
                            
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
                    
                    // Dừng ngay khi có lỗi nghiêm trọng và gửi thông báo lỗi
                    val errorMsg = "Lỗi nghiêm trọng gửi SMS cho ${customer.name}: ${e.message}"
                    Log.e(TAG, errorMsg)
                    handleSendFailure(customer, errorMsg)
                    return@sendSmsToCustomers // Thoát khỏi hàm sendSmsToCustomers ngay lập tức
                    
                    Log.e(TAG, "💥 Processing error for ${customer.name}: ${e.message}")

                    val exceptionMessage = if (shouldSendParallel && currentIndex < customers.size - 1) {
                        val nextCustomer = customers[currentIndex + 1]
                        "💥 Lỗi xử lý song song cho ${customer.name} và ${nextCustomer.name}: ${e.message} - Tiếp tục với người tiếp theo"
                    } else {
                        "💥 Lỗi xử lý ${customer.name}: ${e.message} - Tiếp tục với người tiếp theo"
                    }

                    // KHÔNG xóa customer khi có exception
                    // Customer sẽ được giữ lại trong danh sách
                    Log.d(TAG, "📋 Customer ${customer.name} kept in list due to processing exception")
                    
                    // Vẫn cần delay trước khi xử lý customer tiếp theo
                                            val isLastCustomer = currentIndex >= customers.size - 1
                    
                    if (!isLastCustomer) {
                        // Dual SIM parallel mode: không delay để gửi thực sự song song
                        val effectiveInterval = if (shouldSendParallel) {
                            0 // Không delay cho parallel mode
                        } else if (smsRepository.isDualSimEnabled() && customers.size >= 2) {
                            maxOf(intervalSeconds / 2, 1)
                        } else {
                            intervalSeconds
                        }

                        if (effectiveInterval > 0) {
                            val randomDelay = getRandomDelay(effectiveInterval)
                            Log.d(TAG, "⏳ Waiting ${randomDelay}ms before next customer (after exception, dual SIM: ${smsRepository.isDualSimEnabled()})...")
                            delay(randomDelay)
                        } else {
                            Log.d(TAG, "🚀 Parallel mode: No delay after exception")
                        }
                        
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

                // Tăng currentIndex để xử lý khách hàng tiếp theo
                // Trong parallel mode, currentIndex đã được tăng +2 trong logic gửi song song nếu thành công
                // Chỉ tăng +1 nếu không phải parallel mode
                Log.d(TAG, "🔍 Before increment: currentIndex=$currentIndex, shouldSendParallel=$shouldSendParallel, customers.size=${customers.size}")
                if (!shouldSendParallel) {
                    currentIndex++
                    Log.d(TAG, "➡️ Single mode: Moving to next customer: $currentIndex")
                } else {
                    Log.d(TAG, "⏭️ Parallel mode: currentIndex already handled in parallel logic")
                }
            }

            // Hoàn thành gửi SMS
            val failedCount = totalToSend - totalSent
            Log.i(TAG, "🏁 SMS sending completed: $totalSent/$totalToSend messages sent, $failedCount failed")

            if (failedCount == 0) {
                val completionMessage = "🏁 Đã hoàn thành gửi $totalSent/$totalToSend tin nhắn"
                sendCompletionBroadcast(completionMessage)
            }


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

    private suspend fun handleSendFailure(customer: Customer, error: String? = null) {
        val errorMessage = if (error != null) {
            "Lỗi gửi tới ${customer.name}: $error"
        } else {
            "Gửi SMS tới ${customer.name} thất bại"
        }
        Log.e(TAG, "❌ $errorMessage")

        // LUÔN LUÔN dừng service khi có lỗi - giống như nhấn nút "Dừng gửi"
        Log.e(TAG, "🛑 Stopping service due to SMS failure - same as 'Stop' button")

        // Đặt isRunning = false để dừng tất cả loop
        isRunning = false

        // Gửi completion broadcast với thông báo lỗi
        sendCompletionBroadcast(errorMessage)

        // Dừng service
        stopSelf()

        // Hủy tất cả coroutine đang chạy
        serviceJob?.cancel()

        // Clear tất cả pending operations
        pendingSmsResults.values.forEach { it.cancel() }
        pendingSmsResults.clear()
        pendingSmsDeliveries.clear()
    }

    private suspend fun sendSmsWithDeliveryReport(
        phoneNumber: String,
        message: String,
        selectedSim: Int,
        customer: Customer
    ): Boolean {
        // Kiểm tra SIM trước, nếu lỗi thì xử lý thất bại luôn (bên ngoài suspendCancellableCoroutine)
        val (isValid, errorMessage) = checkSimAndCustomers()
        if (!isValid) {
            Log.e(TAG, "❌ Không thể gửi SMS: $errorMessage")
            handleSendFailure(customer, errorMessage)
            return false
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val requestId = "SMS_${System.currentTimeMillis()}_${phoneNumber.hashCode()}"

                val extraCleanedPhone = phoneNumber.trim()
                    .replace(Regex("[^0-9+]"), "") // Remove non-digits
                    .let { num ->
                        if (num.startsWith("+84")) {
                            "0" + num.substring(3)
                        } else if (num.startsWith("84") && num.length >= 11) {
                            "0" + num.substring(2)
                        } else {
                            num
                        }
                    }

                Log.d(TAG, "📱 Original phone: $phoneNumber → Cleaned: $extraCleanedPhone")

                val subscription = if (selectedSim >= 0) {
                    try {
                        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                        subscriptionManager.getActiveSubscriptionInfo(selectedSim)
                    } catch (e: Exception) {
                        Log.w(TAG, "Cannot get subscription info: ${e.message}")
                        null
                    }
                } else null

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

                smsManagers.add(SmsManager.getDefault())

                Log.d(TAG, "🚀 Sending SMS to: $phoneNumber → $extraCleanedPhone")

                pendingSmsResults[requestId] = continuation
                Log.d(TAG, "💾 Stored continuation for requestId: $requestId")

                // Gửi SMS đơn giản không retry
                val sentPendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestId.hashCode(),
                    Intent(SMS_SENT_ACTION).apply {
                        putExtra("REQUEST_ID", requestId)
                        putExtra("PHONE_NUMBER", extraCleanedPhone)
                        putExtra("CUSTOMER_NAME", customer.name)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val deliveredPendingIntent = PendingIntent.getBroadcast(
                    this,
                    (requestId + "_DELIVERED").hashCode(),
                    Intent(SMS_DELIVERED_ACTION).apply {
                        putExtra("REQUEST_ID", requestId)
                        putExtra("PHONE_NUMBER", extraCleanedPhone)
                        putExtra("CUSTOMER_NAME", customer.name)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val smsManager = smsManagers.firstOrNull() ?: SmsManager.getDefault()
                
                if (message.length > 160) {
                    Log.d(TAG, "📝 Message longer than 160 chars, using divideMessage")
                    val parts = smsManager.divideMessage(message)
                    val sentIntents = ArrayList<PendingIntent>()
                    val deliveredIntents = ArrayList<PendingIntent>()
                    
                    for (i in parts.indices) {
                        sentIntents.add(sentPendingIntent)
                        deliveredIntents.add(deliveredPendingIntent)
                    }
                    
                    smsManager.sendMultipartTextMessage(
                        extraCleanedPhone,
                        null,
                        parts,
                        sentIntents,
                        deliveredIntents
                    )
                    Log.d(TAG, "📤 Sending multi-part SMS (${parts.size} parts)")
                } else {
                    smsManager.sendTextMessage(
                        extraCleanedPhone,
                        null,
                        message,
                        sentPendingIntent,
                        deliveredPendingIntent
                    )
                    Log.d(TAG, "📤 Sending single SMS")
                }

                CoroutineScope(Dispatchers.IO).launch {
                    delay(30000)
                    if (pendingSmsResults.containsKey(requestId)) {
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
    }




    private fun formatMessage(template: String, customer: Customer): String {
        try {
            Log.d(TAG, "⚙️ Formatting template: ${template.take(50)}${if (template.length > 50) "..." else ""}")
            Log.d(TAG, "📋 Customer data: ${customer.name}, ${customer.phoneNumber}, ${customer.idNumber}")

            // Format theo các cột cấu hình: xxx, yyy, sdt, ttt, zzz, www, uuu, vvv, rrr
            var message = template

            // Cột 1: xxx - Tên khách hàng
            // Handle all bracket types: {xxx}, [xxx], (xxx) - keep brackets, replace content
            message = message.replace("{xxx}", "{${customer.name}}")
                .replace("{XXX}", "{${customer.name}}")
                .replace("[xxx]", "[${customer.name}]")
                .replace("[XXX]", "[${customer.name}]")
                .replace("(xxx)", "(${customer.name})")
                .replace("(XXX)", "(${customer.name})")
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

            Log.d(TAG, "🔍 After xxx/XXX replacement: ${message.take(100)}${if (message.length > 100) "..." else ""}")

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
            // Handle all bracket types: {yyy}, [yyy], (yyy) - keep brackets, replace content
            message = message.replace("{yyy}", "{${customer.idNumber}}")
                .replace("{YYY}", "{${customer.idNumber}}")
                .replace("[yyy]", "[${customer.idNumber}]")
                .replace("[YYY]", "[${customer.idNumber}]")
                .replace("(yyy)", "(${customer.idNumber})")
                .replace("(YYY)", "(${customer.idNumber})")
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
            // Handle all bracket types: {sdt}, [sdt], (sdt) - keep brackets, replace content
            message = message.replace("{sdt}", "{${customer.phoneNumber}}")
                .replace("{SDT}", "{${customer.phoneNumber}}")
                .replace("{SĐT}", "{${customer.phoneNumber}}")
                .replace("[sdt]", "[${customer.phoneNumber}]")
                .replace("[SDT]", "[${customer.phoneNumber}]")
                .replace("[SĐT]", "[${customer.phoneNumber}]")
                .replace("(sdt)", "(${customer.phoneNumber})")
                .replace("(SDT)", "(${customer.phoneNumber})")
                .replace("(SĐT)", "(${customer.phoneNumber})")
                .replace(" sdt", " " + customer.phoneNumber)
                .replace(" SDT", " " + customer.phoneNumber)
                .replace(" SĐT", " " + customer.phoneNumber)
                .replace("sdt ", customer.phoneNumber + " ")
                .replace("SDT ", customer.phoneNumber + " ")
                .replace("SĐT ", customer.phoneNumber + " ")

            // Cột 4: ttt - Địa chỉ
            // Handle all bracket types: {ttt}, [ttt], (ttt) - keep brackets, replace content
            message = message.replace("{ttt}", "{${customer.address}}")
                .replace("{TTT}", "{${customer.address}}")
                .replace("[ttt]", "[${customer.address}]")
                .replace("[TTT]", "[${customer.address}]")
                .replace("(ttt)", "(${customer.address})")
                .replace("(TTT)", "(${customer.address})")
                .replace(" ttt", " " + customer.address)
                .replace(" TTT", " " + customer.address)
                .replace("ttt ", customer.address + " ")
                .replace("TTT ", customer.address + " ")

            // Cột 5-9: zzz, www, uuu, vvv, rrr - Các trường tùy chọn 1-5
            // Handle all bracket types: {zzz}, [zzz], (zzz) - keep brackets, replace content
            message = message.replace("{zzz}", "{${customer.option1 ?: ""}}")
                .replace("{ZZZ}", "{${customer.option1 ?: ""}}")
                .replace("[zzz]", "[${customer.option1 ?: ""}]")
                .replace("[ZZZ]", "[${customer.option1 ?: ""}]")
                .replace("(zzz)", "(${customer.option1 ?: ""})")
                .replace("(ZZZ)", "(${customer.option1 ?: ""})")
                .replace(" zzz", " " + (customer.option1 ?: ""))
                .replace(" ZZZ", " " + (customer.option1 ?: ""))
                .replace("zzz ", (customer.option1 ?: "") + " ")
                .replace("ZZZ ", (customer.option1 ?: "") + " ")

            // Handle all bracket types: {www}, [www], (www) - keep brackets, replace content
            message = message.replace("{www}", "{${customer.option2 ?: ""}}")
                .replace("{WWW}", "{${customer.option2 ?: ""}}")
                .replace("[www]", "[${customer.option2 ?: ""}]")
                .replace("[WWW]", "[${customer.option2 ?: ""}]")
                .replace("(www)", "(${customer.option2 ?: ""})")
                .replace("(WWW)", "(${customer.option2 ?: ""})")
                .replace(" www", " " + (customer.option2 ?: ""))
                .replace(" WWW", " " + (customer.option2 ?: ""))
                .replace("www ", (customer.option2 ?: "") + " ")
                .replace("WWW ", (customer.option2 ?: "") + " ")

            // Handle all bracket types: {uuu}, [uuu], (uuu) - keep brackets, replace content
            message = message.replace("{uuu}", "{${customer.option3 ?: ""}}")
                .replace("{UUU}", "{${customer.option3 ?: ""}}")
                .replace("[uuu]", "[${customer.option3 ?: ""}]")
                .replace("[UUU]", "[${customer.option3 ?: ""}]")
                .replace("(uuu)", "(${customer.option3 ?: ""})")
                .replace("(UUU)", "(${customer.option3 ?: ""})")
                .replace(" uuu", " " + (customer.option3 ?: ""))
                .replace(" UUU", " " + (customer.option3 ?: ""))
                .replace("uuu ", (customer.option3 ?: "") + " ")
                .replace("UUU ", (customer.option3 ?: "") + " ")

            // Handle all bracket types: {vvv}, [vvv], (vvv) - keep brackets, replace content
            message = message.replace("{vvv}", "{${customer.option4 ?: ""}}")
                .replace("{VVV}", "{${customer.option4 ?: ""}}")
                .replace("[vvv]", "[${customer.option4 ?: ""}]")
                .replace("[VVV]", "[${customer.option4 ?: ""}]")
                .replace("(vvv)", "(${customer.option4 ?: ""})")
                .replace("(VVV)", "(${customer.option4 ?: ""})")
                .replace(" vvv", " " + (customer.option4 ?: ""))
                .replace(" VVV", " " + (customer.option4 ?: ""))
                .replace("vvv ", (customer.option4 ?: "") + " ")
                .replace("VVV ", (customer.option4 ?: "") + " ")

            // Handle all bracket types: {rrr}, [rrr], (rrr) - keep brackets, replace content
            message = message.replace("{rrr}", "{${customer.option5 ?: ""}}")
                .replace("{RRR}", "{${customer.option5 ?: ""}}")
                .replace("[rrr]", "[${customer.option5 ?: ""}]")
                .replace("[RRR]", "[${customer.option5 ?: ""}]")
                .replace("(rrr)", "(${customer.option5 ?: ""})")
                .replace("(RRR)", "(${customer.option5 ?: ""})")
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
                        val phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: "Unknown"
                        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: "Unknown"
                        
                        when (resultCode) {
                            Activity.RESULT_OK -> {
                                Log.d(TAG, "✅ SMS sent successfully to $phoneNumber!")
                                
                                val continuation = pendingSmsResults.remove(requestId)
                                if (continuation != null) {
                                    Log.d(TAG, "📤 SMS sent (requestId: $requestId, success: true)")
                                    continuation.resume(true)
                                } else {
                                    Log.w(TAG, "⚠️ No continuation found for successful SMS: $requestId")
                                }
                            }
                            else -> {
                                val errorType = when (resultCode) {
                                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Generic failure"
                                    SmsManager.RESULT_ERROR_NO_SERVICE -> "No service"
                                    SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
                                    SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio off"
                                    else -> "Unknown error $resultCode"
                                }
                                
                                Log.e(TAG, "❌ SMS sending failed: $errorType to $phoneNumber")
                                
                                val continuation = pendingSmsResults.remove(requestId)
                                if (continuation != null) {
                                    Log.d(TAG, "📤 SMS sent (requestId: $requestId, success: false)")
                                    continuation.resume(false)
                                } else {
                                    Log.w(TAG, "⚠️ No continuation found for failed SMS: $requestId")
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
                    val deleteIntent = Intent(ACTION_CUSTOMER_DELETED).apply {
                        putExtra(EXTRA_CUSTOMER_ID, customer.id)
                        putExtra(EXTRA_MESSAGE, "Đã xóa khách hàng ${customer.name} sau khi gửi SMS thành công")
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    }
                    sendBroadcast(deleteIntent)
                    applicationContext.sendBroadcast(deleteIntent)
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

    /**
     * Gửi SMS đến tất cả số điện thoại của khách hàng
     * Trả về số tin nhắn đã gửi thành công
     */
    private suspend fun sendSmsToAllPhoneNumbers(customer: Customer, message: String, selectedSim: Int): Int {
        val phoneNumbers = customer.phoneNumber.split(",").map { it.trim() }.filter { it.isNotBlank() }

        if (phoneNumbers.isEmpty()) {
            Log.w(TAG, "❌ Không có số điện thoại hợp lệ cho khách hàng ${customer.name}")
            return 0
        }

        Log.d(TAG, "📱 Gửi SMS đến ${phoneNumbers.size} số điện thoại cho khách hàng ${customer.name}")

        var successCount = 0

        for ((index, phoneNumber) in phoneNumbers.withIndex()) {
            try {
                Log.d(TAG, "📱 Gửi SMS ${index + 1}/${phoneNumbers.size} đến số: $phoneNumber")

                val success = sendSmsWithDeliveryReport(phoneNumber, message, selectedSim, customer)

                if (success) {
                    successCount++
                    Log.d(TAG, "✅ Gửi thành công SMS ${index + 1}/${phoneNumbers.size} đến $phoneNumber")
                } else {
                    Log.e(TAG, "❌ Gửi thất bại SMS ${index + 1}/${phoneNumbers.size} đến $phoneNumber")
                }

                // Thêm delay nhỏ giữa các SMS để tránh spam
                if (index < phoneNumbers.size - 1) {
                    delay(1000) // 1 giây delay giữa các SMS
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi gửi SMS đến $phoneNumber: ${e.message}", e)
            }
        }

        Log.d(TAG, "📊 Kết quả gửi SMS cho ${customer.name}: $successCount/${phoneNumbers.size} thành công")

        return successCount
    }

}