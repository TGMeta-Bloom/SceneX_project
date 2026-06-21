package com.example.scenex.models

import com.google.firebase.Timestamp

data class CastingCall(
    val id: String = "",
    val recruiterId: String = "",
    val posterUrl: String = "",
    val category: String = "Actor", // Added category (Actor/Dancer)
    
    // 1. Production Information
    val projectTitle: String = "",
    val productionType: String = "",
    val productionCompany: String = "",
    val directorName: String = "",
    val projectSynopsis: String = "",
    val productionLanguage: String = "",
    
    // 2. Audition Information
    val auditionType: String = "",
    val auditionDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val auditionLocation: String = "",
    val submissionDeadline: String = "",
    
    // 3. Role Information
    val characterName: String = "",
    val minAge: Int = 15,
    val maxAge: Int = 100,
    val genderRequirement: String = "",
    val roleType: String = "",
    val requiredSkills: String = "",
    val experienceLevel: String = "",
    val characterBreakdown: String = "",
    
    // 4. Compensation & Schedule
    val compensation: String = "",
    val shootLocation: String = "",
    val firstDayOfShoot: String = "",
    
    // 5. Recruiter Contact
    val contactEmail: String = "",
    val phoneNumber: String = "",
    
    val createdAt: Timestamp = Timestamp.now()
)
