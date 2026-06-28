package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeProvider(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "theme_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _currentTheme = MutableStateFlow(
        try {
            AppTheme.valueOf(sharedPreferences.getString(KEY_THEME, AppTheme.SLEEK_DARK.name) ?: AppTheme.SLEEK_DARK.name)
        } catch (e: Exception) {
            AppTheme.SLEEK_DARK
        }
    )
    val currentTheme: StateFlow<AppTheme> = _currentTheme
    
    private val _isDynamicColor = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_DYNAMIC_COLOR, true)
    )
    val isDynamicColor: StateFlow<Boolean> = _isDynamicColor

    fun setTheme(theme: AppTheme) {
        sharedPreferences.edit().putString(KEY_THEME, theme.name).apply()
        _currentTheme.value = theme
    }
    
    fun setDynamicColor(isDynamic: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DYNAMIC_COLOR, isDynamic).apply()
        _isDynamicColor.value = isDynamic
    }

    companion object {
        private const val KEY_THEME = "key_theme"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        
        @Volatile
        private var instance: ThemeProvider? = null

        fun getInstance(context: Context): ThemeProvider {
            return instance ?: synchronized(this) {
                instance ?: ThemeProvider(context).also { instance = it }
            }
        }
    }
}
