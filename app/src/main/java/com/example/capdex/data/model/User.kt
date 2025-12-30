package com.example.capdex.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val cpf: String = "",
    val userType: UserType = UserType.PASSAGEIRO,
    val createdAt: Long = System.currentTimeMillis()
)
