# 🔧 Troubleshooting Guide - SMS App

## 🚨 Vấn đề thường gặp và cách khắc phục

### 1. Import Excel rồi gửi SMS báo thành công ngay mà không gửi thực sự

#### ✅ **ĐÃ KHẮC PHỤC** trong version này

**Nguyên nhân:**
- Khách hàng bị duplicate khi import lại cùng file Excel
- Logic validation không chặt chẽ khi danh sách khách hàng trống
- ID khách hàng được tạo ngẫu nhiên nên mỗi lần import có ID khác nhau

**Cách khắc phục đã áp dụng:**
1. **Smart Import Logic**: Tự động phát hiện và merge khách hàng trùng lặp theo số điện thoại
2. **Consistent Customer ID**: Tạo ID dựa trên số điện thoại + tên, không còn ngẫu nhiên
3. **Enhanced Validation**: Thêm nhiều lớp validation trong quá trình gửi SMS
4. **Better Error Messages**: Thông báo lỗi rõ ràng khi không có khách hàng để gửi

**Cách test sau khi sửa:**
1. Import file Excel lần 1 → Gửi SMS → Kiểm tra thành công
2. Import lại cùng file Excel lần 2 → Gửi SMS → Phải gửi bình thường, không báo thành công giả

---

### 2. Số điện thoại Excel bắt đầu bằng 0 thay vì +84

#### ✅ **ĐÃ KHẮC PHỤC** trong version này

**Nguyên nhân:**
- Logic xử lý format số điện thoại chưa hoàn chỉnh
- Excel thường lưu số điện thoại Việt Nam dưới dạng bắt đầu bằng 0

**Cách khắc phục đã áp dụng:**
1. **Cải thiện `cleanPhoneNumber()`**: Xử lý đúng thứ tự +84 → 84 → 0
2. **Flexible Format Detection**: Tự động phát hiện và chuyển đổi các format khác nhau
3. **Vietnamese Carrier Support**: Hỗ trợ đầy đủ các đầu số nhà mạng Việt Nam

---

### 3. Lỗi Google Services Plugin

#### ✅ **ĐÃ KHẮC PHỤC** trong version này

**Lỗi:**
```
Error resolving plugin [id: 'com.google.gms.google-services', version: '4.4.2']
The request for this plugin could not be satisfied because the plugin is already on the classpath with an unknown version
```

**Cách khắc phục:**
- Thay `alias(libs.plugins.google.service.plugin)` bằng `id("com.google.gms.google-services")`
- Sử dụng ID trực tiếp thay vì alias để tránh conflict

---

### 4. Lỗi Java Version (JVM 8 vs JVM 11)

**Lỗi:**
```
Dependency requires at least JVM runtime version 11. This build uses a Java 8 JVM.
```

**Cách khắc phục:**
1. **Cập nhật Android Studio**: Đảm bảo sử dụng Android Studio mới nhất
2. **Set JAVA_HOME**: 
   ```bash
   # Windows
   set JAVA_HOME=C:\Program Files\Android\Android Studio\jre
   
   # macOS/Linux  
   export JAVA_HOME=/Applications/Android Studio.app/Contents/jre/Contents/Home
   ```
3. **Gradle Settings**: Thêm vào `gradle.properties`:
   ```properties
   org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jre
   ```

---

### 5. Vấn đề permission REQUEST_INSTALL_PACKAGES

**Triệu chứng:**
- Không thể cài đặt APK update tự động
- Android từ chối cài đặt từ nguồn không xác định

**Cách khắc phục:**
1. **Manifest đã có permission**: `REQUEST_INSTALL_PACKAGES` đã được thêm
2. **User cần enable**: Settings → Security → Install unknown apps → Enable cho app
3. **FileProvider đã setup**: `file_paths.xml` đã được cấu hình đúng

---

### 6. Firebase Setup cho tính năng Update

**Các bước cần thiết:**
1. **Tạo Firebase Project** (xem `FIREBASE_UPDATE_GUIDE.md`)
2. **Download `google-services.json`** và đặt vào `app/`
3. **Setup Realtime Database** với structure:
   ```json
   {
     "app_update": {
       "versionCode": 2,
       "versionName": "1.1.0", 
       "downloadUrl": "app_v1.1.0.apk",
       "releaseNotes": "Các tính năng mới...",
       "forceUpdate": false
     }
   }
   ```
4. **Setup Storage** và upload APK vào `app_releases/`

---

### 7. App crashes khi gửi SMS

**Debug steps:**
1. **Check Logs**: Tìm tag `SmsService`, `SendMessageViewModel` trong logcat
2. **Check Permissions**: Đảm bảo tất cả SMS permissions đã được grant
3. **Check SIM**: Đảm bảo SIM card đang hoạt động và có signal
4. **Check Phone Numbers**: Đảm bảo số điện thoại đúng format (10 chữ số, bắt đầu bằng 0)

**Common error patterns:**
```
❌ No customers selected - showing warning message
❌ CRITICAL: Selected customers list is empty after processing  
❌ Failed to send SMS using format: [phone_number]
```

---

### 8. Countdown timer không hoạt động đúng

**Nguyên nhân thường gặp:**
- App bị kill trong background
- System optimization apps tắt background services

**Cách khắc phục:**
1. **Battery Optimization**: Disable battery optimization cho app
2. **Background App Refresh**: Enable cho app trong settings
3. **Keep Screen On**: App tự động keep screen on khi đang gửi SMS

---

### 9. Build errors sau khi update dependencies

**Common solutions:**
1. **Clean Project**: `./gradlew clean`
2. **Invalidate Caches**: Android Studio → File → Invalidate Caches and Restart
3. **Delete .gradle**: Xóa thư mục `.gradle` và rebuild
4. **Update Gradle Wrapper**: Đảm bảo sử dụng Gradle version tương thích

---

### 10. Performance issues với large Excel files

**Hiện tại app hỗ trợ:**
- Auto-detect empty rows và dừng import
- Deep cleaning dữ liệu Excel để loại bỏ ký tự ẩn
- Efficient duplicate detection

**Khuyến nghị:**
- File Excel không nên vượt quá 1000 khách hàng
- Đảm bảo format chuẩn: Tên | ID | Số ĐT | Địa chỉ | Options...

---

## 📞 Support

Nếu gặp vấn đề không có trong danh sách trên:

1. **Enable Debug Logs**: Kiểm tra Android Studio Logcat
2. **Check File Formats**: Đảm bảo Excel file đúng định dạng
3. **Restart App**: Thử restart app và thử lại
4. **Clear App Data**: Settings → Apps → SMS App → Storage → Clear Data (sẽ mất dữ liệu)

**Debug commands hữu ích:**
```bash
# View app logs
adb logcat | grep -E "(SmsService|MainViewModel|ExcelImporter|SendMessageViewModel)"

# Check app permissions  
adb shell dumpsys package com.example.sms_app | grep permission

# Force stop app
adb shell am force-stop com.example.sms_app
```

---

## 🔄 Changelog

### Version 1.1 (Current)
- ✅ Fixed: Import Excel duplicate customers issue
- ✅ Fixed: Phone number format from +84 to 0 prefix  
- ✅ Fixed: Google Services plugin conflict
- ✅ Added: Smart duplicate detection on import
- ✅ Added: Consistent customer ID generation
- ✅ Added: Enhanced SMS validation
- ✅ Added: Firebase auto-update feature
- ✅ Added: Comprehensive error messages

### Version 1.0 (Previous)
- Basic SMS sending functionality
- Excel import support
- Customer management
- Template management
- Session backup 