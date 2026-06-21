package com.example.scenex.models

/**
 * Member 3: Standard Message Model.
 * Represents a single piece of communication within a booking thread.
 */
data class Message(
    val messageId: String = "",
    val bookingId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val status: String = "SENT", // SENT, DELIVERED, SEEN
    val type: String = "NORMAL" // NORMAL, RESCHEDULE
)
