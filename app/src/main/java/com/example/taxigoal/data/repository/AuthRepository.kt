package com.example.taxigoal.data.repository

import com.example.taxigoal.utils.AppLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class AccountType {
    GOOGLE,
    GUEST
}

class AuthRepository(private val auth: FirebaseAuth) {

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            val user = it.currentUser
            AppLogger.info("AUTH", "STATE_CHANGED", if (user != null) "User authenticated: ${user.uid.take(5)}..." else "User signed out")
            trySend(user)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getAccountType(): AccountType {
        val user = auth.currentUser
        return if (user?.isAnonymous == true) AccountType.GUEST else AccountType.GOOGLE
    }

    fun getUserId(): String? = auth.currentUser?.uid

    fun getUserName(): String {
        val user = auth.currentUser ?: return "Пользователь"
        if (user.isAnonymous) return "Гость"
        
        val displayName = user.displayName
        if (!displayName.isNullOrBlank()) return displayName
        
        val email = user.email
        if (!email.isNullOrBlank()) {
            return email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
        
        return "Пользователь"
    }

    fun signOut() {
        AppLogger.info("AUTH", "SIGN_OUT", "User requested sign out")
        auth.signOut()
    }
}
