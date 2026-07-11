@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.debt

import com.example.ui.scrollAnimation
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DebtTransactionEntity
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.PersonEntity
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DebtScreen(
    viewModel: DebtViewModel = koinViewModel(),
    showAddDebtInitially: Boolean = false,
    onAddDebtConsumed: () -> Unit = {}
) {
    val persons by viewModel.persons.collectAsStateWithLifecycle()
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()

    val totalReceivables by viewModel.totalReceivables.collectAsStateWithLifecycle()
    val totalPayables by viewModel.totalPayables.collectAsStateWithLifecycle()
    val customDebtCats by viewModel.customDebtCategories.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<DebtType?>(null) } // null = All, RECEIVABLE = Lent, PAYABLE = Borrowed

    // Dialog & Flow States
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtTransactionEntity?>(null) }
    var showDebtDetailsForDebt by remember { mutableStateOf<DebtTransactionEntity?>(null) }
    var showAddPaymentForDebt by remember { mutableStateOf<DebtTransactionEntity?>(null) }

    val aiInsight by viewModel.aiInsight.collectAsStateWithLifecycle()
    val isRefreshingInsight by viewModel.isRefreshingInsight.collectAsStateWithLifecycle()
    val isThinkingMode by viewModel.isThinkingMode.collectAsStateWithLifecycle()
    var isAiCardExpanded by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showAddDebtInitially) {
        if (showAddDebtInitially) {
            editingDebt = null
            showAddDebtDialog = true
            onAddDebtConsumed()
        }
    }

    LaunchedEffect(viewModel) {
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingDebt = null
                    showAddDebtDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Debt Log", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        val personMap = remember(persons) { persons.associateBy { it.id } }
        val debtPaidMap = remember(payments) { payments.groupBy { it.debtId }.mapValues { (_, list) -> list.sumOf { it.amount } } }
        val debtListState = com.example.utils.rememberSmoothLazyListState()

        val filteredDebts = debts.filter { debt ->
            val person = persons.find { it.id == debt.personId }
            val nameMatch = person?.name?.contains(searchQuery, ignoreCase = true) ?: false
            val amountMatch = debt.amount.toString().contains(searchQuery) ||
                    String.format("%.0f", debt.amount).contains(searchQuery)
            val typeMatch = selectedTypeFilter == null || debt.type == selectedTypeFilter
            (nameMatch || amountMatch) && typeMatch && !debt.isSettled
        }

        LazyColumn(
            state = debtListState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item {
                // Header / App Name area
                Row(
                    modifier = Modifier
                        .scrollAnimation()
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Debt & Loans",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Simple, frictionless tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                // Summary Cards Grid (Home Screen cards)
                Row(
                    modifier = Modifier
                        .scrollAnimation()
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "I Borrowed",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = com.example.utils.FinanceConfig.formatCurrency(totalPayables),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "I Lent",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = com.example.utils.FinanceConfig.formatCurrency(totalReceivables),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                val netBalance = totalReceivables - totalPayables
                val netColor = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                val netSign = if (netBalance >= 0) "+" else ""

                Card(
                    modifier = Modifier
                        .scrollAnimation()
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Balance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$netSign${com.example.utils.FinanceConfig.formatCurrency(netBalance)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = netColor
                        )
                    }
                }
            }

            item {
                // AI Advisor Card
                Card(
                    modifier = Modifier
                        .scrollAnimation()
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { isAiCardExpanded = !isAiCardExpanded },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "✨",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "LifeOS AI Financial Advisor",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isRefreshingInsight) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    IconButton(
                                        onClick = { viewModel.refreshDebtAiInsight() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Refresh Insight",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isAiCardExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (isAiCardExpanded) "Collapse AI Advisor" else "Expand AI Advisor",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = aiInsight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (isAiCardExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isAiCardExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // High Thinking Toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { viewModel.toggleThinkingMode() }
                                ) {
                                    Checkbox(
                                        checked = isThinkingMode,
                                        onCheckedChange = { viewModel.toggleThinkingMode() },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        text = "Deep Strategy (High-Thinking)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Consult Chat Button
                                Button(
                                    onClick = { 
                                        viewModel.clearChat()
                                        showChatDialog = true 
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ask LifeOS AI", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Search and Filters Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or amount...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .scrollAnimation()
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            item {
                // Chips filters for categories
                Row(
                    modifier = Modifier
                        .scrollAnimation()
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == DebtType.RECEIVABLE,
                        onClick = { selectedTypeFilter = DebtType.RECEIVABLE },
                        label = { Text("Lent") }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == DebtType.PAYABLE,
                        onClick = { selectedTypeFilter = DebtType.PAYABLE },
                        label = { Text("Borrowed") }
                    )
                }
            }

            item {
                // List Header
                Text(
                    text = "Active Outstanding",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .scrollAnimation()
                        .padding(vertical = 8.dp)
                )
            }

            if (filteredDebts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .scrollAnimation()
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No outstanding balances!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You are all caught up on your debts.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredDebts, key = { it.id }) { debt ->
                    val personName = personMap[debt.personId]?.name ?: "Unknown"
                    val paid = debtPaidMap[debt.id] ?: 0.0

                    SwipableDebtItem(
                        debt = debt,
                        personName = personName,
                        paidAmount = paid,
                        onMarkPaid = { viewModel.markPaid(debt.id) },
                        onDelete = { viewModel.deleteDebt(debt) },
                        onEdit = {
                            editingDebt = debt
                            showAddDebtDialog = true
                        },
                        onClick = {
                            showDebtDetailsForDebt = debt
                        },
                        modifier = Modifier.scrollAnimation()
                    )
                }
            }
        }
    }

    // Combined Add/Edit Dialog
    if (showAddDebtDialog) {
        val currentPerson = editingDebt?.let { debt -> persons.find { it.id == debt.personId } }
        AddDebtDialog(
            customCategories = customDebtCats,
            onAddCustomCategory = { cat -> viewModel.addCustomDebtCategory(cat) },
            editingDebt = editingDebt,
            editingPersonName = currentPerson?.name,
            editingPhoneNumber = currentPerson?.contactInfo,
            onDismiss = {
                showAddDebtDialog = false
                editingDebt = null
            },
            onSave = { personName, amount, type, date, note, dueDate, phone, reminder, category, attachment ->
                if (editingDebt == null) {
                    viewModel.quickAddDebt(
                        personName = personName,
                        amount = amount,
                        type = type,
                        borrowDate = date,
                        notes = note,
                        dueDate = dueDate,
                        phoneNumber = phone,
                        reminderEnabled = reminder,
                        category = category,
                        attachmentPath = attachment
                    )
                    viewModel.showSnackbar("Logged successfully!")
                } else {
                    viewModel.quickUpdateDebt(
                        debtId = editingDebt!!.id,
                        personName = personName,
                        amount = amount,
                        type = type,
                        borrowDate = date,
                        notes = note,
                        dueDate = dueDate,
                        phoneNumber = phone,
                        reminderEnabled = reminder,
                        category = category,
                        attachmentPath = attachment
                    )
                    viewModel.showSnackbar("Updated successfully!")
                }
                showAddDebtDialog = false
                editingDebt = null
            }
        )
    }

    var debtToDelete by remember { mutableStateOf<DebtTransactionEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<com.example.data.local.entity.DebtPaymentEntity?>(null) }

    if (debtToDelete != null) {
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text("Delete Loan Transaction") },
            text = { Text("Are you sure you want to delete this loan transaction? All associated partial payment records will also be permanently deleted. You can undo this action using the popup at the bottom of the screen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        debtToDelete?.let { viewModel.deleteDebt(it) }
                        debtToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (paymentToDelete != null) {
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("Delete Repayment Record") },
            text = { Text("Are you sure you want to delete this partial repayment record? You can undo this action using the popup at the bottom of the screen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        paymentToDelete?.let { viewModel.deletePayment(it) }
                        paymentToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Detail Pop-up
    showDebtDetailsForDebt?.let { debt ->
        val person = persons.find { it.id == debt.personId }
        val personName = person?.name ?: "Unknown"
        val paid = payments.filter { it.debtId == debt.id }.sumOf { it.amount }
        val debtPayments = remember(payments, debt.id) { payments.filter { it.debtId == debt.id } }

        DebtDetailsDialog(
            debt = debt,
            personName = personName,
            paidAmount = paid,
            payments = debtPayments,
            onDismiss = { showDebtDetailsForDebt = null },
            onAddPaymentClick = {
                showAddPaymentForDebt = debt
            },
            onDeleteClick = {
                debtToDelete = debt
            },
            onDeletePayment = {
                paymentToDelete = it
            }
        )
    }

    // Add Payment Pop-up
    showAddPaymentForDebt?.let { debt ->
        val person = persons.find { it.id == debt.personId }
        AddPaymentDialog(
            debt = debt,
            personName = person?.name ?: "Unknown",
            onDismiss = { showAddPaymentForDebt = null },
            onSave = { amount, notes ->
                viewModel.addPayment(debt.id, amount, notes)
                showAddPaymentForDebt = null
                viewModel.showSnackbar("Payment of ${com.example.utils.FinanceConfig.formatCurrency(amount)} recorded!")
            }
        )
    }

    // Jarvis Chat Dialog
    if (showChatDialog) {
        JarvisChatDialog(
            viewModel = viewModel,
            onDismiss = { showChatDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisChatDialog(
    viewModel: DebtViewModel,
    onDismiss: () -> Unit
) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val isThinkingMode by viewModel.isThinkingMode.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val listState = com.example.utils.rememberSmoothLazyListState()

    // Scroll to bottom whenever new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "📝 Draft SMS reminder",
        "📊 Payoff strategy",
        "🛡️ Overdue protection tips",
        "🇧🇩 বাংলায় পরামর্শ দিন"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✨", style = MaterialTheme.typography.titleLarge)
                    Column {
                        Text(
                            text = "Consult LifeOS AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isThinkingMode) "Strategic High-Thinking Mode" else "Instant Advisor Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isThinkingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Chat")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Messages List
                if (chatMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ask LifeOS AI Anything!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Get professional advice, polite SMS/WhatsApp reminders, or strategic repayment schedules. LifeOS AI is pre-loaded with your active loan portfolio context.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatMessages) { message ->
                            val isUser = message.role == "user"
                            val bubbleColor = if (isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            val alignment = if (isUser) Alignment.End else Alignment.Start
                            val textColor = if (isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            if (!isUser || !message.text.startsWith("System Context:")) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = alignment
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isUser) 12.dp else 0.dp,
                                                    bottomEnd = if (isUser) 0.dp else 12.dp
                                                )
                                            )
                                            .background(bubbleColor)
                                            .padding(12.dp)
                                            .widthIn(max = 280.dp)
                                    ) {
                                        Text(
                                            text = message.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }

                        if (isChatLoading) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "LifeOS AI is compiling...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Prompts Row & Input Field
                Column {
                    // Quick Prompts Scrollable Row
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(quickPrompts) { prompt ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.sendMessage(prompt)
                                },
                                label = {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Input Field Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Ask LifeOS AI a question...") },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                }
                            },
                            enabled = textInput.isNotBlank() && !isChatLoading,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (textInput.isNotBlank() && !isChatLoading) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send Message",
                                tint = if (textInput.isNotBlank() && !isChatLoading) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Unit = {},
    editingDebt: DebtTransactionEntity? = null,
    editingPersonName: String? = null,
    editingPhoneNumber: String? = null,
    onDismiss: () -> Unit,
    onSave: (
        personName: String,
        amount: Double,
        type: DebtType,
        borrowDate: Long,
        notes: String?,
        dueDate: Long?,
        phoneNumber: String?,
        reminderEnabled: Boolean,
        category: String,
        attachmentPath: String?
    ) -> Unit
) {
    var isLent by remember { mutableStateOf(editingDebt == null || editingDebt.type == DebtType.RECEIVABLE) }
    var personName by remember { mutableStateOf(editingPersonName ?: "") }
    var amountStr by remember { mutableStateOf(editingDebt?.amount?.let { String.format(Locale.US, "%.0f", it) } ?: "") }
    
    val context = LocalContext.current
    var borrowDate by remember { mutableStateOf(editingDebt?.borrowDate ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(editingDebt?.notes ?: "") }

    var isAdvancedExpanded by remember { mutableStateOf(false) }

    // Advanced Fields
    var dueDate by remember { mutableStateOf<Long?>(editingDebt?.dueDate) }
    var phoneNumber by remember { mutableStateOf(editingPhoneNumber ?: "") }
    var reminderEnabled by remember { mutableStateOf(editingDebt?.reminderEnabled ?: false) }
    var category by remember { mutableStateOf(editingDebt?.category ?: "Other") }
    var attachmentPath by remember { mutableStateOf(editingDebt?.receiptPath ?: "") }

    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    val categories = listOf("Friend", "Family", "Business", "Other")
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingDebt != null) "Edit Debt Record" else "Record a Debt",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 1. Type
                Text("Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedFilterChip(
                        selected = isLent,
                        onClick = { isLent = true },
                        label = { Text("I Lent (আমি ধার দিয়েছি)", modifier = Modifier.padding(vertical = 4.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    ElevatedFilterChip(
                        selected = !isLent,
                        onClick = { isLent = false },
                        label = { Text("I Borrowed (আমি ধার নিয়েছি)", modifier = Modifier.padding(vertical = 4.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. Person Name
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Amount
                val currencySymbol = when (com.example.utils.FinanceConfig.currentCurrency) {
                    "USD" -> "$"
                    "EUR" -> "€"
                    "INR" -> "₹"
                    "GBP" -> "£"
                    else -> "৳"
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Date (Default to today)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = borrowDate }
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val selected = Calendar.getInstance().apply {
                                        set(y, m, d, 12, 0, 0)
                                    }
                                    borrowDate = selected.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Text(sdf.format(Date(borrowDate)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                // 5. Optional Note
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Advanced Header Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Advanced Options (Optional)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (isAdvancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Toggle Advanced Options"
                    )
                }

                if (isAdvancedExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Due Date
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = dueDate ?: System.currentTimeMillis() }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val selected = Calendar.getInstance().apply {
                                                set(y, m, d, 12, 0, 0)
                                            }
                                            dueDate = selected.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.HourglassEmpty, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Due Date", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = dueDate?.let { sdf.format(Date(it)) } ?: "Set Due Date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (dueDate != null) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Phone Number
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Reminder Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable Reminder", style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { reminderEnabled = it }
                            )
                        }

                        // Category Chips
                        Text("Category", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allCats = remember(customCategories) {
                                listOf("Friend", "Family", "Business", "Other") + customCategories
                            }
                            allCats.forEach { cat ->
                                val selected = !showNewCategoryInput && category == cat
                                FilterChip(
                                    selected = selected,
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
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
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

                        // Attachment Path
                        OutlinedTextField(
                            value = attachmentPath,
                            onValueChange = { attachmentPath = it },
                            label = { Text("Attachment Path/URI") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (personName.isNotBlank() && amt != null && amt > 0) {
                        onSave(
                            personName,
                            amt,
                            if (isLent) DebtType.RECEIVABLE else DebtType.PAYABLE,
                            borrowDate,
                            notes.ifBlank { null },
                            dueDate,
                            phoneNumber.ifBlank { null },
                            reminderEnabled,
                            category,
                            attachmentPath.ifBlank { null }
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipableDebtItem(
    debt: DebtTransactionEntity,
    personName: String,
    paidAmount: Double,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { // Swiped right
                    onMarkPaid()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> { // Swiped left
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    LaunchedEffect(debt) {
        dismissState.reset()
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32) // green
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFC62828) // red
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.CheckCircle
                SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
                else -> Icons.Rounded.Delete
            }
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "Mark Paid"
                SwipeToDismissBoxValue.EndToStart -> "Delete"
                else -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Icon(icon, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(icon, contentDescription = null, tint = Color.White)
                    }
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        val isLent = debt.type == DebtType.RECEIVABLE
        val remaining = debt.amount - paidAmount
        val baseColor = if (isLent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onEdit
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLent) "I Lent" else "I Borrowed",
                        style = MaterialTheme.typography.bodySmall,
                        color = baseColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = com.example.utils.FinanceConfig.formatCurrency(debt.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = baseColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDisplayDate(debt.borrowDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatDisplayDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
        return "Today"
    }
    
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (yesterday.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
        return "Yesterday"
    }
    
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    if (tomorrow.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
        tomorrow.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
        return "Tomorrow"
    }
    
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun DebtDetailsDialog(
    debt: DebtTransactionEntity,
    personName: String,
    paidAmount: Double,
    payments: List<com.example.data.local.entity.DebtPaymentEntity>,
    onDismiss: () -> Unit,
    onAddPaymentClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeletePayment: (com.example.data.local.entity.DebtPaymentEntity) -> Unit
) {
    val remaining = debt.amount - paidAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${debt.description.ifBlank { "Debt Record" }} with $personName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Original Amount:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = com.example.utils.FinanceConfig.formatCurrency(debt.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Paid Amount:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = com.example.utils.FinanceConfig.formatCurrency(paidAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Remaining Amount:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = com.example.utils.FinanceConfig.formatCurrency(remaining),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (debt.type == DebtType.RECEIVABLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                if (!debt.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Note:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = debt.notes, style = MaterialTheme.typography.bodyMedium)
                }

                if (payments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Repayment History:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        payments.forEach { payment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = com.example.utils.FinanceConfig.formatCurrency(payment.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!payment.notes.isNullOrBlank()) {
                                        Text(
                                            text = payment.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { 
                                        onDismiss()
                                        onDeletePayment(payment) 
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete payment",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onDeleteClick()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                if (remaining > 0) {
                    Button(
                        onClick = {
                            onDismiss()
                            onAddPaymentClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Payment")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AddPaymentDialog(
    debt: DebtTransactionEntity,
    personName: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Installment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Register partial payment towards loan to $personName for '${debt.description}'",
                    style = MaterialTheme.typography.bodyMedium
                )
                val currencySymbol = when (com.example.utils.FinanceConfig.currentCurrency) {
                    "USD" -> "$"
                    "EUR" -> "€"
                    "INR" -> "₹"
                    "GBP" -> "£"
                    else -> "৳"
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Installment Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Payment notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull()
                if (amt != null && amt > 0) {
                    onSave(amt, notes.ifBlank { "Partial repayment logged" })
                }
            }) { Text("Record payment") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
