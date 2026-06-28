package com.example.scenex.models

import com.google.firebase.firestore.PropertyName

/**
 * Structured model for a Talent's professional work entry.
 */
data class PortfolioWork(
    val title: String = "",
    val projectType: String = "",
    val rolePlayed: String = "",
    val description: String = "",
    val year: String = "",
    val imageUrl: String? = null
)

/**
 * Data model for SceneX User Profiles.
 */
data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val stageName: String = "",
    val role: String = "",
    val userRole: String = "",
    val profileImage: String = "",
    val profileImageUrl: String = "",
    val headshotUrl: String = "",
    val fullBodyUrl: String = "",
    val phoneNumber: String = "",
    val age: Int = 0,
    val gender: String = "",
    val province: String = "",
    val city: String = "",
    val relationshipStatus: String = "",
    val hobbies: String = "",
    val bio: String = "",
    val physicalSpecs: String = "",

    // Physical Specs (Raw)
    val height: String = "",
    val hairColor: String = "",
    val eyeColor: String = "",
    val bodyType: String = "",
    val accents: List<String> = emptyList(),
    val otherSkills: List<String> = emptyList(),

    val mediaAssets: Map<String, String> = emptyMap(),

    // Professional Metadata
    val spotlightCategory: String = "",
    @get:PropertyName("highest_qualification") @set:PropertyName("highest_qualification") var qualification: String = "",
    val languages: String = "",
    val skills: List<String> = emptyList(),
    @get:PropertyName("experience_level") @set:PropertyName("experience_level") var experience: String = "",
    val portfolioLink: String = "",
    val showreelUrl: String = "",
    val videoUrl: String = "",
    val audioUrl: String = "",
    val socialMediaLinks: String = "",

    // Structured Portfolio Works
    val portfolioWorks: List<PortfolioWork> = emptyList(),

    // Recruiter Specific
    val companyName: String = "",
    val industryProofLinks: List<String> = emptyList(),
    val nicImageUrl: String = "",

    // --- SCENEX DATA ENGINE FIELDS ---
    val calculated_score: Double = 0.0,
    val rankingScore: Double = 0.0,
    val completenessScore: Double = 0.0,
    val rating: Float = 0f,

    val visibility_tier: String = "NORMAL",
    val status: String = "draft",
    val verificationStatus: String = "unverified",

    val createdAt: Long = System.currentTimeMillis()
) {
    val effectiveAvatarUrl: String
        get() {
            return when {
                profileImage.isNotBlank() -> profileImage
                profileImageUrl.isNotBlank() -> profileImageUrl
                mediaAssets["profileImage"] != null -> mediaAssets["profileImage"]!!
                else -> "https://ui-avatars.com/api/?name=${fullName.replace(" ", "+")}&background=B0006D&color=fff"
            }
        }
}
