package com.example.scenex.views.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.scenex.databinding.ItemBookingCardBinding
import com.example.scenex.models.Booking
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(private val onDetailsClick: (Booking) -> Unit) :
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private var bookings: List<Booking> = emptyList()

    fun submitList(newList: List<Booking>) {
        bookings = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    inner class BookingViewHolder(private val binding: ItemBookingCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.txtProjectTitle.text = booking.projectTitle
            binding.txtRecruiterName.text = "by ${booking.recruiterName}"
            
            val sdf = SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault())
            val dateStr = sdf.format(Date(booking.date))
            binding.txtDateTime.text = dateStr

            binding.statusChip.text = booking.status.uppercase()
            updateStatusChipColor(booking.status)

            // Handling the conflict warning chip
            binding.chipConflict.visibility = if (booking.hasConflict) View.VISIBLE else View.GONE

            binding.btnViewDetails.setOnClickListener { onDetailsClick(booking) }
        }

        private fun updateStatusChipColor(status: String) {
            val color = when (status.uppercase()) {
                "PENDING" -> "#FF9800" // Orange
                "CONFIRMED" -> "#4CAF50" // Green
                "CANCELLED" -> "#F44336" // Red
                "COMPLETED" -> "#2196F3" // Blue
                else -> "#9E9E9E" // Grey
            }
            binding.statusChip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(color))
            binding.statusChip.setTextColor(Color.WHITE)
        }
    }
}
