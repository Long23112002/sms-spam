# 🧪 Test App Update Feature

## 📋 Cách test chức năng cập nhật

### 1. Setup Firebase (Lần đầu)

1. **Truy cập Firebase Console**: https://console.firebase.google.com/
2. **Chọn project**: `shop-manager-8a6b8`
3. **Vào Realtime Database**:
   - Nếu chưa có, tạo database mới
   - Import dữ liệu từ file `firebase_sample_data.json`
4. **Vào Storage**:
   - Tạo folder `app_releases/`
   - Upload file APK test (có thể dùng APK hiện tại với tên `sms_app_v1.1.0.apk`)

### 2. Test Scenario 1: App đã là phiên bản mới nhất

```bash
# Đảm bảo versionCode trong build.gradle.kts >= 2
versionCode = 2
```

**Kết quả mong đợi**: 
- Toast: "✅ Đã sử dụng phiên bản mới nhất"
- Không hiển thị dialog update

### 3. Test Scenario 2: Có phiên bản mới

```bash
# Thay đổi versionCode trong build.gradle.kts thành 1
versionCode = 1
```

**Kết quả mong đợi**:
- Toast: "🚀 Có cập nhật mới v1.1.0!"
- Hiển thị UpdateDialog với thông tin chi tiết

### 4. Test Scenario 3: Test download và install

1. Đảm bảo có file APK trong Firebase Storage
2. Nhấn "Cập nhật" trong dialog
3. Quan sát progress bar
4. Sau khi download xong, installer sẽ mở

### 5. Test Scenario 4: Force Update

```json
{
  "app_update": {
    "versionCode": 3,
    "versionName": "1.2.0",
    "downloadUrl": "sms_app_v1.2.0.apk",
    "releaseNotes": "Cập nhật bắt buộc!",
    "forceUpdate": true,
    "minSupportedVersion": 2
  }
}
```

**Kết quả mong đợi**:
- Không có nút "Để sau"
- Không thể đóng dialog bằng cách nhấn outside

### 6. Test Error Scenarios

#### 6.1 Không có internet
- Tắt wifi/mobile data
- Nhấn "Cập nhật"
- **Kết quả**: Toast lỗi kết nối mạng

#### 6.2 File APK không tồn tại
- Thay đổi `downloadUrl` thành file không tồn tại
- **Kết quả**: Toast lỗi tải xuống

#### 6.3 Firebase data không tồn tại
- Xóa node `app_update` trong Firebase
- **Kết quả**: Toast "Không tìm thấy thông tin cập nhật"

### 7. Test UI Flow

1. **Mở app** → **Menu (⋮)** → **"Cập nhật"**
2. **Kiểm tra toast** "Đang kiểm tra cập nhật..."
3. **Nếu có update**: Dialog hiển thị với:
   - Icon update
   - Tiêu đề "🚀 Cập nhật mới!"
   - Thông tin version hiện tại vs mới
   - Release notes
   - Progress bar khi download
   - Nút "Cập nhật" / "Để sau"

### 8. Test Permissions

#### Android 8.0+ (API 26+)
- App sẽ kiểm tra `REQUEST_INSTALL_PACKAGES` permission
- Nếu chưa có, sẽ mở Settings để user cấp quyền

#### Android 7.0 và thấp hơn
- Không cần permission đặc biệt

### 9. Debugging

#### Kiểm tra logs:
```bash
adb logcat | grep -E "(UpdateViewModel|AppUpdateManager)"
```

#### Logs quan trọng:
- `🔍 Checking for updates...`
- `📱 Current version: x.x.x (x)`
- `🆕 Latest version: x.x.x (x)`
- `✅ Update available!` hoặc `✅ App is up to date`
- `🚀 Starting update download...`
- `📥 Download progress: x%`
- `✅ Update download successful`

### 10. Build và Deploy Update

```bash
# 1. Tăng version trong build.gradle.kts
versionCode = 3
versionName = "1.2.0"

# 2. Build release APK
./gradlew assembleRelease

# 3. Upload APK lên Firebase Storage
# Tên file: sms_app_v1.2.0.apk

# 4. Cập nhật Firebase Realtime Database
{
  "app_update": {
    "versionCode": 3,
    "versionName": "1.2.0",
    "downloadUrl": "sms_app_v1.2.0.apk",
    "releaseNotes": "Nội dung cập nhật...",
    "forceUpdate": false,
    "minSupportedVersion": 1
  }
}
```

### 11. Checklist trước khi release

- [ ] Firebase Realtime Database có dữ liệu `app_update`
- [ ] Firebase Storage có file APK tương ứng
- [ ] Version code trong Firebase > version code hiện tại
- [ ] Release notes đã được viết đầy đủ
- [ ] Test trên device thật
- [ ] Test cả force update và normal update
- [ ] Test error scenarios
