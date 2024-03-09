package com.jan.psywikikt.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "substance_index_tripsit",
    indices = [Index(value = ["name"], unique = true)]
)
data class DrugIndexTripSit(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "id_name")
    var idName: String?,

    @ColumnInfo(name = "synonyms")
    var synonyms: String?
)