package com.example.scenex.repository

import com.example.scenex.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * RESTORED: Required for real-time scoring updates.
     */
    fun listenToProfileData(userId: String, onUpdate: (Map<String, Any>?) -> Unit): ListenerRegistration {
        return db.collection("profiles").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                onUpdate(snapshot?.data)
            }
    }

    /**
     * Real-time listener for profile status changes.
     */
    fun listenToProfileStatus(userId: String, onStatusUpdate: (String?) -> Unit): ListenerRegistration {
        return db.collection("profiles").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val status = snapshot?.getString("status")
                onStatusUpdate(status)
            }
    }

    /**
     * BULLETPROOF ROUTING: Checks both 'users' and 'profiles' for identity resolution.
     */
    fun getUserRoutingData(userId: String, onResult: (String?, String?, Exception?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val role = userDoc.getString("role") ?: userDoc.getString("userRole")
                db.collection("profiles").document(userId).get()
                    .addOnSuccessListener { profileDoc ->
                        val profileRole = profileDoc.getString("userRole") ?: profileDoc.getString("role")
                        onResult(profileRole ?: role, profileDoc.getString("status"), null)
                    }
                    .addOnFailureListener { onResult(role, null, null) }
            }
            .addOnFailureListener { onResult(null, null, it) }
    }

    /**
     * RESTORED: Required for Search Flow scoring logic.
     */
    fun getRankingCalibration(onComplete: (Map<String, Any>?) -> Unit) {
        db.collection("ranking_calibration").document("weights").get()
            .addOnSuccessListener { onComplete(it.data) }
            .addOnFailureListener { onComplete(null) }
    }

    fun saveUserRole(role: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        val data = hashMapOf("userId" to userId, "role" to role, "userRole" to role, "updatedAt" to System.currentTimeMillis())
        val batch = db.batch()
        batch.set(db.collection("users").document(userId), data, SetOptions.merge())
        batch.set(db.collection("profiles").document(userId), hashMapOf("userRole" to role), SetOptions.merge())
        batch.commit().addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    fun signupUser(profile: UserProfile, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(profile.email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: ""
                val profileMap = hashMapOf("userId" to userId, "email" to profile.email, "userRole" to profile.role, "status" to "draft")
                db.collection("profiles").document(userId).set(profileMap)
                    .addOnSuccessListener { onComplete(true, null) }
            }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    /**
     * RESTORED: Fragment-specific profile update methods.
     */
    fun saveProfessionalProfile(updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("profiles").document(userId).set(updates, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun saveMediaAssets(assets: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("media_assets").document(userId).set(assets, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun signOut() = auth.signOut()
}
