package com.example.scenex.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scenex.models.SearchCriteria
import com.example.scenex.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder

sealed class TalentResultsState {
    object Loading : TalentResultsState()
    data class Success(val talents: List<UserProfile>) : TalentResultsState()
    object Empty : TalentResultsState()
    data class Error(val message: String) : TalentResultsState()
}

// Simple navigation events
sealed class TalentNavEvent {
    data class OpenProfile(val talent: UserProfile) : TalentNavEvent()
    data class OpenWhatsApp(val url: String) : TalentNavEvent()
}

class TalentResultsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<TalentResultsState>(TalentResultsState.Loading)
    val uiState: StateFlow<TalentResultsState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<TalentNavEvent>()
    val navEvent: SharedFlow<TalentNavEvent> = _navEvent.asSharedFlow()

    private val db = FirebaseFirestore.getInstance()

    fun onTalentSelected(talent: UserProfile) {
        viewModelScope.launch {
            _navEvent.emit(TalentNavEvent.OpenProfile(talent))
        }
    }

    fun initiateHire(talent: UserProfile, recruiterId: String) {
        viewModelScope.launch {
            try {
                // 1. Log the Hire Request in Firestore (Audit Trail for SceneX)
                val requestId = "${recruiterId}_${talent.userId}"
                val requestData = mapOf(
                    "recruiterId" to recruiterId,
                    "talentId" to talent.userId,
                    "talentName" to talent.fullName,
                    "status" to "INTERESTED",
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("hiring_requests")
                    .document(requestId)
                    .set(requestData)
                    .await()

                // 2. Generate WhatsApp Bridge (The "Closure" step)
                // SL Context: Convert 077... to 9477... if necessary
                val rawPhone = talent.phoneNumber.filter { it.isDigit() }
                val cleanPhone = if (rawPhone.startsWith("0")) "94${rawPhone.substring(1)}" else rawPhone

                val message = "Hello ${talent.fullName}, I found your profile on SceneX and I'm interested in hiring you for a project. Let's discuss!"
                val encodedMsg = URLEncoder.encode(message, "UTF-8")
                val whatsappUrl = "https://wa.me/$cleanPhone?text=$encodedMsg"

                _navEvent.emit(TalentNavEvent.OpenWhatsApp(whatsappUrl))

            } catch (e: Exception) {
                Log.e("SceneX_Hire", "Hire logic failed: ${e.message}")
            }
        }
    }

    fun performSearch(criteria: SearchCriteria) {
        // ... (Existing performSearch logic remains exactly as it is) ...
        viewModelScope.launch {
            _uiState.value = TalentResultsState.Loading
            try {
                val snapshot = db.collection("profiles")
                    .whereEqualTo("userRole", "TALENT")
                    .get()
                    .await()

                val allTalents = snapshot.toObjects(UserProfile::class.java)

                val results = allTalents.map { talent ->
                    var matchPoints = 0.0
                    var totalPossible = 0.0

                    if (criteria.query.isNotEmpty()) {
                        totalPossible += 3.0
                        if (talent.fullName.contains(criteria.query, ignoreCase = true) ||
                            talent.spotlightCategory.contains(criteria.query, ignoreCase = true) ||
                            talent.role.contains(criteria.query, ignoreCase = true)) {
                            matchPoints += 3.0
                        }
                    }

                    if (criteria.location.isNotEmpty()) {
                        totalPossible += 2.0
                        if (talent.city.contains(criteria.location, ignoreCase = true) ||
                            talent.province.contains(criteria.location, ignoreCase = true)) {
                            matchPoints += 2.0
                        }
                    }

                    if (criteria.gender != "Any") {
                        totalPossible += 1.0
                        if (talent.gender.equals(criteria.gender, ignoreCase = true)) {
                            matchPoints += 1.0
                        }
                    }

                    totalPossible += 1.0
                    if (talent.age in criteria.minAge..criteria.maxAge) {
                        matchPoints += 1.0
                    }

                    if (criteria.languages.isNotEmpty()) {
                        totalPossible += 2.0
                        val talentLangs = talent.languages.lowercase()
                        val matchedCount = criteria.languages.count { talentLangs.contains(it.lowercase()) }
                        if (matchedCount > 0) {
                            matchPoints += (matchedCount.toDouble() / criteria.languages.size) * 2.0
                        }
                    }

                    totalPossible += 1.5
                    val heightVal = talent.physicalSpecs.filter { it.isDigit() }.toIntOrNull() ?: 0
                    if (heightVal in criteria.minHeight..criteria.maxHeight) {
                        matchPoints += 1.5
                    }

                    val score = if (totalPossible > 0) (matchPoints / totalPossible) else 0.5
                    talent.copy(calculated_score = score)
                }
                    .filter { it.calculated_score > 0.1 }
                    .sortedByDescending { it.calculated_score }

                if (results.isEmpty()) {
                    _uiState.value = TalentResultsState.Empty
                } else {
                    _uiState.value = TalentResultsState.Success(results)
                }

            } catch (e: Exception) {
                Log.e("SceneX_Discovery", "Engine Fail: ${e.message}")
                _uiState.value = TalentResultsState.Error("Discovery Error: ${e.localizedMessage}")
            }
        }
    }
}