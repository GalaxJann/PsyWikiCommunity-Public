package com.jan.psywikikt.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DrugLogDao {
    @Query("SELECT * FROM drug_log ORDER BY timestamp DESC")
    fun getAllLogs(): List<DrugLog>

    @Query("SELECT COUNT(*) FROM drug_log")
    fun getCountLogs(): Int

    @Query("SELECT * FROM drug_log WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getLogsDateRange(start: Long, end: Long): List<DrugLog>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addLog(log: DrugLog)

    @Delete
    suspend fun removeLog(log: DrugLog)

    @Query("DELETE FROM drug_log WHERE id = :id")
    fun removeLogById(id: Int)

    @Query("DELETE FROM drug_log")
    fun reset()
}