package com.example.sms_app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.core.content.edit

class SmsRepository @Inject constructor (@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_TEMPLATES = "sms_templates"
        private const val KEY_CONFIG = "config"
        private const val KEY_CUSTOMERS = "customers"
        private const val KEY_MESSAGE_TEMPLATES = "message_templates"
        private const val KEY_SELECTED_SIM = "selected_sim"
        private const val KEY_DEFAULT_TEMPLATE = "default_template"
        private const val KEY_APP_SETTINGS = "app_settings"
        
        // Chaves para contagem de SMS - Definidas como constantes para garantir consistência
        private const val KEY_LAST_SMS_DATE = "last_sms_date"
        private const val KEY_SMS_COUNT_TODAY = "sms_count_today"
        
        // Chaves para countdown data
        private const val KEY_COUNTDOWN_START_TIME = "countdown_start_time"
        private const val KEY_COUNTDOWN_TOTAL_TIME = "countdown_total_time"
        private const val KEY_COUNTDOWN_CUSTOMER_COUNT = "countdown_customer_count"
        
        // Formato para as chaves específicas de SIM
        private fun getLastSmsDateKey(simId: Int) = "${KEY_LAST_SMS_DATE}_sim_$simId"
        private fun getSmsCountKey(simId: Int) = "${KEY_SMS_COUNT_TODAY}_sim_$simId"
        
        const val MAX_SMS_PER_DAY = 40
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
        val lastSmsDateKey = getLastSmsDateKey(simId)
        val smsCountKey = getSmsCountKey(simId)

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

    /**
     * Força a leitura da contagem de SMS diretamente do SharedPreferences,
     * ignorando qualquer cache e garantindo o valor mais atualizado
     */
    fun forceRefreshSmsCount(simId: Int = getSelectedSim()): Int {
        // Limpar qualquer cache que possa existir
        prefs.edit().apply()
        
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastSmsDateKey = getLastSmsDateKey(simId)
        val smsCountKey = getSmsCountKey(simId)

        // Ler diretamente do SharedPreferences
        val lastSmsDate = prefs.getString(lastSmsDateKey, "")
        val count = prefs.getInt(smsCountKey, 0)
        
        // Debug: Imprimir todos os valores armazenados para este SIM
        debugPrintAllSmsCountValues(simId)
        
        android.util.Log.d("SmsRepository", "🔄 FORCE REFRESHED SMS count for SIM $simId: $count/40")
        
        // Se a data for diferente, resetar a contagem
        if (lastSmsDate != today) {
            val success = prefs.edit()
                .putString(lastSmsDateKey, today)
                .putInt(smsCountKey, 0)
                .commit()
            
            android.util.Log.d("SmsRepository", "Reset SMS count on force refresh, SIM $simId: count = 0, success = $success")
            return 0
        }
        
        return count
    }
    
    fun incrementSmsCount(simId: Int = getSelectedSim()): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastSmsDateKey = getLastSmsDateKey(simId)
        val smsCountKey = getSmsCountKey(simId)

        val lastSmsDate = prefs.getString(lastSmsDateKey, "")
        val smsCountToday = prefs.getInt(smsCountKey, 0)
        
        android.util.Log.d("SmsRepository", "⬆️ Incrementando contagem para SIM $simId: atual = $smsCountToday, data última = '$lastSmsDate', hoje = '$today'")

        if (lastSmsDate != today) {
            // New day, reset counter for this SIM
            val success = prefs.edit()
                .putString(lastSmsDateKey, today)
                .putInt(smsCountKey, 1)
                .commit()  // Using commit() instead of apply() for immediate update
            
            // Verificar se o valor foi realmente salvo
            val newCount = prefs.getInt(smsCountKey, -1)
            
            // Log for debugging
            android.util.Log.d("SmsRepository", "⬆️ Reset SMS count for new day, SIM $simId: count = 1, success = $success, verificado = $newCount")
            
            // Imprimir todos os valores armazenados
            debugPrintAllSmsCountValues(simId)
            
            return 1
        } else if (smsCountToday < MAX_SMS_PER_DAY) {
            val newCount = smsCountToday + 1
            val success = prefs.edit()
                .putString(lastSmsDateKey, today)  // Garantir que a data também está atualizada
                .putInt(smsCountKey, newCount)
                .commit()  // Using commit() instead of apply() for immediate update
            
            // Verificar se o valor foi realmente salvo
            val verifiedCount = prefs.getInt(smsCountKey, -1)
            
            // Log for debugging
            android.util.Log.d("SmsRepository", "⬆️ Incremented SMS count for SIM $simId: new count = $newCount, success = $success, verificado = $verifiedCount")
            
            // Imprimir todos os valores armazenados
            debugPrintAllSmsCountValues(simId)
            
            return newCount
        }
        
        android.util.Log.d("SmsRepository", "⬆️ Não incrementou: SIM $simId já atingiu o limite ou ocorreu um erro")
        return smsCountToday
    }

    fun getSmsCountToday(simId: Int = getSelectedSim()): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastSmsDateKey = getLastSmsDateKey(simId)
        val smsCountKey = getSmsCountKey(simId)

        val lastSmsDate = prefs.getString(lastSmsDateKey, "")
        val count = prefs.getInt(smsCountKey, 0)
        
        // If date is different, reset the count and update the date
        if (lastSmsDate != today) {
            val success = prefs.edit()
                .putString(lastSmsDateKey, today)
                .putInt(smsCountKey, 0)
                .commit()  // Using commit() instead of apply() for immediate update
            
            android.util.Log.d("SmsRepository", "Reset SMS count for new day (in getSmsCountToday), SIM $simId: count = 0, success = $success")
            return 0
        }
        
        // Log the count for debugging
        android.util.Log.d("SmsRepository", "SMS count for SIM $simId today: $count")
        return count
    }

    /**
     * Método de depuração para imprimir todos os valores de contagem de SMS armazenados
     */
    fun debugPrintAllSmsCountValues(simId: Int = getSelectedSim()) {
        val allPrefs = prefs.all
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        android.util.Log.d("SmsRepository", "🔍 DEBUG - Todos os valores para SIM $simId:")
        android.util.Log.d("SmsRepository", "🔍 Data atual: $today")
        
        val lastSmsDateKey = getLastSmsDateKey(simId)
        val smsCountKey = getSmsCountKey(simId)
        
        val lastSmsDate = prefs.getString(lastSmsDateKey, "")
        val count = prefs.getInt(smsCountKey, 0)
        
        android.util.Log.d("SmsRepository", "🔍 Chave data: $lastSmsDateKey = '$lastSmsDate'")
        android.util.Log.d("SmsRepository", "🔍 Chave contagem: $smsCountKey = $count")
        
        // Verificar se há outras chaves relacionadas a este SIM
        allPrefs.forEach { (key, value) ->
            if (key.contains("_sim_$simId")) {
                android.util.Log.d("SmsRepository", "🔍 Outra chave: $key = $value")
            }
        }
        
        // Verificar se há contagens para outros SIMs
        for (otherSimId in -1..5) {
            if (otherSimId != simId) {
                val otherSmsCountKey = getSmsCountKey(otherSimId)
                val otherCount = prefs.getInt(otherSmsCountKey, -1)
                if (otherCount >= 0) {
                    android.util.Log.d("SmsRepository", "🔍 SIM $otherSimId contagem: $otherCount")
                }
            }
        }
    }
    
    /**
     * Método para definir manualmente a contagem de SMS para testes
     */
    fun setManualSmsCount(simId: Int, count: Int): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastSmsDateKey = getLastSmsDateKey(simId)
        val smsCountKey = getSmsCountKey(simId)
        
        val success = prefs.edit()
            .putString(lastSmsDateKey, today)
            .putInt(smsCountKey, count)
            .commit()
        
        android.util.Log.d("SmsRepository", "✏️ Definido manualmente contagem para SIM $simId: $count, sucesso: $success")
        return success
    }

    fun setSelectedSim(simId: Int) {
        prefs.edit().putInt(KEY_SELECTED_SIM, simId).apply()
    }

    fun getSelectedSim(): Int {
        return prefs.getInt(KEY_SELECTED_SIM, -1) // -1 means default SIM
    }

    // Dual SIM configuration
    fun setDualSimConfig(isDualSim: Boolean, sim1Id: Int, sim2Id: Int = -1) {
        prefs.edit()
            .putBoolean("dual_sim_enabled", isDualSim)
            .putInt("dual_sim_1", sim1Id)
            .putInt("dual_sim_2", sim2Id)
            .apply()
    }

    fun isDualSimEnabled(): Boolean {
        return prefs.getBoolean("dual_sim_enabled", false)
    }

    fun getDualSimIds(): Pair<Int, Int> {
        val sim1 = prefs.getInt("dual_sim_1", -1)
        val sim2 = prefs.getInt("dual_sim_2", -1)
        return Pair(sim1, sim2)
    }

    fun getSimForCustomer(customerIndex: Int): Int {
        return if (isDualSimEnabled()) {
            val (sim1, sim2) = getDualSimIds()
            // Xen kẽ: chẵn → SIM 1, lẻ → SIM 2
            if (customerIndex % 2 == 0) sim1 else sim2
        } else {
            getSelectedSim()
        }
    }

    /**
     * Kiểm tra xem có nên gửi song song 2 khách hàng vào 2 SIM không
     */
    fun shouldSendParallelToDualSim(selectedCustomersCount: Int): Boolean {
        return isDualSimEnabled() && selectedCustomersCount >= 2
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

    fun resetSmsCount(simId: Int): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastSmsDateKey = getLastSmsDateKey(simId)
        val smsCountKey = getSmsCountKey(simId)
        
        // Use commit() instead of apply() to ensure immediate write
        val success = prefs.edit()
            .putString(lastSmsDateKey, today)
            .putInt(smsCountKey, 0)
            .commit()
        
        // Double-check that the count was reset
        val newCount = prefs.getInt(smsCountKey, -1)
        android.util.Log.d("SmsRepository", "Reset SMS count for SIM $simId: new count = $newCount, success = $success")
        
        // Verify that the count was actually reset
        return success && newCount == 0
    }
    
    fun resetAllSimCounts(): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        // Reset all SIMs in a single commit for efficiency
        val editor = prefs.edit()
        
        // Reset for default SIM (-1)
        editor.putString(getLastSmsDateKey(-1), today)
        editor.putInt(getSmsCountKey(-1), 0)
        
        // Reset for SIM slots 0, 1, 2 (to cover all possible SIMs)
        for (simId in 0..2) {
            editor.putString(getLastSmsDateKey(simId), today)
            editor.putInt(getSmsCountKey(simId), 0)
        }
        
        // Commit changes immediately
        val success = editor.commit()
        
        // Verify reset was successful
        val defaultSimCount = prefs.getInt(getSmsCountKey(-1), -1)
        
        android.util.Log.d("SmsRepository", "Reset SMS count for all SIMs, success: $success, default SIM count: $defaultSimCount")
        
        return success && defaultSimCount == 0
    }
    
    // Countdown data methods
    fun saveCountdownData(startTime: Long, totalTime: Long, customerCount: Int) {
        prefs.edit {
            putLong(KEY_COUNTDOWN_START_TIME, startTime)
            putLong(KEY_COUNTDOWN_TOTAL_TIME, totalTime)
            putInt(KEY_COUNTDOWN_CUSTOMER_COUNT, customerCount)
        }
    }
    
    fun getCountdownStartTime(): Long {
        return prefs.getLong(KEY_COUNTDOWN_START_TIME, 0)
    }
    
    fun getCountdownTotalTime(): Long {
        return prefs.getLong(KEY_COUNTDOWN_TOTAL_TIME, 0)
    }
    
    fun getCountdownCustomerCount(): Int {
        return prefs.getInt(KEY_COUNTDOWN_CUSTOMER_COUNT, 0)
    }
    
    fun clearCountdownData() {
        prefs.edit {
            remove(KEY_COUNTDOWN_START_TIME)
            remove(KEY_COUNTDOWN_TOTAL_TIME)
            remove(KEY_COUNTDOWN_CUSTOMER_COUNT)
        }
    }
} 