package com.example.sms_app.data

data class MessageTemplate(
    val id: Int,
    val content: String,
    val description: String
)

object TemplateManager {
    fun getDefaultTemplates(): List<MessageTemplate> {
        return listOf(
            MessageTemplate(1, "", "Template 1"),
            MessageTemplate(2, "", "Template 2"),
            MessageTemplate(3, "", "Template 3"),
            MessageTemplate(4, "", "Template 4"),
            MessageTemplate(5, "", "Template 5"),
            MessageTemplate(6, "", "Template 6"),
            MessageTemplate(7, "", "Template 7"),
            MessageTemplate(8, "", "Template 8"),
            MessageTemplate(9, "", "Template 9")
        )
    }
    
    fun getTemplateById(id: Int, customTemplates: List<MessageTemplate>): MessageTemplate? {
        return customTemplates.find { it.id == id } ?: getDefaultTemplates().find { it.id == id }
    }
} 