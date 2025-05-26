package com.example.car_service

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class BookingPage : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper
    private var locationBottomSheet: BottomSheetDialog? = null
    private var addLocationBottomSheet: BottomSheetDialog? = null
    private var vehicleBottomSheet: BottomSheetDialog? = null
    private var confirmBookingBottomSheet: BottomSheetDialog? = null
    private var successBookingBottomSheet: BottomSheetDialog? = null
    private var selectedVehicleIndex = -1
    private var selectedDateOption = 1 // 0: Today, 1: Tomorrow, 2: This week, 3: Custom
    private var selectedDate: Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    // Service data
    private var serviceId: String = ""
    private var serviceName: String = ""
    private var basePrice: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d("BookingPage", "Starting BookingPage onCreate")
            setContentView(R.layout.activity_booking_page)

            // Initialize SharedPreferences helper
            prefsHelper = PrefsHelper(this)

            // Check if user is logged in
            if (!prefsHelper.isLoggedIn()) {
                Log.w("BookingPage", "User not logged in, redirecting to SignIn")
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
                return
            }

            // Get data from Intent
            serviceId = intent.getStringExtra("service_id") ?: ""
            serviceName = intent.getStringExtra("service_name") ?: "Service"
            basePrice = intent.getIntExtra("base_price", 0)

            Log.d("BookingPage", "Service: $serviceName, Price: $basePrice")

            // Validate intent data
            if (serviceId.isEmpty() || basePrice == 0) {
                Log.e("BookingPage", "Invalid service data received")
                Toast.makeText(this, "Invalid service data", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Initialize UI safely
            initializeUI()

        } catch (e: Exception) {
            Log.e("BookingPage", "Error in onCreate", e)
            Toast.makeText(this, "Failed to load booking page: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeUI() {
        try {
            // Update basic UI elements
            findViewById<TextView>(R.id.tvServiceName)?.text = serviceName
            findViewById<TextView>(R.id.tvPrice)?.text = "From AED $basePrice"

            // Set up basic click listeners first
            setupBasicClickListeners()

            // Initialize date UI
            updateDateUI()

            // Load user's saved location if any
            loadSavedLocation()

            // Initialize bottom sheets
            setupLocationBottomSheet()
            setupAddLocationBottomSheet()
            setupVehicleBottomSheet()
            setupConfirmBookingBottomSheet()
            setupSuccessBookingBottomSheet()

        } catch (e: Exception) {
            Log.e("BookingPage", "Error initializing UI", e)
            Toast.makeText(this, "UI initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBasicClickListeners() {
        try {
            // Back button
            findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
                finish()
            }

            // Book now button - now shows confirmation dialog
            findViewById<CardView>(R.id.cardBookNow)?.setOnClickListener {
                handleBookNowClick()
            }

            // Date selection buttons
            findViewById<CardView>(R.id.cardToday)?.setOnClickListener {
                selectedDateOption = 0
                selectedDate = Calendar.getInstance()
                updateDateUI()
            }

            findViewById<CardView>(R.id.cardTomorrow)?.setOnClickListener {
                selectedDateOption = 1
                selectedDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                updateDateUI()
            }

            findViewById<CardView>(R.id.cardThisWeek)?.setOnClickListener {
                selectedDateOption = 2
                selectedDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }
                updateDateUI()
            }

            findViewById<CardView>(R.id.cardCalender)?.setOnClickListener {
                showDatePickerDialog()
            }

            // Vehicle selection
            findViewById<CardView>(R.id.cardSelectVehicle)?.setOnClickListener {
                vehicleBottomSheet?.show()
            }

            // Location selection
            findViewById<CardView>(R.id.cardSelectLocation)?.setOnClickListener {
                locationBottomSheet?.show()
            }

        } catch (e: Exception) {
            Log.e("BookingPage", "Error setting up click listeners", e)
        }
    }

    private fun setupConfirmBookingBottomSheet() {
        try {
            confirmBookingBottomSheet = BottomSheetDialog(this)
            val confirmView = layoutInflater.inflate(R.layout.booking_confirmation_bottom_sheet, null)
            confirmBookingBottomSheet?.setContentView(confirmView)

            val btnCancelBooking = confirmView.findViewById<MaterialButton>(R.id.btnCancelBooking)
            val btnConfirmBooking = confirmView.findViewById<MaterialButton>(R.id.btnConfirmBooking)

            btnCancelBooking?.setOnClickListener {
                confirmBookingBottomSheet?.dismiss()
            }

            btnConfirmBooking?.setOnClickListener {
                confirmBookingBottomSheet?.dismiss()
                createBookingDirectly()
            }

        } catch (e: Exception) {
            Log.e("BookingPage", "Error setting up confirm booking bottom sheet", e)
        }
    }

    private fun setupSuccessBookingBottomSheet() {
        try {
            successBookingBottomSheet = BottomSheetDialog(this)
            val successView = layoutInflater.inflate(R.layout.booking_success_bottom_sheet, null)
            successBookingBottomSheet?.setContentView(successView)

            val btnDoneBooking = successView.findViewById<MaterialButton>(R.id.btnDoneBooking)

            btnDoneBooking?.setOnClickListener {
                successBookingBottomSheet?.dismiss()
                // Navigate back to main activity
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("SHOW_BOOKINGS", true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }

        } catch (e: Exception) {
            Log.e("BookingPage", "Error setting up success booking bottom sheet", e)
        }
    }

    private fun setupLocationBottomSheet() {
        try {
            locationBottomSheet = BottomSheetDialog(this)
            val locationView = layoutInflater.inflate(R.layout.location_bottom_sheet, null)
            locationBottomSheet?.setContentView(locationView)

            // Setup click listeners for location options
            val currentLocationItem = locationView.findViewById<View>(R.id.currentLocationItem)
            val homeLocationItem = locationView.findViewById<View>(R.id.homeLocationItem)
            val addWorkItem = locationView.findViewById<View>(R.id.addWorkItem)
            val selectButton = locationView.findViewById<View>(R.id.selectButton)
            val cancelButton = locationView.findViewById<TextView>(R.id.cancelButton)
            val addNewButton = locationView.findViewById<TextView>(R.id.addNewButton)

            // Update UI with saved locations
            val currentLocationText = locationView.findViewById<TextView>(R.id.currentLocationText)
            val homeLocationText = locationView.findViewById<TextView>(R.id.homeLocationText)
            val workLocationText = locationView.findViewById<TextView>(R.id.workLocationText)
            val workLocationContainer = locationView.findViewById<View>(R.id.workLocationContainer)
            val workTitleText = locationView.findViewById<TextView>(R.id.workTitleText)

            currentLocationText?.text = prefsHelper.getCurrentLocation().ifEmpty { "No location set" }
            homeLocationText?.text = prefsHelper.getHomeLocation().ifEmpty { "No home location set" }

            val savedWorkLocation = prefsHelper.getWorkLocation()
            if (savedWorkLocation.isNotEmpty()) {
                workLocationContainer?.visibility = View.VISIBLE
                workLocationText?.text = savedWorkLocation
                addWorkItem?.visibility = View.GONE
            } else {
                workLocationContainer?.visibility = View.GONE
                addWorkItem?.visibility = View.VISIBLE
                workTitleText?.text = "Add work"
            }

            currentLocationItem?.setOnClickListener {
                if (prefsHelper.getCurrentLocation().isNotEmpty()) {
                    updateLocation("Current", prefsHelper.getCurrentLocation())
                } else {
                    showAddLocationBottomSheet("Current")
                }
                locationBottomSheet?.dismiss()
            }

            homeLocationItem?.setOnClickListener {
                if (prefsHelper.getHomeLocation().isNotEmpty()) {
                    updateLocation("Home", prefsHelper.getHomeLocation())
                } else {
                    showAddLocationBottomSheet("Home")
                }
                locationBottomSheet?.dismiss()
            }

            addWorkItem?.setOnClickListener {
                showAddLocationBottomSheet("Work")
                locationBottomSheet?.dismiss()
            }

            workLocationContainer?.setOnClickListener {
                updateLocation("Work", prefsHelper.getWorkLocation())
                locationBottomSheet?.dismiss()
            }

            selectButton?.setOnClickListener { locationBottomSheet?.dismiss() }
            cancelButton?.setOnClickListener { locationBottomSheet?.dismiss() }
            addNewButton?.setOnClickListener {
                showAddLocationBottomSheet(null)
                locationBottomSheet?.dismiss()
            }

        } catch (e: Exception) {
            Log.e("BookingPage", "Error setting up location bottom sheet", e)
        }
    }

    private fun setupAddLocationBottomSheet() {
        try {
            addLocationBottomSheet = BottomSheetDialog(this)
            val addLocationView = layoutInflater.inflate(R.layout.add_location_bottom_sheet, null)
            addLocationBottomSheet?.setContentView(addLocationView)

            val cancelButton = addLocationView.findViewById<TextView>(R.id.cancelAddLocationButton)
            val saveButton = addLocationView.findViewById<CardView>(R.id.saveButton)
            val saveTopButton = addLocationView.findViewById<TextView>(R.id.saveLocationButton)
            val locationTypeGroup = addLocationView.findViewById<RadioGroup>(R.id.locationTypeGroup)
            val addressInput = addLocationView.findViewById<EditText>(R.id.addressInput)
            val cityInput = addLocationView.findViewById<EditText>(R.id.cityInput)
            val countryInput = addLocationView.findViewById<EditText>(R.id.countryInput)

            countryInput?.setText("U.A.E")

            val saveAction = saveAction@{
                val address = addressInput?.text.toString().trim()
                val city = cityInput?.text.toString().trim()
                val country = countryInput?.text.toString().trim()

                if (address.isNullOrEmpty() || city.isNullOrEmpty() || country.isNullOrEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@saveAction
                }

                val fullAddress = if (address.contains(city)) {
                    "$address · $country"
                } else {
                    "$address · $city, $country"
                }

                val selectedLocationType = when (locationTypeGroup?.checkedRadioButtonId) {
                    R.id.radioHome -> "Home"
                    R.id.radioWork -> "Work"
                    R.id.radioCurrent -> "Current"
                    else -> "Current"
                }

                prefsHelper.saveLocation(selectedLocationType, fullAddress)
                updateLocation(selectedLocationType, fullAddress)

                Toast.makeText(this, "$selectedLocationType location saved", Toast.LENGTH_SHORT).show()
                addLocationBottomSheet?.dismiss()
                setupLocationBottomSheet()
            }

            cancelButton?.setOnClickListener { addLocationBottomSheet?.dismiss() }
            saveButton?.setOnClickListener { saveAction() }
            saveTopButton?.setOnClickListener { saveAction() }

        } catch (e: Exception) {
            Log.e("BookingPage", "Error setting up add location bottom sheet", e)
        }
    }

    private fun setupVehicleBottomSheet() {
        try {
            vehicleBottomSheet = BottomSheetDialog(this)
            val vehicleView = layoutInflater.inflate(R.layout.vehicle_bottom_sheet, null)
            vehicleBottomSheet?.setContentView(vehicleView)

            val cancelButton = vehicleView.findViewById<TextView>(R.id.cancelButton)
            val addNewButton = vehicleView.findViewById<TextView>(R.id.addNewButton)
            val selectButton = vehicleView.findViewById<MaterialButton>(R.id.selectButton)
            val vehicleListContainer = vehicleView.findViewById<LinearLayout>(R.id.vehicleListContainer)
            val emptyStateView = vehicleView.findViewById<TextView>(R.id.emptyStateText)

            // Function to refresh the vehicle list
            fun refreshVehicleList() {
                vehicleListContainer?.removeAllViews()
                val vehicles = prefsHelper.getAllVehicles()

                if (vehicles.isEmpty()) {
                    emptyStateView?.visibility = View.VISIBLE
                    vehicleListContainer?.visibility = View.GONE
                } else {
                    emptyStateView?.visibility = View.GONE
                    vehicleListContainer?.visibility = View.VISIBLE

                    vehicles.forEachIndexed { index, vehicle ->
                        val vehicleItemView = layoutInflater.inflate(R.layout.item_vehicle, null)

                        vehicleItemView.findViewById<TextView>(R.id.vehicleBrandModel)?.text = "${vehicle.brand} ${vehicle.model}"
                        vehicleItemView.findViewById<TextView>(R.id.vehiclePlate)?.text = vehicle.plateNumber

                        if (index == selectedVehicleIndex) {
                            vehicleItemView.setBackgroundResource(R.drawable.selected_vehicle_background)
                        }

                        vehicleItemView.setOnClickListener {
                            selectedVehicleIndex = index
                            refreshVehicleList() // Refresh to update selection
                        }

                        vehicleListContainer?.addView(vehicleItemView)

                        // Add divider between vehicles
                        if (index < vehicles.size - 1) {
                            val divider = View(this)
                            divider.layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                1
                            ).apply {
                                setMargins(0, 16.dpToPx(), 0, 16.dpToPx())
                            }
                            divider.setBackgroundColor(
                                ContextCompat.getColor(this@BookingPage, R.color.divider_gray)
                            )
                            vehicleListContainer?.addView(divider)
                        }
                    }
                }
            }

            // Initial load
            refreshVehicleList()

            addNewButton?.setOnClickListener {
                startActivity(Intent(this@BookingPage, AddVehicleActivity::class.java))
                vehicleBottomSheet?.dismiss()
            }

            cancelButton?.setOnClickListener {
                vehicleBottomSheet?.dismiss()
            }

            selectButton?.setOnClickListener {
                val vehicles = prefsHelper.getAllVehicles()
                if (selectedVehicleIndex in 0 until vehicles.size) {
                    val selectedVehicle = vehicles[selectedVehicleIndex]
                    updateSelectedVehicle(selectedVehicle)
                    Toast.makeText(this@BookingPage,
                        "Selected: ${selectedVehicle.brand} ${selectedVehicle.model}",
                        Toast.LENGTH_SHORT).show()
                    vehicleBottomSheet?.dismiss()
                } else if (vehicles.isNotEmpty()) {
                    // Auto-select first vehicle if none selected
                    selectedVehicleIndex = 0
                    refreshVehicleList()
                    updateSelectedVehicle(vehicles[0])
                    Toast.makeText(this@BookingPage,
                        "Selected: ${vehicles[0].brand} ${vehicles[0].model}",
                        Toast.LENGTH_SHORT).show()
                    vehicleBottomSheet?.dismiss()
                } else {
                    Toast.makeText(this@BookingPage, "No vehicles available", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            Log.e("BookingPage", "Error setting up vehicle bottom sheet", e)
        }
    }

    private fun showAddLocationBottomSheet(locationType: String?) {
        try {
            if (addLocationBottomSheet == null) {
                setupAddLocationBottomSheet()
            }

            val radioHome = addLocationBottomSheet?.findViewById<RadioButton>(R.id.radioHome)
            val radioWork = addLocationBottomSheet?.findViewById<RadioButton>(R.id.radioWork)
            val radioCurrent = addLocationBottomSheet?.findViewById<RadioButton>(R.id.radioCurrent)
            val addressInput = addLocationBottomSheet?.findViewById<EditText>(R.id.addressInput)
            val cityInput = addLocationBottomSheet?.findViewById<EditText>(R.id.cityInput)

            when (locationType) {
                "Home" -> radioHome?.isChecked = true
                "Work" -> radioWork?.isChecked = true
                "Current" -> radioCurrent?.isChecked = true
            }

            addressInput?.setText("")
            cityInput?.setText("")
            addLocationBottomSheet?.show()

        } catch (e: Exception) {
            Log.e("BookingPage", "Error showing add location bottom sheet", e)
        }
    }

    private fun loadSavedLocation() {
        try {
            val savedLocation = prefsHelper.getSelectedAddress()
            if (savedLocation.isNotEmpty() && savedLocation != "Dubai, U.A.E") {
                findViewById<TextView>(R.id.tvHome)?.text = "Saved Location"
                findViewById<TextView>(R.id.tvHomeAddress)?.text = savedLocation
            } else {
                findViewById<TextView>(R.id.tvHome)?.text = "Location"
                findViewById<TextView>(R.id.tvHomeAddress)?.text = "Dubai, U.A.E"
            }
        } catch (e: Exception) {
            Log.e("BookingPage", "Error loading saved location", e)
        }
    }

    private fun handleBookNowClick() {
        try {
            Log.d("BookingPage", "Book Now clicked")

            // Basic validation
            if (selectedVehicleIndex == -1) {
                Toast.makeText(this, "Please select a vehicle first", Toast.LENGTH_SHORT).show()
                vehicleBottomSheet?.show()
                return
            }

            val selectedLocation = findViewById<TextView>(R.id.tvHomeAddress)?.text?.toString()
            if (selectedLocation.isNullOrEmpty() || selectedLocation == "Select location") {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show()
                locationBottomSheet?.show()
                return
            }

            // Show confirmation dialog instead of creating booking directly
            showBookingConfirmation()

        } catch (e: Exception) {
            Log.e("BookingPage", "Error handling book now click", e)
            Toast.makeText(this, "Booking failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showBookingConfirmation() {
        try {
            val vehicles = prefsHelper.getAllVehicles()
            if (selectedVehicleIndex >= vehicles.size) {
                Toast.makeText(this, "Invalid vehicle selection", Toast.LENGTH_SHORT).show()
                return
            }

            val selectedVehicle = vehicles[selectedVehicleIndex]
            val selectedDateText = findViewById<TextView>(R.id.tvChooseFromCalender)?.text?.toString() ?: getFormattedDate(selectedDate)
            val selectedLocation = findViewById<TextView>(R.id.tvHomeAddress)?.text?.toString() ?: "Dubai, U.A.E"

            // Update confirmation dialog with booking details
            val confirmView = confirmBookingBottomSheet?.findViewById<View>(android.R.id.content)
            confirmView?.findViewById<TextView>(R.id.tvConfirmServiceName)?.text = serviceName
            confirmView?.findViewById<TextView>(R.id.tvConfirmVehicle)?.text = "${selectedVehicle.brand} ${selectedVehicle.model} (${selectedVehicle.plateNumber})"
            confirmView?.findViewById<TextView>(R.id.tvConfirmDate)?.text = selectedDateText
            confirmView?.findViewById<TextView>(R.id.tvConfirmLocation)?.text = selectedLocation
            confirmView?.findViewById<TextView>(R.id.tvConfirmPrice)?.text = "AED $basePrice"

            // Show confirmation dialog
            confirmBookingBottomSheet?.show()

        } catch (e: Exception) {
            Log.e("BookingPage", "Error showing booking confirmation", e)
            Toast.makeText(this, "Failed to show confirmation: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createBookingDirectly() {
        try {
            val vehicles = prefsHelper.getAllVehicles()
            if (selectedVehicleIndex >= vehicles.size) {
                Toast.makeText(this, "Invalid vehicle selection", Toast.LENGTH_SHORT).show()
                return
            }

            val selectedVehicle = vehicles[selectedVehicleIndex]
            val selectedDateText = findViewById<TextView>(R.id.tvChooseFromCalender)?.text?.toString() ?: getFormattedDate(selectedDate)
            val selectedLocation = findViewById<TextView>(R.id.tvHomeAddress)?.text?.toString() ?: "Dubai, U.A.E"

            // Create booking object
            val booking = PrefsHelper.Booking(
                serviceId = serviceId,
                serviceName = serviceName,
                vehicleBrand = selectedVehicle.brand,
                vehicleModel = selectedVehicle.model,
                plateNumber = selectedVehicle.plateNumber,
                bookingDate = selectedDateText,
                location = selectedLocation,
                price = basePrice
            )

            // Save booking
            prefsHelper.saveBooking(booking)

            Log.d("BookingPage", "Booking saved successfully")

            // Show success dialog instead of toast and navigation
            successBookingBottomSheet?.show()

        } catch (e: Exception) {
            Log.e("BookingPage", "Error creating booking", e)
            Toast.makeText(this, "Failed to create booking: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateSelectedVehicle(vehicle: Vehicle) {
        try {
            findViewById<TextView>(R.id.tvVehicleName)?.text = "${vehicle.brand} ${vehicle.model}"
            findViewById<TextView>(R.id.tvVehicleNumber)?.text = vehicle.plateNumber
        } catch (e: Exception) {
            Log.e("BookingPage", "Error updating selected vehicle", e)
        }
    }

    private fun updateLocation(locationType: String, address: String) {
        try {
            findViewById<TextView>(R.id.tvHome)?.text = locationType
            findViewById<TextView>(R.id.tvHomeAddress)?.text = address
            prefsHelper.saveLocation(locationType, address)
        } catch (e: Exception) {
            Log.e("BookingPage", "Error updating location", e)
        }
    }

    private fun updateDateUI() {
        try {
            // Get references to all date option TextViews
            val tvToday = findViewById<TextView>(R.id.tvToday)
            val tvTomorrow = findViewById<TextView>(R.id.tvTomorrow)
            val tvThisWeek = findViewById<TextView>(R.id.tvThisWeek)

            // Reset all options to normal state
            tvToday?.apply {
                background = ContextCompat.getDrawable(this@BookingPage, R.drawable.bg_date_option_normal)
                setTextColor(ContextCompat.getColor(this@BookingPage, R.color.gray))
            }

            tvTomorrow?.apply {
                background = ContextCompat.getDrawable(this@BookingPage, R.drawable.bg_date_option_normal)
                setTextColor(ContextCompat.getColor(this@BookingPage, R.color.gray))
            }

            tvThisWeek?.apply {
                background = ContextCompat.getDrawable(this@BookingPage, R.drawable.bg_date_option_normal)
                setTextColor(ContextCompat.getColor(this@BookingPage, R.color.gray))
            }

            // Apply selected state to the chosen option
            when (selectedDateOption) {
                0 -> tvToday?.apply {
                    background = ContextCompat.getDrawable(this@BookingPage, R.drawable.bg_date_option_selected)
                    setTextColor(ContextCompat.getColor(this@BookingPage, R.color.white))
                }
                1 -> tvTomorrow?.apply {
                    background = ContextCompat.getDrawable(this@BookingPage, R.drawable.bg_date_option_selected)
                    setTextColor(ContextCompat.getColor(this@BookingPage, R.color.white))
                }
                2 -> tvThisWeek?.apply {
                    background = ContextCompat.getDrawable(this@BookingPage, R.drawable.bg_date_option_selected)
                    setTextColor(ContextCompat.getColor(this@BookingPage, R.color.white))
                }
            }

            // Update calendar text based on selection
            val calendarText = when (selectedDateOption) {
                0 -> "Today, ${getFormattedDate(selectedDate)}"
                1 -> "Tomorrow, ${getFormattedDate(selectedDate)}"
                2 -> "This week, ${getFormattedDate(selectedDate)}"
                else -> getFullFormattedDate(selectedDate)
            }
            findViewById<TextView>(R.id.tvChooseFromCalender)?.text = calendarText

        } catch (e: Exception) {
            Log.e("BookingPage", "Error updating date UI", e)
        }
    }

    private fun showDatePickerDialog() {
        try {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDateOption = 3 // Custom date
                    selectedDate = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    updateDateUI()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )

            // Set minimum date to today
            datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
            datePicker.show()
        } catch (e: Exception) {
            Log.e("BookingPage", "Error showing date picker", e)
        }
    }

    private fun getFormattedDate(calendar: Calendar): String {
        return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(calendar.time)
    }

    private fun getFullFormattedDate(calendar: Calendar): String {
        return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(calendar.time)
    }

    // Extension function for dp to px conversion
    private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    override fun onBackPressed() {
        try {
            super.onBackPressed()
        } catch (e: Exception) {
            Log.e("BookingPage", "Error in onBackPressed", e)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh vehicle list when returning from AddVehicleActivity
        if (vehicleBottomSheet != null) {
            setupVehicleBottomSheet()
        }
    }
}