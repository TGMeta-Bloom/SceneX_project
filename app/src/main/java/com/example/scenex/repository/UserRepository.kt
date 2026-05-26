package com.example.scenex.repository

import android.util.Log
import com.example.scenex.models.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Fetches complete profile data for the Home Screen.
     */
    fun getProfileData(userId: String, onComplete: (Map<String, Any>?) -> Unit) {
        db.collection("profiles").document(userId).get()
            .addOnSuccessListener { document ->
                onComplete(document.data)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    /**
     * Fetches dynamic ranking weights from the admin calibration collection.
     */
    fun getRankingCalibration(onComplete: (Map<String, Any>?) -> Unit) {
        db.collection("ranking_calibration").document("weights").get()
            .addOnSuccessListener { document ->
                onComplete(document.data)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    /**
     * Saves user role selection to the 'users' collection.
     */
    fun saveUserRole(role: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}"
        val userMap = hashMapOf(
            "userId" to userId,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("users").document(userId)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Updates profile data in the 'profiles' collection.
     */
    fun saveProfessionalProfile(updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("profiles").document(userId)
            .set(updates + ("userId" to userId), SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Submits the talent profile for administrative review.
     */
    fun submitTalentProfileToAdmin(userId: String, fullName: String, userEmail: String, heightAndBuild: String, youtubeLink: String) {
        val profilePayload = hashMapOf(
            "status" to "pending_review",
            "name" to fullName,
            "email" to userEmail,
            "physicalSpecs" to heightAndBuild,
            "showreelUrl" to youtubeLink,
            "updatedAt" to Timestamp.now()
        )

        db.collection("profiles").document(userId)
            .set(profilePayload, SetOptions.merge())
            .addOnSuccessListener { Log.d("SceneX_Sync", "✅ Profile submitted for review.") }
            .addOnFailureListener { e -> Log.e("SceneX_Sync", "❌ Submission failed: ${e.message}") }
    }

    /**
     * Senior Fix: Dynamically assigns the role from the profile object.
     * Prevents Recruiter accounts from being labeled as 'TALENT' in the users collection.
     */
    fun signupUser(profile: UserProfile, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(profile.email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: ""
                
                // FIXED: Use profile.role instead of hardcoded "TALENT"
                val userMap = hashMapOf(
                    "userId" to userId, 
                    "role" to profile.role, 
                    "email" to profile.email
                )
                
                val profileMap = hashMapOf(
                    "userId" to userId, 
                    "fullName" to profile.fullName, 
                    "profileImage" to profile.profileImage,
                    "status" to "draft", 
                    "completenessScore" to 20
                )
                
                val batch = db.batch()
                batch.set(db.collection("users").document(userId), userMap)
                batch.set(db.collection("profiles").document(userId), profileMap)
                batch.commit().addOnSuccessListener { onComplete(true, null) }
            }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun getUserRoutingData(userId: String, onResult: (String?, String?, Exception?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val role = userDoc.getString("role")
                db.collection("profiles").document(userId).get()
                    .addOnSuccessListener { profileDoc ->
                        val status = profileDoc.getString("status")
                        onResult(role, status, null)
                    }
                    .addOnFailureListener { onResult(role, null, it) }
            }
            .addOnFailureListener { onResult(null, null, it) }
    }

    fun saveTalentSpecs(specs: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("talent_specs").document(userId).set(specs + ("userId" to userId), SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun saveMediaAssets(assets: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("media_assets").document(userId).set(assets + ("userId" to userId), SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun listenToProfileStatus(userId: String, onStatusChange: (String?) -> Unit): ListenerRegistration {
        return db.collection("profiles").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    onStatusChange(snapshot.getString("status"))
                }
            }
    }

    fun updateFirestoreField(collection: String, field: String, value: Any, onComplete: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: return
        db.collection(collection).document(userId).update(field, value)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun syncProfileLifecycle(userId: String, payload: Map<String, Any>, onComplete: (Boolean) -> Unit = {}) {
        db.collection("profiles").document(userId)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun signOut() = auth.signOut()
}
