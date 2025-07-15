package com.example.sms_app.data

data class Customer(
    val id: String,
    val name: String, // Cột 1 - Tên khách hàng (xxx)
    val idNumber: String = "", // Cột 2 - Số CMND/CCCD (yyy)
    val phoneNumber: String, // Cột 3 - Số điện thoại (thêm số 0 đầu nếu cần)
    val address: String = "", // Cột 4 - Địa chỉ (ttt)
    val option1: String = "", // Cột 5 - Tùy chọn 1 (zzz)
    val option2: String = "", // Cột 6 - Tùy chọn 2 (www)
    val option3: String = "", // Cột 7 - Tùy chọn 3 (uuu)
    val option4: String = "", // Cột 8 - Tùy chọn 4 (vvv)
    val option5: String = "", // Cột 9 - Tùy chọn 5 (rrr)
    val templateNumber: Int = 0, // Cột 10 - Số mẫu tin nhắn (1-9)
    val carrier: String = detectCarrier(phoneNumber),
    var isSelected: Boolean = false
) {
    fun getPersonalizedMessage(templates: List<MessageTemplate>, defaultTemplateId: Int = 1): String {
        val templateId = defaultTemplateId
        val template = TemplateManager.getTemplateById(templateId, templates)?.content ?: ""
        return template
            // Variables without braces
            .replace("xxx", name)
            .replace("XXX", name)
            .replace("yyy", idNumber)
            .replace("YYY", idNumber)
            .replace("ttt", address)
            .replace("TTT", address)
            .replace("zzz", option1)
            .replace("ZZZ", option1)
            .replace("www", option2)
            .replace("WWW", option2)
            .replace("uuu", option3)
            .replace("UUU", option3)
            .replace("vvv", option4)
            .replace("VVV", option4)
            .replace("rrr", option5)
            .replace("RRR", option5)
            // Variables with curly braces {} - keep brackets, replace content
            .replace("{xxx}", "{$name}")
            .replace("{XXX}", "{$name}")
            .replace("{yyy}", "{$idNumber}")
            .replace("{YYY}", "{$idNumber}")
            .replace("{ttt}", "{$address}")
            .replace("{TTT}", "{$address}")
            .replace("{zzz}", "{$option1}")
            .replace("{ZZZ}", "{$option1}")
            .replace("{www}", "{$option2}")
            .replace("{WWW}", "{$option2}")
            .replace("{uuu}", "{$option3}")
            .replace("{UUU}", "{$option3}")
            .replace("{vvv}", "{$option4}")
            .replace("{VVV}", "{$option4}")
            .replace("{rrr}", "{$option5}")
            .replace("{RRR}", "{$option5}")
            // Variables with square brackets [] - keep brackets, replace content
            .replace("[xxx]", "[$name]")
            .replace("[XXX]", "[$name]")
            .replace("[yyy]", "[$idNumber]")
            .replace("[YYY]", "[$idNumber]")
            .replace("[ttt]", "[$address]")
            .replace("[TTT]", "[$address]")
            .replace("[zzz]", "[$option1]")
            .replace("[ZZZ]", "[$option1]")
            .replace("[www]", "[$option2]")
            .replace("[WWW]", "[$option2]")
            .replace("[uuu]", "[$option3]")
            .replace("[UUU]", "[$option3]")
            .replace("[vvv]", "[$option4]")
            .replace("[VVV]", "[$option4]")
            .replace("[rrr]", "[$option5]")
            .replace("[RRR]", "[$option5]")
            // Variables with parentheses () - keep brackets, replace content
            .replace("(xxx)", "($name)")
            .replace("(XXX)", "($name)")
            .replace("(yyy)", "($idNumber)")
            .replace("(YYY)", "($idNumber)")
            .replace("(ttt)", "($address)")
            .replace("(TTT)", "($address)")
            .replace("(zzz)", "($option1)")
            .replace("(ZZZ)", "($option1)")
            .replace("(www)", "($option2)")
            .replace("(WWW)", "($option2)")
            .replace("(uuu)", "($option3)")
            .replace("(UUU)", "($option3)")
            .replace("(vvv)", "($option4)")
            .replace("(VVV)", "($option4)")
            .replace("(rrr)", "($option5)")
            .replace("(RRR)", "($option5)")
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
                
                phone.startsWith("056") || phone.startsWith("058") || phone.startsWith("092") -> "Vietnamobile"
                
                else -> "Khác"
            }
        }
    }
} 