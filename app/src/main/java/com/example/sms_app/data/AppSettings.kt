package com.example.sms_app.data

data class AppSettings(
    val intervalBetweenSmsSeconds: Int = 25, // Thời gian giữa các tin nhắn
    val maxRetryAttempts: Int = 3, // Số lần thử lại khi gửi thất bại
    val retryDelaySeconds: Int = 5, // Thời gian chờ giữa các lần thử lại
    val randomizeInterval: Boolean = false, // Thêm độ trễ ngẫu nhiên giữa các tin nhắn
    val randomizeContent: Boolean = false, // Thêm ký tự ngẫu nhiên vào nội dung tin nhắn
    val addRandomEmoji: Boolean = false, // Thêm emoji ngẫu nhiên vào tin nhắn
    val useRandomSpacing: Boolean = false, // Sử dụng khoảng cách ngẫu nhiên
    val minIntervalSeconds: Int = 20, // Thời gian tối thiểu giữa các tin nhắn
    val maxIntervalSeconds: Int = 35, // Thời gian tối đa giữa các tin nhắn

    val enableVibrate: Boolean = false,
    val enableSound: Boolean = false,
    val enableFilter: Boolean = false,
    val isRandomNumber: Boolean = false,
    val isLimitCustomer: Boolean = false,
    val customerLimit: Int = 20, // Giới hạn số khách hàng (mặc định 20)
    val enableUpdate: Boolean = false,
)