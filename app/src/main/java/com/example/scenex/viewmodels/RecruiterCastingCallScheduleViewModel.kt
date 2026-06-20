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

    // LIVE DATA FOR TIMELINE ONLY
    private val _timelineSchedules = MutableLiveData<List<Schedule>>()
    val timelineSchedules: LiveData<List<Schedule>> = _timelineSchedules

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // UNTOUCHED: Existing casting call logic
    fun fetchCastingCalls(status: String? = "ACTIVE") {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            _error.value = "User not logged in"
            return
        }

        _isLoading.value = true
        _error.value = null

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
                    try {
                        val statusInDb = document.getString("status") ?: "ACTIVE"

                        if (status != null && !statusInDb.equals(status, ignoreCase = true)) {
                            continue
                        }

                        val countTask = db.collection("applications")
                            .whereEqualTo("castingCallId", document.id)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val uniqueTalentIds = snapshot.documents.mapNotNull { it.getString("talentId") }.toSet()
                                val appliedCount = uniqueTalentIds.size

                                val castingCall = RecruiterCastingCallSchedule(
                                    id = document.id,
                                    recruiterId = document.getString("recruiterId") ?: "",
                                    title = document.getString("projectTitle") ?: "Untitled",
                                    description = document.getString("projectSynopsis") ?: "",
                                    location = document.getString("shootLocation") ?: "Unknown",
                                    date = document.getString("auditionDate") ?: "",
                                    startTime = document.getString("startTime") ?: "",
                                    endTime = document.getString("endTime") ?: "",
                                    directorName = document.getString("directorName") ?: "",
                                    deadlineDate = document.getString("submissionDeadline") ?: "",
                                    status = statusInDb,
                                    appliedCount = appliedCount
                                )
                                synchronized(list) {
                                    list.add(castingCall)
                                }
                            }
                        countTasks.add(countTask)
                    } catch (e: Exception) {
                        Log.e("CastingCallVM", "Error processing document ${document.id}: ${e.message}")
                    }
                }

                if (countTasks.isEmpty()) {
                    _castingCalls.value = emptyList()
                    _isLoading.value = false
                } else {
                    Tasks.whenAllComplete(countTasks).addOnCompleteListener {
                        val sortedList = list.sortedBy { call ->
                            try {
                                if (call.date.isNotEmpty()) dateFormat.parse(call.date)?.time else Long.MAX_VALUE
                            } catch (e: Exception) {
                                Long.MAX_VALUE
                            }
                        }
                        _castingCalls.value = sortedList
                        _isLoading.value = false
                    }
                }
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
                _isLoading.value = false
            }
    }

    // NEW: Logic for 'Timeline View'
    fun fetchTimelineSchedules() {
        val currentUserId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Fetch schedules where user is recruiter OR creator
        val task1 = db.collection("schedules").whereEqualTo("recruiterId", currentUserId).get()
        val task2 = db.collection("schedules").whereEqualTo("userId", currentUserId).get()

        Tasks.whenAllComplete(task1, task2).addOnCompleteListener {
            val combined = mutableListOf<Schedule>()
            
            if (task1.isSuccessful) {
                task1.result?.toObjects(Schedule::class.java)?.let { combined.addAll(it) }
            }
            if (task2.isSuccessful) {
                task2.result?.toObjects(Schedule::class.java)?.let { combined.addAll(it) }
            }

            // Filter expired and sort (Ascending: nearest upcoming first)
            val sortedList = combined.distinctBy { it.id }
                .filter { it.date >= todayStart }
                .sortedWith(compareBy<Schedule> { it.date }.thenBy { parseTimeToMinutes(it.startTime) })

            _timelineSchedules.value = sortedList
            _isLoading.value = false
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val date = SimpleDateFormat("hh:mm a", Locale.US).parse(timeStr.trim().uppercase())
            val cal = Calendar.getInstance().apply { time = date!! }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) { 0 }
    }
}
