package com.jan.psywikikt.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DrugUnitDao {
    @Query("SELECT * FROM drug_unit")
    fun getAllUnits(): List<DrugUnit>

    @Query("SELECT * FROM drug_unit WHERE id = :id")
    fun getUnitById(id: Int): DrugUnit?

    @Query("SELECT * FROM drug_unit WHERE unit = :unit")
    fun getUnitByName(unit: String): DrugUnit?

    @Query("SELECT * FROM drug_unit WHERE usedefault = 1")
    fun getDefaultUnit(): DrugUnit?

    @Query("UPDATE drug_unit SET usedefault = 1 WHERE unit = :unit")
    fun setDefaultUnit(unit: String)

    @Query("UPDATE drug_unit SET usedefault = 0 WHERE usedefault = 1")
    fun disableDefaultUnit()

    @Transaction
    fun setDefault(unit: String) {
        disableDefaultUnit()
        setDefaultUnit(unit)
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addUnit(unit: DrugUnit)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addMultiUnits(unit: List<DrugUnit>)

    @Delete
    suspend fun deleteUnit(unit: DrugUnit)
}