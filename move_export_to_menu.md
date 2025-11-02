# 📱 Chuyển Export vào Menu Dropdown

## 🎯 **Yêu cầu:**
- ✅ Đưa chức năng Export vào menu dropdown (3 chấm)
- ✅ Bỏ nút Export khỏi top bar
- ✅ Thêm "Export Excel" vào menu với icon FileDownload

## 📝 **Files đã sửa:**

### **1. MoreView.kt** (Menu Dropdown)

#### **Thêm import:**
```kotlin
import androidx.compose.material.icons.filled.FileDownload
```

#### **Thêm enum Export:**
```kotlin
enum class MoreVertFunctions(val icon: ImageVector, val text: String) {
    Home(Icons.Filled.Home, "Trang chủ"),
    Support(Icons.Filled.Call, "Hỗ trợ"),
    Filter(Icons.Filled.FilterAlt, "Xóa lặp"),
    Export(Icons.Filled.FileDownload, "Export Excel"), // ✅ THÊM MỚI
    Update(Icons.Filled.CloudDownload, "Cập nhật"),
    Out(Icons.AutoMirrored.Filled.ExitToApp, "Thoát"),
}
```

#### **Thêm parameter onExportClick:**
```kotlin
@Composable
fun MoreView(
    button: BottomButton,
    onDismissRequest: () -> Unit,
    onRemoveDuplicates: () -> Unit = {},
    onRestoreUnsentCustomers: () -> Unit = {},
    onUpdateClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onExportClick: () -> Unit = {} // ✅ THÊM MỚI
)
```

#### **Thêm xử lý onClick:**
```kotlin
when (it) {
    MoreVertFunctions.Home -> { onHomeClick(); onDismissRequest() }
    MoreVertFunctions.Support -> { onSupportClick(); onDismissRequest() }
    MoreVertFunctions.Filter -> { onRemoveDuplicates(); onDismissRequest() }
    MoreVertFunctions.Export -> { // ✅ THÊM MỚI
        onExportClick()
        onDismissRequest()
    }
    MoreVertFunctions.Update -> { onUpdateClick(); onDismissRequest() }
    MoreVertFunctions.Out -> { exitProcess(0) }
}
```

### **2. MyBottomBar.kt** (Bottom Bar)

#### **Thêm parameter:**
```kotlin
fun MyBottomBar(
    // ... existing parameters
    onExportClick: (() -> Unit) = {} // ✅ THÊM MỚI
)
```

#### **Truyền vào MoreView:**
```kotlin
MoreView(
    button = button,
    onDismissRequest = { button = BottomButton.None },
    onRemoveDuplicates = onRemoveDuplicates,
    onRestoreUnsentCustomers = onRestoreUnsentCustomers,
    onUpdateClick = onUpdateClick,
    onHomeClick = onHomeClick,
    onSupportClick = onSupportClick,
    onExportClick = onExportClick // ✅ THÊM MỚI
)
```

### **3. MyTopBar.kt** (Top Bar)

#### **Bỏ parameter onExport:**
```kotlin
// TRƯỚC:
fun MyTopBar(
    selectAll: Boolean,
    onDeleteAll: () -> Unit,
    onUpload: () -> Unit,
    onExport: () -> Unit, // ❌ XÓA
    onCheckedChange: ((Boolean) -> Unit)
)

// SAU:
fun MyTopBar(
    selectAll: Boolean,
    onDeleteAll: () -> Unit,
    onUpload: () -> Unit,
    onCheckedChange: ((Boolean) -> Unit)
)
```

#### **Bỏ nút Export:**
```kotlin
// ❌ XÓA TOÀN BỘ:
// Nút Export - màu xanh lá
Button(
    onClick = { onExport() },
    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
    shape = RoundedCornerShape(4.dp),
    border = BorderStroke(1.dp, Color(0xFF4CAF50)),
    modifier = Modifier.height(50.dp)
) {
    Text("Export", color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Medium)
}
```

### **4. MainScreen.kt** (Main Screen)

#### **Bỏ onExport khỏi MyTopBar:**
```kotlin
// TRƯỚC:
MyTopBar(
    selectAll = selectAll,
    onDeleteAll = { ... },
    onUpload = { ... },
    onExport = { exportLauncher.launch(...) }, // ❌ XÓA
    onCheckedChange = { ... }
)

// SAU:
MyTopBar(
    selectAll = selectAll,
    onDeleteAll = { ... },
    onUpload = { ... },
    onCheckedChange = { ... }
)
```

#### **Thêm onExportClick vào MyBottomBar:**
```kotlin
MyBottomBar(
    // ... existing parameters
    onExportClick = { // ✅ THÊM MỚI
        exportLauncher.launch("khach_hang_${System.currentTimeMillis()}.xlsx")
    }
)
```

## 📱 **Giao diện mới:**

### **Top Bar (đơn giản hơn):**
```
[✓ Chọn tất cả] [Xóa tất cả] [Import]
```

### **Menu Dropdown (3 chấm ở bottom):**
```
🏠 Trang chủ
📞 Hỗ trợ  
🔽 Xóa lặp
📥 Export Excel  ← ✅ MỚI
☁️ Cập nhật
🚪 Thoát
```

## 🎯 **Workflow sử dụng:**

### **Cách Export mới:**
1. **Nhấn icon 3 chấm** ở bottom bar (bên phải)
2. **Chọn "Export Excel"** từ menu dropdown
3. **Chọn vị trí lưu file** (Android file picker)
4. **Nhận file Excel** với 9 cột dữ liệu

### **So sánh:**

**Trước:**
```
Top Bar: [Chọn tất cả] [Xóa tất cả] [Import] [Export]
Bottom Bar: [Settings] [Search] [⋮]
```

**Bây giờ:**
```
Top Bar: [Chọn tất cả] [Xóa tất cả] [Import]
Bottom Bar: [Settings] [Search] [⋮] → Menu có "Export Excel"
```

## 💡 **Lợi ích:**

1. **Top bar gọn gàng hơn:** Bớt 1 nút, không bị chật
2. **Nhóm chức năng logic:** Export cùng với các chức năng khác trong menu
3. **Tiết kiệm không gian:** Top bar có thể hiển thị tốt hơn trên màn hình nhỏ
4. **UX nhất quán:** Các chức năng phụ đều ở menu dropdown
5. **Dễ mở rộng:** Thêm chức năng mới vào menu dễ dàng

## 🚀 **Test scenarios:**

### **Scenario 1: Export từ menu**
```
1. Nhấn icon ⋮ (3 chấm) ở bottom bar
2. Menu hiển thị với "📥 Export Excel"
3. Nhấn "Export Excel"
4. Menu đóng và mở file picker
5. Chọn vị trí → Export thành công
```

### **Scenario 2: Top bar gọn gàng**
```
Expected: Top bar chỉ có [Chọn tất cả] [Xóa tất cả] [Import]
Result: ✅ Không còn nút Export, giao diện gọn gàng
```

### **Scenario 3: Menu đầy đủ**
```
Expected: Menu có 6 mục: Trang chủ, Hỗ trợ, Xóa lặp, Export Excel, Cập nhật, Thoát
Result: ✅ Export Excel ở vị trí thứ 4 với icon FileDownload
```

## ✅ **Hoàn thành:**

Export Excel giờ đây:
- ✅ **Nằm trong menu dropdown** (3 chấm)
- ✅ **Icon FileDownload** và text "Export Excel"
- ✅ **Top bar gọn gàng** hơn (bớt 1 nút)
- ✅ **Chức năng không đổi** (vẫn export 9 cột, không header)
- ✅ **UX nhất quán** với các chức năng khác

Bạn có thể test ngay bây giờ! 🎉
