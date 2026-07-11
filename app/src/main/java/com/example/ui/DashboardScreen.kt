package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.feature.dashboard.presentation.DashboardViewModel
import com.example.feature.dashboard.presentation.SnackbarEvent
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Notes
import com.example.data.local.entity.GoalEntity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.theme.AppTheme

import org.koin.androidx.compose.koinViewModel
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.scrollAnimation(): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "alpha")
    val offsetY by animateFloatAsState(if (visible) 0f else 50f, tween(500), label = "offsetY")
    return this.then(Modifier.graphicsLayer(alpha = alpha, translationY = offsetY))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    innerPadding: PaddingValues,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    isDynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val totalExpenses by viewModel.totalExpenses.collectAsStateWithLifecycle()
    val journals by viewModel.journals.collectAsStateWithLifecycle()
    val aiInsightState by viewModel.aiInsight.collectAsStateWithLifecycle()
    val isAwaitingResponse by com.example.core.ai.GemmaLocalConfig.isModelAwaitingResponse.collectAsStateWithLifecycle()

    val aiInsight = when (val state = aiInsightState) {
        is com.example.utils.Result.Success -> state.data
        is com.example.utils.Result.Loading -> "LifeOS AI is analyzing your data..."
        is com.example.utils.Result.Error -> state.message
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var triggerAddDebt by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(viewModel) {
        launch {
            viewModel.snackbarEvent.collect { event ->
                val result = snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.onAction()
                }
            }
        }
        launch {
            viewModel.showConfetti.collect {
                showConfetti = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CustomBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onAddClick = { 
                    if (currentTab == 3) {
                        triggerAddDebt = true
                    } else {
                        showQuickAddDialog = true
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            if (isAwaitingResponse) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(durationMillis = 300),
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    0 -> HomeContent(
                        viewModel = viewModel,
                        tasks = tasks,
                        goals = goals,
                        journals = journals,
                        totalExpenses = totalExpenses,
                        aiInsight = aiInsight,
                        onChatClicked = { showChatDialog = true },
                        onNavigateToTab = { currentTab = it },
                        onViewNotes = { showNotesDialog = true },
                        onAddNote = { showCreateNoteDialog = true }
                    )
                    1 -> PlanContent(tasks, goals, viewModel)
                    2 -> ExpensesContent(totalExpenses, expenses, viewModel.incomeList.collectAsStateWithLifecycle().value, viewModel, aiInsight, onAddIncome = { showAddIncomeDialog = true })
                    3 -> com.example.ui.debt.DebtScreen(
                        showAddDebtInitially = triggerAddDebt,
                        onAddDebtConsumed = { triggerAddDebt = false }
                    )
                    4 -> VaultContent(currentTheme, onThemeChange, isDynamicColor, onDynamicColorChange, viewModel, onViewDiagnostics = { showDiagnosticsDialog = true })
                }
            }
        }
    }

    if (showDiagnosticsDialog) {
        APIDiagnosticsDialog(onDismiss = { showDiagnosticsDialog = false })
    }

    if (showChatDialog) {
        val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
        val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
        ChatDialog(
            chatHistory = chatHistory,
            isLoading = isChatLoading,
            onDismiss = { showChatDialog = false },
            onSendMessage = { msg, useThinking -> viewModel.askJarvisChat(msg, useThinking) }
        )
    }

    val notesList by viewModel.notes.collectAsStateWithLifecycle()

    if (showNotesDialog) {
        com.example.ui.notes.NotesDialog(
            notes = notesList,
            onDismiss = { showNotesDialog = false },
            onAddNote = { title, content -> viewModel.addNote(title, content) },
            onUpdateNote = { note -> viewModel.updateNote(note) },
            onDeleteNote = { id -> viewModel.deleteNote(id) }
        )
    }

    if (showCreateNoteDialog) {
        com.example.ui.notes.NoteEditorDialog(
            note = null,
            onDismiss = { showCreateNoteDialog = false },
            onSave = { title, content ->
                viewModel.addNote(title, content)
                showCreateNoteDialog = false
            }
        )
    }

    val customTaskCats by viewModel.customTaskCategories.collectAsStateWithLifecycle()
    val customExpenseCats by viewModel.customExpenseCategories.collectAsStateWithLifecycle()

    if (showAddTaskDialog) {
        AddTaskDialog(
            customCategories = customTaskCats,
            onAddCustomCategory = { cat -> viewModel.addCustomTaskCategory(cat) },
            onDismiss = { showAddTaskDialog = false },
            onAdd = { title, priority, category ->
                viewModel.addTask(title, priority, category)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            customCategories = customExpenseCats,
            onAddCustomCategory = { cat -> viewModel.addCustomExpenseCategory(cat) },
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { amount, category, note ->
                viewModel.addExpense(amount, category, note)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddIncomeDialog) {
        AddIncomeDialog(
            onDismiss = { showAddIncomeDialog = false },
            onAdd = { amount, source, note ->
                viewModel.addIncome(amount, source, note)
                showAddIncomeDialog = false
            }
        )
    }

    if (showQuickAddDialog) {
        QuickAddDialog(
            onDismiss = { showQuickAddDialog = false },
            onOptionSelected = { option ->
                showQuickAddDialog = false
                when (option) {
                    "task" -> showAddTaskDialog = true
                    "goal" -> showAddGoalDialog = true
                    "expense" -> showAddExpenseDialog = true
                    "income" -> showAddIncomeDialog = true
                    "note" -> showCreateNoteDialog = true
                    "debt" -> {
                        currentTab = 3
                        triggerAddDebt = true
                    }
                }
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onAdd = { title, desc ->
                viewModel.addGoal(title, desc)
                showAddGoalDialog = false
            }
        )
    }

    if (showConfetti) {
        ConfettiAnimation(
            modifier = Modifier.fillMaxSize(),
            onAnimFinished = { showConfetti = false }
        )
    }
}

@Composable
fun CustomBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = Icons.Rounded.Home,
            label = "Life",
            isSelected = currentTab == 0,
            onClick = { onTabSelected(0) }
        )
        BottomNavItem(
            icon = Icons.AutoMirrored.Rounded.ListAlt,
            label = "Plan",
            isSelected = currentTab == 1,
            onClick = { onTabSelected(1) }
        )
        
        // FAB in the middle
        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        BottomNavItem(
            icon = Icons.Rounded.AccountBalance,
            label = "Expenses",
            isSelected = currentTab == 2,
            onClick = { onTabSelected(2) }
        )
        BottomNavItem(
            icon = Icons.Rounded.Wallet,
            label = "Debt",
            isSelected = currentTab == 3,
            onClick = { onTabSelected(3) }
        )
        BottomNavItem(
            icon = Icons.Rounded.Settings,
            label = "Settings",
            isSelected = currentTab == 4,
            onClick = { onTabSelected(4) }
        )
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun PlanContent(tasks: List<TaskEntity>, goals: List<GoalEntity>, viewModel: DashboardViewModel) {
    val prioritizationState by viewModel.taskPrioritization.collectAsStateWithLifecycle()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    
    var selectedCategory by remember { mutableStateOf("All") }
    val customCategories by viewModel.customTaskCategories.collectAsStateWithLifecycle()
    val categories = remember(customCategories) {
        listOf("All", "Work", "Personal", "Health", "Other") + customCategories
    }
    
    val filteredTasks = if (selectedCategory == "All") tasks else tasks.filter { it.category == selectedCategory }
    val sortedTasks = filteredTasks.sortedWith(compareByDescending<TaskEntity> { !it.isCompleted }.thenByDescending { it.priority }.thenByDescending { it.timestamp })
    
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<GoalEntity?>(null) }
    
    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onAdd = { title, desc ->
                viewModel.addGoal(title, desc)
                showAddGoalDialog = false
            }
        )
    }
    
    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task? You can undo this action using the popup at the bottom of the screen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskToDelete?.let { viewModel.deleteTask(it) }
                        taskToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Delete Daily Goal") },
            text = { Text("Are you sure you want to delete this goal? You can undo this action using the popup at the bottom of the screen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        goalToDelete?.let { viewModel.deleteGoal(it) }
                        goalToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Daily Goals",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = { showAddGoalDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (goals.isEmpty()) {
            item {
                Text(
                    text = "No daily goals set.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalItem(
                    goal = goal,
                    onToggle = { viewModel.toggleGoal(goal) },
                    onDelete = { goalToDelete = goal },
                    modifier = Modifier.animateItem()
                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(
                text = "All Tasks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        item {
            OutlinedButton(
                onClick = { viewModel.getTaskPrioritization() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Smart Prioritization", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            when (val state = prioritizationState) {
                is com.example.utils.Result.Loading -> {
                    // Do nothing or show loading
                }
                is com.example.utils.Result.Success -> {
                    if (state.data != "No tasks to prioritize!") {
                        GlassCard(
                            icon = Icons.Rounded.Insights,
                            title = "AI Priority Suggestion",
                            content = state.data,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            onRefresh = { viewModel.getTaskPrioritization() }
                        )
                    }
                }
                is com.example.utils.Result.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
                }
            }
        }

        if (sortedTasks.isEmpty()) {
            item {
                Text(
                    text = "No tasks yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(sortedTasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onDelete = { taskToDelete = task },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
fun ExpensesContent(totalExpenses: Double, expenses: List<ExpenseEntity>, incomes: List<com.example.data.local.entity.IncomeEntity>, viewModel: DashboardViewModel, aiInsight: String, onAddIncome: () -> Unit) {
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val dailyBudget by viewModel.dailyBudget.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    var showBudgetDialog by remember { mutableStateOf(false) }

    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var incomeToDelete by remember { mutableStateOf<com.example.data.local.entity.IncomeEntity?>(null) }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense? You can undo this action using the popup at the bottom of the screen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        expenseToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (incomeToDelete != null) {
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text("Delete Income") },
            text = { Text("Are you sure you want to delete this income? You can undo this action using the popup at the bottom of the screen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        incomeToDelete?.let { viewModel.deleteIncome(it) }
                        incomeToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val transactions = remember(expenses, incomes) {
        (expenses + incomes).sortedByDescending { 
            if (it is ExpenseEntity) it.timestamp else (it as com.example.data.local.entity.IncomeEntity).timestamp 
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Finances",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row {
                    IconButton(onClick = onAddIncome) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Income",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showBudgetDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.AccountBalance,
                    title = "Monthly Budget",
                    value = com.example.utils.FinanceConfig.formatCurrency(monthlyBudget),
                    iconTint = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Savings,
                    title = "Daily Budget",
                    value = com.example.utils.FinanceConfig.formatCurrency(dailyBudget),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.AccountBalance,
                    title = "Total Income",
                    value = com.example.utils.FinanceConfig.formatCurrency(totalIncome),
                    iconTint = Color(0xFF81C784)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Wallet,
                    title = "Total Spent",
                    value = com.example.utils.FinanceConfig.formatCurrency(totalExpenses),
                    iconTint = MaterialTheme.colorScheme.error
                )
            }
        }
        item {
            val remaining = (monthlyBudget + totalIncome - totalExpenses).coerceAtLeast(0.0)
            StatCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Savings,
                title = "Remaining Balance",
                value = com.example.utils.FinanceConfig.formatCurrency(remaining),
                iconTint = if (remaining > 0) Color(0xFF81C784) else MaterialTheme.colorScheme.error
            )
        }
        item {
            OutlinedButton(
                onClick = { viewModel.getBudgetSuggestion() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Get AI Spending Suggestion", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            if (aiInsight.isNotBlank() && aiInsight != "No insights yet.") {
                GlassCard(
                    icon = Icons.Rounded.Insights,
                    title = "AI Budget Suggestion",
                    content = aiInsight,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    onRefresh = { viewModel.getBudgetSuggestion() }
                )
            }
        }
        if (expenses.isNotEmpty()) {
            item {
                ExpensePieChart(expenses = expenses)
            }
            item {
                SpendingTrendChart(expenses = expenses)
            }
        }
        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (transactions.isEmpty()) {
            item {
                Text(
                    text = "No transactions recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(transactions, key = { 
                if (it is ExpenseEntity) "exp_${it.id}" else "inc_${(it as com.example.data.local.entity.IncomeEntity).id}" 
            }) { transaction ->
                if (transaction is ExpenseEntity) {
                    ExpenseItem(
                        expense = transaction,
                        onDelete = { expenseToDelete = transaction },
                        modifier = Modifier.animateItem()
                    )
                } else if (transaction is com.example.data.local.entity.IncomeEntity) {
                    IncomeItem(
                        income = transaction,
                        onDelete = { incomeToDelete = transaction },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showBudgetDialog) {
        var monthlyBudgetInput by remember { mutableStateOf(monthlyBudget.toString()) }
        var dailyBudgetInput by remember { mutableStateOf(dailyBudget.toString()) }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Budgets") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = monthlyBudgetInput,
                        onValueChange = { monthlyBudgetInput = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Monthly Budget") }
                    )
                    OutlinedTextField(
                        value = dailyBudgetInput,
                        onValueChange = { dailyBudgetInput = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Daily Budget") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    monthlyBudgetInput.toDoubleOrNull()?.let {
                        viewModel.updateMonthlyBudget(it)
                    }
                    dailyBudgetInput.toDoubleOrNull()?.let {
                        viewModel.updateDailyBudget(it)
                    }
                    showBudgetDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VaultContent(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    isDynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    viewModel: DashboardViewModel,
    onViewDiagnostics: () -> Unit
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val currencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()
    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(userName) }

    val stateProvider = org.koin.compose.koinInject<com.example.core.ai.GemmaLocalStateProvider>()
    var autoBackupEnabled by remember { mutableStateOf(stateProvider.isAutoBackupEnabled()) }
    var autoBackupInterval by remember { mutableStateOf(stateProvider.getAutoBackupInterval()) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Local AI History?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear your local AI conversation history and reset the configuration state? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        stateProvider.clearHistory()
                        com.example.core.ai.GemmaLocalConfig.resetState()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear and Reset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "USER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditingName) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (nameInput.isNotBlank()) {
                        viewModel.updateUserName(nameInput)
                    }
                    isEditingName = false
                }) {
                    Icon(Icons.Rounded.TaskAlt, contentDescription = "Save Name", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Text("Name: $userName", style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = {
                    nameInput = userName
                    isEditingName = true
                }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Text(
            text = "CURRENCY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val currencies = listOf(
                "BDT" to "৳ BDT",
                "USD" to "$ USD",
                "EUR" to "€ EUR",
                "INR" to "₹ INR",
                "GBP" to "£ GBP"
            )
            currencies.forEach { (code, label) ->
                val selected = currencyCode == code
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateCurrencyCode(code) },
                    label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }
        
        Text(
            text = "APPEARANCE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dynamic Color (Android 12+)", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isDynamicColor, onCheckedChange = onDynamicColorChange)
        }

        if (!isDynamicColor) {
            Text("Select Theme:", style = MaterialTheme.typography.bodyMedium)
            
            val themes = AppTheme.values()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onThemeChange(theme) }
                            .background(if (currentTheme == theme) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = theme.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentTheme == theme) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentTheme == theme) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Text(
            text = "LOCAL AI DATA & PRIVATE STORAGE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Automated Backup Enabled Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-backup Local Chat History", style = MaterialTheme.typography.bodyLarge)
                Text("Periodically generates a secure offline JSON backup file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = autoBackupEnabled,
                onCheckedChange = {
                    autoBackupEnabled = it
                    stateProvider.setAutoBackupEnabled(it)
                }
            )
        }

        // Automated Backup Interval Choice (Chips)
        if (autoBackupEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup frequency:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val intervals = listOf("daily" to "Daily", "weekly" to "Weekly")
                    intervals.forEach { (value, label) ->
                        val selected = autoBackupInterval == value
                        FilterChip(
                            selected = selected,
                            onClick = {
                                autoBackupInterval = value
                                stateProvider.setAutoBackupInterval(value)
                            },
                            label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }

        // Clear Local History Button
        Button(
            onClick = { showClearHistoryDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Local Chat History & Reset AI", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        Text(
            text = "DIAGNOSTICS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Button(
            onClick = onViewDiagnostics,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Insights, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View AI Diagnostics Logs", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun GemmaConfigPanel() {
    val stateProvider = org.koin.compose.koinInject<com.example.core.ai.GemmaLocalStateProvider>()
    val modelState by com.example.core.ai.GemmaLocalConfig.modelState.collectAsStateWithLifecycle()
    val hardware by com.example.core.ai.GemmaLocalConfig.hardwareAcceleration.collectAsStateWithLifecycle()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    var temp by remember { mutableStateOf(com.example.core.ai.GemmaLocalConfig.temperature) }
    var latency by remember { mutableStateOf(com.example.core.ai.GemmaLocalConfig.simulatedLatencyMs.toFloat()) }
    var forceInitFail by remember { mutableStateOf(com.example.core.ai.GemmaLocalConfig.forceInitializationFailure) }
    var forceTimeout by remember { mutableStateOf(com.example.core.ai.GemmaLocalConfig.forceInferenceTimeout) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Local AI Co-Pilot Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("On-device AI configuration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                // Status chip
                val (statusText, statusBg, statusTextClr) = when (modelState) {
                    com.example.core.ai.GemmaModelState.UNINITIALIZED -> Triple("OFFLINE", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant)
                    com.example.core.ai.GemmaModelState.INITIALIZING -> Triple("INITIALIZING...", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    com.example.core.ai.GemmaModelState.INITIALIZED -> Triple("ONLINE / ACTIVE", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                    com.example.core.ai.GemmaModelState.FAILED -> Triple("LOAD FAILED", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(statusText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusTextClr)
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // Hardware Acceleration
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("HARDWARE NEURAL ACCELERATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.core.ai.GemmaHardwareAcceleration.values().forEach { accelerationType ->
                        val selected = hardware == accelerationType
                        Button(
                            onClick = { com.example.core.ai.GemmaLocalConfig.setHardware(accelerationType) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(accelerationType.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            
            // Parameter Sliders
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MODEL INFERENCE TUNING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                // Temperature
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Creativity (Temperature): ${String.format("%.1f", temp)}", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = temp,
                    onValueChange = {
                        temp = it
                        com.example.core.ai.GemmaLocalConfig.temperature = it
                    },
                    valueRange = 0.1f..1.0f,
                    steps = 8,
                    modifier = Modifier.height(24.dp)
                )
                
                // Simulated Latency
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Inference Latency: ${latency.toInt()} ms", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = latency,
                    onValueChange = {
                        latency = it
                        com.example.core.ai.GemmaLocalConfig.simulatedLatencyMs = it.toLong()
                    },
                    valueRange = 200f..5000f,
                    modifier = Modifier.height(24.dp)
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            // Test Switches
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("DIAGNOSTIC TEST SCENARIOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Force load failure", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Triggers resource allocation errors during weight initialization", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = forceInitFail,
                        onCheckedChange = {
                            forceInitFail = it
                            com.example.core.ai.GemmaLocalConfig.forceInitializationFailure = it
                            if (it) {
                                com.example.core.ai.GemmaLocalConfig.resetState()
                            }
                        }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Force inference timeout (8s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Triggers a cancellation timeout to test UI boundaries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = forceTimeout,
                        onCheckedChange = {
                            forceTimeout = it
                            com.example.core.ai.GemmaLocalConfig.forceInferenceTimeout = it
                        }
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Sync & Backup section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PERSISTENT DATA MANAGEMENT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Local History Backup", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Syncs state database & exports history to a secure JSON file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            val filePath = stateProvider.syncAndExportHistoryToFile()
                            if (filePath != null) {
                                val jsonStr = stateProvider.syncAndExportHistory()
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(jsonStr))
                                exportStatusMessage = "Sync Complete!\nExported to:\n$filePath\n\n(JSON backup copied to clipboard)"
                            } else {
                                exportStatusMessage = "Export failed. Check storage"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync & Export", style = MaterialTheme.typography.labelLarge)
                    }
                }
                
                exportStatusMessage?.let { status ->
                    androidx.compose.material3.Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    com.example.core.ai.GemmaLocalConfig.resetState()
                    scope.launch {
                        com.example.core.ai.GemmaLocalConfig.initializeModel()
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Initialize / Reset Model")
                }
            }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun APIDiagnosticsDialog(onDismiss: () -> Unit) {
    val logs by com.example.utils.APIDiagnosticLogger.logs.collectAsStateWithLifecycle()
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AI Core Diagnostics") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(onClick = { com.example.utils.APIDiagnosticLogger.clear() }) {
                            Text("Clear")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Item 1: Settings Config Card
                item {
                    GemmaConfigPanel()
                }

                // Item 2: Diagnostics Header Divider
                item {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        Text(
                            text = "DIAGNOSTIC INFERENCE LOGS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (logs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Insights, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("No AI logs recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(logs, key = { it.id }) { log ->
                        androidx.compose.material3.Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = if (log.isSuccess) androidx.compose.material3.MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) else androidx.compose.material3.MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(if (log.isSuccess) Color(0xFF81C784) else androidx.compose.material3.MaterialTheme.colorScheme.error)
                                        )
                                        Text(log.feature, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Text(log.formattedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                Text("Latency: ${log.durationMs}ms", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                
                                Text("Request Summary:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(log.requestSummary, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }

                                if (log.payloadStructure != null) {
                                    Text("Payload Structure:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(log.payloadStructure, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                }
                                
                                if (log.isSuccess && log.responseSummary != null) {
                                    Text("Response Preview:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(log.responseSummary.take(500), style = MaterialTheme.typography.bodySmall)
                                    }
                                } else if (!log.isSuccess && (log.errorMessage != null || log.apiResponseErrors != null)) {
                                    Text("Error Message:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        val displayErr = log.apiResponseErrors ?: log.errorMessage ?: "Unknown error"
                                        Text(displayErr, style = MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    icon: ImageVector,
    title: String,
    content: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .scrollAnimation()
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LIFEOS AI INTELLIGENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                }
                if (onRefresh != null) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp).padding(start = 8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Insight",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onRefresh?.invoke() },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Optimize Now", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                ) {
                    Text("Review Changes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun TaskCompletionCard(tasks: List<TaskEntity>) {
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val completionRate = if (totalTasks > 0) {
        (completedTasks.toFloat() / totalTasks) * 100
    } else {
        0f
    }
    
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = completionRate / 100f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "TaskProgress"
    )
    
    val isLevelUp = completionRate >= 100f && totalTasks > 0
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLevelUp) 1.05f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "LevelUpScale"
    )

    Box(
        modifier = Modifier
            .scrollAnimation()
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isLevelUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TASK COMPLETION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isLevelUp) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Level Up",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${completionRate.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLevelUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$completedTasks of $totalTasks completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (isLevelUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun FinancialHealthCard(totalExpenses: Double, monthlyBudget: Double) {
    val healthScore = if (monthlyBudget > 0) {
        ((1.0 - (totalExpenses / monthlyBudget)) * 100).coerceIn(0.0, 100.0).toInt()
    } else {
        0
    }
    
    val healthColor = when {
        healthScore >= 80 -> Color(0xFF4CAF50)
        healthScore >= 50 -> Color(0xFFFFC107)
        else -> MaterialTheme.colorScheme.error
    }
    
    val healthGrade = when {
        healthScore >= 80 -> "A"
        healthScore >= 70 -> "B"
        healthScore >= 50 -> "C"
        else -> "D"
    }

    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (healthScore / 100f).coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "HealthProgress"
    )

    val isLevelUp = healthScore >= 80
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLevelUp) 1.05f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "LevelUpScale"
    )

    Box(
        modifier = Modifier
            .scrollAnimation()
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (isLevelUp) healthColor else MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FINANCIAL HEALTH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Score: $healthScore/100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(healthColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Grade $healthGrade",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = healthColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = healthColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (isLevelUp) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = healthColor,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Savings,
                    contentDescription = null,
                    tint = healthColor,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun SpendingTrendChart(expenses: List<ExpenseEntity>) {
    val last7Days = remember(expenses) {
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val grouped = expenses.groupBy {
            val expenseDate = java.util.Calendar.getInstance().apply { timeInMillis = it.timestamp }
            expenseDate.apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        
        val format = java.text.SimpleDateFormat("E", java.util.Locale.getDefault())
        val fullFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        (6 downTo 0).map { daysAgo ->
            val date = java.util.Calendar.getInstance().apply {
                timeInMillis = today.timeInMillis
                add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val label = format.format(date.time)
            val fullLabel = fullFormat.format(date.time)
            val dayExpenses = grouped[date.timeInMillis] ?: emptyList()
            val total = dayExpenses.sumOf { it.amount }
            Triple(label, total, Pair(fullLabel, dayExpenses))
        }
    }

    val maxAmount = last7Days.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "7-Day Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    last7Days.forEachIndexed { index, (label, amount, _) ->
                        val heightRatio = (amount / maxAmount).toFloat()
                        val isSelected = selectedIndex == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            selectedIndex = index
                                            tryAwaitRelease()
                                            selectedIndex = null
                                        }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(heightRatio.coerceAtLeast(0.05f))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                // Tooltip Overlay
                selectedIndex?.let { index ->
                    val data = last7Days[index]
                    val fullLabel = data.third.first
                    val dayExpenses = data.third.second
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = fullLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (dayExpenses.isEmpty()) {
                                Text(
                                    text = "No expenses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                                )
                            } else {
                                val groupedByCategory = dayExpenses.groupBy { it.category }
                                groupedByCategory.forEach { (category, expenses) ->
                                    val catTotal = expenses.sumOf { it.amount }
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.9f),
                                            modifier = Modifier.widthIn(max = 100.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = com.example.utils.FinanceConfig.formatCurrency(catTotal),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.inverseOnSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    iconTint: Color
) {
    Box(
        modifier = modifier
            .scrollAnimation()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(16.dp)
            .height(100.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun GoalItem(
    goal: com.example.data.local.entity.GoalEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .scrollAnimation()
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (goal.isCompleted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .border(1.dp, if (goal.isCompleted) MaterialTheme.colorScheme.outlineVariant else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (goal.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (goal.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (goal.isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                )
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (goal.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (goal.description.isNotBlank()) {
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TaskItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverdue = !task.isCompleted && (System.currentTimeMillis() - task.timestamp > 24 * 60 * 60 * 1000)
    
    val priorityColor = when (task.priority) {
        0 -> Color(0xFF4CAF50)
        1 -> Color(0xFFFF9800)
        2 -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }
    
    val priorityText = when (task.priority) {
        0 -> "LOW"
        1 -> "MEDIUM"
        2 -> "HIGH"
        else -> ""
    }

    Row(
        modifier = modifier
            .scrollAnimation()
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (task.isCompleted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background)
            .border(
                width = if (task.priority == 2 || isOverdue) 2.dp else 1.dp,
                color = when {
                    isOverdue -> MaterialTheme.colorScheme.error
                    task.priority == 2 -> priorityColor
                    task.isCompleted -> MaterialTheme.colorScheme.outlineVariant
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                    contentDescription = "Toggle Complete",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isOverdue) {
                    Text(
                        text = "OVERDUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = priorityColor.copy(alpha = 0.1f),
                    contentColor = priorityColor,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = priorityText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = task.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Task",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Daily Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onAdd(title, description) }
            ) {
                Text("Add Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onAdd: (String, Int, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(1) } // 0 = Low, 1 = Med, 2 = High
    var category by remember { mutableStateOf("Personal") }
    
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilterChip(selected = priority == 0, onClick = { priority = 0 }, label = { Text("Low") })
                    FilterChip(selected = priority == 1, onClick = { priority = 1 }, label = { Text("Medium") })
                    FilterChip(selected = priority == 2, onClick = { priority = 2 }, label = { Text("High") })
                }
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allCats = remember(customCategories) {
                        listOf("Work", "Personal", "Health", "Other") + customCategories
                    }
                    allCats.forEach { cat ->
                        FilterChip(
                            selected = !showNewCategoryInput && category == cat,
                            onClick = {
                                category = cat
                                showNewCategoryInput = false
                            },
                            label = { Text(cat) }
                        )
                    }
                    FilterChip(
                        selected = showNewCategoryInput,
                        onClick = { showNewCategoryInput = !showNewCategoryInput },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Custom")
                            }
                        }
                    )
                }
                
                if (showNewCategoryInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCategoryText,
                            onValueChange = { newCategoryText = it },
                            placeholder = { Text("Category name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                if (newCategoryText.isNotBlank()) {
                                    val cleaned = newCategoryText.trim()
                                    onAddCustomCategory(cleaned)
                                    category = cleaned
                                    newCategoryText = ""
                                    showNewCategoryInput = false
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onAdd(text, priority, category) }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun IncomeItem(
    income: com.example.data.local.entity.IncomeEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .scrollAnimation()
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "INCOME",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF81C784)
            )
            Text(
                text = income.source,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (income.note.isNotBlank()) {
                Text(
                    text = income.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "+${com.example.utils.FinanceConfig.formatCurrency(income.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF81C784)
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Income",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun ExpenseItem(
    expense: ExpenseEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .scrollAnimation()
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.category,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = expense.note.ifBlank { "Uncategorized Item" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = com.example.utils.FinanceConfig.formatCurrency(expense.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Expense",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onAdd: (Double, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food & Dining") }

    val categories = remember(customCategories) {
        listOf("Food & Dining", "Transportation", "Shopping", "Entertainment", "Bills & Utilities", "Other") + customCategories
    }
    var expanded by remember { mutableStateOf(false) }
    
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showNewCategoryInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCategoryText,
                            onValueChange = { newCategoryText = it },
                            label = { Text("New Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                if (newCategoryText.isNotBlank()) {
                                    val cleaned = newCategoryText.trim()
                                    onAddCustomCategory(cleaned)
                                    category = cleaned
                                    newCategoryText = ""
                                    showNewCategoryInput = false
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Add")
                        }
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Add Custom Category", color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    showNewCategoryInput = true
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    if (parsedAmount != null && item.isNotBlank() && category.isNotBlank()) {
                        onAdd(parsedAmount, category, item)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    onDismiss: () -> Unit,
    onAdd: (Double, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Income") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Income Source") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    if (parsedAmount != null && source.isNotBlank()) {
                        onAdd(parsedAmount, source, note)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun ExpensePieChart(expenses: List<ExpenseEntity>) {
    if (expenses.isEmpty()) return

    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val total = categoryTotals.sumOf { it.second }
    val colors = listOf(
        Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
        Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC)
    )

    var animationProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(expenses) {
        animationProgress = 0f
        androidx.compose.animation.core.animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }

    Column(
        modifier = Modifier
            .scrollAnimation()
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var currentAngle = -90f
                categoryTotals.forEachIndexed { index, (category, amount) ->
                    val sweepAngle = ((amount / total) * 360f).toFloat() * animationProgress
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = currentAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = size
                    )
                    currentAngle += ((amount / total) * 360f).toFloat() // keep the next start angle correct even while animating
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        categoryTotals.forEachIndexed { index, (category, amount) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(colors[index % colors.size]))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = com.example.utils.FinanceConfig.formatCurrency(amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDialog(
    chatHistory: List<com.example.core.ai.ChatMessage>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSendMessage: (String, Boolean) -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Chat with LifeOS AI") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    }
                )
            },
            bottomBar = {
                var message by remember { mutableStateOf("") }
                var useThinking by remember { mutableStateOf(false) }
                val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Insights, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (useThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "High Thinking Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (useThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useThinking,
                            onCheckedChange = { useThinking = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message LifeOS AI...") },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (message.isNotBlank()) {
                                        onSendMessage(message, useThinking)
                                        message = ""
                                        keyboardController?.hide()
                                    }
                                }
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (message.isNotBlank()) {
                                    onSendMessage(message, useThinking)
                                    message = ""
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        ) { padding ->
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LaunchedEffect(chatHistory.size) {
                if (chatHistory.isNotEmpty()) {
                    listState.animateScrollToItem(chatHistory.size - 1)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(chatHistory) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .clip(RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 16.dp
                                ))
                                .background(if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp)
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddDialog(
    onDismiss: () -> Unit,
    onOptionSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("✨", style = MaterialTheme.typography.titleLarge)
                Text("Quick Add", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "What would you like to create?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                QuickAddButton(
                    icon = Icons.AutoMirrored.Rounded.ListAlt,
                    title = "New Task",
                    subtitle = "Manage your active todo items",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onOptionSelected("task") }
                )
                
                QuickAddButton(
                    icon = Icons.Rounded.TaskAlt,
                    title = "New Daily Goal",
                    subtitle = "Track daily milestone progress",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onOptionSelected("goal") }
                )
                
                QuickAddButton(
                    icon = Icons.Rounded.AccountBalance,
                    title = "New Expense",
                    subtitle = "Log daily spending & budget",
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onOptionSelected("expense") }
                )
                
                QuickAddButton(
                    icon = Icons.Rounded.Savings,
                    title = "New Income",
                    subtitle = "Record incoming funds",
                    color = Color(0xFF2E7D32),
                    onClick = { onOptionSelected("income") }
                )

                QuickAddButton(
                    icon = Icons.Rounded.Wallet,
                    title = "New Debt/Loan Record",
                    subtitle = "Track borrowings or lendings",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { onOptionSelected("debt") }
                )

                QuickAddButton(
                    icon = Icons.AutoMirrored.Rounded.Notes,
                    title = "New Note",
                    subtitle = "Quick markdown note capture",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onOptionSelected("note") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun QuickAddButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}