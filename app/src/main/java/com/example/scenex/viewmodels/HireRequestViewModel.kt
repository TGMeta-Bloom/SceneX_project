package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scenex.models.HireRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
                _hireRequest.value = request
            } catch (e: Exception) {
                _events.emit(HireRequestEvent.Error("Failed to load request details"))
            }
        }
    }

    fun updateRequestStatus(requestId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                db.collection("hire_requests").document(requestId)
                    .update("status", newStatus)
                    .await()
                
                _events.emit(HireRequestEvent.StatusUpdated(newStatus))
            } catch (e: Exception) {
                _events.emit(HireRequestEvent.Error("Failed to update status"))
            }
        }
    }
}
