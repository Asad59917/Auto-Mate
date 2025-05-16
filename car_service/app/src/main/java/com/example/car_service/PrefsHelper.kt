package com.example.car_service

import android.content.Context
import android.content.SharedPreferences

class PrefsHelper(context: Context) {
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

    fun saveUser(email: String, password: String, isLoggedIn: Boolean) {
        sharedPref.edit().apply {
            putString("EMAIL", email)
            putString("PASSWORD", password)
            putBoolean("IS_LOGGED_IN", isLoggedIn)
            apply()
        }
    }

    fun checkCredentials(email: String, password: String): Boolean {
        val savedEmail = sharedPref.getString("EMAIL", "")
        val savedPassword = sharedPref.getString("PASSWORD", "")
        return email == savedEmail && password == savedPassword
    }

    fun isLoggedIn(): Boolean = sharedPref.getBoolean("IS_LOGGED_IN", false)

    fun clearUser() {
        sharedPref.edit().clear().apply()
    }
}