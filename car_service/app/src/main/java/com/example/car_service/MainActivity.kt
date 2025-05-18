package com.example.car_service

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var locationBottomSheet: BottomSheetDialog
    private lateinit var addLocationBottomSheet: BottomSheetDialog
    private lateinit var vehicleBottomSheet: BottomSheetDialog

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

        // Initialize the vehicle selection bottom sheet
        setupVehicleBottomSheet()
    }

    private fun loadUserLocation() {
        // Get the location data for the currently logged in user
        val locationTitle = findViewById<TextView>(R.id.locationTitle)
        val locationType = prefsHelper.getLocationType()
        val address = prefsHelper.getSelectedAddress()

        if (address.isNotEmpty() && address != "Dubai, U.A.E") {
            locationTitle.text = address
        } else {
            locationTitle.text = "Select location"
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
        val workLocationText = locationView.findViewById<TextView>(R.id.workLocationText)
        val workLocationContainer = locationView.findViewById<View>(R.id.workLocationContainer)
        val workTitleText = locationView.findViewById<TextView>(R.id.workTitleText)

        // Set saved locations
        currentLocationText.text = prefsHelper.getCurrentLocation().ifEmpty { "No location set" }
        homeLocationText.text = prefsHelper.getHomeLocation().ifEmpty { "No home location set" }

        // Handle work location visibility
        val savedWorkLocation = prefsHelper.getWorkLocation()
        if (savedWorkLocation.isNotEmpty()) {
            workLocationContainer.visibility = View.VISIBLE
            workLocationText.text = savedWorkLocation
            addWorkItem.visibility = View.GONE
        } else {
            workLocationContainer.visibility = View.GONE
            addWorkItem.visibility = View.VISIBLE
            workTitleText.text = "Add work"
        }

        currentLocationItem.setOnClickListener {
            if (prefsHelper.getCurrentLocation().isNotEmpty()) {
                updateLocation("Current", prefsHelper.getCurrentLocation())
            } else {
                showAddLocationBottomSheet("Current")
            }
            locationBottomSheet.dismiss()
        }

        homeLocationItem.setOnClickListener {
            if (prefsHelper.getHomeLocation().isNotEmpty()) {
                updateLocation("Home", prefsHelper.getHomeLocation())
            } else {
                showAddLocationBottomSheet("Home")
            }
            locationBottomSheet.dismiss()
        }

        addWorkItem.setOnClickListener {
            showAddLocationBottomSheet("Work")
            locationBottomSheet.dismiss()
        }

        workLocationContainer.setOnClickListener {
            updateLocation("Work", prefsHelper.getWorkLocation())
            locationBottomSheet.dismiss()
        }

        selectButton.setOnClickListener {
            locationBottomSheet.dismiss()
        }

        cancelButton.setOnClickListener {
            locationBottomSheet.dismiss()
        }

        addNewButton.setOnClickListener {
            showAddLocationBottomSheet(null)
            locationBottomSheet.dismiss()
        }
    }

    private fun setupAddLocationBottomSheet() {
        addLocationBottomSheet = BottomSheetDialog(this)
        val addLocationView = layoutInflater.inflate(R.layout.add_location_bottom_sheet, null)
        addLocationBottomSheet.setContentView(addLocationView)

        val cancelButton = addLocationView.findViewById<TextView>(R.id.cancelAddLocationButton)
        val saveButton = addLocationView.findViewById<CardView>(R.id.saveButton)
        val saveTopButton = addLocationView.findViewById<TextView>(R.id.saveLocationButton)
        val locationTypeGroup = addLocationView.findViewById<RadioGroup>(R.id.locationTypeGroup)
        val addressInput = addLocationView.findViewById<EditText>(R.id.addressInput)
        val cityInput = addLocationView.findViewById<EditText>(R.id.cityInput)
        val countryInput = addLocationView.findViewById<EditText>(R.id.countryInput)

        // Set default country
        countryInput.setText("U.A.E")

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

            // Save the location
            prefsHelper.saveLocation(selectedLocationType, fullAddress)
            updateLocation(selectedLocationType, fullAddress)

            Toast.makeText(this, "$selectedLocationType location saved", Toast.LENGTH_SHORT).show()
            addLocationBottomSheet.dismiss()
            setupLocationBottomSheet() // Refresh location sheet
        }

        cancelButton.setOnClickListener { addLocationBottomSheet.dismiss() }
        saveButton.setOnClickListener { saveAction() }
        saveTopButton.setOnClickListener { saveAction() }
    }

    private fun setupVehicleBottomSheet() {
        // Create the bottom sheet dialog for vehicles
        vehicleBottomSheet = BottomSheetDialog(this)
        val vehicleView = layoutInflater.inflate(R.layout.vehicle_bottom_sheet, null)
        vehicleBottomSheet.setContentView(vehicleView)

        // Get references to views
        val cancelButton = vehicleView.findViewById<TextView>(R.id.cancelButton)
        val addNewButton = vehicleView.findViewById<TextView>(R.id.addNewButton)
        val selectButton = vehicleView.findViewById<CardView>(R.id.selectButton)
        val vehicleListContainer = vehicleView.findViewById<LinearLayout>(R.id.vehicleListContainer)

        // Empty state initially
        vehicleListContainer.removeAllViews()

        // Setup add new vehicle button
        addNewButton.setOnClickListener {
            Toast.makeText(this, "Add new vehicle clicked", Toast.LENGTH_SHORT).show()
            vehicleBottomSheet.dismiss()
            // Add navigation to vehicle add screen here if needed
            startActivity(Intent(this, AddVehicleActivity::class.java))
        }

        // Setup edit vehicles button
        val editVehiclesButton = vehicleView.findViewById<View>(R.id.editVehiclesContainer)
        editVehiclesButton.setOnClickListener {
            Toast.makeText(this, "Edit vehicles clicked", Toast.LENGTH_SHORT).show()
            vehicleBottomSheet.dismiss()
            // Add navigation to vehicle edit screen here if needed
            // startActivity(Intent(this, EditVehiclesActivity::class.java))
        }

        // Setup cancel button
        cancelButton.setOnClickListener {
            vehicleBottomSheet.dismiss()
        }

        // Setup select button
        selectButton.setOnClickListener {
            Toast.makeText(this, "No vehicle selected", Toast.LENGTH_SHORT).show()
            vehicleBottomSheet.dismiss()
        }
    }

    private fun showAddLocationBottomSheet(locationType: String?) {
        if (!this::addLocationBottomSheet.isInitialized) {
            setupAddLocationBottomSheet()
        }

        val radioHome = addLocationBottomSheet.findViewById<RadioButton>(R.id.radioHome)
        val radioWork = addLocationBottomSheet.findViewById<RadioButton>(R.id.radioWork)
        val radioCurrent = addLocationBottomSheet.findViewById<RadioButton>(R.id.radioCurrent)
        val addressInput = addLocationBottomSheet.findViewById<EditText>(R.id.addressInput)
        val cityInput = addLocationBottomSheet.findViewById<EditText>(R.id.cityInput)

        when (locationType) {
            "Home" -> radioHome?.isChecked = true
            "Work" -> radioWork?.isChecked = true
            "Current" -> radioCurrent?.isChecked = true
        }

        addressInput?.setText("")
        cityInput?.setText("")
        addLocationBottomSheet.show()
    }

    private fun updateLocation(locationType: String, address: String) {
        findViewById<TextView>(R.id.locationTitle).text = address
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
            locationBottomSheet.show()
        }

        // Bottom navigation
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        findViewById<View>(R.id.homeNavItem).setOnClickListener {
            // Already on home screen
        }

        findViewById<View>(R.id.searchNavItem).setOnClickListener {
            Toast.makeText(this, "Search clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.locationNavItem).setOnClickListener {
            locationBottomSheet.show()
        }

        findViewById<View>(R.id.profileNavItem).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                prefsHelper.logout()
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setupSearchBar() {
        findViewById<CardView>(R.id.searchBar).setOnClickListener {
            Toast.makeText(this, "Search bar clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.filtersButton).setOnClickListener {
            Toast.makeText(this, "Filters clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLocationAndVehicles() {
        findViewById<CardView>(R.id.myLocationButton).setOnClickListener {
            locationBottomSheet.show()
        }

        findViewById<CardView>(R.id.myVehiclesButton).setOnClickListener {
            vehicleBottomSheet.show()
        }
    }

    private fun setupServiceCards() {
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

        val serviceTypes = listOf(
            "Service", "Car Towing", "Brake Service", "Car Wash",
            "Fuel Up", "Tire Change", "Battery Change", "Service Contract"
        )

        serviceCards.forEachIndexed { index, cardView ->
            cardView.setOnClickListener {
                navigateToService(serviceTypes[index])
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
        findViewById<CardView>(R.id.oilChangeOffer).setOnClickListener {
            Toast.makeText(this, "Oil change offer clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<CardView>(R.id.carWashOffer).setOnClickListener {
            Toast.makeText(this, "Car wash offer clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        when {
            this::locationBottomSheet.isInitialized && locationBottomSheet.isShowing ->
                locationBottomSheet.dismiss()
            this::addLocationBottomSheet.isInitialized && addLocationBottomSheet.isShowing ->
                addLocationBottomSheet.dismiss()
            this::vehicleBottomSheet.isInitialized && vehicleBottomSheet.isShowing ->
                vehicleBottomSheet.dismiss()
            else -> {
                if (prefsHelper.isLoggedIn()) {
                    super.onBackPressed()
                } else {
                    finish()
                }
            }
        }
    }
}