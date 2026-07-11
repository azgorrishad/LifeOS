package com.example.feature.finance.domain.repository

import com.example.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

interface IncomeRepository {
    fun getAllIncome(): Flow<List<IncomeEntity>>
    fun getTotalIncome(): Flow<Double?>
    suspend fun addIncome(income: IncomeEntity)
    suspend fun deleteIncome(id: Int)
}
