package com.example.capdex.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_locations")
data class CachedLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val tripId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Float,
    val altitude: Double,
    val accuracy: Float,
    val bearing: Float,
    val timestamp: Long,
    val userType: String,
    val userName: String,
    val synced: Boolean = false // false = não enviado ao Firebase ainda
)
