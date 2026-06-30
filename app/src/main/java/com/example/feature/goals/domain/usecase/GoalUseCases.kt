package com.example.feature.goals.domain.usecase

import com.example.data.local.entity.GoalEntity
import com.example.feature.goals.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

class GetGoalsUseCase(private val repository: GoalRepository) {
    operator fun invoke(): Flow<List<GoalEntity>> = repository.getAllGoals()
}

class AddGoalUseCase(private val repository: GoalRepository) {
    suspend operator fun invoke(goal: GoalEntity) {
        repository.insertGoal(goal)
    }
}

class DeleteGoalUseCase(private val repository: GoalRepository) {
    suspend operator fun invoke(id: String) {
        repository.deleteGoal(id)
    }
}

class ToggleGoalCompletionUseCase(private val repository: GoalRepository) {
    suspend operator fun invoke(goal: GoalEntity) {
        repository.updateGoal(goal.copy(isCompleted = !goal.isCompleted))
    }
}
