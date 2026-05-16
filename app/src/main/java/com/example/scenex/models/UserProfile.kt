package com.example.scenex.models

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val stageName: String = "",
    val role: String = "", // "TALENT" or "RECRUITER"
    val profileImage: String = "",
    val phoneNumber: String = "",
    val age: String = "",
    val gender: String = "",
    val province: String = "",
    val city: String = "",
    val relationshipStatus: String = "",
    val hobbies: String = "",
    val bio: String = "",
    val spotlightCategory: String = "",
    val qualification: String = "",
    val languages: String = "",
    val experience: String = "",
    val portfolioLink: String = "",
    val socialMediaLinks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)