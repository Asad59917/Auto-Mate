package com.example.car_service

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private var bookings: List<PrefsHelper.Booking> = emptyList()
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvServiceName: TextView = itemView.findViewById(R.id.tvServiceName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvVehicleInfo: TextView = itemView.findViewById(R.id.tvVehicleInfo)
        val tvBookingDate: TextView = itemView.findViewById(R.id.tvBookingDate)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        with(holder) {
            tvServiceName.text = booking.serviceName
            tvPrice.text = "AED ${booking.price}"
            tvVehicleInfo.text = "${booking.vehicleBrand} ${booking.vehicleModel} - ${booking.plateNumber}"
            tvLocation.text = booking.location

            // Format the booking date
            try {
                val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(booking.bookingDate)
                tvBookingDate.text = date?.let { outputFormat.format(it) } ?: booking.bookingDate
            } catch (e: Exception) {
                tvBookingDate.text = booking.bookingDate
            }

            // Set status based on booking date or other logic
            tvStatus.text = getBookingStatus(booking)
            updateStatusAppearance(tvStatus, getBookingStatus(booking))
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<PrefsHelper.Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    private fun getBookingStatus(booking: PrefsHelper.Booking): String {
        // You can implement your own logic here
        // For now, we'll check if the booking date is today, future, or past
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val bookingDate = inputFormat.parse(booking.bookingDate)
            val today = Calendar.getInstance().time

            when {
                bookingDate == null -> "BOOKED"
                bookingDate.after(today) -> "UPCOMING"
                isSameDay(bookingDate, today) -> "TODAY"
                else -> "COMPLETED"
            }
        } catch (e: Exception) {
            "BOOKED"
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateStatusAppearance(textView: TextView, status: String) {
        val context = textView.context
        when (status) {
            "UPCOMING" -> {
                textView.setBackgroundColor(context.getColor(android.R.color.holo_blue_dark))
            }
            "TODAY" -> {
                textView.setBackgroundColor(context.getColor(android.R.color.holo_orange_dark))
            }
            "COMPLETED" -> {
                textView.setBackgroundColor(context.getColor(android.R.color.holo_green_dark))
            }
            else -> {
                textView.setBackgroundColor(context.getColor(android.R.color.holo_blue_dark))
            }
        }
    }
}