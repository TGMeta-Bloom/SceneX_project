package com.example.scenex.repository

import android.util.Log
import com.example.scenex.models.CastingCall
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CastingRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getAllCastingCalls(onUpdate: (List<CastingCall>) -> Unit) {
        val now = Timestamp.now()
        
        // 🎯 FIX: Removed .orderBy() to avoid mandatory Composite Index requirement.
        // We now fetch all active calls and handle sorting/filtering in memory for better reliability.
        db.collection("CastingCalls")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SceneX_Casting", "Firestore Error: ${error.message}")
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d("SceneX_Casting", "No active casting calls found in DB.")
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                try {
                    val allCalls = snapshot.toObjects(CastingCall::class.java)
                    
                    // 🎯 In-Memory Processing: Filter by expiry and sort by creation date
                    val validCalls = allCalls.filter { call ->
                        // Show if expiryDate is null (permanent) or if it's in the future
                        call.expiryDate == null || call.expiryDate!!.seconds > now.seconds
                    }.sortedByDescending { it.createdAt }
                    
                    Log.d("SceneX_Casting", "Displaying ${validCalls.size} casting calls.")
                    onUpdate(validCalls)
                } catch (e: Exception) {
                    Log.e("SceneX_Casting", "Mapping Error: ${e.message}")
                    onUpdate(emptyList())
                }
            }
    }
}
