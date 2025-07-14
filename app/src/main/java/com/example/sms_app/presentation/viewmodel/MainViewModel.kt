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
        _customers.postValue(smsRepository.getCustomers().sortedBy { it.id })
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
        smsRepository.saveCustomers(listOf())
        android.util.Log.d("MainViewModel", "✅ Deleted all customers")

        // Xóa session backup và countdown data khi xóa tất cả khách hàng
        val sessionBackup = com.example.sms_app.data.SessionBackup(getApplication())
        sessionBackup.clearActiveSession()
        smsRepository.clearCountdownData()
        android.util.Log.d("MainViewModel", "🗑️ Cleared session backup and countdown data on delete all")

        sync()
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
                    
                    // Merge và loại bỏ duplicate theo số điện thoại
                    val allCustomers = (currentCustomers + importedCustomers)
                    val uniqueCustomers = allCustomers.groupBy { it.phoneNumber }
                        .map { (phoneNumber, duplicates) ->
                            if (duplicates.size > 1) {
                                android.util.Log.d("MainViewModel", "📱 Found duplicates for phone: $phoneNumber, keeping the latest one")
                                // Giữ lại customer mới nhất (từ import)
                                duplicates.maxBy { customer ->
                                    // Ưu tiên customer từ import (isSelected = true)
                                    if (customer.isSelected) 1 else 0
                                }
                            } else {
                                duplicates.first()
                            }
                        }
                    
                    val duplicatesRemoved = allCustomers.size - uniqueCustomers.size
                    val actualNewCustomers = uniqueCustomers.size - currentCustomers.size
                    
                    smsRepository.saveCustomers(uniqueCustomers)
                    
                    val message = when {
                        duplicatesRemoved > 0 && actualNewCustomers > 0 -> 
                            "Đã nhập ${actualNewCustomers} khách hàng mới và loại bỏ ${duplicatesRemoved} khách hàng trùng lặp"
                        duplicatesRemoved > 0 && actualNewCustomers == 0 -> 
                            "Đã cập nhật ${duplicatesRemoved} khách hàng trùng lặp"
                        actualNewCustomers > 0 -> 
                            "Đã nhập thành công ${actualNewCustomers} khách hàng từ Excel"
                        else -> 
                            "Tất cả khách hàng trong file đã tồn tại"
                    }
                    
                    onMessage(message)
                    android.util.Log.d("MainViewModel", "📋 Import summary: Total=${uniqueCustomers.size}, New=${actualNewCustomers}, Duplicates removed=${duplicatesRemoved}")
                } else {
                    onMessage("Không tìm thấy khách hàng hợp lệ trong file Excel")
                }
            }.onFailure {
                onMessage("Lỗi nhập Excel: ${it.message}")
            }

            sync()
        }

    fun selectAll() = viewModelScope.launch(Dispatchers.IO) {
        val allCustomers = smsRepository.getCustomers()
        android.util.Log.d("MainViewModel", "🎯 SelectAll called - Total customers: ${allCustomers.size}")
        
        val updatedCustomers = allCustomers.map { it.copy(isSelected = true) }
        smsRepository.saveCustomers(updatedCustomers)
        
        android.util.Log.d("MainViewModel", "✅ Selected all ${updatedCustomers.size} customers")
        updatedCustomers.forEach { customer ->
            android.util.Log.d("MainViewModel", "✅ Customer: ${customer.name} - Selected: ${customer.isSelected}")
        }
        
        sync()
    }
    
    fun unselectAll() = viewModelScope.launch(Dispatchers.IO) {
        val allCustomers = smsRepository.getCustomers()
        android.util.Log.d("MainViewModel", "🎯 UnselectAll called - Total customers: ${allCustomers.size}")
        
        val updatedCustomers = allCustomers.map { it.copy(isSelected = false) }
        smsRepository.saveCustomers(updatedCustomers)
        
        android.util.Log.d("MainViewModel", "✅ Unselected all ${updatedCustomers.size} customers")
        updatedCustomers.forEach { customer ->
            android.util.Log.d("MainViewModel", "❌ Customer: ${customer.name} - Selected: ${customer.isSelected}")
        }
        
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
}