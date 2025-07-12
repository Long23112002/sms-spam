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
import com.example.sms_app.MainActivity.SmsProgress
import com.example.sms_app.data.Customer
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.service.SmsService
import com.example.sms_app.utils.ExcelImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.plus

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val smsRepository: SmsRepository
) : AndroidViewModel(application) {
    private val _customers = MutableLiveData<List<Customer>>()
    val customers: LiveData<List<Customer>> = _customers

    private val smsProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModelScope.launch(Dispatchers.IO) {
                when (intent?.action) {
                    SmsService.ACTION_CUSTOMER_DELETED -> {
                        sync()
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
        sync()
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        smsRepository.saveCustomers(listOf())
        sync()
    }

    fun handleExcelFile(uri: Uri, onMessage: (String) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            val application = getApplication<Application>()
            onMessage("Đang nhập dữ liệu từ Excel...")
            runCatching {
                val excelImporter = ExcelImporter(application)
                val importedCustomers = excelImporter.importCustomers(uri)

                if (importedCustomers.isNotEmpty()) {
                    smsRepository.apply {
                        saveCustomers(
                            getCustomers() + importedCustomers
                        )
                    }
                    onMessage("Đã nhập thành công ${importedCustomers.size} khách hàng từ Excel")
                } else {
                    onMessage("Không tìm thấy khách hàng hợp lệ trong file Excel")
                }
            }.onFailure {
                onMessage("Lỗi nhập Excel: ${it.message}")
            }

            sync()
        }

    fun selectAll() = viewModelScope.launch(Dispatchers.IO) {
        smsRepository.apply {
            saveCustomers(
                getCustomers().map { it.copy(isSelected = true) }
            )
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
}