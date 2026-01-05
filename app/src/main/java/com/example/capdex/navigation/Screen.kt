package com.example.capdex.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object BoatRegistration : Screen("boat_registration")
    data object BoatList : Screen("boat_list")
    data object BoatEdit : Screen("boat_edit/{boatId}") {
        fun createRoute(boatId: String) = "boat_edit/$boatId"
    }
    data object RouteSelection : Screen("route_selection")
    data object Map : Screen("map")
}
