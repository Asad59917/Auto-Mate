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
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Button
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
    }

    private fun loadUserData() {
        val userEmail = prefsHelper.getCurrentUserEmail()

        if (userEmail != null) {
            // Set email
            tvUserEmail.text = userEmail

            // Extract name from email (before @ symbol) and format it
            val nameFromEmail = userEmail.substringBefore("@")
            val formattedName = formatName(nameFromEmail)

            // Set name in both profile section and details
            tvProfileName.text = formattedName
            tvUserName.text = formattedName
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

        // Sign out click - show confirmation dialog
        cvSignOut.setOnClickListener {
            showSignOutDialog()
        }
    }

    private fun showSignOutDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_signout_confirmation)

        // Configure dialog window
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

        // Show dialog
        dialog.show()
    }

    private fun signOut() {
        prefsHelper.logout()

        // Navigate to login screen
        startActivity(Intent(requireContext(), SignInActivity::class.java))
        requireActivity().finish()
    }
}