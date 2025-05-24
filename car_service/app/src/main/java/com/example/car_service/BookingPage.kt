package com.example.car_service

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class BookingPage : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var locationBottomSheet: BottomSheetDialog
    private lateinit var addLocationBottomSheet: BottomSheetDialog
    private lateinit var vehicleBottomSheet: BottomSheetDialog
    private lateinit var bookingConfirmationBottomSheet: BottomSheetDialog
    private lateinit var bookingSuccessBottomSheet: BottomSheetDialog
    private var selectedVehicleIndex = -1
    private var selectedDateOption = 1 // 0: Today, 1: Tomorrow, 2: This week, 3: Custom
    private var selectedDate: Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_page)

        // Initialize SharedPreferences helper
        prefsHelper = PrefsHelper(this)

        // Get data from Intent
        val serviceId = intent.getStringExtra("service_id") ?: ""
        val serviceName = intent.getStringExtra("service_name") ?: "Service"
        val basePrice = intent.getIntExtra("base_price", 0)

        // Update UI
        findViewById<TextView>(R.id.tvServiceName).text = serviceName
        findViewById<TextView>(R.id.tvPrice).text = "From AED $basePrice"

        // Initialize bottom sheets
        setupLocationBottomSheet()
        setupAddLocationBottomSheet()
        setupVehicleBottomSheet()
        setupBookingConfirmationBottomSheet()
        setupBookingSuccessBottomSheet()

        // Set click listeners
        findViewById<CardView>(R.id.cardSelectVehicle).setOnClickListener {
            vehicleBottomSheet.show()
        }

        findViewById<CardView>(R.id.cardSelectLocation).setOnClickListener {
            locationBottomSheet.show()
        }

        // Set back button click listener
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        // Set book now button click listener
        findViewById<CardView>(R.id.cardBookNow).setOnClickListener {
            showBookingConfirmation()
        }

        // Initialize date UI
        updateDateUI()

        // Set click listeners for date options
        findViewById<CardView>(R.id.cardToday).setOnClickListener {
            selectedDateOption = 0
            selectedDate = Calendar.getInstance()
            updateDateUI()
        }

        findViewById<CardView>(R.id.cardTomorrow).setOnClickListener {
            selectedDateOption = 1
            selectedDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            updateDateUI()
        }

        findViewById<CardView>(R.id.cardThisWeek).setOnClickListener {
            selectedDateOption = 2
            selectedDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) } // Example: 3 days later
            updateDateUI()
        }

        findViewById<CardView>(R.id.cardCalender).setOnClickListener {
            showDatePickerDialog()
        }

        // Handle booking logic based on serviceId
        when (serviceId) {
            ServiceConstants.BATTERY_CHANGE -> setupBatteryBooking()
            ServiceConstants.BRAKE_SERVICE -> setupBrakeServiceBooking()
            // Add other services...
        }
    }

    private fun setupBookingConfirmationBottomSheet() {
        bookingConfirmationBottomSheet = BottomSheetDialog(this)
        val confirmationView = layoutInflater.inflate(R.layout.booking_confirmation_bottom_sheet, null)
        bookingConfirmationBottomSheet.setContentView(confirmationView)

        // Get references to views
        val btnCancelBooking = confirmationView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelBooking)
        val btnConfirmBooking = confirmationView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmBooking)

        // Set click listeners
        btnCancelBooking.setOnClickListener {
            bookingConfirmationBottomSheet.dismiss()
        }

        btnConfirmBooking.setOnClickListener {
            bookingConfirmationBottomSheet.dismiss()
            confirmBooking()
        }

        // Make it non-cancelable by touch outside
        bookingConfirmationBottomSheet.setCanceledOnTouchOutside(false)
    }

    private fun setupBookingSuccessBottomSheet() {
        bookingSuccessBottomSheet = BottomSheetDialog(this)
        val successView = layoutInflater.inflate(R.layout.booking_success_bottom_sheet, null)
        bookingSuccessBottomSheet.setContentView(successView)

        // Get reference to done button
        val btnDoneBooking = successView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDoneBooking)

        // Set click listener
        btnDoneBooking.setOnClickListener {
            bookingSuccessBottomSheet.dismiss()
            // Navigate back to main activity or finish this activity
            finish()
        }

        // Make it non-cancelable by touch outside
        bookingSuccessBottomSheet.setCanceledOnTouchOutside(false)
    }

    private fun showBookingConfirmation() {
        // Validate selections
        if (selectedVehicleIndex == -1) {
            Toast.makeText(this, "Please select a vehicle", Toast.LENGTH_SHORT).show()
            return
        }

        if (findViewById<TextView>(R.id.tvHomeAddress).text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            return
        }

        // Get selected vehicle
        val vehicles = prefsHelper.getAllVehicles()
        if (selectedVehicleIndex !in vehicles.indices) {
            Toast.makeText(this, "Invalid vehicle selection", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedVehicle = vehicles[selectedVehicleIndex]

        // Get service details from intent
        val serviceName = intent.getStringExtra("service_name") ?: "Service"
        val basePrice = intent.getIntExtra("base_price", 0)

        // Get selected date and location
        val selectedDateText = findViewById<TextView>(R.id.tvChooseFromCalender).text.toString()
        val selectedLocation = findViewById<TextView>(R.id.tvHomeAddress).text.toString()

        // Update confirmation dialog with booking details
        val confirmationView = bookingConfirmationBottomSheet.findViewById<View>(android.R.id.content)
        confirmationView?.let { view ->
            view.findViewById<TextView>(R.id.tvConfirmServiceName)?.text = serviceName
            view.findViewById<TextView>(R.id.tvConfirmVehicle)?.text = "${selectedVehicle.brand} ${selectedVehicle.model} (${selectedVehicle.plateNumber})"
            view.findViewById<TextView>(R.id.tvConfirmDate)?.text = selectedDateText
            view.findViewById<TextView>(R.id.tvConfirmLocation)?.text = selectedLocation
            view.findViewById<TextView>(R.id.tvConfirmPrice)?.text = "AED $basePrice"
        }

        // Show confirmation dialog
        bookingConfirmationBottomSheet.show()
    }

    private fun confirmBooking() {
        // Get selected vehicle
        val vehicles = prefsHelper.getAllVehicles()
        val selectedVehicle = vehicles[selectedVehicleIndex]

        // Get service details from intent
        val serviceId = intent.getStringExtra("service_id") ?: ""
        val serviceName = intent.getStringExtra("service_name") ?: "Service"
        val basePrice = intent.getIntExtra("base_price", 0)

        // Get selected date and location
        val selectedDateText = findViewById<TextView>(R.id.tvChooseFromCalender).text.toString()
        val selectedLocation = findViewById<TextView>(R.id.tvHomeAddress).text.toString()

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

        // Show success dialog
        showBookingSuccess()
    }

    private fun showBookingSuccess() {
        bookingSuccessBottomSheet.show()
    }

    // ... (rest of the existing methods remain the same)

    private fun updateDateUI() {
        // Reset all options to normal state
        findViewById<TextView>(R.id.tvToday).apply {
            setBackgroundResource(R.drawable.bg_date_option_normal)
            setTextColor(ContextCompat.getColor(this@BookingPage, R.color.gray))
        }
        findViewById<TextView>(R.id.tvTomorrow).apply {
            setBackgroundResource(R.drawable.bg_date_option_normal)
            setTextColor(ContextCompat.getColor(this@BookingPage, R.color.gray))
        }
        findViewById<TextView>(R.id.tvThisWeek).apply {
            setBackgroundResource(R.drawable.bg_date_option_normal)
            setTextColor(ContextCompat.getColor(this@BookingPage, R.color.gray))
        }

        // Highlight selected option
        when (selectedDateOption) {
            0 -> findViewById<TextView>(R.id.tvToday).apply {
                setBackgroundResource(R.drawable.bg_date_option_selected)
                setTextColor(ContextCompat.getColor(this@BookingPage, R.color.white))
            }
            1 -> findViewById<TextView>(R.id.tvTomorrow).apply {
                setBackgroundResource(R.drawable.bg_date_option_selected)
                setTextColor(ContextCompat.getColor(this@BookingPage, R.color.white))
            }
            2 -> findViewById<TextView>(R.id.tvThisWeek).apply {
                setBackgroundResource(R.drawable.bg_date_option_selected)
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
        findViewById<TextView>(R.id.tvChooseFromCalender).text = calendarText
    }

    private fun showDatePickerDialog() {
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
    }

    private fun getFormattedDate(calendar: Calendar): String {
        return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(calendar.time)
    }

    private fun getFullFormattedDate(calendar: Calendar): String {
        return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(calendar.time)
    }

    private fun setupLocationBottomSheet() {
        locationBottomSheet = BottomSheetDialog(this)
        val locationView = layoutInflater.inflate(R.layout.location_bottom_sheet, null)
        locationBottomSheet.setContentView(locationView)

        // Initialize views
        val currentLocationItem = locationView.findViewById<View>(R.id.currentLocationItem)
        val homeLocationItem = locationView.findViewById<View>(R.id.homeLocationItem)
        val addWorkItem = locationView.findViewById<View>(R.id.addWorkItem)
        val workLocationContainer = locationView.findViewById<View>(R.id.workLocationContainer)
        val selectButton = locationView.findViewById<View>(R.id.selectButton)
        val cancelButton = locationView.findViewById<TextView>(R.id.cancelButton)
        val addNewButton = locationView.findViewById<TextView>(R.id.addNewButton)
        val currentLocationText = locationView.findViewById<TextView>(R.id.currentLocationText)
        val homeLocationText = locationView.findViewById<TextView>(R.id.homeLocationText)
        val workLocationText = locationView.findViewById<TextView>(R.id.workLocationText)
        val workTitleText = locationView.findViewById<TextView>(R.id.workTitleText)

        // Update UI with saved locations
        currentLocationText.text = prefsHelper.getCurrentLocation().ifEmpty { "No location set" }
        homeLocationText.text = prefsHelper.getHomeLocation().ifEmpty { "No home location set" }

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

        // Set click listeners
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
            if (prefsHelper.getWorkLocation().isNotEmpty()) {
                updateLocation("Work", prefsHelper.getWorkLocation())
            }
            locationBottomSheet.dismiss()
        }

        selectButton.setOnClickListener { locationBottomSheet.dismiss() }
        cancelButton.setOnClickListener { locationBottomSheet.dismiss() }
        addNewButton.setOnClickListener {
            showAddLocationBottomSheet(null)
            locationBottomSheet.dismiss()
        }
    }

    private fun setupAddLocationBottomSheet() {
        addLocationBottomSheet = BottomSheetDialog(this)
        val addLocationView = layoutInflater.inflate(R.layout.add_location_bottom_sheet, null)
        addLocationBottomSheet.setContentView(addLocationView)

        // Initialize views
        val cancelButton = addLocationView.findViewById<TextView>(R.id.cancelAddLocationButton)
        val saveButton = addLocationView.findViewById<CardView>(R.id.saveButton)
        val saveTopButton = addLocationView.findViewById<TextView>(R.id.saveLocationButton)
        val locationTypeGroup = addLocationView.findViewById<RadioGroup>(R.id.locationTypeGroup)
        val addressInput = addLocationView.findViewById<EditText>(R.id.addressInput)
        val cityInput = addLocationView.findViewById<EditText>(R.id.cityInput)
        val countryInput = addLocationView.findViewById<EditText>(R.id.countryInput)

        // Set default country
        countryInput.setText("U.A.E")

        // Save action
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

            prefsHelper.saveLocation(selectedLocationType, fullAddress)
            updateLocation(selectedLocationType, fullAddress)

            Toast.makeText(this, "$selectedLocationType location saved", Toast.LENGTH_SHORT).show()
            addLocationBottomSheet.dismiss()
            setupLocationBottomSheet() // Refresh location bottom sheet
        }

        // Set click listeners
        cancelButton.setOnClickListener { addLocationBottomSheet.dismiss() }
        saveButton.setOnClickListener { saveAction() }
        saveTopButton.setOnClickListener { saveAction() }
    }

    private fun setupVehicleBottomSheet() {
        vehicleBottomSheet = BottomSheetDialog(this)
        val vehicleView = layoutInflater.inflate(R.layout.vehicle_bottom_sheet, null)
        vehicleBottomSheet.setContentView(vehicleView)

        // Initialize views
        val cancelButton = vehicleView.findViewById<TextView>(R.id.cancelButton)
        val addNewButton = vehicleView.findViewById<TextView>(R.id.addNewButton)
        val selectButton = vehicleView.findViewById<CardView>(R.id.selectButton)
        val vehicleListContainer = vehicleView.findViewById<LinearLayout>(R.id.vehicleListContainer)
        val emptyStateView = vehicleView.findViewById<TextView>(R.id.emptyStateText)

        // Function to refresh vehicle list
        fun refreshVehicleList() {
            vehicleListContainer.removeAllViews()
            val vehicles = prefsHelper.getAllVehicles()

            if (vehicles.isEmpty()) {
                emptyStateView.visibility = View.VISIBLE
                vehicleListContainer.visibility = View.GONE
            } else {
                emptyStateView.visibility = View.GONE
                vehicleListContainer.visibility = View.VISIBLE

                vehicles.forEachIndexed { index, vehicle ->
                    val vehicleItemView = layoutInflater.inflate(R.layout.item_vehicle, null)

                    // Set vehicle details
                    vehicleItemView.findViewById<TextView>(R.id.vehicleBrandModel).text =
                        "${vehicle.brand} ${vehicle.model}"
                    vehicleItemView.findViewById<TextView>(R.id.vehiclePlate).text = vehicle.plateNumber

                    // Highlight selected vehicle
                    if (index == selectedVehicleIndex) {
                        vehicleItemView.setBackgroundColor(
                            ContextCompat.getColor(this, R.color.selected_item_background)
                        )
                    }

                    vehicleItemView.setOnClickListener {
                        selectedVehicleIndex = index
                        refreshVehicleList() // Refresh to update selection
                    }

                    vehicleListContainer.addView(vehicleItemView)

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
                            ContextCompat.getColor(this, R.color.divider_gray)
                        )
                        vehicleListContainer.addView(divider)
                    }
                }
            }
        }

        // Initial load
        refreshVehicleList()

        // Set click listeners
        addNewButton.setOnClickListener {
            startActivity(Intent(this, AddVehicleActivity::class.java))
            vehicleBottomSheet.dismiss()
        }

        cancelButton.setOnClickListener {
            vehicleBottomSheet.dismiss()
        }

        selectButton.setOnClickListener {
            val vehicles = prefsHelper.getAllVehicles()
            if (selectedVehicleIndex in 0 until vehicles.size) {
                val selectedVehicle = vehicles[selectedVehicleIndex]
                updateSelectedVehicle(selectedVehicle)
                vehicleBottomSheet.dismiss()
            } else if (vehicles.isNotEmpty()) {
                // Auto-select first vehicle if none selected
                selectedVehicleIndex = 0
                refreshVehicleList()
                updateSelectedVehicle(vehicles[0])
                vehicleBottomSheet.dismiss()
            } else {
                Toast.makeText(this, "No vehicles available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSelectedVehicle(vehicle: PrefsHelper.Vehicle) {
        findViewById<TextView>(R.id.tvVehicleName).text = "${vehicle.brand} ${vehicle.model}"
        findViewById<TextView>(R.id.tvVehicleNumber).text = vehicle.plateNumber
    }

    private fun updateLocation(locationType: String, address: String) {
        findViewById<TextView>(R.id.tvHome).text = locationType
        findViewById<TextView>(R.id.tvHomeAddress).text = address
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

        // Set location type
        when (locationType) {
            "Home" -> radioHome?.isChecked = true
            "Work" -> radioWork?.isChecked = true
            "Current" -> radioCurrent?.isChecked = true
            else -> {
                radioHome?.isChecked = false
                radioWork?.isChecked = false
                radioCurrent?.isChecked = false
            }
        }

        // Clear inputs
        addressInput?.setText("")
        cityInput?.setText("")

        addLocationBottomSheet.show()
    }

    private fun setupBatteryBooking() {
        // Battery-specific setup
    }

    private fun setupBrakeServiceBooking() {
        // Brake-service-specific setup
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onBackPressed() {
        when {
            this::bookingSuccessBottomSheet.isInitialized && bookingSuccessBottomSheet.isShowing ->
                bookingSuccessBottomSheet.dismiss()
            this::bookingConfirmationBottomSheet.isInitialized && bookingConfirmationBottomSheet.isShowing ->
                bookingConfirmationBottomSheet.dismiss()
            this::locationBottomSheet.isInitialized && locationBottomSheet.isShowing ->
                locationBottomSheet.dismiss()
            this::addLocationBottomSheet.isInitialized && addLocationBottomSheet.isShowing ->
                addLocationBottomSheet.dismiss()
            this::vehicleBottomSheet.isInitialized && vehicleBottomSheet.isShowing ->
                vehicleBottomSheet.dismiss()
            else -> super.onBackPressed()
        }
    }
}