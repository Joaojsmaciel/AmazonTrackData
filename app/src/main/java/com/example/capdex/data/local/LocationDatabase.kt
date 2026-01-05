package com.example.capdex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CachedLocationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LocationDatabase : RoomDatabase() {
    
    abstract fun locationDao(): LocationDao
    
    companion object {
        @Volatile
        private var INSTANCE: LocationDatabase? = null
        
        fun getDatabase(context: Context): LocationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationDatabase::class.java,
                    "location_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
