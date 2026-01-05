package com.example.capdex.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.capdex.data.model.UserType
import com.example.capdex.ui.screens.*
import com.example.capdex.ui.viewmodel.AuthViewModel
import com.example.capdex.ui.viewmodel.BoatViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsState()
    val boatViewModel: BoatViewModel = viewModel()
    val boatUiState by boatViewModel.uiState.collectAsState()
    
    var hasCheckedBoats by remember { mutableStateOf(false) }
    
    // Verifica se barqueiro tem embarcações
    LaunchedEffect(uiState.isAuthenticated, uiState.user?.userType) {
        if (uiState.isAuthenticated && uiState.user?.userType == UserType.BARQUEIRO && !hasCheckedBoats) {
            boatViewModel.loadUserBoats()
            hasCheckedBoats = true
        }
    }
    
    // Determina a tela inicial baseado no estado de autenticação
    val startDestination = if (uiState.isAuthenticated) {
        // Se for barqueiro, verifica se tem embarcações
        if (uiState.user?.userType == UserType.BARQUEIRO) {
            if (hasCheckedBoats && boatUiState.boats.isEmpty()) {
                Screen.BoatRegistration.route
            } else {
                Screen.Map.route
            }
        } else {
            // Outros usuários vão direto para o mapa
            Screen.Map.route
        }
    } else {
        Screen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.BoatRegistration.route) {
            BoatRegistrationScreen(
                authViewModel = authViewModel,
                onBoatRegistered = {
                    // Após cadastrar barco, vai para o mapa
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.BoatRegistration.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.BoatList.route) {
            BoatListScreen(
                onBoatClick = { boat ->
                    // Navega para edição do barco
                    navController.navigate(Screen.BoatEdit.createRoute(boat.id))
                },
                onAddBoat = {
                    navController.navigate(Screen.BoatRegistration.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.BoatEdit.route) { backStackEntry ->
            val boatId = backStackEntry.arguments?.getString("boatId")
            if (boatId != null) {
                BoatRegistrationScreen(
                    authViewModel = authViewModel,
                    onBoatRegistered = {
                        navController.popBackStack()
                    }
                )
            }
        }
        
        composable(Screen.RouteSelection.route) {
            RouteSelectionScreen(
                onRouteSelected = { boat, route ->
                    // Navega para o mapa com a rota selecionada
                    // TODO: passar boat e route para iniciar rastreamento
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Map.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateBack = {
                    authViewModel.signOut()
                },
                onNavigateToBoatList = {
                    navController.navigate(Screen.BoatList.route)
                },
                onNavigateToRouteSelection = {
                    navController.navigate(Screen.RouteSelection.route)
                },
                authViewModel = authViewModel
            )
        }
    }
    
    // Observa mudanças no estado de autenticação e navega automaticamente
    LaunchedEffect(uiState.isAuthenticated, boatUiState.boats, hasCheckedBoats) {
        if (uiState.isAuthenticated) {
            val currentRoute = navController.currentDestination?.route
            
            // Não redirecionar se já estiver em uma tela autenticada
            if (currentRoute == Screen.Login.route || currentRoute == Screen.SignUp.route) {
                val destination = if (uiState.user?.userType == UserType.BARQUEIRO) {
                    // Se barqueiro não tem embarcações, vai para cadastro
                    if (hasCheckedBoats && boatUiState.boats.isEmpty()) {
                        Screen.BoatRegistration.route
                    } else {
                        Screen.Map.route
                    }
                } else {
                    Screen.Map.route
                }
                
                navController.navigate(destination) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (!uiState.isAuthenticated) {
            val currentRoute = navController.currentDestination?.route
            
            // Redirecionar para login se não estiver autenticado
            if (currentRoute != Screen.Login.route && currentRoute != Screen.SignUp.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}
