package com.example.scenex.models

data class Booking(
    val id: String = "",
    val projectTitle: String = "",
    val recruiterName: String = "",
    val projectImageUrl: String = "",
    val date: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val location: String = "",
    val status: String = "PENDING", // PENDING, CONFIRMED, CANCELLED, COMPLETED
    val talentId: String = "",
    val hasConflict: Boolean = false
)
