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
        
        val taskDao = try { GlobalContext.get().get<com.example.data.local.dao.TaskDao>() } catch (e: Exception) { null }
        val expenseDao = try { GlobalContext.get().get<com.example.data.local.dao.ExpenseDao>() } catch (e: Exception) { null }
        val noteDao = try { GlobalContext.get().get<com.example.data.local.dao.NoteDao>() } catch (e: Exception) { null }
        val goalDao = try { GlobalContext.get().get<com.example.data.local.dao.GoalDao>() } catch (e: Exception) { null }
        val habitDao = try { GlobalContext.get().get<com.example.data.local.dao.HabitDao>() } catch (e: Exception) { null }

        val activeDebts = debtRepo?.getAllDebts()?.firstOrNull() ?: emptyList()
        val totalReceivables = activeDebts.filter { !it.isSettled && it.type == DebtType.RECEIVABLE }.sumOf { it.remainingAmount }
        val totalPayables = activeDebts.filter { !it.isSettled && it.type == DebtType.PAYABLE }.sumOf { it.remainingAmount }
        
        val name = prefs?.getUserName() ?: "Rishad"
        val currency = prefs?.getCurrencyCode() ?: "BDT"
        val budget = prefs?.getMonthlyBudget() ?: 15000.0
        val dailyBudget = prefs?.getDailyBudget() ?: 500.0

        val actualPrompt = if (query.contains("\nUser's prompt: ")) {
            query.substringAfter("\nUser's prompt: ")
        } else {
            query
        }
        val q = actualPrompt.lowercase().trim()

        return when {
            q.contains("hello") || q.contains("hi") || q.contains("hey") || q.contains("greetings") || q.contains("who are you") || q.contains("what is your name") -> {
                "Hello, $name! I am Jarvis, your intelligent Local AI Assistant. 🧠\n\n" +
                "I have complete on-device local awareness of your lifestyle profile, including your tasks, financial ledger, goals, notes, habits, and debts. How can I help you today?"
            }
            q.contains("task") || q.contains("todo") || q.contains("to-do") || q.contains("plan") || q.contains("productivity") || q.contains("priorit") -> {
                val tasksList = taskDao?.getAllTasks()?.firstOrNull() ?: emptyList()
                val pending = tasksList.filter { !it.isCompleted }
                val completed = tasksList.filter { it.isCompleted }
                
                val sb = StringBuilder()
                sb.append("### 🎯 Your Task & Productivity Profile\n\n")
                if (tasksList.isEmpty()) {
                    sb.append("You don't have any tasks in your list yet! Type in the Plan tab to organize your day, or ask me to draft one for you.")
                } else {
                    sb.append("You currently have **${pending.size} active** and **${completed.size} completed** tasks:\n\n")
                    pending.take(6).forEachIndexed { index, task ->
                        val priorityStr = when (task.priority) {
                            2 -> "🔴 High"
                            1 -> "🟡 Medium"
                            else -> "🔵 Low"
                        }
                        sb.append("${index + 1}. **${task.title}** ($priorityStr)\n")
                        if (task.description.isNotBlank()) {
                            sb.append("   - *${task.description}*\n")
                        }
                    }
                    if (pending.size > 6) {
                        sb.append("- *And ${pending.size - 6} more active tasks...*\n")
                    }
                    
                    sb.append("\n**💡 Jarvis Focus Advice**:\n")
                    val highPriority = pending.firstOrNull { task -> task.priority == 2 }
                    if (highPriority != null) {
                        sb.append("Set your target on your high priority item: **\"${highPriority.title}\"**! Finish it first using deep-work intervals (25 mins work, 5 mins break) to capture momentum early.")
                    } else if (pending.isNotEmpty()) {
                        sb.append("Focus on **\"${pending.first().title}\"** next. Tackling the oldest active item breaks procrastination patterns.")
                    } else {
                        sb.append("Outstanding work! All tasks are completed. Use this window to set future milestones or review your goals.")
                    }
                }
                sb.toString()
            }
            q.contains("debt") || q.contains("loan") || q.contains("pay") || q.contains("borrow") || q.contains("lent") || q.contains("receivable") || q.contains("payable") -> {
                val sb = StringBuilder()
                sb.append("### 💳 Live Debt & Balance Sheet\n\n")
                if (activeDebts.isEmpty()) {
                    sb.append("Fantastic posture! You have **no active loans, receivables, or payables** on file.")
                } else {
                    sb.append("Your live balance sheet registers:\n")
                    sb.append("- 📈 **Receivables (Lent)**: $currency ${String.format("%.2f", totalReceivables)}\n")
                    sb.append("- 📉 **Payables (Borrowed)**: $currency ${String.format("%.2f", totalPayables)}\n")
                    val net = totalReceivables - totalPayables
                    sb.append("- ⚖️ **Net Outlook**: $currency ${String.format("%.2f", net)} (${if (net >= 0) "Net Surplus" else "Net Deficit"})\n\n")
                    
                    val persons = debtRepo?.getAllPersons()?.firstOrNull() ?: emptyList()
                    val activeDebtsList = activeDebts.filter { !it.isSettled }
                    sb.append("**Active Items**:\n")
                    activeDebtsList.take(5).forEachIndexed { index, debt ->
                        val personName = persons.firstOrNull { it.id == debt.personId }?.name ?: "Someone"
                        val typeStr = if (debt.type == DebtType.RECEIVABLE) "Lent to" else "Borrowed from"
                        sb.append("${index + 1}. **$personName** ($typeStr): $currency ${String.format("%.2f", debt.remainingAmount)}\n")
                    }
                    
                    sb.append("\n**💡 Jarvis Debt Insights**:\n")
                    if (totalPayables > 0) {
                        sb.append("- Use the snowball approach: clear smallest borrowed amounts first to streamline your active creditors list.\n")
                    }
                    if (totalReceivables > 0) {
                        sb.append("- Generate polite reminder templates inside the Debt screen to send quick nudges over WhatsApp or SMS.")
                    }
                }
                sb.toString()
            }
            q.contains("budget") || q.contains("expense") || q.contains("spend") || q.contains("finance") || q.contains("cost") || q.contains("money") || q.contains("outflow") -> {
                val expensesList = expenseDao?.getAllExpenses()?.firstOrNull() ?: emptyList()
                val totalSpent = expensesList.sumOf { it.amount }
                
                val sb = StringBuilder()
                sb.append("### 📊 Live Financial Diagnostics & Budget\n\n")
                sb.append("- **Monthly Target Limit**: $currency ${String.format("%.2f", budget)}\n")
                sb.append("- **Total Tracked Outflow**: $currency ${String.format("%.2f", totalSpent)}\n")
                
                val remaining = budget - totalSpent
                if (budget > 0) {
                    val pct = (totalSpent / budget) * 100
                    sb.append("- **Budget Burn Rate**: **${String.format("%.1f", pct)}%** consumed\n")
                    sb.append("- **Remaining Runway**: $currency ${String.format("%.2f", remaining)}\n\n")
                } else {
                    sb.append("\n")
                }

                if (expensesList.isNotEmpty()) {
                    sb.append("**Recent Cash Outlays**:\n")
                    expensesList.take(5).forEachIndexed { index, exp ->
                        sb.append("${index + 1}. **${exp.category}**: $currency ${String.format("%.2f", exp.amount)} *(Note: ${exp.note})*\n")
                    }
                } else {
                    sb.append("No active transaction entries logged on-device yet.")
                }

                sb.append("\n**💡 Jarvis Fiscal Advice**:\n")
                if (budget > 0 && remaining < 0) {
                    sb.append("⚠️ **Attention $name**: You are currently over budget by **$currency ${String.format("%.2f", -remaining)}**! Implement an immediate discretionary spend freeze. Postpone luxury or dining purchases for 72 hours.")
                } else if (budget > 0 && remaining < (budget * 0.15)) {
                    sb.append("⚠️ You are close to your threshold! Limit shopping and restrict outlays to fixed necessities only.")
                } else {
                    sb.append("Spending flow looks stable and healthy. Keep tracking every transaction to keep your runway clear!")
                }
                sb.toString()
            }
            q.contains("note") || q.contains("memo") || q.contains("idea") -> {
                val notesList = noteDao?.getAllNotes()?.firstOrNull() ?: emptyList()
                val sb = StringBuilder()
                sb.append("### 📝 Live Notes & Brainstorming Vault\n\n")
                if (notesList.isEmpty()) {
                    sb.append("Your memo vault is empty! Tap the Notes card to scribble down creative ideas, tasks, or meeting drafts.")
                } else {
                    sb.append("You have **${notesList.size} notes** stored safely on-device:\n\n")
                    notesList.take(6).forEachIndexed { index, note ->
                        sb.append("${index + 1}. **${note.title}**\n")
                        if (note.content.isNotBlank()) {
                            val snippet = if (note.content.length > 80) note.content.take(80) + "..." else note.content
                            sb.append("   - *\"$snippet\"*\n")
                        }
                    }
                    if (notesList.size > 6) {
                        sb.append("- *And ${notesList.size - 6} more notes...*\n")
                    }
                    sb.append("\n*Need to write down something fast? Go to the Notes widget to start a new scratchpad.*")
                }
                sb.toString()
            }
            q.contains("goal") || q.contains("target") || q.contains("ambition") || q.contains("milestone") -> {
                val goalsList = goalDao?.getAllGoals()?.firstOrNull() ?: emptyList()
                val sb = StringBuilder()
                sb.append("### 🏆 Your Active Milestones & Goals\n\n")
                if (goalsList.isEmpty()) {
                    sb.append("No goals registered yet. Defining clear, metric-driven targets in the dashboard keeps you focused and aligned!")
                } else {
                    sb.append("Here is your progressive goal summary:\n\n")
                    goalsList.take(5).forEachIndexed { index, goal ->
                        val status = if (goal.isCompleted) "✅ Completed" else "⏳ Progressive"
                        val progressBar = "[" + "#".repeat((goal.progress * 10).toInt()).padEnd(10, '-') + "]"
                        sb.append("${index + 1}. **${goal.title}** - $status\n")
                        sb.append("   - Progress: $progressBar **${(goal.progress * 100).toInt()}%**\n")
                        if (goal.description.isNotBlank()) {
                            sb.append("   - *${goal.description}*\n")
                        }
                    }
                }
                sb.toString()
            }
            q.contains("habit") || q.contains("streak") || q.contains("routine") -> {
                val habitsList = habitDao?.getAllHabits()?.firstOrNull() ?: emptyList()
                val sb = StringBuilder()
                sb.append("### ⚡ Habit Tracking & Routines\n\n")
                if (habitsList.isEmpty()) {
                    sb.append("No habits tracking on-device yet! Building atomic, consistent routines is the foundation of peak productivity.")
                } else {
                    sb.append("Your active habits registry:\n\n")
                    habitsList.take(5).forEachIndexed { index, habit ->
                        sb.append("${index + 1}. **${habit.name}** - Streak: **${habit.currentStreak} days**\n")
                        sb.append("   - Routine: ${habit.frequency}\n")
                    }
                }
                sb.toString()
            }
            else -> {
                val tasksList = taskDao?.getAllTasks()?.firstOrNull() ?: emptyList()
                val pendingTasks = tasksList.count { !it.isCompleted }
                val notesList = noteDao?.getAllNotes()?.firstOrNull() ?: emptyList()
                
                "### 🧠 Jarvis AI Assistant\n\n" +
                "Greetings, **$name**! I have examined your active local profile. Here is a high-level briefing:\n" +
                "- 🎯 You have **$pendingTasks active tasks** waiting in your Plan list.\n" +
                "- 💳 Net debt posture shows **$currency ${String.format("%.2f", totalReceivables)}** receivables versus **$currency ${String.format("%.2f", totalPayables)}** payables.\n" +
                "- 📝 You have **${notesList.size} active notes** recorded in your vault.\n\n" +
                "How should we proceed? You can type a question, or tap any category tag ('Work', 'Personal', 'Ideas') below to categorize our chat interactions. Try asking:\n" +
                "- *\"What are my pending tasks?\"*\n" +
                "- *\"Analyze my financial balance and budget\"*\n" +
                "- *\"Show my notes and scratchpads\"*\n" +
                "- *\"Check my debt sheet\"*"
            }
        }
    }
}
