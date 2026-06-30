package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val frequency: String, // e.g., DAILY, WEEKLY
    val targetDays: Int,
    val currentStreak: Int,
    val createdAt: Long
)
