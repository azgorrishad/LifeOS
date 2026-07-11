package com.example.feature.finance.data.repository

import com.example.data.local.dao.IncomeDao
import com.example.data.local.entity.IncomeEntity
import com.example.feature.finance.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow

class IncomeRepositoryImpl(private val incomeDao: IncomeDao) : IncomeRepository {
    override fun getAllIncome(): Flow<List<IncomeEntity>> = incomeDao.getAllIncome()
    override fun getTotalIncome(): Flow<Double?> = incomeDao.getTotalIncome()
    override suspend fun addIncome(income: IncomeEntity) = incomeDao.insertIncome(income)
    override suspend fun deleteIncome(id: Int) = incomeDao.deleteIncomeById(id)
}
