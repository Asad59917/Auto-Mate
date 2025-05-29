package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.car_service.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsHelper = PrefsHelper(this)

        // Check if user can auto-login (more secure approach)
        if (prefsHelper.canAutoLogin()) {
            navigateToMainActivity()
            return
        }

        // Load remembered email if available
        loadRememberedEmail()

        binding.signInButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val rememberMe = binding.rememberMeSwitch.isChecked

            if (validateInputs(email, password)) {
                if (prefsHelper.loginUser(email, password)) {
                    // Handle remember me functionality
                    if (rememberMe) {
                        prefsHelper.saveRememberMe(email)
                    } else {
                        prefsHelper.clearRememberMe()
                    }

                    navigateToMainActivity()
                } else {
                    Toast.makeText(
                        this,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun loadRememberedEmail() {
        val rememberedEmail = prefsHelper.getRememberedEmail()
        if (rememberedEmail != null) {
            binding.emailInput.setText(rememberedEmail)
            binding.rememberMeSwitch.isChecked = true
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.emailInput.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInput.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }
}