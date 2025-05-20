package com.example.car_service

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class PrefsHelper(context: Context) {
    private val context: Context = context.applicationContext
    private val sharedPref: SharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    private val USER_DATA_FILE = "user_data.json"

    // ======================== Authentication Methods ========================
    fun registerUser(email: String, password: String): Boolean {
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        // Check if user already exists
        for (i in 0 until users.length()) {
            if (users.getJSONObject(i).getString("email").equals(email, ignoreCase = true)) {
                return false
            }
        }

        // Add new user
        val newUser = JSONObject().apply {
            put("email", email)
            put("password", hashPassword(password))
            put("isLoggedIn", false)
            put("locations", JSONObject().apply {
                put("selectedLocationType", "Current")
                put("selectedAddress", "Dubai, U.A.E")
                put("homeAddress", "")
                put("workAddress", "")
                put("currentAddress", "Dubai, U.A.E")
            })
            put("vehicles", JSONArray())
        }

        users.put(newUser)
        saveUserData(userData)
        return true
    }

    fun loginUser(email: String, password: String): Boolean {
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email").equals(email, ignoreCase = true) &&
                user.getString("password") == hashPassword(password)) {

                // Mark user as logged in
                user.put("isLoggedIn", true)
                saveUserData(userData)

                // Store current user email in SharedPreferences for quick access
                sharedPref.edit().putString("CURRENT_USER", email).apply()
                return true
            }
        }
        return false
    }

    fun isLoggedIn(): Boolean {
        val currentUserEmail = sharedPref.getString("CURRENT_USER", null) ?: return false
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail && user.getBoolean("isLoggedIn")) {
                return true
            }
        }
        return false
    }

    fun getCurrentUserEmail(): String? {
        return sharedPref.getString("CURRENT_USER", null)
    }

    fun logout() {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.put("isLoggedIn", false)
                saveUserData(userData)
                break
            }
        }

        sharedPref.edit().remove("CURRENT_USER").apply()
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

    // ======================== Location Methods ========================
    fun saveLocation(locationType: String, address: String) {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                val locations = user.getJSONObject("locations")
                locations.put("selectedLocationType", locationType)
                locations.put("selectedAddress", address)

                when (locationType) {
                    "Home" -> locations.put("homeAddress", address)
                    "Work" -> locations.put("workAddress", address)
                    "Current" -> locations.put("currentAddress", address)
                }
                break
            }
        }
        saveUserData(userData)
    }

    fun getLocationType(): String {
        return getCurrentUserLocations()?.optString("selectedLocationType", "Current") ?: "Current"
    }

    fun getSelectedAddress(): String {
        val locations = getCurrentUserLocations() ?: return "Dubai, U.A.E"
        val selectedAddress = locations.optString("selectedAddress", null)

        return selectedAddress ?: when (getLocationType()) {
            "Home" -> getHomeLocation().ifEmpty { "Dubai, U.A.E" }
            "Work" -> getWorkLocation().ifEmpty { "Dubai, U.A.E" }
            else -> getCurrentLocation()
        }
    }

    fun saveHomeLocation(address: String) = saveUserLocation("homeAddress", address)
    fun saveWorkLocation(address: String) = saveUserLocation("workAddress", address)
    fun saveCurrentLocation(address: String) = saveUserLocation("currentAddress", address)

    fun getHomeLocation(): String = getUserLocation("homeAddress")
    fun getWorkLocation(): String = getUserLocation("workAddress")
    fun getCurrentLocation(): String = getUserLocation("currentAddress", "Dubai, U.A.E")

    private fun saveUserLocation(key: String, address: String) {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.getJSONObject("locations").put(key, address)
                break
            }
        }
        saveUserData(userData)
    }

    private fun getUserLocation(key: String, default: String = ""): String {
        return getCurrentUserLocations()?.optString(key, default) ?: default
    }

    private fun getCurrentUserLocations(): JSONObject? {
        val currentUserEmail = getCurrentUserEmail() ?: return null
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                return user.getJSONObject("locations")
            }
        }
        return null
    }

    // ======================== Vehicle Methods ========================
    fun saveVehicle(
        type: String,
        brand: String,
        model: String,
        color: String,
        chassisNumber: String,
        plateNumber: String
    ) {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                val vehicles = user.getJSONArray("vehicles")
                vehicles.put(JSONObject().apply {
                    put("type", type)
                    put("brand", brand)
                    put("model", model)
                    put("color", color)
                    put("chassisNumber", chassisNumber)
                    put("plateNumber", plateNumber)
                })
                break
            }
        }
        saveUserData(userData)
    }

    fun getVehicleCount(): Int {
        return getCurrentUserVehicles()?.length() ?: 0
    }

    fun getVehicle(index: Int): Vehicle? {
        val vehicles = getCurrentUserVehicles() ?: return null
        if (index < 0 || index >= vehicles.length()) return null

        val vehicleObj = vehicles.getJSONObject(index)
        return Vehicle(
            vehicleObj.getString("type"),
            vehicleObj.getString("brand"),
            vehicleObj.getString("model"),
            vehicleObj.optString("color", ""),
            vehicleObj.optString("chassisNumber", ""),
            vehicleObj.getString("plateNumber")
        )
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
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.put("vehicles", JSONArray())
                break
            }
        }
        saveUserData(userData)
    }

    private fun getCurrentUserVehicles(): JSONArray? {
        val currentUserEmail = getCurrentUserEmail() ?: return null
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                return user.getJSONArray("vehicles")
            }
        }
        return null
    }

    // ======================== JSON File Operations ========================
    private fun loadUserData(): JSONObject {
        return try {
            val file = File(context.filesDir, USER_DATA_FILE)
            if (!file.exists()) return JSONObject().apply { put("users", JSONArray()) }

            val jsonString = file.readText()
            JSONObject(jsonString)
        } catch (e: Exception) {
            JSONObject().apply { put("users", JSONArray()) }
        }
    }

    private fun saveUserData(data: JSONObject) {
        try {
            val file = File(context.filesDir, USER_DATA_FILE)
            file.writeText(data.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ======================== Data Classes ========================
    data class Vehicle(
        val type: String,
        val brand: String,
        val model: String,
        val color: String,
        val chassisNumber: String,
        val plateNumber: String
    )
}