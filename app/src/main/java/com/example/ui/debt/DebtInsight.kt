package com.example.ui.debt

import com.example.core.ai.AIEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun generateDebtInsight(aiEngine: AIEngine, totalReceivables: Double, totalPayables: Double): Flow<String> = flow {
    emit("Analyzing your debts...")
    // In a real implementation we would call aiEngine, but here we can just return a basic string if needed
    emit("You have \$${totalReceivables} to receive and \$${totalPayables} to pay.")
}
