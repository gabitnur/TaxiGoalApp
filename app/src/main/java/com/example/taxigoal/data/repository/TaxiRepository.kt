package com.example.taxigoal.data.repository

import com.example.taxigoal.data.dao.TaxiDao
import com.example.taxigoal.data.entities.*
import kotlinx.coroutines.flow.Flow

class TaxiRepository(private val taxiDao: TaxiDao) {
    // Shifts
    fun getAllShifts(userId: String): Flow<List<Shift>> = taxiDao.getAllShifts(userId)
    fun getShiftsInRange(userId: String, start: Long, end: Long): Flow<List<Shift>> = taxiDao.getShiftsInRange(userId, start, end)
    suspend fun insertShift(shift: Shift) = taxiDao.insertShift(shift)
    suspend fun deleteShift(shift: Shift) = taxiDao.deleteShift(shift)

    // Goals
    fun getAllGoals(userId: String): Flow<List<Goal>> = taxiDao.getAllGoals(userId)
    fun getActiveGoalFlow(userId: String): Flow<Goal?> = taxiDao.getActiveGoalFlow(userId)
    suspend fun insertGoal(goal: Goal) = taxiDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = taxiDao.updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = taxiDao.deleteGoal(goal)

    // Transactions
    fun getAllTransactions(userId: String): Flow<List<FinancialTransaction>> = taxiDao.getAllTransactions(userId)
    suspend fun insertTransaction(transaction: FinancialTransaction) = taxiDao.insertTransaction(transaction)

    // Logs
    fun getAllAppLogs(userIdHash: String): Flow<List<AppLog>> = taxiDao.getAllLogs(userIdHash)
    suspend fun insertAppLog(log: AppLog) = taxiDao.insertAppLog(log)
}
