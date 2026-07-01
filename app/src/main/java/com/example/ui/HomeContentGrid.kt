package com.example.ui

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

@Composable
fun HomeContent(viewModel: DashboardViewModel, tasks: List<TaskEntity>, goals: List<GoalEntity>, totalExpenses: Double, aiInsight: String, onChatClicked: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val welcomeMessage by viewModel.welcomeMessage.collectAsStateWithLifecycle()
    val widgetLayout by viewModel.widgetLayout.collectAsStateWithLifecycle()
    var editMode by remember { mutableStateOf(false) }

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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            itemsIndexed(widgets, key = { _, pair -> pair.first }, span = { _, pair -> GridItemSpan(pair.second) }) { index, pair ->
                val id = pair.first
                val size = pair.second

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (id) {
                        "welcome" -> WelcomeWidget(userName, welcomeMessage, onChatClicked)
                        "search" -> SearchWidget(searchQuery, { searchQuery = it }, viewModel)
                        "stats" -> StatsWidget(tasks, totalExpenses)
                        "health" -> FinancialHealthCard(totalExpenses = totalExpenses, monthlyBudget = monthlyBudget)
                        "completion" -> TaskCompletionCard(tasks = tasks)
                        "goals" -> GoalsWidget(goals, viewModel)
                        "tasks" -> TasksWidget(tasks, viewModel)
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
                                }) { Icon(if (size == 1) Icons.Rounded.KeyboardArrowRight else Icons.Rounded.KeyboardArrowLeft, contentDescription = "Resize") }
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
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "Chat with Jarvis", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(welcomeMessage, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary, lineHeight = 24.sp)
        }
    }
}

@Composable
fun SearchWidget(searchQuery: String, onSearchQueryChange: (String) -> Unit, viewModel: DashboardViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(modifier = Modifier.scrollAnimation(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask Jarvis to analyze or schedule...") },
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
    Column {
        Text("TODAY'S STACK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text("No tasks. You're all caught up.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            tasks.take(3).forEach { task ->
                TaskItem(task = task, onToggle = { viewModel.toggleTask(task) }, onDelete = { viewModel.deleteTask(task) }, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}
