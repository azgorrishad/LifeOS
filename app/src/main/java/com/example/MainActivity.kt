package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.ui.DashboardScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.ThemeProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val themeProvider = ThemeProvider.getInstance(applicationContext)
        
        setContent {
            val currentTheme by themeProvider.currentTheme.collectAsState()
            val isDynamicColor by themeProvider.isDynamicColor.collectAsState()

            MyApplicationTheme(
                dynamicColor = isDynamicColor,
                appTheme = currentTheme
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(
                        innerPadding = innerPadding,
                        currentTheme = currentTheme,
                        onThemeChange = { themeProvider.setTheme(it) },
                        isDynamicColor = isDynamicColor,
                        onDynamicColorChange = { themeProvider.setDynamicColor(it) }
                    )
                }
            }
        }
    }
}
