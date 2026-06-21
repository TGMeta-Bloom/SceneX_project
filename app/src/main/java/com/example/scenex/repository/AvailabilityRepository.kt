package com.example.scenex.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * Member 1: Availability Data Bridge.
 * Optimized: Uses Range Indexes for Bookings/Schedules.
 * Fixed: CastingCalls fetch is now casing-resilient for Recruiters.
 */
class AvailabilityRepository {

    private val db = FirebaseFirestore.getInstance()

    private suspend fun getUserRole(userId: String): String? {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            val role = userDoc.getString("role")
            if (role != null) return role
            db.collection("profiles").document(userId).get().await().getString("role")
        } catch (e: Exception) { null }
    }

    suspend fun getAvailabilityContext(userId: String, start: Long, end: Long): RoleContext = coroutineScope {
        val role = getUserRole(userId) ?: "Talent"
        val isRecruiter = role.equals("Recruiter", ignoreCase = true)

        // 🚀 Range queries for Bookings and Schedules (efficient use of indexes)
        val asTalentB = async { 
            db.collection("bookings")
                .whereEqualTo("talentId", userId)
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThanOrEqualTo("date", end)
                .get().await() 
        }
        val asRecruiterB = async { 
            db.collection("bookings")
                .whereEqualTo("recruiterId", userId)
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThanOrEqualTo("date", end)
                .get().await() 
        }
        
        val asTalentS = async { 
            db.collection("schedules")
                .whereEqualTo("userId", userId) 
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThanOrEqualTo("date", end)
                .get().await() 
        }
        val asRecruiterS = async { 
            db.collection("schedules")
                .whereEqualTo("recruiterId", userId)
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThanOrEqualTo("date", end)
                .get().await() 
        }
        
        // 🎯 FIX: Fetch all to allow casing-resilient filtering in the ViewModel
        val castingCalls = async {
            db.collection("CastingCalls").get().await()
        }

        val allBookings = asTalentB.await().documents + asRecruiterB.await().documents
        val allSchedules = asTalentS.await().documents + asRecruiterS.await().documents
        
        RoleContext(allBookings, allSchedules, castingCalls.await().documents, role)
    }

    suspend fun getAvailabilityRange(userId: String, start: Long, end: Long): Triple<List<DocumentSnapshot>, List<DocumentSnapshot>, List<DocumentSnapshot>> = coroutineScope {
        val context = getAvailabilityContext(userId, start, end)
        Triple(context.bookings, context.schedules, context.castingCalls)
    }

    suspend fun getDailyEvents(userId: String, timestamp: Long): Triple<List<DocumentSnapshot>, List<DocumentSnapshot>, List<DocumentSnapshot>> = coroutineScope {
        val context = getAvailabilityContext(userId, timestamp, timestamp + 86399999)
        Triple(context.bookings, context.schedules, context.castingCalls)
    }
}

data class RoleContext(
    val bookings: List<DocumentSnapshot>,
    val schedules: List<DocumentSnapshot>,
    val castingCalls: List<DocumentSnapshot>,
    val role: String
)
