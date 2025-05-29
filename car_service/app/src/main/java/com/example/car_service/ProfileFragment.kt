package com.example.car_service

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class ProfileFragment : Fragment() {

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var tvProfileName: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var backButton: ImageButton
    private lateinit var cvSignOut: CardView
    private lateinit var cvTermsConditions: CardView
    private lateinit var cvChangeName: CardView
    private lateinit var cvChangePassword: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize PrefsHelper
        prefsHelper = PrefsHelper(requireContext())

        // Initialize views
        initViews(view)

        // Load user data
        loadUserData()

        // Set click listeners
        setClickListeners()
    }

    private fun initViews(view: View) {
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        backButton = view.findViewById(R.id.backButton)
        cvSignOut = view.findViewById(R.id.cvSignOut)
        cvTermsConditions = view.findViewById(R.id.cvTermsConditions)
        cvChangeName = view.findViewById(R.id.cvChangeName)
        cvChangePassword = view.findViewById(R.id.cvChangePassword)
    }

    private fun loadUserData() {
        val userEmail = prefsHelper.getCurrentUserEmail()
        val userName = prefsHelper.getCurrentUserName()

        if (userEmail != null) {
            // Set email
            tvUserEmail.text = userEmail

            // Use actual name if available, otherwise fallback to formatted email
            val displayName = if (!userName.isNullOrEmpty()) {
                userName
            } else {
                // Fallback to formatted email for existing users who don't have fullName
                formatName(userEmail.substringBefore("@"))
            }

            // Set name in both profile section and details
            tvProfileName.text = displayName
            tvUserName.text = displayName
        } else {
            // Fallback if no user is logged in
            tvProfileName.text = "Guest User"
            tvUserName.text = "Guest User"
            tvUserEmail.text = "No email available"
        }
    }

    private fun formatName(nameFromEmail: String): String {
        // Remove numbers and special characters, replace dots/underscores with spaces
        val cleanName = nameFromEmail
            .replace(Regex("[0-9]"), "")
            .replace(".", " ")
            .replace("_", " ")
            .replace("-", " ")

        // Capitalize each word
        return cleanName.split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }
            .ifEmpty { "User" }
    }

    private fun setClickListeners() {
        // Back button click
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Terms & Conditions click
        cvTermsConditions.setOnClickListener {
            startActivity(Intent(requireContext(), TermsConditionsActivity::class.java))
        }

        // Change Name click
        cvChangeName.setOnClickListener {
            showChangeNameDialog()
        }

        // Change Password click
        cvChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Sign out click - show confirmation dialog
        cvSignOut.setOnClickListener {
            showSignOutDialog()
        }
    }

    private fun showChangeNameDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_change_name)

        // Configure dialog window
        configureDialogWindow(dialog)

        // Find views in dialog
        val etNewName = dialog.findViewById<EditText>(R.id.etNewName)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        // Pre-fill current name
        val currentName = prefsHelper.getCurrentUserName()
        if (!currentName.isNullOrEmpty()) {
            etNewName.setText(currentName)
            etNewName.setSelection(currentName.length)
        }

        // Set click listeners
        btnSave.setOnClickListener {
            val newName = etNewName.text.toString().trim()
            if (newName.isNotEmpty()) {
                if (prefsHelper.updateUserName(newName)) {
                    Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT).show()
                    loadUserData() // Refresh the displayed data
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a valid name", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showChangePasswordDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_change_password)

        // Configure dialog window
        configureDialogWindow(dialog)

        // Find views in dialog
        val etCurrentPassword = dialog.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialog.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialog.findViewById<EditText>(R.id.etConfirmPassword)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        // Set click listeners
        btnSave.setOnClickListener {
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            when {
                currentPassword.isEmpty() -> {
                    Toast.makeText(requireContext(), "Please enter your current password", Toast.LENGTH_SHORT).show()
                }
                newPassword.isEmpty() -> {
                    Toast.makeText(requireContext(), "Please enter a new password", Toast.LENGTH_SHORT).show()
                }
                newPassword.length < 6 -> {
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val result = prefsHelper.updateUserPassword(currentPassword, newPassword)
                    when (result) {
                        PrefsHelper.PasswordUpdateResult.SUCCESS -> {
                            Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        PrefsHelper.PasswordUpdateResult.WRONG_CURRENT_PASSWORD -> {
                            Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show()
                        }
                        PrefsHelper.PasswordUpdateResult.ERROR -> {
                            Toast.makeText(requireContext(), "Failed to update password", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSignOutDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_signout_confirmation)

        // Configure dialog window
        configureDialogWindow(dialog)

        // Find views in dialog
        val btnYes = dialog.findViewById<Button>(R.id.btnYes)
        val btnNo = dialog.findViewById<Button>(R.id.btnNo)

        // Set click listeners
        btnYes.setOnClickListener {
            dialog.dismiss()
            signOut()
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun configureDialogWindow(dialog: Dialog) {
        val window = dialog.window
        window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setGravity(Gravity.BOTTOM)
            it.setWindowAnimations(R.style.DialogSlideAnimation)

            // Set background to transparent for rounded corners
            it.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.transparent))

            // Add margin from edges
            val layoutParams = it.attributes
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            it.attributes = layoutParams
        }
    }

    private fun signOut() {
        prefsHelper.logout()

        // Navigate to login screen
        startActivity(Intent(requireContext(), SignInActivity::class.java))
        requireActivity().finish()
    }
}