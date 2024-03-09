package com.jan.psywikikt.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface DrugIndexDao {
    @Query("SELECT * FROM substance_index")
    fun getAllDrugs(): List<DrugIndex>

    @Query("SELECT * FROM substance_index WHERE name LIKE :query or common_names LIKE :query LIMIT :limit")
    suspend fun getSearchDrugs(query: String, limit: Int = 1000): List<DrugIndex>

    @RawQuery
    suspend fun getFilterDrugs(query: SupportSQLiteQuery): List<DrugIndex>

    @Query("SELECT COUNT(*) FROM substance_index")
    suspend fun getCountDrugs(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDrug(index: DrugIndex)

    @Update
    suspend fun updateDrug(index: DrugIndex)

    @Delete
    suspend fun deleteDrug(index: DrugIndex)

    @Query("DELETE FROM substance_index WHERE name = :drugName")
    fun deleteDrugByName(drugName: String): Int

    @Query("DELETE FROM substance_index WHERE psychoactive_class IS NULL")
    suspend fun updateReset()
}