package com.example.feature.events.domain.usecase

import com.example.data.local.entity.EventEntity
import com.example.feature.events.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

class GetEventsUseCase(private val repository: EventRepository) {
    operator fun invoke(): Flow<List<EventEntity>> = repository.getAllEvents()
}

class AddEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(event: EventEntity) {
        repository.insertEvent(event)
    }
}

class DeleteEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(id: String) {
        repository.deleteEvent(id)
    }
}
