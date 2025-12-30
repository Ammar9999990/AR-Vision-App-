package com.example.arvision

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val emailInput = findViewById<TextInputEditText>(R.id.forgot_email_input)
        val newPasswordInput = findViewById<TextInputEditText>(R.id.forgot_new_password_input)
        val confirmPasswordInput = findViewById<TextInputEditText>(R.id.forgot_confirm_password_input)
        val resetButton = findViewById<Button>(R.id.reset_password_button)

        resetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (email.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (UserManager.updatePasswordByEmail(this, email, newPassword)) {
                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                finish() // Go back to LoginActivity
            } else {
                Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
