package com.example.scenex.models

/**
 * Member 1: Dynamic Calendar State Models.
 */
enum class DayStatus {
    BUSY,                // 🟠 Confirmed Booking/Schedule (Orange)
    PENDING,             // 🟢 Pending Request (Green)
    CASTING_CALL,        // 🔵 Audition Guide (Blue)
    MANUAL_UNAVAILABLE,  // 🔴 Manually Set Unavailable (Red)
    FREE
}

data class CalendarEvent(
    val title: String,
    val time: String,
    val type: DayStatus,
    val description: String,
    val location: String = "",
    val otherParty: String = ""
)

data class CalendarDay(
    val dateString: String = "", // yyyy-MM-dd
    val dayOfMonth: String = "",
    val statuses: Set<DayStatus> = emptySet(),
    val events: List<CalendarEvent> = emptyList(),
    val isCurrentMonth: Boolean = true
)
