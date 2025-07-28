package com.example.sms_app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.sms_app.data.Customer
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.util.ArrayList
import java.util.UUID

class ExcelImporter(private val context: Context) {


    /**
     * Làm sạch hoàn toàn dữ liệu từ Excel, loại bỏ mọi ký tự ẩn và không mong muốn
     */
    private fun deepCleanExcelData(input: String): String {
        if (input.isBlank()) return ""
        
        try {
            var cleaned = input
            
            // Bước 1: Áp dụng tất cả các kiểu normalization để loại bỏ ký tự ẩn
            cleaned = java.text.Normalizer.normalize(cleaned, java.text.Normalizer.Form.NFD)
            cleaned = java.text.Normalizer.normalize(cleaned, java.text.Normalizer.Form.NFC)
            cleaned = java.text.Normalizer.normalize(cleaned, java.text.Normalizer.Form.NFKD)
            cleaned = java.text.Normalizer.normalize(cleaned, java.text.Normalizer.Form.NFKC)
            
            // Bước 2: Log hex values để debug
            val originalHex = input.map { "\\u" + it.code.toString(16).padStart(4, '0') }.joinToString("")
            Log.d("ExcelImporter", "Original hex: $originalHex")
            
            // Bước 3: Loại bỏ các ký tự điều khiển và ký tự ẩn
            cleaned = cleaned.replace(Regex("\\p{Cntrl}"), "") // Control characters
            cleaned = cleaned.replace(Regex("\\p{Cf}"), "")    // Format characters
            cleaned = cleaned.replace(Regex("\\p{Cc}"), "")    // Control characters (more specific)
            cleaned = cleaned.replace(Regex("\\p{Cn}"), "")    // Unassigned characters
            cleaned = cleaned.replace(Regex("\\p{Co}"), "")    // Private use characters
            cleaned = cleaned.replace(Regex("\\p{Cs}"), "")    // Surrogate characters
            
            // Bước 4: Loại bỏ zero-width characters và invisible characters
            val invisibleChars = listOf(
                '\u200B', // Zero Width Space
                '\u200C', // Zero Width Non-Joiner
                '\u200D', // Zero Width Joiner
                '\u200E', // Left-to-Right Mark
                '\u200F', // Right-to-Left Mark
                '\u2060', // Word Joiner
                '\uFEFF', // Byte Order Mark / Zero Width No-Break Space
                '\u00A0', // Non-Breaking Space
                '\u2000', // En Quad
                '\u2001', // Em Quad
                '\u2002', // En Space
                '\u2003', // Em Space
                '\u2004', // Three-Per-Em Space
                '\u2005', // Four-Per-Em Space
                '\u2006', // Six-Per-Em Space
                '\u2007', // Figure Space
                '\u2008', // Punctuation Space
                '\u2009', // Thin Space
                '\u200A', // Hair Space
                '\u202F', // Narrow No-Break Space
                '\u205F', // Medium Mathematical Space
                '\u3000'  // Ideographic Space
            )
            
            invisibleChars.forEach { char ->
                cleaned = cleaned.replace(char.toString(), "")
            }
            
            // Bước 5: Loại bỏ các ký tự Excel đặc biệt
            cleaned = cleaned.replace("'", "") // Excel single quote prefix
            cleaned = cleaned.replace("\"", "") // Double quotes
            cleaned = cleaned.replace("`", "") // Backticks
            
            // Bước 6: Trim và normalize spaces
            cleaned = cleaned.trim()
            cleaned = cleaned.replace(Regex("\\s+"), " ") // Multiple spaces to single space
            
            // Bước 7: Rebuild string character by character để đảm bảo chỉ có ký tự hợp lệ
            val validChars = StringBuilder()
            for (char in cleaned) {
                val charCode = char.code
                
                // Chỉ giữ lại các ký tự hợp lệ
                when {
                    // ASCII printable characters
                    charCode in 32..126 -> validChars.append(char)
                    // Vietnamese characters
                    char in 'à'..'ỹ' || char in 'À'..'Ỹ' -> validChars.append(char)
                    // Numbers
                    char.isDigit() -> validChars.append(char)
                    // Basic punctuation
                    char in ".,;:!?()-[]{}/@#$%&*+=<>|\\/_^~" -> validChars.append(char)
                    // Whitespace
                    char.isWhitespace() && char == ' ' -> validChars.append(char)
                    // Log suspicious characters
                    else -> {
                        Log.d("ExcelImporter", "Filtered out suspicious character: '$char' (U+${charCode.toString(16).padStart(4, '0')})")
                    }
                }
            }
            
            val result = validChars.toString().trim()
            
            // Log nếu có thay đổi
            if (result != input) {
                Log.d("ExcelImporter", "Deep cleaned: '$input' → '$result'")
                val resultHex = result.map { "\\u" + it.code.toString(16).padStart(4, '0') }.joinToString("")
                Log.d("ExcelImporter", "Result hex: $resultHex")
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e("ExcelImporter", "Error in deep clean", e)
            return input.trim()
        }
    }
    
    /**
     * Làm sạch số điện thoại với deep cleaning
     */
    fun cleanPhoneNumber(phoneNumber: String): String {
        if (phoneNumber.isBlank()) {
            return ""
        }
        
        try {
            // Bước 1: Deep clean trước
            var cleaned = deepCleanExcelData(phoneNumber)
            
            // Bước 2: Xử lý format số điện thoại
            
            // Xử lý trường hợp có +84 ở đầu
            if (cleaned.startsWith("+84")) {
                Log.d("ExcelImporter", "📱 Phát hiện số điện thoại có +84: $cleaned")
                cleaned = "0" + cleaned.substring(3)
                Log.d("ExcelImporter", "📱 Chuyển +84 thành 0: $cleaned")
            }
            // Xử lý trường hợp có 84 ở đầu (không có dấu +)
            else if (cleaned.startsWith("84") && cleaned.length >= 11) {
                Log.d("ExcelImporter", "📱 Phát hiện số điện thoại có 84: $cleaned")
                cleaned = "0" + cleaned.substring(2)
                Log.d("ExcelImporter", "📱 Chuyển 84 thành 0: $cleaned")
            }
            
            // Bước 3: Chỉ giữ lại số (sau khi đã xử lý +84)
            cleaned = cleaned.toCharArray().filter { it.isDigit() }.joinToString("")
            
            // Bước 4: Xử lý các trường hợp khác
            if (!cleaned.startsWith("0")) {
                Log.d("ExcelImporter", "📱 Phát hiện số điện thoại không bắt đầu bằng 0: $cleaned")
                
                // Trường hợp có 9 chữ số (Excel thường xóa số 0 đầu của số điện thoại VN)
                if (cleaned.length == 9) {
                    cleaned = "0$cleaned"
                    Log.d("ExcelImporter", "📱 Tự động thêm số 0 đầu cho số điện thoại 9 chữ số: $cleaned")
                }
                // Các trường hợp khác, vẫn thêm số 0 đầu nếu có thể là số điện thoại Việt Nam
                else if (cleaned.length >= 9 && cleaned.length <= 11) {
                    cleaned = "0$cleaned"
                    Log.d("ExcelImporter", "📱 Tự động thêm số 0 đầu cho số có thể là số điện thoại: $cleaned")
                }
            }
            
            // Xử lý trường hợp Excel lưu số dưới dạng số (có thể thêm các số 0 ở cuối)
            if (cleaned.length > 11 && !cleaned.startsWith("+")) {
                // Nếu số quá dài, có thể Excel đã thêm các số 0 ở cuối
                Log.d("ExcelImporter", "📱 Phát hiện số quá dài, có thể Excel thêm số 0: $cleaned")
                
                // Thử cắt số để chỉ giữ lại 10 chữ số đầu nếu đã có số 0 ở đầu
                if (cleaned.startsWith("0") && cleaned.length > 10) {
                    cleaned = cleaned.substring(0, 10)
                    Log.d("ExcelImporter", "📱 Cắt ngắn số điện thoại thành 10 chữ số: $cleaned")
                }
                // Hoặc lấy 9 chữ số đầu và thêm số 0 nếu chưa có
                else if (!cleaned.startsWith("0")) {
                    // Lấy 9 chữ số đầu tiên và thêm 0 vào đầu
                    if (cleaned.length >= 9) {
                        val potential9Digits = cleaned.substring(0, 9)
                        cleaned = "0$potential9Digits"
                        Log.d("ExcelImporter", "📱 Phát hiện số Excel bị sai định dạng, chuyển thành: $cleaned")
                    }
                }
            }
            
            // Xử lý các đầu số không hợp lệ hoặc đã ngừng sử dụng tại Việt Nam
            if (cleaned.startsWith("0") && cleaned.length == 10) {
                // Kiểm tra và xử lý các tiền tố đặc biệt
                val oldPrefixMap = mapOf(
                    "0123" to "083", "0124" to "084", "0125" to "085",
                    "0127" to "081", "0129" to "082", // Vinaphone
                    
                    "0120" to "070", "0121" to "079", "0122" to "077",
                    "0126" to "076", "0128" to "078", // Mobifone
                    
                    "0162" to "032", "0163" to "033", "0164" to "034",
                    "0165" to "035", "0166" to "036", "0167" to "037",
                    "0168" to "038", "0169" to "039", // Viettel
                    
                    "0188" to "058", "0186" to "056", // Vietnamobile
                    
                    "0199" to "059" // Gmobile
                )
                
                // Kiểm tra và chuyển đổi đầu số cũ
                for ((oldPrefix, newPrefix) in oldPrefixMap) {
                    if (cleaned.startsWith(oldPrefix)) {
                        val newNumber = newPrefix + cleaned.substring(4)
                        Log.d("ExcelImporter", "📱 Chuyển đầu số cũ: $cleaned → $newNumber")
                        cleaned = newNumber
                        break
                    }
                }
                
                // Đầu số 013x là không hợp lệ nữa, chuyển sang 083x
                if (cleaned.startsWith("013")) {
                    val newNumber = "083" + cleaned.substring(3)
                    Log.d("ExcelImporter", "📱 Chuyển đầu số không hợp lệ 013 -> 083: $cleaned -> $newNumber")
                    cleaned = newNumber
                }
                
                // Kiểm tra xem đầu số có hợp lệ không
                val prefix = cleaned.substring(0, 3)
                val validPrefixes = listOf(
                    // Viettel
                    "032", "033", "034", "035", "036", "037", "038", "039",
                    "086", "096", "097", "098",
                    // Mobifone
                    "070", "076", "077", "078", "079",
                    "089", "090", "093",
                    // Vinaphone
                    "081", "082", "083", "084", "085",
                    "088", "091", "094",
                    // Vietnamobile
                    "056", "058", "092",
                    // ITelecom
                    "099",
                    // Reddi/Gmobile
                    "059"
                )
                
                if (!validPrefixes.contains(prefix)) {
                    Log.w("ExcelImporter", "⚠️ Cảnh báo: Tiền tố $prefix không hợp lệ")
                }
            }
            
            // Ghi log để debug
            if (cleaned != phoneNumber) {
                Log.d("ExcelImporter", "Cleaned phone: Original='$phoneNumber', Cleaned='$cleaned'")
            }
            
            return cleaned
        } catch (e: Exception) {
            Log.e("ExcelImporter", "Error cleaning phone number", e)
            return phoneNumber // Trả về chuỗi gốc nếu có lỗi
        }
    }

    /**
     * Import customers from Excel file
     */
    fun importCustomers(uri: Uri): List<Customer> {
        Log.d("ExcelImporter", "Starting Excel import from $uri")
        val customers = mutableListOf<Customer>()
        var inputStream: InputStream? = null
        var customerSequence = 0 // Để tạo ID theo thứ tự
        
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("ExcelImporter", "Failed to open input stream for $uri")
                return emptyList()
            }
            
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0) // Get first sheet
            
            // Bắt đầu từ dòng đầu tiên (không bỏ qua header)
            val startRow = 0
            var emptyRowCount = 0
            var consecutiveEmptyRows = 0
            
            Log.d("ExcelImporter", "Reading data from Excel sheet with ${sheet.physicalNumberOfRows} rows")
            Log.d("ExcelImporter", "Starting from row $startRow (0-based index)")

            var totalRowsProcessed = 0
            var validCustomersFound = 0
            var skippedRows = 0

            for (rowIndex in startRow until sheet.physicalNumberOfRows) {
                totalRowsProcessed++
                val row = sheet.getRow(rowIndex) ?: continue

                // Log dòng đầu tiên để đảm bảo không bị bỏ qua
                if (rowIndex == 0) {
                    Log.d("ExcelImporter", "🔍 Processing FIRST ROW (index 0)")
                }
                
                // Check if this row is empty
                val isRowEmpty = isEmptyRow(row)
                if (isRowEmpty) {
                    emptyRowCount++
                    skippedRows++
                    Log.d("ExcelImporter", "Row $rowIndex: Empty row detected, count: $emptyRowCount")

                    // Stop processing after 3 consecutive empty rows
                    if (emptyRowCount >= 3) {
                        Log.d("ExcelImporter", "Stopping import after 3 consecutive empty rows at row $rowIndex")
                        break
                    }
                    continue
                } else {
                    // Reset empty row counter if a non-empty row is found
                    emptyRowCount = 0
                }
                
                // Read customer data from columns
                // Column A (0): Name (xxx)
                // Column B (1): ID Number (yyy)
                // Column C (2): Phone Number
                // Column D (3): Address (ttt)
                // Column E (4): Option1 (zzz)
                // Columns F-I (5-8): Options 2-5
                // Column J (9): Template Number (optional)
                
                // Đọc dữ liệu từ row - áp dụng deep clean cho tất cả
                val name = deepCleanExcelData(getCellValueAsString(row.getCell(0)))
                val idNumber = deepCleanExcelData(getCellValueAsString(row.getCell(1)))
                val rawPhoneNumber = getCellValueAsString(row.getCell(2))
                val address = deepCleanExcelData(getCellValueAsString(row.getCell(3)))
                val option1 = deepCleanExcelData(getCellValueAsString(row.getCell(4)))
                val option2 = deepCleanExcelData(getCellValueAsString(row.getCell(5)))
                val option3 = deepCleanExcelData(getCellValueAsString(row.getCell(6)))
                val option4 = deepCleanExcelData(getCellValueAsString(row.getCell(7)))
                val option5 = deepCleanExcelData(getCellValueAsString(row.getCell(8)))
                val templateId = getCellValueAsInt(row.getCell(9))

                // Log raw data from Excel for debugging
                Log.d("ExcelImporter", "Row $rowIndex: Raw data before cleaning")
                Log.d("ExcelImporter", "  Raw name: '${getCellValueAsString(row.getCell(0))}'")
                Log.d("ExcelImporter", "  Raw phone: '$rawPhoneNumber'")

                // Xử lý nhiều số điện thoại cách nhau bởi dấu phẩy
                val phoneNumbers = if (rawPhoneNumber.contains(",")) {
                    rawPhoneNumber.split(",").map { it.trim() }.filter { it.isNotBlank() }
                } else {
                    listOf(rawPhoneNumber.trim()).filter { it.isNotBlank() }
                }

                Log.d("ExcelImporter", "Row $rowIndex: Found ${phoneNumbers.size} phone numbers: $phoneNumbers")

                // Nếu không có số điện thoại nào, bỏ qua dòng này
                if (phoneNumbers.isEmpty() || phoneNumbers.all { it.isBlank() }) {
                    skippedRows++
                    Log.d("ExcelImporter", "Row $rowIndex: SKIPPED - no valid phone numbers found")
                    consecutiveEmptyRows++
                    if (consecutiveEmptyRows >= 3) {
                        Log.d("ExcelImporter", "Found 3 consecutive empty rows, stopping import")
                        break
                    }
                    continue
                }

                consecutiveEmptyRows = 0 // Reset counter khi tìm thấy dữ liệu

                // Tạo một khách hàng cho mỗi số điện thoại
                phoneNumbers.forEachIndexed { phoneIndex, phone ->
                    val cleanedPhone = cleanPhoneNumber(phone)

                    if (cleanedPhone.isNotBlank()) {
                        // Create customer object with sequential ID to preserve import order
                        customerSequence++
                        val customer = Customer(
                            id = "customer_${System.currentTimeMillis()}_${customerSequence.toString().padStart(6, '0')}",
                            name = name.trim(),
                            idNumber = idNumber.trim(),
                            phoneNumber = cleanedPhone,
                            address = address.trim(),
                            option1 = option1.trim(),
                            option2 = option2.trim(),
                            option3 = option3.trim(),
                            option4 = option4.trim(),
                            option5 = option5.trim(),
                            isSelected = false,
                            carrier = determineCarrier(cleanedPhone),
                            templateNumber = templateId
                        )

                        customers.add(customer)
                        validCustomersFound++
                        Log.d("ExcelImporter", "Row $rowIndex, Phone ${phoneIndex + 1}: ADDED customer: '${customer.name}' with phone '${customer.phoneNumber}'")
                    } else {
                        Log.w("ExcelImporter", "Row $rowIndex, Phone ${phoneIndex + 1}: SKIPPED - Invalid phone number: '$phone'")
                    }
                }
            }
            
            Log.d("ExcelImporter", "📊 IMPORT SUMMARY:")
            Log.d("ExcelImporter", "   Total rows processed: $totalRowsProcessed")
            Log.d("ExcelImporter", "   Valid customers found: $validCustomersFound")
            Log.d("ExcelImporter", "   Rows skipped: $skippedRows")
            Log.d("ExcelImporter", "   Final customer list size: ${customers.size}")
            Log.d("ExcelImporter", "✅ Successfully imported ${customers.size} customers from Excel")
            
            // TỰ ĐỘNG XÓA CACHE SAU KHI IMPORT ĐỂ NGĂN CHẶN TỰ ĐỘNG GỬI SMS
            try {
                val cacheManager = CacheManager(context)
                cacheManager.clearCacheAfterImport()
                Log.d("ExcelImporter", "✅ Đã xóa cache sau khi import để ngăn tự động gửi SMS")
            } catch (e: Exception) {
                Log.e("ExcelImporter", "❌ Lỗi khi xóa cache sau import", e)
            }
            
        } catch (e: Exception) {
            Log.e("ExcelImporter", "Error importing Excel file", e)
        } finally {
            inputStream?.close()
        }
        
        return customers
    }
    
    /**
     * Check if a row is empty (all cells are null or blank)
     */
    private fun isEmptyRow(row: Row): Boolean {
        // Check first 5 essential columns
        for (i in 0..4) {
            val cell = row.getCell(i)
            if (cell != null && getCellValueAsString(cell).isNotBlank()) {
                return false
            }
        }
        return true
    }
    
    // Cập nhật phương thức getCellValueAsString để xử lý đặc biệt cho số điện thoại trong Excel
    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        
        return try {
            val rawValue = when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue ?: ""
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // For date cells, convert to readable format
                        val date = cell.dateCellValue
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date)
                    } else {
                        // For numeric cells, convert to string
                        val numericValue = cell.numericCellValue
                        if (numericValue == numericValue.toLong().toDouble()) {
                            // Số nguyên
                            numericValue.toLong().toString()
                        } else {
                            // Số thập phân
                            numericValue.toString()
                        }
                    }
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        val evaluator = cell.sheet.workbook.creationHelper.createFormulaEvaluator()
                        val result = evaluator.evaluate(cell)
                        when (result.cellType) {
                            CellType.STRING -> result.stringValue ?: ""
                            CellType.NUMERIC -> {
                                val numValue = result.numberValue
                                if (numValue == numValue.toLong().toDouble()) {
                                    numValue.toLong().toString()
                                } else {
                                    numValue.toString()
                                }
                            }
                            CellType.BOOLEAN -> result.booleanValue.toString()
                            else -> ""
                        }
                    } catch (e: Exception) {
                        Log.w("ExcelImporter", "Cannot evaluate formula: ${e.message}")
                        ""
                    }
                }
                else -> ""
            }
            
            // Apply deep cleaning to all extracted string values
            deepCleanExcelData(rawValue)
            
        } catch (e: Exception) {
            Log.e("ExcelImporter", "Error reading cell value: ${e.message}")
            ""
        }
    }

    private fun getCellValueAsInt(cell: Cell?): Int {
        if (cell == null) return 1 // Default to 1 if cell is null
        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Date doesn't have toInt() method, convert to timestamp first
                        cell.dateCellValue?.time?.div(1000)?.toInt() ?: 1
                    } else {
                        cell.numericCellValue.toInt()
                    }
                }
                CellType.STRING -> {
                    cell.stringCellValue?.toIntOrNull() ?: 1
                }
                CellType.BOOLEAN -> {
                    if (cell.booleanCellValue) 1 else 0
                }
                CellType.FORMULA -> {
                    try {
                        cell.numericCellValue.toInt()
                    } catch (e: Exception) {
                        cell.stringCellValue?.toIntOrNull() ?: 1
                    }
                }
                else -> 1
            }
        } catch (e: Exception) {
            Log.e("ExcelImporter", "Error getting cell value as int", e)
            1
        }
    }

    private fun determineCarrier(phoneNumber: String): String {
        try {
            val cleanedPhone = cleanPhoneNumber(phoneNumber)
            if (cleanedPhone.length < 3) {
                return "Unknown"
            }
            val prefix = cleanedPhone.substring(0, 3)

            return when (prefix) {
                "032", "033", "034", "035", "036", "037", "038", "039" -> "Viettel"
                "086", "096", "097", "098" -> "Viettel"
                "070", "076", "077", "078", "079" -> "Mobifone"
                "089", "090", "093" -> "Mobifone"
                "081", "082", "083", "084", "085" -> "Vinaphone"
                "088", "091", "094" -> "Vinaphone"
                "056", "058", "092" -> "Vietnamobile"
                "099" -> "ITelecom"
                "059" -> "Reddi/Gmobile"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.w("ExcelImporter", "Error determining carrier for phone: $phoneNumber", e)
            return "Unknown"
        }
    }

    /**
     * Debug function để phân tích và hiển thị tất cả ký tự trong chuỗi
     */
    private fun debugCharacterAnalysis(input: String, label: String) {
        Log.d("ExcelImporter", "=== CHARACTER ANALYSIS for $label ===")
        Log.d("ExcelImporter", "String: '$input'")
        Log.d("ExcelImporter", "Length: ${input.length}")
        
        input.forEachIndexed { index, char ->
            val charCode = char.code
            val charName = when {
                char.isDigit() -> "DIGIT"
                char.isLetter() -> "LETTER"
                char.isWhitespace() -> "WHITESPACE"
                charCode < 32 -> "CONTROL"
                charCode in 32..126 -> "ASCII_PRINTABLE"
                charCode in 127..159 -> "C1_CONTROL"
                charCode in 160..255 -> "LATIN1_SUPPLEMENT"
                charCode in 8192..8303 -> "GENERAL_PUNCTUATION"
                charCode in 8192..8303 -> "SPACE_CHARS"
                else -> "OTHER"
            }
            
            val hex = charCode.toString(16).padStart(4, '0')
            val displayChar = if (charCode in 32..126) char.toString() else "□"
            
            Log.d("ExcelImporter", "[$index] '$displayChar' U+$hex ($charCode) - $charName")
        }
        Log.d("ExcelImporter", "=== END CHARACTER ANALYSIS ===")
    }
    
    /**
     * Test function để kiểm tra việc làm sạch dữ liệu
     */
    fun testDataCleaning(testString: String): String {
        Log.d("ExcelImporter", "🧪 TESTING DATA CLEANING")
        debugCharacterAnalysis(testString, "BEFORE CLEANING")
        
        val cleaned = deepCleanExcelData(testString)
        debugCharacterAnalysis(cleaned, "AFTER CLEANING")
        
        Log.d("ExcelImporter", "🧪 CLEANING RESULT: '$testString' → '$cleaned'")
        return cleaned
    }
}
