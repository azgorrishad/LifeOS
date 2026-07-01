package com.example.feature.dashboard.presentation

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.AIEngine
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.TaskEntity
import com.example.feature.finance.domain.usecase.*
import com.example.feature.tasks.domain.usecase.*
import com.example.utils.Result
import com.example.widget.SummaryWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProjectEntity
import com.example.feature.events.domain.usecase.*
import com.example.feature.goals.domain.usecase.*
import com.example.feature.habits.domain.usecase.*
import com.example.feature.notes.domain.usecase.*
import com.example.feature.projects.domain.usecase.*

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: () -> Unit = {}
)

class DashboardViewModel(
    application: Application,
    getTasksUseCase: GetTasksUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    getExpensesUseCase: GetExpensesUseCase,
    getTotalExpensesUseCase: GetTotalExpensesUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    getEventsUseCase: GetEventsUseCase,
    private val addEventUseCase: AddEventUseCase,
    getNotesUseCase: GetNotesUseCase,
    private val addNoteUseCase: AddNoteUseCase,
    getProjectsUseCase: GetProjectsUseCase,
    private val addProjectUseCase: AddProjectUseCase,
    getHabitsUseCase: GetHabitsUseCase,
    private val addHabitUseCase: AddHabitUseCase,
    private val getGoalsUseCase: GetGoalsUseCase,
    private val addGoalUseCase: AddGoalUseCase,
    private val toggleGoalCompletionUseCase: ToggleGoalCompletionUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val aiEngine: AIEngine,
    private val financePreferences: com.example.utils.FinancePreferences
) : AndroidViewModel(application) {

    private val _monthlyBudget = MutableStateFlow(financePreferences.getMonthlyBudget())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _dailyBudget = MutableStateFlow(financePreferences.getDailyBudget())
    val dailyBudget: StateFlow<Double> = _dailyBudget.asStateFlow()

    fun updateMonthlyBudget(budget: Double) {
        financePreferences.setMonthlyBudget(budget)
        _monthlyBudget.value = budget
    }

    fun updateDailyBudget(budget: Double) {
        financePreferences.setDailyBudget(budget)
        _dailyBudget.value = budget
    }

    private val _widgetLayout = MutableStateFlow(financePreferences.getWidgetLayout())
    val widgetLayout: StateFlow<String> = _widgetLayout.asStateFlow()

    fun updateWidgetLayout(layout: String) {
        financePreferences.setWidgetLayout(layout)
        _widgetLayout.value = layout
    }

    val tasks: StateFlow<List<TaskEntity>> = getTasksUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val expenses: StateFlow<List<ExpenseEntity>> = getExpensesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val events: StateFlow<List<EventEntity>> = getEventsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notes: StateFlow<List<NoteEntity>> = getNotesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val projects: StateFlow<List<ProjectEntity>> = getProjectsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val habits: StateFlow<List<HabitEntity>> = getHabitsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val goals: StateFlow<List<GoalEntity>> = getGoalsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalExpenses: StateFlow<Double> = getTotalExpensesUseCase()
        .combine(MutableStateFlow(0.0)) { total, _ -> total ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private val _aiInsight = MutableStateFlow<Result<String>>(Result.Loading)
    val aiInsight: StateFlow<Result<String>> = _aiInsight

    private val _chatHistory = MutableStateFlow<List<com.example.core.ai.ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<com.example.core.ai.ChatMessage>> = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun askJarvisChat(message: String) {
        val userMessage = com.example.core.ai.ChatMessage(role = "user", text = message)
        _chatHistory.value = _chatHistory.value + userMessage
        _isChatLoading.value = true
        viewModelScope.launch {
            try {
                val response = aiEngine.askJarvisChat(_chatHistory.value)
                if (response is Result.Success) {
                    val modelMessage = com.example.core.ai.ChatMessage(role = "model", text = response.data)
                    _chatHistory.value = _chatHistory.value + modelMessage
                } else if (response is Result.Error) {
                    val errorMessage = com.example.core.ai.ChatMessage(role = "model", text = "Error: ${response.message}")
                    _chatHistory.value = _chatHistory.value + errorMessage
                }
            } catch (e: Exception) {
                val errorMessage = com.example.core.ai.ChatMessage(role = "model", text = "Error: Something went wrong. Please try again later.")
                _chatHistory.value = _chatHistory.value + errorMessage
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    private val _welcomeMessage = MutableStateFlow<String>("Gathering data for your briefing...")
    val welcomeMessage: StateFlow<String> = _welcomeMessage.asStateFlow()

    init {
        refreshInsight()
        generateWelcomeMessage()
    }

    private fun generateWelcomeMessage() {
        viewModelScope.launch {
            val name = financePreferences.getUserName()
            val timeOfDay = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                in 0..11 -> "morning"
                in 12..16 -> "afternoon"
                else -> "evening"
            }
            // Wait for goals to be loaded
            val firstGoal = try {
                getGoalsUseCase().first().firstOrNull()?.title
            } catch (e: Exception) { null }

            val prompt = "Generate a short, encouraging 1-sentence welcome message for a user named $name. It is currently $timeOfDay. If they have a primary goal, incorporate it: ${firstGoal ?: "No specific goal yet"}."
            
            val response = aiEngine.askJarvis(prompt)
            if (response is Result.Success) {
                _welcomeMessage.value = response.data
            } else {
                _welcomeMessage.value = "Good $timeOfDay, $name! Ready to tackle your goals today?"
            }
        }
    }

    fun addGoal(title: String, description: String = "") {
        viewModelScope.launch {
            val goal = com.example.data.local.entity.GoalEntity(
                title = title,
                description = description,
                targetDate = System.currentTimeMillis(),
                isCompleted = false,
                progress = 0f
            )
            addGoalUseCase(goal)
        }
    }

    fun toggleGoal(goal: com.example.data.local.entity.GoalEntity) {
        viewModelScope.launch {
            toggleGoalCompletionUseCase(goal)
        }
    }

    fun deleteGoal(goal: com.example.data.local.entity.GoalEntity) {
        viewModelScope.launch {
            deleteGoalUseCase(goal.id)
            lastDeletedGoal = goal
            showSnackbar("Goal deleted", "Undo") {
                lastDeletedGoal?.let {
                    addGoal(it.title, it.description)
                    lastDeletedGoal = null
                }
            }
        }
    }

    private val _snackbarEvent = kotlinx.coroutines.flow.MutableSharedFlow<SnackbarEvent>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    fun showSnackbar(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        viewModelScope.launch {
            _snackbarEvent.emit(SnackbarEvent(message, actionLabel, onAction))
        }
    }

    private var lastDeletedTask: TaskEntity? = null
    private var lastDeletedExpense: ExpenseEntity? = null
    private var lastDeletedGoal: com.example.data.local.entity.GoalEntity? = null

    fun addTask(title: String, priority: Int) {
        viewModelScope.launch {
            addTaskUseCase(title, priority)
            refreshInsight()
            updateWidget()
            showSnackbar("Task added", "Undo") {
                // Technically to undo an add without an ID, we'd need to delete the last one.
                // But typically undoing an input just reverts it. 
                // We could delete by title for simplicity if we can't delete by ID.
            }
        }
    }
    
    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            toggleTaskCompletionUseCase(task)
            updateWidget()
        }
    }

    fun addExpense(amount: Double, category: String, note: String) {
        viewModelScope.launch {
            addExpenseUseCase(amount, category, note)
            refreshInsight()
            updateWidget()
            showSnackbar("Expense added")
            
            val budget = _dailyBudget.value
            if (budget > 0) {
                val todayStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val currentDailyTotal = expenses.value.filter { it.timestamp >= todayStart }.sumOf { it.amount }
                val newDailyTotal = currentDailyTotal + amount
                
                if (newDailyTotal > budget * 0.8) {
                    val prompt = "The user has spent ${com.example.utils.FinanceConfig.formatCurrency(newDailyTotal)} today, which is over 80% of their daily budget of ${com.example.utils.FinanceConfig.formatCurrency(budget)}. Generate a short, friendly, but urgent warning message (max 2 sentences)."
                    val response = aiEngine.askJarvis(prompt)
                    if (response is Result.Success) {
                        showSnackbar("⚠️ ${response.data}")
                    }
                }
            }
        }
    }
    
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { 
            deleteTaskUseCase(task)
            lastDeletedTask = task
            updateWidget()
            showSnackbar("Task deleted", "Undo") {
                lastDeletedTask?.let {
                    addTask(it.title, it.priority)
                    lastDeletedTask = null
                }
            }
        }
    }
    
    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { 
            deleteExpenseUseCase(expense)
            lastDeletedExpense = expense
            updateWidget()
            showSnackbar("Expense deleted", "Undo") {
                lastDeletedExpense?.let {
                    addExpense(it.amount, it.category, it.note)
                    lastDeletedExpense = null
                }
            }
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
            
            val taskList = tasks.value.take(5).map { it.title + (if(it.isCompleted) " (Done)" else " (Pending)") }
            val expenseList = expenses.value.take(5).map { "${it.category}: ${com.example.utils.FinanceConfig.formatCurrency(it.amount)}" }
            
            _aiInsight.value = aiEngine.getInsights(taskList, expenseList)
        }
    }

    fun resolveSchedulingConflict(conflictDetails: String) {
        viewModelScope.launch {
            _aiInsight.value = Result.Loading
            _aiInsight.value = aiEngine.resolveConflict(conflictDetails)
        }
    }

    fun analyzeHabits(productivityData: String) {
        viewModelScope.launch {
            _aiInsight.value = Result.Loading
            _aiInsight.value = aiEngine.analyzeHabits(productivityData)
        }
    }

    fun askJarvis(query: String) {
        viewModelScope.launch {
            _aiInsight.value = Result.Loading
            _aiInsight.value = aiEngine.askJarvis(query)
        }
    }

    fun getBudgetSuggestion() {
        viewModelScope.launch {
            _aiInsight.value = Result.Loading
            val budget = monthlyBudget.value
            val spent = totalExpenses.value
            val expenseList = expenses.value.take(10).map { "${it.category}: ${it.amount}" }.joinToString(", ")
            val taskList = tasks.value.map { it.title }.joinToString(", ")
            val prompt = "My monthly budget is ${com.example.utils.FinanceConfig.formatCurrency(budget)} and I have spent ${com.example.utils.FinanceConfig.formatCurrency(spent)}. My recent expenses: $expenseList. My current tasks: $taskList. Please analyze my expenses and tasks to provide natural language spending tips and budget adjustments."
            _aiInsight.value = aiEngine.askJarvis(prompt)
        }
    }

    private val _userName = MutableStateFlow(financePreferences.getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun updateUserName(name: String) {
        financePreferences.setUserName(name)
        _userName.value = name
    }

    private val _taskPrioritization = MutableStateFlow<Result<String>>(Result.Loading)
    val taskPrioritization: StateFlow<Result<String>> = _taskPrioritization.asStateFlow()

    fun getTaskPrioritization() {
        viewModelScope.launch {
            _taskPrioritization.value = Result.Loading
            val currentTasks = tasks.value.map { it.title }
            if (currentTasks.isEmpty()) {
                _taskPrioritization.value = Result.Success("No tasks to prioritize!")
                return@launch
            }
            _taskPrioritization.value = aiEngine.getTaskPrioritization(currentTasks)
        }
    }
}
