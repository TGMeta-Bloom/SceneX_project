package com.example.scenex.repository

import android.util.Log
import com.example.scenex.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "SceneX_Repository"

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun listenToProfileData(userId: String, onUpdate: (Map<String, Any>?) -> Unit): ListenerRegistration {
        return db.collection("profiles").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                onUpdate(snapshot?.data)
            }
    }

    /**
     * BULLETPROOF ROUTING: Checks both 'users' and 'profiles' collections.
     * Checks both 'role' and 'userRole' field names.
     */
    fun getUserRoutingData(userId: String, onResult: (String?, String?, Exception?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                var role = userDoc.getString("role") ?: userDoc.getString("userRole")
                
                db.collection("profiles").document(userId).get()
                    .addOnSuccessListener { profileDoc ->
                        val profileRole = profileDoc.getString("userRole") ?: profileDoc.getString("role")
                        val status = profileDoc.getString("status")
                        
                        // Use the role from either document, prioritizing profile if it exists
                        val finalRole = profileRole ?: role
                        onResult(finalRole, status, null)
                    }
                    .addOnFailureListener { 
                        // If profile doesn't exist, we still have the role from 'users'
                        onResult(role, null, null)
                    }
            }
            .addOnFailureListener { onResult(null, null, it) }
    }

    fun saveUserRole(role: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        val userMap = hashMapOf("userId" to userId, "role" to role, "userRole" to role, "createdAt" to System.currentTimeMillis())
        
        // Save to both for safety
        val batch = db.batch()
        batch.set(db.collection("users").document(userId), userMap, SetOptions.merge())
        batch.set(db.collection("profiles").document(userId), hashMapOf("userRole" to role), SetOptions.merge())
        
        batch.commit()
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

    fun signupUser(profile: UserProfile, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(profile.email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: ""
                val userMap = hashMapOf("userId" to userId, "role" to profile.role, "userRole" to profile.role, "email" to profile.email)
                val profileMap = hashMapOf(
                    "userId" to userId, 
                    "fullName" to profile.fullName, 
                    "userRole" to profile.role,
                    "status" to "draft", 
                    "completenessScore" to 20.0
                )
                val batch = db.batch()
                batch.set(db.collection("users").document(userId), userMap)
                batch.set(db.collection("profiles").document(userId), profileMap)
                batch.commit().addOnSuccessListener { onComplete(true, null) }
            }
            .addOnFailureListener { onComplete(false, it.message) }
    }

    fun getProfileData(userId: String, onComplete: (Map<String, Any>?) -> Unit) {
        db.collection("profiles").document(userId).get().addOnSuccessListener { onComplete(it.data) }
    }

    fun getRankingCalibration(onComplete: (Map<String, Any>?) -> Unit) {
        db.collection("ranking_calibration").document("weights").get().addOnSuccessListener { onComplete(it.data) }
    }

    fun saveProfessionalProfile(updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("profiles").document(userId).set(updates, SetOptions.merge()).addOnSuccessListener { onComplete(true) }
    }

    fun saveTalentSpecs(specs: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("talent_specs").document(userId).set(specs, SetOptions.merge()).addOnSuccessListener { onComplete(true) }
    }

    fun saveMediaAssets(assets: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("media_assets").document(userId).set(assets, SetOptions.merge()).addOnSuccessListener { onComplete(true) }
    }

    fun signOut() = auth.signOut()
}
