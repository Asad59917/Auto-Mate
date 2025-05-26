package com.example.car_service

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

class BookingFragment : Fragment() {

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var rvBookings: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var bookingAdapter: BookingAdapter
    private var cancelBookingBottomSheet: BottomSheetDialog? = null
    private var bookingToCancel: PrefsHelper.Booking? = null
    private var cancelPosition: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize PrefsHelper
        prefsHelper = PrefsHelper(requireContext())

        // Initialize views
        initViews(view)

        // Setup cancel dialog
        setupCancelDialog()

        // Setup RecyclerView
        setupRecyclerView()

        // Load bookings
        loadBookings()
    }

    override fun onResume() {
        super.onResume()
        // Refresh bookings when fragment becomes visible
        loadBookings()
    }

    private fun initViews(view: View) {
        rvBookings = view.findViewById(R.id.rvBookings)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
    }

    private fun setupRecyclerView() {
        // Pass the cancel callback to the adapter
        bookingAdapter = BookingAdapter(
            bookings = mutableListOf(),
            onCancelClick = { booking, position ->
                showCancelConfirmation(booking, position)
            }
        )

        rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun setupCancelDialog() {
        cancelBookingBottomSheet = BottomSheetDialog(requireContext())
        val cancelView = layoutInflater.inflate(R.layout.cancel_booking_bottom_sheet, null)
        cancelBookingBottomSheet?.setContentView(cancelView)

        val btnKeepBooking = cancelView.findViewById<MaterialButton>(R.id.btnKeepBooking)
        val btnConfirmCancel = cancelView.findViewById<MaterialButton>(R.id.btnConfirmCancel)

        btnKeepBooking?.setOnClickListener {
            cancelBookingBottomSheet?.dismiss()
        }

        btnConfirmCancel?.setOnClickListener {
            confirmCancelBooking()
        }
    }

    private fun showCancelConfirmation(booking: PrefsHelper.Booking, position: Int) {
        bookingToCancel = booking
        cancelPosition = position

        // Update dialog with booking details
        val cancelView = cancelBookingBottomSheet?.findViewById<View>(android.R.id.content)
        cancelView?.findViewById<TextView>(R.id.tvCancelServiceName)?.text = booking.serviceName
        cancelView?.findViewById<TextView>(R.id.tvCancelVehicleInfo)?.text =
            "${booking.vehicleBrand} ${booking.vehicleModel} - ${booking.plateNumber}"
        cancelView?.findViewById<TextView>(R.id.tvCancelDateTime)?.text = booking.bookingDate
        cancelView?.findViewById<TextView>(R.id.tvCancelLocation)?.text = booking.location

        cancelBookingBottomSheet?.show()
    }

    private fun confirmCancelBooking() {
        bookingToCancel?.let { booking ->
            val success = prefsHelper.cancelBooking(booking)

            if (success) {
                bookingAdapter.removeBooking(cancelPosition)
                Toast.makeText(requireContext(), "Booking cancelled successfully", Toast.LENGTH_SHORT).show()

                // Show empty state if no bookings left
                if (bookingAdapter.isEmpty()) {
                    showEmptyState()
                }
            } else {
                Toast.makeText(requireContext(), "Failed to cancel booking", Toast.LENGTH_SHORT).show()
            }
        }

        cancelBookingBottomSheet?.dismiss()
        bookingToCancel = null
        cancelPosition = -1
    }

    private fun loadBookings() {
        val bookings = prefsHelper.getAllBookings()

        if (bookings.isEmpty()) {
            showEmptyState()
        } else {
            showBookings(bookings)
        }
    }

    private fun showEmptyState() {
        rvBookings.visibility = View.GONE
        emptyStateContainer.visibility = View.VISIBLE
    }

    private fun showBookings(bookings: List<PrefsHelper.Booking>) {
        emptyStateContainer.visibility = View.GONE
        rvBookings.visibility = View.VISIBLE
        bookingAdapter.updateBookings(bookings)
    }

    // Public method to refresh bookings (can be called from other fragments/activities)
    fun refreshBookings() {
        loadBookings()
    }
}