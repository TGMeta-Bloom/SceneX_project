package com.example.scenex.models

/**
 * Data model for SceneX User Profiles.
 * Optimized for strict Identity Asset separation (Profile Picture vs Portfolio Headshot).
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
    val physicalSpecs: String = "", // Added for Portfolio Detail View
    
    // Nested assets map from backend standard
    val mediaAssets: Map<String, String> = emptyMap(),
    
    // Professional Metadata
    val spotlightCategory: String = "",
    val qualification: String = "",
    val languages: String = "",
    val experience: String = "",
    val portfolioLink: String = "",
    val showreelUrl: String = "",
    val socialMediaLinks: String = "",
    
    // Recruiter Specific
    val companyName: String = "",
    val industryProofLinks: List<String> = emptyList(),
    val nicImageUrl: String = "",
    
    // --- SCENEX DATA ENGINE FIELDS ---
    val calculated_score: Double = 0.0, 
    val rankingScore: Double = 0.0,      
    val completenessScore: Double = 0.0, 
    
    val visibility_tier: String = "NORMAL",
    val status: String = "draft",   
    val verificationStatus: String = "unverified",
    
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * MASTER IDENTITY RESOLVER:
     * Strictly prioritizes the Official Profile Image (Account Photo) for avatars.
     * Uses UI-Avatars initials as the final fail-safe.
     */
    val effectiveAvatarUrl: String
        get() {
            return when {
                // 🎯 IDENTITY FIRST: Use account-level profile images
                profileImage.isNotBlank() -> profileImage
                profileImageUrl.isNotBlank() -> profileImageUrl
                mediaAssets["profileImage"] != null -> mediaAssets["profileImage"]!!
                
                // FINAL FALLBACK: Initials (Never use professional headshots for identity circles)
                else -> "https://ui-avatars.com/api/?name=${fullName.replace(" ", "+")}&background=B0006D&color=fff"
            }
        }
}
