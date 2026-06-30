package com.example.feature.projects.domain.usecase

import com.example.data.local.entity.ProjectEntity
import com.example.feature.projects.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow

class GetProjectsUseCase(private val repository: ProjectRepository) {
    operator fun invoke(): Flow<List<ProjectEntity>> = repository.getAllProjects()
}

class AddProjectUseCase(private val repository: ProjectRepository) {
    suspend operator fun invoke(project: ProjectEntity) {
        repository.insertProject(project)
    }
}

class DeleteProjectUseCase(private val repository: ProjectRepository) {
    suspend operator fun invoke(id: String) {
        repository.deleteProject(id)
    }
}
