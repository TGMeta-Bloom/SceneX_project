package com.example.scenex.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scenex.models.ApplicantTalent
import com.example.scenex.models.CastingCall
import com.example.scenex.models.Schedule
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class CastingManagementViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _castingCall = MutableStateFlow<CastingCall?>(null)
    val castingCall: StateFlow<CastingCall?> = _castingCall

    private val _applicants = MutableStateFlow<List<ApplicantTalent>>(emptyList())
    val applicants: StateFlow<List<ApplicantTalent>> = _applicants

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadCastingData(castingId: String) {
        Log.d("SceneX_Manage", "Starting load for ID: $castingId")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch Casting Details
                val castingTask = db.collection("CastingCalls").document(castingId).get()
                
                // Fetch Applications
                val appSnapshots = db.collection("applications")
                    .whereEqualTo("castingCallId", castingId)
                    .get().await()
                
                Log.d("SceneX_Manage", "Applications found: ${appSnapshots.size()}")

                val castingDoc = castingTask.await()
                _castingCall.value = castingDoc.toObject(CastingCall::class.java)?.copy(id = castingDoc.id)

                if (!appSnapshots.isEmpty) {
                    val talentIds = appSnapshots.documents.mapNotNull { 
                        val tid = it.getString("talentId")
                        Log.d("SceneX_Manage", "Found application for TalentID: $tid")
                        tid
                    }.distinct()
                    val appMap = appSnapshots.documents.associateBy { it.getString("talentId") }

                    // Resolve profiles in chunks (Firestore limit is 30 for whereIn)
                    val applicantList = mutableListOf<ApplicantTalent>()
                    talentIds.chunked(30).forEach { chunk ->
                        Log.d("SceneX_Manage", "Fetching profile chunk: $chunk")
                        // 🎯 FIX: Query by DocumentId (FieldPath.documentId()) since the profiles' ID is the userId.
                        val profiles = db.collection("profiles")
                            .whereIn(FieldPath.documentId(), chunk)
                            .get().await()

                        Log.d("SceneX_Manage", "Profiles retrieved: ${profiles.size()}")

                        profiles.forEach { doc ->
                            val tid = doc.id 
                            applicantList.add(
                                ApplicantTalent(
                                    talentId = tid,
                                    fullName = doc.getString("fullName") ?: "Unknown",
                                    spotlightCategory = doc.getString("spotlightCategory") ?: "Talent",
                                    applicationStatus = appMap[tid]?.getString("status") ?: "pending",
                                    appliedAt = appMap[tid]?.getTimestamp("appliedAt")?.toDate()?.time ?: 0L,
                                    avatarUrl = doc.getString("profileImageUrl") ?: doc.getString("avatarUrl") ?: "",
                                    calculatedScore = doc.getDouble("calculated_score") ?: 0.0
                                )
                            )
                        }
                    }
                    Log.d("SceneX_Manage", "Final Applicant List Size: ${applicantList.size}")
                    // 🎯 FIX: Apply filtering based on status AND sort by calculatedScore
                    val filteredList = applicantList.filter { it.applicationStatus != "rejected" }
                    _applicants.value = filteredList.sortedByDescending { it.calculatedScore }
                } else {
                    Log.w("SceneX_Manage", "No applications found in DB for this castingId.")
                    _applicants.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("SceneX_Manage", "Load Error", e)
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStatus(talentId: String, castingId: String, status: String) {
        viewModelScope.launch {
            try {
                // 1. Find the application document
                val querySnapshot = db.collection("applications")
                    .whereEqualTo("talentId", talentId)
                    .whereEqualTo("castingCallId", castingId)
                    .get()
                    .await()
                
                if (!querySnapshot.isEmpty) {
                    val appDoc = querySnapshot.documents[0]
                    val castingCall = _castingCall.value ?: return@launch
                    val batch = db.batch()

                    // 2. Update Application Status
                    batch.update(db.collection("applications").document(appDoc.id), "status", status)

                    // 3. Automated Flow: Notifications & Scheduling
                    val notifRef = db.collection("notifications").document()
                    val timestamp = System.currentTimeMillis()
                    
                    if (status == "shortlisted") {
                        // 🟢 SHORTLISTED PATH: Notify & Schedule
                        val notifData = mapOf(
                            "notificationId" to notifRef.id,
                            "senderId" to castingCall.recruiterId,
                            "receiverId" to talentId,
                            "title" to "You're Shortlisted!",
                            "message" to "Congratulations! You are eligible to participate in the audition for '${castingCall.projectTitle}'. Check your schedule for details.",
                            "type" to "SHORTLIST",
                            "referenceId" to castingId,
                            "read" to false,
                            "createdAt" to timestamp
                        )
                        batch.set(notifRef, notifData)

                        // Parse Audition Date to Long
                        val dateLong = try {
                            SimpleDateFormat("dd MMM yyyy", Locale.US).parse(castingCall.auditionDate)?.time ?: 0L
                        } catch (e: Exception) { 0L }

                        // Create Schedule using project details
                        val scheduleId = db.collection("schedules").document().id
                        val schedule = Schedule(
                            id = scheduleId,
                            bookingId = appDoc.id,
                            castingCallId = castingId,
                            castingTitle = castingCall.projectTitle,
                            recruiterId = castingCall.recruiterId,
                            userId = talentId,
                            role = castingCall.roleType,
                            date = dateLong,
                            startTime = castingCall.startTime,
                            endTime = castingCall.endTime,
                            location = castingCall.auditionLocation,
                            status = "CONFIRMED"
                        )
                        
                        // Sync schedule to global collection and talent sub-collection
                        batch.set(db.collection("schedules").document(scheduleId), schedule)
                        batch.set(db.collection("profiles").document(talentId).collection("schedules").document(scheduleId), schedule)

                    } else if (status == "rejected") {
                        // 🔴 REJECTED PATH: Notify only
                        val notifData = mapOf(
                            "notificationId" to notifRef.id,
                            "senderId" to castingCall.recruiterId,
                            "receiverId" to talentId,
                            "title" to "Application Update",
                            "message" to "Thank you for applying to '${castingCall.projectTitle}'. Unfortunately, we have decided to move forward with other candidates at this time.",
                            "type" to "REJECTION",
                            "referenceId" to castingId,
                            "read" to false,
                            "createdAt" to timestamp
                        )
                        batch.set(notifRef, notifData)
                    }

                    batch.commit().await()

                    // 4. Update UI: Filter out if rejected, or just update status if shortlisted
                    val updatedList = _applicants.value.filter {
                        if (it.talentId == talentId) {
                            if (status == "rejected") return@filter false // Remove from list
                        }
                        true
                    }.map {
                        if (it.talentId == talentId) it.copy(applicationStatus = status)
                        else it
                    }
                    _applicants.value = updatedList
                }
            } catch (e: Exception) {
                Log.e("SceneX_Manage", "Update Error", e)
                _error.value = "Failed to update status: ${e.message}"
            }
        }
    }
}
