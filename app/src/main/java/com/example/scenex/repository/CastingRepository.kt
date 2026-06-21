package com.example.scenex.repository

import android.util.Log
import com.example.scenex.models.CastingCall
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CastingRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getAllCastingCalls(onUpdate: (List<CastingCall>) -> Unit) {
        Log.d("SceneX_Casting", "Fetching all casting calls...")
        
        // Temporarily removed orderBy to verify if any data exists at all
        db.collection("CastingCalls")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SceneX_Casting", "Firestore Error: ${error.message}")
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d("SceneX_Casting", "No documents found in 'CastingCalls' collection.")
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                Log.d("SceneX_Casting", "Found ${snapshot.size()} documents. Attempting to map...")

                try {
                    val calls = snapshot.toObjects(CastingCall::class.java)
                    Log.d("SceneX_Casting", "Successfully mapped ${calls.size} calls.")
                    onUpdate(calls)
                } catch (e: Exception) {
                    Log.e("SceneX_Casting", "Mapping Error: ${e.message}")
                    e.printStackTrace()
                    onUpdate(emptyList())
                }
            }
    }
}
