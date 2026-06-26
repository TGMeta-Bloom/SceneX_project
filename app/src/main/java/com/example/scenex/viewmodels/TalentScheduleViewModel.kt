package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.models.Booking
import com.example.scenex.models.HireRequest
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
    private var hireRequestListener: ListenerRegistration? = null
    private var applicationListener: ListenerRegistration? = null

    private val _schedules = MutableLiveData<List<Schedule>>()
    val schedules: LiveData<List<Schedule>> = _schedules

    private val _bookings = MutableLiveData<List<Booking>>()
    val bookings: LiveData<List<Booking>> = _bookings
    
    private val _hireRequests = MutableLiveData<List<HireRequest>>()
    val hireRequests: LiveData<List<HireRequest>> = _hireRequests

    // Layer C: Talent's own applications (The Handshake Bridge)
    private val _applicationsAsBookings = MutableLiveData<List<Booking>>()
    val applicationsAsBookings: LiveData<List<Booking>> = _applicationsAsBookings

    private val _totalPendingCount = MutableLiveData<Int>(0)
    val totalPendingCount: LiveData<Int> = _totalPendingCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    val currentUserId: String? get() = auth.currentUser?.uid

    private var allSchedules = listOf<Schedule>()
    private var allBookings = listOf<Booking>()
    private var allHireRequests = listOf<HireRequest>()
    private var currentStatusFilter = "ALL"

    fun startListening() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        // 1. Finalized Schedule
        scheduleListener = db.collection("profiles").document(userId)
            .collection("schedules")
            .addSnapshotListener { value, _ ->
                allSchedules = value?.toObjects(Schedule::class.java) ?: emptyList()
                updateSchedules()
            }

        // 2. Confirmed Bookings
        bookingListener = db.collection("bookings")
            .whereEqualTo("talentId", userId)
            .addSnapshotListener { value, _ ->
                allBookings = value?.toObjects(Booking::class.java) ?: emptyList()
                calculatePendingCount()
                updateBookings()
            }

        // 3. Direct Hire Requests
        hireRequestListener = db.collection("hire_requests")
            .whereEqualTo("talentId", userId)
            .addSnapshotListener { value, _ ->
                allHireRequests = value?.toObjects(HireRequest::class.java) ?: emptyList()
                _hireRequests.value = allHireRequests
                calculatePendingCount()
                _isLoading.value = false
            }

        // 4. THE HANDSHAKE: Applications mapped to Booking UI
        applicationListener = db.collection("applications")
            .whereEqualTo("talentId", userId)
            .addSnapshotListener { value, _ ->
                val apps = value?.documents?.map { doc ->
                    Booking(
                        id = doc.id,
                        castingCallId = doc.getString("castingCallId") ?: "",
                        castingTitle = "Applied: ${doc.getString("projectTitle") ?: "Casting Call"}",
                        status = doc.getString("status")?.uppercase() ?: "PENDING",
                        date = doc.getTimestamp("appliedAt")?.toDate()?.time ?: 0L,
                        type = "APPLICATION"
                    )
                } ?: emptyList()
                _applicationsAsBookings.value = apps
            }
    }

    private fun calculatePendingCount() {
        val pendingBookings = allBookings.count { it.status.uppercase() == "PENDING" }
        val pendingHires = allHireRequests.count { it.status.uppercase() == "PENDING" }
        _totalPendingCount.value = pendingBookings + pendingHires
    }

    private fun updateSchedules() {
        val startOfToday = getStartOfToday()
        _schedules.value = allSchedules.filter { it.date >= startOfToday }.sortedBy { it.date }
    }

    private fun updateBookings() {
        val startOfToday = getStartOfToday()
        var filtered = allBookings
        if (currentStatusFilter != "ALL") {
            filtered = filtered.filter { it.status.uppercase() == currentStatusFilter }
        }
        _bookings.value = filtered.sortedByDescending { it.date }
    }

    private fun getStartOfToday() = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun setStatusFilter(status: String) {
        currentStatusFilter = status
        updateBookings()
    }

    override fun onCleared() {
        super.onCleared()
        scheduleListener?.remove()
        bookingListener?.remove()
        hireRequestListener?.remove()
        applicationListener?.remove()
    }
}
