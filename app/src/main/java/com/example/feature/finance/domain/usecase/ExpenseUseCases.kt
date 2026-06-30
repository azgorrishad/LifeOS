package com.example.feature.finance.domain.usecase

import com.example.data.local.entity.ExpenseEntity
import com.example.feature.finance.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class GetExpensesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<ExpenseEntity>> = repository.getAllExpenses()
}

class GetTotalExpensesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<Double?> = repository.getTotalExpenses()
}

class AddExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(amount: Double, category: String, note: String) {
        repository.insertExpense(ExpenseEntity(amount = amount, category = category, note = note))
    }
}

class DeleteExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(expense: ExpenseEntity) {
        repository.deleteExpense(expense.id)
    }
}
