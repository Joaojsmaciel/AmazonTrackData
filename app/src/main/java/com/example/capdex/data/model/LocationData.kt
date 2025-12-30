package com.example.capdex.data.model

data class LocationData(
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float = 0f, // em m/s
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val bearing: Float = 0f, // direção em graus
    val timestamp: Long = System.currentTimeMillis(),
    val userType: String = "",
    val userName: String = ""
)
