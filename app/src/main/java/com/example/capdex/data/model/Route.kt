package com.example.capdex.data.model

data class Route(
    val id: String = "",
    val name: String = "",              // Ex: "Manaus - Itacoatiara"
    val origin: Stop,                   // Ponto de partida
    val destination: Stop,              // Ponto final
    val stops: List<Stop> = emptyList(), // Paradas intermediárias
    val estimatedDurationMinutes: Int = 0, // Duração estimada
    val isFrequent: Boolean = true,     // Se é uma rota frequente
    val createdAt: Long = System.currentTimeMillis()
) {
    // Construtor vazio para Firestore
    constructor() : this(
        "", "", 
        Stop(), 
        Stop(), 
        emptyList(), 
        0, 
        true, 
        System.currentTimeMillis()
    )
    
    // Retorna todas as paradas (origem + intermediárias + destino)
    fun getAllStops(): List<Stop> {
        return listOf(origin) + stops + listOf(destination)
    }
    
    // Retorna string formatada da rota
    fun getRouteDescription(): String {
        return if (stops.isEmpty()) {
            "${origin.name} → ${destination.name}"
        } else {
            val middle = stops.joinToString(" → ") { it.name }
            "${origin.name} → $middle → ${destination.name}"
        }
    }
}
