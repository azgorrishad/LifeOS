package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class FinancePreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "finance_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat("monthly_budget", 0f).toDouble()
    }

    fun setMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat("monthly_budget", budget.toFloat()).apply()
    }

    fun getDailyBudget(): Double {
        return sharedPreferences.getFloat("daily_budget", 0f).toDouble()
    }

    fun setDailyBudget(budget: Double) {
        sharedPreferences.edit().putFloat("daily_budget", budget.toFloat()).apply()
    }

    fun getUserName(): String {
        return sharedPreferences.getString("user_name", "Alex") ?: "Alex"
    }

    fun setUserName(name: String) {
        sharedPreferences.edit().putString("user_name", name).apply()
    }

    fun getCurrencyCode(): String {
        return sharedPreferences.getString("currency_code", "BDT") ?: "BDT"
    }

    fun setCurrencyCode(code: String) {
        sharedPreferences.edit().putString("currency_code", code).apply()
    }

    fun getWidgetLayout(): String {
        var layout = sharedPreferences.getString("widget_layout", "welcome:2,jarvis_insights:2,summary:2,chat:2,tasks:1,journals:1,forecast:2,stats:2,goals:2,notes:2") ?: "welcome:2,jarvis_insights:2,summary:2,chat:2,tasks:1,journals:1,forecast:2,stats:2,goals:2,notes:2"
        if (!layout.contains("summary")) layout = "summary:2,$layout"
        if (!layout.contains("debt")) layout = "$layout,debt:2"
        if (!layout.contains("jarvis_insights")) layout = "jarvis_insights:2,$layout"
        if (!layout.contains("notes")) layout = "$layout,notes:2"
        return layout
    }

    fun setWidgetLayout(layout: String) {
        sharedPreferences.edit().putString("widget_layout", layout).apply()
    }

    fun getCustomTaskCategories(): List<String> {
        val serialized = sharedPreferences.getString("custom_task_categories", "") ?: ""
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun addCustomTaskCategory(category: String) {
        val current = getCustomTaskCategories().toMutableList()
        val cleaned = category.trim()
        if (cleaned.isNotEmpty() && !current.contains(cleaned)) {
            current.add(cleaned)
            sharedPreferences.edit().putString("custom_task_categories", current.joinToString(",")).apply()
        }
    }

    fun getCustomExpenseCategories(): List<String> {
        val serialized = sharedPreferences.getString("custom_expense_categories", "") ?: ""
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun addCustomExpenseCategory(category: String) {
        val current = getCustomExpenseCategories().toMutableList()
        val cleaned = category.trim()
        if (cleaned.isNotEmpty() && !current.contains(cleaned)) {
            current.add(cleaned)
            sharedPreferences.edit().putString("custom_expense_categories", current.joinToString(",")).apply()
        }
    }

    fun getCustomDebtCategories(): List<String> {
        val serialized = sharedPreferences.getString("custom_debt_categories", "") ?: ""
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun addCustomDebtCategory(category: String) {
        val current = getCustomDebtCategories().toMutableList()
        val cleaned = category.trim()
        if (cleaned.isNotEmpty() && !current.contains(cleaned)) {
            current.add(cleaned)
            sharedPreferences.edit().putString("custom_debt_categories", current.joinToString(",")).apply()
        }
    }
}
