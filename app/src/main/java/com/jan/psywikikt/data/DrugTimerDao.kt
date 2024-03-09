package com.jan.psywikikt.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.Date

@Dao
interface DrugTimerDao {
    @Query("SELECT * FROM drug_timer ORDER BY timestamp DESC")
    fun getAllTimers(): List<DrugTimer>

    @Query("SELECT COUNT(*) FROM drug_timer")
    fun getCountTimers(): Int

    @Query("UPDATE drug_timer SET name = :name, dose = :dose, unit = :unit, notes = :notes, timestamp = :timestamp WHERE id = :id")
    fun updateTimer(id: Int, name: String, dose: Double?, unit: String?, notes: String?, timestamp: Date)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addTimer(timer: DrugTimer)

    @Delete
    suspend fun removeTimer(timer: DrugTimer)

    // Delete all timers
    @Query("DELETE FROM drug_timer")
    suspend fun removeAllTimers()
}