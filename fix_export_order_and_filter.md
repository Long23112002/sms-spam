# 📋 Sửa thứ tự Export theo đúng App

## 🎯 **Vấn đề:**
- ❌ Export lấy `allCustomers` từ repository (không filter)
- ❌ Thứ tự export khác với thứ tự hiển thị trên app
- ❌ Không respect filter theo nhà mạng (provider)

## ✅ **Giải pháp:**

### **1. MainViewModel.kt - Thêm filter theo provider**

#### **Trước:**
```kotlin
fun exportToExcel(uri: Uri, onMessage: (String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
    val excelExporter = ExcelExporter(application)
    val allCustomers = smsRepository.getCustomers() // ❌ Lấy tất cả, không filter
    
    val success = excelExporter.exportCustomers(allCustomers, uri)
    // ...
}
```

#### **Sau:**
```kotlin
fun exportToExcel(uri: Uri, selectedProvider: String = "all", onMessage: (String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
    val excelExporter = ExcelExporter(application)
    val allCustomers = smsRepository.getCustomers()
    
    // ✅ Filter customers theo provider giống như trên app
    val customersToExport = if (selectedProvider == "all") {
        allCustomers
    } else {
        allCustomers.filter { customer ->
            customer.carrier.lowercase() == selectedProvider.lowercase()
        }
    }
    
    Log.d("MainViewModel", "📋 Export: Total customers: ${allCustomers.size}, Filtered for '$selectedProvider': ${customersToExport.size}")
    
    if (customersToExport.isEmpty()) {
        onMessage("❌ Không có khách hàng nào để xuất cho nhà mạng '$selectedProvider'")
        return@launch
    }
    
    val success = excelExporter.exportCustomers(customersToExport, uri)
    // ...
}
```

### **2. MainScreen.kt - Truyền selectedProvider**

#### **Export launcher:**
```kotlin
val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
) { uri ->
    uri?.let {
        mainViewModel.exportToExcel(it, selectedProvider) { msg -> // ✅ Truyền selectedProvider
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
```

#### **Filename theo provider:**
```kotlin
onExportClick = {
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.getDefault())
    val timestamp = dateFormat.format(java.util.Date())
    val filename = if (selectedProvider == "all") {
        "data_all_$timestamp.xlsx"      // ✅ Tất cả nhà mạng
    } else {
        "data_${selectedProvider}_$timestamp.xlsx"  // ✅ Theo nhà mạng cụ thể
    }
    exportLauncher.launch(filename)
}
```

## 📱 **Kết quả:**

### **Thứ tự và filter giống hệt app:**

#### **Khi chọn "Tất cả":**
```
App hiển thị: 100 khách hàng (tất cả nhà mạng)
Export: 100 khách hàng (đúng thứ tự như app)
Filename: data_all_2024-01-15_14-30-25.xlsx
```

#### **Khi chọn "Viettel":**
```
App hiển thị: 60 khách hàng Viettel
Export: 60 khách hàng Viettel (đúng thứ tự như app)
Filename: data_viettel_2024-01-15_14-30-25.xlsx
```

#### **Khi chọn "Mobifone":**
```
App hiển thị: 25 khách hàng Mobifone
Export: 25 khách hàng Mobifone (đúng thứ tự như app)
Filename: data_mobifone_2024-01-15_14-30-25.xlsx
```

#### **Khi chọn "Vinaphone":**
```
App hiển thị: 15 khách hàng Vinaphone
Export: 15 khách hàng Vinaphone (đúng thứ tự như app)
Filename: data_vinaphone_2024-01-15_14-30-25.xlsx
```

## 📊 **So sánh trước và sau:**

### **Trước khi sửa:**
```
App filter: Viettel (60 customers)
App hiển thị:
1. Nguyễn A (Viettel)
2. Trần B (Viettel)
3. Lê C (Viettel)
...

Export: ALL customers (100 customers) ❌
1. Nguyễn A (Viettel)
2. Phạm D (Mobifone)  ← Không nên có
3. Hoàng E (Vinaphone) ← Không nên có
4. Trần B (Viettel)
...
```

### **Sau khi sửa:**
```
App filter: Viettel (60 customers)
App hiển thị:
1. Nguyễn A (Viettel)
2. Trần B (Viettel)
3. Lê C (Viettel)
...

Export: Viettel customers only (60 customers) ✅
1. Nguyễn A (Viettel)
2. Trần B (Viettel)
3. Lê C (Viettel)
...
```

## 🎯 **Logic filter chính xác:**

### **Code filter trong app (MainScreen.kt):**
```kotlin
val customers = remember(allCustomers, selectedProvider) {
    if (selectedProvider == "all") {
        allCustomers
    } else {
        allCustomers.filter { customer ->
            customer.carrier.lowercase() == selectedProvider.lowercase()
        }
    }
}
```

### **Code filter trong export (MainViewModel.kt):**
```kotlin
val customersToExport = if (selectedProvider == "all") {
    allCustomers
} else {
    allCustomers.filter { customer ->
        customer.carrier.lowercase() == selectedProvider.lowercase()
    }
}
```

**→ Hoàn toàn giống nhau!** ✅

## 📁 **Filename examples:**

| Provider | Filename |
|----------|----------|
| **Tất cả** | `data_all_2024-01-15_14-30-25.xlsx` |
| **Viettel** | `data_viettel_2024-01-15_14-30-25.xlsx` |
| **Mobifone** | `data_mobifone_2024-01-15_14-30-25.xlsx` |
| **Vinaphone** | `data_vinaphone_2024-01-15_14-30-25.xlsx` |

## 🚀 **Test scenarios:**

### **Scenario 1: Export tất cả**
```
1. Chọn provider "Tất cả"
2. App hiển thị 100 customers
3. Export → data_all_2024-01-15_14-30-25.xlsx
4. File có 100 rows đúng thứ tự như app
```

### **Scenario 2: Export Viettel**
```
1. Chọn provider "Viettel"
2. App hiển thị 60 customers Viettel
3. Export → data_viettel_2024-01-15_14-30-25.xlsx
4. File có 60 rows chỉ Viettel, đúng thứ tự như app
```

### **Scenario 3: Export provider trống**
```
1. Chọn provider "Vietnamobile" (không có customer nào)
2. App hiển thị 0 customers
3. Export → "❌ Không có khách hàng nào để xuất cho nhà mạng 'vietnamobile'"
4. Không tạo file
```

### **Scenario 4: Thứ tự chính xác**
```
App hiển thị:
Row 1: Nguyễn A
Row 2: Trần B  
Row 3: Lê C

Excel file:
Row 0: Nguyễn A
Row 1: Trần B
Row 2: Lê C

✅ Thứ tự hoàn toàn giống nhau
```

## ✅ **Hoàn thành:**

Export Excel giờ đây:
- ✅ **Thứ tự giống hệt app** (từ trên xuống dưới)
- ✅ **Filter theo provider** (Viettel/Mobifone/Vinaphone/Tất cả)
- ✅ **Filename có provider** (data_viettel_timestamp.xlsx)
- ✅ **Số lượng chính xác** (chỉ export những gì hiển thị trên app)
- ✅ **Log chi tiết** để debug

Bạn có thể test ngay bây giờ! 🎉
