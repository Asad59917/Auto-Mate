package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences helper
        prefsHelper = PrefsHelper(this)

        // Check if user is logged in, if not redirect to SignInActivity
        if (!prefsHelper.isLoggedIn()) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Original setup functions
        setupNavigation()
        setupSearchBar()
        setupLocationAndVehicles()
        setupServiceCards()
        setupSpecialOffers()
    }

    private fun setupNavigation() {
        // Top navigation
        findViewById<ImageButton>(R.id.menuButton).setOnClickListener {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.notificationButton).setOnClickListener {
            Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.locationTitle).setOnClickListener {
            Toast.makeText(this, "Location clicked", Toast.LENGTH_SHORT).show()
        }

        // Bottom navigation
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val homeNav = findViewById<View>(R.id.homeNavItem)
        val searchNav = findViewById<View>(R.id.searchNavItem)
        val locationNav = findViewById<View>(R.id.locationNavItem)
        val profileNav = findViewById<View>(R.id.profileNavItem)

        homeNav.setOnClickListener {
            // Already on home screen
            Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
        }

        searchNav.setOnClickListener {
            Toast.makeText(this, "Search clicked", Toast.LENGTH_SHORT).show()
            // Navigate to search screen
        }

        locationNav.setOnClickListener {
            Toast.makeText(this, "Location clicked", Toast.LENGTH_SHORT).show()
            // Navigate to location screen
        }

        profileNav.setOnClickListener {
            // Modified to handle logout
            showLogoutConfirmation()
        }

    }

    private fun showLogoutConfirmation() {
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
        prefsHelper.clearUser()
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun setupSearchBar() {
        val searchBar = findViewById<CardView>(R.id.searchBar)
        val filtersButton = findViewById<TextView>(R.id.filtersButton)

        searchBar.setOnClickListener {
            Toast.makeText(this, "Search bar clicked", Toast.LENGTH_SHORT).show()
            // Open search screen or show keyboard
        }

        filtersButton.setOnClickListener {
            Toast.makeText(this, "Search button clicked", Toast.LENGTH_SHORT).show()
            // Start search with current filters
        }
    }

    private fun setupLocationAndVehicles() {
        val locationButton = findViewById<CardView>(R.id.myLocationButton)
        val vehiclesButton = findViewById<CardView>(R.id.myVehiclesButton)

        locationButton.setOnClickListener {
            Toast.makeText(this, "My location clicked", Toast.LENGTH_SHORT).show()
            // Show location selector dialog
        }

        vehiclesButton.setOnClickListener {
            Toast.makeText(this, "My vehicles clicked", Toast.LENGTH_SHORT).show()
            // Show vehicles selector dialog
        }
    }

    private fun setupServiceCards() {
        // Get all service cards
        val serviceCards = listOf(
            findViewById<CardView>(R.id.serviceCard1),
            findViewById<CardView>(R.id.serviceCard2),
            findViewById<CardView>(R.id.serviceCard3),
            findViewById<CardView>(R.id.serviceCard4),
            findViewById<CardView>(R.id.serviceCard5),
            findViewById<CardView>(R.id.serviceCard6),
            findViewById<CardView>(R.id.serviceCard7),
            findViewById<CardView>(R.id.serviceCard8)
        )

        // Service types corresponding to the cards
        val serviceTypes = listOf(
            "Service", "Car Towing", "Brake Service", "Car Wash",
            "Fuel Up", "Tire Change", "Battery Change", "Service Contract"
        )

        // Set click listeners for each service card
        serviceCards.forEachIndexed { index, cardView ->
            cardView.setOnClickListener {
                val serviceName = serviceTypes[index]
                Toast.makeText(this, "$serviceName clicked", Toast.LENGTH_SHORT).show()
                // Open detail screen for the selected service
                navigateToService(serviceName)
            }
        }
    }

    private fun navigateToService(serviceName: String) {
        val intent = when (serviceName) {
            "Service" -> Intent(this, Servicepage::class.java)
            "Car Towing" -> Intent(this, cartowing::class.java)
            "Brake Service" -> Intent(this, BrakeService::class.java)
            "Car Wash" -> Intent(this, CarWashServicePage::class.java)
            "Tire Change" -> Intent(this, TireChangeServicePage::class.java)
            "Battery Change" -> Intent(this, BatteryChangeServicePage::class.java)
            "Service Contract" -> Intent(this, ServiceContractPage::class.java)
            else -> null
        }

        intent?.let { startActivity(it) }
    }

    private fun setupSpecialOffers() {
        val oilChangeOffer = findViewById<CardView>(R.id.oilChangeOffer)
        val carWashOffer = findViewById<CardView>(R.id.carWashOffer)

        oilChangeOffer.setOnClickListener {
            Toast.makeText(this, "Oil change offer clicked", Toast.LENGTH_SHORT).show()
            // Open oil change offer details
        }

        carWashOffer.setOnClickListener {
            Toast.makeText(this, "Car wash offer clicked", Toast.LENGTH_SHORT).show()
            // Open car wash offer details
        }
    }

    override fun onBackPressed() {
        // Optional: Prevent going back to auth screens if logged out
        if (prefsHelper.isLoggedIn()) {
            super.onBackPressed()
        } else {
            finish()
        }
    }
}