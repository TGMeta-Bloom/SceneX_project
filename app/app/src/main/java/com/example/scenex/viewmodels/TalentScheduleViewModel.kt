package com.example.scenex.viewmodels

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

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var allSchedules = listOf<Schedule>()
    private var allBookings = listOf<Booking>()
    private var currentFilterType = "UPCOMING"

    fun startListening() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        if (scheduleListener == null) {
            scheduleListener = db.collection("schedules")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { value, error ->
                    if (error != null) return@addSnapshotListener
                    allSchedules = value?.toObjects(Schedule::class.java) ?: emptyList()
                    applyFilter(currentFilterType)
                }
        }

        if (bookingListener == null) {
            bookingListener = db.collection("bookings")
                .whereEqualTo("talentId", userId)
                .addSnapshotListener { value, error ->
                    _isLoading.value = false
                    if (error != null) return@addSnapshotListener
                    allBookings = value?.toObjects(Booking::class.java) ?: emptyList()
                    applyFilter(currentFilterType)
                }
        }
    }

    fun applyFilter(type: String) {
        currentFilterType = type

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        if (currentFilterType == "UPCOMING") {
            _schedules.value = allSchedules.filter { it.date >= startOfToday }.sortedBy { it.date }
            
            // Upcoming: Pending (Any Date) + Confirmed (Future/Today)
            _bookings.value = allBookings.filter { booking ->
                val status = booking.status.uppercase()
                status == "PENDING" || (status == "CONFIRMED" && booking.date >= startOfToday)
            }.sortedBy { it.date }
            
        } else {
            _schedules.value = allSchedules.filter { it.date < startOfToday }.sortedByDescending { it.date }
            
            // Past: Cancelled, Completed, and Confirmed (Past)
            _bookings.value = allBookings.filter { booking ->
                val status = booking.status.uppercase()
                status == "CANCELLED" || status == "COMPLETED" || (status == "CONFIRMED" && booking.date < startOfToday)
            }.sortedByDescending { it.date }
        }
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
