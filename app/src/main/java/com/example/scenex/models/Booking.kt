package com.example.scenex.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Booking(
    val id: String = "",
    val castingCallId: String = "",
    val castingTitle: String = "",
    val recruiterId: String = "",
    val recruiterName: String = "",
    val talentId: String = "",
    val name: String = "", // talent name
    val role: String = "", // spotlight category/role
    val projectImageUrl: String = "",
    val date: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val location: String = "",
    val status: String = "PENDING",
    val notes: String = "",
    val hasConflict: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "", "", "", "", "", 0L, "", "", "", "PENDING", "", false, System.currentTimeMillis())
}
