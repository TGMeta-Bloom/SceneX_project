package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.models.Schedule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TalentScheduleViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var snapshotListener: ListenerRegistration? = null

    private val _schedules = MutableLiveData<List<Schedule>>()
    val schedules: LiveData<List<Schedule>> = _schedules

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var allSchedules = listOf<Schedule>()
    private var currentFilterType = "UPCOMING"

    /**
     * Set up a SINGLE real-time listener that stays active.
     */
    fun startListening() {
        if (snapshotListener != null) return // Already listening

        val userId = auth.currentUser?.uid ?: "test_user_123"
        _isLoading.value = true

        snapshotListener = db.collection("schedules")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { value, error ->
                _isLoading.value = false
                if (error != null) return@addSnapshotListener

                allSchedules = value?.toObjects(Schedule::class.java) ?: emptyList()
                applyFilter(currentFilterType)
            }
    }

    /**
     * Switches the filter without adding new listeners.
     */
    fun applyFilter(type: String) {
        currentFilterType = type
        val now = System.currentTimeMillis()
        
        val filteredList = if (currentFilterType == "UPCOMING") {
            // Filter: date is in the future | Sort: Nearest first
            allSchedules.filter { it.date >= now }
                .sortedBy { it.date }
        } else {
            // Filter: date is in the past | Sort: Recently passed first
            allSchedules.filter { it.date < now }
                .sortedByDescending { it.date }
        }
        
        _schedules.value = filteredList
    }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove() // Clean up when ViewModel is destroyed
    }

    fun deleteSchedule(scheduleId: String) {
        db.collection("schedules").document(scheduleId).delete()
    }
}
