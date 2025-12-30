package com.example.capdex.utils

object ValidationUtils {
    
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun isValidCPF(cpf: String): Boolean {
        val cleanCPF = cpf.replace("[^0-9]".toRegex(), "")
        
        if (cleanCPF.length != 11) return false
        if (cleanCPF.all { it == cleanCPF[0] }) return false
        
        // Validação do primeiro dígito verificador
        var sum = 0
        for (i in 0..8) {
            sum += cleanCPF[i].toString().toInt() * (10 - i)
        }
        var digit = 11 - (sum % 11)
        if (digit >= 10) digit = 0
        if (digit != cleanCPF[9].toString().toInt()) return false
        
        // Validação do segundo dígito verificador
        sum = 0
        for (i in 0..9) {
            sum += cleanCPF[i].toString().toInt() * (11 - i)
        }
        digit = 11 - (sum % 11)
        if (digit >= 10) digit = 0
        if (digit != cleanCPF[10].toString().toInt()) return false
        
        return true
    }
    
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    fun isValidFullName(name: String): Boolean {
        return name.trim().split(" ").size >= 2 && name.trim().length >= 3
    }
    
    fun formatCPF(cpf: String): String {
        val cleaned = cpf.replace("[^0-9]".toRegex(), "")
        return when {
            cleaned.length <= 3 -> cleaned
            cleaned.length <= 6 -> "${cleaned.substring(0, 3)}.${cleaned.substring(3)}"
            cleaned.length <= 9 -> "${cleaned.substring(0, 3)}.${cleaned.substring(3, 6)}.${cleaned.substring(6)}"
            else -> "${cleaned.substring(0, 3)}.${cleaned.substring(3, 6)}.${cleaned.substring(6, 9)}-${cleaned.substring(9, minOf(11, cleaned.length))}"
        }
    }
}
