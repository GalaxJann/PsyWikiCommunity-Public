package com.jan.psywikikt.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [DrugIndex::class, DrugIndexTripSit::class, DrugFavourite::class, DrugTimer::class, DrugLog::class, DrugUnit::class],
    version = 11,
    autoMigrations = [
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10, spec = MainDatabase.SubstanceIndexMigration::class),
        AutoMigration(from = 10, to = 11)
    ],
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MainDatabase: RoomDatabase() {
    abstract fun drugIndexDao(): DrugIndexDao
    abstract fun drugIndexTripSitDao(): DrugIndexTripSitDao
    abstract fun drugFavouriteDao(): DrugFavouriteDao
    abstract fun drugTimerDao(): DrugTimerDao
    abstract fun drugLogDao(): DrugLogDao
    abstract fun drugUnitDao(): DrugUnitDao

    @DeleteColumn(
        tableName = "substance_index",
        columnName = "psychoactive_class"
    )
    class SubstanceIndexMigration: AutoMigrationSpec {}

    companion object {
        private var instance: MainDatabase? = null
        fun getInstance(context: Context): MainDatabase {
            if(instance == null) {
                instance = Room.databaseBuilder(
                    context,
                    MainDatabase::class.java, "main_database"
                ).allowMainThreadQueries().build()
            }
            return instance as MainDatabase
        }
    }
}