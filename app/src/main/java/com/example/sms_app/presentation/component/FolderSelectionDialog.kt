package com.example.sms_app.presentation.component

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val lastModified: String,
    val size: String = ""
)

@Composable
fun FolderSelectionDialog(
    onDismiss: () -> Unit,
    onFileSelected: (String) -> Unit
) {
    val context = LocalContext.current

    // State để track thư mục hiện tại
    var currentPath by remember {
        mutableStateOf(Environment.getExternalStorageDirectory().absolutePath)
    }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Hàm để load files từ thư mục
    fun loadFiles(path: String) {
        isLoading = true
        errorMessage = null

        try {
            val directory = File(path)

            if (!directory.exists()) {
                errorMessage = "Thư mục không tồn tại"
                files = emptyList()
                isLoading = false
                return
            }

            if (!directory.canRead()) {
                errorMessage = "Không có quyền truy cập thư mục"
                files = emptyList()
                isLoading = false
                return
            }

            if (directory.isDirectory) {
                val fileList = directory.listFiles()?.mapNotNull { file ->
                    try {
                        // Bỏ qua các file ẩn
                        if (file.name.startsWith(".")) return@mapNotNull null

                        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                        val lastModified = dateFormat.format(Date(file.lastModified()))
                        val size = if (file.isFile) {
                            val sizeInBytes = file.length()
                            when {
                                sizeInBytes < 1024 -> "${sizeInBytes}B"
                                sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024}KB"
                                else -> "${sizeInBytes / (1024 * 1024)}MB"
                            }
                        } else ""

                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = file.isDirectory,
                            lastModified = lastModified,
                            size = size
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FolderDialog", "Error processing file: ${file.name}", e)
                        null
                    }
                }?.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()

                files = fileList

                if (fileList.isEmpty()) {
                    errorMessage = "Thư mục trống"
                }
            } else {
                errorMessage = "Đây không phải là thư mục"
                files = emptyList()
            }
        } catch (e: SecurityException) {
            android.util.Log.e("FolderDialog", "Security exception", e)
            errorMessage = "Không có quyền truy cập"
            files = emptyList()
        } catch (e: Exception) {
            android.util.Log.e("FolderDialog", "Error loading files", e)
            errorMessage = "Lỗi khi tải danh sách file: ${e.message}"
            files = emptyList()
        }

        isLoading = false
    }

    // Load files khi khởi tạo
    LaunchedEffect(Unit) {
        loadFiles(currentPath)
    }

    // Load files khi path thay đổi
    LaunchedEffect(currentPath) {
        loadFiles(currentPath)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header với đường dẫn hiện tại
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Nút Back
                        if (currentPath != Environment.getExternalStorageDirectory().absolutePath) {
                            IconButton(
                                onClick = {
                                    val parentFile = File(currentPath).parentFile
                                    if (parentFile != null && parentFile.exists()) {
                                        currentPath = parentFile.absolutePath
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF2196F3)
                                )
                            }
                        }

                        Text(
                            text = "Chọn tệp tin",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Hiển thị đường dẫn hiện tại
                    Text(
                        text = currentPath.replace(Environment.getExternalStorageDirectory().absolutePath, ""),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Divider(color = Color.Gray, thickness = 1.dp)

                // File list
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Đang tải...", color = Color.Gray)
                            }
                        }
                    }

                    errorMessage != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = errorMessage!!,
                                    color = Color.Red,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { loadFiles(currentPath) }
                                ) {
                                    Text("Thử lại")
                                }
                            }
                        }
                    }

                    files.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Thư mục trống",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(files) { file ->
                                FileItemRow(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) {
                                            currentPath = file.path
                                        } else {
                                            // Chỉ cho phép chọn file Excel
                                            if (file.name.endsWith(".xls", true) ||
                                                file.name.endsWith(".xlsx", true)) {
                                                onFileSelected(file.path)
                                            }
                                        }
                                    }
                                )

                                Divider(
                                    color = Color.LightGray,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
                
                // Footer với nút Hủy bỏ
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text(
                            text = "Hủy bỏ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(
    file: FileItem,
    onClick: () -> Unit
) {
    val isExcelFile = file.name.endsWith(".xls", true) || file.name.endsWith(".xlsx", true)
    val canSelect = file.isDirectory || isExcelFile

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canSelect) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .let { modifier ->
                if (!canSelect) modifier.background(Color(0xFFF0F0F0))
                else modifier
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File/Folder icon
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = when {
                file.isDirectory -> Color(0xFF2196F3)
                isExcelFile -> Color(0xFF4CAF50)
                else -> Color.Gray
            },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // File/Folder info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                fontSize = 16.sp,
                color = if (canSelect) Color.Black else Color.Gray
            )

            if (!file.isDirectory && file.size.isNotEmpty()) {
                Text(
                    text = file.size,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Last modified date
        Text(
            text = file.lastModified,
            fontSize = 12.sp,
            color = Color(0xFF2196F3),
            textAlign = TextAlign.End
        )
    }
}
