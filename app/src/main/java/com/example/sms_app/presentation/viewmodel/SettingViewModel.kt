package com.example.sms_app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sms_app.data.AppSettings
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.presentation.component.SwitchSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    application: Application,
    private val smsRepository: SmsRepository,
) : AndroidViewModel(application) {

    private val _appSettings = MutableLiveData<AppSettings>()
    val appSettings: LiveData<AppSettings> = _appSettings

    init {
        sync()
    }

    fun sync() = viewModelScope.launch(Dispatchers.IO) {
        _appSettings.postValue(smsRepository.getAppSettings())
    }

    fun saveDelay(string: String) = viewModelScope.launch(Dispatchers.IO) {
        string.toIntOrNull()?.let {
            smsRepository.apply {
                saveAppSettings(
                    getAppSettings().copy(intervalBetweenSmsSeconds = it)
                )
            }
        }
        sync()
    }

    fun saveRetry(string: String): Job = viewModelScope.launch(Dispatchers.IO) {
        string.toIntOrNull()?.let {
            smsRepository.apply {
                saveAppSettings(
                    getAppSettings().copy(maxRetryAttempts = it)
                )
            }
        }
        sync()
    }

    fun saveBool(setting: SwitchSetting, bool: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        smsRepository.apply {
            saveAppSettings(getAppSettings().edit(setting, bool))
        }
        sync()
    }
    fun saveNumber(setting: com.example.sms_app.presentation.component.NumSetting, number: Int) = viewModelScope.launch(Dispatchers.IO) {
        smsRepository.apply {
            saveAppSettings(getAppSettings().edit(setting, number))
        }
        sync()
    }

    fun saveCustomerLimit(number: Int) = viewModelScope.launch(Dispatchers.IO) {
        smsRepository.apply {
            saveAppSettings(getAppSettings().copy(customerLimit = number))
        }
        sync()
    }

    fun AppSettings.edit(setting: SwitchSetting, bool: Boolean): AppSettings = when(setting) {
//        SwitchSetting.Vibrate -> copy(enableVibrate = bool)
//        SwitchSetting.Audio -> copy(enableSound = bool)
//        SwitchSetting.FilterHead -> copy(enableFilter = bool)
//        SwitchSetting.Random -> copy(isRandomNumber = bool)
        SwitchSetting.Limit -> copy(isLimitCustomer = bool)
        SwitchSetting.Update -> copy(enableUpdate = bool)
    }
    
    fun AppSettings.edit(setting: com.example.sms_app.presentation.component.NumSetting, number: Int): AppSettings = when(setting) {
        com.example.sms_app.presentation.component.NumSetting.Delay -> copy(intervalBetweenSmsSeconds = number)
        com.example.sms_app.presentation.component.NumSetting.Limit -> copy(maxRetryAttempts = number)
    }

}