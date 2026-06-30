package com.example.feature.finance.data.repository

import com.example.data.local.dao.ExpenseDao
import com.example.data.local.entity.ExpenseEntity
import com.example.feature.finance.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {
    override fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    override fun getTotalExpenses(): Flow<Double?> = expenseDao.getTotalExpenses()
    override suspend fun insertExpense(expense: ExpenseEntity) = expenseDao.insertExpense(expense)
    override suspend fun deleteExpense(id: Int) = expenseDao.deleteExpenseById(id)
}
