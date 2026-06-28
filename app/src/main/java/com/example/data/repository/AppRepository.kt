package com.example.data.repository

import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val taskDao: TaskDao,
    private val expenseDao: ExpenseDao
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val totalExpenses: Flow<Double?> = expenseDao.getTotalExpenses()

    suspend fun insertTask(task: TaskEntity) = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(id: Int) = taskDao.deleteTaskById(id)

    suspend fun insertExpense(expense: ExpenseEntity) = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(id: Int) = expenseDao.deleteExpenseById(id)
}
