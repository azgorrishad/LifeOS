package com.example.utils

import java.text.NumberFormat
import java.util.Locale

object FinanceConfig {
    private val localeBDT = java.util.Locale.Builder().setLanguage("en").setRegion("BD").build()
    
    fun formatCurrency(amount: Double, locale: Locale = localeBDT): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        // Hardcode BDT symbol if the locale doesn't provide it properly
        if (locale == localeBDT) {
            return "৳" + String.format("%.2f", amount)
        }
        return format.format(amount)
    }
}
