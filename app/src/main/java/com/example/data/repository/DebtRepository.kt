package com.example.data.repository

import com.example.data.local.dao.DebtDao
import com.example.data.local.dao.PersonDao
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.DebtTransactionEntity
import com.example.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow

class DebtRepository(private val debtDao: DebtDao, private val personDao: PersonDao) {
    private val _writeEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val writeEvents = _writeEvents.asSharedFlow()

    private suspend fun notifyWrite() {
        _writeEvents.emit(Unit)
    }

    fun getAllDebts(): Flow<List<DebtTransactionEntity>> = debtDao.getAllDebts()
    fun getDebtsByStatus(isSettled: Boolean): Flow<List<DebtTransactionEntity>> = debtDao.getDebtsByStatus(isSettled)
    fun getDebtsByPersonId(personId: Int): Flow<List<DebtTransactionEntity>> = debtDao.getDebtsByPersonId(personId)
    suspend fun getDebtsByPersonIdSync(personId: Int): List<DebtTransactionEntity> = debtDao.getDebtsByPersonIdSync(personId)
    suspend fun getPaymentsForDebtSync(debtId: Int): List<DebtPaymentEntity> = debtDao.getPaymentsForDebtSync(debtId)
    
    suspend fun insertDebt(debt: DebtTransactionEntity): Long {
        val result = debtDao.insertDebt(debt)
        notifyWrite()
        return result
    }

    suspend fun deleteDebt(debt: DebtTransactionEntity) {
        debtDao.deleteDebt(debt)
        notifyWrite()
    }

    suspend fun updateDebtStatus(debtId: Int, isSettled: Boolean) {
        debtDao.updateDebtStatus(debtId, isSettled)
        notifyWrite()
    }

    fun getPaymentsForDebt(debtId: Int): Flow<List<DebtPaymentEntity>> = debtDao.getPaymentsForDebt(debtId)
    fun getAllPayments(): Flow<List<DebtPaymentEntity>> = debtDao.getAllPayments()
    
    suspend fun insertPayment(payment: DebtPaymentEntity) {
        debtDao.insertPayment(payment)
        val debt = debtDao.getDebtById(payment.debtId)
        if (debt != null) {
            val newRemaining = (debt.remainingAmount - payment.amount).coerceAtLeast(0.0)
            val isSettled = newRemaining <= 0.0
            debtDao.updateDebtRemainingAndStatus(debt.id, newRemaining, isSettled)
        }
        notifyWrite()
    }

    suspend fun deletePayment(payment: DebtPaymentEntity) {
        debtDao.deletePayment(payment)
        val debt = debtDao.getDebtById(payment.debtId)
        if (debt != null) {
            val newRemaining = (debt.remainingAmount + payment.amount).coerceAtMost(debt.amount)
            val isSettled = newRemaining <= 0.0
            debtDao.updateDebtRemainingAndStatus(debt.id, newRemaining, isSettled)
        }
        notifyWrite()
    }

    fun getAllPersons(): Flow<List<PersonEntity>> = personDao.getAllPersons()
    
    suspend fun insertPerson(person: PersonEntity): Long {
        val result = personDao.insertPerson(person)
        notifyWrite()
        return result
    }

    suspend fun deletePerson(person: PersonEntity) {
        personDao.deletePerson(person)
        notifyWrite()
    }
}
