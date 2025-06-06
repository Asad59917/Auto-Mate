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

    // Data class for Booking (keeping this as it's specific to PrefsHelper)
    data class Booking(
        val serviceId: String,
        val serviceName: String,
        val vehicleBrand: String,
        val vehicleModel: String,
        val plateNumber: String,
        val bookingDate: String,
        val location: String,
        val price: Int,
        val bookingTime: Long = System.currentTimeMillis()
    )

    // Enum for password update results
    enum class PasswordUpdateResult {
        SUCCESS,
        WRONG_CURRENT_PASSWORD,
        ERROR
    }

    // Authentication Methods
    fun registerUser(email: String, password: String, fullName: String): Boolean {
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            if (users.getJSONObject(i).getString("email").equals(email, ignoreCase = true)) {
                return false
            }
        }

        val newUser = JSONObject().apply {
            put("email", email)
            put("password", hashPassword(password))
            put("fullName", fullName)
            put("isLoggedIn", false)
            put("locations", JSONObject().apply {
                put("selectedLocationType", "Current")
                put("selectedAddress", "Dubai, U.A.E")
                put("homeAddress", "")
                put("workAddress", "")
                put("currentAddress", "Dubai, U.A.E")
            })
            put("vehicles", JSONArray())
            put("bookings", JSONArray())
        }

        users.put(newUser)
        return saveUserData(userData)
    }

    fun loginUser(email: String, password: String): Boolean {
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email").equals(email, ignoreCase = true) &&
                user.getString("password") == hashPassword(password)) {

                user.put("isLoggedIn", true)
                if (saveUserData(userData)) {
                    sharedPref.edit().putString("CURRENT_USER", email).apply()
                    return true
                }
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

    fun getCurrentUserName(): String? {
        val currentUserEmail = getCurrentUserEmail() ?: return null
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                return user.optString("fullName", null)
            }
        }
        return null
    }

    fun logout(clearRememberMe: Boolean = false) {
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

        if (clearRememberMe) {
            clearRememberMe()
        }
    }

    // Profile Update Methods
    fun updateUserName(newName: String): Boolean {
        val currentUserEmail = getCurrentUserEmail() ?: return false
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                user.put("fullName", newName)
                return saveUserData(userData)
            }
        }
        return false
    }

    fun updateUserPassword(currentPassword: String, newPassword: String): PasswordUpdateResult {
        val currentUserEmail = getCurrentUserEmail() ?: return PasswordUpdateResult.ERROR
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                // Verify current password
                val storedPassword = user.getString("password")
                val hashedCurrentPassword = hashPassword(currentPassword)

                if (storedPassword != hashedCurrentPassword) {
                    return PasswordUpdateResult.WRONG_CURRENT_PASSWORD
                }

                // Update password
                user.put("password", hashPassword(newPassword))
                return if (saveUserData(userData)) {
                    PasswordUpdateResult.SUCCESS
                } else {
                    PasswordUpdateResult.ERROR
                }
            }
        }
        return PasswordUpdateResult.ERROR
    }

    // Remember Me Methods (Secure Version)
    fun saveRememberMe(email: String) {
        sharedPref.edit().apply {
            putBoolean("REMEMBER_ME", true)
            putString("REMEMBERED_EMAIL", email)
            apply()
        }
    }

    fun clearRememberMe() {
        sharedPref.edit().apply {
            remove("REMEMBER_ME")
            remove("REMEMBERED_EMAIL")
            apply()
        }
    }

    fun shouldRememberUser(): Boolean {
        return sharedPref.getBoolean("REMEMBER_ME", false)
    }

    fun getRememberedEmail(): String? {
        return if (shouldRememberUser()) {
            sharedPref.getString("REMEMBERED_EMAIL", null)
        } else {
            null
        }
    }

    // Auto-login check (only if remember me is enabled and user was previously logged in)
    fun canAutoLogin(): Boolean {
        return shouldRememberUser() && getCurrentUserEmail() != null && isLoggedIn()
    }

    // Location Methods
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

    // Vehicle Methods - Updated to work with Vehicle data class from VehicleListHelper
    fun saveVehicle(vehicle: Vehicle) {
        saveVehicle(
            type = vehicle.type,
            brand = vehicle.brand,
            model = vehicle.model,
            color = vehicle.color,
            chassisNumber = vehicle.chassisNumber,
            plateNumber = vehicle.plateNumber
        )
    }

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
            type = vehicleObj.getString("type"),
            brand = vehicleObj.getString("brand"),
            model = vehicleObj.getString("model"),
            color = vehicleObj.optString("color", ""),
            chassisNumber = vehicleObj.optString("chassisNumber", ""),
            plateNumber = vehicleObj.getString("plateNumber"),
            isSelected = false // Default to not selected when loading from preferences
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

    fun getAllVehiclesWithSelection(selectedPlateNumber: String? = null): List<Vehicle> {
        val vehicles = mutableListOf<Vehicle>()
        val count = getVehicleCount()

        for (i in 0 until count) {
            getVehicle(i)?.let { vehicle ->
                vehicles.add(vehicle.copy(isSelected = vehicle.plateNumber == selectedPlateNumber))
            }
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

    // Booking Methods
    fun saveBooking(booking: Booking) {
        val currentUserEmail = getCurrentUserEmail() ?: return
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail) {
                if (!user.has("bookings")) {
                    user.put("bookings", JSONArray())
                }

                val bookings = user.getJSONArray("bookings")
                bookings.put(JSONObject().apply {
                    put("serviceId", booking.serviceId)
                    put("serviceName", booking.serviceName)
                    put("vehicleBrand", booking.vehicleBrand)
                    put("vehicleModel", booking.vehicleModel)
                    put("plateNumber", booking.plateNumber)
                    put("bookingDate", booking.bookingDate)
                    put("location", booking.location)
                    put("price", booking.price)
                    put("bookingTime", booking.bookingTime)
                })
                break
            }
        }
        saveUserData(userData)
    }

    fun getAllBookings(): List<Booking> {
        val currentUserEmail = getCurrentUserEmail() ?: return emptyList()
        val userData = loadUserData()
        val users = userData.getJSONArray("users")
        val bookings = mutableListOf<Booking>()

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail && user.has("bookings")) {
                val bookingsArray = user.getJSONArray("bookings")
                for (j in 0 until bookingsArray.length()) {
                    val bookingObj = bookingsArray.getJSONObject(j)
                    bookings.add(Booking(
                        bookingObj.getString("serviceId"),
                        bookingObj.getString("serviceName"),
                        bookingObj.getString("vehicleBrand"),
                        bookingObj.getString("vehicleModel"),
                        bookingObj.getString("plateNumber"),
                        bookingObj.getString("bookingDate"),
                        bookingObj.getString("location"),
                        bookingObj.getInt("price"),
                        bookingObj.getLong("bookingTime")
                    ))
                }
                break
            }
        }
        return bookings.sortedByDescending { it.bookingTime }
    }

    // Cancel booking method
    fun cancelBooking(bookingToCancel: Booking): Boolean {
        val currentUserEmail = getCurrentUserEmail() ?: return false
        val userData = loadUserData()
        val users = userData.getJSONArray("users")

        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("email") == currentUserEmail && user.has("bookings")) {
                val bookings = user.getJSONArray("bookings")

                // Find and remove the booking
                for (j in bookings.length() - 1 downTo 0) {
                    val booking = bookings.getJSONObject(j)
                    if (booking.getString("serviceId") == bookingToCancel.serviceId &&
                        booking.getString("plateNumber") == bookingToCancel.plateNumber &&
                        booking.getLong("bookingTime") == bookingToCancel.bookingTime) {

                        bookings.remove(j)
                        return saveUserData(userData)
                    }
                }
                break
            }
        }
        return false
    }

    // Get booking by index (useful for list operations)
    fun getBookingByIndex(index: Int): Booking? {
        val allBookings = getAllBookings()
        return if (index in 0 until allBookings.size) allBookings[index] else null
    }

    // Helper Methods
    private fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            password
        }
    }

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

    private fun saveUserData(data: JSONObject): Boolean {
        return try {
            val file = File(context.filesDir, USER_DATA_FILE)
            file.writeText(data.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}