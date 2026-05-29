package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.repository.UserRepository
import com.google.firebase.firestore.ListenerRegistration

class TalentHomeViewModel : ViewModel() {
    private val repository = UserRepository()
    private var profileListener: ListenerRegistration? = null

    private val _profileData = MutableLiveData<Map<String, Any>?>()
    val profileData: LiveData<Map<String, Any>?> get() = _profileData

    /**
     * REAL-TIME SYNC ENGINE: 
     * Switches from .get() to Snapshot Listener to ensure Admin-side 
     * score updates reflect instantly on the Talent's dashboard.
     */
    fun fetchProfileData() {
        if (profileListener != null) return // Prevent duplicate listeners
        
        val userId = repository.getCurrentUserId() ?: return
        profileListener = repository.listenToProfileData(userId) { data ->
            _profileData.postValue(data)
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove() // Cleanup to prevent memory leaks
    }
}
