package com.example.feature.habits.data.repository

import com.example.data.local.dao.HabitDao
import com.example.data.local.entity.HabitEntity
import com.example.feature.habits.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class HabitRepositoryImpl(private val dao: HabitDao) : HabitRepository {
    override fun getAllHabits(): Flow<List<HabitEntity>> = dao.getAllHabits()
    override suspend fun insertHabit(habit: HabitEntity) = dao.insertHabit(habit)
    override suspend fun updateHabit(habit: HabitEntity) = dao.updateHabit(habit)
    override suspend fun deleteHabit(id: String) = dao.deleteHabitById(id)
}
