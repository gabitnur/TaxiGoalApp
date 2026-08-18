package com.example.taxigoal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var pbAuth: ProgressBar
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                setLoading(false)
                AppLogger.logError(this, "Google Sign In Failed", e)
                Toast.makeText(this, "Ошибка входа через Google", Toast.LENGTH_LONG).show()
            }
        } else {
            setLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        
        auth = FirebaseAuth.getInstance()
        setupGoogleSignIn()
        initViews()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            navigateToMain()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initViews() {
        pbAuth = findViewById(R.id.pbAuth)
        etEmail = findViewById(R.id.etEmail)
        etPass = findViewById(R.id.etPassword)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()
            if (email.isNotEmpty() && pass.isNotEmpty()) loginWithCredentials(email, pass)
            else Toast.makeText(this, "Введите Email и пароль", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()
            if (email.isNotEmpty() && pass.isNotEmpty()) registerWithCredentials(email, pass)
            else Toast.makeText(this, "Введите Email и пароль для регистрации", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnGoogleSignIn).setOnClickListener {
            setLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        findViewById<Button>(R.id.btnAnonymous).setOnClickListener {
            loginAnonymously()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    AppLogger.logInfo(this, "Google Sign In Successful: ${user?.email}")
                    navigateToMain()
                } else {
                    setLoading(false)
                    Toast.makeText(this, "Ошибка Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun loginAnonymously() {
        setLoading(true)
        auth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                AppLogger.logInfo(this, "Anonymous Sign In Successful: ${auth.currentUser?.uid}")
                navigateToMain()
            } else {
                setLoading(false)
                Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loginWithCredentials(email: String, pass: String) {
        setLoading(true)
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                AppLogger.logInfo(this, "Email Sign In Successful: $email")
                navigateToMain()
            } else {
                setLoading(false)
                Toast.makeText(this, "Ошибка входа: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registerWithCredentials(email: String, pass: String) {
        setLoading(true)
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) navigateToMain()
            else {
                setLoading(false)
                Toast.makeText(this, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() 
    }

    private fun setLoading(isLoading: Boolean) {
        pbAuth.visibility = if (isLoading) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnLogin).isEnabled = !isLoading
        findViewById<Button>(R.id.btnRegister).isEnabled = !isLoading
        findViewById<Button>(R.id.btnGoogleSignIn).isEnabled = !isLoading
    }
}
