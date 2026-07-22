package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("debtId")]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val debtId: Int,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String? = null
)
