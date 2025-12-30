package com.example.arvision

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth

        val emailInput = findViewById<TextInputEditText>(R.id.login_email_input)
        val passwordInput = findViewById<TextInputEditText>(R.id.login_password_input)
        val loginButton = findViewById<Button>(R.id.login_button)
        val signupLink = findViewById<TextView>(R.id.login_signup_link)
        val forgotPasswordLink = findViewById<TextView>(R.id.login_forgot_password_link)

        // Pre-fill email from signup
        intent.getStringExtra("EMAIL")?.let {
            emailInput.setText(it)
        }

        // Firebase Login
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim().lowercase()
            val password = passwordInput.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, sync history, and update UI
                        val user = auth.currentUser
                        DatabaseManager.syncHistoryOnLogin(this)
                        navigateToMainApp(user?.displayName ?: user?.email)
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                            baseContext,
                            "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }

        signupLink.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }
        forgotPasswordLink.setOnClickListener { startActivity(Intent(this, ForgotPasswordActivity::class.java)) }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            DatabaseManager.syncHistoryOnLogin(this)
            navigateToMainApp(currentUser.displayName ?: currentUser.email)
        }
    }

    private fun navigateToMainApp(userName: String?) {
        val welcomeMessage = if (!userName.isNullOrEmpty()) "Welcome back, $userName!" else "Welcome!"
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Prevent user from coming back to the login screen
    }
}
