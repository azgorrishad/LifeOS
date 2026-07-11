package com.example.utils

import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.DebtTransactionEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.PersonEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataSummarizer {
    
    fun summarizeTasks(tasks: List<TaskEntity>): String {
        if (tasks.isEmpty()) return "No tasks registered."
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val pending = total - completed
        val pendingSample = tasks.filter { !it.isCompleted }
            .sortedByDescending { it.priority }
            .take(5)
            .joinToString(", ") { "${it.title} (P${it.priority})" }
            
        return "Tasks Summary: Total=$total, Done=$completed, Pending=$pending. Top pending tasks: $pendingSample"
    }

    fun summarizeExpenses(expenses: List<ExpenseEntity>): String {
        if (expenses.isEmpty()) return "No recent expenses registered."
        val totalAmount = expenses.sumOf { it.amount }
        val count = expenses.size
        val categorySums = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .joinToString(", ") { "${it.first}: ${FinanceConfig.formatCurrency(it.second)}" }
            
        return "Expenses Summary: Total Spent=${FinanceConfig.formatCurrency(totalAmount)} across $count transactions. Top categories: $categorySums"
    }

    fun summarizeDebts(
        debts: List<DebtTransactionEntity>,
        payments: List<DebtPaymentEntity>,
        persons: List<PersonEntity>
    ): String {
        if (debts.isEmpty()) return "No active debt or loan profiles."
        
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        
        // Receivables (User lent money to others)
        val receivables = debts.filter { it.type == com.example.data.local.entity.DebtType.RECEIVABLE }
        val totalLent = receivables.sumOf { it.amount }
        val totalLentRecovered = receivables.sumOf { debt ->
            payments.filter { it.debtId == debt.id }.sumOf { it.amount }
        }
        val remainingLent = totalLent - totalLentRecovered
        
        // Payables (User borrowed money from others)
        val payables = debts.filter { it.type == com.example.data.local.entity.DebtType.PAYABLE }
        val totalBorrowed = payables.sumOf { it.amount }
        val totalBorrowedRepaid = payables.sumOf { debt ->
            payments.filter { it.debtId == debt.id }.sumOf { it.amount }
        }
        val remainingBorrowed = totalBorrowed - totalBorrowedRepaid

        // Overdue & Upcoming loan details
        val overdueItems = debts.filter { !it.isSettled && it.dueDate < now }
        val upcomingItems = debts.filter { !it.isSettled && it.dueDate >= now && it.dueDate <= now + 86400000L * 7 }
        
        val overdueSummary = if (overdueItems.isEmpty()) "None" else overdueItems.joinToString("; ") { debt ->
            val name = persons.find { it.id == debt.personId }?.name ?: "Unknown"
            val overDueDate = dateFormat.format(Date(debt.dueDate))
            if (debt.type == com.example.data.local.entity.DebtType.RECEIVABLE) {
                "$name owes you ${FinanceConfig.formatCurrency(debt.remainingAmount)} (Overdue since $overDueDate)"
            } else {
                "You owe $name ${FinanceConfig.formatCurrency(debt.remainingAmount)} (Overdue since $overDueDate)"
            }
        }

        val upcomingSummary = if (upcomingItems.isEmpty()) "None" else upcomingItems.joinToString("; ") { debt ->
            val name = persons.find { it.id == debt.personId }?.name ?: "Unknown"
            val due = dateFormat.format(Date(debt.dueDate))
            if (debt.type == com.example.data.local.entity.DebtType.RECEIVABLE) {
                "$name will pay you ${FinanceConfig.formatCurrency(debt.remainingAmount)} (Due: $due)"
            } else {
                "You must pay $name ${FinanceConfig.formatCurrency(debt.remainingAmount)} (Due: $due)"
            }
        }

        return """
            Debt & Loans Portfolio Summary:
            - Money Lent (Receivables): Total Lent=${FinanceConfig.formatCurrency(totalLent)}, Recovered=${FinanceConfig.formatCurrency(totalLentRecovered)}, Outstanding=${FinanceConfig.formatCurrency(remainingLent)}
            - Money Borrowed (Payables): Total Borrowed=${FinanceConfig.formatCurrency(totalBorrowed)}, Repaid=${FinanceConfig.formatCurrency(totalBorrowedRepaid)}, Outstanding=${FinanceConfig.formatCurrency(remainingBorrowed)}
            - Overdue Payments: $overdueSummary
            - Upcoming Deadlines (Next 7 days): $upcomingSummary
        """.trimIndent()
    }
}
