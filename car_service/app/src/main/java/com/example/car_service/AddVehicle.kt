package com.example.car_service

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.car_service.databinding.ActivityAddVehicleBinding
import com.google.android.material.card.MaterialCardView

class AddVehicleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddVehicleBinding
    private var currentSelectedPosition = 0
    private lateinit var vehicleTypeContainers: List<View>
    private lateinit var vehicleTypeTextViews: List<TextView>
    private lateinit var selectionIndicator: MaterialCardView

    // Vehicle type to icon mapping
    private val vehicleIcons = mapOf(
        "Car" to R.drawable.ic_car,
        "Motorcycle" to R.drawable.ic_motorcycle,
        "Boat" to R.drawable.ic_boat,
        "Other" to R.drawable.ic_other_vehicle
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVehicleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI components
        setupVehicleTypeSelector()
        setupBackButton()
        setupSaveButton()
    }

    private fun setupVehicleTypeSelector() {
        // Get references to vehicle type containers and text views
        vehicleTypeContainers = listOf(
            binding.carContainer,
            binding.motorcycleContainer,
            binding.boatContainer,
            binding.otherContainer
        )

        vehicleTypeTextViews = listOf(
            binding.carTextView,
            binding.motorcycleTextView,
            binding.boatTextView,
            binding.otherTextView
        )

        // Get reference to the selection indicator
        selectionIndicator = binding.selectionIndicator

        // Initially hide the selection indicator until we can calculate positions
        selectionIndicator.visibility = View.INVISIBLE

        // Wait for layout to be ready before positioning the selection indicator
        binding.vehicleTypeCardView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.vehicleTypeCardView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Position the selection indicator behind the first option (Car)
                updateSelectionIndicatorPosition(0, false)

                // Now make it visible
                selectionIndicator.visibility = View.VISIBLE
            }
        })

        // Set up click listeners for each vehicle type
        vehicleTypeContainers.forEachIndexed { index, container ->
            container.setOnClickListener {
                if (currentSelectedPosition != index) {
                    updateVehicleTypeSelection(index)
                    currentSelectedPosition = index
                }
            }
        }

        // Set initial selection (Car is already styled in XML)
        currentSelectedPosition = 0
    }

    private fun updateVehicleTypeSelection(selectedPosition: Int) {
        val selectedColor = getColor(R.color.black)
        val unselectedColor = getColor(R.color.gray_text)

        // Update text appearances
        vehicleTypeTextViews.forEachIndexed { index, textView ->
            textView.setTextColor(if (index == selectedPosition) selectedColor else unselectedColor)
            textView.setTypeface(null, if (index == selectedPosition) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

        // Animate the selection indicator to the new position
        updateSelectionIndicatorPosition(selectedPosition, true)
    }

    private fun updateSelectionIndicatorPosition(position: Int, animate: Boolean) {
        if (!::vehicleTypeContainers.isInitialized || position >= vehicleTypeContainers.size) return

        val targetContainer = vehicleTypeContainers[position]

        // Calculate the target position and size
        val targetX = targetContainer.left.toFloat()
        val targetWidth = targetContainer.width
        val targetHeight = targetContainer.height

        if (animate) {
            // Get the current position of the indicator
            val fromX = selectionIndicator.translationX

            // Create and start the animation
            val animator = ValueAnimator.ofFloat(fromX, targetX)
            animator.duration = 250 // Animation duration in milliseconds
            animator.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Float
                selectionIndicator.translationX = value
            }
            animator.start()
        } else {
            // Immediately set position without animation
            selectionIndicator.translationX = targetX
        }

        // Set the size of the indicator to match the container
        val params = selectionIndicator.layoutParams as android.widget.FrameLayout.LayoutParams
        params.width = targetWidth
        params.height = targetHeight
        selectionIndicator.layoutParams = params
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            // Get the selected vehicle type
            val vehicleType = vehicleTypeTextViews[currentSelectedPosition].text.toString()

            // Get all input values
            val color = binding.colorInput.text.toString()
            val make = binding.makeInput.text.toString()
            val model = binding.modelInput.text.toString()
            val chassisNumber = binding.chassisInput.text.toString()
            val licensePlate = binding.licenseInput.text.toString()

            // Validate required fields
            if (make.isEmpty() || model.isEmpty() || licensePlate.isEmpty()) {
                Toast.makeText(this, "Please fill in Make, Model and License Plate fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save vehicle to preferences
            val prefsHelper = PrefsHelper(this)
            prefsHelper.saveVehicle(
                type = vehicleType,
                brand = make,
                model = model,
                color = color,
                chassisNumber = chassisNumber,
                plateNumber = licensePlate
            )

            Toast.makeText(this, "Vehicle saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Helper function to get vehicle icon resource ID
    fun getVehicleIconResource(vehicleType: String): Int {
        return vehicleIcons[vehicleType] ?: R.drawable.ic_other_vehicle
    }
}