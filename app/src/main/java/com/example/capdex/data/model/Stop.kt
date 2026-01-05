package com.example.capdex.data.model

data class Stop(
    val id: String = "",
    val name: String = "",              // Ex: "Porto de Manaus"
    val city: String = "",              // Cidade
    val state: String = "AM",           // Estado
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 500.0,         // Raio em metros para geofencing
    val type: StopType = StopType.PORTO
) {
    // Construtor vazio para Firestore
    constructor() : this("", "", "", "AM", 0.0, 0.0, 500.0, StopType.PORTO)
}

enum class StopType(val displayName: String) {
    PORTO("Porto"),
    TERMINAL("Terminal"),
    COMUNIDADE("Comunidade"),
    CIDADE("Cidade"),
    OUTRO("Outro")
}

// Portos pré-cadastrados comuns na região amazônica
object CommonStops {
    val MANAUS = Stop(
        id = "manaus_porto",
        name = "Porto de Manaus",
        city = "Manaus",
        state = "AM",
        latitude = -3.1190,
        longitude = -60.0217,
        radius = 1000.0,
        type = StopType.PORTO
    )
    
    val ITACOATIARA = Stop(
        id = "itacoatiara_porto",
        name = "Porto de Itacoatiara",
        city = "Itacoatiara",
        state = "AM",
        latitude = -3.1430,
        longitude = -58.4443,
        radius = 500.0,
        type = StopType.PORTO
    )
    
    val PARINTINS = Stop(
        id = "parintins_porto",
        name = "Porto de Parintins",
        city = "Parintins",
        state = "AM",
        latitude = -2.6283,
        longitude = -56.7358,
        radius = 500.0,
        type = StopType.PORTO
    )
    
    val SANTAREM = Stop(
        id = "santarem_porto",
        name = "Porto de Santarém",
        city = "Santarém",
        state = "PA",
        latitude = -2.4376,
        longitude = -54.6981,
        radius = 800.0,
        type = StopType.PORTO
    )
    
    val MANACAPURU = Stop(
        id = "manacapuru_porto",
        name = "Porto de Manacapuru",
        city = "Manacapuru",
        state = "AM",
        latitude = -3.3000,
        longitude = -60.6208,
        radius = 400.0,
        type = StopType.PORTO
    )
    
    val COARI = Stop(
        id = "coari_porto",
        name = "Porto de Coari",
        city = "Coari",
        state = "AM",
        latitude = -4.0850,
        longitude = -63.1415,
        radius = 500.0,
        type = StopType.PORTO
    )
    
    // Lista de todos os portos pré-cadastrados
    val ALL_STOPS = listOf(
        MANAUS,
        ITACOATIARA,
        PARINTINS,
        SANTAREM,
        MANACAPURU,
        COARI
    )
}
