package com.jan.psywikikt.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "drug_unit",
    indices = [Index(value = ["unit"], unique = true)]
)
data class DrugUnit(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "unit")
    var unit: String,

    @ColumnInfo(name = "usedefault")
    var usedefault: Boolean
)