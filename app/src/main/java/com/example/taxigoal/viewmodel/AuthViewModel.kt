package com.example.taxigoal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.data.repository.AuthRepository
import com.example.taxigoal.utils.AppLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository(FirebaseAuth.getInstance())

    val authState: StateFlow<AuthState> = repository.observeAuthState()
        .map { user ->
            if (user != null) AuthState.Authenticated(user)
            else AuthState.Unauthenticated
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    fun signInAnonymously(onComplete: (Boolean) -> Unit) {
        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    fun signInWithGoogle(idToken: String, onComplete: (Boolean) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                AppLogger.info("Auth", "FIREBASE_AUTH_SUCCESS", "User: ${auth.currentUser?.email}")
                onComplete(true)
            } else {
                val error = task.exception?.message ?: "Unknown Firebase error"
                AppLogger.error("Auth", "FIREBASE_AUTH_FAILED", error)
                onComplete(false)
            }
        }
    }
}
