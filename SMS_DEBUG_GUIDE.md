# Hướng dẫn Debug lỗi gửi SMS

## 🔍 Tổng quan về các cải thiện đã thực hiện

### 1. Cải thiện logging và debug

- ✅ Thêm emoji và ký hiệu để dễ dàng theo dõi log
- ✅ Logging chi tiết cho từng bước gửi SMS
- ✅ Thêm thông tin về template, settings, và device check
- ✅ Tracking delivery report với timeout 30 giây

### 2. Sử dụng phương thức gửi SMS tốt hơn

- ✅ Chuyển từ `sendSmsWithRetry()` sang `sendSmsWithDeliveryReport()`
- ✅ Có delivery report để biết chính xác trạng thái gửi SMS
- ✅ Xử lý multipart SMS một cách chính xác
- ✅ Timeout và retry mechanism

### 3. Kiểm tra thiết bị trước khi gửi SMS

- ✅ Kiểm tra quyền SMS
- ✅ Kiểm tra trạng thái SIM card
- ✅ Kiểm tra airplane mode
- ✅ Kiểm tra trạng thái mạng
- ✅ Kiểm tra SmsManager availability

## 📱 Cách kiểm tra lỗi gửi SMS

### Bước 1: Kiểm tra Log

Sử dụng Android Studio Logcat hoặc adb để xem log:

```bash
adb logcat | grep -E "SmsService|SMS|🚀|✅|❌|📤|💥"
```

### Bước 2: Các log quan trọng cần chú ý

#### 🔍 Log kiểm tra broadcast receiver:

```
📡 Broadcast receiver registered
📡 Received broadcast: com.example.sms_app.ACTION_PROGRESS_UPDATE
📊 Progress update: 1/5 - ✅ Đã gửi Nguyễn Văn A (0123456789)
📡 Received broadcast: com.example.sms_app.ACTION_SMS_COMPLETED
🏁 SMS completed: 🏁 Đã hoàn thành gửi 5/5 tin nhắn
```

#### ✅ Log thành công:

```
🔍 Device check result: ✅ Thiết bị sẵn sàng gửi SMS
🚀 Starting SMS sending: 5 messages to send
📋 Template: Template 1
⚙️ Settings: interval=25s, maxRetry=3, retryDelay=10s
🚀 Attempting to send SMS to Nguyễn Văn A (0123456789)
📝 Message content: Xin chào Nguyễn Văn A, đây là tin nhắn test...
📤 Sending SMS to 0123456789 (requestId: SMS_1234567890_123456)
✅ SMS sent successfully to Nguyễn Văn A (0123456789)
⏳ Waiting 25000ms before next SMS...
🏁 SMS sending completed: 5/5 messages sent
```

#### ❌ Log lỗi:

```
❌ Device check failed, stopping service
❌ Thiếu quyền SMS
❌ Không có SIM card
❌ Chế độ máy bay đang bật
❌ SMS send failed: No service (requestId: SMS_1234567890_123456)
❌ Exception during SMS sending to 0123456789: SecurityException
⏰ SMS timeout after 30000ms (requestId: SMS_1234567890_123456)
```

### Bước 3: Các lỗi thường gặp và cách khắc phục

#### 1. Lỗi quyền SMS

**Triệu chứng:** `❌ Thiếu quyền SMS`
**Khắc phục:**

- Vào Settings > Apps > SMS App > Permissions
- Bật quyền SMS, Phone, Storage

#### 2. Lỗi SIM card

**Triệu chứng:** `❌ Không có SIM card` hoặc `❌ SIM bị khóa mạng`
**Khắc phục:**

- Kiểm tra SIM card đã được lắp đúng
- Nhập PIN nếu cần
- Kiểm tra SIM có bị khóa mạng không

#### 3. Lỗi airplane mode

**Triệu chứng:** `❌ Chế độ máy bay đang bật`
**Khắc phục:**

- Tắt airplane mode trong Settings

#### 4. Lỗi mạng

**Triệu chứng:** `❌ SMS send failed: No service`
**Khắc phục:**

- Kiểm tra tín hiệu mạng
- Thử chuyển từ 4G sang 3G/2G
- Khởi động lại điện thoại

#### 5. Lỗi timeout

**Triệu chứng:** `⏰ SMS timeout after 30000ms`
**Khắc phục:**

- Mạng yếu, thử lại sau
- Kiểm tra cài đặt APN
- Liên hệ nhà mạng

#### 6. Lỗi SecurityException

**Triệu chứng:** `❌ Exception during SMS sending: SecurityException`
**Khắc phục:**

- Cấp lại quyền SMS
- Khởi động lại ứng dụng
- Kiểm tra app có bị restrict không

#### 7. UI bị treo không cập nhật

**Triệu chứng:** UI hiển thị "Đang gửi SMS..." mãi không thay đổi
**Khắc phục:**

- Kiểm tra log broadcast receiver: `📡 Received broadcast`
- Nếu không có broadcast, service có thể bị crash
- UI sẽ tự động timeout sau 15 phút
- Service sẽ tự động timeout sau 10 phút

#### 8. Lỗi broadcast receiver

**Triệu chứng:** Không thấy log `📡 Received broadcast`
**Khắc phục:**

- Kiểm tra log `📡 Broadcast receiver registered`
- Restart ứng dụng
- Kiểm tra service có gửi broadcast không

## 🛠️ Các cải thiện kỹ thuật

### 1. Delivery Report System

- Sử dụng PendingIntent để nhận delivery report
- Timeout 30 giây cho mỗi SMS
- Tracking multipart SMS với part ID

### 2. Error Handling

- Try-catch cho tất cả SMS operations
- Fallback mechanism khi sendMultipartTextMessage fail
- Detailed error logging với error codes

### 3. Device Compatibility

- Kiểm tra Android version cho PendingIntent flags
- Support cả SIM đơn và SIM kép
- Fallback to default SIM khi có lỗi

### 4. Performance Optimization

- Coroutine-based SMS sending
- Proper cancellation handling
- Memory leak prevention

## 📋 Checklist debug

Khi gặp lỗi gửi SMS, hãy kiểm tra theo thứ tự:

1. ☐ Quyền SMS đã được cấp?
2. ☐ SIM card hoạt động bình thường?
3. ☐ Airplane mode đã tắt?
4. ☐ Có tín hiệu mạng?
5. ☐ Template có nội dung?
6. ☐ Số điện thoại hợp lệ?
7. ☐ Ứng dụng có bị restrict?
8. ☐ Log có hiển thị lỗi cụ thể?

## 🔧 File APK đã cải thiện

APK release mới đã được build với các cải thiện:

- **Đường dẫn:** `app/build/outputs/apk/release/app-release.apk`
- **Kích thước:** ~14.6 MB
- **Thời gian build:** 27/06/2025 12:38
- **Tính năng mới:**
  - ✅ Enhanced logging với emoji
  - ✅ Device check trước khi gửi SMS
  - ✅ Delivery report với timeout
  - ✅ UI timeout fallback (15 phút)
  - ✅ Service timeout (10 phút)
  - ✅ Cải thiện xin quyền SMS
  - ✅ Broadcast receiver logging
  - ✅ Try-catch toàn diện

## 📞 Hỗ trợ

Nếu vẫn gặp lỗi sau khi kiểm tra, hãy cung cấp:

1. Log đầy đủ từ Logcat
2. Thông tin thiết bị (Android version, SIM type)
3. Mô tả chi tiết lỗi
4. Các bước đã thử
