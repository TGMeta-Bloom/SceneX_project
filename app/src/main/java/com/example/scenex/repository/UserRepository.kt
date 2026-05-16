package com.example.scenex.repository

import android.util.Log
import com.example.scenex.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun saveUserRole(role: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}"
        
        val userMap = hashMapOf(
            "userId" to userId,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener { 
                Log.d("FirebaseDebug", "Role Save Success")
                onComplete(true) 
            }
            .addOnFailureListener { e -> 
                Log.e("FirebaseDebug", "Role Save Error: ${e.message}")
                onComplete(false) 
            }
    }

    fun signupUser(profile: UserProfile, password: String, onComplete: (Boolean, String?) -> Unit) {
        Log.d("FirebaseDebug", "Starting signup for: ${profile.email}")
        
        auth.createUserWithEmailAndPassword(profile.email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: ""
                Log.d("FirebaseDebug", "Auth Success. User ID: $userId")
                
                val userMap = hashMapOf(
                    "userId" to userId,
                    "fullName" to profile.fullName,
                    "email" to profile.email,
                    "stageName" to profile.stageName,
                    "role" to profile.role,
                    "profileImage" to profile.profileImage,
                    "phoneNumber" to profile.phoneNumber,
                    "age" to profile.age,
                    "gender" to profile.gender,
                    "province" to profile.province,
                    "city" to profile.city,
                    "relationshipStatus" to profile.relationshipStatus,
                    "hobbies" to profile.hobbies,
                    "bio" to profile.bio,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("users").document(userId)
                    .set(userMap)
                    .addOnSuccessListener {
                        Log.d("FirebaseDebug", "Firestore Save Success")
                        onComplete(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseDebug", "Firestore Error: ${e.message}")
                        onComplete(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDebug", "Auth Error: ${e.message}")
                onComplete(false, e.message)
            }
    }

    fun updateUserField(field: String, value: Any, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("users").document(userId)
            .update(field, value)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateUserFields(updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
