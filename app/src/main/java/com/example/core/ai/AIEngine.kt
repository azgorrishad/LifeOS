package com.example.core.ai

import com.example.utils.Result

data class ChatMessage(val role: String, val text: String)

interface AIEngine {
    suspend fun getInsights(tasks: List<String>, expenses: List<String>): Result<String>
    suspend fun resolveConflict(conflictDetails: String): Result<String>
    suspend fun analyzeHabits(productivityData: String): Result<String>
    suspend fun askJarvis(query: String): Result<String>
    suspend fun askJarvisChat(history: List<ChatMessage>, useThinking: Boolean = false): Result<String>
    suspend fun getTaskPrioritization(tasks: List<String>): Result<String>
}
