# 🚀 Quick Firebase Setup cho App Update

## 🔥 Bước 1: Setup Firebase Realtime Database

### 1.1 Truy cập Firebase Console
1. Vào https://console.firebase.google.com/
2. Chọn project: **shop-manager-8a6b8**
3. Vào **Realtime Database** (sidebar trái)

### 1.2 Tạo Database (nếu chưa có)
1. Nhấn **"Create Database"**
2. Chọn **"Start in test mode"** (cho development)
3. Chọn location: **asia-southeast1** (Singapore)

### 1.3 Import dữ liệu
1. Trong Realtime Database, nhấn **"⋮"** → **"Import JSON"**
2. Upload file `firebase_sample_data.json` hoặc copy-paste JSON sau:

```json
{
  "app_update": {
    "versionCode": 2,
    "versionName": "1.1.0",
    "downloadUrl": "sms_app_v1.1.0.apk",
    "releaseNotes": "🚀 Cập nhật mới v1.1.0:\n\n✅ Sửa lỗi hiển thị hoàn thành SMS ngay lập tức\n✅ Cải thiện hiệu suất gửi SMS\n✅ Thêm tính năng reset progress state\n✅ Sửa lỗi ID khách hàng trùng lặp\n✅ Cải thiện UI/UX",
    "forceUpdate": false,
    "minSupportedVersion": 1
  }
}
```

### 1.4 Cấu hình Security Rules
1. Vào tab **"Rules"**
2. Thay thế rules bằng:

```javascript
{
  "rules": {
    ".read": true,
    ".write": false,
    "app_update": {
      ".read": true,
      ".write": false
    }
  }
}
```

3. Nhấn **"Publish"**

## 🗂️ Bước 2: Setup Firebase Storage (Optional)

### 2.1 Tạo Storage
1. Vào **Storage** (sidebar trái)
2. Nhấn **"Get started"**
3. Chọn **"Start in test mode"**

### 2.2 Tạo folder và upload APK
1. Tạo folder: **app_releases/**
2. Upload file APK với tên: **sms_app_v1.1.0.apk**

## 🧪 Bước 3: Test ngay lập tức

### 3.1 Test Firebase Connection
1. Mở app SMS
2. Nhấn menu **⋮** → **"Test Firebase"**
3. Xem toast message:
   - ✅ **"Firebase OK! Data: true"** = Thành công
   - ❌ **"Firebase not connected!"** = Lỗi kết nối
   - ❌ **"Firebase test error"** = Lỗi khác

### 3.2 Test Update Check
1. Thay đổi `versionCode` trong `app/build.gradle.kts`:
```kotlin
versionCode = 1  // Thay đổi thành 1 để test
```

2. Build và cài đặt app:
```bash
./gradlew assembleDebug
```

3. Mở app → Menu **⋮** → **"Cập nhật"**
4. Kết quả mong đợi:
   - Toast: **"🚀 Có cập nhật mới v1.1.0!"**
   - Hiển thị UpdateDialog

## 🐛 Troubleshooting

### Lỗi "Firebase not connected"
1. **Kiểm tra internet**: Đảm bảo device có kết nối mạng
2. **Kiểm tra google-services.json**: File có đúng trong `app/` folder không
3. **Rebuild project**: Clean và rebuild lại project

### Lỗi "Không tìm thấy thông tin cập nhật"
1. **Kiểm tra Firebase Database**: Node `app_update` có tồn tại không
2. **Kiểm tra Rules**: Đảm bảo `.read: true`
3. **Kiểm tra project ID**: Đúng project `shop-manager-8a6b8` không

### Lỗi "Permission denied"
1. **Cập nhật Security Rules**: Đảm bảo `.read: true`
2. **Kiểm tra Authentication**: Có thể cần enable Anonymous Auth

## 📱 Debug Commands

### Xem logs realtime:
```bash
adb logcat | grep -E "(UpdateViewModel|AppUpdateManager|Firebase)"
```

### Logs quan trọng cần tìm:
- `🔍 Checking for updates...`
- `🔗 Firebase connected: true`
- `📊 app_update exists: true`
- `✅ Update available!`

## ⚡ Quick Fix nếu không hoạt động

### 1. Enable Anonymous Authentication
1. Vào Firebase Console → **Authentication**
2. Tab **"Sign-in method"**
3. Enable **"Anonymous"**

### 2. Thay đổi Database Rules thành public
```javascript
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

### 3. Kiểm tra package name
- Trong `google-services.json`: `"package_name": "com.example.sms_app"`
- Trong `app/build.gradle.kts`: `applicationId = "com.example.sms_app"`

## ✅ Checklist hoàn thành

- [ ] Firebase Realtime Database đã tạo
- [ ] Dữ liệu `app_update` đã import
- [ ] Security Rules đã cấu hình
- [ ] Test Firebase connection thành công
- [ ] Test update check thành công
- [ ] App hiển thị UpdateDialog đúng

Sau khi hoàn thành checklist, chức năng update sẽ hoạt động hoàn hảo! 🎉
