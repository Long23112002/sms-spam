package com.example.sms_app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sms_app.data.Customer
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.data.SessionBackup
import com.example.sms_app.presentation.component.CustomerField
import com.example.sms_app.utils.isValidPhoneNumber
import com.example.sms_app.utils.validateAndFormatPhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCustomerViewModel @Inject constructor(
    application: Application,
    private val smsRepository: SmsRepository,
) : AndroidViewModel(application) {

    fun verify(strings: List<String>, onSuccess: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val phoneNumber = CustomerField.PhoneNumber.getValue(strings)
        val formattedPhoneNumber = phoneNumber.validateAndFormatPhoneNumber()

        val customer: Customer? = formattedPhoneNumber.isValidPhoneNumber().takeIf { it }?.let {
            Customer(
                id = "customer_${java.util.UUID.randomUUID()}",
                name = CustomerField.Name.getValue(strings),
                idNumber = CustomerField.Id.getValue(strings),
                phoneNumber = formattedPhoneNumber,
                address = CustomerField.Address.getValue(strings),
                option1 = CustomerField.Option1.getValue(strings),
                option2 = CustomerField.Option2.getValue(strings),
                option3 = CustomerField.Option3.getValue(strings),
                option4 = CustomerField.Option4.getValue(strings),
                option5 = CustomerField.Option5.getValue(strings),
                templateNumber = CustomerField.Pattern.getValue(strings).toIntOrNull() ?: 1
            )
        }

        customer?.let {
            smsRepository.apply {
                saveCustomers(
                    getCustomers().toMutableList().also { l ->
                        l.add(it)
                    }
                )

                // Xóa session backup khi thêm khách hàng mới
                val sessionBackup = SessionBackup(getApplication())
                sessionBackup.clearActiveSession()
                clearCountdownData()
                android.util.Log.d("AddCustomerViewModel", "🗑️ Cleared session backup and countdown data on customer add")

                onSuccess()
            }
        }
    }

    fun CustomerField.getValue(strings: List<String>) = strings[this.ordinal].trim()
}