package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.IncomeEntity

@Database(
    entities = [
        TaskEntity::class, 
        ExpenseEntity::class,
        EventEntity::class,
        NoteEntity::class,
        ProjectEntity::class,
        HabitEntity::class,
        GoalEntity::class,
        IncomeEntity::class,
        com.example.data.local.entity.JournalEntity::class,
        com.example.data.local.entity.PersonEntity::class,
        com.example.data.local.entity.DebtTransactionEntity::class,
        com.example.data.local.entity.DebtPaymentEntity::class
    ], 
    version = 8, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): com.example.data.local.dao.IncomeDao
    abstract fun eventDao(): com.example.data.local.dao.EventDao
    abstract fun noteDao(): com.example.data.local.dao.NoteDao
    abstract fun projectDao(): com.example.data.local.dao.ProjectDao
    abstract fun habitDao(): com.example.data.local.dao.HabitDao
    abstract fun goalDao(): com.example.data.local.dao.GoalDao
    abstract fun journalDao(): com.example.data.local.dao.JournalDao
    abstract fun personDao(): com.example.data.local.dao.PersonDao
    abstract fun debtDao(): com.example.data.local.dao.DebtDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifeos_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
