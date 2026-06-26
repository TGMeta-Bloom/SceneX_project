package com.example.scenex.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scenex.models.HireRequest
import com.example.scenex.models.Schedule
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

sealed class HireRequestEvent {
    object Success : HireRequestEvent()
    data class Error(val message: String) : HireRequestEvent()
    data class StatusUpdated(val status: String) : HireRequestEvent()
}

class HireRequestViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    
    private val _events = MutableSharedFlow<HireRequestEvent>()
    val events: SharedFlow<HireRequestEvent> = _events.asSharedFlow()

    private val _hireRequest = MutableLiveData<HireRequest?>()
    val hireRequest: LiveData<HireRequest?> = _hireRequest

    fun submitHireRequest(request: HireRequest, recruiterName: String) {
        viewModelScope.launch {
            try {
                val hireRef = db.collection("hire_requests").document()
                val finalRequest = request.copy(requestId = hireRef.id)
                hireRef.set(finalRequest).await()

                val notificationRef = db.collection("notifications").document()
                val notificationData = mapOf(
                    "notificationId" to notificationRef.id,
                    "senderId" to request.recruiterId,
                    "senderName" to recruiterName,
                    "receiverId" to request.talentId,
                    "title" to "New Hire Request",
                    "message" to "$recruiterName sent you a hire request for '${request.projectTitle}'",
                    "type" to "HIRE_REQUEST",
                    "referenceId" to hireRef.id,
                    "read" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                notificationRef.set(notificationData).await()

                _events.emit(HireRequestEvent.Success)
            } catch (e: Exception) {
                _events.emit(HireRequestEvent.Error(e.localizedMessage ?: "Failed to send request"))
            }
        }
    }

    fun fetchHireRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("hire_requests").document(requestId).get().await()
                val request = snapshot.toObject(HireRequest::class.java)
                _hireRequest.postValue(request)
            } catch (e: Exception) {
                _events.emit(HireRequestEvent.Error("Failed to load request details"))
            }
        }
    }

    /**
     * Handles Talent's response (Accept or Reject).
     */
    fun updateRequestStatus(requestId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("hire_requests").document(requestId).get().await()
                val request = snapshot.toObject(HireRequest::class.java) ?: return@launch

                if (newStatus == "ACCEPTED") {
                    performFinalBooking(request, isRecruiterConfirming = false)
                } else {
                    val talentProfile = db.collection("profiles").document(request.talentId).get().await()
                    val talentName = talentProfile.getString("fullName") ?: "A talent"

                    db.collection("hire_requests").document(requestId).update("status", newStatus).await()

                    val notificationRef = db.collection("notifications").document()
                    val notificationData = mapOf(
                        "notificationId" to notificationRef.id,
                        "senderId" to request.talentId,
                        "senderName" to talentName,
                        "receiverId" to request.recruiterId,
                        "title" to "Hire Request Update",
                        "message" to "$talentName has $newStatus your request for '${request.projectTitle}'",
                        "type" to "HIRE_RESPONSE",
                        "referenceId" to requestId,
                        "read" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                    notificationRef.set(notificationData).await()
                    _events.emit(HireRequestEvent.StatusUpdated(newStatus))
                    fetchHireRequest(requestId)
                }
            } catch (e: Exception) {
                _events.emit(HireRequestEvent.Error("Failed: ${e.message}"))
            }
        }
    }

    fun proposeNewTime(requestId: String, date: String, time: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("hire_requests").document(requestId).get().await()
                val request = snapshot.toObject(HireRequest::class.java) ?: return@launch

                val talentProfile = db.collection("profiles").document(request.talentId).get().await()
                val talentName = talentProfile.getString("fullName") ?: "Talent"

                val updates = mapOf(
                    "status" to "RESCHEDULED",
                    "proposedDate" to date,
                    "proposedTime" to time
                )
                db.collection("hire_requests").document(requestId).update(updates).await()

                val notificationRef = db.collection("notifications").document()
                val notificationData = mapOf(
                    "notificationId" to notificationRef.id,
                    "senderId" to request.talentId,
                    "senderName" to talentName,
                    "receiverId" to request.recruiterId,
                    "title" to "New Time Proposed",
                    "message" to "$talentName suggested a new time: $date at $time",
                    "type" to "HIRE_RESPONSE",
                    "referenceId" to requestId,
                    "read" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                notificationRef.set(notificationData).await()
                _events.emit(HireRequestEvent.StatusUpdated("RESCHEDULED"))
                fetchHireRequest(requestId)
            } catch (e: Exception) {
                _events.emit(HireRequestEvent.Error("Proposal failed: ${e.message}"))
            }
        }
    }

    fun confirmAndBook(requestId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("hire_requests").document(requestId).get().await()
                val request = snapshot.toObject(HireRequest::class.java) ?: return@launch
                performFinalBooking(request, isRecruiterConfirming = true)
            } catch (e: Exception) {
                _events.emit(HireRequestEvent.Error("Booking failed: ${e.message}"))
            }
        }
    }

    private suspend fun performFinalBooking(request: HireRequest, isRecruiterConfirming: Boolean) {
        val finalDate = if (!request.proposedDate.isNullOrEmpty()) request.proposedDate else request.startDate
        val finalTime = if (!request.proposedTime.isNullOrEmpty()) request.proposedTime else "09:00"

        // 1. Update main record
        val requestUpdate = mapOf(
            "status" to "BOOKED",
            "startDate" to finalDate,
            "proposedTime" to finalTime
        )
        db.collection("hire_requests").document(request.requestId).update(requestUpdate).await()

        // 2. Resolve Party Names
        val recruiterProfile = db.collection("profiles").document(request.recruiterId).get().await()
        val recruiterName = recruiterProfile.getString("fullName") ?: "Recruiter"
        val talentProfile = db.collection("profiles").document(request.talentId).get().await()
        val talentName = talentProfile.getString("fullName") ?: "Talent"

        val dateLong = try { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(finalDate)?.time ?: 0L } catch (e: Exception) { 0L }

        // 3. Create Schedule with "Direct Hire" role for professional appearance in Timeline
        val schedule = Schedule(
            id = request.requestId,
            bookingId = request.requestId,
            castingCallId = request.castingId,
            castingTitle = request.projectTitle,
            recruiterId = request.recruiterId,
            recruiterName = recruiterName,
            userId = request.talentId,
            name = talentName,
            role = "Direct Hire", 
            date = dateLong,
            startTime = finalTime,
            location = request.location,
            status = "CONFIRMED"
        )

        // 4. Mirror Data across collections
        val batch = db.batch()
        batch.set(db.collection("profiles").document(request.recruiterId).collection("schedules").document(request.requestId), schedule)
        batch.set(db.collection("profiles").document(request.talentId).collection("schedules").document(request.requestId), schedule)
        batch.set(db.collection("schedules").document(request.requestId), schedule)
        batch.commit().await()

        // 5. Final Notification
        val receiverId = if (isRecruiterConfirming) request.talentId else request.recruiterId
        val senderId = if (isRecruiterConfirming) request.recruiterId else request.talentId
        val senderName = if (isRecruiterConfirming) recruiterName else talentName
        
        val notificationRef = db.collection("notifications").document()
        val notificationData = mapOf(
            "notificationId" to notificationRef.id,
            "senderId" to senderId,
            "senderName" to senderName,
            "receiverId" to receiverId,
            "title" to "Booking Confirmed!",
            "message" to "The hire for '${request.projectTitle}' is officially booked for $finalDate",
            "type" to "HIRE_RESPONSE",
            "referenceId" to request.requestId,
            "read" to false,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("notifications").document(notificationRef.id).set(notificationData).await()

        _events.emit(HireRequestEvent.StatusUpdated("BOOKED"))
        fetchHireRequest(request.requestId)
    }
}
