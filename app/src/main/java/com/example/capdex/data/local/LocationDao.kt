package com.example.capdex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LocationDao {
    
    @Insert
    suspend fun insert(location: CachedLocationEntity): Long
    
    @Query("SELECT * FROM cached_locations WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedLocations(): List<CachedLocationEntity>
    
    @Update
    suspend fun update(location: CachedLocationEntity)
    
    @Query("UPDATE cached_locations SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
    
    @Query("DELETE FROM cached_locations WHERE synced = 1 AND timestamp < :olderThan")
    suspend fun deleteSyncedOlderThan(olderThan: Long)
    
    @Query("SELECT COUNT(*) FROM cached_locations WHERE synced = 0")
    suspend fun getUnsyncedCount(): Int
}
