# 🚀 Hướng dẫn cập nhật app qua Firebase

## 📋 Mục lục
1. [Cấu hình Firebase](#1-cấu-hình-firebase)
2. [Cấu hình Realtime Database](#2-cấu-hình-realtime-database)
3. [Cấu hình Storage](#3-cấu-hình-storage)
4. [Upload APK mới](#4-upload-apk-mới)
5. [Cập nhật thông tin version](#5-cập-nhật-thông-tin-version)
6. [Kiểm tra tính năng](#6-kiểm-tra-tính-năng)

---

## 1. Cấu hình Firebase

### Bước 1: Tạo Firebase Project
1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Nhấn "Add project" hoặc "Create a project"
3. Nhập tên project (ví dụ: `sms-app-updates`)
4. Chọn `Continue`
5. Tắt Google Analytics (không cần thiết cho tính năng này)
6. Nhấn `Create project`

### Bước 2: Thêm Android App
1. Trong Firebase Console, nhấn vào biểu tượng Android
2. Nhập package name: `com.example.sms_app`
3. Nhập App nickname: `SMS App`
4. Nhấn `Register app`

### Bước 3: Tải config file
1. Tải file `google-services.json`
2. Thay thế file `app/google-services.json` hiện tại bằng file vừa tải
3. Nhấn `Next` và `Continue to console`

---

## 2. Cấu hình Realtime Database

### Bước 1: Tạo Database
1. Trong Firebase Console, chọn `Realtime Database`
2. Nhấn `Create Database`
3. Chọn location (ví dụ: `asia-southeast1`)
4. Chọn `Start in test mode` (tạm thời)
5. Nhấn `Done`

### Bước 2: Cấu hình Security Rules
Vào tab `Rules` và thay đổi rules thành:

```json
{
  "rules": {
    "app_update": {
      ".read": true,
      ".write": true
    }
  }
}
```

⚠️ **Lưu ý**: Để bảo mật hơn, bạn có thể chỉ cho phép read public và write cần authentication.

### Bước 3: Tạo cấu trúc dữ liệu
1. Vào tab `Data`
2. Nhấn vào biểu tượng `+` bên cạnh URL database
3. Tạo node `app_update` với cấu trúc sau:

```json
{
  "app_update": {
    "versionCode": 2,
    "versionName": "1.1.0",
    "downloadUrl": "sms_app_v1.1.0.apk",
    "releaseNotes": "- Thêm tính năng cập nhật tự động\n- Sửa lỗi gửi SMS\n- Cải thiện giao diện",
    "forceUpdate": false,
    "minSupportedVersion": 1
  }
}
```

---

## 3. Cấu hình Storage

### Bước 1: Tạo Storage
1. Trong Firebase Console, chọn `Storage`
2. Nhấn `Get started`
3. Chọn `Start in test mode`
4. Chọn location (giống với Database)
5. Nhấn `Done`

### Bước 2: Cấu hình Security Rules
Vào tab `Rules` và thay đổi rules thành:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /app_releases/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

### Bước 3: Tạo thư mục
1. Vào tab `Files`
2. Nhấn `Create folder`
3. Tạo thư mục tên `app_releases`

---

## 4. Upload APK mới

### Bước 1: Build APK
```bash
# Trong thư mục project
./gradlew assembleRelease

# APK sẽ được tạo tại:
# app/build/outputs/apk/release/app-release.apk
```

### Bước 2: Upload lên Firebase Storage
1. Vào Firebase Console → Storage
2. Mở thư mục `app_releases`
3. Nhấn `Upload file`
4. Chọn file APK vừa build
5. Đặt tên file theo format: `sms_app_v[VERSION].apk`
   - Ví dụ: `sms_app_v1.1.0.apk`

### Bước 3: Lấy Download URL
1. Sau khi upload xong, nhấn vào file
2. Copy `Download URL`
3. Lưu lại để cập nhật Database

---

## 5. Cập nhật thông tin version

### Bước 1: Cập nhật version code trong app
Sửa file `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.example.sms_app"
    minSdk = 26
    targetSdk = 34
    versionCode = 2  // Tăng số này lên
    versionName = "1.1.0"  // Cập nhật version name
    // ...
}
```

### Bước 2: Cập nhật Database
Vào Firebase Console → Realtime Database → Data:

```json
{
  "app_update": {
    "versionCode": 2,  // Phải lớn hơn version hiện tại
    "versionName": "1.1.0",
    "downloadUrl": "sms_app_v1.1.0.apk",  // Tên file APK đã upload
    "releaseNotes": "- Thêm tính năng mới\n- Sửa lỗi\n- Cải thiện hiệu suất",
    "forceUpdate": false,  // true nếu bắt buộc cập nhật
    "minSupportedVersion": 1  // Version code thấp nhất được hỗ trợ
  }
}
```

### Giải thích các trường:
- `versionCode`: Số version code (integer), phải lớn hơn version hiện tại
- `versionName`: Tên version hiển thị (string)
- `downloadUrl`: Tên file APK trong Storage (không phải full URL)
- `releaseNotes`: Ghi chú phiên bản mới
- `forceUpdate`: `true` = bắt buộc cập nhật, `false` = tùy chọn
- `minSupportedVersion`: Version code thấp nhất vẫn được hỗ trợ

---

## 6. Kiểm tra tính năng

### Bước 1: Test với app cũ
1. Cài đặt app với version code cũ (1)
2. Mở app và vào menu "Thông tin" → "Cập nhật"
3. Hoặc đợi app tự động kiểm tra khi khởi động

### Bước 2: Kiểm tra flow cập nhật
1. **Kiểm tra dialog**: Dialog cập nhật có hiển thị không?
2. **Kiểm tra thông tin**: Version hiện tại và version mới có đúng không?
3. **Kiểm tra download**: APK có download được không?
4. **Kiểm tra install**: App có cài đặt được không?

### Bước 3: Debug (nếu cần)
Check logs trong Android Studio:
```
// Lọc logs với tag:
AppUpdateManager
UpdateViewModel
MainScreen
```

---

## 📱 Cách sử dụng tính năng Update

### Tự động kiểm tra:
- App sẽ tự động kiểm tra update mỗi khi khởi động
- Nếu có update, dialog sẽ hiển thị

### Kiểm tra thủ công:
1. Mở app
2. Nhấn vào menu "⋮" (3 chấm dọc) ở bottom bar
3. Chọn "Cập nhật"
4. Nếu có update, dialog sẽ hiển thị

### Update process:
1. **Dialog hiển thị**: Thông tin version mới, release notes
2. **Nhấn "Cập nhật"**: Bắt đầu download APK
3. **Download progress**: Hiển thị tiến độ download
4. **Auto install**: Hệ thống sẽ mở dialog cài đặt APK
5. **Install**: User nhấn "Install" để cài đặt version mới

---

## 🔧 Troubleshooting

### Lỗi "No update available"
- Kiểm tra `versionCode` trong Database > version code hiện tại
- Kiểm tra connection internet
- Kiểm tra Firebase configuration

### Lỗi download APK
- Kiểm tra file APK có tồn tại trong Storage không
- Kiểm tra `downloadUrl` trong Database
- Kiểm tra Storage rules

### Lỗi install APK
- Kiểm tra permission `REQUEST_INSTALL_PACKAGES`
- Kiểm tra FileProvider configuration
- Kiểm tra APK file có bị corrupt không

### Lỗi "Permission denied"
- Kiểm tra Database rules
- Kiểm tra Storage rules
- Kiểm tra Firebase authentication (nếu dùng)

---

## 🔒 Security Best Practices

### 1. Bảo mật Database
```json
{
  "rules": {
    "app_update": {
      ".read": true,
      ".write": "auth != null"  // Chỉ user đã login mới write được
    }
  }
}
```

### 2. Bảo mật Storage
```javascript
match /app_releases/{allPaths=**} {
  allow read: if true;
  allow write: if request.auth != null;  // Chỉ user đã login mới upload được
}
```

### 3. Kiểm tra APK signature
Trong production, nên thêm kiểm tra signature của APK trước khi install.

---

## 📈 Analytics & Monitoring

### Track update events
Bạn có thể thêm Firebase Analytics để track:
- Số lần kiểm tra update
- Số lần download thành công
- Số lần install thành công
- Số lần user bỏ qua update

### Monitor Firebase usage
- Theo dõi Storage usage
- Theo dõi Database reads/writes
- Theo dõi bandwidth usage

---

## 🎯 Tips & Best Practices

### 1. Version naming
- Dùng semantic versioning: `1.0.0`, `1.1.0`, `2.0.0`
- Version code tăng dần: 1, 2, 3, 4...

### 2. Release notes
- Viết ngắn gọn, dễ hiểu
- Highlight các tính năng mới
- Đề cập đến bug fixes

### 3. Testing
- Test trên nhiều device khác nhau
- Test với different Android versions
- Test với different network conditions

### 4. Rollback plan
- Giữ backup của version cũ
- Có thể rollback database config nếu cần
- Monitor crash reports sau update

---

## 📞 Support

Nếu gặp vấn đề, hãy check:
1. Firebase Console logs
2. Android Studio logs
3. Network connectivity
4. File permissions

**Chúc bạn thành công! 🎉** 