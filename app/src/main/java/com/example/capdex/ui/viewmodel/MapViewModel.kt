package com.example.capdex.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capdex.data.model.TrackedUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    private var currentUserId: String? = null
    
    private val _trackedUsers = MutableStateFlow<List<TrackedUser>>(emptyList())
    val trackedUsers: StateFlow<List<TrackedUser>> = _trackedUsers.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Mapa para guardar os dados temporários de cada usuário
    private val trackedUsersMap = mutableMapOf<String, TrackedUser>()
    
    // Listeners ativos para limpar depois
    private var usersListener: ListenerRegistration? = null
    private val locationListeners = mutableMapOf<String, ListenerRegistration>()
    
    init {
        startListeningToTrackedUsers()
    }
    
    fun setCurrentUserId(userId: String?) {
        currentUserId = userId
        updateTrackedUsersList()
    }
    
    private fun startListeningToTrackedUsers() {
        _isLoading.value = true
        
        // Listener para mudanças na lista de usuários (quando alguém ativa/desativa rastreamento)
        usersListener = firestore.collection("users")
            .whereIn("userType", listOf("MOTOTAXI", "BARQUEIRO"))
            .addSnapshotListener { usersSnapshot, error ->
                if (error != null) {
                    _error.value = error.message
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                
                if (usersSnapshot != null) {
                    val currentUserIds = usersSnapshot.documents.map { it.id }.toSet()
                    
                    // Remove listeners de usuários que não existem mais
                    val removedUsers = locationListeners.keys.filter { it !in currentUserIds }
                    removedUsers.forEach { userId ->
                        locationListeners[userId]?.remove()
                        locationListeners.remove(userId)
                        trackedUsersMap.remove(userId)
                    }
                    
                    // Adiciona ou atualiza listeners para cada usuário
                    for (userDoc in usersSnapshot.documents) {
                        val userId = userDoc.id
                        val userName = userDoc.getString("fullName") ?: userDoc.getString("name") ?: ""
                        val userType = userDoc.getString("userType") ?: ""
                        
                        // Se já tem listener para este usuário, pula
                        if (locationListeners.containsKey(userId)) {
                            // Atualiza apenas nome e tipo se mudaram
                            trackedUsersMap[userId]?.let { existingUser ->
                                if (existingUser.userName != userName || existingUser.userType != userType) {
                                    trackedUsersMap[userId] = existingUser.copy(
                                        userName = userName,
                                        userType = userType
                                    )
                                    updateTrackedUsersList()
                                }
                            }
                            continue
                        }
                        
                        // Cria listener em tempo real para a localização deste usuário
                        val locationListener = firestore
                            .collection("locations")
                            .document(userId)
                            .collection("tracks")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .addSnapshotListener { locationSnapshot, locError ->
                                if (locError != null) {
                                    return@addSnapshotListener
                                }
                                
                                if (locationSnapshot != null && !locationSnapshot.isEmpty) {
                                    val lastLocation = locationSnapshot.documents[0]
                                    val latitude = lastLocation.getDouble("latitude") ?: 0.0
                                    val longitude = lastLocation.getDouble("longitude") ?: 0.0
                                    // Tenta ler speed como Double primeiro, depois como Long (caso seja inteiro)
                                    val speedValue = lastLocation.get("speed")
                                    val speed = when (speedValue) {
                                        is Double -> speedValue.toFloat()
                                        is Long -> speedValue.toFloat()
                                        is Float -> speedValue
                                        is Number -> speedValue.toFloat()
                                        else -> 0f
                                    }
                                    val timestamp = lastLocation.getLong("timestamp") ?: 0L
                                    
                                    Log.d("MapViewModel", "User: $userName, Speed value: $speedValue, Speed type: ${speedValue?.javaClass?.simpleName}, Final speed: $speed")
                                    
                                    // Verifica se a localização é recente (últimos 60 segundos)
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - timestamp < 60 * 1000) {
                                        // Atualiza ou adiciona usuário no mapa
                                        trackedUsersMap[userId] = TrackedUser(
                                            userId = userId,
                                            userName = userName,
                                            userType = userType,
                                            latitude = latitude,
                                            longitude = longitude,
                                            speed = speed,
                                            lastUpdate = timestamp
                                        )
                                    } else {
                                        // Remove usuário se localização muito antiga
                                        trackedUsersMap.remove(userId)
                                    }
                                    
                                    updateTrackedUsersList()
                                } else {
                                    // Sem localização, remove do mapa
                                    trackedUsersMap.remove(userId)
                                    updateTrackedUsersList()
                                }
                            }
                        
                        locationListeners[userId] = locationListener
                    }
                    
                    _isLoading.value = false
                    updateTrackedUsersList()
                }
            }
    }
    
    private fun updateTrackedUsersList() {
        // Filtra o usuário atual e atualiza a lista
        val filteredList = trackedUsersMap.values
            .filter { it.userId != currentUserId }
            .toList()
        
        _trackedUsers.value = filteredList
    }
    
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // Remove todos os listeners ao destruir o ViewModel
        usersListener?.remove()
        locationListeners.values.forEach { it.remove() }
        locationListeners.clear()
        trackedUsersMap.clear()
    }
}
