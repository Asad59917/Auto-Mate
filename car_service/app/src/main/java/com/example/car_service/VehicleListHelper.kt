package com.example.car_service

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat

data class Vehicle(
    val type: String,
    val brand: String,
    val model: String,
    val color: String,
    val chassisNumber: String,
    val plateNumber: String,
    val isSelected: Boolean = false
)

class VehicleListHelper(private val context: Context) {

    // Vehicle type to icon mapping
    private val vehicleIcons = mapOf(
        "Car" to R.drawable.ic_car,
        "Motorcycle" to R.drawable.ic_motorcycle,
        "Boat" to R.drawable.ic_boat,
        "Other" to R.drawable.ic_other_vehicle
    )

    // Vehicle type to gradient colors mapping
    private val vehicleGradientColors = mapOf(
        "Car" to Pair("#4267F6", "#6B73FF"),
        "Motorcycle" to Pair("#FF6B35", "#FF8E53"),
        "Boat" to Pair("#36D1DC", "#5B86E5"),
        "Other" to Pair("#667eea", "#764ba2")
    )

    fun createVehicleItemView(
        vehicle: Vehicle,
        onItemClick: (Vehicle) -> Unit
    ): View {
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.item_vehicle, null)

        // Find views
        val vehicleIcon = itemView.findViewById<ImageView>(R.id.vehicleIcon)
        val vehicleBrandModel = itemView.findViewById<TextView>(R.id.vehicleBrandModel)
        val vehicleDetails = itemView.findViewById<TextView>(R.id.vehicleDetails)
        val vehiclePlate = itemView.findViewById<TextView>(R.id.vehiclePlate)
        val selectionIndicator = itemView.findViewById<View>(R.id.selectionIndicator)
        val arrowRight = itemView.findViewById<ImageView>(R.id.arrowRight)
        val rootLayout = itemView.findViewById<ConstraintLayout>(R.id.rootLayout)

        // Set vehicle icon based on type
        val iconResource = vehicleIcons[vehicle.type] ?: R.drawable.ic_other_vehicle
        vehicleIcon.setImageResource(iconResource)

        // Set vehicle information
        vehicleBrandModel.text = "${vehicle.brand} ${vehicle.model}"
        vehicleDetails.text = "${vehicle.color} • ${vehicle.type}"
        vehiclePlate.text = vehicle.plateNumber

        // Update selection state
        updateSelectionState(vehicle.isSelected, selectionIndicator, arrowRight, rootLayout)

        // Set click listener
        itemView.setOnClickListener {
            onItemClick(vehicle)
        }

        return itemView
    }

    private fun updateSelectionState(
        isSelected: Boolean,
        selectionIndicator: View,
        arrowRight: ImageView,
        rootLayout: ConstraintLayout
    ) {
        if (isSelected) {
            selectionIndicator.visibility = View.VISIBLE
            arrowRight.visibility = View.GONE
            rootLayout.background = ContextCompat.getDrawable(context, R.drawable.vehicle_item_selected_bg)
        } else {
            selectionIndicator.visibility = View.GONE
            arrowRight.visibility = View.VISIBLE
            rootLayout.background = ContextCompat.getDrawable(context, R.drawable.vehicle_item_background)
        }
    }


}