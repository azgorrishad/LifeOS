package com.example.feature.goals.data.repository

import com.example.data.local.dao.GoalDao
import com.example.data.local.entity.GoalEntity
import com.example.feature.goals.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

class GoalRepositoryImpl(private val dao: GoalDao) : GoalRepository {
    override fun getAllGoals(): Flow<List<GoalEntity>> = dao.getAllGoals()
    override suspend fun insertGoal(goal: GoalEntity) = dao.insertGoal(goal)
    override suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    override suspend fun deleteGoal(id: String) = dao.deleteGoalById(id)
}
