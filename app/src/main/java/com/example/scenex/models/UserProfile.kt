package com.example.scenex.models

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val stageName: String = "",
    val role: String = "", // "TALENT" or "RECRUITER"
    val profileImage: String = "",
    val handle: String = "",
    val bio: String = "",
    val location: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
