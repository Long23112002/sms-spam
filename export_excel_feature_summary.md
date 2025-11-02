# 📊 Chức năng Export khách hàng ra Excel

## 🎯 **Tính năng mới:**
- ✅ Export tất cả khách hàng ra file Excel (.xlsx)
- ✅ Định dạng đẹp với header và border
- ✅ Auto-size columns để dễ đọc
- ✅ Tên file tự động với timestamp

## 📁 **Files đã tạo/sửa:**

### 1. **ExcelExporter.kt** (Mới)
```kotlin
class ExcelExporter(private val context: Context) {
    fun exportCustomers(customers: List<Customer>, uri: Uri): Boolean
    fun generateDefaultFilename(): String
}
```

**Tính năng:**
- ✅ Export tất cả thông tin khách hàng
- ✅ Header với style đẹp (màu xanh, font đậm)
- ✅ Border cho tất cả cells
- ✅ Auto-size columns
- ✅ Xử lý lỗi an toàn

### 2. **MainViewModel.kt** (Cập nhật)
```kotlin
fun exportToExcel(uri: Uri, onMessage: (String) -> Unit)
```

**Logic:**
- ✅ Lấy tất cả khách hàng từ repository
- ✅ Kiểm tra có dữ liệu không
- ✅ Gọi ExcelExporter để xuất file
- ✅ Thông báo kết quả cho user

### 3. **MyTopBar.kt** (Cập nhật)
```kotlin
fun MyTopBar(
    selectAll: Boolean,
    onDeleteAll: () -> Unit,
    onUpload: () -> Unit,
    onExport: () -> Unit, // ✅ Thêm parameter mới
    onCheckedChange: ((Boolean) -> Unit)
)
```

**UI Changes:**
- ✅ Thêm nút "Export" màu xanh lá
- ✅ Đổi "Chọn tệp tin" thành "Import"
- ✅ Layout 2 nút: Import (tím) | Export (xanh lá)

### 4. **MainScreen.kt** (Cập nhật)
```kotlin
val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
) { uri -> ... }
```

**Features:**
- ✅ Export launcher với MIME type Excel
- ✅ Tên file mặc định với timestamp
- ✅ Toast thông báo kết quả
- ✅ Kết nối với MyTopBar

## 📋 **Cấu trúc Excel file:**

### **Headers:**
| Cột | Tên | Dữ liệu |
|-----|-----|---------|
| A | Tên khách hàng | customer.name |
| B | Số CMND/CCCD | customer.idNumber |
| C | Số điện thoại | customer.phoneNumber |
| D | Địa chỉ | customer.address |
| E | Tùy chọn 1 | customer.option1 |
| F | Tùy chọn 2 | customer.option2 |
| G | Tùy chọn 3 | customer.option3 |
| H | Tùy chọn 4 | customer.option4 |
| I | Tùy chọn 5 | customer.option5 |
| J | Số mẫu tin nhắn | customer.templateNumber |
| K | Nhà mạng | customer.carrier |
| L | Trạng thái | "Đã chọn" / "Chưa chọn" |

### **Styling:**
- ✅ **Header**: Nền xanh, chữ trắng, font đậm
- ✅ **Data**: Border mỏng cho tất cả cells
- ✅ **Columns**: Auto-size với width tối thiểu 3000

## 🚀 **Cách sử dụng:**

### **Bước 1:** Nhấn nút "Export"
- Nút màu xanh lá ở top bar
- Bên cạnh nút "Import"

### **Bước 2:** Chọn vị trí lưu file
- Android file picker mở ra
- Tên file mặc định: `khach_hang_2024-01-15_14-30-25.xlsx`
- User có thể đổi tên

### **Bước 3:** Chờ export hoàn thành
- Toast hiển thị: "Đang xuất dữ liệu ra Excel..."
- Khi xong: "✅ Đã xuất thành công X khách hàng ra Excel"

### **Bước 4:** Mở file Excel
- File được lưu ở vị trí user chọn
- Có thể mở bằng Excel, Google Sheets, WPS Office...

## 🎯 **Test scenarios:**

### **Scenario 1: Export thành công**
```
- Có 100 khách hàng trong database
- Nhấn Export → Chọn vị trí → Lưu
- Kết quả: "✅ Đã xuất thành công 100 khách hàng ra Excel"
- File Excel có 101 rows (1 header + 100 data)
```

### **Scenario 2: Không có dữ liệu**
```
- Database trống (0 khách hàng)
- Nhấn Export
- Kết quả: "❌ Không có khách hàng nào để xuất"
- Không mở file picker
```

### **Scenario 3: Lỗi ghi file**
```
- Có dữ liệu nhưng không thể ghi file (permission, disk full...)
- Kết quả: "❌ Lỗi khi xuất file Excel"
```

### **Scenario 4: Exception**
```
- Lỗi bất ngờ (OutOfMemory, etc.)
- Kết quả: "❌ Lỗi xuất Excel: [error message]"
```

## 💡 **Lợi ích:**

1. **Backup dữ liệu**: User có thể backup toàn bộ khách hàng
2. **Chia sẻ**: Gửi file Excel cho người khác
3. **Phân tích**: Mở trong Excel để phân tích, lọc, sắp xếp
4. **In ấn**: In danh sách khách hàng từ Excel
5. **Tương thích**: Mở được trên mọi thiết bị có Excel/Sheets

## 🔄 **Workflow hoàn chỉnh:**

```
Import Excel → App Database → Export Excel
     ↑                              ↓
   User data                    Backup/Share
```

**Bây giờ user có thể:**
- ✅ Import dữ liệu từ Excel vào app
- ✅ Quản lý khách hàng trong app  
- ✅ Export dữ liệu ra Excel để backup/chia sẻ
- ✅ Chu trình hoàn chỉnh: Excel ↔ App ↔ Excel
