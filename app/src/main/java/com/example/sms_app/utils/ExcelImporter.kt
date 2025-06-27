package com.example.sms_app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.sms_app.data.Customer
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.util.ArrayList
import java.util.UUID

class ExcelImporter(private val context: Context) {
    
    companion object {
        private const val TAG = "ExcelImporter"
    }
    
    fun importCustomers(uri: Uri): List<Customer> {
        val customers = ArrayList<Customer>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = try {
                    XSSFWorkbook(inputStream)
                } catch (e: Exception) {
                    Log.d(TAG, "Not XLSX format, trying XLS...")
                    context.contentResolver.openInputStream(uri)?.use { newInputStream ->
                        HSSFWorkbook(newInputStream)
                    } ?: throw Exception("Cannot open Excel file")
                }
                
                val sheet = workbook.getSheetAt(0)
                Log.d(TAG, "Sheet has ${sheet.lastRowNum + 1} rows")
                
                val startRow = if (isHeaderRow(sheet)) 1 else 0
                
                for (i in startRow..sheet.lastRowNum) {
                    try {
                        val row = sheet.getRow(i) ?: continue
                        
                        val (name, phone) = detectNameAndPhone(row)
                        
                        if (name.isEmpty() || phone.isEmpty()) {
                            Log.d(TAG, "Skip row $i: missing name or phone")
                            continue
                        }
                        
                        val customer = Customer(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            idNumber = "",
                            phoneNumber = phone,
                            address = "",
                            option1 = "",
                            option2 = "",
                            option3 = "",
                            option4 = "",
                            option5 = "",
                            templateNumber = 0
                        )
                        
                        customers.add(customer)
                        Log.d(TAG, "Added customer: $name - $phone")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading row $i: ${e.message}")
                    }
                }
                
                workbook.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importing Excel file: ${e.message}")
            throw Exception("Error reading Excel file: ${e.message}")
        }
        
        Log.d(TAG, "Imported ${customers.size} customers")
        return customers
    }
    
    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""
        
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> {
                val numValue = cell.numericCellValue
                if (numValue == numValue.toLong().toDouble()) {
                    numValue.toLong().toString()
                } else {
                    numValue.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }
    
    private fun isHeaderRow(sheet: Sheet): Boolean {
        val firstRow = sheet.getRow(0) ?: return false
        
        try {
            for (i in 0 until minOf(firstRow.lastCellNum.toInt(), 5)) {
                val cellContent = getCellValueAsString(firstRow.getCell(i)).lowercase()
                if (cellContent.contains("name") || cellContent.contains("phone") || 
                    cellContent.contains("ten") || cellContent.contains("sdt")) {
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
    
    private fun isPhoneNumber(text: String): Boolean {
        val digitsOnly = text.replace(Regex("[^0-9]"), "")
        Log.d(TAG, "Checking phone '$text' -> digits: '$digitsOnly', length: ${digitsOnly.length}")

        // Số điện thoại Việt Nam: 9-11 chữ số
        if (digitsOnly.length < 8 || digitsOnly.length > 11) {
            Log.d(TAG, "Phone length invalid: ${digitsOnly.length}")
            return false
        }

        // Chấp nhận số bắt đầu bằng 0, 84, hoặc các đầu số di động phổ biến
        val isValid = digitsOnly.startsWith("0") ||
                     digitsOnly.startsWith("84") ||
                     digitsOnly.startsWith("3") ||
                     digitsOnly.startsWith("5") ||
                     digitsOnly.startsWith("7") ||
                     digitsOnly.startsWith("8") ||
                     digitsOnly.startsWith("9")

        Log.d(TAG, "Phone validation result: $isValid")
        return isValid
    }
    
    private fun formatPhoneNumber(phone: String): String {
        val digitsOnly = phone.replace(Regex("[^0-9]"), "")
        
        // Nếu số điện thoại không bắt đầu bằng 0 và có 9 chữ số, thêm 0 vào đầu
        if (!digitsOnly.startsWith("0") && digitsOnly.length == 9) {
            return "0$digitsOnly"
        }
        
        // Nếu số bắt đầu bằng 84, chuyển 84 thành 0
        if (digitsOnly.startsWith("84") && digitsOnly.length >= 10) {
            return "0${digitsOnly.substring(2)}"
        }
        
        return digitsOnly
    }
    
    private fun detectNameAndPhone(row: Row): Pair<String, String> {
        var name = ""
        var phone = ""

        // Log all cell contents for debugging
        val cellContents = mutableListOf<String>()
        for (col in 0 until minOf(row.lastCellNum.toInt(), 10)) {
            try {
                val cellContent = getCellValueAsString(row.getCell(col))
                cellContents.add("Col$col: '$cellContent'")

                if (cellContent.isEmpty()) continue

                Log.d(TAG, "Checking cell $col: '$cellContent'")

                if (isPhoneNumber(cellContent)) {
                    // Format phone number before saving
                    phone = formatPhoneNumber(cellContent)
                    Log.d(TAG, "Found phone: '$cellContent' -> formatted: '$phone'")
                } else if (name.isEmpty() && isValidName(cellContent)) {
                    name = cellContent
                    Log.d(TAG, "Found name: '$cellContent'")
                }
            } catch (e: Exception) {
                cellContents.add("Col$col: ERROR")
                Log.e(TAG, "Error reading cell $col: ${e.message}")
            }
        }

        Log.d(TAG, "Row ${row.rowNum}: ${cellContents.joinToString(", ")}")
        Log.d(TAG, "Detected - Name: '$name', Phone: '$phone'")

        return Pair(name, phone)
    }
    
    private fun isValidName(text: String): Boolean {
        Log.d(TAG, "Checking name '$text'")

        // Tên không được chỉ toàn số
        if (text.matches(Regex("^[0-9]+$"))) {
            Log.d(TAG, "Name rejected: all digits")
            return false
        }

        // Tên phải có ít nhất 2 ký tự
        if (text.length < 2) {
            Log.d(TAG, "Name rejected: too short")
            return false
        }

        // Tên không được chứa quá nhiều số (cho phép linh hoạt hơn)
        val digitCount = text.count { it.isDigit() }
        val isValid = digitCount <= text.length / 2

        Log.d(TAG, "Name validation - digits: $digitCount/${text.length}, valid: $isValid")
        return isValid
    }
}
