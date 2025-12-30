package com.example.capdex.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capdex.data.model.AuthResult
import com.example.capdex.data.model.User
import com.example.capdex.data.model.UserType
import com.example.capdex.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val user: User? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthState()
        observeAuthChanges()
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            val user = repository.getCurrentUserData()
            _uiState.value = _uiState.value.copy(
                user = user,
                isAuthenticated = user != null
            )
        }
    }
    
    private fun observeAuthChanges() {
        viewModelScope.launch {
            repository.observeAuthState().collect { firebaseUser ->
                if (firebaseUser == null) {
                    _uiState.value = AuthUiState(isAuthenticated = false)
                } else {
                    val userData = repository.getCurrentUserData()
                    _uiState.value = _uiState.value.copy(
                        user = userData,
                        isAuthenticated = true
                    )
                }
            }
        }
    }
    
    fun signUp(
        email: String,
        password: String,
        fullName: String,
        cpf: String,
        userType: UserType
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = repository.signUp(email, password, fullName, cpf, userType)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = result.data,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                else -> {}
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = repository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = result.data,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                else -> {}
            }
        }
    }
    
    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState(isAuthenticated = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
