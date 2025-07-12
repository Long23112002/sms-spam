package com.example.sms_app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.data.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatternViewModel @Inject constructor(
    application: Application,
    private val smsRepository: SmsRepository
) : AndroidViewModel(application) {

    private val _messageTemplate = MutableLiveData<List<MessageTemplate>>()
    val messageTemplate: LiveData<List<MessageTemplate>> = _messageTemplate

    private val _selectedSim = MutableLiveData<Int>()
    val selectedSim: LiveData<Int> = _selectedSim

    private val _default = MutableLiveData<Int>()
    val default: LiveData<Int> = _default

    init {
        sync()
    }

    fun sync() = viewModelScope.launch(Dispatchers.IO) {
        smsRepository.apply {
            _messageTemplate.postValue(getMessageTemplates().sortedBy { it.id })
            _default.postValue(getDefaultTemplate())
            _selectedSim.postValue(getSelectedSim())
        }
    }

    fun saveTemplate(string: String, i: Int) = viewModelScope.launch(Dispatchers.IO) {
        smsRepository.apply {
            val list = getMessageTemplates().filter { it.id != i }.toMutableList().apply {
                add(MessageTemplate(i, string, "Template $i"))
            }

            saveMessageTemplates(list)
            setDefaultTemplate(i)
        }
        sync()
    }
}