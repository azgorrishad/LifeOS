package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 0, // 0 = Low, 1 = Med, 2 = High
    val category: String = "Personal",
    val dueDate: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)
