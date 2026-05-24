package com.example.scenex.models

data class Schedule(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val type: String = "OTHER", // AUDITION, SHOOT, OTHER
    val location: String = "",
    val date: Long = 0L,
    val startTime: String = "",
    val endTime: String = "",
    val isAllDay: Boolean = false,
    val description: String = "",
    val alert: String = "",
    val notes: String = ""
)
