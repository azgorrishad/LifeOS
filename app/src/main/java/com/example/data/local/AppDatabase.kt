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

@Database(
    entities = [
        TaskEntity::class, 
        ExpenseEntity::class,
        EventEntity::class,
        NoteEntity::class,
        ProjectEntity::class,
        HabitEntity::class,
        GoalEntity::class
    ], 
    version = 2, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun eventDao(): com.example.data.local.dao.EventDao
    abstract fun noteDao(): com.example.data.local.dao.NoteDao
    abstract fun projectDao(): com.example.data.local.dao.ProjectDao
    abstract fun habitDao(): com.example.data.local.dao.HabitDao
    abstract fun goalDao(): com.example.data.local.dao.GoalDao

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
