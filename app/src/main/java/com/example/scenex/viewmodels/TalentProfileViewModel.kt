package com.example.scenex.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scenex.models.UserProfile
import com.example.scenex.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TalentProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = UserRepository()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _portfolioImages = MutableStateFlow<List<String>>(emptyList())
    val portfolioImages: StateFlow<List<String>> = _portfolioImages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var profileListener: ListenerRegistration? = null
    private var mediaListener: ListenerRegistration? = null

    fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return
        
        profileListener?.remove()
        mediaListener?.remove()

        profileListener = db.collection("profiles").document(userId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.toObject(UserProfile::class.java)?.let {
                    _userProfile.value = it
                }
            }

        mediaListener = db.collection("media_assets").document(userId)
            .addSnapshotListener { snapshot, _ ->
                val images = snapshot?.get("portfolioImages") as? List<String> ?: emptyList()
                _portfolioImages.value = images
            }
    }

    /**
     * Updates the talent profile using raw field names to ensure compatibility with 
     * the existing Firestore structure without modifying the UserProfile data class.
     */
    fun updateTalentProfile(
        updates: Map<String, Any>,
        onComplete: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val finalUpdates = updates.toMutableMap()
                finalUpdates["updatedAt"] = FieldValue.serverTimestamp()
                
                db.collection("profiles").document(userId).update(finalUpdates).await()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 🗑️ NEW: Permanent removal of the talent account and data.
     */
    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.deleteUserAccount()
            onComplete(success)
            _isLoading.value = false
        }
    }

    fun logout() = auth.signOut()

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
        mediaListener?.remove()
    }
}
