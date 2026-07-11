package com.example.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.DebtTransactionEntity
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.PersonEntity
import com.example.data.repository.DebtRepository
import com.example.core.ai.AIEngine
import com.example.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.feature.dashboard.presentation.SnackbarEvent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat

class DebtViewModel(
    private val debtRepository: DebtRepository,
    private val aiEngine: AIEngine,
    private val financePreferences: com.example.utils.FinancePreferences
) : ViewModel() {

    private val _snackbarEvent = MutableSharedFlow<SnackbarEvent>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()
    
    private val _customDebtCategories = MutableStateFlow<List<String>>(financePreferences.getCustomDebtCategories())
    val customDebtCategories: StateFlow<List<String>> = _customDebtCategories.asStateFlow()

    fun addCustomDebtCategory(category: String) {
        financePreferences.addCustomDebtCategory(category)
        _customDebtCategories.value = financePreferences.getCustomDebtCategories()
    }

    fun showSnackbar(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        viewModelScope.launch {
            _snackbarEvent.emit(SnackbarEvent(message, actionLabel, onAction))
        }
    }

    private val _aiInsight = MutableStateFlow("Welcome to Debt Management! Track who owes you money and who you owe. Tap the Refresh icon (🔄) above to generate a dynamic, smart analysis of your current portfolio.")
    val aiInsight: StateFlow<String> = _aiInsight

    val persons: StateFlow<List<PersonEntity>> = debtRepository.getAllPersons().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val debts: StateFlow<List<DebtTransactionEntity>> = debtRepository.getAllDebts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val payments: StateFlow<List<DebtPaymentEntity>> = debtRepository.getAllPayments().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Calculate dynamic totals & statistics
    val totalReceivables: StateFlow<Double> = debts.combine(payments) { debtsList, paymentsList ->
        debtsList.filter { it.type == DebtType.RECEIVABLE && !it.isSettled }.sumOf { debt ->
            val paid = paymentsList.filter { it.debtId == debt.id }.sumOf { it.amount }
            val remaining = debt.amount - paid
            if (remaining > 0) remaining else 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayables: StateFlow<Double> = debts.combine(payments) { debtsList, paymentsList ->
        debtsList.filter { it.type == DebtType.PAYABLE && !it.isSettled }.sumOf { debt ->
            val paid = paymentsList.filter { it.debtId == debt.id }.sumOf { it.amount }
            val remaining = debt.amount - paid
            if (remaining > 0) remaining else 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val overdueCount: StateFlow<Int> = debts.combine(payments) { debtsList, paymentsList ->
        val now = System.currentTimeMillis()
        debtsList.filter { !it.isSettled }.count { debt ->
            val paid = paymentsList.filter { it.debtId == debt.id }.sumOf { it.amount }
            val remaining = debt.amount - paid
            remaining > 0 && debt.dueDate < now
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dueTodayCount: StateFlow<Int> = debts.combine(payments) { debtsList, paymentsList ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        debtsList.filter { !it.isSettled }.count { debt ->
            val paid = paymentsList.filter { it.debtId == debt.id }.sumOf { it.amount }
            val remaining = debt.amount - paid
            remaining > 0 && debt.dueDate in todayStart..todayEnd
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isRefreshingInsight = MutableStateFlow(false)
    val isRefreshingInsight: StateFlow<Boolean> = _isRefreshingInsight.asStateFlow()

    private val _isThinkingMode = MutableStateFlow(false)
    val isThinkingMode: StateFlow<Boolean> = _isThinkingMode.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<com.example.core.ai.ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<com.example.core.ai.ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun toggleThinkingMode() {
        _isThinkingMode.value = !_isThinkingMode.value
        refreshDebtAiInsight()
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val currentMessages = _chatMessages.value.toMutableList()
        val userMessage = com.example.core.ai.ChatMessage(role = "user", text = userText)
        currentMessages.add(userMessage)
        _chatMessages.value = currentMessages

        viewModelScope.launch {
            _isChatLoading.value = true
            val contextPrefix = buildContextPrefix()
            val promptMessageList = mutableListOf<com.example.core.ai.ChatMessage>()
            
            promptMessageList.add(com.example.core.ai.ChatMessage(
                role = "user",
                text = "System Context: You are LifeOS AI, a highly intelligent financial advisor. Here is the user's current debt, loan, and payment data:\n$contextPrefix\nUse this data to answer the user's queries concisely, smartly, and professionally. Support both English and Bengali as the user prefers."
            ))
            promptMessageList.add(com.example.core.ai.ChatMessage(
                role = "model",
                text = "Understood. I have fully reviewed the debt and repayment data. I am ready to assist the user with expert insights, copyable SMS/WhatsApp reminders, or payoff strategies."
            ))

            currentMessages.forEach { msg ->
                promptMessageList.add(msg)
            }

            val result = aiEngine.askJarvisChat(promptMessageList, useThinking = _isThinkingMode.value)
            if (result is Result.Success) {
                currentMessages.add(com.example.core.ai.ChatMessage(role = "model", text = result.data))
            } else {
                val errorMsg = (result as? Result.Error)?.message ?: "An error occurred."
                currentMessages.add(com.example.core.ai.ChatMessage(role = "model", text = "I'm having trouble connecting to my AI core: $errorMsg. Please check your API Key in Settings."))
            }
            _chatMessages.value = currentMessages
            _isChatLoading.value = false
        }
    }

    fun buildContextPrefix(): String {
        return com.example.utils.DataSummarizer.summarizeDebts(debts.value, payments.value, persons.value)
    }

    init {
        // Manual refresh only to save API quota. Friendly welcome message is set as initial value.
    }

    fun refreshDebtAiInsight() {
        viewModelScope.launch {
            _isRefreshingInsight.value = true
            generateDebtAiInsight(debts.value, payments.value, persons.value)
            _isRefreshingInsight.value = false
        }
    }

    private suspend fun generateDebtAiInsight(
        debtsList: List<DebtTransactionEntity>,
        paymentsList: List<DebtPaymentEntity>,
        personsList: List<PersonEntity>
    ) {
        if (debtsList.isEmpty()) {
            _aiInsight.value = "Welcome to Debt Management! Track who owes you money and who you owe, with dynamic partial repayments and trust scores."
            return
        }

        val debtSummary = com.example.utils.DataSummarizer.summarizeDebts(debtsList, paymentsList, personsList)

        val prompt = if (_isThinkingMode.value) {
            """
                As LifeOS AI, the smart strategic financial advisor, perform a Deep Thinking analysis of the user's debt/loan profile:
                
                $debtSummary
                
                Generate a highly detailed, professional, and sophisticated strategic debt payoff plan, alert breakdown, or payment recovery guide. Give concrete strategies for cash flow preservation and how to handle overdue accounts. Support both English and Bengali.
            """.trimIndent()
        } else {
            """
                As LifeOS AI, the smart financial assistant, analyze the user's debt/loan profile:
                
                $debtSummary
                
                Based on these stats, generate 2 sentences of hyper-focused financial advice, alerts, or insights about outstanding loans, trust patterns, or payment recovery. Keep it premium, actionable, and direct.
            """.trimIndent()
        }

        val result = if (_isThinkingMode.value) {
            aiEngine.askJarvisChat(
                listOf(com.example.core.ai.ChatMessage(role = "user", text = prompt)),
                useThinking = true
            )
        } else {
            aiEngine.askJarvis(prompt)
        }

        if (result is Result.Success) {
            _aiInsight.value = result.data
        } else {
            _aiInsight.value = if (_isThinkingMode.value) {
                "Unable to perform deep strategy calculation. Keep a close eye on upcoming deadlines. Prioritize recovering high-value overdue receivables first to maintain sound cash flow."
            } else {
                "Keep a close eye on upcoming deadlines. Prioritize recovering high-value overdue receivables first to maintain sound cash flow."
            }
        }
    }

    fun addPerson(name: String, contactInfo: String?) {
        viewModelScope.launch {
            debtRepository.insertPerson(PersonEntity(name = name, contactInfo = contactInfo))
        }
    }

    fun addDebt(
        personId: Int,
        amount: Double,
        type: DebtType,
        description: String,
        category: String,
        dueDate: Long,
        interestRate: Double = 0.0,
        borrowDate: Long = System.currentTimeMillis(),
        notes: String? = null,
        paymentMethod: String = "Cash",
        receiptPath: String? = null,
        reminderEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            debtRepository.insertDebt(
                DebtTransactionEntity(
                    personId = personId,
                    amount = amount,
                    remainingAmount = amount,
                    type = type,
                    description = description,
                    category = category,
                    dueDate = dueDate,
                    interestRate = interestRate,
                    borrowDate = borrowDate,
                    notes = notes,
                    paymentMethod = paymentMethod,
                    receiptPath = receiptPath,
                    reminderEnabled = reminderEnabled
                )
            )
        }
    }

    fun quickAddDebt(
        personName: String,
        amount: Double,
        type: DebtType,
        borrowDate: Long = System.currentTimeMillis(),
        notes: String? = null,
        dueDate: Long? = null,
        phoneNumber: String? = null,
        reminderEnabled: Boolean = false,
        category: String = "Other",
        attachmentPath: String? = null
    ) {
        viewModelScope.launch {
            val trimmedName = personName.trim()
            val existingPerson = persons.value.find { it.name.trim().equals(trimmedName, ignoreCase = true) }
            val personId = if (existingPerson != null) {
                existingPerson.id
            } else {
                val newPersonId = debtRepository.insertPerson(
                    PersonEntity(
                        name = trimmedName,
                        contactInfo = phoneNumber?.trim()?.ifBlank { null }
                    )
                )
                newPersonId.toInt()
            }

            val finalDueDate = dueDate ?: (borrowDate + 30 * 86400000L)
            debtRepository.insertDebt(
                DebtTransactionEntity(
                    personId = personId,
                    amount = amount,
                    remainingAmount = amount,
                    type = type,
                    description = notes?.trim()?.ifBlank { "Loan" } ?: "Loan",
                    category = category,
                    dueDate = finalDueDate,
                    borrowDate = borrowDate,
                    notes = notes?.trim()?.ifBlank { null },
                    receiptPath = attachmentPath,
                    reminderEnabled = reminderEnabled
                )
            )
        }
    }

    fun quickUpdateDebt(
        debtId: Int,
        personName: String,
        amount: Double,
        type: DebtType,
        borrowDate: Long,
        notes: String?,
        dueDate: Long? = null,
        phoneNumber: String? = null,
        reminderEnabled: Boolean = false,
        category: String = "Other",
        attachmentPath: String? = null
    ) {
        viewModelScope.launch {
            val trimmedName = personName.trim()
            val existingPerson = persons.value.find { it.name.trim().equals(trimmedName, ignoreCase = true) }
            val personId = if (existingPerson != null) {
                existingPerson.id
            } else {
                val newPersonId = debtRepository.insertPerson(
                    PersonEntity(
                        name = trimmedName,
                        contactInfo = phoneNumber?.trim()?.ifBlank { null }
                    )
                )
                newPersonId.toInt()
            }

            val finalDueDate = dueDate ?: (borrowDate + 30 * 86400000L)
            val oldDebt = debts.value.find { it.id == debtId }
            if (oldDebt != null) {
                val totalPaid = oldDebt.amount - oldDebt.remainingAmount
                val newRemaining = (amount - totalPaid).coerceAtLeast(0.0)
                val isSettled = newRemaining <= 0.0

                debtRepository.insertDebt(
                    DebtTransactionEntity(
                        id = debtId,
                        personId = personId,
                        amount = amount,
                        remainingAmount = newRemaining,
                        type = type,
                        description = notes?.trim()?.ifBlank { "Loan" } ?: "Loan",
                        category = category,
                        dueDate = finalDueDate,
                        isSettled = isSettled,
                        interestRate = oldDebt.interestRate,
                        borrowDate = borrowDate,
                        notes = notes?.trim()?.ifBlank { null },
                        paymentMethod = oldDebt.paymentMethod,
                        receiptPath = attachmentPath,
                        reminderEnabled = reminderEnabled,
                        createdAt = oldDebt.createdAt
                    )
                )
            }
        }
    }

    fun addPayment(debtId: Int, amount: Double, notes: String?) {
        viewModelScope.launch {
            debtRepository.insertPayment(
                DebtPaymentEntity(
                    debtId = debtId,
                    amount = amount,
                    notes = notes
                )
            )
            // Automatically update settled status if remaining is 0 or less
            val debt = debts.value.find { it.id == debtId }
            if (debt != null) {
                val paid = payments.value.filter { it.debtId == debtId }.sumOf { it.amount } + amount
                if (paid >= debt.amount) {
                    debtRepository.updateDebtStatus(debtId, true)
                }
            }
        }
    }

    fun markPaid(debtId: Int) {
        viewModelScope.launch {
            val debt = debts.value.find { it.id == debtId }
            if (debt != null) {
                val paid = payments.value.filter { it.debtId == debtId }.sumOf { it.amount }
                val remaining = debt.amount - paid
                if (remaining > 0) {
                    debtRepository.insertPayment(
                        DebtPaymentEntity(
                            debtId = debtId,
                            amount = remaining,
                            notes = "Quick Settled"
                        )
                    )
                }
                debtRepository.updateDebtStatus(debtId, true)
            }
        }
    }

    private var lastDeletedDebt: DebtTransactionEntity? = null
    private var lastDeletedDebtPayments: List<DebtPaymentEntity>? = null

    private var lastDeletedPerson: PersonEntity? = null
    private var lastDeletedPersonDebts: List<DebtTransactionEntity>? = null
    private var lastDeletedPersonPayments: List<DebtPaymentEntity>? = null

    private var lastDeletedPayment: DebtPaymentEntity? = null

    fun deleteDebt(debt: DebtTransactionEntity) {
        viewModelScope.launch {
            val relatedPayments = debtRepository.getPaymentsForDebtSync(debt.id)
            debtRepository.deleteDebt(debt)
            lastDeletedDebt = debt
            lastDeletedDebtPayments = relatedPayments
            showSnackbar("Debt transaction deleted", "Undo") {
                viewModelScope.launch {
                    lastDeletedDebt?.let { savedDebt ->
                        debtRepository.insertDebt(savedDebt)
                        lastDeletedDebtPayments?.forEach { payment ->
                            debtRepository.insertPayment(payment)
                        }
                    }
                    lastDeletedDebt = null
                    lastDeletedDebtPayments = null
                }
            }
        }
    }

    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch {
            val relatedDebts = debtRepository.getDebtsByPersonIdSync(person.id)
            val relatedPayments = relatedDebts.flatMap { debtRepository.getPaymentsForDebtSync(it.id) }
            debtRepository.deletePerson(person)
            lastDeletedPerson = person
            lastDeletedPersonDebts = relatedDebts
            lastDeletedPersonPayments = relatedPayments
            showSnackbar("Trust profile deleted", "Undo") {
                viewModelScope.launch {
                    lastDeletedPerson?.let { savedPerson ->
                        debtRepository.insertPerson(savedPerson)
                        lastDeletedPersonDebts?.forEach { debt ->
                            debtRepository.insertDebt(debt)
                        }
                        lastDeletedPersonPayments?.forEach { payment ->
                            debtRepository.insertPayment(payment)
                        }
                    }
                    lastDeletedPerson = null
                    lastDeletedPersonDebts = null
                    lastDeletedPersonPayments = null
                }
            }
        }
    }

    fun deletePayment(payment: DebtPaymentEntity) {
        viewModelScope.launch {
            debtRepository.deletePayment(payment)
            lastDeletedPayment = payment
            showSnackbar("Repayment record deleted", "Undo") {
                viewModelScope.launch {
                    lastDeletedPayment?.let { savedPayment ->
                        debtRepository.insertPayment(savedPayment)
                    }
                    lastDeletedPayment = null
                }
            }
        }
    }
}
