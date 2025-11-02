# 📊 Đơn giản hóa Excel Export

## 🎯 **Yêu cầu:**
- ✅ Chỉ export đến cột "Tùy chọn 5" 
- ✅ Bỏ các cột: Số mẫu tin nhắn, Nhà mạng, Trạng thái
- ✅ Bỏ luôn dòng tiêu đề (header)

## 📝 **Thay đổi trong ExcelExporter.kt:**

### **1. Bỏ Header Row**
```kotlin
// TRƯỚC:
val headerRow = sheet.createRow(0)
val headers = arrayOf(
    "Tên khách hàng", "Số CMND/CCCD", "Số điện thoại", 
    "Địa chỉ", "Tùy chọn 1", "Tùy chọn 2", "Tùy chọn 3", 
    "Tùy chọn 4", "Tùy chọn 5", "Số mẫu tin nhắn", 
    "Nhà mạng", "Trạng thái"
)

// SAU:
// Bỏ header row - bắt đầu trực tiếp từ dữ liệu
```

### **2. Bắt đầu từ Row 0**
```kotlin
// TRƯỚC:
customers.forEachIndexed { rowIndex, customer ->
    val row = sheet.createRow(rowIndex + 1) // Row 1, 2, 3...

// SAU:
customers.forEachIndexed { rowIndex, customer ->
    val row = sheet.createRow(rowIndex) // Row 0, 1, 2...
```

### **3. Chỉ export 9 cột đầu**
```kotlin
// TRƯỚC: 12 cột
row.createCell(9).apply { setCellValue(customer.templateNumber.toDouble()) }
row.createCell(10).apply { setCellValue(customer.carrier) }
row.createCell(11).apply { setCellValue(if (customer.isSelected) "Đã chọn" else "Chưa chọn") }

// SAU: Chỉ 9 cột, dừng ở Tùy chọn 5
row.createCell(8).apply {
    setCellValue(customer.option5)
    cellStyle = dataStyle
}
// Chỉ export đến cột "Tùy chọn 5" - bỏ các cột khác
```

### **4. Column Widths cho 9 cột**
```kotlin
// TRƯỚC: 12 column widths
val columnWidths = arrayOf(4000, 3500, 3500, 5000, 3000, 3000, 3000, 3000, 3000, 2500, 2500, 2500)

// SAU: 9 column widths
val columnWidths = arrayOf(
    4000,  // Tên khách hàng
    3500,  // Số CMND/CCCD
    3500,  // Số điện thoại
    5000,  // Địa chỉ
    3000,  // Tùy chọn 1
    3000,  // Tùy chọn 2
    3000,  // Tùy chọn 3
    3000,  // Tùy chọn 4
    3000   // Tùy chọn 5
)

for (i in 0 until 9) { // Chỉ set width cho 9 cột đầu
    sheet.setColumnWidth(i, columnWidths[i])
}
```

## 📋 **Cấu trúc Excel file mới:**

### **Columns (9 cột):**
| Cột | Tên | Dữ liệu | Width |
|-----|-----|---------|-------|
| A | Tên khách hàng | customer.name | 4000 |
| B | Số CMND/CCCD | customer.idNumber | 3500 |
| C | Số điện thoại | customer.phoneNumber | 3500 |
| D | Địa chỉ | customer.address | 5000 |
| E | Tùy chọn 1 | customer.option1 | 3000 |
| F | Tùy chọn 2 | customer.option2 | 3000 |
| G | Tùy chọn 3 | customer.option3 | 3000 |
| H | Tùy chọn 4 | customer.option4 | 3000 |
| I | Tùy chọn 5 | customer.option5 | 3000 |

### **Rows:**
```
Row 0: Nguyễn Văn A | 123456789 | 0888880243 | Hà Nội | Option1 | Option2 | Option3 | Option4 | Option5
Row 1: Trần Thị B | 987654321 | 0986170323 | TP.HCM | Option1 | Option2 | Option3 | Option4 | Option5
Row 2: ...
```

### **Không có:**
- ❌ Header row (dòng tiêu đề)
- ❌ Cột "Số mẫu tin nhắn"
- ❌ Cột "Nhà mạng" 
- ❌ Cột "Trạng thái"

## 🎯 **Kết quả:**

### **Trước khi sửa:**
```
File Excel có 13 rows cho 12 khách hàng:
Row 0: [HEADER] Tên KH | CMND | SĐT | Địa chỉ | ... | Nhà mạng | Trạng thái
Row 1: Nguyễn A | 123 | 0888 | HN | ... | Viettel | Đã chọn
Row 2: Trần B | 456 | 0986 | HCM | ... | Mobifone | Chưa chọn
...
```

### **Sau khi sửa:**
```
File Excel có 12 rows cho 12 khách hàng:
Row 0: Nguyễn A | 123 | 0888 | HN | Opt1 | Opt2 | Opt3 | Opt4 | Opt5
Row 1: Trần B | 456 | 0986 | HCM | Opt1 | Opt2 | Opt3 | Opt4 | Opt5
...
```

## 💡 **Lợi ích:**

1. **File nhỏ gọn hơn**: Bớt 3 cột không cần thiết
2. **Không có header**: Dữ liệu thuần túy, dễ import vào hệ thống khác
3. **Tập trung vào dữ liệu chính**: Chỉ thông tin khách hàng cơ bản
4. **Dễ xử lý**: Không cần skip header row khi đọc file

## 🚀 **Test scenarios:**

### **Scenario 1: Export 100 khách hàng**
```
Input: 100 customers trong database
Expected: File Excel có 100 rows (không có header)
Columns: A-I (9 cột từ Tên đến Tùy chọn 5)
Result: ✅ "Đã xuất thành công 100 khách hàng ra Excel"
```

### **Scenario 2: Kiểm tra cấu trúc**
```
Expected: 
- Row 0 = khách hàng đầu tiên (không phải header)
- 9 cột từ A đến I
- Không có cột J, K, L (template, carrier, status)
Result: ✅ Đúng cấu trúc
```

### **Scenario 3: Column widths**
```
Expected: 
- Tên KH (4000), Địa chỉ (5000) rộng nhất
- SĐT, CMND (3500) 
- Tùy chọn 1-5 (3000)
Result: ✅ Hiển thị đẹp, dễ đọc
```

## ✅ **Hoàn thành:**

Excel Export giờ đây:
- ✅ **Chỉ 9 cột cần thiết** (Tên → Tùy chọn 5)
- ✅ **Không có header row** (dữ liệu thuần túy)
- ✅ **File nhỏ gọn** (bớt 3 cột + 1 row)
- ✅ **Dễ xử lý** (không cần skip header)
- ✅ **Tập trung vào dữ liệu chính** khách hàng

Bạn có thể test ngay bây giờ! 🎉
