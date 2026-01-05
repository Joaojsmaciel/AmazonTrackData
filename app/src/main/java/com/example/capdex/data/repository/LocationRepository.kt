package com.example.capdex.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.capdex.data.local.CachedLocationEntity
import com.example.capdex.data.local.LocationDatabase
import com.example.capdex.data.model.LocationData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

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
        val hasInternet = isNetworkAvailable()
        Log.d("LocationRepository", "🌐 Internet disponível: $hasInternet")
        
        return try {
            if (hasInternet) {
                Log.d("LocationRepository", "📤 Tentando enviar para Firebase...")
                Log.d("LocationRepository", "   UserId: ${locationData.userId}")
                Log.d("LocationRepository", "   TripId: ${locationData.tripId}")
                
                // Tenta enviar com timeout de 10 segundos
                withTimeout(10000L) {
                    firestore.collection("locations")
                        .document(locationData.userId)
                        .collection("tracks")
                        .add(locationData)
                        .await()
                }
                
                Log.d("LocationRepository", "✅ SUCESSO! Enviado para Firebase! TripId: ${locationData.tripId}")
                
                // Sincroniza dados pendentes se houver
                try {
                    val synced = syncPendingLocations()
                    if (synced > 0) {
                        Log.d("LocationRepository", "📤 Sincronizados $synced dados pendentes")
                    }
                } catch (e: Exception) {
                    Log.w("LocationRepository", "⚠️ Erro ao sincronizar pendentes: ${e.message}")
                }
                
                Result.success(Unit)
            } else {
                Log.w("LocationRepository", "📵 Sem internet - Salvando localmente")
                // Sem conexão: salva localmente
                val cachedLocation = CachedLocationEntity(
                    userId = locationData.userId,
                    tripId = locationData.tripId,
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
                Log.d("LocationRepository", "💾 Salvo localmente no Room")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("LocationRepository", "❌ Erro ao enviar para Firebase: ${e.message}", e)
            // Em caso de erro, salva localmente
            try {
                val cachedLocation = CachedLocationEntity(
                    userId = locationData.userId,
                    tripId = locationData.tripId,
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
                Log.d("LocationRepository", "💾 Salvo localmente após erro")
                Result.success(Unit)
            } catch (localError: Exception) {
                Log.e("LocationRepository", "❌❌ Erro crítico ao salvar localmente: ${localError.message}", localError)
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
                        tripId = cached.tripId,
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
