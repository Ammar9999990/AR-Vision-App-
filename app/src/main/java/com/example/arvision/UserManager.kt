package com.example.arvision

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

// A simple local user manager. In a real app, this would be a remote database.
data class User(val name: String, val email: String)

object UserManager {

    private const val PREFS_NAME = "user_prefs"

    // Using a simple convention for storing registered users: key = "user_email@example.com", value = "password,FullName"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveRegistration(context: Context, name: String, email: String, password: String) {
        val editor = getPreferences(context).edit()
        // In a real app, HASH THE PASSWORD! For this simulation, we store it as is.
        editor.putString(email.lowercase(), "$password,$name")
        editor.apply()
    }

    fun validateUser(context: Context, email: String, password: String): String? {
        val prefs = getPreferences(context)
        val userData = prefs.getString(email.lowercase(), null)?.split(",")
        if (userData != null && userData.size == 2) {
            val savedPassword = userData[0]
            val savedName = userData[1]
            if (savedPassword == password) {
                return savedName
            }
        }
        return null // Return null if validation fails
    }

    // This function is now only for the non-Firebase email/password flow.
    fun setCurrentUser(context: Context, name: String, email: String) {
        val editor = getPreferences(context).edit()
        editor.putString("current_name", name)
        editor.putString("current_email", email)
        editor.apply()
    }

    fun getCurrentUser(context: Context): User? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            return User(firebaseUser.displayName ?: "User", firebaseUser.email ?: "")
        } else {
            // Fallback for non-Firebase email/password users
            val prefs = getPreferences(context)
            val name = prefs.getString("current_name", null)
            val email = prefs.getString("current_email", null)
            return if (name != null && email != null) {
                User(name, email)
            } else {
                null
            }
        }
    }

    fun updateName(context: Context, newName: String) {
        val currentUser = getCurrentUser(context)
        currentUser?.let {
            // Update the master registration record for the local user
            val prefs = getPreferences(context)
            val userData = prefs.getString(it.email.lowercase(), null)?.split(",")
            if (userData != null && userData.size == 2) {
                val password = userData[0]
                saveRegistration(context, newName, it.email, password)
            }
        }
    }

    // For the logged-in user (in EditProfileActivity)
    fun updatePassword(context: Context, newPassword: String) {
        val currentUser = getCurrentUser(context)
        currentUser?.let {
            saveRegistration(context, it.name, it.email, newPassword)
        }
    }

    // For the logged-out user (in ForgotPasswordActivity)
    fun updatePasswordByEmail(context: Context, email: String, newPassword: String): Boolean {
        val prefs = getPreferences(context)
        val lowercasedEmail = email.lowercase()
        if (!prefs.contains(lowercasedEmail)) {
            return false // User not found
        }

        val userData = prefs.getString(lowercasedEmail, null)?.split(",")
        return if (userData != null && userData.size == 2) {
            val name = userData[1]
            saveRegistration(context, name, lowercasedEmail, newPassword)
            true
        } else {
            false
        }
    }

    fun logout(context: Context) {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Clear local non-Firebase user data
        val editor = getPreferences(context).edit()
        editor.remove("current_name")
        editor.remove("current_email")
        editor.apply()
    }
}
