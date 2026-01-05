package com.example.capdex.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capdex.data.model.Boat
import com.example.capdex.data.model.Route
import com.example.capdex.data.repository.BoatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BoatUiState(
    val boats: List<Boat> = emptyList(),
    val selectedBoat: Boat? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isUploadingPhoto: Boolean = false
)

class BoatViewModel : ViewModel() {
    
    private val repository = BoatRepository()
    
    private val _uiState = MutableStateFlow(BoatUiState())
    val uiState: StateFlow<BoatUiState> = _uiState.asStateFlow()
    
    init {
        loadUserBoats()
    }
    
    /**
     * Carrega todas as embarcações do usuário
     */
    fun loadUserBoats() {
        viewModelScope.launch {
            android.util.Log.d("BoatViewModel", "loadUserBoats: Iniciando carregamento de embarcações")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = repository.getUserBoats()
            
            result.fold(
                onSuccess = { boats ->
                    android.util.Log.d("BoatViewModel", "loadUserBoats: Sucesso! ${boats.size} embarcações carregadas")
                    _uiState.value = _uiState.value.copy(
                        boats = boats,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    android.util.Log.e("BoatViewModel", "loadUserBoats: Erro ao carregar", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar embarcações"
                    )
                }
            )
        }
    }
    
    /**
     * Salva ou atualiza uma embarcação
     */
    fun saveBoat(boat: Boat) {
        viewModelScope.launch {
            android.util.Log.d("BoatViewModel", "saveBoat: Iniciando salvamento de ${boat.name}")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = repository.saveBoat(boat)
            
            result.fold(
                onSuccess = { savedBoat ->
                    android.util.Log.d("BoatViewModel", "saveBoat: Sucesso! Boat ID: ${savedBoat.id}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedBoat = savedBoat,
                        successMessage = "Embarcação salva com sucesso!",
                        errorMessage = null
                    )
                    loadUserBoats() // Recarrega lista
                },
                onFailure = { error ->
                    android.util.Log.e("BoatViewModel", "saveBoat: Erro ao salvar", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao salvar embarcação"
                    )
                }
            )
        }
    }
    
    /**
     * Faz upload da foto do barco
     */
    fun uploadBoatPhoto(boatId: String, imageUri: Uri, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingPhoto = true, errorMessage = null)
            
            val result = repository.uploadBoatPhoto(boatId, imageUri)
            
            result.fold(
                onSuccess = { photoUrl ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingPhoto = false,
                        successMessage = "Foto enviada com sucesso!"
                    )
                    onSuccess(photoUrl)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingPhoto = false,
                        errorMessage = error.message ?: "Erro ao enviar foto"
                    )
                }
            )
        }
    }
    
    /**
     * Adiciona uma rota a uma embarcação
     */
    fun addRoute(boatId: String, route: Route) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = repository.addRouteToBoat(boatId, route)
            
            result.fold(
                onSuccess = { updatedBoat ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedBoat = updatedBoat,
                        successMessage = "Rota adicionada com sucesso!",
                        errorMessage = null
                    )
                    loadUserBoats()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao adicionar rota"
                    )
                }
            )
        }
    }
    
    /**
     * Remove uma rota de uma embarcação
     */
    fun removeRoute(boatId: String, routeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = repository.removeRouteFromBoat(boatId, routeId)
            
            result.fold(
                onSuccess = { updatedBoat ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedBoat = updatedBoat,
                        successMessage = "Rota removida com sucesso!",
                        errorMessage = null
                    )
                    loadUserBoats()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao remover rota"
                    )
                }
            )
        }
    }
    
    /**
     * Seleciona uma embarcação
     */
    fun selectBoat(boat: Boat?) {
        _uiState.value = _uiState.value.copy(selectedBoat = boat)
    }
    
    /**
     * Desativa uma embarcação
     */
    fun deactivateBoat(boatId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = repository.deactivateBoat(boatId)
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Embarcação desativada com sucesso!",
                        errorMessage = null
                    )
                    loadUserBoats()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao desativar embarcação"
                    )
                }
            )
        }
    }
    
    /**
     * Limpa mensagens
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
