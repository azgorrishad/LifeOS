package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.DebtTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debt_transactions ORDER BY dueDate ASC")
    fun getAllDebts(): Flow<List<DebtTransactionEntity>>

    @Query("SELECT * FROM debt_transactions WHERE isSettled = :isSettled ORDER BY dueDate ASC")
    fun getDebtsByStatus(isSettled: Boolean): Flow<List<DebtTransactionEntity>>

    @Query("SELECT * FROM debt_transactions WHERE personId = :personId")
    fun getDebtsByPersonId(personId: Int): Flow<List<DebtTransactionEntity>>

    @Query("SELECT * FROM debt_transactions WHERE personId = :personId")
    suspend fun getDebtsByPersonIdSync(personId: Int): List<DebtTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtTransactionEntity): Long

    @Delete
    suspend fun deleteDebt(debt: DebtTransactionEntity)

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDate DESC")
    fun getPaymentsForDebt(debtId: Int): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDate DESC")
    suspend fun getPaymentsForDebtSync(debtId: Int): List<DebtPaymentEntity>
    
    @Query("SELECT * FROM debt_payments")
    fun getAllPayments(): Flow<List<DebtPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: DebtPaymentEntity)

    @Query("UPDATE debt_transactions SET isSettled = :isSettled WHERE id = :debtId")
    suspend fun updateDebtStatus(debtId: Int, isSettled: Boolean)

    @Query("SELECT * FROM debt_transactions WHERE id = :debtId")
    suspend fun getDebtById(debtId: Int): DebtTransactionEntity?

    @Query("UPDATE debt_transactions SET remainingAmount = :remainingAmount, isSettled = :isSettled WHERE id = :debtId")
    suspend fun updateDebtRemainingAndStatus(debtId: Int, remainingAmount: Double, isSettled: Boolean)
}
