package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.utils.FinanceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_summary)

            // Create an Intent to launch MainActivity
            val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).let { intent ->
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            }

            views.setOnClickPendingIntent(R.id.widget_tasks_text, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_budget_text, pendingIntent)

            // Query data asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                val database = AppDatabase.getDatabase(context)
                val taskDao = database.taskDao()
                val expenseDao = database.expenseDao()

                val tasks = taskDao.getAllTasks().firstOrNull() ?: emptyList()
                val pendingTasksCount = tasks.count { !it.isCompleted }
                
                val expenses = expenseDao.getAllExpenses().firstOrNull() ?: emptyList()
                val totalExpenses = expenses.sumOf { it.amount }

                withContext(Dispatchers.Main) {
                    views.setTextViewText(
                        R.id.widget_tasks_text,
                        "$pendingTasksCount pending"
                    )
                    views.setTextViewText(
                        R.id.widget_budget_text,
                        FinanceConfig.formatCurrency(totalExpenses)
                    )

                    // Instruct the widget manager to update the widget
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
            
            // Initial loading state update
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
