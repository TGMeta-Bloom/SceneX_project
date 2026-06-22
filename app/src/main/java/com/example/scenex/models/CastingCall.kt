package com.example.scenex.models

import com.google.firebase.Timestamp

/**
 * Casting Call Data Model.
 * Includes logic fields for status tracking and expiration.
 */
data class CastingCall(
    val id: String = "",
    val recruiterId: String = "",
    val posterUrl: String = "",
    val category: String = "Actor",
    
    // 1. Production Information
    val projectTitle: String = "",
    val productionType: String = "",
    val productionCompany: String = "",
    val directorName: String = "",
    val projectSynopsis: String = "",
    val productionLanguage: String = "",
    
    // 2. Audition Hub
    val auditionType: String = "",
    val auditionDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val auditionLocation: String = "",
    val submissionDeadline: String = "",
    val expiryDate: Timestamp? = null,
    
    // 3. Talent Specs
    val characterName: String = "",
    val minAge: Int = 0,
    val maxAge: Int = 100,
    val genderRequirement: String = "Any",
    val roleType: String = "",
    val requiredSkills: String = "",
    val experienceLevel: String = "",
    val characterBreakdown: String = "",
    
    // 4. Logistics
    val compensation: String = "",
    val shootLocation: String = "",
    val firstDayOfShoot: String = "",
    
    // 5. Contact Info
    val contactEmail: String = "",
    val phoneNumber: String = "",
    
    // 6. Metadata
    val status: String = "active",
    val createdAt: Timestamp = Timestamp.now()
)
