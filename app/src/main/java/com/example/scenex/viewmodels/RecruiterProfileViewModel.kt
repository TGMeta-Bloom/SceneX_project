package com.example.scenex.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scenex.models.CastingCall
import com.example.scenex.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Optimized ViewModel for Recruiter Profiles.
 * Implements Snapshot Listeners for real-time data sync.
 */
class RecruiterProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _castingCalls = MutableStateFlow<List<CastingCall>>(emptyList())
    val castingCalls: StateFlow<List<CastingCall>> = _castingCalls.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var profileListener: ListenerRegistration? = null
    private var castingsListener: ListenerRegistration? = null

    fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return

        // Clear previous listeners if any
        profileListener?.remove()
        castingsListener?.remove()

        //  REAL-TIME PROFILE LISTENER
        profileListener = db.collection("profiles").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    _userProfile.value = snapshot.toObject(UserProfile::class.java)
                }
            }

        //  REAL-TIME CASTINGS LISTENER
        castingsListener = db.collection("CastingCalls")
            .whereEqualTo("recruiterId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                //  Manually map document ID to CastingCall object
                val castings = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(CastingCall::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _castingCalls.value = castings.sortedByDescending { it.createdAt }
            }
    }

    fun updateStatus(newStatus: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                //  UNIFIED: Update both 'status' and 'manualAvailabilityStatus' to keep logic synced
                val updates = mapOf(
                    "status" to newStatus,
                    "manualAvailabilityStatus" to newStatus.uppercase()
                )
                db.collection("profiles").document(userId).update(updates).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateRecruiterProfile(
        name: String,
        company: String,
        phone: String,
        experience: String,
        role: String,
        city: String,
        province: String,
        onComplete: (Boolean) -> Unit
    ) {
        val updates = mutableMapOf<String, Any>(
            "fullName" to name,
            "companyName" to company,
            "phoneNumber" to phone,
            "experience" to experience,
            "spotlightCategory" to role,
            "city" to city,
            "province" to province,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        updateProfileFields(updates, onComplete)
    }

    fun updateProfileFields(updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("profiles").document(userId).update(updates).await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProfile(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return onComplete(false)
        val userId = user.uid
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val castings = db.collection("CastingCalls").whereEqualTo("recruiterId", userId).get().await()
                if (!castings.isEmpty) {
                    val batch = db.batch()
                    castings.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
                db.collection("profiles").document(userId).delete().await()
                user.delete().await()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() = auth.signOut()

    fun deleteCastingCall(castingId: String) {
        viewModelScope.launch {
            try {
                // Using correct PascalCase collection name "CastingCalls"
                db.collection("CastingCalls").document(castingId).delete().await()
                // The listener (castingsListener) will automatically update the UI list
            } catch (e: Exception) {
                Log.e("SceneX_Recruiter", "Delete failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
        castingsListener?.remove()
    }
}
