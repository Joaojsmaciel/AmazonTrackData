package com.example.capdex.data.repository

import android.net.Uri
import android.util.Log
import com.example.capdex.data.model.Boat
import com.example.capdex.data.model.Route
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class BoatRepository {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    
    private val boatsCollection = firestore.collection("boats")
    private val storageRef = storage.reference.child("boat_images")
    
    /**
     * Cria ou atualiza uma embarcação no Firestore
     */
    suspend fun saveBoat(boat: Boat): Result<Boat> {
        return try {
            withTimeout(15000L) {
                val currentUser = auth.currentUser 
                    ?: throw Exception("Usuário não autenticado")
                
                Log.d("BoatRepository", "saveBoat: User ${currentUser.uid} salvando boat: ${boat.name}")
                
                val boatWithOwner = if (boat.id.isEmpty()) {
                    // Novo barco - gera ID e adiciona dono
                    val docRef = boatsCollection.document()
                    Log.d("BoatRepository", "saveBoat: Novo barco com ID: ${docRef.id}")
                    boat.copy(
                        id = docRef.id,
                        ownerId = currentUser.uid
                    )
                } else {
                    // Atualização - mantém dados existentes
                    Log.d("BoatRepository", "saveBoat: Atualizando barco existente: ${boat.id}")
                    boat
                }
                
                Log.d("BoatRepository", "saveBoat: Salvando no Firestore - Boat: id=${boatWithOwner.id}, name=${boatWithOwner.name}, ownerId=${boatWithOwner.ownerId}, photoUrl=${boatWithOwner.photoUrl}, isActive=${boatWithOwner.isActive}")
                
                // Converte para Map para garantir que todos os campos são salvos
                val boatMap = hashMapOf(
                    "id" to boatWithOwner.id,
                    "ownerId" to boatWithOwner.ownerId,
                    "ownerName" to boatWithOwner.ownerName,
                    "name" to boatWithOwner.name,
                    "photoUrl" to boatWithOwner.photoUrl,
                    "type" to boatWithOwner.type,
                    "capacity" to boatWithOwner.capacity,
                    "routes" to boatWithOwner.routes.map { route ->
                        hashMapOf(
                            "origin" to route.origin,
                            "destination" to route.destination,
                            "stops" to route.stops.map { stop ->
                                hashMapOf(
                                    "name" to stop.name,
                                    "latitude" to stop.latitude,
                                    "longitude" to stop.longitude,
                                    "radius" to stop.radius
                                )
                            },
                            "estimatedDuration" to route.estimatedDuration
                        )
                    },
                    "createdAt" to boatWithOwner.createdAt,
                    "isActive" to boatWithOwner.isActive
                )
                
                Log.d("BoatRepository", "saveBoat: BoatMap = $boatMap")
                
                boatsCollection.document(boatWithOwner.id)
                    .set(boatMap)
                    .await()
                
                Log.d("BoatRepository", "saveBoat: Boat saved successfully: ${boatWithOwner.id}")
                Result.success(boatWithOwner)
            }
        } catch (e: Exception) {
            Log.e("BoatRepository", "saveBoat: Error saving boat", e)
            Result.failure(e)
        }
    }
    
    /**
     * Faz upload da foto do barco para Firebase Storage
     */
    suspend fun uploadBoatPhoto(boatId: String, imageUri: Uri): Result<String> {
        return try {
            withTimeout(30000L) { // 30 segundos para upload de imagem
                val fileName = "boat_${boatId}_${System.currentTimeMillis()}.jpg"
                val imageRef = storageRef.child(fileName)
                
                // Upload da imagem
                imageRef.putFile(imageUri).await()
                
                // Obter URL de download
                val downloadUrl = imageRef.downloadUrl.await().toString()
                
                Log.d("BoatRepository", "Photo uploaded successfully: $downloadUrl")
                Result.success(downloadUrl)
            }
        } catch (e: Exception) {
            Log.e("BoatRepository", "Error uploading photo", e)
            Result.failure(e)
        }
    }
    
    /**
     * Busca todas as embarcações do usuário logado
     */
    suspend fun getUserBoats(): Result<List<Boat>> {
        return try {
            withTimeout(10000L) {
                val currentUser = auth.currentUser 
                    ?: throw Exception("Usuário não autenticado")
                
                Log.d("BoatRepository", "getUserBoats: Buscando barcos para userId: ${currentUser.uid}")
                
                val snapshot = boatsCollection
                    .whereEqualTo("ownerId", currentUser.uid)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                
                Log.d("BoatRepository", "getUserBoats: Query retornou ${snapshot.size()} documentos")
                
                val boats = snapshot.documents.mapNotNull { doc ->
                    Log.d("BoatRepository", "getUserBoats: Documento encontrado - id: ${doc.id}, data: ${doc.data}")
                    try {
                        val boat = doc.toObject(Boat::class.java)
                        Log.d("BoatRepository", "getUserBoats: Barco convertido: ${boat?.name}")
                        boat
                    } catch (e: Exception) {
                        Log.e("BoatRepository", "getUserBoats: Erro ao converter documento ${doc.id}", e)
                        null
                    }
                }
                
                Log.d("BoatRepository", "getUserBoats: Total de ${boats.size} barcos convertidos com sucesso")
                
                boats.forEach { boat ->
                    Log.d("BoatRepository", "getUserBoats: - ${boat.name} (${boat.type}), rotas: ${boat.routes.size}")
                }
                
                Log.d("BoatRepository", "Found ${boats.size} boats for user")
                Result.success(boats)
            }
        } catch (e: Exception) {
            Log.e("BoatRepository", "Error getting user boats", e)
            Result.failure(e)
        }
    }
    
    /**
     * Busca uma embarcação específica por ID
     */
    suspend fun getBoatById(boatId: String): Result<Boat> {
        return try {
            withTimeout(10000L) {
                val snapshot = boatsCollection
                    .document(boatId)
                    .get()
                    .await()
                
                val boat = snapshot.toObject(Boat::class.java)
                    ?: throw Exception("Barco não encontrado")
                
                Log.d("BoatRepository", "Boat found: ${boat.name}")
                Result.success(boat)
            }
        } catch (e: Exception) {
            Log.e("BoatRepository", "Error getting boat", e)
            Result.failure(e)
        }
    }
    
    /**
     * Adiciona uma rota a uma embarcação
     */
    suspend fun addRouteToBoat(boatId: String, route: Route): Result<Boat> {
        return try {
            withTimeout(10000L) {
                // Buscar barco atual
                val boatResult = getBoatById(boatId)
                if (boatResult.isFailure) {
                    throw boatResult.exceptionOrNull() ?: Exception("Erro ao buscar barco")
                }
                
                val boat = boatResult.getOrThrow()
                val routeWithId = if (route.id.isEmpty()) {
                    route.copy(id = "${boatId}_route_${System.currentTimeMillis()}")
                } else {
                    route
                }
                
                // Adicionar nova rota
                val updatedRoutes = boat.routes + routeWithId
                val updatedBoat = boat.copy(routes = updatedRoutes)
                
                // Salvar
                boatsCollection.document(boatId)
                    .set(updatedBoat)
                    .await()
                
                Log.d("BoatRepository", "Route added to boat: ${route.name}")
                Result.success(updatedBoat)
            }
        } catch (e: Exception) {
            Log.e("BoatRepository", "Error adding route", e)
            Result.failure(e)
        }
    }
    
    /**
     * Remove uma rota de uma embarcação
     */
    suspend fun removeRouteFromBoat(boatId: String, routeId: String): Result<Boat> {
        return try {
            withTimeout(10000L) {
                val boatResult = getBoatById(boatId)
                if (boatResult.isFailure) {
                    throw boatResult.exceptionOrNull() ?: Exception("Erro ao buscar barco")
                }
                
                val boat = boatResult.getOrThrow()
                val updatedRoutes = boat.routes.filter { it.id != routeId }
                val updatedBoat = boat.copy(routes = updatedRoutes)
                
                boatsCollection.document(boatId)
                    .set(updatedBoat)
                    .await()
                
                Log.d("BoatRepository", "Route removed from boat")
                Result.success(updatedBoat)
            }
        } catch (e: Exception) {
            Log.e("BoatRepository", "Error removing route", e)
            Result.failure(e)
        }
    }
    
    /**
     * Desativa uma embarcação (soft delete)
     */
    suspend fun deactivateBoat(boatId: String): Result<Unit> {
        return try {
            withTimeout(10000L) {
                boatsCollection.document(boatId)
                    .update("isActive", false)
                    .await()
                
                Log.d("BoatRepository", "Boat deactivated")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("BoatRepository", "Error deactivating boat", e)
            Result.failure(e)
        }
    }
    
    /**
     * Observa mudanças nas embarcações do usuário em tempo real
     */
    fun observeUserBoats(): Flow<List<Boat>> = flow {
        val currentUser = auth.currentUser ?: throw Exception("Usuário não autenticado")
        
        boatsCollection
            .whereEqualTo("ownerId", currentUser.uid)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BoatRepository", "Error observing boats", error)
                    return@addSnapshotListener
                }
                
                val boats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Boat::class.java)
                } ?: emptyList()
                
                // Emitir novos dados
            }
    }
}
