package com.example.scenex.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scenex.models.*
import com.example.scenex.repository.AvailabilityRepository
import com.example.scenex.repository.UserRepository
import com.example.scenex.utils.AvailabilityEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Member 1 Implementation: SceneX Availability Intelligence (MVVM).
 * Updated: Manual Override integration with Priority Logic.
 */
class AvailabilityViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val repository = AvailabilityRepository()
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val engine = AvailabilityEngine()

    private val _calendarDays = MutableLiveData<List<CalendarDay>>()
    val calendarDays: LiveData<List<CalendarDay>> = _calendarDays

    private val _currentMonthText = MutableLiveData<String>()
    val currentMonthText: LiveData<String> = _currentMonthText

    private val _syncError = MutableLiveData<String?>()
    val syncError: LiveData<String?> = _syncError

    // 🎯 NEW: Combined Availability Logic (Manual + Calendar)
    private val _calculatedStatus = MutableLiveData<String>("🟢 Available Now")
    val calculatedStatus: LiveData<String> = _calculatedStatus

    private val _manualStatusPreference = MutableLiveData<String>("AVAILABLE")
    val manualStatusPreference: LiveData<String> = _manualStatusPreference

    // Use UTC for internal calendar state to prevent date shifting
    private var currentMonth = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    /**
     * 🎯 INTELLIGENCE SYNC: Resolves final status based on Priority:
     * 1. MANUALLY_UNAVAILABLE (🔴 Unavailable)
     * 2. BUSY_NOW via Engine (🟠 Busy Now)
     * 3. AVAILABLE (🟢 Available Now)
     *
     * Refactored: Uses Snapshot Listeners for real-time accuracy.
     */
    private var statusListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun resolveCombinedStatus(userId: String) {
        statusListener?.remove()

        statusListener = userRepository.listenToProfileData(userId) { profile ->
            // 🎯 ROBUST: Check both potential fields and use case-insensitive matching
            val manual = (profile?.get("manualAvailabilityStatus") as? String
                ?: profile?.get("status") as? String ?: "AVAILABLE").uppercase()

            _manualStatusPreference.postValue(manual)

            if (manual == "UNAVAILABLE") {
                _calculatedStatus.postValue("🔴 Unavailable")
            } else {
                // Priority 2: Check Calendar Busy status
                viewModelScope.launch {
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val (bookings, schedules, castings) = repository.getDailyEvents(userId, today)
                    val allEvents = bookings + schedules + castings

                    val calendarStatus = engine.calculateCurrentStatus(allEvents)
                    val result = if (calendarStatus == AvailabilityStatus.BUSY_NOW) "🟠 Busy Now" else "🟢 Available Now"
                    _calculatedStatus.postValue(result)
                }
            }
        }
    }

    fun updateManualOverride(status: String) {
        val uid = auth.currentUser?.uid ?: return
        userRepository.updateManualAvailability(status) { success ->
            if (success) {
                _manualStatusPreference.postValue(status)
                resolveCombinedStatus(uid) // Instant Refresh
            }
        }
    }

    private fun getSafeTimestamp(doc: DocumentSnapshot, field: String): Long? {
        return try {
            when (val value = doc.get(field)) {
                is Long -> value
                is com.google.firebase.Timestamp -> value.toDate().time
                is Number -> value.toLong()
                else -> null
            }
        } catch (e: Exception) { null }
    }

    /**
     * UNIVERSAL DATE NORMALIZER:
     * Aligns all timestamps to UTC to prevent date-shifting.
     */
    private fun getFormattedDate(doc: DocumentSnapshot): String? {
        val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateFields = arrayOf("auditionDate", "date", "audition_date", "Date", "audition_day")

        for (field in dateFields) {
            val ts = getSafeTimestamp(doc, field)
            if (ts != null && ts > 0) return targetFormat.format(Date(ts))

            val raw = doc.getString(field)?.trim() ?: continue
            if (raw.isBlank()) continue

            val clean = raw.replace("\u00A0", " ").replace(Regex("\\s+"), " ")

            // Handle ISO/Slash formats (yyyy-MM-dd) even with single digits
            if (clean.matches(Regex("\\d{4}[\\-/]\\d{1,2}[\\-/]\\d{1,2}"))) {
                val p = clean.split(Regex("[\\-/]"))
                return String.format(Locale.US, "%04d-%02d-%02d", p[0].toInt(), p[1].toInt(), p[2].toInt())
            }

            try {
                val parts = clean.split(Regex("[\\s\\-/]+"))
                if (parts.size >= 2) {
                    var day: Int? = null
                    var month: Int = -1
                    val p0 = parts[0].filter { it.isDigit() }.toIntOrNull()
                    if (p0 != null && p0 <= 31) {
                        day = p0
                        month = parseMonthSafe(parts[1])
                    } else {
                        val p1 = parts[1].filter { it.isDigit() }.toIntOrNull()
                        if (p1 != null && p1 <= 31) {
                            day = p1
                            month = parseMonthSafe(parts[0])
                        }
                    }
                    if (day != null && month != -1) {
                        val year = if (parts.size >= 3) parts[parts.size - 1].filter { it.isDigit() }.take(4).toIntOrNull() ?: currentMonth.get(Calendar.YEAR) else currentMonth.get(Calendar.YEAR)
                        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
                    }
                }
            } catch (e: Exception) {}
        }
        return null
    }

    private fun parseMonthSafe(m: String): Int {
        val low = m.lowercase()
        val num = low.filter { it.isDigit() }.toIntOrNull()
        if (num != null && num in 1..12) return num
        return when {
            low.startsWith("jan") -> 1; low.startsWith("feb") -> 2; low.startsWith("mar") -> 3
            low.startsWith("apr") -> 4; low.startsWith("may") -> 5; low.startsWith("jun") -> 6
            low.startsWith("jul") -> 7; low.startsWith("aug") -> 8; low.startsWith("sep") -> 9
            low.startsWith("oct") -> 10; low.startsWith("nov") -> 11; low.startsWith("dec") -> 12
            else -> -1
        }
    }

    fun refreshAvailabilityGrid(userId: String) {
        _currentMonthText.value = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time)
        if (userId.isEmpty()) return

        viewModelScope.launch {
            try {
                // 🎯 FIX: Fetch latest Manual Status and check both potential fields (Talent/Recruiter)
                val profile = db.collection("profiles").document(userId).get().await()
                val manual = (profile.getString("manualAvailabilityStatus")
                    ?: profile.getString("status") ?: "AVAILABLE").uppercase()

                _manualStatusPreference.value = manual // Immediate update on Main thread

                val cal = currentMonth.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = normalizeToMidnight(cal.timeInMillis)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = normalizeToMidnight(cal.timeInMillis) + 86399999

                val context = repository.getAvailabilityContext(userId, start, end)
                val dayDataMap = processEventMetadata(userId, context.role, context.bookings, context.schedules, context.castingCalls)
                _calendarDays.postValue(generateGrid(currentMonth, dayDataMap))
            } catch (e: Exception) {
                Log.e("SceneX_Intel", "Sync Error", e)
                _syncError.postValue("Grid Sync Failure")
            }
        }
    }

    private fun processEventMetadata(
        viewedUserId: String,
        userRole: String?,
        bookings: List<DocumentSnapshot>,
        schedules: List<DocumentSnapshot>,
        castingCalls: List<DocumentSnapshot>
    ): Map<String, Pair<Set<DayStatus>, List<CalendarEvent>>> {
        val dataMap = mutableMapOf<String, Pair<MutableSet<DayStatus>, MutableList<CalendarEvent>>>()

        fun getEntry(date: String) = dataMap.getOrPut(date) { mutableSetOf<DayStatus>() to mutableListOf<CalendarEvent>() }

        // 1. Casting Calls Processing
        castingCalls.forEach { doc ->
            getFormattedDate(doc)?.let { dateStr ->
                val rId = doc.getString("recruiterId") ?: doc.getString("recruiterid") ?:
                doc.getString("recruiter_id") ?: doc.getString("userId") ?: ""

                val isOwner = rId == viewedUserId

                if (isOwner) {
                    val project = doc.getString("projectTitle") ?: "Casting Audition"
                    val role = doc.getString("characterName") ?: doc.getString("category") ?: "Audition"
                    val time = "${doc.getString("startTime") ?: "TBA"} - ${doc.getString("endTime") ?: "TBA"}"
                    val loc = doc.getString("auditionLocation") ?: "TBA"

                    val (statuses, events) = getEntry(dateStr)
                    statuses.add(DayStatus.CASTING_CALL)
                    events.add(CalendarEvent(
                        title = project,
                        time = time,
                        type = DayStatus.CASTING_CALL,
                        description = "Casting Call | Role: $role | Loc: $loc"
                    ))

                    statuses.add(DayStatus.BUSY)
                    events.add(CalendarEvent(
                        title = "Audition Commitment: $project",
                        time = time,
                        type = DayStatus.BUSY,
                        description = "You are conducting this audition session.\nRole: $role\nLoc: $loc"
                    ))
                }
            }
        }

        // 2. Bookings Processing
        bookings.forEach { doc ->
            getFormattedDate(doc)?.let { dateStr ->
                val statusStr = doc.getString("status")?.uppercase() ?: "PENDING"
                if (statusStr == "REJECTED" || statusStr == "CANCELLED") return@forEach

                val statusType = if (statusStr == "CONFIRMED" || statusStr == "ACCEPTED") DayStatus.BUSY else DayStatus.PENDING

                val project = doc.getString("castingTitle") ?: "Project: ${doc.getString("role") ?: "Booking"}"
                val time = "${doc.getString("startTime") ?: "TBA"} - ${doc.getString("endTime") ?: "TBA"}"
                val location = doc.getString("location") ?: "TBA"

                val docRecruiterId = doc.getString("recruiterId") ?: doc.getString("recruiterid") ?: doc.getString("recruiter_id") ?: ""
                val partner = if (viewedUserId == docRecruiterId) {
                    "With Talent: ${doc.getString("name") ?: "N/A"}"
                } else {
                    "With Recruiter: ${doc.getString("recruiterName") ?: "Client"}"
                }

                val (statuses, events) = getEntry(dateStr)
                statuses.add(statusType)
                events.add(CalendarEvent(
                    title = project,
                    time = time,
                    type = statusType,
                    description = "Status: $statusStr\n$partner\nLoc: $location"
                ))
            }
        }

        // 3. Schedules Processing
        schedules.forEach { doc ->
            getFormattedDate(doc)?.let { dateStr ->
                val project = doc.getString("castingTitle") ?: "Confirmed Session"
                val time = "${doc.getString("startTime") ?: "TBA"} - ${doc.getString("endTime") ?: "TBA"}"
                val location = doc.getString("location") ?: "TBA"

                val docRecruiterId = doc.getString("recruiterId") ?: doc.getString("recruiterid") ?: doc.getString("recruiter_id") ?: ""
                val partner = if (viewedUserId == docRecruiterId) {
                    "With Talent: ${doc.getString("name") ?: "N/A"}"
                } else {
                    "With Recruiter"
                }

                val (statuses, events) = getEntry(dateStr)
                statuses.add(DayStatus.BUSY)
                events.add(CalendarEvent(
                    title = project,
                    time = time,
                    type = DayStatus.BUSY,
                    description = "CONFIRMED\n$partner\nLoc: $location\nNote: ${doc.getString("notes") ?: ""}"
                ))
            }
        }

        return dataMap.mapValues { it.value.first to it.value.second }
    }

    fun navigateMonth(delta: Int, userId: String) {
        currentMonth.add(Calendar.MONTH, delta)
        refreshAvailabilityGrid(userId)
    }

    private fun normalizeToMidnight(timestamp: Long): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun generateGrid(calendar: Calendar, dataMap: Map<String, Pair<Set<DayStatus>, List<CalendarEvent>>>): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        var offset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        if (offset < 0) offset += 7
        cal.add(Calendar.DAY_OF_MONTH, -offset)

        repeat(42) {
            val dStr = sdf.format(cal.time)
            val dayData = dataMap[dStr]

            // 🎯 FIX: Merge Manual Unavailability with System Events (Case-Insensitive)
            val statuses = (dayData?.first ?: emptySet<DayStatus>()).toMutableSet()
            if (_manualStatusPreference.value?.uppercase() == "UNAVAILABLE") {
                statuses.add(DayStatus.MANUAL_UNAVAILABLE)
            }

            days.add(CalendarDay(
                dateString = dStr,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString(),
                statuses = statuses,
                events = dayData?.second ?: emptyList<CalendarEvent>(),
                isCurrentMonth = cal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
            ))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return days
    }

    fun checkAvailability(userId: String, date: Long, startTime: String, endTime: String, onResult: (AvailabilityResult) -> Unit) {
        viewModelScope.launch {
            try {
                // 🎯 Priority 1: Check Manual Override first (Recruiter/Talent robust check)
                val profile = db.collection("profiles").document(userId).get().await()
                val manualStatus = (profile.getString("manualAvailabilityStatus")
                    ?: profile.getString("status") ?: "AVAILABLE").uppercase()

                if (manualStatus == "UNAVAILABLE") {
                    onResult(AvailabilityResult.BUSY)
                    return@launch
                }

                val timestamp = normalizeToMidnight(date)
                val targetDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date(timestamp))
                val (bookings, schedules, castingCalls) = repository.getDailyEvents(userId, timestamp)

                val hardBlocks = (bookings + schedules).filter {
                    val status = it.getString("status")?.uppercase() ?: "CONFIRMED"
                    getFormattedDate(it) == targetDateStr && (status == "CONFIRMED" || status == "ACCEPTED")
                } + castingCalls

                onResult(engine.checkSlotAvailability(hardBlocks, startTime, endTime))
            } catch (e: Exception) { onResult(AvailabilityResult.BUSY) }
        }
    }
}