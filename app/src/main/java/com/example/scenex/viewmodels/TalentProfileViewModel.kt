package com.example.scenex.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scenex.models.UserProfile
import com.example.scenex.models.PortfolioWork
import com.example.scenex.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
     * Updates the talent profile and RECALCULATES the completeness and ranking scores
     * to ensure the UI and system reflect the profile growth immediately.
     */
    fun updateTalentProfile(
        updates: Map<String, Any>,
        onComplete: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Apply primary updates
                val finalUpdates = updates.toMutableMap()
                finalUpdates["updatedAt"] = FieldValue.serverTimestamp()
                db.collection("profiles").document(userId).update(finalUpdates).await()

                // 2. Fetch the newly merged profile to calculate scores
                recalculateScores(userId)

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun recalculateScores(userId: String) {
        try {
            val snapshot = db.collection("profiles").document(userId).get().await()
            val profile = snapshot.toObject(UserProfile::class.java) ?: return

            //  IMPROVED: Smarter, more inclusive scoring logic
            var cScore = 0

            // 1. Identity & Contact (15 pts)
            if (profile.fullName.isNotBlank() && profile.email.isNotBlank() && profile.phoneNumber.isNotBlank()) cScore += 15

            // 2. Physical & Appearance (15 pts)
            if (profile.height.isNotBlank() || profile.physicalSpecs.isNotBlank()) cScore += 5
            if (profile.age > 0 && profile.gender.isNotBlank()) cScore += 10

            // 3. Professional Foundation (20 pts)
            if (profile.spotlightCategory.isNotBlank()) cScore += 10
            if (profile.experience.isNotBlank() || profile.qualification.isNotBlank()) cScore += 10

            // 4. Visual Assets (25 pts)
            val hasHeadshot = profile.headshotUrl.isNotBlank() || profile.profileImage.isNotBlank() || profile.profileImageUrl.isNotBlank()
            val hasFullBody = profile.fullBodyUrl.isNotBlank()
            if (hasHeadshot) cScore += 15
            if (hasFullBody) cScore += 10

            // 5. Media & Credits (25 pts)
            val hasVideo = profile.videoUrl.isNotBlank() || profile.showreelUrl.isNotBlank()
            val hasPortfolio = profile.portfolioWorks.isNotEmpty() || profile.portfolioLink.isNotBlank()
            if (hasVideo) cScore += 15
            if (hasPortfolio) cScore += 10

            val completeness = cScore.coerceAtMost(100)

            // 6. Ranking Logic (Weighted calculation)
            val skillsYield = Math.min(100.0, (profile.accents.size + profile.otherSkills.size) * 20.0)
            val experienceYield = if (profile.experience.isNotBlank()) 100.0 else 0.0
            val portfolioYield = Math.min(100.0, ((if (profile.portfolioLink.isNotBlank()) 50 else 0) + (profile.portfolioWorks.size) * 10).toDouble())

            // Standard SceneX Weights: P(30), S(25), E(18), C(15)
            val rankingDecimal = ((portfolioYield * 30.0) + (skillsYield * 25.0) + (experienceYield * 18.0) + (completeness * 15.0)) / 88.0

            // 5. Sync Scores back to Firestore
            val scoreUpdates = mapOf(
                "completenessScore" to completeness.toDouble(),
                "rankingScore" to rankingDecimal.coerceAtMost(100.0)
            )
            db.collection("profiles").document(userId).update(scoreUpdates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 🗑 NEW: Permanent removal of the talent account and data.
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

    /**
     * NEW: Add Portfolio Work after registration.
     */
    fun addPortfolioWork(work: PortfolioWork, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("profiles").document(userId)
                    .update("portfolioWorks", FieldValue.arrayUnion(work))
                    .await()
                recalculateScores(userId)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    /**
     * NEW: Delete Portfolio Work.
     */
    fun deletePortfolioWork(work: PortfolioWork, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("profiles").document(userId)
                    .update("portfolioWorks", FieldValue.arrayRemove(work))
                    .await()
                recalculateScores(userId)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    /**
     *  NEW: Edit Portfolio Work.
     * Replaces old work with updated one.
     */
    fun editPortfolioWork(oldWork: PortfolioWork, newWork: PortfolioWork, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val docRef = db.collection("profiles").document(userId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val works = snapshot.toObject(UserProfile::class.java)?.portfolioWorks?.toMutableList()
                        ?: mutableListOf()

                    val index = works.indexOfFirst {
                        it.title == oldWork.title && it.year == oldWork.year
                    }
                    if (index != -1) {
                        works[index] = newWork
                        transaction.update(docRef, "portfolioWorks", works)
                    }
                }.await()
                recalculateScores(userId)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
        mediaListener?.remove()
    }
}
