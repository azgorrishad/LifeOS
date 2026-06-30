package com.example.feature.notes.data.repository

import com.example.data.local.dao.NoteDao
import com.example.data.local.entity.NoteEntity
import com.example.feature.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class NoteRepositoryImpl(private val dao: NoteDao) : NoteRepository {
    override fun getAllNotes(): Flow<List<NoteEntity>> = dao.getAllNotes()
    override suspend fun insertNote(note: NoteEntity) = dao.insertNote(note)
    override suspend fun updateNote(note: NoteEntity) = dao.updateNote(note)
    override suspend fun deleteNote(id: String) = dao.deleteNoteById(id)
}
