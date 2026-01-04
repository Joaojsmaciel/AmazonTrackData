package com.example.capdex.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import com.example.capdex.data.model.User
import com.example.capdex.service.LocationTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackingUiState(
    val isTracking: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false
)

class LocationViewModel : ViewModel() {
    
    private val _trackingState = MutableStateFlow(TrackingUiState())
    val trackingState: StateFlow<TrackingUiState> = _trackingState.asStateFlow()
    
    fun updateLocationPermission(granted: Boolean) {
        _trackingState.value = _trackingState.value.copy(hasLocationPermission = granted)
    }
    
    fun updateNotificationPermission(granted: Boolean) {
        _trackingState.value = _trackingState.value.copy(hasNotificationPermission = granted)
    }
    
    fun startTracking(context: Context, user: User) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRACKING
            putExtra(LocationTrackingService.EXTRA_USER_ID, user.uid)
            putExtra(LocationTrackingService.EXTRA_USER_NAME, user.fullName)
            putExtra(LocationTrackingService.EXTRA_USER_TYPE, user.userType.displayName)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        
        _trackingState.value = _trackingState.value.copy(isTracking = true)
    }
    
    fun stopTracking(context: Context) {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        context.stopService(intent)
        
        _trackingState.value = _trackingState.value.copy(isTracking = false)
    }
}
