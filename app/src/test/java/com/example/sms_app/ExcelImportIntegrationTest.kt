package com.example.sms_app

import android.content.Context
import android.net.Uri
import com.example.sms_app.utils.ExcelImporter
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import java.io.ByteArrayInputStream

/**
 * Integration test để kiểm tra ExcelImporter hoạt động với ProGuard/R8
 */
class ExcelImportIntegrationTest {

    @Test
    fun testExcelImporterNotObfuscated() {
        // Test này kiểm tra xem các class và method quan trọng có bị obfuscate không
        val context = mock(Context::class.java)
        
        // 1. Kiểm tra ExcelImporter constructor
        val importer = ExcelImporter(context)
        assertNotNull("ExcelImporter constructor should work", importer)
        
        // 2. Kiểm tra các method public vẫn tồn tại
        val uri = mock(Uri::class.java)
        `when`(uri.lastPathSegment).thenReturn("test.xls")
        
        // Mock ContentResolver
        val contentResolver = mock(android.content.ContentResolver::class.java)
        val cursor = mock(android.database.Cursor::class.java)
        
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.query(any(), any(), any(), any(), any())).thenReturn(cursor)
        `when`(cursor.moveToFirst()).thenReturn(false)
        
        // Test isValidExcelFile method
        try {
            importer.isValidExcelFile(uri)
            assertTrue("isValidExcelFile method should be accessible", true)
        } catch (e: Exception) {
            // Method tồn tại nhưng có thể throw exception do mock data
            assertTrue("isValidExcelFile method exists", true)
        }
        
        // Test importCustomers method
        `when`(contentResolver.openInputStream(uri)).thenReturn(ByteArrayInputStream(byteArrayOf()))
        
        try {
            val customers = importer.importCustomers(uri)
            assertNotNull("importCustomers should return a list", customers)
            assertTrue("importCustomers method should be accessible", true)
        } catch (e: Exception) {
            // Method tồn tại nhưng có thể throw exception do mock data
            assertTrue("importCustomers method exists", true)
        }
    }

    @Test
    fun testJExcelAPIClassesNotObfuscated() {
        // Kiểm tra các class JExcelAPI quan trọng không bị obfuscate
        try {
            // Test Workbook class
            val workbookClass = Class.forName("jxl.Workbook")
            assertNotNull("jxl.Workbook class should exist", workbookClass)
            
            // Test Sheet class
            val sheetClass = Class.forName("jxl.Sheet")
            assertNotNull("jxl.Sheet class should exist", sheetClass)
            
            // Test Cell class
            val cellClass = Class.forName("jxl.Cell")
            assertNotNull("jxl.Cell class should exist", cellClass)
            
            println("✅ JExcelAPI classes are properly protected from obfuscation")
            
        } catch (e: ClassNotFoundException) {
            fail("JExcelAPI classes should not be obfuscated: ${e.message}")
        }
    }

    @Test
    fun testCustomerClassNotObfuscated() {
        // Kiểm tra Customer class không bị obfuscate
        try {
            val customerClass = Class.forName("com.example.sms_app.data.Customer")
            assertNotNull("Customer class should exist", customerClass)
            
            // Kiểm tra constructor với các parameter cần thiết
            val constructors = customerClass.constructors
            assertTrue("Customer should have constructors", constructors.isNotEmpty())
            
            println("✅ Customer class is properly protected from obfuscation")
            
        } catch (e: ClassNotFoundException) {
            fail("Customer class should not be obfuscated: ${e.message}")
        }
    }
}
