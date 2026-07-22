package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class DebtType {
    RECEIVABLE, // Money lent to someone (LENT)
    PAYABLE     // Money borrowed from someone (BORROWED)
}

@Entity(
    tableName = "debt_transactions",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"], onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("personId")]
)
data class DebtTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val personId: Int,
    val amount: Double, // original amount
    val remainingAmount: Double = amount, // dynamic remaining amount
    val type: DebtType, // RECEIVABLE or PAYABLE
    val description: String, // purpose
    val category: String, // Friend, Family, Client, Business, Employee, Other
    val dueDate: Long,
    val isSettled: Boolean = false,
    val interestRate: Double = 0.0, // interest rate (optional)
    val borrowDate: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val paymentMethod: String = "Cash",
    val receiptPath: String? = null, // Path to receipt image/photo
    val reminderEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
