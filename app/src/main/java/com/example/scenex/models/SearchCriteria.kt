package com.example.scenex.models

/**
 * Apex Data Model: Sophisticated search state for SceneX Discovery.
 * Structured for precise Sri Lankan geographic and physical filtering.
 */
data class SearchCriteria(
    val query: String = "",
    val location: String = "",   
    val province: String = "",   
    val city: String = "",       
    val gender: String = "Any",
    val minAge: Int = 18,
    val maxAge: Int = 60,
    val minHeight: Int = 140, // in cm
    val maxHeight: Int = 200, // in cm
    val languages: List<String> = emptyList(),
    val minRating: Float = 0f
)
