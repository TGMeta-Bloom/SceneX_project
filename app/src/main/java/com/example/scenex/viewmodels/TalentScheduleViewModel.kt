package com.example.scenex.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.models.Booking
import com.example.scenex.models.Schedule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class TalentScheduleViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var scheduleListener: ListenerRegistration? = null
    private var bookingListener: ListenerRegistration? = null

    private val _schedules = MutableLiveData<List<Schedule>>()
    val schedules: LiveData<List<Schedule>> = _schedules

    private val _bookings = MutableLiveData<List<Booking>>()
    val bookings: LiveData<List<Booking>> = _bookings

    private val _totalPendingCount = MutableLiveData<Int>(0)
    val totalPendingCount: LiveData<Int> = _totalPendingCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    val currentUserId: String? get() = auth.currentUser?.uid

    private var allSchedules = listOf<Schedule>()
    private var allBookings = listOf<Booking>()
    private var currentStatusFilter = "ALL"

    fun startListening() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        // 1. Personal availability listener (Timeline View)
        if (scheduleListener == null) {
            scheduleListener = db.collection("schedules")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { value, error ->
                    if (error != null) return@addSnapshotListener
                    allSchedules = value?.toObjects(Schedule::class.java) ?: emptyList()
                    updateSchedules()
                }
        }

        // 2. Booking Requests listener
        if (bookingListener == null) {
            bookingListener = db.collection("bookings")
                .whereEqualTo("talentId", userId)
                .addSnapshotListener { value, error ->
                    _isLoading.value = false
                    if (error != null) {
                        Log.e("TalentScheduleVM", "Firestore error: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    if (value != null) {
                        val tempList = mutableListOf<Booking>()
                        for (doc in value.documents) {
                            try {
                                val b = doc.toObject(Booking::class.java)
                                if (b != null) tempList.add(b)
                            } catch (e: Exception) {
                                Log.e("TalentScheduleVM", "Error parsing booking: ${e.message}")
                            }
                        }
                        allBookings = tempList
                        
                        // Calculate total pending count for the UI badge
                        _totalPendingCount.value = tempList.count { it.status.trim().uppercase() == "PENDING" }
                        
                        updateBookings()
                    }
                }
        }
    }

    private fun updateSchedules() {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Timeline: Soonest upcoming first (Ascending)
        _schedules.value = allSchedules.filter { it.date >= startOfToday }.sortedBy { it.date }
    }

    private fun updateBookings() {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var filtered = allBookings
        
        // Filter by Status if selected
        if (currentStatusFilter != "ALL") {
            if (currentStatusFilter == "CANCELLED") {
                // "Cancelled" filter includes both CANCELLED and REJECTED statuses
                filtered = filtered.filter { 
                    val s = it.status.trim().uppercase()
                    s == "CANCELLED" || s == "REJECTED"
                }
            } else {
                filtered = filtered.filter { it.status.trim().uppercase() == currentStatusFilter.uppercase() }
            }
        }

        // Logic: Show Upcoming bookings (Soonest first) at the top,
        // then Past bookings (Most recent first) at the bottom.
        val upcoming = filtered.filter { it.date >= startOfToday }.sortedBy { it.date }
        val past = filtered.filter { it.date < startOfToday }.sortedByDescending { it.date }

        _bookings.value = upcoming + past
        
        Log.d("TalentScheduleVM", "Bookings updated. Filter: $currentStatusFilter, Count: ${filtered.size}")
    }

    fun setStatusFilter(status: String) {
        currentStatusFilter = status
        updateBookings()
    }

    override fun onCleared() {
        super.onCleared()
        scheduleListener?.remove()
        bookingListener?.remove()
    }

    fun deleteSchedule(scheduleId: String) {
        db.collection("schedules").document(scheduleId).delete()
    }
}
