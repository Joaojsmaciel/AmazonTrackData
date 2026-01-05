package com.example.capdex.data.model

data class Boat(
    val id: String = "",
    val ownerId: String = "",           // UID do barqueiro
    val ownerName: String = "",         // Nome do barqueiro
    val name: String = "",              // Nome do barco
    val photoUrl: String? = null,       // URL da foto no Firebase Storage
    val routes: List<Route> = emptyList(), // Rotas cadastradas
    val capacity: Int? = null,          // Capacidade de passageiros (opcional)
    val type: String = "Barco",         // Tipo: Barco, Lancha, Balsa, etc
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true        // Se o barco está ativo
) {
    // Construtor vazio necessário para Firestore
    constructor() : this("", "", "", "", null, emptyList(), null, "Barco", System.currentTimeMillis(), true)
}
