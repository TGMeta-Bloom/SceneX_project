package com.example.scenex.models

/**
 * Member 1 Intelligence Models.
 * Defines the result types for the rule-based calculation.
 */

enum class AvailabilityResult {
    AVAILABLE,
    BUSY
}

enum class AvailabilityStatus {
    AVAILABLE_NOW,
    BUSY_NOW,
    AVAILABLE_TODAY
}

data class AvailabilityIntelligence(
    val status: AvailabilityStatus,
    val nextAvailableGap: String? = null
)
