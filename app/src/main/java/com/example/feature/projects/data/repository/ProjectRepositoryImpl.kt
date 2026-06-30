package com.example.feature.projects.data.repository

import com.example.data.local.dao.ProjectDao
import com.example.data.local.entity.ProjectEntity
import com.example.feature.projects.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow

class ProjectRepositoryImpl(private val dao: ProjectDao) : ProjectRepository {
    override fun getAllProjects(): Flow<List<ProjectEntity>> = dao.getAllProjects()
    override suspend fun insertProject(project: ProjectEntity) = dao.insertProject(project)
    override suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)
    override suspend fun deleteProject(id: String) = dao.deleteProjectById(id)
}
