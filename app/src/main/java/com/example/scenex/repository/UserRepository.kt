package com.example.scenex.repository

import com.example.scenex.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Fix for RoleSelectViewModel
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

    // Fix for Signup Process
    fun signupUser(profile: UserProfile, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(profile.email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: ""
                
                // 1. users table (Auth strictly)
                val userMap = hashMapOf(
                    "userId" to userId,
                    "email" to profile.email,
                    "phoneNumber" to profile.phoneNumber,
                    "role" to profile.role,
                    "stageName" to profile.stageName,
                    "createdAt" to System.currentTimeMillis()
                )

                // 2. profiles table (Foundation)
                val profileMap = hashMapOf(
                    "userId" to userId,
                    "fullName" to profile.fullName,
                    "age" to profile.age,
                    "gender" to profile.gender,
                    "province" to profile.province,
                    "city" to profile.city,
                    "relationshipStatus" to profile.relationshipStatus,
                    "hobbies" to profile.hobbies,
                    "bio" to profile.bio,
                    "completenessScore" to 20
                )

                val batch = db.batch()
                batch.set(db.collection("users").document(userId), userMap, SetOptions.merge())
                batch.set(db.collection("profiles").document(userId), profileMap, SetOptions.merge())

                batch.commit()
                    .addOnSuccessListener { onComplete(true, null) }
                    .addOnFailureListener { e -> onComplete(false, e.message) }
            }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }

    fun saveProfessionalProfile(updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("profiles").document(userId)
            .set(updates + ("userId" to userId), SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun saveTalentSpecs(specs: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("talent_specs").document(userId)
            .set(specs + ("userId" to userId), SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun saveMediaAssets(assets: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("media_assets").document(userId)
            .set(assets + ("userId" to userId), SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateFirestoreField(collection: String, field: String, value: Any, onComplete: (Boolean) -> Unit = {}) {
        val userId = auth.currentUser?.uid ?: return
        db.collection(collection).document(userId).update(field, value)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
