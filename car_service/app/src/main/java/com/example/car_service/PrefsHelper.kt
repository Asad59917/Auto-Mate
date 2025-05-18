package com.example.car_service

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class PrefsHelper(context: Context) {
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

    private val locationPref: SharedPreferences =
        context.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)

    private val vehiclePref: SharedPreferences =
        context.getSharedPreferences("VehiclePrefs", Context.MODE_PRIVATE)

    // Authentication methods
    fun registerUser(email: String, password: String): Boolean {
        val existingEmail = sharedPref.getString("EMAIL", null)
        if (existingEmail != null && existingEmail.equals(email, ignoreCase = true)) {
            return false // User already exists
        }

        sharedPref.edit().apply {
            putString("EMAIL", email)
            putString("PASSWORD", hashPassword(password))
            putBoolean("IS_LOGGED_IN", false)
            apply()
        }
        return true
    }

    fun loginUser(email: String, password: String): Boolean {
        val savedEmail = sharedPref.getString("EMAIL", "")
        val savedPassword = sharedPref.getString("PASSWORD", "")

        return if (email.equals(savedEmail, ignoreCase = true) &&
            hashPassword(password) == savedPassword) {
            sharedPref.edit().putBoolean("IS_LOGGED_IN", true).apply()
            true
        } else {
            false
        }
    }

    fun isLoggedIn(): Boolean = sharedPref.getBoolean("IS_LOGGED_IN", false)

    fun getCurrentUserEmail(): String? = sharedPref.getString("EMAIL", null)

    fun logout() {
        sharedPref.edit().putBoolean("IS_LOGGED_IN", false).apply()
    }

    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            password // Fallback to plain text if hashing fails
        }
    }

    // Location methods
    fun saveLocation(locationType: String, address: String) {
        val userEmail = getCurrentUserEmail() ?: return
        val locationTypeKey = "${userEmail}_SELECTED_LOCATION_TYPE"
        val selectedAddressKey = "${userEmail}_SELECTED_ADDRESS"

        locationPref.edit().apply {
            putString(locationTypeKey, locationType)
            putString(selectedAddressKey, address)
            apply()
        }

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
        val address = locationPref.getString(selectedAddressKey, null)

        return address ?: when (getLocationType()) {
            "Home" -> getHomeLocation().ifEmpty { "Dubai, U.A.E" }
            "Work" -> getWorkLocation().ifEmpty { "Dubai, U.A.E" }
            else -> getCurrentLocation()
        }
    }

    fun saveHomeLocation(address: String) {
        val userEmail = getCurrentUserEmail() ?: return
        locationPref.edit().putString("${userEmail}_HOME_ADDRESS", address).apply()
    }

    fun getHomeLocation(): String {
        val userEmail = getCurrentUserEmail() ?: return ""
        return locationPref.getString("${userEmail}_HOME_ADDRESS", "") ?: ""
    }

    fun saveWorkLocation(address: String) {
        val userEmail = getCurrentUserEmail() ?: return
        locationPref.edit().putString("${userEmail}_WORK_ADDRESS", address).apply()
    }

    fun getWorkLocation(): String {
        val userEmail = getCurrentUserEmail() ?: return ""
        return locationPref.getString("${userEmail}_WORK_ADDRESS", "") ?: ""
    }

    fun saveCurrentLocation(address: String) {
        val userEmail = getCurrentUserEmail() ?: return
        locationPref.edit().putString("${userEmail}_CURRENT_ADDRESS", address).apply()
    }

    fun getCurrentLocation(): String {
        val userEmail = getCurrentUserEmail() ?: return "Dubai, U.A.E"
        return locationPref.getString("${userEmail}_CURRENT_ADDRESS", "Dubai, U.A.E") ?: "Dubai, U.A.E"
    }

    // Vehicle methods
    fun saveVehicle(
        type: String,
        brand: String,
        model: String,
        color: String,
        chassisNumber: String,
        plateNumber: String
    ) {
        val userEmail = getCurrentUserEmail() ?: return
        val vehicleCount = vehiclePref.getInt("${userEmail}_VEHICLE_COUNT", 0)

        vehiclePref.edit().apply {
            putString("${userEmail}_VEHICLE_${vehicleCount}_TYPE", type)
            putString("${userEmail}_VEHICLE_${vehicleCount}_BRAND", brand)
            putString("${userEmail}_VEHICLE_${vehicleCount}_MODEL", model)
            putString("${userEmail}_VEHICLE_${vehicleCount}_COLOR", color)
            putString("${userEmail}_VEHICLE_${vehicleCount}_CHASSIS", chassisNumber)
            putString("${userEmail}_VEHICLE_${vehicleCount}_PLATE", plateNumber)
            putInt("${userEmail}_VEHICLE_COUNT", vehicleCount + 1)
            apply()
        }
    }

    fun getVehicleCount(): Int {
        val userEmail = getCurrentUserEmail() ?: return 0
        return vehiclePref.getInt("${userEmail}_VEHICLE_COUNT", 0)
    }

    fun getVehicle(index: Int): Vehicle? {
        val userEmail = getCurrentUserEmail() ?: return null
        val type = vehiclePref.getString("${userEmail}_VEHICLE_${index}_TYPE", null) ?: return null
        val brand = vehiclePref.getString("${userEmail}_VEHICLE_${index}_BRAND", null) ?: return null
        val model = vehiclePref.getString("${userEmail}_VEHICLE_${index}_MODEL", null) ?: return null
        val color = vehiclePref.getString("${userEmail}_VEHICLE_${index}_COLOR", "") ?: ""
        val chassis = vehiclePref.getString("${userEmail}_VEHICLE_${index}_CHASSIS", "") ?: ""
        val plate = vehiclePref.getString("${userEmail}_VEHICLE_${index}_PLATE", null) ?: return null

        return Vehicle(type, brand, model, color, chassis, plate)
    }

    fun getAllVehicles(): List<Vehicle> {
        val vehicles = mutableListOf<Vehicle>()
        val count = getVehicleCount()

        for (i in 0 until count) {
            getVehicle(i)?.let { vehicles.add(it) }
        }

        return vehicles
    }

    fun clearVehicles() {
        val userEmail = getCurrentUserEmail() ?: return
        val count = getVehicleCount()

        vehiclePref.edit().apply {
            for (i in 0 until count) {
                remove("${userEmail}_VEHICLE_${i}_TYPE")
                remove("${userEmail}_VEHICLE_${i}_BRAND")
                remove("${userEmail}_VEHICLE_${i}_MODEL")
                remove("${userEmail}_VEHICLE_${i}_COLOR")
                remove("${userEmail}_VEHICLE_${i}_CHASSIS")
                remove("${userEmail}_VEHICLE_${i}_PLATE")
            }
            putInt("${userEmail}_VEHICLE_COUNT", 0)
            apply()
        }
    }

    data class Vehicle(
        val type: String,
        val brand: String,
        val model: String,
        val color: String,
        val chassisNumber: String,
        val plateNumber: String
    )
}