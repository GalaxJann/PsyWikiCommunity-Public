package com.jan.psywikikt.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DrugIndexTripSitDao {
    @Query("SELECT * FROM substance_index_tripsit WHERE name LIKE :query or synonyms LIKE :query")
    suspend fun getSearchDrugs(query: String): List<DrugIndexTripSit>

    @Query("SELECT COUNT(*) FROM substance_index_tripsit")
    suspend fun getCountDrugs(): Int

    @Query("SELECT id_name FROM substance_index_tripsit WHERE name = :query")
    suspend fun getIDNameFromPrettyName(query: String): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDrug(index: DrugIndexTripSit)

    @Update
    suspend fun updateDrug(index: DrugIndexTripSit)

    @Delete
    suspend fun deleteDrug(index: DrugIndexTripSit)

    @Query("DELETE FROM substance_index_tripsit WHERE id_name IS NULL AND synonyms IS NULL")
    suspend fun updateReset()
}