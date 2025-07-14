# 🔥 Firebase App Update Setup Guide

## 📋 Cấu hình Firebase Realtime Database

### 1. Cấu trúc dữ liệu trong Firebase Realtime Database

Truy cập Firebase Console → Realtime Database và tạo cấu trúc sau:

```json
{
  "app_update": {
    "versionCode": 2,
    "versionName": "1.1.0",
    "downloadUrl": "sms_app_v1.1.0.apk",
    "releaseNotes": "🚀 Cập nhật mới:\n• Sửa lỗi hiển thị hoàn thành SMS ngay lập tức\n• Cải thiện hiệu suất gửi SMS\n• Thêm tính năng reset progress\n• Sửa lỗi ID khách hàng trùng lặp\n• Cải thiện UI/UX",
    "forceUpdate": false,
    "minSupportedVersion": 1
  }
}
```

### 2. Cấu hình Firebase Storage

1. Truy cập Firebase Console → Storage
2. Tạo folder `app_releases/`
3. Upload file APK mới với tên tương ứng với `downloadUrl` trong database

### 3. Quy trình cập nhật app

#### Bước 1: Build APK mới
```bash
./gradlew assembleRelease
```

#### Bước 2: Upload APK lên Firebase Storage
- Vào Firebase Console → Storage
- Upload file APK vào folder `app_releases/`
- Đặt tên file theo format: `sms_app_v{version}.apk`

#### Bước 3: Cập nhật thông tin trong Realtime Database
```json
{
  "app_update": {
    "versionCode": 3,  // Tăng version code
    "versionName": "1.2.0",  // Cập nhật version name
    "downloadUrl": "sms_app_v1.2.0.apk",  // Tên file APK
    "releaseNotes": "Nội dung cập nhật...",
    "forceUpdate": false,  // true nếu bắt buộc cập nhật
    "minSupportedVersion": 1
  }
}
```

### 4. Cách sử dụng trong app

1. Người dùng mở app → nhấn menu (⋮) → chọn "Cập nhật"
2. App sẽ kiểm tra Firebase Realtime Database
3. Nếu có phiên bản mới → hiển thị dialog cập nhật
4. Người dùng nhấn "Cập nhật" → tải xuống từ Firebase Storage
5. Sau khi tải xong → tự động mở installer

### 5. Tính năng

✅ **Kiểm tra cập nhật tự động** từ Firebase
✅ **Progress bar** hiển thị tiến độ tải xuống
✅ **Force update** - bắt buộc cập nhật nếu cần
✅ **Release notes** - hiển thị nội dung cập nhật
✅ **Version comparison** - so sánh version hiện tại với version mới
✅ **Error handling** - xử lý lỗi khi tải xuống hoặc cài đặt

### 6. Permissions cần thiết

App đã có các permissions sau trong AndroidManifest.xml:
- `INTERNET` - để kết nối Firebase
- `REQUEST_INSTALL_PACKAGES` - để cài đặt APK
- `WRITE_EXTERNAL_STORAGE` - để lưu file APK

### 7. Testing

1. Thay đổi `versionCode` trong `build.gradle.kts` thành số nhỏ hơn
2. Build và cài đặt app
3. Cập nhật Firebase với version code cao hơn
4. Test chức năng cập nhật

### 8. Security Rules cho Firebase

```javascript
{
  "rules": {
    "app_update": {
      ".read": true,
      ".write": false
    }
  }
}
```

### 9. Troubleshooting

**Lỗi "Không tìm thấy thông tin cập nhật":**
- Kiểm tra Firebase Realtime Database có dữ liệu `app_update`
- Kiểm tra internet connection

**Lỗi tải xuống:**
- Kiểm tra file APK có tồn tại trong Firebase Storage
- Kiểm tra tên file trong `downloadUrl` có đúng không

**Lỗi cài đặt:**
- Kiểm tra permission `REQUEST_INSTALL_PACKAGES`
- Kiểm tra FileProvider configuration
