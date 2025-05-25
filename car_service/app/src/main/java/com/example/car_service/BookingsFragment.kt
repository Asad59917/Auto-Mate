package com.example.car_service

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BookingFragment : Fragment() {

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var rvBookings: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var bookingAdapter: BookingAdapter

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
        bookingAdapter = BookingAdapter()
        rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
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