package com.example.scenex.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.scenex.models.ApplicantTalent
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore

class RecruiterApplicantsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _applicants = MutableLiveData<List<ApplicantTalent>>()
    val applicants: LiveData<List<ApplicantTalent>> = _applicants

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchApplicants(castingCallId: String) {
        _isLoading.value = true
        _error.value = null

        db.collection("applications")
            .whereEqualTo("castingCallId", castingCallId)
            .get()
            .addOnSuccessListener { result ->
                val talentTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()
                val applicantList = mutableListOf<ApplicantTalent>()

                if (result.isEmpty) {
                    _applicants.value = emptyList()
                    _isLoading.value = false
                    return@addOnSuccessListener
                }

                for (document in result) {
                    val talentId = document.getString("talentId") ?: continue
                    val status = document.getString("status") ?: ""
                    val appliedAt = document.getTimestamp("appliedAt")?.toDate()?.time ?: 0L
                    
                    val task = db.collection("profiles").document(talentId).get()
                        .addOnSuccessListener { profileDoc ->
                            if (profileDoc.exists()) {
                                val fullName = profileDoc.getString("fullName") ?: "Unknown Talent"
                                val category = profileDoc.getString("spotlightCategory") ?: "Category not specified"
                                
                                synchronized(applicantList) {
                                    applicantList.add(
                                        ApplicantTalent(
                                            talentId = talentId,
                                            fullName = fullName,
                                            spotlightCategory = category,
                                            applicationStatus = status,
                                            appliedAt = appliedAt
                                        )
                                    )
                                }
                            }
                        }
                    talentTasks.add(task)
                }

                Tasks.whenAllComplete(talentTasks).addOnCompleteListener {
                    // FIXED: Ensure each talent only appears once even if duplicate application records exist
                    val distinctList = applicantList.distinctBy { it.talentId }
                    
                    // Sort locally: Newest applications first
                    val sortedList = distinctList.sortedByDescending { it.appliedAt }
                    _applicants.value = sortedList
                    _isLoading.value = false
                }
            }
            .addOnFailureListener { e ->
                Log.e("ApplicantsVM", "Error: ${e.message}")
                _error.value = e.message
                _isLoading.value = false
            }
    }
}
