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
    private var cachedTalents: List<UserProfile> = emptyList()
    private var pendingCriteria: SearchCriteria? = null

    fun onTalentSelected(talent: UserProfile) {
        viewModelScope.launch {
            _navEvent.emit(TalentNavEvent.OpenProfile(talent))
        }
    }

    /**
     * Triggers navigation to the Hire Request fragment.
     */
    fun onHireRequested(talent: UserProfile) {
        viewModelScope.launch {
            _navEvent.emit(TalentNavEvent.OpenHireForm(talent))
        }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = TalentResultsState.Loading
            try {
                //  FIX: Filter only verified (approved) talents
                val snapshot = db.collection("profiles")
                    .whereEqualTo("userRole", "TALENT")
                    .whereEqualTo("verificationStatus", "verified")
                    .get()
                    .await()
                cachedTalents = snapshot.toObjects(UserProfile::class.java)

                performSearch(pendingCriteria ?: SearchCriteria())
                pendingCriteria = null
            } catch (e: Exception) {
                Log.e("SceneX_Discovery", "Load Fail: ${e.message}")
                _uiState.value = TalentResultsState.Error("Failed to sync talent hub.")
            }
        }
    }

    fun performSearch(criteria: SearchCriteria) {
        if (cachedTalents.isEmpty()) {
            pendingCriteria = criteria
            loadInitialData()
            return
        }

        viewModelScope.launch {
            val results = cachedTalents.map { talent ->
                var matchPoints = 0.0
                var totalPossible = 0.0

                if (criteria.query.isNotEmpty()) {
                    totalPossible += 10.0
                    val q = criteria.query.lowercase()
                    if (talent.fullName.lowercase().contains(q) ||
                        talent.spotlightCategory.lowercase().contains(q) ||
                        talent.role.lowercase().contains(q) ||
                        talent.skills.any { it.lowercase().contains(q) }) {
                        matchPoints += 10.0
                    }
                }

                if (criteria.city.isNotEmpty() && criteria.city != "All Cities") {
                    totalPossible += 5.0
                    if (talent.city.equals(criteria.city, ignoreCase = true)) matchPoints += 5.0
                } else if (criteria.province.isNotEmpty() && criteria.province != "All Provinces") {
                    totalPossible += 5.0
                    if (talent.province.equals(criteria.province, ignoreCase = true)) matchPoints += 5.0
                } else if (criteria.location.isNotEmpty()) {
                    totalPossible += 5.0
                    val loc = criteria.location.lowercase()
                    if (talent.city.lowercase().contains(loc) ||
                        talent.province.lowercase().contains(loc) ||
                        loc.contains(talent.city.lowercase())) {
                        matchPoints += 5.0
                    }
                }

                if (criteria.gender != "Any" && criteria.gender != "All") {
                    totalPossible += 5.0
                    if (talent.gender.equals(criteria.gender, ignoreCase = true)) matchPoints += 5.0
                }

                totalPossible += 2.0
                if (talent.age in criteria.minAge..criteria.maxAge) matchPoints += 2.0

                if (criteria.languages.isNotEmpty()) {
                    totalPossible += 3.0
                    val talentLangs = talent.languages.lowercase()
                    if (criteria.languages.any { lang -> talentLangs.contains(lang.lowercase()) }) {
                        matchPoints += 3.0
                    }
                }

                if (criteria.minRating > 0) {
                    totalPossible += 2.0
                    if (talent.rating >= criteria.minRating) matchPoints += 2.0
                }

                val finalScore = if (totalPossible > 0) (matchPoints / totalPossible) else 1.0
                talent.copy(calculated_score = finalScore)
            }
                .filter { it.calculated_score > 0.05 || criteria.query.isEmpty() }
                .sortedByDescending { it.calculated_score }

            if (results.isEmpty()) {
                _uiState.value = TalentResultsState.Empty
            } else {
                _uiState.value = TalentResultsState.Success(results)
            }
        }
    }
}
