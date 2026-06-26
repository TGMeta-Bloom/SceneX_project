package com.example.scenex.repository

import android.util.Log
import com.example.scenex.models.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Senior Technical Implementation: SceneX Data Bridge.
 * Unified with security handshake for Admin approval enforcement.
 */
class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * REAL-TIME SYNC: Listens for Admin-side weight/score changes.
     */
    fun listenToProfileData(userId: String, onUpdate: (Map<String, Any>?) -> Unit): ListenerRegistration {
        return db.collection("profiles").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                onUpdate(snapshot?.data)
            }
    }

    /**
     * 🎯 NEW: Manual Availability status override persistence.
     */
    fun updateManualAvailability(status: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        val updates = hashMapOf<String, Any>(
            "manualAvailabilityStatus" to status,
            "mediaAssets.manualAvailabilityStatus" to status
        )
        db.collection("profiles").document(userId).update(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /** 
     * HANDSHAKE PROTOCOL: 
     * Provides role, status, and verificationStatus for secure app routing.
     * Signature includes Exception for detailed error handling in UI.
     */
    fun getUserRoutingData(userId: String, onResult: (String?, String?, String?, Exception?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val role = userDoc.getString("role")
                db.collection("profiles").document(userId).get()
                    .addOnSuccessListener { profileDoc ->
                        val status = profileDoc.getString("status")
                        val vStatus = profileDoc.getString("verificationStatus")
                        onResult(role, status, vStatus, null)
                    }
                    .addOnFailureListener { onResult(role, null, null, it) }
            }
            .addOnFailureListener { onResult(null, null, null, it) }
    }

    /** RESTORED: Required by RoleSelectViewModel */
    fun saveUserRole(role: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        val userMap = hashMapOf("userId" to userId, "role" to role, "createdAt" to System.currentTimeMillis())
        db.collection("users").document(userId).set(userMap, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /** RESTORED: Required by WaitingRoomActivity */
    fun listenToProfileStatus(userId: String, onStatusUpdate: (String?, String?) -> Unit): ListenerRegistration {
        return db.collection("profiles").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    val vStatus = snapshot.getString("verificationStatus")
                    onStatusUpdate(status, vStatus)
                }
            }
    }

    /** signup logic */
    fun signupUser(profile: UserProfile, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(profile.email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: ""
                val userMap = hashMapOf("userId" to userId, "role" to profile.role, "email" to profile.email)
                val profileMap = hashMapOf(
                    "userId" to userId,
                    "fullName" to profile.fullName,
                    "status" to "draft",
                    "completenessScore" to 20.0,
                    "manualAvailabilityStatus" to "AVAILABLE",
                    "mediaAssets" to mapOf("manualAvailabilityStatus" to "AVAILABLE")
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

    suspend fun deleteUserAccount(): Boolean {
        val user = auth.currentUser ?: return false
        val userId = user.uid
        return try {
            val batch = db.batch()
            batch.delete(db.collection("users").document(userId))
            batch.delete(db.collection("profiles").document(userId))
            batch.delete(db.collection("media_assets").document(userId))
            batch.delete(db.collection("talent_specs").document(userId))
            batch.commit().await()
            user.delete().await()
            true
        } catch (e: Exception) {
            Log.e("SceneX_Repo", "Deletion Error", e)
            false
        }
    }

    fun signOut() = auth.signOut()
}
