package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.repository.UserRepository

class RoleSelectViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _roleSaveStatus = MutableLiveData<Boolean>()
    val roleSaveStatus: LiveData<Boolean> get() = _roleSaveStatus

    fun selectRole(role: String) {
        repository.saveUserRole(role) { success ->
            _roleSaveStatus.value = success
        }
    }
}
