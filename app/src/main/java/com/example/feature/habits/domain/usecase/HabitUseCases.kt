package com.example.feature.habits.domain.usecase

import com.example.data.local.entity.HabitEntity
import com.example.feature.habits.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class GetHabitsUseCase(private val repository: HabitRepository) {
    operator fun invoke(): Flow<List<HabitEntity>> = repository.getAllHabits()
}

class AddHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(habit: HabitEntity) {
        repository.insertHabit(habit)
    }
}

class DeleteHabitUseCase(private val repository: HabitRepository) {
    suspend operator fun invoke(id: String) {
        repository.deleteHabit(id)
    }
}
