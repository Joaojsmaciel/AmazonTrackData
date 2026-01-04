package com.example.capdex.data.repository

import android.util.Log
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
import kotlinx.coroutines.withTimeout

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
            // Timeout de 15 segundos
            withTimeout(15000L) {
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
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            AuthResult.Error("Tempo esgotado. Verifique sua conexão com a internet.")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Erro desconhecido ao criar conta")
        }
    }
    
    suspend fun signIn(email: String, password: String): AuthResult<User> {
        return try {
            Log.d("AuthRepository", "signIn: Attempting login with email: $email")
            
            // Timeout de 15 segundos para evitar loading infinito
            withTimeout(15000L) {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user ?: throw Exception("Falha ao fazer login")
                
                Log.d("AuthRepository", "signIn: Firebase auth successful, uid: ${firebaseUser.uid}")
                
                // Buscar dados do usuário no Firestore
                val userDoc = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .get()
                    .await()
                
                Log.d("AuthRepository", "signIn: Firestore query completed, exists: ${userDoc.exists()}")
                
                val user = userDoc.toObject(User::class.java) 
                    ?: throw Exception("Usuário não encontrado no banco de dados")
                
                Log.d("AuthRepository", "signIn: User data retrieved: ${user.email}")
                AuthResult.Success(user)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("AuthRepository", "signIn: Timeout - conexão muito lenta ou sem internet")
            AuthResult.Error("Tempo esgotado. Verifique sua conexão com a internet.")
        } catch (e: Exception) {
            Log.e("AuthRepository", "signIn: Error occurred", e)
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
