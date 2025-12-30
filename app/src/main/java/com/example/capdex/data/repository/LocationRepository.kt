package com.example.capdex.data.repository

import com.example.capdex.data.model.LocationData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LocationRepository {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    suspend fun saveLocation(locationData: LocationData): Result<Unit> {
        return try {
            // Salva na coleção "locations" com subcoleção por usuário
            firestore.collection("locations")
                .document(locationData.userId)
                .collection("tracks")
                .add(locationData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
