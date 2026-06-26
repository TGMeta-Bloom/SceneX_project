package com.example.scenex.models

data class ApplicantTalent(
    val talentId: String = "",
    val fullName: String = "",
    val spotlightCategory: String = "",
    val applicationStatus: String = "",
    val appliedAt: Long = 0L,
    val avatarUrl: String = ""
)
