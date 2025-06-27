package com.example.sms_app.data

data class SmsTemplate(
    val id: String,
    val name: String,
    val content: String,
    val variables: List<String> = listOf("xxxx", "yyyy", "zzzz")
) {
    fun replaceVariables(variableValues: Map<String, String>): String {
        var result = content
        variableValues.forEach { (key, value) ->
            result = result.replace(key, value)
        }
        return result
    }
}

data class SmsConfig(
    val phoneNumber: String,
    val template: SmsTemplate,
    val variableValues: Map<String, String>,
    val intervalSeconds: Long = 25,
    val isActive: Boolean = false
) 