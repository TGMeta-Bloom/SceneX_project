package com.example.scenex.utils

import com.example.scenex.models.AvailabilityResult
import com.example.scenex.models.AvailabilityStatus
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.*

/**
 * Member 1: Availability Logic Engine.
 * 
 * A rule-based engine that calculates whether a Talent is FREE or BUSY
 * by analyzing event data. It does not store availability; it computes it.
 */
class AvailabilityEngine {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val amPmFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private val standardSlots = listOf(
        "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
        "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00"
    )

    /**
     * Core Rule: Checks if a requested slot overlaps with any existing events.
     */
    fun checkSlotAvailability(
        allEvents: List<DocumentSnapshot>,
        startTime: String,
        endTime: String
    ): AvailabilityResult {
        val hasConflict = allEvents.any { isOverlapping(it, startTime, endTime) }
        return if (hasConflict) AvailabilityResult.BUSY else AvailabilityResult.AVAILABLE
    }

    /**
     * Calculates the dynamic status (FREE/BUSY) based on the current system time.
     */
    fun calculateCurrentStatus(allEvents: List<DocumentSnapshot>): AvailabilityStatus {
        val now = Calendar.getInstance()
        val currentTimeStr = timeFormat.format(now.time)
        val isBusyNow = allEvents.any { isCurrentTimeInSlot(it, currentTimeStr) }

        return if (isBusyNow) {
            AvailabilityStatus.BUSY_NOW
        } else {
            if (allEvents.isNotEmpty()) AvailabilityStatus.AVAILABLE_TODAY 
            else AvailabilityStatus.AVAILABLE_NOW
        }
    }

    /**
     * Rule-Based Gap Finder: Finds the first open window for the Talent.
     */
    fun findNextAvailableGap(allEvents: List<DocumentSnapshot>): String? {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (slot in standardSlots) {
            val parts = slot.split("-")
            val slotStart = parts[0]
            val slotEnd = parts[1]
            if (timeToMinutes(slotStart) > currentMinutes) {
                if (checkSlotAvailability(allEvents, slotStart, slotEnd) == AvailabilityResult.AVAILABLE) {
                    return slot.split("-")[0]
                }
            }
        }
        return null
    }

    private fun isOverlapping(doc: DocumentSnapshot, reqStart: String, reqEnd: String): Boolean {
        val ds = timeToMinutes(doc.getString("startTime") ?: "")
        val de = timeToMinutes(doc.getString("endTime") ?: "")
        val rs = timeToMinutes(reqStart)
        val re = timeToMinutes(reqEnd)
        return rs < de && re > ds
    }

    private fun isCurrentTimeInSlot(doc: DocumentSnapshot, currentTime: String): Boolean {
        val ds = timeToMinutes(doc.getString("startTime") ?: "")
        val de = timeToMinutes(doc.getString("endTime") ?: "")
        val now = timeToMinutes(currentTime)
        return now in ds until de
    }

    private fun timeToMinutes(time: String): Int {
        if (time.isBlank()) return 0
        return try {
            val format = if (time.contains("AM", true) || time.contains("PM", true)) amPmFormat else timeFormat
            val date = format.parse(time.trim())
            val cal = Calendar.getInstance().apply { this.time = date!! }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) { 0 }
    }
}
