package com.jan.psywikikt.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.util.Date

@Dao
interface DrugFavouriteDao {
    @Query("SELECT * FROM substance_favourites")
    fun getAllFavourites(): List<DrugFavourite>

    @Query("SELECT * FROM substance_favourites WHERE name = :drugName")
    fun getFavourite(drugName: String): List<DrugFavourite>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFavourite(favourite: DrugFavourite)

    @Update
    suspend fun updateFavourite(favourite: DrugFavourite)

    @Query("UPDATE substance_favourites SET substance_data = :substanceData, update_date = :updateDate WHERE name = :drugName")
    fun updateByName(drugName: String, substanceData: String, updateDate: Date)

    @Delete
    suspend fun deleteFavourite(favourite: DrugFavourite)

    @Query("DELETE FROM substance_favourites WHERE name = :drugName")
    fun deleteFavouriteByName(drugName: String): Int
}