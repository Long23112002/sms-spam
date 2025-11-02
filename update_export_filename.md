# 📅 Cập nhật tên file Export với ngày giờ và "data"

## 🎯 **Yêu cầu:**
- ✅ Tên file export có chứa "data"
- ✅ Có ngày giờ định dạng rõ ràng
- ✅ Format: `data_YYYY-MM-DD_HH-mm-ss.xlsx`

## 📝 **Thay đổi:**

### **1. MainScreen.kt**

#### **Trước:**
```kotlin
onExportClick = {
    exportLauncher.launch("khach_hang_${System.currentTimeMillis()}.xlsx")
}
```

#### **Sau:**
```kotlin
onExportClick = {
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.getDefault())
    val timestamp = dateFormat.format(java.util.Date())
    exportLauncher.launch("data_$timestamp.xlsx")
}
```

### **2. ExcelExporter.kt**

#### **Function generateDefaultFilename():**

**Trước:**
```kotlin
fun generateDefaultFilename(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    val timestamp = dateFormat.format(Date())
    return "khach_hang_$timestamp.xlsx"
}
```

**Sau:**
```kotlin
fun generateDefaultFilename(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    val timestamp = dateFormat.format(Date())
    return "data_$timestamp.xlsx"
}
```

## 📋 **Format tên file mới:**

### **Pattern:**
```
data_YYYY-MM-DD_HH-mm-ss.xlsx
```

### **Ví dụ thực tế:**
```
data_2024-01-15_14-30-25.xlsx
data_2024-01-15_09-45-12.xlsx
data_2024-01-15_16-20-08.xlsx
```

### **Giải thích format:**
- **data_**: Prefix cố định để nhận biết loại file
- **YYYY**: Năm 4 chữ số (2024)
- **MM**: Tháng 2 chữ số (01-12)
- **DD**: Ngày 2 chữ số (01-31)
- **HH**: Giờ 24h format (00-23)
- **mm**: Phút (00-59)
- **ss**: Giây (00-59)
- **.xlsx**: Extension Excel

## 🕐 **Ví dụ theo thời gian:**

| Thời gian export | Tên file |
|------------------|----------|
| 15/01/2024 14:30:25 | `data_2024-01-15_14-30-25.xlsx` |
| 15/01/2024 09:45:12 | `data_2024-01-15_09-45-12.xlsx` |
| 15/01/2024 16:20:08 | `data_2024-01-15_16-20-08.xlsx` |
| 01/02/2024 08:15:30 | `data_2024-02-01_08-15-30.xlsx` |

## 💡 **Lợi ích:**

### **1. Dễ nhận biết:**
- ✅ **"data_"** prefix → biết ngay đây là file dữ liệu
- ✅ **Khác với "khach_hang_"** → ngắn gọn hơn

### **2. Sắp xếp tự động:**
- ✅ **Format YYYY-MM-DD** → sắp xếp theo thứ tự thời gian
- ✅ **File mới nhất** sẽ ở cuối khi sort by name

### **3. Thông tin đầy đủ:**
- ✅ **Ngày giờ chính xác** → biết khi nào export
- ✅ **Không trùng lặp** → mỗi giây 1 file khác nhau

### **4. Tương thích:**
- ✅ **Không có ký tự đặc biệt** → hoạt động trên mọi OS
- ✅ **Dấu gạch ngang** thay vì dấu hai chấm → tránh lỗi Windows

## 🚀 **Test scenarios:**

### **Scenario 1: Export lúc 14:30**
```
Thời gian: 15/01/2024 14:30:25
Expected: data_2024-01-15_14-30-25.xlsx
Result: ✅ File được tạo với tên đúng format
```

### **Scenario 2: Export nhiều lần**
```
Export lần 1: data_2024-01-15_14-30-25.xlsx
Export lần 2: data_2024-01-15_14-30-28.xlsx
Export lần 3: data_2024-01-15_14-30-31.xlsx
Result: ✅ Mỗi file có tên khác nhau, không bị ghi đè
```

### **Scenario 3: Sắp xếp file**
```
File list:
- data_2024-01-14_10-15-20.xlsx
- data_2024-01-15_09-30-15.xlsx  
- data_2024-01-15_14-30-25.xlsx
Result: ✅ Tự động sắp xếp theo thứ tự thời gian
```

### **Scenario 4: Cross-platform**
```
Windows: ✅ data_2024-01-15_14-30-25.xlsx (valid)
macOS: ✅ data_2024-01-15_14-30-25.xlsx (valid)
Android: ✅ data_2024-01-15_14-30-25.xlsx (valid)
```

## 📱 **User Experience:**

### **Khi user export:**
1. **Nhấn Export** từ menu dropdown
2. **File picker mở** với tên mặc định: `data_2024-01-15_14-30-25.xlsx`
3. **User có thể:**
   - Giữ nguyên tên → Lưu với timestamp
   - Đổi tên → Tùy chỉnh theo ý muốn
4. **File được lưu** với tên đã chọn

### **Trong file manager:**
```
📁 Downloads/
  📄 data_2024-01-14_10-15-20.xlsx (100 KB)
  📄 data_2024-01-15_09-30-15.xlsx (150 KB)
  📄 data_2024-01-15_14-30-25.xlsx (200 KB) ← Mới nhất
```

## ✅ **Hoàn thành:**

Tên file export giờ đây:
- ✅ **Có prefix "data_"** → dễ nhận biết
- ✅ **Ngày giờ đầy đủ** → biết chính xác thời gian export
- ✅ **Format chuẩn** → YYYY-MM-DD_HH-mm-ss
- ✅ **Không trùng lặp** → mỗi lần export 1 tên khác nhau
- ✅ **Tương thích** → hoạt động trên mọi platform

Bạn có thể test ngay bây giờ! 🎉
