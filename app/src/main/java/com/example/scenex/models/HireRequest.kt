package com.example.scenex.models

data class HireRequest(
    val requestId: String = "",
    val recruiterId: String = "",
    val talentId: String = "",
    val castingId: String = "",
    val projectTitle: String = "",
    val description: String = "",
    val payment: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val location: String = "",
    val message: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, RESCHEDULED, BOOKED
    val proposedDate: String? = null,
    val proposedTime: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
