package com.example.sms_app.presentation.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sms_app.data.SmsProgress
import com.example.sms_app.data.Customer
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.data.SessionBackup
import com.example.sms_app.presentation.component.formatDuration
import com.example.sms_app.service.SmsService
import com.example.sms_app.utils.SimInfo
import com.example.sms_app.utils.SimConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SendMessageViewModel @Inject constructor(
    application: Application,
    private val smsRepository: SmsRepository,
) : AndroidViewModel(application) {

    private val _progress = MutableLiveData<SmsProgress>()
    val progress: LiveData<SmsProgress> = _progress

    private val _completion = MutableLiveData<String>()
    val completion: LiveData<String> = _completion

    private val _isSending = MutableLiveData<Boolean>()
    val isSending: LiveData<Boolean> = _isSending

    private val _millisUntilFinished = MutableLiveData<Long>(0)
    val millisUntilFinished: LiveData<Long> = _millisUntilFinished
    
    private var currentCountDownTimer: CountDownTimer? = null
    private var totalCustomers: Int = 0

    private val smsProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModelScope.launch(Dispatchers.IO) {
                Timber.tag(TAG).i("onReceive: action ${intent?.action}")
                when (intent?.action) {
                    SmsService.ACTION_PROGRESS_UPDATE -> {
                        intent.let {
                            val progress = it.getIntExtra(SmsService.EXTRA_PROGRESS, 0)
                            val total = it.getIntExtra(SmsService.EXTRA_TOTAL, 0)
                            val message = it.getStringExtra(SmsService.EXTRA_MESSAGE) ?: ""
                            val p = SmsProgress(progress, total, message)
                            Timber.tag(TAG).i("onReceive: SmsProgress $p")
                            _progress.postValue(p)
                            
                            // Dừng countdown timer và kết thúc quá trình gửi khi đạt 100%
                            if (progress >= total && total > 0) {
                                currentCountDownTimer?.cancel()
                                _millisUntilFinished.postValue(0L)
                                _isSending.postValue(false)
                                // Xóa dữ liệu countdown đã lưu
                                smsRepository.clearCountdownData()
                                android.util.Log.d(TAG, "🗑️ Cleared countdown data on 100% progress")
                                android.util.Log.d(TAG, "🏁 Countdown stopped and sending finished - reached 100% progress")
                            }
                            // Xóa logic cập nhật countdown timer để tránh "giật giật"
                            // Countdown timer sẽ tự chạy từ startCountdownTimer()
                        }
                    }

                    SmsService.ACTION_SMS_COMPLETED -> {
                        val message =
                            intent.getStringExtra(SmsService.EXTRA_MESSAGE) ?: "Hoàn thành gửi SMS"

                        // Dừng countdown timer khi hoàn thành
                        currentCountDownTimer?.cancel()
                        _millisUntilFinished.postValue(0L)
                        _isSending.postValue(false)

                        // Xóa dữ liệu countdown đã lưu
                        smsRepository.clearCountdownData()
                        android.util.Log.d(TAG, "🗑️ Cleared countdown data on SMS completed")

                        // Xóa session backup khi hoàn thành
                        val sessionBackup = SessionBackup(getApplication())
                        sessionBackup.completeSession()
                        android.util.Log.d(TAG, "🗑️ Completed and cleared session backup on SMS completed")

                        android.util.Log.d(TAG, "✅ Setting completion message: $message")
                        _completion.postValue(message)
                    }
                }
            }
        }
    }

    init {
        IntentFilter().apply {
            addAction(SmsService.ACTION_PROGRESS_UPDATE)
            addAction(SmsService.ACTION_SMS_COMPLETED)
        }.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.registerReceiver(smsProgressReceiver, it, Context.RECEIVER_NOT_EXPORTED)
            } else {
                application.registerReceiver(smsProgressReceiver, it)
            }
        }
        
        // Khôi phục countdown timer nếu service đang chạy
        restoreCountdownIfServiceRunning()
    }
    
    private fun restoreCountdownIfServiceRunning() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Clear any existing countdown data to prevent showing stale countdown state
                smsRepository.clearCountdownData()
                android.util.Log.d(TAG, "🗑️ Cleared any existing countdown data on ViewModel initialization")

                // Clear any active session backup to prevent completion state conflicts
                val sessionBackup = SessionBackup(getApplication())
                sessionBackup.clearActiveSession()
                android.util.Log.d(TAG, "🗑️ Cleared any active session backup on ViewModel initialization")

                // Reset states to initial values
                _isSending.postValue(false)
                _millisUntilFinished.postValue(0L)
                _completion.postValue(null)
                _progress.postValue(SmsProgress(0, 0, ""))

                android.util.Log.d(TAG, "🔄 Reset all countdown states to initial values")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error clearing countdown data", e)
            }
        }
    }

    /**
     * Reset tất cả states về trạng thái ban đầu
     */
    fun resetAllStates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Dừng countdown timer nếu có
                currentCountDownTimer?.cancel()
                currentCountDownTimer = null

                // Reset tất cả states
                _isSending.postValue(false)
                _millisUntilFinished.postValue(0L)
                _progress.postValue(SmsProgress(0, 0, ""))
                _completion.postValue(null)

                // Xóa dữ liệu đã lưu
                smsRepository.clearCountdownData()
                val sessionBackup = SessionBackup(getApplication())
                sessionBackup.clearActiveSession()

                android.util.Log.d(TAG, "🔄 Reset all states to initial values")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error resetting states", e)
            }
        }
    }

    fun sendMessage(messageTemplate: MessageTemplate, simConfig: SimConfig) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Validate mẫu tin nhắn trước khi gửi
                if (messageTemplate.content.isBlank()) {
                    android.util.Log.e(TAG, "❌ Message template is empty")
                    withContext(Dispatchers.Main) {
                        _progress.value = SmsProgress(0, 0, "❌ Vui lòng cấu hình mẫu tin nhắn trước khi gửi!")
                    }
                    return@launch
                }

                android.util.Log.d(TAG, "✅ Message template validated: ${messageTemplate.content.take(50)}...")

                // Reset tất cả states trước khi bắt đầu
                android.util.Log.d(TAG, "🔄 Resetting all states before starting new SMS session")

                // Dừng countdown timer nếu có
                currentCountDownTimer?.cancel()
                currentCountDownTimer = null

                // Clear any existing countdown data and session backup
                smsRepository.clearCountdownData()
                val sessionBackup = SessionBackup(getApplication())
                sessionBackup.clearActiveSession()

                // Reset tất cả states
                _isSending.postValue(false) // Set false trước, sau đó set true
                _millisUntilFinished.postValue(0L)
                _progress.postValue(SmsProgress(0, 0, ""))
                _completion.postValue(null)

                android.util.Log.d(TAG, "✅ All states reset, now starting SMS sending")
                _isSending.postValue(true)

                // Thêm delay nhỏ để đảm bảo dữ liệu UI đã được flush xuống repository
                kotlinx.coroutines.delay(100) // 100ms delay
                Log.d(TAG, "⏳ Added 100ms delay to ensure data is flushed to repository")

                // Lấy số lượng khách hàng được chọn
                val allCustomers = smsRepository.getCustomers()
                var selectedCustomers = allCustomers.filter { it.isSelected }
                
                // Validation: Kiểm tra nếu không có khách hàng nào được chọn
                if (selectedCustomers.isEmpty()) {
                    android.util.Log.d(TAG, "❌ No customers selected - aborting SMS sending")
                    _isSending.postValue(false)
                    _millisUntilFinished.postValue(0L)
                    _progress.postValue(SmsProgress(0, 0, ""))
                    // Đảm bảo không bao giờ báo completion khi không có customers
                    _completion.postValue("❌ Không có khách hàng nào được chọn để gửi SMS")
                    return@launch
                }
                
                // Lấy settings trước
                val settings = smsRepository.getAppSettings()
                
                // Debug: Log settings values
                android.util.Log.d(TAG, "🔍 DEBUG Customer Limit Settings:")
                android.util.Log.d(TAG, "   isLimitCustomer: ${settings.isLimitCustomer}")
                android.util.Log.d(TAG, "   customerLimit: ${settings.customerLimit}")
                android.util.Log.d(TAG, "   selectedCustomers.size: ${selectedCustomers.size}")
                android.util.Log.d(TAG, "   selectedCustomers: ${selectedCustomers.map { it.name }}")
                
                // Kiểm tra giới hạn số khách hàng nếu được bật
                android.util.Log.d(TAG, "🔍 Checking customer limit: isLimitCustomer=${settings.isLimitCustomer}")
                
                if (settings.isLimitCustomer) {
                    android.util.Log.d(TAG, "🚫 Customer limit is ENABLED - applying limit of ${settings.customerLimit}")
                    android.util.Log.d(TAG, "🚫 Before limit: ${selectedCustomers.size} customers → ${selectedCustomers.map { it.name }}")
                    
                    selectedCustomers = selectedCustomers.take(settings.customerLimit)
                    
                    android.util.Log.d(TAG, "✂️ After limit: ${selectedCustomers.size} customers → ${selectedCustomers.map { it.name }}")
                    
                    // Thông báo cho user biết giới hạn đã được áp dụng
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            getApplication<Application>(),
                            "⚠️ Giới hạn áp dụng: Chỉ gửi cho ${settings.customerLimit} khách hàng đầu tiên",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    android.util.Log.d(TAG, "🔍 Customer limit is DISABLED - sending to all selected customers")
                }
                
                totalCustomers = selectedCustomers.size
                android.util.Log.d(TAG, "🎯 FINAL: Will send SMS to ${selectedCustomers.size} customers: ${selectedCustomers.map { it.name }}")
                
                // Final validation: Đảm bảo vẫn còn customers sau khi apply limit
                if (selectedCustomers.isEmpty()) {
                    android.util.Log.e(TAG, "❌ CRITICAL: No customers remaining after applying settings/limits")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            getApplication<Application>(),
                            "❌ Không có khách hàng nào để gửi sau khi áp dụng cài đặt",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    _isSending.postValue(false)
                    _millisUntilFinished.postValue(0L)
                    _progress.postValue(SmsProgress(0, 0, ""))
                    _completion.postValue("❌ Không có khách hàng nào để gửi SMS")
                    return@launch
                }
                
                Log.d(TAG, "�� SendMessage: Total customers: ${allCustomers.size}, Selected: $totalCustomers")
                
                // Debug: Hiển thị tất cả khách hàng và trạng thái selection
                Log.d(TAG, "🔍 ALL CUSTOMERS AND THEIR SELECTION STATUS:")
                allCustomers.forEachIndexed { index, customer ->
                    Log.d(TAG, "   $index. ${customer.name} (${customer.phoneNumber}) - isSelected: ${customer.isSelected}")
                }
                
                Log.d(TAG, "🎯 SELECTED CUSTOMERS (after limit check):")
                selectedCustomers.forEachIndexed { index, customer ->
                    Log.d(TAG, "   $index. ${customer.name} (${customer.phoneNumber})")
                }
                

                
                // Chuyển sang main thread để tạo countdown timer và khởi động service
                withContext(Dispatchers.Main) {
                    val application = getApplication<Application>()

                    // Dừng service cũ nếu có trước khi khởi động mới
                    try {
                        application.stopService(Intent(application, SmsService::class.java))
                        android.util.Log.d(TAG, "🛑 Stopped any existing SMS service before starting new one")
                        // Đợi một chút để đảm bảo service đã dừng hoàn toàn
                        kotlinx.coroutines.delay(500)
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "⚠️ Error stopping existing service: ${e.message}")
                    }

                    // Lưu cấu hình SIM
                    if (simConfig.isDualSim && simConfig.allSims.size >= 2) {
                        android.util.Log.d(TAG, "🔄 Dual SIM mode enabled with ${simConfig.allSims.size} SIMs")
                        smsRepository.setDualSimConfig(
                            isDualSim = true,
                            sim1Id = simConfig.allSims[0].subscriptionId,
                            sim2Id = simConfig.allSims[1].subscriptionId
                        )
                        smsRepository.setSelectedSim(simConfig.allSims[0].subscriptionId)

                        // Dual SIM: chỉ giảm thời gian một nửa khi có ít nhất 2 khách hàng
                        val dualSimInterval = if (totalCustomers >= 2) {
                            maxOf(settings.intervalBetweenSmsSeconds / 2, 1)
                        } else {
                            settings.intervalBetweenSmsSeconds
                        }
                        android.util.Log.d(TAG, "⏱️ Dual SIM countdown: ${totalCustomers} customers, ${dualSimInterval}s interval (original: ${settings.intervalBetweenSmsSeconds}s)")
                        startCountdownTimer(totalCustomers, dualSimInterval)
                    } else {
                        android.util.Log.d(TAG, "📱 Single SIM mode: ${simConfig.primarySim.subscriptionId}")
                        smsRepository.setDualSimConfig(isDualSim = false, sim1Id = simConfig.primarySim.subscriptionId)
                        smsRepository.setSelectedSim(simConfig.primarySim.subscriptionId)

                        // Single SIM: thời gian bình thường
                        android.util.Log.d(TAG, "⏱️ Single SIM countdown: ${totalCustomers} customers, ${settings.intervalBetweenSmsSeconds}s interval")
                        startCountdownTimer(totalCustomers, settings.intervalBetweenSmsSeconds)
                    }
                    val intent = Intent(application, SmsService::class.java).apply {
                        putExtra(SmsService.EXTRA_TEMPLATE_ID, messageTemplate.id)
                        putExtra(SmsService.EXTRA_INTERVAL_SECONDS, settings.intervalBetweenSmsSeconds)
                    }
                    ContextCompat.startForegroundService(application, intent)
                    android.util.Log.d(TAG, "🚀 Started new SMS service")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendMessage", e)
                _isSending.postValue(false)
                _millisUntilFinished.postValue(0L)
                _progress.postValue(SmsProgress(0, 0, ""))
                _completion.postValue("❌ Lỗi khi gửi SMS: ${e.message}")
            }
        }

    private fun startCountdownTimer(totalCustomers: Int, intervalSeconds: Int) {
        try {
            // Dừng timer cũ nếu có
            currentCountDownTimer?.cancel()
            
            val safeIntervalSeconds = maxOf(intervalSeconds, 1) // Đảm bảo >= 1 giây
            
            // Kiểm tra số khách hàng thực tế
            Log.d(TAG, "🔍 Countdown timer setup: $totalCustomers customers provided")
            
            if (totalCustomers <= 0) {
                Log.w(TAG, "⚠️ No customers selected for countdown timer")
                _millisUntilFinished.postValue(0L)
                _isSending.postValue(false)
                return
            }
            
            // Tính tổng thời gian: initial delay + (số tin nhắn - 1) × interval
            // Có initial delay trước tin nhắn đầu tiên, sau đó delay giữa các tin nhắn
            val totalTimeInSeconds = if (totalCustomers > 1) {
                // Initial delay + delay giữa các tin nhắn = totalCustomers × interval
                totalCustomers * safeIntervalSeconds
            } else {
                // Chỉ có 1 tin nhắn thì có initial delay
                safeIntervalSeconds
            }
            val totalTimeInMillis = totalTimeInSeconds * 1000L
            
            Log.d(TAG, "🕒 Starting countdown timer: $totalCustomers customers, ${safeIntervalSeconds}s interval")
            Log.d(TAG, "🕒 Countdown calculation: ${if (totalCustomers > 1) "initial delay + (${totalCustomers - 1} × ${safeIntervalSeconds}s) = ${totalTimeInSeconds}s" else "${safeIntervalSeconds}s for single message (initial delay)"}")
            Log.d(TAG, "🕒 Total countdown time: ${totalTimeInSeconds}s (${totalTimeInMillis}ms)")
            
            // Lưu thông tin countdown để khôi phục sau này
            val startTime = System.currentTimeMillis()
            viewModelScope.launch(Dispatchers.IO) {
                smsRepository.saveCountdownData(startTime, totalTimeInMillis, totalCustomers)
                android.util.Log.d(TAG, "💾 Saved countdown data: start=$startTime, total=$totalTimeInMillis, customers=$totalCustomers")
            }
            
            // Cập nhật giá trị ban đầu ngay lập tức
            _millisUntilFinished.postValue(totalTimeInMillis)
            
            currentCountDownTimer = object : CountDownTimer(totalTimeInMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    try {
                        Log.d(TAG, "⏰ Countdown tick: ${millisUntilFinished}ms (${millisUntilFinished/1000}s)")
                        _millisUntilFinished.postValue(millisUntilFinished)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating countdown timer", e)
                    }
                }

                override fun onFinish() {
                    try {
                        Log.d(TAG, "✅ Countdown finished")
                        _millisUntilFinished.postValue(0L)
                        _isSending.postValue(false)
                        // Xóa dữ liệu countdown đã lưu
                        viewModelScope.launch(Dispatchers.IO) {
                            smsRepository.clearCountdownData()
                            android.util.Log.d(TAG, "🗑️ Cleared countdown data on finish")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error finishing countdown timer", e)
                    }
                }
            }
            currentCountDownTimer?.start()
            Log.d(TAG, "🚀 Countdown timer started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting countdown timer", e)
            // Fallback: set default values
            _millisUntilFinished.postValue(0L)
            _isSending.postValue(false)
        }
    }

    fun stop() = viewModelScope.launch(Dispatchers.IO) {
        android.util.Log.d(TAG, "🛑 Stop button pressed - stopping SMS service")

        _isSending.postValue(false)

        // Dừng countdown timer
        currentCountDownTimer?.cancel()
        currentCountDownTimer = null
        _millisUntilFinished.postValue(0L)

        // Reset progress state về trạng thái ban đầu
        _progress.postValue(SmsProgress(0, 0, ""))

        android.util.Log.d(TAG, "🔄 Reset progress states to initial values")

        // Xóa dữ liệu countdown đã lưu
        smsRepository.clearCountdownData()
        android.util.Log.d(TAG, "🗑️ Cleared countdown data on stop")

        // Xóa session backup khi dừng
        val sessionBackup = SessionBackup(getApplication())
        sessionBackup.clearActiveSession()
        android.util.Log.d(TAG, "🗑️ Cleared session backup on stop")

        // Dừng service và gửi broadcast
        getApplication<Application>().let {
            it.stopService(
                Intent(it, SmsService::class.java).apply {
                    setAction("STOP_SMS_SERVICE")
                }
            )
        }

        android.util.Log.d(TAG, "✅ Stop process completed")
    }

    override fun onCleared() {
        super.onCleared()
        // Dừng countdown timer khi ViewModel bị hủy
        currentCountDownTimer?.cancel()
        currentCountDownTimer = null
        getApplication<Application>().unregisterReceiver(smsProgressReceiver)
    }

    companion object {
        private const val TAG = "SendMessageViewModel"
    }
}