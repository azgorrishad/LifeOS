package com.example.core.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.utils.APIDiagnosticLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PersistentChatMessage(
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toChatMessage() = ChatMessage(role, text)
}

class GemmaLocalStateProvider(private val context: Context) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("gemma_local_history", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val _chatHistory = MutableStateFlow<List<PersistentChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<PersistentChatMessage>> = _chatHistory.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadHistory()
        startPeriodicAutoBackupCheck()
    }

    private fun startPeriodicAutoBackupCheck() {
        scope.launch {
            while (isActive) {
                checkAndTriggerAutoBackup()
                delay(1000L * 60 * 60) // Check every hour
            }
        }
    }

    fun isAutoBackupEnabled(): Boolean {
        return sharedPrefs.getBoolean("auto_backup_enabled", true)
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("auto_backup_enabled", enabled).apply()
        checkAndTriggerAutoBackup()
    }

    fun getAutoBackupInterval(): String {
        return sharedPrefs.getString("auto_backup_interval", "daily") ?: "daily"
    }

    fun setAutoBackupInterval(interval: String) {
        sharedPrefs.edit().putString("auto_backup_interval", interval).apply()
        checkAndTriggerAutoBackup()
    }

    fun getLastAutoBackupTime(): Long {
        return sharedPrefs.getLong("last_auto_backup_time", 0L)
    }

    fun checkAndTriggerAutoBackup() {
        val enabled = sharedPrefs.getBoolean("auto_backup_enabled", true)
        if (!enabled) return

        val interval = sharedPrefs.getString("auto_backup_interval", "daily") ?: "daily"
        val lastBackup = sharedPrefs.getLong("last_auto_backup_time", 0L)
        val now = System.currentTimeMillis()

        val thresholdMs = if (interval == "weekly") {
            7L * 24 * 60 * 60 * 1000 // 7 days
        } else {
            24L * 60 * 60 * 1000 // 1 day (default)
        }

        if (now - lastBackup >= thresholdMs) {
            val path = syncAndExportHistoryToFile()
            if (path != null) {
                sharedPrefs.edit().putLong("last_auto_backup_time", now).apply()
                APIDiagnosticLogger.logCall(
                    feature = "AutomatedBackupScheduler",
                    requestSummary = "Periodic Auto-Backup triggered. Interval: $interval",
                    isSuccess = true,
                    responseSummary = "Backup exported to $path",
                    errorMessage = null,
                    durationMs = 0,
                    payloadStructure = "Scheduler"
                )
            }
        }
    }

    @Synchronized
    fun saveMessage(role: String, text: String) {
        val currentList = _chatHistory.value.toMutableList()
        currentList.add(PersistentChatMessage(role, text))
        _chatHistory.value = currentList
        persistHistory(currentList)
    }

    @Synchronized
    fun clearHistory() {
        _chatHistory.value = emptyList()
        sharedPrefs.edit().remove("history_json").apply()
    }

    /**
     * Reconnects and re-initializes the Gemma4 local core configuration state.
     * Attempts to reconnect or re-initialize up to three times if an error/failure occurs.
     */
    suspend fun reconnectAndInitializeWithRetry(): Boolean {
        var attempts = 0
        while (attempts < 3) {
            attempts++
            try {
                GemmaLocalConfig.resetState()
                val success = GemmaLocalConfig.initializeModel()
                if (success) {
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(500L * attempts)
        }
        return false
    }

    /**
     * Exports the persistent local AI history to a beautifully formatted JSON string.
     * This acts as a manual sync/backup mechanism for the persistent state.
     */
    @Synchronized
    fun syncAndExportHistory(): String {
        return try {
            val prettyJson = Json { prettyPrint = true; ignoreUnknownKeys = true }
            prettyJson.encodeToString(_chatHistory.value)
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    /**
     * Synchronizes and exports the persistent local AI history to a JSON backup file.
     * Returns the absolute path of the generated backup file, or null if an error occurs.
     */
    @Synchronized
    fun syncAndExportHistoryToFile(): String? {
        return try {
            val prettyJson = Json { prettyPrint = true; ignoreUnknownKeys = true }
            val jsonStr = prettyJson.encodeToString(_chatHistory.value)
            val file = java.io.File(context.filesDir, "gemma_history_backup.json")
            file.writeText(jsonStr)
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadHistory() {
        try {
            val jsonStr = sharedPrefs.getString("history_json", null)
            if (!jsonStr.isNullOrBlank()) {
                val list = json.decodeFromString<List<PersistentChatMessage>>(jsonStr)
                _chatHistory.value = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistHistory(list: List<PersistentChatMessage>) {
        try {
            val jsonStr = json.encodeToString(list)
            sharedPrefs.edit().putString("history_json", jsonStr).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Formats conversation history correctly for Gemma4 LLM context window ingestion.
     * Uses the standard Gemma prompt sequence: <start_of_turn>role\ntext<end_of_turn>
     */
    fun formatForGemma4Inference(systemPrompt: String, currentPrompt: String): String {
        val sb = java.lang.StringBuilder()
        
        // Add system instruction if present
        if (systemPrompt.isNotBlank()) {
            sb.append("<start_of_turn>system\n$systemPrompt<end_of_turn>\n")
        }
        
        // Add chat history
        _chatHistory.value.forEach { msg ->
            val roleName = if (msg.role.lowercase() == "user") "user" else "model"
            sb.append("<start_of_turn>$roleName\n${msg.text}<end_of_turn>\n")
        }
        
        // Add current prompt
        sb.append("<start_of_turn>user\n$currentPrompt<end_of_turn>\n")
        sb.append("<start_of_turn>model\n")
        
        return sb.toString()
    }
}
