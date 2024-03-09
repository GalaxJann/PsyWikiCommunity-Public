package com.jan.psywikikt.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "substance_index",
    indices = [Index(value = ["name"], unique = true)]
)
data class DrugIndex(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "common_names")
    var commonNames: String,

    @ColumnInfo(name = "psychoactive_class")
    var psychoactiveClass: String?
)
