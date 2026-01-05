package com.example.capdex.data.model

data class TrackedUser(
    val userId: String = "",
    val userName: String = "",
    val userType: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float = 0f,
    val lastUpdate: Long = 0L
)
