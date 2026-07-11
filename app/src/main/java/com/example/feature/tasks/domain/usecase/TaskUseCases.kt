package com.example.feature.tasks.domain.usecase

import com.example.data.local.entity.TaskEntity
import com.example.feature.tasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class GetTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(): Flow<List<TaskEntity>> = repository.getAllTasks()
}

class AddTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(title: String, priority: Int, category: String = "Personal") {
        repository.insertTask(TaskEntity(title = title, priority = priority, category = category))
    }
}

class ToggleTaskCompletionUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: TaskEntity) {
        repository.updateTask(task.copy(isCompleted = !task.isCompleted))
    }
}

class DeleteTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: TaskEntity) {
        repository.deleteTask(task.id)
    }
}
