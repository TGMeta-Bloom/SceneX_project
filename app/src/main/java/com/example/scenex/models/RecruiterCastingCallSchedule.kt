package com.example.scenex.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RecruiterCastingCallSchedule(
    val id: String = "",
    val recruiterId: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val directorName: String = "",
    val deadlineDate: String = "",
    val status: String = "ACTIVE",
    val appliedCount: Int = 0
) {
    // Required empty constructor for Firestore
    constructor() : this("", "", "", "", "", "", "", "", "", "", "ACTIVE", 0)
}
