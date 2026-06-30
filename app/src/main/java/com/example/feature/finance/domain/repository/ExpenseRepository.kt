package com.example.feature.finance.domain.repository

import com.example.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    fun getTotalExpenses(): Flow<Double?>
    suspend fun insertExpense(expense: ExpenseEntity)
    suspend fun deleteExpense(id: Int)
}
