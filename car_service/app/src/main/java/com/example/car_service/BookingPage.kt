package com.example.car_service

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BookingPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_page)

        // Get data from Intent
        val serviceId = intent.getStringExtra("service_id") ?: ""
        val serviceName = intent.getStringExtra("service_name") ?: "Service"
        val basePrice = intent.getIntExtra("base_price", 0)

        // Update UI (example)
        findViewById<TextView>(R.id.tvServiceName).text = serviceName
        findViewById<TextView>(R.id.tvPrice).text = "From AED $basePrice"

        // Handle booking logic based on serviceId
        when (serviceId) {
            ServiceConstants.BATTERY_CHANGE -> setupBatteryBooking()
            ServiceConstants.BRAKE_SERVICE -> setupBrakeServiceBooking()
            // Add other services...
        }
    }

    private fun setupBatteryBooking() {
        // Battery-specific setup
    }

    private fun setupBrakeServiceBooking() {
        // Brake-service-specific setup
    }
}