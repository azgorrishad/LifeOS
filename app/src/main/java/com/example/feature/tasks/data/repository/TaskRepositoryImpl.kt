package com.example.feature.tasks.data.repository

import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.TaskEntity
import com.example.feature.tasks.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {
    override fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()
    override suspend fun insertTask(task: TaskEntity) = taskDao.insertTask(task)
    override suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    override suspend fun deleteTask(id: Int) = taskDao.deleteTaskById(id)
}
