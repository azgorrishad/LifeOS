package com.example.core.di

import com.example.data.local.AppDatabase
import com.example.core.ai.AIEngine
import com.example.core.ai.AIEngineImpl
import com.example.data.remote.GeminiRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

import org.koin.androidx.viewmodel.dsl.viewModel

val coreDiModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().taskDao() }
    single { get<AppDatabase>().expenseDao() }
    single { get<AppDatabase>().eventDao() }
    single { get<AppDatabase>().noteDao() }
    single { get<AppDatabase>().projectDao() }
    single { get<AppDatabase>().habitDao() }
    single { get<AppDatabase>().goalDao() }
    single { get<AppDatabase>().incomeDao() }
    single { get<AppDatabase>().journalDao() }
    single { get<AppDatabase>().personDao() }
    single { get<AppDatabase>().debtDao() }
    
    single<com.example.feature.tasks.domain.repository.TaskRepository> { com.example.feature.tasks.data.repository.TaskRepositoryImpl(get()) }
    single<com.example.feature.finance.domain.repository.ExpenseRepository> { com.example.feature.finance.data.repository.ExpenseRepositoryImpl(get()) }
    single<com.example.feature.finance.domain.repository.IncomeRepository> { com.example.feature.finance.data.repository.IncomeRepositoryImpl(get()) }
    single<com.example.feature.events.domain.repository.EventRepository> { com.example.feature.events.data.repository.EventRepositoryImpl(get()) }
    single<com.example.feature.notes.domain.repository.NoteRepository> { com.example.feature.notes.data.repository.NoteRepositoryImpl(get()) }
    single<com.example.feature.projects.domain.repository.ProjectRepository> { com.example.feature.projects.data.repository.ProjectRepositoryImpl(get()) }
    single<com.example.feature.habits.domain.repository.HabitRepository> { com.example.feature.habits.data.repository.HabitRepositoryImpl(get()) }
    single<com.example.feature.goals.domain.repository.GoalRepository> { com.example.feature.goals.data.repository.GoalRepositoryImpl(get()) }
    single { com.example.data.repository.JournalRepository(get()) }
    single { com.example.data.repository.DebtRepository(get(), get()) }
    
    // Task Use Cases
    factory { com.example.feature.tasks.domain.usecase.GetTasksUseCase(get()) }
    factory { com.example.feature.tasks.domain.usecase.AddTaskUseCase(get()) }
    factory { com.example.feature.tasks.domain.usecase.ToggleTaskCompletionUseCase(get()) }
    factory { com.example.feature.tasks.domain.usecase.DeleteTaskUseCase(get()) }
    
    // Expense Use Cases
    factory { com.example.feature.finance.domain.usecase.GetExpensesUseCase(get()) }
    factory { com.example.feature.finance.domain.usecase.GetTotalExpensesUseCase(get()) }
    factory { com.example.feature.finance.domain.usecase.AddExpenseUseCase(get()) }
    factory { com.example.feature.finance.domain.usecase.DeleteExpenseUseCase(get()) }

    // Income Use Cases
    factory { com.example.feature.finance.domain.usecase.GetIncomeUseCase(get()) }
    factory { com.example.feature.finance.domain.usecase.GetTotalIncomeUseCase(get()) }
    factory { com.example.feature.finance.domain.usecase.AddIncomeUseCase(get()) }
    factory { com.example.feature.finance.domain.usecase.DeleteIncomeUseCase(get()) }

    // Event Use Cases
    factory { com.example.feature.events.domain.usecase.GetEventsUseCase(get()) }
    factory { com.example.feature.events.domain.usecase.AddEventUseCase(get()) }
    factory { com.example.feature.events.domain.usecase.DeleteEventUseCase(get()) }

    // Note Use Cases
    factory { com.example.feature.notes.domain.usecase.GetNotesUseCase(get()) }
    factory { com.example.feature.notes.domain.usecase.AddNoteUseCase(get()) }
    factory { com.example.feature.notes.domain.usecase.DeleteNoteUseCase(get()) }

    // Project Use Cases
    factory { com.example.feature.projects.domain.usecase.GetProjectsUseCase(get()) }
    factory { com.example.feature.projects.domain.usecase.AddProjectUseCase(get()) }
    factory { com.example.feature.projects.domain.usecase.DeleteProjectUseCase(get()) }

    // Habit Use Cases
    factory { com.example.feature.habits.domain.usecase.GetHabitsUseCase(get()) }
    factory { com.example.feature.habits.domain.usecase.AddHabitUseCase(get()) }
    factory { com.example.feature.habits.domain.usecase.DeleteHabitUseCase(get()) }

    // Goal Use Cases
    factory { com.example.feature.goals.domain.usecase.GetGoalsUseCase(get()) }
    factory { com.example.feature.goals.domain.usecase.AddGoalUseCase(get()) }
    factory { com.example.feature.goals.domain.usecase.ToggleGoalCompletionUseCase(get()) }
    factory { com.example.feature.goals.domain.usecase.DeleteGoalUseCase(get()) }
    
    single { com.example.core.ai.GemmaLocalStateProvider(androidContext()) }
    single<AIEngine> { com.example.core.ai.Gemma4LocalEngine(get(), com.example.core.ai.LocalAIHeuristicsEngine) }
    single { com.example.utils.FinancePreferences(androidContext()) }

    viewModel { com.example.feature.dashboard.presentation.DashboardViewModel(
        get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
    ) }
    
    viewModel { com.example.ui.debt.DebtViewModel(get(), get(), get()) }
}
