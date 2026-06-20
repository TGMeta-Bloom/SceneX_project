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

sealed class TalentResultsState {
    object Loading : TalentResultsState()
    data class Success(val talents: List<UserProfile>) : TalentResultsState()
    object Empty : TalentResultsState()
    data class Error(val message: String) : TalentResultsState()
}

sealed class TalentNavEvent {
    data class OpenProfile(val talent: UserProfile) : TalentNavEvent()
    data class OpenWhatsApp(val url: String) : TalentNavEvent()
    data class OpenHireForm(val talent: UserProfile) : TalentNavEvent()
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
            _navEvent.emit(TalentNavEvent.OpenHireForm(talent))
        }
    }

    fun performSearch(criteria: SearchCriteria) {
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

                    // 🎯 HYBRID MATCHING: Combine Search Relevancy with Friend's Calculated Score (Profile Quality)
                    val searchRelevancy = if (totalPossible > 0) (matchPoints / totalPossible) else 0.5
                    
                    // Friend's score is in talent.calculated_score (loaded from Firestore)
                    // We normalize friend's score if it's 0-100 (assuming completeness/ranking logic)
                    val profileQuality = if (talent.calculated_score > 1.0) talent.calculated_score / 100.0 else talent.calculated_score
                    
                    // Final Hybrid Score: 70% Relevancy, 30% Profile Quality
                    val finalScore = (searchRelevancy * 0.7) + (profileQuality * 0.3)
                    
                    talent.copy(calculated_score = finalScore)
                }
                    .filter { it.calculated_score > 0.05 }
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
