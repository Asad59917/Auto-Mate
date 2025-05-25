package com.example.car_service

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var bottomNavigation: BottomNavigationView

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

        setupBottomNavigation()

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_notification -> {
                    // Create ServicesFragment or navigate to services
                    loadFragment(NotificationFragment())
                    true
                }
                R.id.nav_booking -> {
                    // Create BookingsFragment or navigate to bookings
                    loadFragment(BookingFragment())
                    true
                }
                R.id.nav_profile -> {
                    // Create ProfileFragment or navigate to profile
                     loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onBackPressed() {
        // Handle back press for fragments if needed
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (currentFragment is HomeFragment) {
            // If we're on home fragment, exit the app
            if (prefsHelper.isLoggedIn()) {
                super.onBackPressed()
            } else {
                finish()
            }
        } else {
            // If we're on other fragments, go back to home
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }
}