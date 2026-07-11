
package com.example.ui
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.TaskEntity
import com.example.feature.dashboard.presentation.DashboardViewModel
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color

import com.example.data.local.entity.JournalEntity
import com.example.ui.notes.NotesWidget

@Composable
fun HomeContent(
    viewModel: DashboardViewModel,
    tasks: List<TaskEntity>,
    goals: List<GoalEntity>,
    journals: List<JournalEntity>,
    totalExpenses: Double,
    aiInsight: String,
    onChatClicked: () -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onViewNotes: () -> Unit,
    onAddNote: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val welcomeMessage by viewModel.welcomeMessage.collectAsStateWithLifecycle()
    val widgetLayout by viewModel.widgetLayout.collectAsStateWithLifecycle()
    val weeklySummary by viewModel.weeklySummary.collectAsStateWithLifecycle()
    var editMode by remember { mutableStateOf(false) }

    val debtViewModel: com.example.ui.debt.DebtViewModel = org.koin.androidx.compose.koinViewModel()
    val totalReceivables by debtViewModel.totalReceivables.collectAsStateWithLifecycle()
    val totalPayables by debtViewModel.totalPayables.collectAsStateWithLifecycle()
    val overdueCount by debtViewModel.overdueCount.collectAsStateWithLifecycle()
    val dueTodayCount by debtViewModel.dueTodayCount.collectAsStateWithLifecycle()

    val widgets = remember(widgetLayout) {
        widgetLayout.split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to parts[1].toInt() else null
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (editMode) "Edit Dashboard" else "Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { editMode = !editMode }) {
                Text(if (editMode) "Done" else "Edit layout")
            }
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            itemsIndexed(widgets, key = { _, pair -> pair.first }, span = { _, pair -> if (pair.second == 2) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane }) { index, pair ->
                val id = pair.first
                val size = pair.second

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (id) {
                        "welcome" -> WelcomeWidget(userName, welcomeMessage, onChatClicked)
                        "summary" -> WeeklySummaryWidget(weeklySummary, onRefresh = { viewModel.getWeeklySummary() })
                        "jarvis_insights" -> JarvisInsightCard(viewModel, debtViewModel, onNavigateToDebt = { onNavigateToTab(3) })
                        "chat" -> ChatWidget(viewModel)
                        "search" -> SearchWidget(searchQuery, { searchQuery = it }, viewModel, aiInsight)
                        "stats" -> StatsWidget(tasks, totalExpenses)
                        "health" -> FinancialHealthCard(totalExpenses = totalExpenses, monthlyBudget = monthlyBudget)
                        "completion" -> TaskCompletionCard(tasks = tasks)
                        "goals" -> GoalsWidget(goals, viewModel)
                        "tasks" -> TasksWidget(tasks, viewModel)
                        "journals" -> JournalsWidget(journals, viewModel)
                        "forecast" -> ForecastWidget(viewModel)
                        "notes" -> {
                            val notes by viewModel.notes.collectAsStateWithLifecycle()
                            NotesWidget(
                                notes = notes,
                                onViewAll = onViewNotes,
                                onAddNote = onAddNote
                            )
                        }
                        "debt" -> DebtWidget(
                            totalReceivables = totalReceivables,
                            totalPayables = totalPayables,
                            overdueCount = overdueCount,
                            dueTodayCount = dueTodayCount,
                            size = size,
                            onNavigateToDebt = { onNavigateToTab(3) }
                        )
                    }

                    if (editMode) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = {
                                    if (index > 0) {
                                        val mut = widgets.toMutableList()
                                        val temp = mut[index]
                                        mut[index] = mut[index - 1]
                                        mut[index - 1] = temp
                                        viewModel.updateWidgetLayout(mut.joinToString(",") { "${it.first}:${it.second}" })
                                    }
                                }) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move Up") }
                                IconButton(onClick = {
                                    if (index < widgets.size - 1) {
                                        val mut = widgets.toMutableList()
                                        val temp = mut[index]
                                        mut[index] = mut[index + 1]
                                        mut[index + 1] = temp
                                        viewModel.updateWidgetLayout(mut.joinToString(",") { "${it.first}:${it.second}" })
                                    }
                                }) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move Down") }
                                IconButton(onClick = {
                                    val mut = widgets.toMutableList()
                                    mut[index] = id to if (size == 1) 2 else 1
                                    viewModel.updateWidgetLayout(mut.joinToString(",") { "${it.first}:${it.second}" })
                                }) { Icon(if (size == 1) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Resize") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeWidget(userName: String, welcomeMessage: String, onChatClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .scrollAnimation()
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
            .padding(24.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("LIFEOS AI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Text("Hello, $userName", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)).clickable { onChatClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "Chat with LifeOS AI", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(welcomeMessage, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary, lineHeight = 24.sp)
        }
    }
}



@Composable
fun SearchWidget(searchQuery: String, onSearchQueryChange: (String) -> Unit, viewModel: DashboardViewModel, aiInsight: String) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(modifier = Modifier.scrollAnimation(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask LifeOS AI to analyze or schedule...") },
            shape = RoundedCornerShape(20.dp),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        viewModel.askJarvis(searchQuery)
                        onSearchQueryChange("")
                        keyboardController?.hide()
                    }) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (searchQuery.isNotEmpty()) {
                        viewModel.askJarvis(searchQuery)
                        onSearchQueryChange("")
                        keyboardController?.hide()
                    }
                }
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                onClick = { viewModel.analyzeHabits("Productivity score: 85%. Tasks completed on time: 90%. Focus time: 4 hours.") },
                label = { Text("Analyze Habits") },
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            SuggestionChip(
                onClick = { viewModel.getBudgetSuggestion() },
                label = { Text("Budget Advice") },
                icon = { Icon(Icons.Rounded.Savings, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        
        if (aiInsight.isNotBlank() && aiInsight != "No insights yet.") {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(
                        text = aiInsight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsWidget(tasks: List<TaskEntity>, totalExpenses: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.TaskAlt,
            title = "Tasks",
            value = "${tasks.count { !it.isCompleted }} pending",
            iconTint = MaterialTheme.colorScheme.primary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Wallet,
            title = "Spent Today",
            value = com.example.utils.FinanceConfig.formatCurrency(totalExpenses),
            iconTint = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun GoalsWidget(goals: List<GoalEntity>, viewModel: DashboardViewModel) {
    Column {
        Text("DAILY GOALS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        if (goals.isEmpty()) {
            Text("No daily goals.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            goals.take(3).forEach { goal ->
                GoalItem(goal = goal, onToggle = { viewModel.toggleGoal(goal) }, onDelete = { viewModel.deleteGoal(goal) }, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
fun TasksWidget(tasks: List<TaskEntity>, viewModel: DashboardViewModel) {
    var newTaskTitle by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    
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

    Column {
        Text("TODAY'S STACK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = newTaskTitle,
            onValueChange = { newTaskTitle = it },
            placeholder = { Text("Quick add task...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (newTaskTitle.isNotBlank()) {
                    viewModel.addTask(newTaskTitle, 1)
                    newTaskTitle = ""
                    keyboardController?.hide()
                }
            }),
            trailingIcon = {
                IconButton(onClick = {
                    if (newTaskTitle.isNotBlank()) {
                        viewModel.addTask(newTaskTitle, 1)
                        newTaskTitle = ""
                        keyboardController?.hide()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
        
        if (tasks.isEmpty()) {
            Text("No tasks. You're all caught up.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            val sortedTasks = tasks.sortedWith(compareByDescending<TaskEntity> { !it.isCompleted }.thenByDescending { it.priority }.thenByDescending { it.timestamp })
            sortedTasks.take(3).forEach { task ->
                TaskItem(task = task, onToggle = { viewModel.toggleTask(task) }, onDelete = { taskToDelete = task }, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
fun ForecastWidget(viewModel: DashboardViewModel) {
    val forecastResult by viewModel.spendingForecast.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.getSpendingForecast()
    }
    
    Box(
        modifier = Modifier
            .scrollAnimation()
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp)
            .clickable { viewModel.getSpendingForecast() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Analytics,
                    contentDescription = "Forecast",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI SPENDING FORECAST",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            when (val state = forecastResult) {
                is com.example.utils.Result.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is com.example.utils.Result.Success -> {
                    Text(
                        text = state.data,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is com.example.utils.Result.Error -> {
                    Text(
                        text = "Unable to forecast. Tap to retry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun JournalsWidget(journals: List<JournalEntity>, viewModel: DashboardViewModel) {
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    var journalToDelete by remember { mutableStateOf<JournalEntity?>(null) }

    if (journalToDelete != null) {
        AlertDialog(
            onDismissRequest = { journalToDelete = null },
            title = { Text("Delete Journal Entry") },
            text = { Text("Are you sure you want to delete this journal entry? You can undo this action using the popup at the bottom of the screen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        journalToDelete?.let { viewModel.deleteJournal(it) }
                        journalToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { journalToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Text("LATEST JOURNALS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            placeholder = { Text("Title...") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = newContent,
            onValueChange = { newContent = it },
            placeholder = { Text("Write your thoughts...") },
            modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = {
                    if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                        viewModel.addJournal(newTitle, newContent)
                        newTitle = ""
                        newContent = ""
                        keyboardController?.hide()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add Journal", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
        
        if (journals.isEmpty()) {
            Text("No journals yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            journals.take(2).forEach { journal ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(journal.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { journalToDelete = journal }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(journal.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatWidget(viewModel: DashboardViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf("") }
    var useThinking by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("LIFEOS AI ASSISTANT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "High Thinking",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (useThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = useThinking,
                    onCheckedChange = { useThinking = it },
                    modifier = Modifier.scale(0.7f)
                )
            }
        }
        
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = false
        ) {
            items(chatHistory.size) { index ->
                val chat = chatHistory[index]
                val isUser = chat.role == "user"
                if (!isUser || !chat.text.startsWith("User's current tasks:")) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (isUser) chat.text else chat.text,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            if (isChatLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterStart) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask LifeOS AI...") },
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (message.isNotBlank()) {
                            viewModel.askJarvisChat(message, useThinking)
                            message = ""
                            keyboardController?.hide()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (message.isNotBlank()) {
                        viewModel.askJarvisChat(message, useThinking)
                        message = ""
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun DebtWidget(
    totalReceivables: Double,
    totalPayables: Double,
    overdueCount: Int,
    dueTodayCount: Int,
    size: Int,
    onNavigateToDebt: () -> Unit
) {
    Card(
        modifier = Modifier
            .scrollAnimation()
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onNavigateToDebt() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Wallet,
                        contentDescription = "Debt Widget",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DEBT & TRUST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }

            if (size == 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Receivables
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Receivable",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = com.example.utils.FinanceConfig.formatCurrency(totalReceivables),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    // Payables
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Payable",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = com.example.utils.FinanceConfig.formatCurrency(totalPayables),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            } else {
                // Size 1 layout (vertical list format)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Receivable",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = com.example.utils.FinanceConfig.formatCurrency(totalReceivables),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Payable",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = com.example.utils.FinanceConfig.formatCurrency(totalPayables),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            // Alerts
            if (overdueCount > 0 || dueTodayCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (overdueCount > 0) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Overdue",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$overdueCount Overdue",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (dueTodayCount > 0) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = "Due Today",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$dueTodayCount Due Today",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All loan accounts are currently up to date.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

