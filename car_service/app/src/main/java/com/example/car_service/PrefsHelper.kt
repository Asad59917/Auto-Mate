package com.example.car_service

import android.content.Context
import android.content.SharedPreferences

class PrefsHelper(context: Context) {
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

    private val locationPref: SharedPreferences =
        context.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)

    // Authentication methods
    fun saveUser(email: String, password: String, isLoggedIn: Boolean): Boolean {
        // Check if user already exists
        val existingEmail = sharedPref.getString("EMAIL", null)
        if (existingEmail != null && existingEmail.equals(email, ignoreCase = true)) {
            return false // User already exists
        }

        sharedPref.edit().apply {
            putString("EMAIL", email)
            putString("PASSWORD", password)
            putBoolean("IS_LOGGED_IN", isLoggedIn)
            apply()
        }
        return true
    }

    fun checkCredentials(email: String, password: String): Boolean {
        val savedEmail = sharedPref.getString("EMAIL", "")
        val savedPassword = sharedPref.getString("PASSWORD", "")
        return email.equals(savedEmail, ignoreCase = true) && password == savedPassword
    }

    fun isLoggedIn(): Boolean = sharedPref.getBoolean("IS_LOGGED_IN", false)

    fun getCurrentUserEmail(): String? {
        return sharedPref.getString("EMAIL", null)
    }

    fun clearUser() {
        // Only clear the login status, not the actual user data
        sharedPref.edit().putBoolean("IS_LOGGED_IN", false).apply()
    }

    // Location type and selected location management
    fun saveLocation(locationType: String, address: String) {
        val userEmail = getCurrentUserEmail() ?: return

        // Store the currently selected location type and address
        val locationTypeKey = "${userEmail}_SELECTED_LOCATION_TYPE"
        val selectedAddressKey = "${userEmail}_SELECTED_ADDRESS"

        locationPref.edit().apply {
            putString(locationTypeKey, locationType)
            putString(selectedAddressKey, address)
            apply()
        }

        // Also save address to appropriate location type storage
        when (locationType) {
            "Home" -> saveHomeLocation(address)
            "Work" -> saveWorkLocation(address)
            "Current" -> saveCurrentLocation(address)
        }
    }

    fun getLocationType(): String {
        val userEmail = getCurrentUserEmail() ?: return "Current"
        val locationTypeKey = "${userEmail}_SELECTED_LOCATION_TYPE"
        return locationPref.getString(locationTypeKey, "Current") ?: "Current"
    }

    fun getSelectedAddress(): String {
        val userEmail = getCurrentUserEmail() ?: return "Dubai, U.A.E"
        val selectedAddressKey = "${userEmail}_SELECTED_ADDRESS"

        // Get the currently selected address or fall back to location-type specific address
        val address = locationPref.getString(selectedAddressKey, null)
        if (!address.isNullOrEmpty()) {
            return address
        }

        // If no selected address is explicitly saved, get address based on selected type
        return when (getLocationType()) {
            "Home" -> getHomeLocation().ifEmpty { "Dubai, U.A.E" }
            "Work" -> getWorkLocation().ifEmpty { "Dubai, U.A.E" }
            else -> getCurrentLocation()
        }
    }

    // Home location methods
    fun saveHomeLocation(address: String) {
        val userEmail = getCurrentUserEmail() ?: return
        val homeAddressKey = "${userEmail}_HOME_ADDRESS"

        locationPref.edit().apply {
            putString(homeAddressKey, address)
            apply()
        }
    }

    fun getHomeLocation(): String {
        val userEmail = getCurrentUserEmail() ?: return ""
        val homeAddressKey = "${userEmail}_HOME_ADDRESS"
        return locationPref.getString(homeAddressKey, "") ?: ""
    }

    // Work location methods
    fun saveWorkLocation(address: String) {
        val userEmail = getCurrentUserEmail() ?: return
        val workAddressKey = "${userEmail}_WORK_ADDRESS"

        locationPref.edit().apply {
            putString(workAddressKey, address)
            apply()
        }
    }

    fun getWorkLocation(): String {
        val userEmail = getCurrentUserEmail() ?: return ""
        val workAddressKey = "${userEmail}_WORK_ADDRESS"
        return locationPref.getString(workAddressKey, "") ?: ""
    }

    // Current location methods
    fun saveCurrentLocation(address: String) {
        val userEmail = getCurrentUserEmail() ?: return
        val currentAddressKey = "${userEmail}_CURRENT_ADDRESS"

        locationPref.edit().apply {
            putString(currentAddressKey, address)
            apply()
        }
    }

    fun getCurrentLocation(): String {
        val userEmail = getCurrentUserEmail() ?: return "Dubai, U.A.E"
        val currentAddressKey = "${userEmail}_CURRENT_ADDRESS"
        return locationPref.getString(currentAddressKey, "Dubai, U.A.E") ?: "Dubai, U.A.E"
    }
}