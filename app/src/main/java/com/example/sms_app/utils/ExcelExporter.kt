package com.example.sms_app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.sms_app.data.Customer
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExcelExporter(private val context: Context) {
    
    companion object {
        private const val TAG = "ExcelExporter"
    }
    
    /**
     * Export customers to Excel file
     */
    fun exportCustomers(customers: List<Customer>, uri: Uri): Boolean {
        Log.d(TAG, "Starting Excel export to $uri with ${customers.size} customers")
        
        return try {
            Log.d(TAG, "Opening output stream for $uri")
            val outputStream = context.contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                Log.e(TAG, "Failed to open output stream for $uri")
                return false
            }

            Log.d(TAG, "Creating Excel workbook...")
            
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Khách hàng")
            
            // Tạo header style
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.LIGHT_BLUE.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                setBorderTop(BorderStyle.THIN)
                setBorderBottom(BorderStyle.THIN)
                setBorderLeft(BorderStyle.THIN)
                setBorderRight(BorderStyle.THIN)
            }
            
            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            headerStyle.setFont(headerFont)
            
            // Tạo data style
            val dataStyle = workbook.createCellStyle().apply {
                setBorderTop(BorderStyle.THIN)
                setBorderBottom(BorderStyle.THIN)
                setBorderLeft(BorderStyle.THIN)
                setBorderRight(BorderStyle.THIN)
            }
            
            // Bỏ header row - bắt đầu trực tiếp từ dữ liệu
            
            // Thêm dữ liệu khách hàng (bắt đầu từ row 0, không có header)
            customers.forEachIndexed { rowIndex, customer ->
                val row = sheet.createRow(rowIndex)
                
                // Tên khách hàng
                row.createCell(0).apply {
                    setCellValue(customer.name)
                    cellStyle = dataStyle
                }
                
                // Số CMND/CCCD
                row.createCell(1).apply {
                    setCellValue(customer.idNumber)
                    cellStyle = dataStyle
                }
                
                // Số điện thoại
                row.createCell(2).apply {
                    setCellValue(customer.phoneNumber)
                    cellStyle = dataStyle
                }
                
                // Địa chỉ
                row.createCell(3).apply {
                    setCellValue(customer.address)
                    cellStyle = dataStyle
                }
                
                // Tùy chọn 1-5
                row.createCell(4).apply {
                    setCellValue(customer.option1)
                    cellStyle = dataStyle
                }
                
                row.createCell(5).apply {
                    setCellValue(customer.option2)
                    cellStyle = dataStyle
                }
                
                row.createCell(6).apply {
                    setCellValue(customer.option3)
                    cellStyle = dataStyle
                }
                
                row.createCell(7).apply {
                    setCellValue(customer.option4)
                    cellStyle = dataStyle
                }
                
                row.createCell(8).apply {
                    setCellValue(customer.option5)
                    cellStyle = dataStyle
                }

                // Chỉ export đến cột "Tùy chọn 5" - bỏ các cột khác
            }
            
            // Đặt width cố định cho 9 cột (từ Tên đến Tùy chọn 5)
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
            
            // Ghi file
            Log.d(TAG, "Writing Excel file...")
            workbook.write(outputStream)
            workbook.close()
            outputStream.close()

            Log.d(TAG, "✅ Successfully exported ${customers.size} customers to Excel")
            true

        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "❌ NoClassDefFoundError - Android compatibility issue: ${e.message}", e)
            false
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "❌ ClassNotFoundException - Missing dependency: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error exporting to Excel: ${e.message}", e)
            false
        }
    }
    
    /**
     * Generate default filename for export
     */
    fun generateDefaultFilename(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "data_$timestamp.xlsx"
    }
}
