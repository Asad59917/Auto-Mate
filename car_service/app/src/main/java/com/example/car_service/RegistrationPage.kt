package com.example.car_service

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.car_service.databinding.ActivityRegistrationPageBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationPageBinding
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsHelper = PrefsHelper(this)

        binding.signUpButton.setOnClickListener {
            val fullName = binding.fullNameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()
            val confirmPassword = binding.confirmPasswordInput.text.toString().trim()

            if (validateInputs(fullName, email, password, confirmPassword)) {
                // Save user and show success dialog
                prefsHelper.saveUser(email, password, true)
                showSuccessDialog()
            }
        }

        binding.signInLink.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_success)

        // Set the dialog window properties for bottom sheet behavior
        val window = dialog.window
        window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.decorView.setPadding(0, 0, 0, 0)
            it.setGravity(Gravity.BOTTOM)

            // Add slide up animation if you have it defined
            try {
                it.setWindowAnimations(R.style.BottomSheetAnimation)
            } catch (e: Exception) {
                // If animation style doesn't exist, continue without it
            }
        }

        dialog.setCancelable(false)

        // Find and set up the login button
        val loginButton = dialog.findViewById<Button>(R.id.sign_in_button)
        loginButton?.setOnClickListener {
            dialog.dismiss()
            // Navigate to SignInActivity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        dialog.show()
    }

    private fun validateInputs(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        // Clear previous errors
        binding.fullNameInput.error = null
        binding.emailInput.error = null
        binding.passwordInput.error = null
        binding.confirmPasswordInput.error = null

        if (fullName.isEmpty()) {
            binding.fullNameInput.error = "Full name is required"
            isValid = false
        }

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

        if (confirmPassword != password) {
            binding.confirmPasswordInput.error = "Passwords don't match"
            isValid = false
        }

        return isValid
    }
}