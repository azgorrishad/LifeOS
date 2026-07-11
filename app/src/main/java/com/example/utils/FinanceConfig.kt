package com.example.utils

object FinanceConfig {
    var currentCurrency: String = "BDT"
    
    fun formatCurrency(amount: Double): String {
        val symbol = when (currentCurrency) {
            "USD" -> "$"
            "EUR" -> "€"
            "INR" -> "₹"
            "GBP" -> "£"
            else -> "৳"
        }
        return symbol + String.format("%.2f", amount)
    }
}
