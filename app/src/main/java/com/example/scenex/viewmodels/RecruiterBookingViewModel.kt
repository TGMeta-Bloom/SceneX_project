package com.example.scenex.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RecruiterBookingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _bookings = MutableLiveData<List<Booking>>()
    val bookings: LiveData<List<Booking>> = _bookings

    private var bookingListener: ListenerRegistration? = null

    fun fetchBookings(statusCategory: String) {
        val recruiterId = auth.currentUser?.uid ?: return
        bookingListener?.remove()

        bookingListener = db.collection("bookings")
            .whereEqualTo("recruiterId", recruiterId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                val allBookings = snapshot?.toObjects(Booking::class.java) ?: emptyList()
                
                // LOGIC MAPPING:
                val filteredList = when (statusCategory) {
                    "PENDING" -> allBookings.filter { it.status == "PENDING" }
                    "UPCOMING" -> allBookings.filter { it.status == "CONFIRMED" }
                    "PAST" -> allBookings.filter { it.status == "COMPLETED" || it.status == "CANCELLED" }
                    else -> allBookings
                }.sortedByDescending { it.date } // Show newest first

                _bookings.value = filteredList
            }
    }

    override fun onCleared() {
        super.onCleared()
        bookingListener?.remove()
    }
}
