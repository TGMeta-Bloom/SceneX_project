package com.example.scenex.utils

import com.example.scenex.models.AvailabilityResult
import com.example.scenex.models.AvailabilityStatus
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.*

/**
 * Member 1: Availability Logic Engine.
 *
 * A rule-based engine that calculates whether a Talent/Recruiter is FREE or BUSY
 * by analyzing event data. It does not store availability; it computes it.
 */
class AvailabilityEngine {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val amPmFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val standardSlots = listOf(
        "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00",
        "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00"
    )

    /**
     * Core Rule: Checks if a requested slot overlaps with any existing events.
     * Enhanced: Handles overnight/wrap-around time and robust scalar comparison.
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
     * Rule-Based Gap Finder: Finds the first open window.
     */
    fun findNextAvailableGap(allEvents: List<DocumentSnapshot>, isToday: Boolean = true): String? {
        val currentMinutes = if (isToday) {
            val now = Calendar.getInstance()
            now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        } else -1

        for (slot in standardSlots) {
            val parts = slot.split("-")
            val slotStart = parts[0]
            val slotEnd = parts[1]
            if (timeToMinutes(slotStart) > currentMinutes) {
                if (checkSlotAvailability(allEvents, slotStart, slotEnd) == AvailabilityResult.AVAILABLE) {
                    return slotStart
                }
            }
        }
        return null
    }

    /**
     * Intelligent Suggestion: Returns all available 1-hour slots for a given day.
     */
    fun findAllFreeTimeSlots(allEvents: List<DocumentSnapshot>, isToday: Boolean): List<String> {
        val currentMinutes = if (isToday) {
            val now = Calendar.getInstance()
            now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        } else -1

        return standardSlots.map { it.split("-")[0] }.filter {
            timeToMinutes(it) > currentMinutes &&
                    checkSlotAvailability(allEvents, it, addOneHour(it)) == AvailabilityResult.AVAILABLE
        }
    }

    private fun isOverlapping(doc: DocumentSnapshot, reqStart: String, reqEnd: String): Boolean {
        val ds = timeToMinutes(doc.getString("startTime") ?: "")
        var de = timeToMinutes(doc.getString("endTime") ?: "")
        val rs = timeToMinutes(reqStart)
        var re = timeToMinutes(reqEnd)

        //  Handle numerical ambiguity (e.g. 10:00 to 01:00 interpreted as overnight/wrap-around)
        if (de <= ds && de != 0) de += 1440
        if (re <= rs && re != 0) re += 1440

        return rs < de && re > ds
    }

    private fun isCurrentTimeInSlot(doc: DocumentSnapshot, currentTime: String): Boolean {
        val ds = timeToMinutes(doc.getString("startTime") ?: "")
        var de = timeToMinutes(doc.getString("endTime") ?: "")
        val now = timeToMinutes(currentTime)

        if (de <= ds && de != 0) de += 1440
        return now in ds until de
    }

    private fun addOneHour(time: String): String {
        val mins = (timeToMinutes(time) + 60) % 1440
        return String.format(Locale.US, "%02d:%02d", mins / 60, mins % 60)
    }

    /**
     * Robust Time Parser: Handles dots, colons, and AM/PM variants.
     */
    fun timeToMinutes(time: String): Int {
        if (time.isBlank()) return 0
        val cleanTime = time.replace(".", ":").replace(";", ":").trim()
        return try {
            val format = if (cleanTime.contains("AM", true) || cleanTime.contains("PM", true)) {
                SimpleDateFormat("hh:mm a", Locale.US)
            } else {
                SimpleDateFormat("HH:mm", Locale.US)
            }
            val date = format.parse(cleanTime)
            val cal = Calendar.getInstance().apply { this.time = date!! }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) { 0 }
    }

    /**
     * 🛡 UNIVERSAL DATE NORMALIZER: Aligns any format to yyyy-MM-dd for precision matching.
     */
    fun normalizeRawDate(rawDate: Any?): String {
        if (rawDate == null) return ""
        return try {
            when (rawDate) {
                is com.google.firebase.Timestamp -> isoDateFormat.format(rawDate.toDate())
                is Long -> isoDateFormat.format(Date(rawDate))
                is String -> {
                    val clean = rawDate.trim()
                    if (clean.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return clean
                    val formats = listOf("dd MMM yyyy", "dd-MM-yyyy", "dd/MM/yyyy", "MMM dd, yyyy")
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val parsed = sdf.parse(clean)
                            if (parsed != null) return isoDateFormat.format(parsed)
                        } catch (e: Exception) {}
                    }
                    clean
                }
                else -> rawDate.toString()
            }
        } catch (e: Exception) { "" }
    }
}
