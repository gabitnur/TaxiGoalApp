package com.example.taxigoal.data.dao

import androidx.room.*
import com.example.taxigoal.data.entities.AppLog
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AppLog>>

    @Query("SELECT * FROM app_logs WHERE level = :level ORDER BY timestamp DESC")
    fun getLogsByLevel(level: String): Flow<List<AppLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AppLog)

    @Query("DELETE FROM app_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM app_logs WHERE timestamp < :threshold")
    suspend fun deleteOldLogs(threshold: Long)
    @Query("DELETE FROM app_logs WHERE id IN (:ids)")
    suspend fun deleteLogsByIds(ids: List<Long>)
}
