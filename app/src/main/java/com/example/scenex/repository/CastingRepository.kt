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
        
        // 🎯 LOGIC: Only fetch calls that haven't expired yet
        db.collection("CastingCalls")
            .whereEqualTo("status", "active")
            .whereGreaterThan("expiryDate", now)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SceneX_Casting", "Firestore Error: ${error.message}")
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                try {
                    val calls = snapshot.toObjects(CastingCall::class.java)
                    onUpdate(calls)
                } catch (e: Exception) {
                    Log.e("SceneX_Casting", "Mapping Error: ${e.message}")
                    onUpdate(emptyList())
                }
            }
    }
}
