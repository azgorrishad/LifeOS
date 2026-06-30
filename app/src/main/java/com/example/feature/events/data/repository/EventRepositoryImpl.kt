package com.example.feature.events.data.repository

import com.example.data.local.dao.EventDao
import com.example.data.local.entity.EventEntity
import com.example.feature.events.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

class EventRepositoryImpl(private val dao: EventDao) : EventRepository {
    override fun getAllEvents(): Flow<List<EventEntity>> = dao.getAllEvents()
    override suspend fun insertEvent(event: EventEntity) = dao.insertEvent(event)
    override suspend fun updateEvent(event: EventEntity) = dao.updateEvent(event)
    override suspend fun deleteEvent(id: String) = dao.deleteEventById(id)
}
