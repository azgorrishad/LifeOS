package com.example.data.repository

import com.example.data.local.dao.JournalDao
import com.example.data.local.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    fun getAllJournals(): Flow<List<JournalEntity>> = journalDao.getAllJournals()

    suspend fun insertJournal(journal: JournalEntity) {
        journalDao.insertJournal(journal)
    }

    suspend fun deleteJournal(journal: JournalEntity) {
        journalDao.deleteJournal(journal)
    }
}
