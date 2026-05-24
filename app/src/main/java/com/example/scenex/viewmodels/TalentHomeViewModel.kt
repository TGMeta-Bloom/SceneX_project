package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.repository.UserRepository

class TalentHomeViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _profileData = MutableLiveData<Map<String, Any>?>()
    val profileData: LiveData<Map<String, Any>?> get() = _profileData

    fun fetchProfileData() {
        val userId = repository.getCurrentUserId() ?: return
        repository.getProfileData(userId) { data ->
            _profileData.postValue(data)
        }
    }
}
