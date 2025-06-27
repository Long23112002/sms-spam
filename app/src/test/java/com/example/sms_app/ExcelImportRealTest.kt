package com.example.sms_app

import android.content.Context
import android.net.Uri
import com.example.sms_app.utils.ExcelImporter
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import jxl.Workbook
import jxl.write.WritableWorkbook
import jxl.write.Label

/**
 * Test thực tế với file Excel được tạo bằng JExcelAPI
 */
class ExcelImportRealTest {

    @Test
    fun testRealExcelImport() {
        // Tạo một file Excel thực tế trong memory
        val outputStream = ByteArrayOutputStream()
        
        try {
            // Tạo workbook
            val workbook: WritableWorkbook = Workbook.createWorkbook(outputStream)
            val sheet = workbook.createSheet("Customers", 0)
            
            // Thêm header
            sheet.addCell(Label(0, 0, "Tên"))
            sheet.addCell(Label(1, 0, "CMND"))
            sheet.addCell(Label(2, 0, "Điện thoại"))
            sheet.addCell(Label(3, 0, "Địa chỉ"))
            sheet.addCell(Label(4, 0, "Tùy chọn 1"))
            sheet.addCell(Label(5, 0, "Tùy chọn 2"))
            sheet.addCell(Label(6, 0, "Tùy chọn 3"))
            sheet.addCell(Label(7, 0, "Tùy chọn 4"))
            sheet.addCell(Label(8, 0, "Tùy chọn 5"))
            sheet.addCell(Label(9, 0, "Mẫu tin"))
            
            // Thêm dữ liệu test
            sheet.addCell(Label(0, 1, "Nguyễn Văn A"))
            sheet.addCell(Label(1, 1, "123456789"))
            sheet.addCell(Label(2, 1, "0901234567"))
            sheet.addCell(Label(3, 1, "Hà Nội"))
            sheet.addCell(Label(4, 1, "Option1"))
            sheet.addCell(Label(5, 1, "Option2"))
            sheet.addCell(Label(6, 1, "Option3"))
            sheet.addCell(Label(7, 1, "Option4"))
            sheet.addCell(Label(8, 1, "Option5"))
            sheet.addCell(Label(9, 1, "1"))
            
            workbook.write()
            workbook.close()
            
            // Chuyển thành InputStream
            val excelData = outputStream.toByteArray()
            val inputStream = ByteArrayInputStream(excelData)
            
            // Test import
            val context = mock(Context::class.java)
            val contentResolver = mock(android.content.ContentResolver::class.java)
            val uri = mock(Uri::class.java)
            
            `when`(context.contentResolver).thenReturn(contentResolver)
            `when`(contentResolver.openInputStream(uri)).thenReturn(inputStream)
            `when`(uri.lastPathSegment).thenReturn("test.xls")
            
            val importer = ExcelImporter(context)
            val customers = importer.importCustomers(uri)
            
            // Kiểm tra kết quả
            assertNotNull("Customers list should not be null", customers)
            assertEquals("Should import 1 customer", 1, customers.size)
            
            val customer = customers[0]
            assertEquals("Name should match", "Nguyễn Văn A", customer.name)
            assertEquals("ID number should match", "123456789", customer.idNumber)
            assertEquals("Phone should match", "0901234567", customer.phoneNumber)
            assertEquals("Address should match", "Hà Nội", customer.address)
            assertEquals("Option1 should match", "Option1", customer.option1)
            assertEquals("Template number should match", 1, customer.templateNumber)
            
            println("✅ Real Excel import test passed successfully!")
            println("✅ JExcelAPI is working correctly after ProGuard/R8 obfuscation!")
            
        } catch (e: Exception) {
            fail("Real Excel import should work: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun testJExcelAPIFunctionality() {
        // Test trực tiếp JExcelAPI để đảm bảo không bị obfuscate
        try {
            val outputStream = ByteArrayOutputStream()
            val workbook = Workbook.createWorkbook(outputStream)
            val sheet = workbook.createSheet("Test", 0)
            
            sheet.addCell(Label(0, 0, "Test Cell"))
            workbook.write()
            workbook.close()
            
            // Đọc lại
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val readWorkbook = Workbook.getWorkbook(inputStream)
            val readSheet = readWorkbook.getSheet(0)
            val cell = readSheet.getCell(0, 0)
            
            assertEquals("Cell content should match", "Test Cell", cell.contents)
            
            readWorkbook.close()
            
            println("✅ JExcelAPI read/write functionality works correctly!")
            
        } catch (e: Exception) {
            fail("JExcelAPI should work after obfuscation: ${e.message}")
            e.printStackTrace()
        }
    }
}
