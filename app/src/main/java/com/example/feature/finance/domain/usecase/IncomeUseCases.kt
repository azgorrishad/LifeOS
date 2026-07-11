package com.example.feature.finance.domain.usecase

import com.example.data.local.entity.IncomeEntity
import com.example.feature.finance.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow

class GetIncomeUseCase(private val repository: IncomeRepository) {
    operator fun invoke(): Flow<List<IncomeEntity>> = repository.getAllIncome()
}

class GetTotalIncomeUseCase(private val repository: IncomeRepository) {
    operator fun invoke(): Flow<Double?> = repository.getTotalIncome()
}

class AddIncomeUseCase(private val repository: IncomeRepository) {
    suspend operator fun invoke(amount: Double, source: String, note: String = "") {
        repository.addIncome(IncomeEntity(amount = amount, source = source, note = note))
    }
}

class DeleteIncomeUseCase(private val repository: IncomeRepository) {
    suspend operator fun invoke(id: Int) = repository.deleteIncome(id)
}
