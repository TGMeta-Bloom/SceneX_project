package com.example.scenex.models

/**
 * Data model for SceneX User Profiles.
 * Updated to support the Weighted Profile Completion Engine and Ranking System.
 */
data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val stageName: String = "",
    val role: String = "", // "TALENT" or "RECRUITER"
    val profileImage: String = "",
    val phoneNumber: String = "",
    val age: Int = 0,
    val gender: String = "",
    val province: String = "",
    val city: String = "",
    val relationshipStatus: String = "",
    val hobbies: String = "",
    val bio: String = "",
    
    // Professional Metadata
    val spotlightCategory: String = "",
    val qualification: String = "",
    val languages: String = "",
    val experience: String = "",
    val portfolioLink: String = "",
    val socialMediaLinks: String = "",
    
    // Recruiter Specific (SL Industry Standard)
    val companyName: String = "",
    val industryType: String = "",
    val industryProofLinks: List<String> = emptyList(),
    val nicImageUrl: String = "",
    
    // Weighted Scoring Engine Fields
    val completenessScore: Int = 0, // 0-100 based on weightage
    val rankingScore: Int = 0,      // Search priority score
    val status: String = "draft",   // draft, active, eligible_for_review, pending_review, verified
    
    val verificationStatus: String = "pending_review",
    val createdAt: Long = System.currentTimeMillis()
)
