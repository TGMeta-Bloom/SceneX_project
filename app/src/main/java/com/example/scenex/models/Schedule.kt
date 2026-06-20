package com.example.scenex.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Schedule(
    val id: String = "",           // scheduleId
    val bookingId: String = "",
    val castingCallId: String = "",
    val castingTitle: String = "",
    val recruiterId: String = "",
    val recruiterName: String = "", // Added this to show to Talent
    val userId: String = "",       // talentId
    val name: String = "",         // talent name
    val role: String = "",
    val date: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val location: String = "",
    val status: String = "CONFIRMED",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
