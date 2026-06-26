package com.example.scenex.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.models.RecruiterCastingCallSchedule
import com.example.scenex.models.Schedule
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RecruiterCastingCallScheduleViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _castingCalls = MutableLiveData<List<RecruiterCastingCallSchedule>>()
    val castingCalls: LiveData<List<RecruiterCastingCallSchedule>> = _castingCalls

    private val _timelineSchedules = MutableLiveData<List<Schedule>>()
    val timelineSchedules: LiveData<List<Schedule>> = _timelineSchedules

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun fetchCastingCalls(status: String? = "ACTIVE") {
        val currentUserId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        db.collection("CastingCalls")
            .whereEqualTo("recruiterId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<RecruiterCastingCallSchedule>()
                val countTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

                if (result.isEmpty) {
                    _castingCalls.value = emptyList()
                    _isLoading.value = false
                    return@addOnSuccessListener
                }

                for (document in result) {
                    val statusInDb = document.getString("status") ?: "ACTIVE"
                    if (status != null && !statusInDb.equals(status, ignoreCase = true)) continue

                    val countTask = db.collection("applications")
                        .whereEqualTo("castingCallId", document.id)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val uniqueTalentIds = snapshot.documents.mapNotNull { it.getString("talentId") }.toSet()
                            list.add(RecruiterCastingCallSchedule(
                                id = document.id,
                                recruiterId = currentUserId,
                                title = document.getString("projectTitle") ?: "Untitled",
                                description = document.getString("projectSynopsis") ?: "",
                                location = document.getString("shootLocation") ?: "Unknown",
                                date = document.getString("auditionDate") ?: "",
                                startTime = document.getString("startTime") ?: "",
                                endTime = document.getString("endTime") ?: "",
                                directorName = document.getString("directorName") ?: "",
                                deadlineDate = document.getString("submissionDeadline") ?: "",
                                status = statusInDb,
                                appliedCount = uniqueTalentIds.size
                            ))
                        }
                    countTasks.add(countTask)
                }

                Tasks.whenAllComplete(countTasks).addOnCompleteListener {
                    _castingCalls.value = list.sortedBy { it.date }
                    _isLoading.value = false
                }
            }
    }

    /**
     * Optimized Timeline Logic: Listens directly to the recruiter's personal schedule sub-collection.
     */
    fun fetchTimelineSchedules() {
        val currentUserId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Directly query the optimized sub-collection created in the HireRequest handshake
        db.collection("profiles").document(currentUserId)
            .collection("schedules")
            .whereGreaterThanOrEqualTo("date", todayStart)
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Schedule::class.java)
                _timelineSchedules.value = list.sortedWith(compareBy<Schedule> { it.date }.thenBy { it.startTime })
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = e.message
                _isLoading.value = false
            }
    }
}
