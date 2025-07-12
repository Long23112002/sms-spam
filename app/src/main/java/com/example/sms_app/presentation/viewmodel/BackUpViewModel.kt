package com.example.sms_app.presentation.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sms_app.data.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BackUpViewModel @Inject constructor(
    application: Application,
    private val smsRepository: SmsRepository
) : AndroidViewModel(application) {

    fun backUp(name: String, onResponse: (String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val n =
                if (name.isEmpty()) SimpleDateFormat("dd-MM-yyyy", java.util.Locale("vi")).format(
                    Date()
                ) else name
            val path = Environment.getExternalStorageDirectory().toPath()
                .resolve(Environment.DIRECTORY_DOWNLOADS).resolve(n)

            val sb = StringBuilder()

            smsRepository.getCustomers().forEach {
                sb.append(it.name).append(",")
                    .append(it.idNumber).append(",")
                    .append(it.phoneNumber).append(",")
                    .append(it.address).append(",")
                    .append(it.option1).append(",")
                    .append(it.option2).append(",")
                    .append(it.option3).append(",")
                    .append(it.option4).append(",")
                    .append(it.option5).append(",")
                    .append(it.templateNumber).append(",")
                    .append(it.carrier).append(",")
                    .append(it.isSelected).append("\n")
            }

            Files.write(path, sb.toString().toByteArray())
            onResponse("Xuất ${smsRepository.getCustomers().size} khách hàng ra Excel thành công!\nLưu tại: $path")
        }.onFailure {
            onResponse("Lỗi xuất Excel: ${it.message}")
        }
    }
}