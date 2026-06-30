package com.example.feature.notes.domain.usecase

import com.example.data.local.entity.NoteEntity
import com.example.feature.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class GetNotesUseCase(private val repository: NoteRepository) {
    operator fun invoke(): Flow<List<NoteEntity>> = repository.getAllNotes()
}

class AddNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(note: NoteEntity) {
        repository.insertNote(note)
    }
}

class DeleteNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(id: String) {
        repository.deleteNote(id)
    }
}
