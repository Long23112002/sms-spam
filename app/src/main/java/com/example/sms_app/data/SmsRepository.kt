package com.example.sms_app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SmsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_TEMPLATES = "sms_templates"
        private const val KEY_CONFIG = "sms_config"
        private const val KEY_CUSTOMERS = "customers"
        private const val KEY_MESSAGE_TEMPLATES = "message_templates"
        private const val KEY_SMS_COUNT_TODAY = "sms_count_today"
        private const val KEY_LAST_SMS_DATE = "last_sms_date"
        private const val KEY_SELECTED_SIM = "selected_sim"
        private const val KEY_DEFAULT_TEMPLATE = "default_template"
        private const val KEY_APP_SETTINGS = "app_settings"
        private const val MAX_SMS_PER_DAY = 40
    }

    fun saveTemplates(templates: List<SmsTemplate>) {
        val json = gson.toJson(templates)
        prefs.edit().putString(KEY_TEMPLATES, json).apply()
    }

    fun getTemplates(): List<SmsTemplate> {
        val json = prefs.getString(KEY_TEMPLATES, null) ?: return getDefaultTemplates()
        val type = object : TypeToken<List<SmsTemplate>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveConfig(config: SmsConfig) {
        val json = gson.toJson(config)
        prefs.edit().putString(KEY_CONFIG, json).apply()
    }

    fun getConfig(): SmsConfig? {
        val json = prefs.getString(KEY_CONFIG, null) ?: return null
        return gson.fromJson(json, SmsConfig::class.java)
    }

    fun saveCustomers(customers: List<Customer>) {
        val json = gson.toJson(customers)
        prefs.edit().putString(KEY_CUSTOMERS, json).apply()
    }

    fun getCustomers(): List<Customer> {
        val json = prefs.getString(KEY_CUSTOMERS, null) ?: return getDefaultCustomers()
        val type = object : TypeToken<List<Customer>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveMessageTemplates(templates: List<MessageTemplate>) {
        val json = gson.toJson(templates)
        prefs.edit().putString(KEY_MESSAGE_TEMPLATES, json).apply()
    }

    fun getMessageTemplates(): List<MessageTemplate> {
        val json = prefs.getString(KEY_MESSAGE_TEMPLATES, null)
            ?: return TemplateManager.getDefaultTemplates()
        val type = object : TypeToken<List<MessageTemplate>>() {}.type
        return gson.fromJson(json, type)
    }

    fun canSendMoreSms(simId: Int = getSelectedSim()): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastSmsDateKey = "${KEY_LAST_SMS_DATE}_sim_$simId"
        val smsCountKey = "${KEY_SMS_COUNT_TODAY}_sim_$simId"

        val lastSmsDate = prefs.getString(lastSmsDateKey, "")
        val smsCountToday = prefs.getInt(smsCountKey, 0)

        return if (lastSmsDate != today) {
            // New day, reset counter for this SIM
            prefs.edit()
                .putString(lastSmsDateKey, today)
                .putInt(smsCountKey, 0)
                .apply()
            true
        } else {
            smsCountToday < MAX_SMS_PER_DAY
        }
    }

    fun incrementSmsCount(simId: Int = getSelectedSim()): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastSmsDateKey = "${KEY_LAST_SMS_DATE}_sim_$simId"
        val smsCountKey = "${KEY_SMS_COUNT_TODAY}_sim_$simId"

        val lastSmsDate = prefs.getString(lastSmsDateKey, "")
        val smsCountToday = prefs.getInt(smsCountKey, 0)

        if (lastSmsDate != today) {
            // New day, reset counter for this SIM
            prefs.edit()
                .putString(lastSmsDateKey, today)
                .putInt(smsCountKey, 1)
                .apply()
            return true
        } else if (smsCountToday < MAX_SMS_PER_DAY) {
            prefs.edit()
                .putInt(smsCountKey, smsCountToday + 1)
                .apply()
            return true
        }
        return false
    }

    fun getSmsCountToday(simId: Int = getSelectedSim()): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastSmsDateKey = "${KEY_LAST_SMS_DATE}_sim_$simId"
        val smsCountKey = "${KEY_SMS_COUNT_TODAY}_sim_$simId"

        val lastSmsDate = prefs.getString(lastSmsDateKey, "")

        return if (lastSmsDate == today) {
            prefs.getInt(smsCountKey, 0)
        } else {
            0
        }
    }

    fun setSelectedSim(simId: Int) {
        prefs.edit().putInt(KEY_SELECTED_SIM, simId).apply()
    }

    fun getSelectedSim(): Int {
        return prefs.getInt(KEY_SELECTED_SIM, -1) // -1 means default SIM
    }

    fun setDefaultTemplate(templateId: Int) {
        prefs.edit().putInt(KEY_DEFAULT_TEMPLATE, templateId).apply()
    }

    fun getDefaultTemplate(): Int {
        return prefs.getInt(KEY_DEFAULT_TEMPLATE, 1) // Default to template 1
    }

    fun saveAppSettings(settings: AppSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_APP_SETTINGS, json).apply()
        android.util.Log.d("SmsRepository", "Saved app settings: intervalBetweenSmsSeconds=${settings.intervalBetweenSmsSeconds}, maxRetryAttempts=${settings.maxRetryAttempts}, retryDelaySeconds=${settings.retryDelaySeconds}")
    }

    fun getAppSettings(): AppSettings {
        val json = prefs.getString(KEY_APP_SETTINGS, null)
        if (json == null) {
            android.util.Log.d("SmsRepository", "No saved settings found, using default: intervalBetweenSmsSeconds=25")
            return AppSettings()
        }
        return try {
            val settings = gson.fromJson(json, AppSettings::class.java)
            android.util.Log.d("SmsRepository", "Loaded app settings: intervalBetweenSmsSeconds=${settings.intervalBetweenSmsSeconds}, maxRetryAttempts=${settings.maxRetryAttempts}, retryDelaySeconds=${settings.retryDelaySeconds}")
            settings
        } catch (e: Exception) {
            android.util.Log.e("SmsRepository", "Error parsing settings, using default", e)
            AppSettings()
        }
    }

    private fun getDefaultTemplates(): List<SmsTemplate> {
        return listOf()
    }

    private fun getDefaultCustomers(): List<Customer> {
        return listOf()
    }

    fun resetSmsCount(simId: Int) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastSmsDateKey = "${KEY_LAST_SMS_DATE}_sim_$simId"
        val smsCountKey = "${KEY_SMS_COUNT_TODAY}_sim_$simId"
        
        prefs.edit()
            .putString(lastSmsDateKey, today)
            .putInt(smsCountKey, 0)
            .apply()
        
        android.util.Log.d("SmsRepository", "Reset SMS count for SIM $simId")
    }
    
    fun resetAllSimCounts() {
        // Reset cho SIM mặc định (-1)
        resetSmsCount(-1)
        
        // Reset cho SIM 1 và 2 (thường gặp nhất)
        resetSmsCount(0)
        resetSmsCount(1)
        
        android.util.Log.d("SmsRepository", "Reset SMS count for all SIMs")
    }
} 