package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val colorHex: String,
    val status: String, // e.g., ACTIVE, ARCHIVED, COMPLETED
    val deadline: Long? = null
)
