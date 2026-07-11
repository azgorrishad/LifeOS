package com.example.core.ai

import com.example.data.repository.DebtRepository
import com.example.utils.FinancePreferences
import com.example.data.local.entity.DebtType
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.flow.firstOrNull

object LocalAIHeuristicsEngine {
    // Generate highly intelligent, context-aware financial and productivity insights locally
    suspend fun generateLifeInsightsFallback(tasksStr: String, expensesStr: String): String {
        val debtRepo = try { GlobalContext.get().get<DebtRepository>() } catch (e: Exception) { null }
        val prefs = try { GlobalContext.get().get<FinancePreferences>() } catch (e: Exception) { null }
        
        val activeDebts = debtRepo?.getAllDebts()?.firstOrNull() ?: emptyList()
        val totalReceivables = activeDebts.filter { !it.isSettled && it.type == DebtType.RECEIVABLE }.sumOf { it.remainingAmount }
        val totalPayables = activeDebts.filter { !it.isSettled && it.type == DebtType.PAYABLE }.sumOf { it.remainingAmount }
        
        val name = prefs?.getUserName() ?: "Rishad"
        val currency = prefs?.getCurrencyCode() ?: "BDT"
        val budget = prefs?.getMonthlyBudget() ?: 15000.0

        val sb = StringBuilder()
        sb.append("### 🧠 LifeOS Insights\n\n")

        // 1. Debt Analytics & Insights
        sb.append("#### 💳 Strategic Debt & Receivable Insights\n")
        if (totalReceivables > 0 || totalPayables > 0) {
            sb.append("- **Debt Registry**: You have **$currency ${String.format("%.2f", totalReceivables)}** in receivables (lent) and owe **$currency ${String.format("%.2f", totalPayables)}** (borrowed).\n")
            val balanceDiff = totalReceivables - totalPayables
            if (balanceDiff > 0) {
                sb.append("- **Net Capital Outlook**: Your net balance is improving! You are currently a net creditor by **$currency ${String.format("%.2f", balanceDiff)}**.\n")
            } else if (balanceDiff < 0) {
                sb.append("- **Action Plan**: You are a net debtor by **$currency ${String.format("%.2f", -balanceDiff)}**. Prioritize clear, small-balance payables to streamline your commitments.\n")
            }
            
            // Check soon due debts
            val soonOrOverdue = activeDebts.filter { !it.isSettled && it.dueDate > 0 }
            if (soonOrOverdue.isNotEmpty()) {
                val nextDebt = soonOrOverdue.minByOrNull { it.dueDate }
                if (nextDebt != null) {
                    val personId = nextDebt.personId
                    val personList = debtRepo?.getAllPersons()?.firstOrNull() ?: emptyList()
                    val personName = personList.firstOrNull { it.id == personId }?.name ?: "someone"
                    sb.append("- **⚠️ Repayment Alert**: **$personName's** debt of **$currency ${String.format("%.2f", nextDebt.remainingAmount)}** requires attention. Head to the Debt module to check terms or draft a friendly nudge.\n")
                }
            }
        } else {
            sb.append("- **Clean Sheet**: You currently have zero outstanding receivables or payables. Excellent debt-free posture!\n")
        }

        // 2. Budget Analytics
        sb.append("\n#### 📊 Budget & Expense Diagnostics\n")
        val expLines = expensesStr.split("\n").filter { it.isNotBlank() && !it.contains("No expenses") }
        if (expLines.isNotEmpty()) {
            sb.append("- **Activity Tracker**: Analyzed ${expLines.size} recent transactions.\n")
            var parsedSum = 0.0
            expLines.forEach { line ->
                val match = Regex("\\d+(\\.\\d+)?").find(line)
                if (match != null) {
                    parsedSum += match.value.toDoubleOrNull() ?: 0.0
                }
            }
            if (parsedSum > 0) {
                sb.append("- **Recent Outflow**: Estimated expenses at **$currency ${String.format("%.2f", parsedSum)}**.\n")
                if (budget > 0) {
                    val pct = (parsedSum / budget) * 100
                    sb.append("- **Monthly Utilization**: You have consumed **${String.format("%.1f", pct)}%** of your **$currency ${String.format("%.2f", budget)}** limit.\n")
                    if (pct > 80) {
                        sb.append("- **Discipline Alert**: You are near your monthly budget threshold. Freeze any non-essential luxury/recreation spending.\n")
                    } else {
                        sb.append("- **Health Check**: Spending flow is safe and well within limits. Good work!\n")
                    }
                }
            }
        } else {
            sb.append("- **Spending**: No expenses logged today. High-saving rate is maintained.\n")
        }

        // 3. Task Productivity
        sb.append("\n#### 🎯 Productivity & Action Focus\n")
        val taskLines = tasksStr.split("\n").filter { it.isNotBlank() && !it.contains("No tasks") }
        if (taskLines.isNotEmpty()) {
            val completed = taskLines.filter { it.contains("[x]") || it.contains("Completed") || it.contains("Done") }.size
            val pending = taskLines.size - completed
            sb.append("- **Todo Metrics**: You have **$pending active tasks** and **$completed completed** milestones.\n")
            if (pending > 0) {
                sb.append("- **Action Recommendation**: Eat the frog! Target your highest-priority pending item first. Break it down into 15-minute intervals to avoid procrastination.\n")
            } else {
                sb.append("- **Peak Performance**: All tasks completed. Use this surplus time for strategic planning or physical rest.\n")
            }
        } else {
            sb.append("- **Todo Status**: Task list is clear. Add an item in the Plan module to activate your momentum.\n")
        }
        
        return sb.toString()
    }

    // fallback for conversational chatbot interface
    suspend fun generateChatFallback(query: String): String {
        val debtRepo = try { GlobalContext.get().get<DebtRepository>() } catch (e: Exception) { null }
        val prefs = try { GlobalContext.get().get<FinancePreferences>() } catch (e: Exception) { null }
        
        val activeDebts = debtRepo?.getAllDebts()?.firstOrNull() ?: emptyList()
        val totalReceivables = activeDebts.filter { !it.isSettled && it.type == DebtType.RECEIVABLE }.sumOf { it.remainingAmount }
        val totalPayables = activeDebts.filter { !it.isSettled && it.type == DebtType.PAYABLE }.sumOf { it.remainingAmount }
        
        val currency = prefs?.getCurrencyCode() ?: "BDT"
        val q = query.lowercase()

        return when {
            q.contains("hello") || q.contains("hi") || q.contains("hey") || q.contains("greetings") -> {
                "Hello! How can I assist you today? I can help you analyze your finances, manage your debts, or guide your productivity."
            }
            q.contains("debt") || q.contains("loan") || q.contains("pay") || q.contains("borrow") || q.contains("lent") -> {
                val sb = StringBuilder()
                sb.append("### 💳 Debt Portfolio Analysis\n\n")
                if (activeDebts.isEmpty()) {
                    sb.append("You currently do not have any active loans or debts in your portfolio. Your financial sheet is clean!")
                } else {
                    sb.append("Here is your active debt summary:\n")
                    sb.append("- **Total Receivables (Lent)**: $currency ${String.format("%.2f", totalReceivables)}\n")
                    sb.append("- **Total Payables (Borrowed)**: $currency ${String.format("%.2f", totalPayables)}\n")
                    val net = totalReceivables - totalPayables
                    sb.append("- **Net Balance**: $currency ${String.format("%.2f", net)} (${if (net >= 0) "Surplus" else "Deficit"})\n\n")
                    
                    sb.append("**Strategic Repayment Recommendations**:\n")
                    if (totalPayables > 0) {
                        sb.append("1. **Debt Avalanche/Snowball**: We recommend compiling all borrow entries. Contact the highest/smallest creditor first to discuss or settle installments.\n")
                    }
                    if (totalReceivables > 0) {
                        sb.append("2. **Polite Nudges**: Use the Debt Screen to generate customizable, polite reminders (SMS or WhatsApp templates) to encourage quick returns on lent funds.\n")
                    }
                }
                sb.toString()
            }
            q.contains("budget") || q.contains("expense") || q.contains("spend") || q.contains("finance") -> {
                val budget = prefs?.getMonthlyBudget() ?: 0.0
                val daily = prefs?.getDailyBudget() ?: 0.0
                "### 📊 Budget Diagnostics\n\nYour monthly budget is currently set to **$currency ${String.format("%.2f", budget)}** (Daily allowance: **$currency ${String.format("%.2f", daily)}**).\n\n" +
                "To optimize your finances:\n" +
                "- **Track micro-transactions**: Ensure every cup of tea or rickshaw fare is registered under Expenses immediately.\n" +
                "- **Postpone non-essential purchases**: Delay any lifestyle spending by 48 hours to check if it's a need or a temporary impulse."
            }
            q.contains("task") || q.contains("priorit") || q.contains("productiv") || q.contains("todo") -> {
                "### 🎯 Productivity Strategy\n\nTo maximize focus and accomplishment:\n" +
                "1. **Eat the Frog**: Complete the hardest, most vital task before checking notifications.\n" +
                "2. **Time-Blocking**: Allocate fixed 15-to-30 minute segments for dedicated work followed by a 5-minute break.\n" +
                "3. **Clear Distractions**: Place your device face down and clear physical workspace clutter."
            }
            else -> {
                "### 🧠 Assistant\n\n" +
                "I can analyze your local portfolio, budget parameters, and task milestones.\n\n" +
                "**Try asking me about:**\n" +
                "- *\"Analyze my debt portfolio\"*\n" +
                "- *\"Give me budget optimization tips\"*\n" +
                "- *\"Show my productivity strategy\"*"
            }
        }
    }
}
