package com.example.taxigoal.data.dao

import androidx.room.*
import com.example.taxigoal.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxiDao {
    // Shifts
    @Query("SELECT * FROM shifts WHERE userId = :userId ORDER BY date DESC")
    fun getAllShifts(userId: String): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE userId = :userId AND date >= :start AND date <= :end")
    fun getShiftsInRange(userId: String, start: Long, end: Long): Flow<List<Shift>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift)

    @Delete
    suspend fun deleteShift(shift: Shift)

    @Query("SELECT * FROM shifts")
    fun getAllShiftsSync(): List<Shift>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllShifts(list: List<Shift>)

    // Goals
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdDate DESC")
    fun getAllGoals(userId: String): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND isActive = 1 LIMIT 1")
    fun getActiveGoalFlow(userId: String): Flow<Goal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("SELECT * FROM goals")
    fun getAllGoalsSync(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGoals(list: List<Goal>)

    // Transactions
    @Query("SELECT * FROM financial_transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<FinancialTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialTransaction)

    @Query("SELECT * FROM financial_transactions")
    fun getAllTransactionsSync(): List<FinancialTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(list: List<FinancialTransaction>)

    // Error Logs
    @Query("SELECT * FROM app_logs WHERE userIdHash = :userIdHash ORDER BY timestamp DESC")
    fun getAllLogs(userIdHash: String): Flow<List<AppLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppLog(log: AppLog)
}
