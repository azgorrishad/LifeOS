package com.example.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.remote.GeminiRepository
import com.example.data.repository.AppRepository
import com.example.utils.Result
import com.example.widget.SummaryWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LifeOsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.taskDao(), database.expenseDao())
    private val geminiRepository = GeminiRepository()

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalExpenses: StateFlow<Double> = repository.totalExpenses
        .combine(MutableStateFlow(0.0)) { total, _ -> total ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private val _aiInsight = MutableStateFlow<Result<String>>(Result.Loading)
    val aiInsight: StateFlow<Result<String>> = _aiInsight

    init {
        refreshInsight()
    }

    fun addTask(title: String, priority: Int) {
        viewModelScope.launch {
            repository.insertTask(TaskEntity(title = title, priority = priority))
            refreshInsight()
            updateWidget()
        }
    }
    
    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
            updateWidget()
        }
    }

    fun addExpense(amount: Double, category: String, note: String) {
        viewModelScope.launch {
            repository.insertExpense(ExpenseEntity(amount = amount, category = category, note = note))
            refreshInsight()
            updateWidget()
        }
    }
    
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { 
            repository.deleteTask(task.id)
            updateWidget()
        }
    }
    
    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { 
            repository.deleteExpense(expense.id)
            updateWidget()
        }
    }

    private fun updateWidget() {
        val intent = Intent(getApplication<Application>(), SummaryWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(getApplication())
            .getAppWidgetIds(ComponentName(getApplication(), SummaryWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        getApplication<Application>().sendBroadcast(intent)
    }

    fun refreshInsight() {
        viewModelScope.launch {
            _aiInsight.value = Result.Loading
            
            val taskList = tasks.value.take(5).joinToString { it.title + (if(it.isCompleted) " (Done)" else " (Pending)") }
            val expenseList = expenses.value.take(5).joinToString { "${it.category}: ${com.example.utils.FinanceConfig.formatCurrency(it.amount)}" }
            
            val promptTask = if (taskList.isEmpty()) "No tasks yet." else taskList
            val promptExpense = if (expenseList.isEmpty()) "No expenses today." else expenseList
            
            _aiInsight.value = geminiRepository.getLifeInsights(promptTask, promptExpense)
        }
    }

    fun analyzeHabits(productivityData: String) {
        viewModelScope.launch {
            _aiInsight.value = Result.Loading
            _aiInsight.value = geminiRepository.analyzeHabits(productivityData)
        }
    }

    fun resolveSchedulingConflict(conflictDetails: String) {
        viewModelScope.launch {
            _aiInsight.value = Result.Loading
            _aiInsight.value = geminiRepository.resolveSchedulingConflict(conflictDetails)
        }
    }

    fun askJarvis(query: String) {
        viewModelScope.launch {
            _aiInsight.value = Result.Loading
            _aiInsight.value = geminiRepository.askJarvis(query)
        }
    }
}
