package com.example.capdex.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.capdex.data.local.CachedLocationEntity
import com.example.capdex.data.local.LocationDatabase
import com.example.capdex.data.model.LocationData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LocationRepository(context: Context) {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val locationDao = LocationDatabase.getDatabase(context).locationDao()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    suspend fun saveLocation(locationData: LocationData): Result<Unit> {
        return try {
            if (isNetworkAvailable()) {
                // Tenta enviar diretamente ao Firebase
                firestore.collection("locations")
                    .document(locationData.userId)
                    .collection("tracks")
                    .add(locationData)
                    .await()
                
                // Sincroniza dados pendentes se houver
                syncPendingLocations()
                
                Result.success(Unit)
            } else {
                // Sem conexão: salva localmente
                val cachedLocation = CachedLocationEntity(
                    userId = locationData.userId,
                    latitude = locationData.latitude,
                    longitude = locationData.longitude,
                    speed = locationData.speed,
                    altitude = locationData.altitude,
                    accuracy = locationData.accuracy,
                    bearing = locationData.bearing,
                    timestamp = locationData.timestamp,
                    userType = locationData.userType,
                    userName = locationData.userName,
                    synced = false
                )
                locationDao.insert(cachedLocation)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // Em caso de erro, salva localmente
            try {
                val cachedLocation = CachedLocationEntity(
                    userId = locationData.userId,
                    latitude = locationData.latitude,
                    longitude = locationData.longitude,
                    speed = locationData.speed,
                    altitude = locationData.altitude,
                    accuracy = locationData.accuracy,
                    bearing = locationData.bearing,
                    timestamp = locationData.timestamp,
                    userType = locationData.userType,
                    userName = locationData.userName,
                    synced = false
                )
                locationDao.insert(cachedLocation)
                Result.success(Unit)
            } catch (localError: Exception) {
                Result.failure(localError)
            }
        }
    }
    
    suspend fun syncPendingLocations(): Int {
        if (!isNetworkAvailable()) return 0
        
        var syncedCount = 0
        try {
            val unsyncedLocations = locationDao.getUnsyncedLocations()
            
            for (cached in unsyncedLocations) {
                try {
                    val locationData = LocationData(
                        userId = cached.userId,
                        latitude = cached.latitude,
                        longitude = cached.longitude,
                        speed = cached.speed,
                        altitude = cached.altitude,
                        accuracy = cached.accuracy,
                        bearing = cached.bearing,
                        timestamp = cached.timestamp,
                        userType = cached.userType,
                        userName = cached.userName
                    )
                    
                    firestore.collection("locations")
                        .document(cached.userId)
                        .collection("tracks")
                        .add(locationData)
                        .await()
                    
                    locationDao.markAsSynced(cached.id)
                    syncedCount++
                } catch (e: Exception) {
                    // Se falhar em um, continua tentando os próximos
                    continue
                }
            }
            
            // Remove dados sincronizados com mais de 7 dias
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            locationDao.deleteSyncedOlderThan(sevenDaysAgo)
            
        } catch (e: Exception) {
            // Erro ao sincronizar
        }
        
        return syncedCount
    }
    
    suspend fun getUnsyncedCount(): Int {
        return try {
            locationDao.getUnsyncedCount()
        } catch (e: Exception) {
            0
        }
    }
    
    suspend fun getRecentLocations(userId: String, limit: Int = 100): Result<List<LocationData>> {
        return try {
            val snapshot = firestore.collection("locations")
                .document(userId)
                .collection("tracks")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val locations = snapshot.documents.mapNotNull { 
                it.toObject(LocationData::class.java) 
            }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
