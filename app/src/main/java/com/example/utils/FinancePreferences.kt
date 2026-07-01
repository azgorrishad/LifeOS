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

    fun getWidgetLayout(): String {
        return sharedPreferences.getString("widget_layout", "welcome:2,search:2,insight:2,priority:2,stats:2,tasks:2,goals:2") ?: "welcome:2,search:2,insight:2,priority:2,stats:2,tasks:2,goals:2"
    }

    fun setWidgetLayout(layout: String) {
        sharedPreferences.edit().putString("widget_layout", layout).apply()
    }
}
