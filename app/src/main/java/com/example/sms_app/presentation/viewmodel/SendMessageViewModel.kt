package com.example.sms_app.presentation.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sms_app.MainActivity.SmsProgress
import com.example.sms_app.data.Customer
import com.example.sms_app.data.MessageTemplate
import com.example.sms_app.data.SmsRepository
import com.example.sms_app.presentation.component.formatDuration
import com.example.sms_app.service.SmsService
import com.example.sms_app.utils.SimInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SendMessageViewModel @Inject constructor(
    application: Application,
    private val smsRepository: SmsRepository,
) : AndroidViewModel(application) {

    private val _progress = MutableLiveData<SmsProgress>()
    val progress: LiveData<SmsProgress> = _progress

    private val _completion = MutableLiveData<String>()
    val completion: LiveData<String> = _completion

    private val _isSending = MutableLiveData<Boolean>()
    val isSending: LiveData<Boolean> = _isSending

    private val _millisUntilFinished = MutableLiveData<Long>(0)
    val millisUntilFinished: LiveData<Long> = _millisUntilFinished

    private val smsProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModelScope.launch(Dispatchers.IO) {
                Timber.tag(TAG).i("onReceive: action ${intent?.action}")
                when (intent?.action) {
                    SmsService.ACTION_PROGRESS_UPDATE -> {
                        intent.let {
                            val progress = it.getIntExtra(SmsService.EXTRA_PROGRESS, 0)
                            val total = it.getIntExtra(SmsService.EXTRA_TOTAL, 0)
                            val message = it.getStringExtra(SmsService.EXTRA_MESSAGE) ?: ""
                            val p = SmsProgress(progress, total, message)
                            Timber.tag(TAG).i("onReceive: SmsProgress $p")
                            _progress.postValue(p)
                        }
                    }

                    SmsService.ACTION_SMS_COMPLETED -> {
                        val message =
                            intent.getStringExtra(SmsService.EXTRA_MESSAGE) ?: "Hoàn thành gửi SMS"

                        _completion.postValue(message)
                    }
                }
            }
        }
    }

    init {
        IntentFilter().apply {
            addAction(SmsService.ACTION_PROGRESS_UPDATE)
            addAction(SmsService.ACTION_SMS_COMPLETED)
        }.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.registerReceiver(smsProgressReceiver, it, Context.RECEIVER_NOT_EXPORTED)
            } else {
                application.registerReceiver(smsProgressReceiver, it)
            }
        }

        val countDownTimer: CountDownTimer = object : CountDownTimer(smsRepository.getAppSettings().intervalBetweenSmsSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                viewModelScope.launch(Dispatchers.IO) {
                    _millisUntilFinished.postValue(millisUntilFinished)
                }
            }

            override fun onFinish() {
                this.start()
            }
        }
        countDownTimer.start()
    }


    fun sendMessage(messageTemplate: MessageTemplate, simInfo: SimInfo) =
        viewModelScope.launch(Dispatchers.IO) {
            _isSending.postValue(true)

            val application = getApplication<Application>()
            smsRepository.setSelectedSim(simInfo.subscriptionId)
            val intent = smsRepository.getAppSettings().let {
                Intent(application, SmsService::class.java).apply {
                    putExtra(SmsService.EXTRA_TEMPLATE_ID, messageTemplate.id)
                    putExtra(SmsService.EXTRA_INTERVAL_SECONDS, it.intervalBetweenSmsSeconds)
                    putExtra(SmsService.EXTRA_MAX_RETRY, it.maxRetryAttempts)
                    putExtra(SmsService.EXTRA_RETRY_DELAY, it.retryDelaySeconds)
                }
            }
            ContextCompat.startForegroundService(application, intent)
        }

    fun stop() = viewModelScope.launch(Dispatchers.IO) {
        _isSending.postValue(false)

        getApplication<Application>().let {
            it.stopService(
                Intent(it, SmsService::class.java).apply {
                    setAction("STOP_SMS_SERVICE")
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(smsProgressReceiver)
    }

    companion object {
        private const val TAG = "SendMessageViewModel"
    }
}