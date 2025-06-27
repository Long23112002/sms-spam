package com.example.sms_app.data

data class Customer(
    val id: String,
    val name: String, // Cột A - Tên khách hàng (xxx)
    val idNumber: String = "", // Cột B - Số CMND/CCCD (yyy)
    val phoneNumber: String, // Cột C - Số điện thoại
    val address: String = "", // Cột D - Địa chỉ (ttt)
    val option1: String = "", // Cột E - Tùy chọn 1 (zzz)
    val option2: String = "", // Cột F - Tùy chọn 2 (www)
    val option3: String = "", // Cột G - Tùy chọn 3 (uuuu)
    val option4: String = "", // Cột H - Tùy chọn 4 (vvv)
    val option5: String = "", // Cột I - Tùy chọn 5 (rrr)
    val templateNumber: Int = 0, // Cột J - Số mẫu tin nhắn (1-9)
    val carrier: String = detectCarrier(phoneNumber),
    var isSelected: Boolean = false
) {
    fun getPersonalizedMessage(templates: List<MessageTemplate>, defaultTemplateId: Int = 1): String {
        val templateId = defaultTemplateId
        val template = TemplateManager.getTemplateById(templateId, templates)?.content ?: ""
        return template
            .replace("xxx", name)
            .replace("yyy", idNumber)
            .replace("ttt", address)
            .replace("zzz", option1)
            .replace("www", option2)
            .replace("uuuu", option3)
            .replace("vvv", option4)
            .replace("rrr", option5)
    }
    
    companion object {
        fun detectCarrier(phoneNumber: String): String {
            val phone = phoneNumber.replace("+84", "0").replace(" ", "")
            return when {
                phone.startsWith("086") || phone.startsWith("096") || phone.startsWith("097") || 
                phone.startsWith("098") || phone.startsWith("032") || phone.startsWith("033") || 
                phone.startsWith("034") || phone.startsWith("035") || phone.startsWith("036") || 
                phone.startsWith("037") || phone.startsWith("038") || phone.startsWith("039") -> "Viettel"
                
                phone.startsWith("089") || phone.startsWith("090") || phone.startsWith("093") || 
                phone.startsWith("070") || phone.startsWith("079") || phone.startsWith("077") || 
                phone.startsWith("076") || phone.startsWith("078") -> "Mobifone"
                
                phone.startsWith("091") || phone.startsWith("094") || phone.startsWith("088") || 
                phone.startsWith("083") || phone.startsWith("084") || phone.startsWith("085") || 
                phone.startsWith("081") || phone.startsWith("082") -> "Vinaphone"
                
                else -> "Khác"
            }
        }
    }
} 