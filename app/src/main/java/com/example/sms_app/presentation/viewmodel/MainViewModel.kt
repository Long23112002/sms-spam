package com.example.sms_app.presentation.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sms_app.data.Customer
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.service.SmsService
import com.example.sms_app.utils.ExcelImporter
import com.example.sms_app.utils.ExcelExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val smsRepository: SmsRepository,
) : AndroidViewModel(application) {

    private val _customers = MutableLiveData<List<Customer>>()
    val customers: LiveData<List<Customer>> = _customers

    private val smsProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModelScope.launch(Dispatchers.IO) {
                android.util.Log.d("MainViewModel", "📢 Received broadcast: ${intent?.action}")
                when (intent?.action) {
                    SmsService.ACTION_CUSTOMER_DELETED -> {
                        val customerId = intent.getStringExtra(SmsService.EXTRA_CUSTOMER_ID)
                        val message = intent.getStringExtra(SmsService.EXTRA_MESSAGE) ?: ""
                        android.util.Log.d("MainViewModel", "🗑️ Customer deleted broadcast received: ID=$customerId, Message=$message")
                        sync()
                        android.util.Log.d("MainViewModel", "🔄 Sync completed after customer deletion")
                    }
                }
            }
        }
    }

    init {
        IntentFilter().apply {
            addAction(SmsService.ACTION_CUSTOMER_DELETED)
        }.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.registerReceiver(smsProgressReceiver, it, Context.RECEIVER_NOT_EXPORTED)
            } else {
                application.registerReceiver(smsProgressReceiver, it)
            }
        }
        sync()
    }

    fun sync() = viewModelScope.launch(Dispatchers.IO) {
        // Không sort để giữ nguyên thứ tự import từ Excel
        _customers.postValue(smsRepository.getCustomers())
    }

    fun delete(customer: Customer) = viewModelScope.launch(Dispatchers.IO) {
        smsRepository.apply {
            saveCustomers(
                getCustomers().filter { it != customer }
            )
        }

        // Xóa session backup khi xóa khách hàng
        val sessionBackup = com.example.sms_app.data.SessionBackup(getApplication())
        sessionBackup.clearActiveSession()
        android.util.Log.d("MainViewModel", "🗑️ Cleared session backup on customer delete")

        sync()
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        // Lấy danh sách khách hàng hiện tại
        val currentCustomers = smsRepository.getCustomers()

        // Chỉ giữ lại những khách hàng KHÔNG được chọn (isSelected = false)
        val remainingCustomers = currentCustomers.filter { !it.isSelected }

        android.util.Log.d("MainViewModel", "🗑️ Deleting selected customers: ${currentCustomers.size - remainingCustomers.size} customers")
        android.util.Log.d("MainViewModel", "✅ Keeping unselected customers: ${remainingCustomers.size} customers")

        // Lưu lại danh sách khách hàng còn lại
        smsRepository.saveCustomers(remainingCustomers)

        // Chỉ xóa session backup nếu xóa hết tất cả khách hàng
        if (remainingCustomers.isEmpty()) {
            val sessionBackup = com.example.sms_app.data.SessionBackup(getApplication())
            sessionBackup.clearActiveSession()
            smsRepository.clearCountdownData()
            android.util.Log.d("MainViewModel", "🗑️ Cleared session backup and countdown data (no customers left)")
        }

        sync()
    }

    fun exportToExcel(uri: Uri, selectedProvider: String = "all", onMessage: (String) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val application = getApplication<Application>()

            runCatching {
                onMessage("Đang xuất dữ liệu ra Excel...")

                val excelExporter = ExcelExporter(application)
                val allCustomers = smsRepository.getCustomers()

                // Filter customers theo provider giống như trên app
                val customersToExport = if (selectedProvider == "all") {
                    allCustomers
                } else {
                    allCustomers.filter { customer ->
                        customer.carrier.lowercase() == selectedProvider.lowercase()
                    }
                }

                android.util.Log.d("MainViewModel", "📋 Export: Total customers: ${allCustomers.size}, Filtered for '$selectedProvider': ${customersToExport.size}")

                if (customersToExport.isEmpty()) {
                    onMessage("❌ Không có khách hàng nào để xuất cho nhà mạng '$selectedProvider'")
                    return@launch
                }

                val success = excelExporter.exportCustomers(customersToExport, uri)

                if (success) {
                    val message = "✅ Đã xuất thành công ${customersToExport.size} khách hàng ra Excel"
                    onMessage(message)
                    android.util.Log.d("MainViewModel", "📋 Export summary: ${customersToExport.size} customers exported for provider: $selectedProvider")
                } else {
                    onMessage("❌ Lỗi khi xuất file Excel")
                }
            }.onFailure {
                onMessage("❌ Lỗi xuất Excel: ${it.message}")
                android.util.Log.e("MainViewModel", "Error exporting Excel", it)
            }
        }

    fun handleExcelFile(uri: Uri, onMessage: (String) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val application = getApplication<Application>()
            
            // Thêm delay nhỏ để đảm bảo dữ liệu UI đã được flush xuống repository
            kotlinx.coroutines.delay(100) // 100ms delay
            android.util.Log.d("MainViewModel", "⏳ Added 100ms delay to ensure data is flushed to repository")
            
            runCatching {
                onMessage("Đang nhập dữ liệu từ Excel...")
                
                val excelImporter = ExcelImporter(application)
                val importedCustomers = excelImporter.importCustomers(uri)

                if (importedCustomers.isNotEmpty()) {
                    // Xóa session backup và countdown data cũ khi import khách hàng mới
                    val sessionBackup = com.example.sms_app.data.SessionBackup(application)
                    sessionBackup.clearActiveSession()
                    smsRepository.clearCountdownData()
                    android.util.Log.d("MainViewModel", "🗑️ Cleared session backup and countdown data on customer import")

                    // Lấy danh sách khách hàng hiện tại
                    val currentCustomers = smsRepository.getCustomers()

                    // Thêm khách hàng mới theo đúng thứ tự import từ Excel
                    // Import customers sẽ được thêm vào cuối danh sách hiện tại nhưng giữ nguyên thứ tự từ Excel
                    val allCustomers = currentCustomers + importedCustomers
                    val actualNewCustomers = importedCustomers.size

                    smsRepository.saveCustomers(allCustomers)

                    android.util.Log.d("MainViewModel", "📋 Import order preserved: ${importedCustomers.size} customers added in Excel order")

                    val message = "Đã nhập thành công ${actualNewCustomers} khách hàng từ Excel (bao gồm cả trùng lặp)"

                    onMessage(message)
                    android.util.Log.d("MainViewModel", "📋 Import summary: Total=${allCustomers.size}, New=${actualNewCustomers}, Duplicates allowed")
                } else {
                    onMessage("Không tìm thấy khách hàng hợp lệ trong file Excel")
                }
            }.onFailure {
                onMessage("Lỗi nhập Excel: ${it.message}")
            }

            sync()
        }

    fun selectAll(selectedProvider: String = "all") = viewModelScope.launch(Dispatchers.IO) {
        val allCustomers = smsRepository.getCustomers()
        android.util.Log.d("MainViewModel", "🎯 SelectAll called - Provider: $selectedProvider, Total customers: ${allCustomers.size}")

        val updatedCustomers = allCustomers.map { customer ->
            if (selectedProvider == "all" || customer.carrier.lowercase() == selectedProvider.lowercase()) {
                customer.copy(isSelected = true)
            } else {
                customer // Giữ nguyên trạng thái hiện tại
            }
        }

        val selectedCount = updatedCustomers.count { it.isSelected }
        smsRepository.saveCustomers(updatedCustomers)

        android.util.Log.d("MainViewModel", "✅ Selected $selectedCount customers for provider: $selectedProvider")

        sync()
    }
    
    fun unselectAll(selectedProvider: String = "all") = viewModelScope.launch(Dispatchers.IO) {
        val allCustomers = smsRepository.getCustomers()
        android.util.Log.d("MainViewModel", "🎯 UnselectAll called - Provider: $selectedProvider, Total customers: ${allCustomers.size}")

        val updatedCustomers = allCustomers.map { customer ->
            if (selectedProvider == "all" || customer.carrier.lowercase() == selectedProvider.lowercase()) {
                customer.copy(isSelected = false)
            } else {
                customer // Giữ nguyên trạng thái hiện tại
            }
        }

        val unselectedCount = updatedCustomers.count { !it.isSelected }
        smsRepository.saveCustomers(updatedCustomers)

        android.util.Log.d("MainViewModel", "✅ Unselected customers for provider: $selectedProvider")

        sync()
    }

    fun select(customer: Customer) = viewModelScope.launch(Dispatchers.IO) {
        smsRepository.apply {
            saveCustomers(
                getCustomers().filter { it.id != customer.id }.toMutableList().apply {
                    add(customer)
                }
            )
        }
    }
    
    fun updateCustomers(updatedCustomers: List<Customer>) = viewModelScope.launch(Dispatchers.IO) {
        android.util.Log.d("MainViewModel", "🔄 updateCustomers called with ${updatedCustomers.size} customers")
        
        // Debug: Hiển thị trạng thái trước khi lưu
        updatedCustomers.forEach { customer ->
            android.util.Log.d("MainViewModel", "   Before save: ${customer.name} - isSelected: ${customer.isSelected}")
        }
        
        smsRepository.saveCustomers(updatedCustomers)
        android.util.Log.d("MainViewModel", "💾 Data saved to repository, calling sync()")
        
        // Debug: Đọc lại data từ repository để xác minh
        val verifyCustomers = smsRepository.getCustomers()
        android.util.Log.d("MainViewModel", "🔍 Verification read from repository:")
        verifyCustomers.forEach { customer ->
            android.util.Log.d("MainViewModel", "   After save: ${customer.name} - isSelected: ${customer.isSelected}")
        }
        
        sync()
    }
    
    fun getMessageTemplates() = smsRepository.getMessageTemplates()
    fun getDefaultTemplate() = smsRepository.getDefaultTemplate()
    
    fun removeDuplicatesByPhoneNumber() = viewModelScope.launch(Dispatchers.IO) {
        val allCustomers = smsRepository.getCustomers()
        android.util.Log.d("MainViewModel", "🔄 Removing duplicates from ${allCustomers.size} customers")
        
        // Lọc khách hàng duy nhất theo số điện thoại
        // Giữ lại khách hàng đầu tiên cho mỗi số điện thoại
        val uniqueCustomers = allCustomers.groupBy { it.phoneNumber }
            .map { (phoneNumber, duplicates) ->
                if (duplicates.size > 1) {
                    android.util.Log.d("MainViewModel", "📱 Found ${duplicates.size} duplicates for phone: $phoneNumber")
                    duplicates.forEach { customer ->
                        android.util.Log.d("MainViewModel", "   - ${customer.name} (ID: ${customer.id})")
                    }
                    // Giữ lại khách hàng đầu tiên
                    duplicates.first()
                } else {
                    duplicates.first()
                }
            }
        
        val duplicatesRemoved = allCustomers.size - uniqueCustomers.size
        android.util.Log.d("MainViewModel", "✅ Removed $duplicatesRemoved duplicates, ${uniqueCustomers.size} customers remaining")
        
        if (duplicatesRemoved > 0) {
            smsRepository.saveCustomers(uniqueCustomers)
            sync()
        }
    }

    // Hàm tìm kiếm khách hàng
    fun searchCustomers(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allCustomers = smsRepository.getCustomers()

                if (query.isBlank()) {
                    // Nếu không có từ khóa, hiển thị tất cả
                    _customers.postValue(allCustomers)
                    android.util.Log.d("MainViewModel", "🔍 Search cleared, showing all ${allCustomers.size} customers")
                } else {
                    // Tìm kiếm theo tên hoặc số điện thoại
                    val filteredCustomers = allCustomers.filter { customer ->
                        customer.name.contains(query, ignoreCase = true) ||
                        customer.phoneNumber.contains(query, ignoreCase = true)
                    }

                    _customers.postValue(filteredCustomers)
                    android.util.Log.d("MainViewModel", "🔍 Search '$query': found ${filteredCustomers.size}/${allCustomers.size} customers")

                    // Log kết quả tìm kiếm
                    filteredCustomers.forEach { customer ->
                        android.util.Log.d("MainViewModel", "   Found: ${customer.name} (${customer.phoneNumber})")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "❌ Error searching customers", e)
            }
        }
    }

    // Hàm reset về hiển thị tất cả khách hàng
    fun clearSearch() {
        searchCustomers("")
    }
}