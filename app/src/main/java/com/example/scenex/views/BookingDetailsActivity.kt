package com.example.scenex.views

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scenex.databinding.ActivityBookingDetailsBinding
import com.example.scenex.models.Booking
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookingDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingDetailsBinding
    private val db = FirebaseFirestore.getInstance()
    private var bookingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        bookingId = intent.getStringExtra("BOOKING_ID")
        
        if (bookingId != null) {
            fetchBookingDetails(bookingId!!)
        } else {
            Toast.makeText(this, "Error: Booking not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupButtons()
    }

    private fun fetchBookingDetails(id: String) {
        db.collection("bookings").document(id).get()
            .addOnSuccessListener { document ->
                val booking = document.toObject(Booking::class.java)
                if (booking != null) {
                    displayDetails(booking)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayDetails(booking: Booking) {
        binding.collapsingToolbar.title = booking.projectTitle
        binding.txtDetailProjectTitle.text = booking.projectTitle
        binding.txtDetailRecruiterName.text = booking.recruiterName
        
        val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        binding.txtDetailDate.text = sdf.format(Date(booking.date))
        binding.txtDetailTime.text = "${booking.startTime} - ${booking.endTime}"
        
        binding.txtDetailLocation.text = booking.location
        binding.detailStatusChip.text = booking.status
        
        // Show/Hide Conflict Warning if the card exists in layout
        binding.cardConflictWarning.visibility = if (booking.hasConflict) View.VISIBLE else View.GONE
        
        updateStatusUI(booking.status)
    }

    private fun updateStatusUI(status: String) {
        val color = when (status.uppercase()) {
            "PENDING" -> "#FF9800"
            "CONFIRMED" -> "#4CAF50"
            "CANCELLED" -> "#F44336"
            else -> "#9E9E9E"
        }
        binding.detailStatusChip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(color))
        binding.detailStatusChip.setTextColor(Color.WHITE)

        // Only show Actions if Pending
        if (status.uppercase() == "PENDING") {
            binding.layoutActions.visibility = View.VISIBLE
        } else {
            binding.layoutActions.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnAccept.setOnClickListener {
            updateBookingStatus("CONFIRMED")
        }

        binding.btnReject.setOnClickListener {
            updateBookingStatus("CANCELLED")
        }

        binding.btnReschedule.setOnClickListener {
            Toast.makeText(this, "Suggest Time feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBookingStatus(newStatus: String) {
        bookingId?.let { id ->
            db.collection("bookings").document(id)
                .update("status", newStatus)
                .addOnSuccessListener {
                    Toast.makeText(this, "Booking $newStatus", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
