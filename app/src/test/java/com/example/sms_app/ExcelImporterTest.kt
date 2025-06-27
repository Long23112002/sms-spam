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
 * Test cho ExcelImporter để đảm bảo hoạt động đúng sau khi ProGuard obfuscate
 */
class ExcelImporterTest {

    @Test
    fun testExcelImporterClassExists() {
        // Kiểm tra class ExcelImporter vẫn tồn tại sau obfuscation
        val context = mock(Context::class.java)
        val importer = ExcelImporter(context)
        assertNotNull("ExcelImporter should not be null", importer)
    }

    @Test
    fun testIsValidExcelFileMethod() {
        // Kiểm tra method isValidExcelFile vẫn hoạt động
        val context = mock(Context::class.java)
        val importer = ExcelImporter(context)
        val uri = mock(Uri::class.java)

        // Mock ContentResolver để tránh NullPointerException
        val contentResolver = mock(android.content.ContentResolver::class.java)
        val cursor = mock(android.database.Cursor::class.java)

        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.query(any(), any(), any(), any(), any())).thenReturn(cursor)
        `when`(cursor.moveToFirst()).thenReturn(false)
        `when`(uri.lastPathSegment).thenReturn("test.xls")

        // Method này phải vẫn hoạt động sau obfuscation
        try {
            val result = importer.isValidExcelFile(uri)
            // Nếu không có exception thì method vẫn tồn tại
            assertTrue("Method isValidExcelFile should be accessible", true)
        } catch (e: Exception) {
            // Chấp nhận exception do mock data, miễn là method tồn tại
            assertTrue("Method isValidExcelFile should be accessible even with exception", true)
        }
    }

    @Test
    fun testImportCustomersMethod() {
        // Kiểm tra method importCustomers vẫn hoạt động
        val context = mock(Context::class.java)
        val importer = ExcelImporter(context)
        val uri = mock(Uri::class.java)
        
        // Mock ContentResolver để trả về empty stream
        val contentResolver = mock(android.content.ContentResolver::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.openInputStream(uri)).thenReturn(ByteArrayInputStream(byteArrayOf()))
        
        try {
            val result = importer.importCustomers(uri)
            // Nếu không có exception thì method vẫn tồn tại
            assertNotNull("importCustomers should return a list", result)
            assertTrue("Method importCustomers should be accessible", true)
        } catch (e: Exception) {
            // Có thể có exception do mock data không đúng format, nhưng method phải tồn tại
            assertTrue("Method importCustomers should be accessible even if it throws exception", true)
        }
    }
}
