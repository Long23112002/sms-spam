# 🚀 API Setup Guide cho App Update

## 📋 API Endpoints

### 1. Version Check API
**URL**: `http://42.96.16.211:6868/version`
**Method**: `GET`

**Response khi có update mới**:
```json
{
  "version": "1.1.0",
  "versionCode": 2,
  "releaseNotes": "🚀 Cập nhật mới v1.1.0:\n\n✅ Sửa lỗi hiển thị hoàn thành SMS\n✅ Cải thiện hiệu suất gửi SMS\n✅ Thêm tính năng reset progress",
  "forceUpdate": false,
  "downloadUrl": "",
  "fileSize": 25600000
}
```

**Response khi KHÔNG có update** (HTTP 404):
```json
{
  "message": "No version info found"
}
```

### 2. Download API
**URL**: `http://42.96.16.211:6868/download`
**Method**: `GET`
**Response**: Binary APK file (chỉ được gọi khi có update)

## 🔧 App Configuration

### Current Version
- **Version Name**: `1.0.0`
- **Version Code**: `1`

### API Base URL
```kotlin
private const val BASE_URL = "http://42.96.16.211:6868/"
```

## 🧪 Testing

### 1. Test Update Check (Không có update)
1. Mở app SMS
2. Menu (⋮) → **"Cập nhật"**
3. Kết quả mong đợi khi không có update:
   - ✅ **"Đã sử dụng phiên bản mới nhất"** = API trả về 404

### 2. Test Update Check (Có update)
1. Cập nhật API để trả về `versionCode > 1` (HTTP 200)
2. Menu (⋮) → **"Cập nhật"**
3. Kết quả mong đợi khi có update:
   - **"🚀 Có cập nhật mới vX.X.X!"**
   - Hiển thị UpdateDialog

### 3. Test Download
1. Nhấn **"Cập nhật"** trong dialog
2. Quan sát progress bar
3. Sau khi download xong → installer mở

## 🛡️ Error Handling

### API Lỗi → App vẫn chạy bình thường
- ⏰ **Timeout**: "Timeout khi kiểm tra cập nhật"
- 📶 **Network**: "Không thể kết nối server cập nhật"
- ⚠️ **Other**: "Không thể kiểm tra cập nhật"

### Download Lỗi
- ⏰ **Timeout**: "Timeout khi tải xuống"
- 📶 **Network**: "Lỗi kết nối khi tải xuống"
- ❌ **Other**: "Lỗi tải xuống: [error message]"

## 📱 Sample API Server (Node.js)

```javascript
const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = 6868;

// Version endpoint
app.get('/version', (req, res) => {
  // Kiểm tra xem có version mới không
  const currentVersion = 1; // Version hiện tại của app
  const latestVersion = 1;  // Version mới nhất trên server

  if (latestVersion > currentVersion) {
    // Có update mới - trả về HTTP 200
    res.json({
      version: "1.1.0",
      versionCode: 2,
      releaseNotes: "🚀 Cập nhật mới v1.1.0:\n\n✅ Sửa lỗi hiển thị hoàn thành SMS\n✅ Cải thiện hiệu suất gửi SMS\n✅ Thêm tính năng reset progress",
      forceUpdate: false,
      downloadUrl: "",
      fileSize: 25600000
    });
  } else {
    // Không có update - trả về HTTP 404
    res.status(404).json({
      message: "No version info found"
    });
  }
});

// Download endpoint
app.get('/download', (req, res) => {
  const apkPath = path.join(__dirname, 'sms_app_v1.1.0.apk');
  
  if (fs.existsSync(apkPath)) {
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="sms_app_v1.1.0.apk"');
    
    const fileStream = fs.createReadStream(apkPath);
    fileStream.pipe(res);
  } else {
    res.status(404).json({ error: 'APK file not found' });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Update server running on http://0.0.0.0:${PORT}`);
});
```

## 🚀 Deployment Steps

### 1. Build APK mới
```bash
# Tăng version trong build.gradle.kts
versionCode = 2
versionName = "1.1.0"

# Build release APK
./gradlew assembleRelease
```

### 2. Update API Server
1. Upload APK mới lên server
2. Cập nhật response của `/version` endpoint:
   - Tăng `versionCode`
   - Cập nhật `versionName`
   - Cập nhật `releaseNotes`
   - Cập nhật `fileSize`

### 3. Test Update Flow
1. Cài app với version cũ (1.0.0)
2. Test API connection
3. Test update check
4. Test download và install

## 📊 Monitoring

### Logs cần theo dõi:
```bash
adb logcat | grep -E "(UpdateViewModel|AppUpdateManager)"
```

### Key logs:
- `🔍 Checking for updates from API...`
- `📊 API response: [data]`
- `✅ Update available!`
- `🚀 Starting download from: [url]`
- `📥 Download progress: X%`
- `✅ Download completed`

## ⚡ Quick Commands

### Test API manually:
```bash
# Test version endpoint
curl -X GET "http://42.96.16.211:6868/version"

# Test download endpoint (save to file)
curl -X GET "http://42.96.16.211:6868/download" -o test_download.apk
```

### Build and install debug:
```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## ✅ Checklist

- [ ] API server đang chạy tại `http://42.96.16.211:6868`
- [ ] `/version` endpoint trả về JSON đúng format
- [ ] `/download` endpoint trả về APK file
- [ ] App version hiện tại là 1.0.0 (versionCode = 1)
- [ ] Test API connection thành công
- [ ] Test update flow hoàn chỉnh
- [ ] Error handling hoạt động đúng (app không crash khi API lỗi)
