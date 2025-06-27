package com.example.sms_app.data

data class AppSettings(
    val intervalBetweenSmsSeconds: Int = 25, // Thời gian giữa các tin nhắn
    val maxRetryAttempts: Int = 3, // Số lần thử lại khi gửi thất bại
    val retryDelaySeconds: Int = 5, // Thời gian chờ giữa các lần thử lại
    val randomizeInterval: Boolean = true, // Thêm độ trễ ngẫu nhiên giữa các tin nhắn
    val randomizeContent: Boolean = true, // Thêm ký tự ngẫu nhiên vào nội dung tin nhắn
    val addRandomEmoji: Boolean = false, // Thêm emoji ngẫu nhiên vào tin nhắn
    val useRandomSpacing: Boolean = true, // Sử dụng khoảng cách ngẫu nhiên
    val minIntervalSeconds: Int = 20, // Thời gian tối thiểu giữa các tin nhắn
    val maxIntervalSeconds: Int = 35 // Thời gian tối đa giữa các tin nhắn
) 