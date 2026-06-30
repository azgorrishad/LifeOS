package com.example.feature.goals.domain.repository

import com.example.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<GoalEntity>>
    suspend fun insertGoal(goal: GoalEntity)
    suspend fun updateGoal(goal: GoalEntity)
    suspend fun deleteGoal(id: String)
}
