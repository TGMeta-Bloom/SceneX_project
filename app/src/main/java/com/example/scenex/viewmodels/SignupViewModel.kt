package com.example.scenex.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.models.UserProfile
import com.example.scenex.repository.UserRepository

class SignupViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _signupStatus = MutableLiveData<Boolean>()
    val signupStatus: LiveData<Boolean> get() = _signupStatus

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // Step 1 Data
    var fullName: String = ""
    var email: String = ""
    var stageName: String = ""
    var password: String = ""

    fun createAccount() {
        if (email.isEmpty() || password.isEmpty()) {
            _errorMessage.value = "Please fill in all required fields"
            _signupStatus.value = false
            return
        }

        val profile = UserProfile(
            fullName = fullName,
            email = email,
            stageName = stageName,
            role = "TALENT"
        )

        repository.signupUser(profile, password) { success, error ->
            if (success) {
                _signupStatus.value = true
            } else {
                _errorMessage.value = error ?: "Signup failed"
                _signupStatus.value = false
            }
        }
    }
}
