package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var locationBottomSheet: BottomSheetDialog
    private lateinit var addLocationBottomSheet: BottomSheetDialog

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

        // Load saved location for current user
        loadUserLocation()

        // Original setup functions
        setupNavigation()
        setupSearchBar()
        setupLocationAndVehicles()
        setupServiceCards()
        setupSpecialOffers()

        // Initialize the location bottom sheet
        setupLocationBottomSheet()

        // Initialize the add location bottom sheet
        setupAddLocationBottomSheet()
    }

    private fun loadUserLocation() {
        // Get the location data for the currently logged in user
        val locationTitle = findViewById<TextView>(R.id.locationTitle)
        val locationType = prefsHelper.getLocationType()

        // Update UI based on saved location type for this user
        when (locationType) {
            "Home" -> {
                val location = prefsHelper.getHomeLocation()
                if (location.isNotEmpty()) {
                    locationTitle.text = location
                } else {
                    locationTitle.text = "Select location"
                }
            }
            "Work" -> {
                val location = prefsHelper.getWorkLocation()
                if (location.isNotEmpty()) {
                    locationTitle.text = location
                } else {
                    locationTitle.text = "Select location"
                }
            }
            "Current" -> {
                val location = prefsHelper.getCurrentLocation()
                if (location != "Dubai, U.A.E") {
                    locationTitle.text = location
                } else {
                    locationTitle.text = "Select location"
                }
            }
            else -> locationTitle.text = "Select location"
        }
    }

    private fun setupLocationBottomSheet() {
        // Create the bottom sheet dialog
        locationBottomSheet = BottomSheetDialog(this)
        val locationView = layoutInflater.inflate(R.layout.location_bottom_sheet, null)
        locationBottomSheet.setContentView(locationView)

        // Setup click listeners for location options
        val currentLocationItem = locationView.findViewById<View>(R.id.currentLocationItem)
        val homeLocationItem = locationView.findViewById<View>(R.id.homeLocationItem)
        val addWorkItem = locationView.findViewById<View>(R.id.addWorkItem)
        val selectButton = locationView.findViewById<View>(R.id.selectButton)
        val cancelButton = locationView.findViewById<TextView>(R.id.cancelButton)
        val addNewButton = locationView.findViewById<TextView>(R.id.addNewButton)

        // Update UI with saved locations for this user
        val currentLocationText = locationView.findViewById<TextView>(R.id.currentLocationText)
        val homeLocationText = locationView.findViewById<TextView>(R.id.homeLocationText)

        // Set the saved locations text from preferences
        val savedCurrentLocation = prefsHelper.getCurrentLocation()
        if (savedCurrentLocation != "Dubai, U.A.E") {
            currentLocationText.text = savedCurrentLocation
        } else {
            currentLocationText.text = "No location set"
        }

        val savedHomeLocation = prefsHelper.getHomeLocation()
        if (savedHomeLocation.isNotEmpty()) {
            homeLocationText.text = savedHomeLocation
        } else {
            homeLocationText.text = "No home location set"
        }

        // Display work address if it exists
        val savedWorkLocation = prefsHelper.getWorkLocation()
        val workLocationText = locationView.findViewById<TextView>(R.id.workLocationText)
        val workLocationContainer = locationView.findViewById<View>(R.id.workLocationContainer)
        val workTitleText = locationView.findViewById<TextView>(R.id.workTitleText)

        if (savedWorkLocation.isNotEmpty()) {
            // Show work location if saved
            workLocationContainer.visibility = View.VISIBLE
            workLocationText.text = savedWorkLocation

            // Hide "Add work" item when we're showing the work container
            addWorkItem.visibility = View.GONE
        } else {
            // Hide work container if no work location is saved
            workLocationContainer.visibility = View.GONE

            // Show "Add work" item
            addWorkItem.visibility = View.VISIBLE
            workTitleText.text = "Add work"
        }

        currentLocationItem.setOnClickListener {
            // Handle current location selection
            if (savedCurrentLocation != "Dubai, U.A.E") {
                updateLocation("Current", savedCurrentLocation)
            } else {
                Toast.makeText(this, "Please add a current location first", Toast.LENGTH_SHORT).show()
                showAddLocationBottomSheet("Current")
            }
            locationBottomSheet.dismiss()
        }

        homeLocationItem.setOnClickListener {
            // Handle home location selection
            if (savedHomeLocation.isNotEmpty()) {
                updateLocation("Home", savedHomeLocation)
            } else {
                // No home location saved, ask user to add one
                Toast.makeText(this, "Please add a home location first", Toast.LENGTH_SHORT).show()
                showAddLocationBottomSheet("Home")
            }
            locationBottomSheet.dismiss()
        }

        addWorkItem.setOnClickListener {
            if (savedWorkLocation.isNotEmpty()) {
                // Use existing work location
                updateLocation("Work", savedWorkLocation)
            } else {
                // Handle adding work location
                showAddLocationBottomSheet("Work")
            }
            locationBottomSheet.dismiss()
        }

        workLocationContainer.setOnClickListener {
            if (savedWorkLocation.isNotEmpty()) {
                updateLocation("Work", savedWorkLocation)
                locationBottomSheet.dismiss()
            }
        }

        selectButton.setOnClickListener {
            // Process the selected location
            Toast.makeText(this, "Location selected", Toast.LENGTH_SHORT).show()
            locationBottomSheet.dismiss()
        }

        cancelButton.setOnClickListener {
            locationBottomSheet.dismiss()
        }

        addNewButton.setOnClickListener {
            // Show add location bottom sheet
            showAddLocationBottomSheet(null)
            locationBottomSheet.dismiss()
        }
    }

    private fun setupAddLocationBottomSheet() {
        // Create the add location bottom sheet dialog
        addLocationBottomSheet = BottomSheetDialog(this)
        val addLocationView = layoutInflater.inflate(R.layout.add_location_bottom_sheet, null)
        addLocationBottomSheet.setContentView(addLocationView)

        // Get references to views
        val cancelButton = addLocationView.findViewById<TextView>(R.id.cancelAddLocationButton)
        val saveButton = addLocationView.findViewById<CardView>(R.id.saveButton)
        val saveTopButton = addLocationView.findViewById<TextView>(R.id.saveLocationButton)
        val locationTypeGroup = addLocationView.findViewById<RadioGroup>(R.id.locationTypeGroup)
        val addressInput = addLocationView.findViewById<EditText>(R.id.addressInput)
        val cityInput = addLocationView.findViewById<EditText>(R.id.cityInput)
        val countryInput = addLocationView.findViewById<EditText>(R.id.countryInput)
        val radioHome = addLocationView.findViewById<RadioButton>(R.id.radioHome)
        val radioWork = addLocationView.findViewById<RadioButton>(R.id.radioWork)
        val radioCurrent = addLocationView.findViewById<RadioButton>(R.id.radioCurrent)

        // Set default value for country
        countryInput.setText("U.A.E")

        // Setup click listeners
        cancelButton.setOnClickListener {
            addLocationBottomSheet.dismiss()
        }

        val saveAction = saveAction@{
            val address = addressInput.text.toString().trim()
            val city = cityInput.text.toString().trim()
            val country = countryInput.text.toString().trim()

            if (address.isEmpty() || city.isEmpty() || country.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@saveAction
            }

            val fullAddress = if (address.contains(city)) {
                "$address · $country"
            } else {
                "$address · $city, $country"
            }

            val selectedLocationType = when (locationTypeGroup.checkedRadioButtonId) {
                R.id.radioHome -> "Home"
                R.id.radioWork -> "Work"
                R.id.radioCurrent -> "Current"
                else -> "Current"
            }

            when (selectedLocationType) {
                "Home" -> prefsHelper.saveHomeLocation(fullAddress)
                "Work" -> prefsHelper.saveWorkLocation(fullAddress)
                "Current" -> prefsHelper.saveCurrentLocation(fullAddress)
            }

            // Update location type and address
            prefsHelper.saveLocation(selectedLocationType, fullAddress)

            // Update UI
            updateLocation(selectedLocationType, fullAddress)

            Toast.makeText(this, "$selectedLocationType location saved", Toast.LENGTH_SHORT).show()
            addLocationBottomSheet.dismiss()

            // Refresh location bottom sheet if needed later
            setupLocationBottomSheet()
        }

        saveButton.setOnClickListener { saveAction.invoke() }
        saveTopButton.setOnClickListener { saveAction.invoke() }
    }

    private fun showAddLocationBottomSheet(locationType: String?) {
        if (!this::addLocationBottomSheet.isInitialized) {
            setupAddLocationBottomSheet()
        }

        // Get references to radio buttons
        val radioHome = addLocationBottomSheet.findViewById<RadioButton>(R.id.radioHome)
        val radioWork = addLocationBottomSheet.findViewById<RadioButton>(R.id.radioWork)
        val radioCurrent = addLocationBottomSheet.findViewById<RadioButton>(R.id.radioCurrent)

        // Pre-select the specified location type if provided
        when (locationType) {
            "Home" -> radioHome?.isChecked = true
            "Work" -> radioWork?.isChecked = true
            "Current" -> radioCurrent?.isChecked = true
        }

        // Clear input fields
        val addressInput = addLocationBottomSheet.findViewById<EditText>(R.id.addressInput)
        val cityInput = addLocationBottomSheet.findViewById<EditText>(R.id.cityInput)

        addressInput?.setText("")
        cityInput?.setText("")

        // Show the bottom sheet
        addLocationBottomSheet.show()
    }

    private fun updateLocation(locationType: String, address: String) {
        // Update location in UI
        val locationTitle = findViewById<TextView>(R.id.locationTitle)
        locationTitle.text = address

        // Update location in preferences for this specific user
        prefsHelper.saveLocation(locationType, address)

        Toast.makeText(this, "$locationType location selected", Toast.LENGTH_SHORT).show()
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
            // Show location bottom sheet instead of just a toast
            locationBottomSheet.show()
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
            // Show location bottom sheet instead of just a toast
            locationBottomSheet.show()
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
            // Show location bottom sheet instead of just a toast
            locationBottomSheet.show()
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
        // Check if bottom sheets are showing and dismiss them
        if (this::locationBottomSheet.isInitialized && locationBottomSheet.isShowing) {
            locationBottomSheet.dismiss()
            return
        }

        if (this::addLocationBottomSheet.isInitialized && addLocationBottomSheet.isShowing) {
            addLocationBottomSheet.dismiss()
            return
        }

        // Optional: Prevent going back to auth screens if logged out
        if (prefsHelper.isLoggedIn()) {
            super.onBackPressed()
        } else {
            finish()
        }
    }
}