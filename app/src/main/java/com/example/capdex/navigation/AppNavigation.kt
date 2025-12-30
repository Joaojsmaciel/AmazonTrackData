package com.example.capdex.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.capdex.ui.screens.HomeScreen
import com.example.capdex.ui.screens.LoginScreen
import com.example.capdex.ui.screens.SignUpScreen
import com.example.capdex.ui.viewmodel.AuthViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Determina a tela inicial baseado no estado de autenticação
    val startDestination = if (uiState.isAuthenticated) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(viewModel = viewModel)
        }
    }
    
    // Observa mudanças no estado de autenticação e navega automaticamente
    if (uiState.isAuthenticated && navController.currentDestination?.route != Screen.Home.route) {
        navController.navigate(Screen.Home.route) {
            popUpTo(0) { inclusive = true }
        }
    } else if (!uiState.isAuthenticated && 
               navController.currentDestination?.route == Screen.Home.route) {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }
}
