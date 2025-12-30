package com.example.arvision

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class EditProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val nameInput = findViewById<TextInputEditText>(R.id.edit_name_input)
        val passwordInput = findViewById<TextInputEditText>(R.id.edit_password_input)
        val confirmPasswordInput = findViewById<TextInputEditText>(R.id.edit_confirm_password_input)
        val saveButton = findViewById<Button>(R.id.save_changes_button)

        // Pre-fill the user's current name
        val currentUser = UserManager.getCurrentUser(this)
        nameInput.setText(currentUser?.name)

        saveButton.setOnClickListener {
            val newName = nameInput.text.toString().trim()
            val newPassword = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update name
            UserManager.updateName(this, newName)

            // Optionally update password
            if (newPassword.isNotEmpty()) {
                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                UserManager.updatePassword(this, newPassword)
            }

            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            finish() // Go back to ProfileFragment
        }
    }
}
