package com.example.capdex.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.capdex.MainActivity
import com.example.capdex.R
import com.example.capdex.data.model.LocationData
import com.example.capdex.data.repository.LocationRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val locationRepository = LocationRepository()
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    
    private var userId: String = ""
    private var userName: String = ""
    private var userType: String = ""
    
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userId = intent?.getStringExtra(EXTRA_USER_ID) ?: ""
        userName = intent?.getStringExtra(EXTRA_USER_NAME) ?: ""
        userType = intent?.getStringExtra(EXTRA_USER_TYPE) ?: ""
        
        when (intent?.action) {
            ACTION_START_TRACKING -> startLocationTracking()
            ACTION_STOP_TRACKING -> stopLocationTracking()
        }
        
        return START_STICKY
    }
    
    private fun startLocationTracking() {
        val notification = createNotification(
            "Rastreamento ativo",
            "Sua localização está sendo monitorada"
        )
        startForeground(NOTIFICATION_ID, notification)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationData = LocationData(
                        userId = userId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speed = location.speed,
                        altitude = location.altitude,
                        accuracy = location.accuracy,
                        bearing = location.bearing,
                        timestamp = System.currentTimeMillis(),
                        userType = userType,
                        userName = userName
                    )
                    
                    // Salva no Firebase
                    serviceScope.launch {
                        locationRepository.saveLocation(locationData)
                    }
                    
                    // Atualiza notificação com velocidade
                    updateNotification(locationData)
                }
            }
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Atualiza a cada 5 segundos
        ).apply {
            setMinUpdateIntervalMillis(3000L)
            setMaxUpdateDelayMillis(10000L)
        }.build()
        
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }
    
    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun createNotification(title: String, content: String): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Parar",
                stopPendingIntent
            )
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(locationData: LocationData) {
        val speedKmh = locationData.speed * 3.6f // Converte m/s para km/h
        val notification = createNotification(
            "Rastreamento ativo",
            "Velocidade: %.1f km/h • Precisão: %.0fm".format(speedKmh, locationData.accuracy)
        )
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreamento de Localização",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente durante o rastreamento"
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    
    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_USER_NAME = "EXTRA_USER_NAME"
        const val EXTRA_USER_TYPE = "EXTRA_USER_TYPE"
    }
}
