package com.example.capdex.ui.viewmodel

import android.util.Log
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
    
    private var isManualAuthInProgress = false
    
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
                if (isManualAuthInProgress) {
                    Log.d("AuthViewModel", "observeAuthChanges: Skipping - manual auth in progress")
                    return@collect
                }
                
                Log.d("AuthViewModel", "observeAuthChanges: firebaseUser = ${firebaseUser?.uid}")
                if (firebaseUser == null) {
                    _uiState.value = AuthUiState(isAuthenticated = false)
                } else {
                    val userData = repository.getCurrentUserData()
                    Log.d("AuthViewModel", "observeAuthChanges: userData = ${userData?.email}")
                    _uiState.value = _uiState.value.copy(
                        user = userData,
                        isAuthenticated = true,
                        isLoading = false
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
            isManualAuthInProgress = true
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = repository.signUp(email, password, fullName, cpf, userType)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = result.data,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                    isManualAuthInProgress = false
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                    isManualAuthInProgress = false
                }
                else -> {
                    isManualAuthInProgress = false
                }
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            isManualAuthInProgress = true
            Log.d("AuthViewModel", "signIn: Starting login for $email")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = repository.signIn(email, password)) {
                is AuthResult.Success -> {
                    Log.d("AuthViewModel", "signIn: Success - user = ${result.data.email}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = result.data,
                        isAuthenticated = true,
                        errorMessage = null
                    )
                    isManualAuthInProgress = false
                }
                is AuthResult.Error -> {
                    Log.e("AuthViewModel", "signIn: Error - ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                    isManualAuthInProgress = false
                }
                else -> {
                    Log.w("AuthViewModel", "signIn: Unknown result type")
                    isManualAuthInProgress = false
                }
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
