package com.jan.psywikikt.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "drug_log",
    indices = [Index(value = ["name", "timestamp"], unique = true)]
)
data class DrugLog(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "dose")
    var dose: Double?,

    @ColumnInfo(name = "unit")
    var unit: String?,

    @ColumnInfo(name = "notes")
    var notes: String?,

    @ColumnInfo(name = "timestamp")
    var timestamp: Date
)
