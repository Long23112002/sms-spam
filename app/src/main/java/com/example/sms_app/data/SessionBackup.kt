package com.example.sms_app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class SmsSession(
    val sessionId: String,
    val templateId: Int,
    val totalCustomers: Int,
    var sentCount: Int,
    var remainingCustomers: List<Customer>,
    val startTime: Long,
    var lastUpdateTime: Long,
    var sessionName: String = "", // Tên phiên làm việc
    var status: String = "Đang xử lý", // Trạng thái: Đang xử lý, Hoàn thành, Thất bại
    var failedReason: String = "", // Lý do thất bại nếu có
    var failedCustomerId: String = "", // ID của khách hàng thất bại
    var completedCustomers: MutableList<Customer> = mutableListOf() // Danh sách khách hàng đã gửi thành công
)

class SessionBackup(private val context: Context) {
    private val TAG = "SessionBackup"
    private val PREFS_NAME = "sms_session_backup"
    private val ACTIVE_SESSION_KEY = "active_session"
    private val SESSION_HISTORY_KEY = "session_history"
    private val MAX_HISTORY_ITEMS = 20 // Tăng số lượng phiên làm việc lưu trữ
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val gson = Gson()
    
    /**
     * Tạo một ID phiên làm việc mới
     */
    fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Tạo tên phiên mặc định dựa trên thời gian
     */
    private fun generateDefaultSessionName(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return "Phiên ${dateFormat.format(Date())}"
    }
    
    /**
     * Lưu phiên làm việc hiện tại
     */
    fun saveActiveSession(session: SmsSession) {
        try {
            // Đảm bảo phiên có tên
            if (session.sessionName.isEmpty()) {
                session.sessionName = generateDefaultSessionName()
            }
            
            val sessionJson = gson.toJson(session)
            sharedPreferences.edit().putString(ACTIVE_SESSION_KEY, sessionJson).apply()
            Log.d(TAG, "Saved active session: ${session.sessionId} - ${session.sessionName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving active session", e)
        }
    }
    
    /**
     * Lưu phiên làm việc hiện tại với ID và danh sách khách hàng
     * @return true nếu lưu thành công, false nếu có lỗi
     */
    fun saveActiveSession(templateId: Int, customers: List<Customer>, sessionName: String = ""): Boolean {
        try {
            val sessionId = generateSessionId()
            val actualSessionName = if (sessionName.isEmpty()) generateDefaultSessionName() else sessionName
            
            Log.d(TAG, "Bắt đầu lưu session backup với ${customers.size} khách hàng")
            
            // Ghi log chi tiết về khách hàng đầu tiên để debug
            if (customers.isNotEmpty()) {
                val firstCustomer = customers.first()
                Log.d(TAG, "Mẫu khách hàng đầu tiên: ID=${firstCustomer.id}, Tên=${firstCustomer.name}, SĐT=${firstCustomer.phoneNumber}")
            }
            
            val session = SmsSession(
                sessionId = sessionId,
                templateId = templateId,
                totalCustomers = customers.size,
                sentCount = 0,
                remainingCustomers = customers,
                startTime = System.currentTimeMillis(),
                lastUpdateTime = System.currentTimeMillis(),
                sessionName = actualSessionName,
                status = "Đang xử lý",
                completedCustomers = mutableListOf()
            )
            
            // Đảm bảo lưu ngay lập tức và trả về kết quả
            val sessionJson = gson.toJson(session)
            val result = sharedPreferences.edit().putString(ACTIVE_SESSION_KEY, sessionJson).commit() // Dùng commit thay vì apply để đảm bảo lưu ngay lập tức
            
            if (result) {
                Log.d(TAG, "Đã lưu session backup thành công: ID=${session.sessionId}, Tên=${session.sessionName}, Số KH=${session.totalCustomers}")
            } else {
                Log.e(TAG, "Không thể lưu session backup")
            }
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error creating and saving active session", e)
            return false
        }
    }
    
    /**
     * Cập nhật tiến độ của phiên làm việc hiện tại
     */
    fun updateSessionProgress(sentCount: Int, remainingCustomers: List<Customer>) {
        try {
            val session = getActiveSession() ?: return
            session.sentCount = sentCount
            session.remainingCustomers = remainingCustomers
            session.lastUpdateTime = System.currentTimeMillis()
            saveActiveSession(session)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating session progress", e)
        }
    }
    
    /**
     * Đánh dấu khách hàng gửi SMS THÀNH CÔNG - XÓA khỏi session cache
     * Chỉ lưu lại những khách hàng THẤT BẠI trong session để khôi phục
     */
    fun markCustomerProcessed(customerId: String) {
        try {
            val session = getActiveSession() ?: return
            // Tìm khách hàng trong danh sách còn lại
            val customer = session.remainingCustomers.find { it.id == customerId }
            if (customer != null) {
                // XÓA khách hàng khỏi remainingCustomers vì đã gửi THÀNH CÔNG
                session.remainingCustomers = session.remainingCustomers.filter { it.id != customerId }
                
                // Thêm vào danh sách đã hoàn thành 
                session.completedCustomers.add(customer)
                session.sentCount = session.completedCustomers.size
                session.lastUpdateTime = System.currentTimeMillis()
                saveActiveSession(session)
                
                Log.d(TAG, "✅ Customer $customerId gửi THÀNH CÔNG - ĐÃ XÓA khỏi session cache")
            } else {
                Log.d(TAG, "Customer $customerId not found in remaining customers list")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking customer as processed", e)
        }
    }
    
    /**
     * Đánh dấu phiên làm việc thất bại với một khách hàng cụ thể
     */
    fun markSessionFailed(customerId: String, reason: String) {
        try {
            val session = getActiveSession() ?: return
            session.status = "Thất bại"
            session.failedReason = reason
            session.failedCustomerId = customerId
            session.lastUpdateTime = System.currentTimeMillis()
            saveActiveSession(session)
            
            // Thêm phiên làm việc vào lịch sử ngay cả khi thất bại
            addToHistory(session)
            
            Log.d(TAG, "Marked session as failed: ${session.sessionId} - Reason: $reason")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking session as failed", e)
        }
    }
    
    /**
     * Cập nhật thời gian hoạt động của phiên làm việc hiện tại
     */
    fun updateSessionTime() {
        try {
            val session = getActiveSession() ?: return
            session.lastUpdateTime = System.currentTimeMillis()
            saveActiveSession(session)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating session time", e)
        }
    }
    
    /**
     * Thêm phiên làm việc vào lịch sử
     */
    private fun addToHistory(session: SmsSession) {
        try {
            val history = getSessionHistory().toMutableList()
            history.add(0, session)
            
            // Giới hạn số lượng phiên làm việc trong lịch sử
            val limitedHistory = history.take(MAX_HISTORY_ITEMS)
            
            // Lưu lịch sử
            val historyJson = gson.toJson(limitedHistory)
            sharedPreferences.edit().putString(SESSION_HISTORY_KEY, historyJson).apply()
            
            Log.d(TAG, "Added session to history: ${session.sessionId} - ${session.sessionName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding session to history", e)
        }
    }
    
    /**
     * Hoàn thành phiên làm việc hiện tại
     */
    fun completeSession() {
        try {
            val session = getActiveSession() ?: return
            
            // Đánh dấu phiên làm việc đã hoàn thành
            session.status = "Hoàn thành"
            session.lastUpdateTime = System.currentTimeMillis()
            
            // Thêm phiên làm việc vào lịch sử
            addToHistory(session)
            
            // Xóa phiên làm việc hiện tại
            clearActiveSession()
            
            Log.d(TAG, "Completed session: ${session.sessionId} - ${session.sessionName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error completing session", e)
        }
    }
    
    /**
     * Xóa phiên làm việc hiện tại
     */
    fun clearActiveSession() {
        sharedPreferences.edit().remove(ACTIVE_SESSION_KEY).apply()
        Log.d(TAG, "Cleared active session")
    }
    
    /**
     * Lấy phiên làm việc hiện tại
     */
    fun getActiveSession(): SmsSession? {
        try {
            val sessionJson = sharedPreferences.getString(ACTIVE_SESSION_KEY, null) ?: return null
            return gson.fromJson(sessionJson, SmsSession::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active session", e)
            return null
        }
    }
    
    /**
     * Kiểm tra xem có phiên làm việc nào đang hoạt động không
     */
    fun hasActiveSession(): Boolean {
        return getActiveSession() != null
    }
    
    /**
     * Lấy lịch sử các phiên làm việc
     */
    fun getSessionHistory(): List<SmsSession> {
        try {
            val historyJson = sharedPreferences.getString(SESSION_HISTORY_KEY, null) ?: return emptyList()
            val type = object : TypeToken<List<SmsSession>>() {}.type
            return gson.fromJson(historyJson, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting session history", e)
            return emptyList()
        }
    }
    
    /**
     * Lấy thông tin tóm tắt của phiên làm việc
     */
    fun getSessionSummary(session: SmsSession): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val startDate = dateFormat.format(Date(session.startTime))
        
        // Tính tổng số khách hàng (đã gửi + còn lại)
        val totalCustomers = session.completedCustomers.size + session.remainingCustomers.size
        val progress = "${session.sentCount}/$totalCustomers"
        
        val statusText = when (session.status) {
            "Hoàn thành" -> "✅ Hoàn thành"
            "Thất bại" -> "❌ Thất bại"
            else -> "🔄 Đang xử lý"
        }
        
        // Thay thế tên phiên null bằng ngày giờ
        val displayName = if (session.sessionName == null || session.sessionName.isEmpty() || session.sessionName.startsWith("null")) {
            "Phiên $startDate"
        } else {
            session.sessionName
        }
        
        return "$displayName - $statusText - Đã gửi: $progress"
    }
    
    /**
     * Khôi phục danh sách khách hàng từ một phiên làm việc cụ thể
     * CHỈ trả về những khách hàng THẤT BẠI (chưa gửi thành công)
     */
    fun restoreCustomersFromSession(sessionId: String): List<Customer> {
        try {
            Log.d(TAG, "Bắt đầu khôi phục khách hàng THẤT BẠI từ session ID: $sessionId")
            
            // Tìm phiên làm việc trong lịch sử
            val session = getSessionHistory().find { it.sessionId == sessionId }
            
            if (session == null) {
                Log.e(TAG, "Không tìm thấy session với ID: $sessionId")
                return emptyList()
            }
            
            // CHỈ lấy khách hàng còn lại (THẤT BẠI) - không lấy những khách hàng đã thành công
            val failedCustomers = session.remainingCustomers
            
            Log.d(TAG, "Đã tìm thấy session: ${session.sessionName}")
            Log.d(TAG, "Số khách hàng THẤT BẠI: ${failedCustomers.size} (Đã thành công: ${session.completedCustomers.size})")
            
            Log.d(TAG, "✅ Khôi phục ${failedCustomers.size} khách hàng THẤT BẠI từ phiên ${session.status}")
            return failedCustomers
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi khôi phục khách hàng từ session: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * Xóa một phiên làm việc cụ thể khỏi lịch sử
     */
    fun deleteSessionFromHistory(sessionId: String): Boolean {
        try {
            // Lấy danh sách lịch sử hiện tại
            val history = getSessionHistory().toMutableList()
            
            // Tìm và xóa phiên làm việc
            val initialSize = history.size
            history.removeAll { it.sessionId == sessionId }
            
            // Kiểm tra xem có xóa được không
            if (history.size < initialSize) {
                // Lưu lại lịch sử mới
                val historyJson = gson.toJson(history)
                sharedPreferences.edit().putString(SESSION_HISTORY_KEY, historyJson).apply()
                Log.d(TAG, "Deleted session $sessionId from history")
                return true
            }
            
            Log.d(TAG, "Session $sessionId not found in history")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting session from history", e)
            return false
        }
    }
    
    /**
     * Xóa tất cả lịch sử phiên làm việc
     */
    fun clearAllSessionHistory(): Boolean {
        try {
            // Xóa toàn bộ lịch sử
            sharedPreferences.edit().remove(SESSION_HISTORY_KEY).apply()
            Log.d(TAG, "Cleared all session history")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all session history", e)
            return false
        }
    }
} 