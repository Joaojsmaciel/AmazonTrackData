package com.example.capdex.data.repository

import com.example.capdex.data.model.AuthResult
import com.example.capdex.data.model.User
import com.example.capdex.data.model.UserType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AuthRepository {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        cpf: String,
        userType: UserType
    ): AuthResult<User> {
        return try {
            // Criar usuário no Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Falha ao criar usuário")
            
            // Criar documento do usuário no Firestore
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                fullName = fullName,
                cpf = cpf,
                userType = userType
            )
            
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()
            
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Erro desconhecido ao criar conta")
        }
    }
    
    suspend fun signIn(email: String, password: String): AuthResult<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Falha ao fazer login")
            
            // Buscar dados do usuário no Firestore
            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()
            
            val user = userDoc.toObject(User::class.java) 
                ?: throw Exception("Usuário não encontrado no banco de dados")
            
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Erro desconhecido ao fazer login")
        }
    }
    
    suspend fun getCurrentUserData(): User? {
        return try {
            val firebaseUser = currentUser ?: return null
            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()
            userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
}
