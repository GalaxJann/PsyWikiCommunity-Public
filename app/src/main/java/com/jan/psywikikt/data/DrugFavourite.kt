package com.jan.psywikikt.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "substance_favourites",
    indices = [Index(value = ["name"], unique = true)]
)
data class DrugFavourite(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "substance_data")
    var substanceData: String,

    @ColumnInfo(name = "update_date")
    var updateDate: Date
)