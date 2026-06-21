package com.example.scenex.views.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scenex.databinding.ItemBookingCardBinding
import com.example.scenex.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(private val onDetailsClick: (Booking) -> Unit) :
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private var bookings: List<Booking> = emptyList()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val db = FirebaseFirestore.getInstance()

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
        
        private var boundBookingId: String? = null

        fun bind(booking: Booking) {
            boundBookingId = booking.id
            
            // 1. CRITICAL RESET: Hide badge immediately to stop RecyclerView recycling bugs
            binding.layoutConflict.visibility = View.GONE
            
            // 2. Load UI
            if (booking.projectImageUrl.isNotEmpty()) {
                Glide.with(itemView.context).load(booking.projectImageUrl).centerCrop().into(binding.imgProjectBanner)
            }
            binding.txtProjectTitle.text = booking.castingTitle
            
            if (booking.talentId == currentUserId) {
                fetchRecruiterName(booking.castingCallId)
            } else {
                binding.txtTalentName.text = "Talent: ${booking.name}"
            }
            
            val dateSdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.txtBookingDate.text = dateSdf.format(Date(booking.date))
            binding.txtBookingTime.text = "${booking.startTime} - ${booking.endTime}"

            val status = booking.status.uppercase()
            binding.txtStatusBadge.text = status
            updateStatusBadgeColor(status)

            // 3. STRICT FIREBASE CONFLICT CHECK
            if (status == "PENDING" || status == "CONFIRMED") {
                checkStrictFirebaseConflict(booking)
            }
            
            binding.btnViewDetails.setOnClickListener { onDetailsClick(booking) }
        }

        private fun checkStrictFirebaseConflict(booking: Booking) {
            val bookingId = booking.id
            val talentId = booking.talentId
            val date = booking.date

            // We look for ANY OTHER booking for the same talent on the same date
            db.collection("bookings")
                .whereEqualTo("talentId", talentId)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener { snapshot ->
                    // Safety check for RecyclerView recycling
                    if (boundBookingId != bookingId) return@addOnSuccessListener

                    var conflictFound = false
                    for (doc in snapshot.documents) {
                        val other = doc.toObject(Booking::class.java)
                        
                        // LOGIC: Conflict exists ONLY if:
                        // 1. It is NOT the same document (checked by ID or Title/Time combo)
                        // 2. It is PENDING or CONFIRMED
                        // 3. The time overlaps
                        if (other != null && doc.id != bookingId) {
                            if (other.status == "PENDING" || other.status == "CONFIRMED") {
                                if (isTimeOverlapping(booking, other)) {
                                    conflictFound = true
                                    break
                                }
                            }
                        }
                    }
                    
                    // Show badge ONLY if an actual conflict is found in the database
                    binding.layoutConflict.visibility = if (conflictFound) View.VISIBLE else View.GONE
                }
        }

        private fun isTimeOverlapping(b1: Booking, b2: Booking): Boolean {
            val start1 = timeToMinutes(b1.startTime)
            val end1 = timeToMinutes(b1.endTime)
            val start2 = timeToMinutes(b2.startTime)
            val end2 = timeToMinutes(b2.endTime)
            
            if (start1 == -1 || start2 == -1) return false
            
            // Standard overlap math: (Start1 < End2) AND (Start2 < End1)
            return start1 < end2 && start2 < end1
        }

        private fun timeToMinutes(timeString: String): Int {
            return try {
                val sdf = SimpleDateFormat("hh:mm a", Locale.US)
                val date = sdf.parse(timeString.trim().uppercase())
                val cal = Calendar.getInstance().apply { time = date!! }
                cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            } catch (e: Exception) { -1 }
        }

        private fun fetchRecruiterName(castingCallId: String) {
            if (castingCallId.isEmpty()) return
            db.collection("CastingCalls").document(castingCallId).get()
                .addOnSuccessListener { doc ->
                    val recId = doc.getString("recruiterId") ?: ""
                    if (recId.isNotEmpty()) {
                        db.collection("profiles").document(recId).get()
                            .addOnSuccessListener { p ->
                                binding.txtTalentName.text = "Recruiter: ${p.getString("fullName") ?: "Official"}"
                            }
                    }
                }
        }

        private fun updateStatusBadgeColor(status: String) {
            val color = when (status) {
                "PENDING" -> "#FFB300"
                "CONFIRMED" -> "#4CAF50"
                "CANCELLED", "REJECTED" -> "#F44336"
                else -> "#9E9E9E"
            }
            binding.txtStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
        }
    }
}
