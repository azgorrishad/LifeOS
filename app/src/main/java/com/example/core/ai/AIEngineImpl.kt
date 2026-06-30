package com.example.core.ai

import com.example.data.remote.GeminiRepository
import com.example.utils.Result

class AIEngineImpl(private val geminiRepository: GeminiRepository) : AIEngine {
    override suspend fun getInsights(tasks: List<String>, expenses: List<String>): Result<String> {
        val taskStr = if (tasks.isEmpty()) "No tasks yet." else tasks.joinToString("\n")
        val expenseStr = if (expenses.isEmpty()) "No expenses today." else expenses.joinToString("\n")
        return geminiRepository.getLifeInsights(taskStr, expenseStr)
    }

    override suspend fun resolveConflict(conflictDetails: String): Result<String> {
        return geminiRepository.resolveSchedulingConflict(conflictDetails)
    }

    override suspend fun analyzeHabits(productivityData: String): Result<String> {
        return geminiRepository.analyzeHabits(productivityData)
    }

    override suspend fun askJarvis(query: String): Result<String> {
        return geminiRepository.askJarvis(query)
    }

    override suspend fun askJarvisChat(history: List<ChatMessage>): Result<String> {
        return geminiRepository.askJarvisChat(history)
    }

    override suspend fun getTaskPrioritization(tasks: List<String>): Result<String> {
        return geminiRepository.getTaskPrioritization(tasks)
    }
}
